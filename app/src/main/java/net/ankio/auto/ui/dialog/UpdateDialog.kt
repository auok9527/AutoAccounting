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

package net.ankio.auto.ui.dialog


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogUpdateBinding
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.events.UpdateSuccessEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.request.RequestsUtils
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.event.EventBus
import rikka.html.text.toHtml


class UpdateDialog(
    private val context: Context,
    private val download: HashMap<String, String>,//下载地址
    val log: String,//更新日志,html
    val version: String,//版本号
    private val date: String,//更新日期
    val type: Int = 0,//更新类型,0 为App, 1 为规则
    private val code:Int = 0
) : BaseSheetDialog(context) {

    private lateinit var binding: DialogUpdateBinding
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogUpdateBinding.inflate(inflater)

        cardView = binding.cardView
        cardViewInner  = binding.cardViewInner

        binding.version.text = version
        binding.updateInfo.text = log.toHtml()
        binding.date.text = date
        binding.name.text = if (type == 1) context.getString(R.string.rule) else context.getString(R.string.app)
        binding.update.setOnClickListener {
            if (type == 1) {
                lifecycleScope.launch {
                    updateRule(download["category"] ?: "", download["rule"] ?: "")
                }
            } else {
                updateApp(download["url"] ?: "")
            }
            dismiss()
        }

        return binding.root
    }

    private suspend fun updateRule(category: String, rule: String) = withContext(Dispatchers.IO){
        runCatching {
            val requestUtils = RequestsUtils(context)
            val result = requestUtils.get(rule, cacheTime = 0)
            //规则更新
            String(result.byteArray).let {
                AppUtils.getService().set("auto_rule", it)
            }
            //分类更新

            val result2 = requestUtils.get(category, cacheTime = 0)
            //规则更新
            String(result2.byteArray).let {
                AppUtils.getService().set("auto_category", it)
            }

        }.onFailure {
            if(it is AutoServiceException){
                withContext(Dispatchers.Main){
                    EventBus.post(AutoServiceErrorEvent(it))
                }
            }
            Logger.e("更新出错", it)
        }.onSuccess {
            SpUtils.putString("ruleVersionName", version)
            SpUtils.putInt("ruleVersion", code)

            withContext(Dispatchers.Main){
                EventBus.post(UpdateSuccessEvent())
            }
        }
    }
    //URL由外部构造可能出错
    private fun updateApp(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }.onFailure {
            Logger.e("更新出错", it)
        }
    }


}