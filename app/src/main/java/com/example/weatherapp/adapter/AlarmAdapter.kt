package com.example.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.AlarmScreen
import com.example.weatherapp.R

class AlarmAdapter(private val alarms:List<AlarmScreen.Alarm>):RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AlarmAdapter.AlarmViewHolder {
//inflation

        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_item,parent,false)
        return AlarmViewHolder(view)
        }


    override fun onBindViewHolder(holder: AlarmAdapter.AlarmViewHolder, position: Int) {
holder.bind(alarms[position])
    }

    override fun getItemCount(): Int {
         return alarms.size
    }
    inner class AlarmViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)
    {
        fun bind(alarm: AlarmScreen.Alarm){
            alarmTimeTextView.text=alarm.formattedTime
        }
        val alarmTimeTextView: TextView =itemView.findViewById(R.id.formattedTime)
    }
}