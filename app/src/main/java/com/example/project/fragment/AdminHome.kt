package com.example.project.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.R
import com.example.project.UserPrefs
import com.example.project.activities.MainActivity
import com.example.project.activities.Userdetails
import com.example.project.data.Check
import com.example.project.data.User
import com.example.project.databinding.FragmentAdminHomeBinding
import com.example.project.fragment.adapters.UserAdapter
import com.example.project.fragment.adapters.UserWithStatus
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminHome : Fragment() {

    lateinit var binding: FragmentAdminHomeBinding
    lateinit var auth: FirebaseAuth
    lateinit var userRef: DatabaseReference
    private lateinit var userAdapter: UserAdapter
    private val userWithStatusList = mutableListOf<UserWithStatus>()

    val adminDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        binding.tvAdminDate.text = adminDate
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        // Initialize UserAdapter with click callback
        userAdapter = UserAdapter(userWithStatusList) { uid ->
            val intent = Intent(requireContext(), Userdetails::class.java).apply {
                putExtra("userId", uid)  // Pass UID to the Userdetails activity
            }
            startActivity(intent)
        }
        binding.rvUsers.adapter = userAdapter

        binding.ivAdminProfile.setOnClickListener {
            signOut()
        }
        binding.teamFilter.setOnClickListener {
            showUserFilterBottomSheet()
        }

        // Fetch and display users from Firebase
        fetchUsersWithStatus()

        return binding.root
    }

    private fun fetchUsersWithStatus() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        userRef.get().addOnSuccessListener { snapshot ->
            val users = snapshot.children.mapNotNull { snap ->
                val user = snap.getValue(User::class.java)
                val uid = snap.key
                if (user?.role == "user" && uid != null) uid to user else null
            }

            if (users.isEmpty()) return@addOnSuccessListener

            userWithStatusList.clear()
            var loadedCount = 0

            users.forEach { (uid, user) ->
                FirebaseDatabase.getInstance().getReference("users")
                    .child(uid)
                    .child("checks")
                    .get()
                    .addOnSuccessListener { checksSnapshot ->
                        var latestCheckInTime: String? = null

                        // Find the latest check-in for the current date
                        checksSnapshot.children.forEach { check ->
                            val date = check.child("date").getValue(String::class.java)
                            if (date == currentDate) {
                                val checkInTime = check.child("checkInTime").getValue(String::class.java)
                                if (checkInTime != null) {
                                    if (latestCheckInTime == null || timeToIntPair(checkInTime).first > timeToIntPair(
                                            latestCheckInTime!!
                                        ).first) {
                                        latestCheckInTime = checkInTime
                                    }
                                }
                            }
                        }

                        // Determine the status based on the latest check-in time
                        val status = if (latestCheckInTime != null) {
                            val (hour, _) = timeToIntPair(latestCheckInTime!!)
                            when {
                                hour in 8..8 -> "Present"         // 08:00 - 08:59 => Present
                                hour in 9..11 -> "Late"            // 09:00 - 11:59 => Late
                                hour in 12..13 -> "Absent"             // 12:00 - 13:59 => Ignore
                                hour in 14..14 -> "Present"            // 14:00 - 14:59 => Present
                                hour >= 15 -> "Late"               // 15:00+ => Late
                                else -> "null"
                            }
                        } else {
                            "Absent"
                        }
                            userWithStatusList.add(UserWithStatus(uid, user, status))
                            loadedCount++

                        // Update UI once all users are loaded
                        if (loadedCount == users.size) {
                            updateUIWithUserStatus()
                        }
                    }
                    .addOnFailureListener {
                        // Handle error if the user's checks cannot be retrieved
                        userWithStatusList.add(UserWithStatus(uid, user, "Absent"))
                        loadedCount++
                        if (loadedCount == users.size) {
                            updateUIWithUserStatus()
                        }
                    }
            }
        }.addOnFailureListener {
            // Handle error if users cannot be retrieved
        }
    }

    private fun signOut() {
        UserPrefs.savedIsLoggedIn(requireContext(), false)

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIWithUserStatus() {
        userAdapter.updateUsers(userWithStatusList)

        binding.tvPresentCount.text = userWithStatusList.count { it.status == "Present" }.toString()
        binding.tvAbsentCount.text = userWithStatusList.count { it.status == "Absent" }.toString()
        binding.tvLateCount.text = userWithStatusList.count { it.status == "Late" }.toString()
    }

    @SuppressLint("MissingInflatedId", "InflateParams")
    private fun showUserFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_user_filter, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)
        dialog.show()

        val usersStatusGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.userStatusGroup)
        val reset = view.findViewById<MaterialButton>(R.id.btReset)
        val apply = view.findViewById<MaterialButton>(R.id.btApply)

        reset.setOnClickListener {
            usersStatusGroup.clearChecked()
        }

        apply.setOnClickListener {
            if (usersStatusGroup.checkedButtonId != -1) {
                val status = when (usersStatusGroup.checkedButtonId) {
                    R.id.usersPresent -> "Present"
                    R.id.usersAbsent -> "Absent"
                    R.id.usersLate -> "Late"
                    else -> return@setOnClickListener
                }

                val newUsersList = userWithStatusList.filter { it.status == status }
                userAdapter.updateUsers(newUsersList)

                dialog.dismiss()
            }
        }
    }

    fun timeToIntPair(timeString: String): Pair<Int, Int> {
        val parts = timeString.split(" ")
        val timeParts = parts[0].split(":")
        var hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val isPM = parts[1].equals("PM", ignoreCase = true)

        if (isPM && hours != 12) {
            hours += 12
        }

        return Pair(hours, minutes)
    }
}

