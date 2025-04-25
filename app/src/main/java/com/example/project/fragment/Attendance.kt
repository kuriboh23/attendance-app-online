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
                        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                        val monthNameYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

                        binding.tvMonthYear.text = monthNameYear
                        binding.summaryText.text = "Summary of $monthName"
                        binding.weeksText.text = "Week $weekOfMonth"
                    } else {
                        requireContext().showCustomToast("No checks for today", R.layout.error_toast)
                        checkAdapter.setData(emptyList())
                    }
                } else {
                    requireContext().showCustomToast("No check data found", R.layout.error_toast)
                    checkAdapter.setData(emptyList())
                }
            }
            .addOnFailureListener {
                requireContext().showCustomToast("Failed to fetch data", R.layout.error_toast)
            }

        binding.filterMouth.setOnClickListener {
            filterByMonth(dateFormatter, uid)
        }

        return binding.root
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
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val startWeek = calendar.time

            // End of week (Friday)
            calendar.add(Calendar.DAY_OF_WEEK, 4)
            val endWeek = calendar.time

            val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val monthNameYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val startOfWeek = dateFormat.format(startWeek)
            val endOfWeek = dateFormat.format(endWeek)

            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
            val monthNameYear = monthNameYearFormat.format(startWeek)
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(startWeek)

            userRef.child(uid).child("checks").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val checkList = snapshot.children.mapNotNull { dataSnapshot ->
                            val check = dataSnapshot.getValue(Check::class.java)
                            if (check?.date in startOfWeek..endOfWeek) check else null
                        }
                        if (checkList.isNotEmpty()) {
                            checkAdapter.setData(checkList)

                            binding.tvMonthYear.text = monthNameYear
                            binding.summaryText.text = "Summary of $monthName"
                            binding.weeksText.text = "Week $weekOfMonth"

                        }
                    }
                }
        }
    }
}