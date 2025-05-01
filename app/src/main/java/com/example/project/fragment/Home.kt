package com.example.project.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.project.CheckCountPrefs
import com.example.project.CheckInPrefs
import com.example.project.R
import com.example.project.UserPrefs
import com.example.project.activities.MainActivity
import com.example.project.activities.ScanActivity
import com.example.project.data.Check
import com.example.project.data.TimeManager
import com.example.project.databinding.FragmentHomeBinding
import com.example.project.function.function.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Home : Fragment() {

    private val requiredQrText = "Yakuza"
    private var checkInTime: Long? = null
    private var isCheckedIn = false

    private val timeFormatterHM = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private val now = System.currentTimeMillis()
    private val dayName = dayNameFormat.format(Date())
    private val day = dayFormat.format(Date(now))
    private val month = monthFormat.format(Date(now))
    private val year = yearFormat.format(Date(now))

    private val handler = Handler()
    private lateinit var timeRunnable: Runnable

    private lateinit var binding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startQrScan()
            else requireContext().showCustomToast("Camera permission is required to scan", R.layout.error_toast)
        }

    private val scanActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra("SCAN_RESULT")
                scannedText?.let { handleQrResult(it) }
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.loadingOverlay.visibility = View.VISIBLE

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

        binding.tvDate.text = "$month $day, $year - $dayName"

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startQrScan()
        }

        auth.currentUser?.uid?.let { uid ->
            val savedState = CheckInPrefs.loadCheckInState(requireContext())
            if (savedState.checkInStr != null) binding.tvCheckInTime.text = savedState.checkInStr
            checkInTime = if (savedState.isCheckedIn) savedState.checkInMillis else null
            isCheckedIn = savedState.isCheckedIn
            if (savedState.checkOutStr != null) binding.tvCheckOutTime.text = savedState.checkOutStr
            if (savedState.duration != null) binding.tvTotalHours.text = savedState.duration

            updateButtonStyle(isCheckedIn)
            updateAttendance(uid)

            userRef.child(uid).child("lastName").get()
                .addOnSuccessListener {
                    val lastName = it.value.toString()
                    binding.tvGreeting.text = "Hey, $lastName"
                    binding.loadingOverlay.visibility = View.GONE
                }
        }

        binding.ivProfile.setOnClickListener {
            signOut()
        }

        return binding.root
    }
    private fun signOut() {
        UserPrefs.savedIsLoggedIn(requireContext(), false)

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onStart() {
        super.onStart()
        startUpdatingTime()
    }

    private fun startQrScan() {
        binding.checkInBtn.setOnClickListener {
            val intent = Intent(requireContext(), ScanActivity::class.java)
            scanActivityLauncher.launch(intent)
        }
    }

    private fun handleQrResult(scannedText: String) {
        if (scannedText == requiredQrText) {
            val now = System.currentTimeMillis()
            if (!isCheckedIn) {
                // Check-in
                checkInTime = now
                val checkInString = timeFormatterHM.format(Date(now))
                binding.tvCheckInTime.text = checkInString
                binding.checkBtnName.text = "Check Out"
                isCheckedIn = true

                CheckInPrefs.saveCheckIn(requireContext(), true, now, checkInString)
                CheckInPrefs.saveCheckOut(requireContext(),true, "00:00", "00:00")


                binding.tvCheckOutTime.text = "00:00"
                binding.tvTotalHours.text = "00:00"
                updateButtonStyle(true)

            } else {
                // Check-out
                checkOutFunction()
            }
        } else {
            requireContext().showCustomToast("Invalid QR Code", R.layout.error_toast)
        }
    }

    private fun checkOutFunction() {
        val now = System.currentTimeMillis()
        val checkOutString = timeFormatterHM.format(Date(now))
        binding.tvCheckOutTime.text = checkOutString
        binding.checkBtnName.text = "Check In"

        val durationMillis = now - (checkInTime ?: 0L)
        val durationInSeconds = durationMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val durationStr = "${hours}h ${minutes}m"
        binding.tvTotalHours.text = durationStr

        auth.currentUser?.uid?.let { uid ->
            CheckInPrefs.saveCheckOut(requireContext(),false, checkOutString, durationStr)

            val savedState = CheckInPrefs.loadCheckInState(requireContext())
            val checkInTimeStr = savedState.checkInStr ?: "00:00"
            val currentDateStr = dateFormatter.format(Date(now))

            insertCheckToDatabase(uid, currentDateStr, checkInTimeStr, checkOutString, durationInSeconds)
        }

        checkInTime = null
        isCheckedIn = false
        updateButtonStyle(false)

    }

    private fun insertCheckToDatabase(
        uid: String,
        date: String,
        checkInTime: String,
        checkOutTime: String,
        durationInSecond: Long
    ) {
        val check = Check(date, checkInTime, checkOutTime, durationInSecond)
        userRef.child(uid).child("checks").push().setValue(check)

        CheckCountPrefs.incrementCheckoutCount(requireContext())
        val checkoutCount = CheckCountPrefs.getCheckoutCount(requireContext())

        if (checkoutCount == 2) {
            CheckCountPrefs.saveTimeDuration(requireContext(), durationInSecond)
            CheckCountPrefs.resetCheckoutCount(requireContext())
            val finalDuration = CheckCountPrefs.getTimeDuration(requireContext())

            val workTime = (finalDuration / 3600).toInt()
            val calendar = Calendar.getInstance()
            calendar.time = dateFormatter.parse(date) ?: Date()
            val dayName = dayNameFormat.format(calendar.time)

            val isWeekend = dayName == "Saturday" || dayName == "Sunday"
            val finalWorkTime = if (isWeekend) 0 else workTime
            val extraTime = if (isWeekend) workTime else 0
            val absent = finalWorkTime == 0
            val late = if(!absent)(finalWorkTime < 8) else false

            val timeManager = TimeManager(
                date = date,
                workTime = finalWorkTime,
                extraTime = extraTime,
                durationInSecond = finalDuration,
                late = late,
                absent = absent
            )

            userRef.child(uid).child("timeManager").push().setValue(timeManager)
            requireContext().showCustomToast("TimeManager pushed successfully", R.layout.success_toast)
            CheckCountPrefs.resetTimeDuration(requireContext())
        } else {
            CheckCountPrefs.saveTimeDuration(requireContext(), durationInSecond)
            requireContext().showCustomToast("Checkout saved ($checkoutCount/2)", R.layout.info_toast)
        }
    }

    private fun updateButtonStyle(checkedIn: Boolean) {
        if (checkedIn) {
            binding.checkBtnName.text = "Check Out"
            binding.checkInBtn.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.checkOut))
            binding.cardCheckInButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.checkOutLight))
        } else {
            binding.checkBtnName.text = "Check In"
            binding.checkInBtn.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.mainColor))
            binding.cardCheckInButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondColor))
        }
    }

    private fun startUpdatingTime() {
        timeRunnable = object : Runnable {
            override fun run() {
                updateCurrentTimeText()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timeRunnable)
    }

    private fun updateCurrentTimeText() {
        val now = System.currentTimeMillis()
        val currentTimeStr = timeFormatter.format(Date(now))
        binding.tvTime.text = currentTimeStr
    }

    private fun updateAttendance(uid: String) {
        userRef.child(uid).child("timeManager").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val timeManagerList = snapshot.children.mapNotNull { dataSnapshot ->
                        val timeMG = dataSnapshot.getValue(TimeManager::class.java)
                        if (timeMG?.date?.startsWith(currentMonth) == true) timeMG else null
                    }

                    if (timeManagerList.isNotEmpty()) {
                        val absentCount = timeManagerList.count { it.absent == true }
                        val lateCount = timeManagerList.count { it.late == true }

                        binding.tvAbsent.text = absentCount.toString()
                        binding.tvLate.text = lateCount.toString()
                    }
                }
            }
    }

}
