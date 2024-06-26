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

package net.ankio.auto.hooks.alipay.hooks

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.HookMainApp
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.constant.DataType
import org.json.JSONObject


class RedPackageHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "支付宝红包页面"
    override fun onInit(classLoader: ClassLoader, context: Context) {

        val proguard =
            XposedHelpers.findClass("com.alipay.mobile.redenvelope.proguard.c.b", classLoader)
        val syncMessage = XposedHelpers.findClass(
            "com.alipay.mobile.rome.longlinkservice.syncmodel.SyncMessage",
            classLoader
        )

        XposedHelpers.findAndHookMethod(
            proguard,
            "onReceiveMessage",
            syncMessage,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    logD("支付宝收到红包数据hook成功。")
                    super.beforeHookedMethod(param)
                    val syncMessageObject = param.args[0]
                    val getDataMethod = syncMessage.methods.find { it.name == "getData" }
                    getDataMethod?.let {
                        val result = it.invoke(syncMessageObject) as String
                        logD("红包数据： $result")
                        analyzeData(DataType.App.ordinal,result)
                    }
                }
            })
    }
}