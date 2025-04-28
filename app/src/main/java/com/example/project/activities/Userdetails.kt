package com.example.project.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.adapter.MonthAdapter
import com.example.project.data.Leave
import com.example.project.data.TimeManager
import com.example.project.data.User
import com.example.project.databinding.ActivityUserDetailsBinding
import com.example.project.fragment.list.MonthItem
import com.example.project.fragment.list.YearHeader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class Userdetails : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private val currentMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        val uid = intent.getStringExtra("userId") ?: return

        setupUserDetails(uid)
        setupAttendanceSummary(uid, currentMonth, currentMonthName)
        setupLeaveSummary(uid, currentMonth,currentMonthName)

//        binding.ivBack.setOnClickListener { finish() }
        binding.ivFilterCheck.setOnClickListener { showFilterDialog(uid) }
    }

    private fun setupUserDetails(uid: String) {
        userRef.child(uid).get().addOnSuccessListener { snapshot ->
            snapshot.getValue(User::class.java)?.let { user ->
                binding.tvUserFullName.text = "${user.name} ${user.lastName}"
                binding.tvUserGmail.text = user.email
            }
        }
    }

    private fun setupAttendanceSummary(uid: String, monthYear: String, monthName: String) {
        userRef.child(uid).child("timeManager").get().addOnSuccessListener { snapshot ->
            val timeManagers = snapshot.children.mapNotNull {
                it.getValue(TimeManager::class.java)
            }.filter {
                it.date?.startsWith(monthYear) == true
            }

                val present = timeManagers.count { it.late == false && it.absent == false }
                val late = timeManagers.count { it.late == true }
                val absent = timeManagers.count { it.absent == true }

                binding.tvPresentCount.text = present.toString()
                binding.tvLateCount.text = late.toString()
                binding.tvAbsentCount.text = absent.toString()

            // Always set title outside to avoid missing it
            binding.tvSummaryTitle.text = "Summary of $monthName"
            binding.tvSummaryleave.text = "Summary of $monthName"
        }
    }


    private fun setupLeaveSummary(uid: String, monthYear: String,monthName: String) {
        userRef.child(uid).child("leaves").get().addOnSuccessListener { snapshot ->
            val leavesList = snapshot.children.mapNotNull {
                it.getValue(Leave::class.java)?.takeIf { leave -> leave.date.startsWith(monthYear) }
            }

                val totalLeaves = leavesList.size
                val pending = leavesList.count { it.status == "Pending" }
                val approved = leavesList.count { it.status == "Approved" }
                val rejected = leavesList.count { it.status == "Rejected" }

                binding.tvPendingLeaves.text = pending.toString()
                binding.tvApprovedLeaves.text = approved.toString()
                binding.tvRejectedLeaves.text = rejected.toString()
                binding.tvLeaveTitle.text = "$totalLeaves Leaves"
                binding.tvSummaryleave.text = "Summary of $monthName"
        }
    }

    private fun showFilterDialog(uid: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_date, null)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)

        val recyclerView: RecyclerView = view.findViewById(R.id.month_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val items = generateYearMonthItems()

        val monthAdapter = MonthAdapter(items) { selectedMonth ->
            val selectedMonthYear = "${selectedMonth.year}-${selectedMonth.monthNumber.toString().padStart(2, '0')}"
            val selectedMonthName = selectedMonth.monthName

            binding.tvSummaryTitle.text = "Summary of $selectedMonthName"
            binding.tvSummaryleave.text = "Summary of $selectedMonthName"

            setupAttendanceSummary(uid, selectedMonthYear, selectedMonthName)
            setupLeaveSummary(uid, selectedMonthYear,selectedMonthName)

            dialog.dismiss()
        }

        recyclerView.adapter = monthAdapter
        dialog.show()
    }

    private fun generateYearMonthItems(): List<YearHeader> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val items = mutableListOf<YearHeader>()

        for (year in currentYear - 2..currentYear) {
            val months = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            ).mapIndexed { index, name ->
                MonthItem(year, index + 1, name)
            }
            items.add(YearHeader(year, months))
        }

        return items
    }
}
