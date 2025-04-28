package com.example.project.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.Check
import com.example.project.data.User
import com.example.project.databinding.FragmentAdminLeaveBinding
import com.example.project.fragment.adapters.LeaveAdapter
import com.example.project.fragment.adapters.LeaveWithUser
import com.example.project.fragment.adapters.TeamUserAdapter
import com.example.project.fragment.adapters.UserWithUid
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.project.data.Leave
import com.example.project.data.Notif
import com.example.project.fragment.adapters.CheckAdapter
import com.example.project.function.function.showCustomToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AdminLeave : Fragment() {
    private lateinit var binding: FragmentAdminLeaveBinding
    lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    lateinit var leaveAdapter: LeaveAdapter

    private var allLeaves = mutableListOf<LeaveWithUser>() // Store unfiltered leaves
    private var currentFilterType: String? =
        null // Store the current type filter ("All", "Casual", "Sick")

    private val userWithUidList = mutableListOf<UserWithUid>()
    lateinit var leaveWithUid: Pair<String, Leave>
    private lateinit var teamUserAdapter: TeamUserAdapter
    private var currentStatusFilter: String? = null

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminLeaveBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.get().addOnSuccessListener { snapshot ->
            val users = snapshot.children.mapNotNull { snap ->
                val user = snap.getValue(User::class.java)
                val uid = snap.key
                if (user?.role == "user" && uid != null) uid to user else null
            }

            if (users.isEmpty()) return@addOnSuccessListener

            allLeaves.clear()

            var loadedUsers = 0
            val totalUsers = users.size

            users.forEach { (uid, user) ->
                userRef.child(uid).child("leaves").get().addOnSuccessListener { leavesSnapshot ->
                    if (leavesSnapshot.exists()) {
                        leavesSnapshot.children.forEach { leaveSnap ->
                            val leave = leaveSnap.getValue(Leave::class.java)
                            if (leave != null) {
                                allLeaves.add(
                                    LeaveWithUser(leaveSnap.key ?: "", uid, user, leave)
                                )
                            }
                        }
                    }
                    loadedUsers++
                    if (loadedUsers == totalUsers) {
                        setupRecyclerView()
                        binding.tvTotalLeaves.text = allLeaves.size.toString()
                        val pendingApprovals =
                            allLeaves.filter { it.leave.status == "Pending" }.size
                        binding.tvPendingApprovals.text = pendingApprovals.toString()
                        val casualLeaves = allLeaves.filter { it.leave.type == "Casual" }.size
                        binding.tvCasualLeaves.text = casualLeaves.toString()
                        val sickLeaves = allLeaves.filter { it.leave.type == "Sick" }.size
                        binding.tvSickLeaves.text = sickLeaves.toString()
                    }
                }
            }
        }
        val buttonAll = binding.btnAll
        val buttonCasual = binding.btnCasual
        val buttonSick = binding.btnSick

        // Set click listeners for each button
        buttonAll.setOnClickListener {
            setButtonState(buttonAll)
            currentFilterType = null // No type filter (show all)
            updateAdapterWithFilters()
        }
        buttonCasual.setOnClickListener {
            setButtonState(buttonCasual)
            currentFilterType = "Casual"
            updateAdapterWithFilters()
        }
        buttonSick.setOnClickListener {
            setButtonState(buttonSick)
            currentFilterType = "Sick"
            updateAdapterWithFilters()
        }

        binding.leaveFilter.setOnClickListener {
            showLeaveFilterBottomSheet()
        }
        binding.searchUser.setOnClickListener {
            showUseSearchBottomSheet()
        }

        // Set "ALL" as the default highlighted button
        setButtonState(buttonAll)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        leaveAdapter = LeaveAdapter(allLeaves) { leaveWithUser ->
            showLeaveDetailsBottomSheet(leaveWithUser)
        }

        binding.rvLeaves.adapter = leaveAdapter
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged", "SetTextI18n")
    private fun showLeaveDetailsBottomSheet(leaveList: LeaveWithUser) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_admin_leave_details, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)

        val tvLeaveDate = view.findViewById<TextView>(R.id.tvLeaveDate)
        val ivUser = view.findViewById<ImageView>(R.id.ivUser)
        val tvFullName = view.findViewById<TextView>(R.id.tvFullName)
        val tvStartDate = view.findViewById<TextView>(R.id.tvStartDate)
        val tvEndDate = view.findViewById<TextView>(R.id.tvEndDate)
        val tvType = view.findViewById<TextView>(R.id.tvType)
        val tvNote = view.findViewById<TextView>(R.id.tvNote)
        val btnAttachment = view.findViewById<MaterialButton>(R.id.btnAttachment)
        val btnReject = view.findViewById<MaterialButton>(R.id.btnReject)
        val btnApprove = view.findViewById<MaterialButton>(R.id.btnApprove)
        view.findViewById<TextView>(R.id.tvLeaveDetailStatus).apply {
            text = leaveList.leave.status
            when (leaveList.leave.status.lowercase()) {
                "approved" -> setBackgroundResource(R.drawable.status_approved)
                "rejected" -> setBackgroundResource(R.drawable.status_rejected)
                "pending" -> setBackgroundResource(R.drawable.status_pending)
            }
        }

        tvFullName.text = "${leaveList.user.lastName} ${leaveList.user.name}"

        tvLeaveDate.text = getDayAbbreviation(leaveList.leave.date)
        tvStartDate.text = getDayAbbreviation(leaveList.leave.startDate)
        tvEndDate.text = getDayAbbreviation(leaveList.leave.endDate)
        tvType.text = leaveList.leave.type
        tvNote.text = leaveList.leave.note

        if (!leaveList.leave.attachmentUrl.isNullOrEmpty()) {
            btnAttachment.text = "View Attachment"
            btnAttachment.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(leaveList.leave.attachmentUrl)
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(), "No app found to open attachment", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            btnAttachment.text = "No Attachment"
        }

        if (leaveList.leave.status != "Pending") {
            btnReject.visibility = View.GONE
            btnApprove.visibility = View.GONE
        } else {
            btnReject.setOnClickListener {
                showConfirmationDialog("Reject this leave request?", dialog) {
                    val leavePath = "users/${leaveList.uid}/leaves/${leaveList.leaveKey}/status"
                    FirebaseDatabase.getInstance().getReference(leavePath).setValue("Rejected")
                    leaveList.leave.status = "Rejected" // Update locally
                    leaveAdapter.notifyDataSetChanged()
                    sendNotification(leaveList.uid, "Leave Rejected", "Your leave request has been rejected.", "leave_rejected")
                    dialog.dismiss()
                }
            }

            btnApprove.setOnClickListener {
                showConfirmationDialog("Approve this leave request?", dialog) {
                    val leavePath = "users/${leaveList.uid}/leaves/${leaveList.leaveKey}/status"
                    FirebaseDatabase.getInstance().getReference(leavePath).setValue("Approved")
                    leaveList.leave.status = "Approved" // Update locally
                    leaveAdapter.notifyDataSetChanged()
                    sendNotification(leaveList.uid, "Leave Approved", "Your leave request has been approved.", "leave_approved")
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }


    // Helper function to show confirmation dialog
    private fun showConfirmationDialog(
        message: String, dialog: BottomSheetDialog, onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Confirmation").setMessage(message)
            .setIcon(R.drawable.error_red).setPositiveButton("Confirm") { _, _ ->
                onConfirm()
                requireContext().showCustomToast("Leave updated", R.layout.success_toast)
            }.setNegativeButton("Cancel") { _, _ ->
                requireContext().showCustomToast("Leave not updated", R.layout.error_toast)
                dialog.dismiss()
            }.show()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getDayAbbreviation(dateString: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
        val date = LocalDate.parse(dateString, inputFormatter)
        val dayFormatter =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH) // "EEE" gives "Fri"
        return date.format(dayFormatter)
    }

    private fun setButtonState(selectedButton: MaterialButton) {
        binding.btnAll.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.gray_light)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        binding.btnCasual.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.gray_light)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        binding.btnSick.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.gray_light)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        selectedButton.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.mainColor)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateAdapterWithFilters() {
        var filteredLeaves = allLeaves

        // Apply type filter (All, Casual, Sick)
        currentFilterType?.let { type ->
            filteredLeaves = filteredLeaves.filter { it.leave.type == type }.toMutableList()
        }

        // Apply status filter (Pending, Approved, Rejected)
        currentStatusFilter?.let { status ->
            filteredLeaves = filteredLeaves.filter { it.leave.status == status }.toMutableList()
        }
        setupRecyclerView()

        leaveAdapter.setData(filteredLeaves)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "InflateParams")
    private fun showLeaveFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_admin_leave_filter, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)
        dialog.show()

        val leaveStatusGroup =
            view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.leaveStatusGroup)
        val reset = view.findViewById<MaterialButton>(R.id.reset)
        val apply = view.findViewById<MaterialButton>(R.id.apply)

        reset.setOnClickListener {
            leaveStatusGroup.clearChecked()
            currentFilterType = null
            currentStatusFilter = null
            setButtonState(binding.btnAll)
            updateAdapterWithFilters()
        }

        apply.setOnClickListener {
            if (leaveStatusGroup.checkedButtonId != -1) {
                currentStatusFilter = when (leaveStatusGroup.checkedButtonId) {
                    R.id.leavePending -> "Pending"
                    R.id.leaveApproved -> "Approved"
                    R.id.leaveRejected -> "Rejected"
                    else -> null
                }
                updateAdapterWithFilters()
                dialog.dismiss()
            }
        }
    }

    private fun sendNotification(userId: String, title: String, message: String, type: String) {
        val notification = Notif(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        FirebaseDatabase.getInstance().reference
            .child("notifications")
            .child(userId)
            .push()
            .setValue(notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        searchView.findViewById<SearchView.SearchAutoComplete>(
            androidx.appcompat.R.id.search_src_text
        ).apply {
            setTextColor(Color.BLACK)
            setHintTextColor(R.color.black)
            setHint("Search by name...")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        // Make search bar focused and open keyboard
        searchView.isIconified = false
        searchView.requestFocus()

        teamUserAdapter = TeamUserAdapter(userWithUidList,"Leaves") { user ->
            dialog.dismiss()
            val userUid = user.uid

            allLeaves.clear()
            userRef.child(userUid).child("leaves").get().addOnSuccessListener { leavesSnapshot ->
                if (leavesSnapshot.exists()) {
                    leavesSnapshot.children.forEach { leaveSnap ->
                        val leave = leaveSnap.getValue(Leave::class.java)
                        if (leave != null) {
                            allLeaves.add(
                                LeaveWithUser(leaveSnap.key ?: "", userUid, user.user, leave)
                            )
                        }
                    }
                }
                setupRecyclerView()
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