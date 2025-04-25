package com.example.project.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.R
import com.example.project.activities.MainActivity
import com.example.project.activities.Userdetails
import com.example.project.data.User
import com.example.project.databinding.FragmentAdminHomeBinding
import com.example.project.fragment.adapters.UserAdapter
import com.example.project.fragment.adapters.UserWithStatus
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        // Initialize UserAdapter with click callback
        userAdapter = UserAdapter(userWithStatusList) { uid ->
            val intent = Intent(requireContext(), Userdetails::class.java).apply {
                putExtra("userId", uid)  // Pass UID to the Userdetails activity
            }
            startActivity(intent)
        }
        binding.rvUsers.adapter = userAdapter
        println("1 User with status: $userWithStatusList")

        // Fetch and display users from Firebase
        fetchUsersFromFirebase()

        return binding.root
    }

    private fun fetchUsersFromFirebase() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userWithStatusList.clear()  // Clear list before adding new data

                snapshot.children.forEach { userSnapshot ->
                    val user = userSnapshot.getValue(User::class.java) // Get user data
                    val uid = userSnapshot.key  // Get the UID

                    // Process only users with role "user"
                    if (user?.role == "user") {
                        FirebaseDatabase.getInstance().getReference("checks/$uid")
                            .child(currentDate)
                            .get()
                            .addOnSuccessListener { attendanceSnapshot ->
                                val status = if (attendanceSnapshot.exists()) {
                                    val checkInTime = attendanceSnapshot.child("checkInTime").getValue(String::class.java)
                                    if (checkInTime != null && timeToIntPair(checkInTime).first >= 9) "Late" else "Present"
                                } else {
                                    "Absent"
                                }

                                userWithStatusList.add(UserWithStatus(uid ?: "", user, status))
                                println("2 User with status: $userWithStatusList")

                                // Update the adapter on the main thread
                                requireActivity().runOnUiThread {
                                    if (userWithStatusList.size == snapshot.childrenCount.toInt()) {
                                        userAdapter.updateUsers(userWithStatusList)
                                        println("3 User with status: $userWithStatusList")
                                    }
                                }
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun timeToIntPair(timeString: String): Pair<Int, Int> {
        val parts = timeString.split(" ")
        val timeParts = parts[0].split(":")
        var hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val isPM = parts[1].equals("PM", ignoreCase = true)

        if (isPM && hours != 12) {
            hours += 12
        } else if (!isPM && hours == 12) {
            hours = 0
        }

        return Pair(hours, minutes)
    }
}

