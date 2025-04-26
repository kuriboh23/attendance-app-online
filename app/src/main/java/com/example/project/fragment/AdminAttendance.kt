package com.example.project.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.Check
import com.example.project.data.User
import com.example.project.databinding.FragmentAdminAttendanceBinding
import com.example.project.fragment.adapters.CheckAdapter
import com.example.project.fragment.adapters.TeamUserAdapter
import com.example.project.fragment.adapters.UserWithStatus
import com.example.project.fragment.adapters.UserWithUid

import com.example.project.function.function.showCustomToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminAttendance : Fragment() {

    lateinit var binding: FragmentAdminAttendanceBinding
    lateinit var auth: FirebaseAuth
    lateinit var userRef: DatabaseReference

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val now = System.currentTimeMillis()
    private val userWithUidList = mutableListOf<UserWithUid>()

    private lateinit var teamUserAdapter: TeamUserAdapter
    lateinit var checkAdapter: CheckAdapter

    val currentDateStr = dateFormatter.format(Date(now))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminAttendanceBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        binding.searchUser.setOnClickListener {
            showUseSearchBottomSheet()
        }

        val monthNameYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthNameYear = monthNameYearFormat.format(now)

        binding.tvMonthYear.text = monthNameYear
        binding.weeksText.isVisible = false

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun filterByMonth(uid: String, dateFormat: SimpleDateFormat) {
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
                            if (check?.date.toString() in startOfWeek..endOfWeek) check else null
                        }
                        if (checkList.isNotEmpty()) {
                            checkAdapter.updateChecks(checkList.sortedByDescending { it.date })

                            binding.tvMonthYear.text = monthNameYear
                            binding.summaryText.text = "Summary of $monthName"
                            binding.weeksText.isVisible = true
                            binding.weeksText.text = "Week $weekOfMonth"

                        }else{
                            checkAdapter.updateChecks(emptyList())
                            binding.weeksText.isVisible = false
                            requireContext().showCustomToast("No Data Found", R.layout.error_toast)
                        }
                    }
                }
        }
    }

    @SuppressLint("MissingInflatedId", "RestrictedApi", "ResourceAsColor")
    private fun showUseSearchBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_user_search, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)
        dialog.show()

        val searchView = view.findViewById<SearchView>(R.id.search_bar)
        val rvTeamAttendance = view.findViewById<RecyclerView>(R.id.rvTeamAttendance)

        // Customize search view appearance
        val searchAutoComplete = searchView.findViewById<SearchView.SearchAutoComplete>(
            androidx.appcompat.R.id.search_src_text
        )
        searchAutoComplete.setTextColor(Color.BLACK)
        searchAutoComplete.setHintTextColor(R.color.black)
        searchAutoComplete.setHint("Search by name or id...")
        searchAutoComplete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        // Make search bar focused and open keyboard
        searchView.isIconified = false
        searchView.requestFocus()

        teamUserAdapter = TeamUserAdapter(userWithUidList) { user ->
            dialog.dismiss()
            val userUid = user.uid

            binding.teamText.text = "${user.user.lastName} Attendance"

            checkAdapter = CheckAdapter(emptyList())
            binding.rvUserCheck.layoutManager = LinearLayoutManager(requireContext())
            binding.rvUserCheck.adapter = checkAdapter

            userRef
                .child(userUid)
                .child("checks")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val checksList = snapshot.children.mapNotNull { checkSnapshot ->
                            val check = checkSnapshot.getValue(Check::class.java)
                            if (check?.date.toString() == currentDateStr) check else null
                        }
                        if (checksList.isNotEmpty()) {
                            checkAdapter.updateChecks(checksList)

                            val weekOfMonth = Calendar.getInstance().get(Calendar.WEEK_OF_MONTH)
                            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                            val monthNameYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

                            binding.tvMonthYear.text = monthNameYear
                            binding.summaryText.text = "Summary of $monthName"
                            binding.weeksText.isVisible = true
                            binding.weeksText.text = "Week $weekOfMonth"
                        }
                    }
                }
            binding.filterMonth.setOnClickListener {
                filterByMonth(user.uid, dateFormatter)
            }
        }

        rvTeamAttendance.layoutManager = LinearLayoutManager(requireContext())
        rvTeamAttendance.adapter = teamUserAdapter

        fetchUsers(searchView)

    }

    private fun fetchUsers(searchView: SearchView) {
        userRef.get().addOnSuccessListener { snapshot ->
            val users = snapshot.children.mapNotNull { snap ->
                val user = snap.getValue(User::class.java)
                val uid = snap.key
                if (user?.role == "user" && uid != null) uid to user else null
            }

            if (users.isEmpty()) return@addOnSuccessListener

            userWithUidList.clear()
            var loadedCount = 0

            users.forEach { (uid, user) ->
                userWithUidList.add(UserWithUid(uid, user))
                loadedCount++
                // Update UI once all users are loaded
                if (loadedCount == users.size) {
                    teamUserAdapter.updateUsers(userWithUidList)

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean = true
                        override fun onQueryTextChange(newText: String?): Boolean {
                            teamUserAdapter.filterUsers(newText.orEmpty())
                            return true
                        }
                    })
                }
            }
        }.addOnFailureListener {
            // Handle error if users cannot be retrieved
            teamUserAdapter.updateUsers(emptyList())
        }
    }

}
