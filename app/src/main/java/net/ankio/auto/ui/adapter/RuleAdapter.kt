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

package net.ankio.auto.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.constant.DataType
import net.ankio.auto.database.table.AppData
import net.ankio.auto.database.table.Regular
import net.ankio.auto.databinding.AdapterDataBinding
import net.ankio.auto.databinding.AdapterRuleBinding
import net.ankio.auto.utils.AppInfoUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.ThemeUtils
import org.json.JSONObject


class RuleAdapter(private val dataItems: List<Regular>,private val listener: RuleItemListener) : RecyclerView.Adapter<RuleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterRuleBinding.inflate(LayoutInflater.from(parent.context),parent,false),parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataItems[position]
        holder.bind(item,position)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

    inner class ViewHolder(private val binding: AdapterRuleBinding,private val context:Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Regular,position: Int) {
            binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))

            if(!item.auto){
                binding.type.visibility = View.GONE
            }

            binding.app.text = item.category

            binding.deleteData.setOnClickListener {
                listener.onClickDelete(item,position)

            }
            binding.editRule.setOnClickListener{
                listener.onClickEditData(item,position)
            }
        }
    }
}

interface RuleItemListener{
    fun onClickDelete(item: Regular,position: Int)

    fun onClickEditData(item: Regular,position: Int)
}