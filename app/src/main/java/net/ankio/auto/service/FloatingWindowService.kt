/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import androidx.core.content.ContextCompat
import com.hjq.toast.Toaster
import com.quickersilver.themeengine.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.app.BillUtils
import net.ankio.auto.constant.FloatEvent
import net.ankio.auto.databinding.FloatTipBinding
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.events.BillUpdateEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.FloatPermissionUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.server.model.BillInfo
import kotlin.coroutines.CoroutineContext

class FloatingWindowService : Service(), CoroutineScope {
    private val windowManager: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val floatingViews = mutableListOf<FloatTipBinding>()
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var themedContext: Context
    private val list = ArrayDeque<BillInfo>()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var showWindow = false
    private var timeCount: Int = 0

    private var billInfo: BillInfo? = null

    private var child = HashMap<Int, ArrayList<BillInfo>>()

    override fun onCreate() {
        super.onCreate()
        timeCount = runCatching { SpUtils.getString("setting_float_time", "10").toInt() }.getOrNull() ?: 0

        val defaultTheme = ContextThemeWrapper(applicationContext, R.style.AppTheme)

        themedContext =
            ContextThemeWrapper(
                defaultTheme,
                ThemeEngine.getInstance(applicationContext).getTheme(),
            )
        launch {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    if (list.isEmpty() || showWindow) {
                        delay(100)
                        continue
                    }
                    billInfo = list.removeFirst()
                    runCatching {
                        processBillInfo()
                    }.onFailure {
                        if (it is BadTokenException) {
                            if (it.message != null && it.message!!.contains("permission denied")) {
                                Toaster.show(R.string.floatTip)
                                FloatPermissionUtils.requestPermission(themedContext)
                            }
                        }
                        Logger.e("记账失败", it)
                    }
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        val value = intent.getStringExtra("data") ?: return START_REDELIVER_INTENT
        val bill = BillInfo.fromJSON(value)
        launch {
            val tpl = SpUtils.getString("setting_bill_remark", "【商户名称】 - 【商品名称】")
            bill.remark = BillUtils.getRemark(bill, tpl)
            BillUtils.setAccountMap(bill)

            if (BillUtils.noNeedFilter(bill))
                {
                    list.add(bill)
                    return@launch
                }

            val newList = ArrayList<BillInfo>()
            newList.addAll(list)
            if (billInfo != null) {
                newList.add(billInfo!!)
            }

            val repeatBill = BillUtils.checkRepeatBill(bill, newList)
            if (repeatBill != null)
                {
                    Logger.i("重复账单:$bill")
                    BillUtils.updateBillInfo(repeatBill, bill)

                    if (!child.contains(repeatBill.money))
                        {
                            child[repeatBill.money] = ArrayList()
                        }

                    child[repeatBill.money]?.add(bill)
                    EventBus.post(BillUpdateEvent(repeatBill, child[repeatBill.money]))
                    if (repeatBill == billInfo)
                        {
                            Logger.i("重复账单:$bill 与当前显示的账单相同")
                            billInfo = repeatBill
                        } else {
                        list.find { it == repeatBill }?.let {
                            list.remove(it)
                            list.add(repeatBill)
                        }
                    }
                } else {
                list.add(bill)
            }
        }
        return START_REDELIVER_INTENT
    }

    private suspend fun processBillInfo() {
        showWindow = true
        if (timeCount == 0) {
            callBillInfoEditor("setting_float_on_badge_timeout")
            // 显示编辑悬浮窗
            return
        }

        // 使用 ViewBinding 初始化悬浮窗视图
        val binding = FloatTipBinding.inflate(LayoutInflater.from(themedContext))
        binding.root.visibility = View.INVISIBLE
        binding.money.text = BillUtils.getFloatMoney(billInfo!!.money).toString()

        val colorRes = BillUtils.getColor(billInfo!!.type.toInt())
        val color = ContextCompat.getColor(themedContext, colorRes)
        binding.money.setTextColor(color)
        binding.time.text = String.format("%ss", timeCount.toString())

        val countDownTimer =
            object : CountDownTimer(timeCount * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.time.text = String.format("%ss", (millisUntilFinished / 1000).toString())
                }

                override fun onFinish() {
                    // 取消倒计时
                    removeTips(binding)
                    callBillInfoEditor("setting_float_on_badge_timeout")
                }
            }
        countDownTimer.start()

        binding.root.setOnClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTips(binding)
            callBillInfoEditor("setting_float_on_badge_click")
        }

        binding.root.setOnLongClickListener {
            countDownTimer.cancel() // 定时器停止
            removeTips(binding)
            // 不记录
            callBillInfoEditor("setting_float_on_badge_long_click")
            true
        }

        // 设置 WindowManager.LayoutParams
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                x = 0 // 居中
                y = -120 // 居中偏上
                gravity = Gravity.CENTER or Gravity.END
            }

        // 将视图添加到 WindowManager
        windowManager.addView(binding.root, params)
        binding.root.post {
            val widthInner =
                binding.logo.width + binding.money.width + binding.time.width + 150 // logo间隔
            // 更新悬浮窗的宽度和高度
            params.width = widthInner // 新宽度，单位：像素
            params.height = binding.logo.height + 60 // 新高度，单位：像素
            // 应用新的布局参数
            windowManager.updateViewLayout(binding.root, params)
            binding.root.visibility = View.VISIBLE
        }

        // 将绑定添加到列表中以便管理
        floatingViews.add(binding)
    }

    private fun removeTips(binding: FloatTipBinding) {
        if (binding.root.isAttachedToWindow) windowManager.removeView(binding.root)
        floatingViews.remove(binding)
    }

    private fun recordBillInfo(billInfo: BillInfo) {
        launch {
            runCatching {
                BillUtils.groupBillInfo(billInfo, child[billInfo.money])
                if (SpUtils.getBoolean("setting_book_success", true)) {
                    Toaster.show(
                        getString(
                            R.string.auto_success,
                            BillUtils.getFloatMoney(billInfo.money).toString(),
                        ),
                    )
                }
                child.remove(billInfo.money)
            }.onFailure {
                if (it is AutoServiceException) {
                    EventBus.post(AutoServiceErrorEvent(it))
                }
            }
        }
    }

    private fun callBillInfoEditor(key: String) {
        when (SpUtils.getInt(key, FloatEvent.POP_EDIT_WINDOW.ordinal)) {
            FloatEvent.AUTO_ACCOUNT.ordinal -> {
                // 记账
                recordBillInfo(billInfo!!)
                showWindow = false
            }

            FloatEvent.POP_EDIT_WINDOW.ordinal -> {
                launch {
                    AppUtils.getService().config().let {
                        // 编辑
                        withContext(Dispatchers.Main) {
                            FloatEditorDialog(themedContext, billInfo!!, it, true, false) {
                                showWindow = false
                            }.show(true)
                        }
                    }
                }
            }

            FloatEvent.NO_ACCOUNT.ordinal -> {
                showWindow = false
            }
        }
    }

    override fun onDestroy() {
        // 清理所有悬浮窗
        for (binding in floatingViews) {
            if (binding.root.isAttachedToWindow) windowManager.removeView(binding.root)
        }
        floatingViews.clear()
        job.cancel()
        super.onDestroy()
    }
}
