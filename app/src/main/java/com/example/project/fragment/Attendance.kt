package com.example.project.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.R
import com.example.project.UserPrefs
import com.example.project.data.Check
import com.example.project.data.TimeManager
import com.example.project.databinding.FragmentAttendanceBinding
import com.example.project.fragment.list.CheckAdapter
import com.example.project.function.function.showCustomToast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Attendance : Fragment() {
    private lateinit var binding: FragmentAttendanceBinding
    private lateinit var checkAdapter: CheckAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)

        // Setup Firebase
        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        val uid = auth.currentUser?.uid
        if (uid == null) {
            requireContext().showCustomToast("User not logged in", R.layout.error_toast)
            return binding.root
        }

        // Setup RecyclerView
        checkAdapter = CheckAdapter()
        binding.RecView.layoutManager = LinearLayoutManager(requireContext())
        binding.RecView.adapter = checkAdapter

        updateCheckCounts(uid,false,currentMonth)

        binding.filterMouth.setOnClickListener {
            filterByMonth(dateFormatter, uid)
        }

        binding.tvMonthYear.text ="${ SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }"

        return binding.root
    }


    @SuppressLint("SetTextI18n")
    fun updateCheckCounts(uid: String,
      isFiltered: Boolean = false,
      monthYear: String = "",
      startOfWeek: String = "",
      endOfWeek: String = "",
      monthName: String = "",
      weekOfMonth: Int = 0,
      monthNameYear: String = "") {

        if (!isFiltered) {
            binding.loadingOverlay.visibility = View.VISIBLE

            val todayDate = dateFormatter.format(Date())

            userRef.child(uid).child("checks").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {

                        val checkList = snapshot.children.mapNotNull { dataSnapshot ->
                            val check = dataSnapshot.getValue(Check::class.java)
                            if (check?.date == todayDate) check else null
                        }
                        if (checkList.isNotEmpty()) {
                            checkAdapter.setData(checkList)

                            val weekOfMonth = Calendar.getInstance().get(Calendar.WEEK_OF_MONTH)
                            val monthName =
                                SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                            val monthNameYear =
                                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

                            binding.tvMonthYear.text = monthNameYear
                            binding.summaryText.text = "Summary of $monthName"
                            binding.weeksText.text = "Week $weekOfMonth"

                            val earlyLeaves = checkList.count { check ->
                                val checkOut = check.checkOutTime
                                checkOut?.let {
                                    val parts = it.split(" ")
                                    val timePart = parts.getOrNull(0)
                                    val amPm = parts.getOrNull(1)

                                    val hour = timePart?.split(":")?.getOrNull(0)?.toIntOrNull()

                                    if (hour != null && amPm != null) {
                                        (amPm == "PM" && hour in 3..5) || // 3 PM to 5 PM (6 not included)
                                                (amPm == "AM" && hour in 8..11)   // 8 AM to 11 AM
                                    } else false
                                } ?: false
                            }

                            binding.tvLeaveCount.text = earlyLeaves.toString()
                            binding.loadingOverlay.visibility = View.GONE

                        } else {
                            requireContext().showCustomToast(
                                "No checks for today",
                                R.layout.error_toast
                            )
                            binding.tvLeaveCount.text = "0"
                            checkAdapter.setData(emptyList())
                            binding.loadingOverlay.visibility = View.GONE
                        }
                    } else {
                        requireContext().showCustomToast(
                            "No check data found",
                            R.layout.error_toast
                        )
                        binding.tvLeaveCount.text = "0"
                        checkAdapter.setData(emptyList())
                        binding.loadingOverlay.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    requireContext().showCustomToast("Failed to fetch data", R.layout.error_toast)
                }

            userRef.child(uid).child("timeManager").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val timeManagerList = snapshot.children.mapNotNull { dataSnapshot ->
                            val timeMG = dataSnapshot.getValue(TimeManager::class.java)
                            if (timeMG?.date?.startsWith(monthYear) == true) timeMG else null
                        }
                        if (timeManagerList.isNotEmpty()) {
                            val absentCount = timeManagerList.count { it.absent == true }
                            binding.tvAbsentCount.text = absentCount.toString()
                            val lateCount = timeManagerList.count { it.late == true }
                            binding.tvLateCount.text = lateCount.toString()
                            val extraTimeCount = timeManagerList.sumBy { it.extraTime!! }
                            binding.tvExtraTime.text = extraTimeCount.toString()
                        }
                    }
                }
        }
        else{
            binding.loadingOverlay.visibility = View.VISIBLE

            userRef.child(uid).child("checks").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val checkList = snapshot.children.mapNotNull { dataSnapshot ->
                            val check = dataSnapshot.getValue(Check::class.java)
                            if (check?.date.toString() in startOfWeek..endOfWeek) check else null
                        }
                        if (checkList.isNotEmpty()) {
                            checkAdapter.setData(checkList.sortedByDescending { it.date })

                            binding.tvMonthYear.text = monthNameYear
                            binding.summaryText.text = "Summary of $monthName"
                            binding.weeksText.text = "Week $weekOfMonth"

                            val earlyLeaves = checkList.count { check ->
                                val checkOut = check.checkOutTime
                                checkOut?.let {
                                    val parts = it.split(" ")
                                    val timePart = parts.getOrNull(0)
                                    val amPm = parts.getOrNull(1)

                                    val hour = timePart?.split(":")?.getOrNull(0)?.toIntOrNull()

                                    if (hour != null && amPm != null) {
                                        (amPm == "PM" && hour in 3..5) || // 3 PM to 5 PM (6 not included)
                                                (amPm == "AM" && hour in 8..11)   // 8 AM to 11 AM
                                    } else false
                                } ?: false
                            }
                            binding.tvLeaveCount.text = earlyLeaves.toString()
                            binding.loadingOverlay.visibility = View.GONE

                        }else{
                            checkAdapter.setData(emptyList())
                            requireContext().showCustomToast("No Data Found", R.layout.error_toast)
                            binding.loadingOverlay.visibility = View.GONE
                        }
                    }
                }

            userRef.child(uid).child("timeManager").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val timeManagerList = snapshot.children.mapNotNull { dataSnapshot ->
                            val timeMG = dataSnapshot.getValue(TimeManager::class.java)
                            if (timeMG?.date?.startsWith(monthYear) == true) timeMG else null

                        }
                        if (timeManagerList.isNotEmpty()) {
                            val absentCount = timeManagerList.count { it.absent == true }
                            binding.tvAbsentCount.text = absentCount.toString()
                            val lateCount = timeManagerList.count { it.late == true }
                            binding.tvLateCount.text = lateCount.toString()
                            val extraTimeCount = timeManagerList.sumBy { it.extraTime!! }
                            binding.tvExtraTime.text = extraTimeCount.toString()
                        }
                    }
                }
        }
    }



    @SuppressLint("SetTextI18n")
    private fun filterByMonth(dateFormat: SimpleDateFormat, uid: String) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pick a date we will use its week")
            .build()

        datePicker.show(childFragmentManager, "WEEK_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection

            // Align to start of the week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            val startWeek = calendar.time

            // End of week (Friday)
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val endWeek = calendar.time

            val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val monthNameYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val startOfWeek = dateFormat.format(startWeek)
            val endOfWeek = dateFormat.format(endWeek)
            val monthYear = monthYearFormat.format(selection)

            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
            val monthNameYear = monthNameYearFormat.format(startWeek)
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(startWeek)

            updateCheckCounts(uid,true,monthYear,startOfWeek,endOfWeek,monthName,weekOfMonth,monthNameYear)
        }
    }
}