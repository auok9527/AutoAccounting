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

package net.ankio.auto.hooks.auto.hooks

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.database.table.AssetsMap


class ActiveHooker(hooker: Hooker) : PartHooker(hooker) {
    override val hookName: String
        get() = "自动记账激活"
    override fun onInit(classLoader: ClassLoader?, context: Context?) {
        val activeUtils = XposedHelpers.findClass("net.ankio.auto.utils.ActiveUtils", classLoader)
        // hook激活方法
        XposedHelpers.findAndHookMethod(
            activeUtils,
            "getActiveAndSupportFramework",
            Context::class.java,
            XC_MethodReplacement.returnConstant(true)
        )


    }
}