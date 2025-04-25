package com.example.project.fragment.list

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.TimeManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class SalaryAdapter(private var timeManagers: List<TimeManager>
) : RecyclerView.Adapter<SalaryAdapter.TimeManagerViewHolder>() {

    class TimeManagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txDayNum: TextView = itemView.findViewById(R.id.txDayNum)
        val txDayText: TextView = itemView.findViewById(R.id.txDayText)
        val tvWorkTime: TextView = itemView.findViewById(R.id.tv_work_time)
        val tvExtraTime: TextView = itemView.findViewById(R.id.tv_extra_time)
        val tvSalary: TextView = itemView.findViewById(R.id.tv_salary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeManagerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_manager, parent, false)
        return TimeManagerViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TimeManagerViewHolder, position: Int) {
        val timeManager = timeManagers[position]
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Skip if critical data is missing
        val date = timeManager.date ?: return
        val workTime = timeManager.workTime ?: 0
        val extraTime = timeManager.extraTime ?: 0

        val dailySalary = (workTime + extraTime) * 50
        val dayNum = date.split("-").getOrNull(2) ?: "01"

        holder.txDayNum.text = dayNum
        holder.txDayText.text = getDayAbbreviation(date)
        holder.tvWorkTime.text = "$workTime hours"
        holder.tvExtraTime.text = "$extraTime hours"
        holder.tvSalary.text = "MAD $dailySalary"

    }

    override fun getItemCount(): Int = timeManagers.size

    fun updateData(newTimeManagers: List<TimeManager>) {
        timeManagers = newTimeManagers
        notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDayAbbreviation(dateString: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
        val date = LocalDate.parse(dateString, inputFormatter)
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH) // "EEE" gives "Fri"
        return date.format(dayFormatter)
    }
}
