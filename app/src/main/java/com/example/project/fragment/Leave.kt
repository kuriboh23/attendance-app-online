package com.example.project.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.R
import com.example.project.activities.ApplyLeave
import com.example.project.activities.HomeActivity
import com.example.project.data.Leave
import com.example.project.databinding.FragmentLeaveBinding
import com.example.project.fragment.list.LeaveAdapter
import com.example.project.function.function.showCustomToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class Leave : Fragment() {

    private lateinit var binding: FragmentLeaveBinding
    private lateinit var leaveAdapter: LeaveAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private val applyLeaveLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.bottom_sheet_pending, null)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setContentView(view)
            dialog.show()

            view.findViewById<MaterialButton>(R.id.home_btn).setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(requireContext(), HomeActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLeaveBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")
        val uid = auth.currentUser?.uid ?: return binding.root

        leaveAdapter = LeaveAdapter { leave ->
            showLeaveDetailsBottomSheet(leave)
        }

        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRequests.adapter = leaveAdapter

        fetchLeavesFromFirebase(uid)

        binding.addRequest.setOnClickListener {
            val intent = Intent(requireContext(), ApplyLeave::class.java)
            applyLeaveLauncher.launch(intent)
        }

        binding.tvLeaveFilter.setOnClickListener {
            showLeaveFilterBottomSheet(uid)
        }

        return binding.root
    }

    private fun fetchLeavesFromFirebase(uid: String) {
        userRef.child(uid).child("leaves").get()
            .addOnSuccessListener { snapshot ->
                val leaveList = snapshot.children.mapNotNull { it.getValue(Leave::class.java) }
                leaveAdapter.setData(leaveList)
                updateSummary(leaveList)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load leaves", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSummary(leaves: List<Leave>) {
        binding.tvTotalLeaves.text = leaves.size.toString()
        binding.tvCasualLeaves.text = leaves.count { it.type == "Casual" }.toString()
        binding.tvSickLeaves.text = leaves.count { it.type == "Sick" }.toString()
        binding.tvPendingApprovals.text = leaves.count { it.status == "Pending" }.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showLeaveDetailsBottomSheet(leave: Leave) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_leave_details, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvLeaveDetailDate).text = getDayAbbreviation(leave.date)
        view.findViewById<TextView>(R.id.tvLeaveDetailType).text = leave.type
        view.findViewById<TextView>(R.id.tvLeaveDetailStatus).apply {
            text = leave.status
            when (leave.status.lowercase()) {
                "approved" -> setBackgroundResource(R.drawable.status_approved)
                "rejected" -> setBackgroundResource(R.drawable.status_rejected)
                "pending" -> setBackgroundResource(R.drawable.status_pending)
            }
        }

        view.findViewById<TextView>(R.id.tvLeaveDetailNote).text = leave.note

        val attachmentButton = view.findViewById<MaterialButton>(R.id.tvLeaveDetailAttachment)
        if (!leave.attachmentUrl.isNullOrEmpty()) {
            attachmentButton.text = "View Attachment"
            attachmentButton.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(leave.attachmentUrl)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No app found to open attachment", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            attachmentButton.text = "No Attachment"
        }

        dialog.show()
    }

    private fun showLeaveFilterBottomSheet(uid: String) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_leave_filter, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)
        dialog.show()

        val statusGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.tvLeaveStatusGroup)
        val typeGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.tvLeaveTypeGroup)
        val resetBtn = view.findViewById<MaterialButton>(R.id.tvLeaveReset)
        val applyBtn = view.findViewById<MaterialButton>(R.id.tvLeaveApply)

        resetBtn.setOnClickListener {
            statusGroup.clearChecked()
            typeGroup.clearChecked()
        }

        applyBtn.setOnClickListener {
            val selectedStatus = when (statusGroup.checkedButtonId) {
                R.id.tvLeavePending -> "Pending"
                R.id.tvLeaveApproved -> "Approved"
                R.id.tvLeaveRejected -> "Rejected"
                else -> null
            }

            val selectedType = when (typeGroup.checkedButtonId) {
                R.id.tvLeaveCasual -> "Casual"
                R.id.tvLeaveSick -> "Sick"
                else -> null
            }

            userRef.child(uid).child("leaves").get()
                .addOnSuccessListener { snapshot ->
                    val filtered = snapshot.children.mapNotNull { it.getValue(Leave::class.java) }
                        .filter { leave -> leave.status == selectedStatus && leave.type == selectedType }
                    leaveAdapter.setData(filtered)
                }
                .addOnFailureListener {
                    requireContext().showCustomToast("Failed to filter leaves", R.layout.error_toast)
                }

            dialog.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDayAbbreviation(dateString: String?): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
            val date = LocalDate.parse(dateString, inputFormatter)
            val dayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
            date.format(dayFormatter)
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}
