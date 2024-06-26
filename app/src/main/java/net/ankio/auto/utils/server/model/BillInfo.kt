/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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
package net.ankio.auto.utils.server.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger

class BillInfo {
    // 账单列表
    var id = 0

    /**
     * 账单类型 只有三种
     */
    var type: Int = 0

    /**
     * 币种类型
     */
    var currency: String = ""

    /**
     * 金额 大于0
     */
    var money: Int = 0

    /**
     * 手续费
     */
    var fee: Int = 0

    /**
     * 记账时间
     * yyyy-MM-dd HH:mm:ss
     */
    var timeStamp: Long = 0

    /**
     * 商户名称
     */
    var shopName: String = ""

    /**
     * 商品名称
     */
    var shopItem: String = ""

    /**
     * 分类名称
     */
    var cateName: String = "其他"

    /**
     * 拓展数据域，如果是报销或者销账，会对应账单ID
     */
    var extendData: String = ""

    /**
     * 账本名称
     */
    var bookName: String = "默认账本"

    /**
     * 账单所属资产名称（或者转出账户）
     */
    var accountNameFrom: String = ""

    /**
     * 转入账户
     */
    var accountNameTo = ""

    /**
     * 这笔账单的来源,例如是微信还是支付宝
     */
    var from = ""

    /**
     * 来源类型，app、无障碍、通知、短信
     */
    var fromType: Int = 0

    /**
     * 分组id，这个id是指将短时间内捕获到的同等金额进行合并的分组id
     */
    var groupId: Int = 0

    /**
     * 数据渠道，这里指的是更具体的渠道，例如【建设银行】微信公众号，用户【xxxx】这种
     */
    var channel: String = ""

    /**
     * 是否已从App同步
     */
    var syncFromApp: Boolean = false

    /**
     * 备注信息
     */
    var remark: String = ""

    fun copy(): BillInfo {
        val billInfo = BillInfo()
        billInfo.id = id
        billInfo.type = type
        billInfo.currency = currency
        billInfo.money = money
        billInfo.fee = fee
        billInfo.timeStamp = timeStamp
        billInfo.shopName = shopName
        billInfo.shopItem = shopItem
        billInfo.cateName = cateName
        billInfo.extendData = extendData
        billInfo.bookName = bookName
        billInfo.accountNameFrom = accountNameFrom
        billInfo.accountNameTo = accountNameTo
        billInfo.from = from
        billInfo.fromType = fromType
        billInfo.groupId = groupId
        billInfo.channel = channel
        billInfo.syncFromApp = syncFromApp
        billInfo.remark = remark
        return billInfo
    }

    companion object {
        suspend fun put(billInfo: BillInfo): Int {
            return AppUtils.getService().sendMsg("bill/put", billInfo) as Int
        }

        suspend fun getBillListGroup(limit: Int = 500): List<Pair<String, String>> {
            val array = ArrayList<Pair<String, String>>()
            runCatching {
                val data = AppUtils.getService().sendMsg("bill/list/group", mapOf("limit" to limit)) as JsonArray

                data.forEach {
                    val obj = it.asJsonObject
                    val date = obj.get("date").asString
                    val ids = obj.get("ids").asString
                    array.add(Pair(date, ids))
                }
            }.onFailure {
                Logger.e("getBillListGroup", it)
            }
            return array
        }

        suspend fun getBillByIds(ids: String): Array<BillInfo> {
            val data = AppUtils.getService().sendMsg("bill/list/id", mapOf("ids" to ids))
            return data as Array<BillInfo>
        }

        suspend fun getBillByGroup(group: Int): Array<BillInfo> {
            val data = AppUtils.getService().sendMsg("bill/list/child", mapOf("groupId" to group))
            return data as Array<BillInfo>
        }

        fun fromJSON(value: String): BillInfo {
            return Gson().fromJson(value, BillInfo::class.java)
        }

        suspend fun getAllParents(): Array<BillInfo> {
            val data = AppUtils.getService().sendMsg("bill/list/parent", null)
            return data as Array<BillInfo>
        }
    }
}
