package com.example.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.data.Notif
import com.example.project.databinding.FragmentNotificationBinding
import com.example.project.fragment.adapters.NotifAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Notification : Fragment() {

    private lateinit var binding: FragmentNotificationBinding
    private lateinit var adapter: NotifAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var notifRef: DatabaseReference
    private val notifList = mutableListOf<Notif>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationBinding.inflate(inflater, container, false)

        initFirebase()
        fetchNotifications()

        return binding.root
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        notifRef = FirebaseDatabase.getInstance().getReference("notifications")
    }

    private fun fetchNotifications() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) return

        notifRef.child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    notifList.clear()
                    for (notifSnap in snapshot.children) {
                        val notif = notifSnap.getValue(Notif::class.java)
                        notif?.let { notifList.add(it) }
                    }
                    notifList.sortByDescending { it.timestamp }
                    setupRecyclerView()
                }
            }
    }

    private fun setupRecyclerView() {
        adapter = NotifAdapter(notifList)
        binding.rvNotification.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotification.adapter = adapter
    }

}
