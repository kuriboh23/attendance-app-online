package com.example.project.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.UserPrefs
import com.example.project.adapter.MonthAdapter
import com.example.project.data.Check
import com.example.project.data.TimeManager
import com.example.project.databinding.FragmentSalaryBinding
import com.example.project.fragment.list.MonthItem
import com.example.project.fragment.list.SalaryAdapter
import com.example.project.fragment.list.YearHeader
import com.example.project.function.function.showCustomToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Salary : Fragment() {

    lateinit var binding: FragmentSalaryBinding
    lateinit var auth: FirebaseAuth
    lateinit var userRef: DatabaseReference

    lateinit var salaryAdapter: SalaryAdapter

    private val now = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSalaryBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")
        val uid = auth.currentUser?.uid ?: return binding.root

        val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthNameYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val monthYear = monthYearFormat.format(now)
        val monthNameYear = monthNameYearFormat.format(now)

        binding.tvMonthYear.text = monthNameYear

        // Set up RecyclerView for salary details
        binding.RecView.layoutManager = LinearLayoutManager(requireContext())
        salaryAdapter = SalaryAdapter(emptyList())
        binding.RecView.adapter = salaryAdapter

        userRef.child(uid).child("timeManager").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val timeManagersList = snapshot.children.mapNotNull { dataSnapshot ->
                        val timeMG = dataSnapshot.getValue(TimeManager::class.java)
                        if (timeMG?.date?.startsWith(monthYear) == true) timeMG else null

                    }
                    if (timeManagersList.isNotEmpty()){
                        val workTime = timeManagersList.sumOf { it.workTime ?:0 }
                        val extraTime = timeManagersList.sumOf { it.extraTime ?: 0 }
                        val salaryNet = (workTime + extraTime) * 50

                        binding.tvSalaryNet.text = "MAD $salaryNet"
                        salaryAdapter.updateData(timeManagersList)
                    }else{
                        binding.tvSalaryNet.text = "MAD 0"
                        salaryAdapter.updateData(emptyList())

                        requireContext().showCustomToast("No Data Found", R.layout.error_toast)
                    }
                }
            }


        // Filter button to select a different month
        binding.filterMouth.setOnClickListener {
            filterByMonth(uid)
        }

        return binding.root
    }

    fun filterByMonth(uid:String) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_date, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)

        // Set up RecyclerView for month selection
        val recyclerView: RecyclerView = view.findViewById(R.id.month_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Prepare data for month selection
        val items = mutableListOf<YearHeader>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        for (year in currentYear - 2..currentYear) {
            val monthsList = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            ).mapIndexed { index, name ->
                MonthItem(
                    year = year,
                    monthNumber = index + 1,
                    monthName = name
                )
            }
            items.add(YearHeader(year = year, months = monthsList))
        }

        val monthAdapter = MonthAdapter(items) { selectedMonth ->
            val monthYearStr =
                "${selectedMonth.year}-${selectedMonth.monthNumber.toString().padStart(2, '0')}"
            println("Selected month: $monthYearStr")

            userRef.child(uid).child("timeManager").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val timeManagersList = snapshot.children.mapNotNull { dataSnapshot ->
                            val timeMG = dataSnapshot.getValue(TimeManager::class.java)
                            if (timeMG?.date?.startsWith(monthYearStr) == true) timeMG else null
                        }
                        if (timeManagersList.isNotEmpty()) {
                            val workTime = timeManagersList.sumOf { it.workTime ?: 0 }
                            val extraTime = timeManagersList.sumOf { it.extraTime ?: 0 }
                            val salaryNet = (workTime + extraTime) * 50

                            binding.tvSalaryNet.text = "MAD $salaryNet"
                            salaryAdapter.updateData(timeManagersList)
                        }else{
                            binding.tvSalaryNet.text = "MAD 0"
                            salaryAdapter.updateData(emptyList())

                            requireContext().showCustomToast("No Data Found", R.layout.error_toast)
                        }
                        binding.tvMonthYear.text =
                            "${selectedMonth.monthName} ${selectedMonth.year}"
                    }
                }
            dialog.dismiss()
        }
        recyclerView.adapter = monthAdapter
        dialog.show()
    }
}