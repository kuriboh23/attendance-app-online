package com.example.project.activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.Leave
import com.example.project.databinding.ActivityApplyLeaveBinding
import com.example.project.function.function.showCustomToast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ApplyLeave : AppCompatActivity() {

    private lateinit var binding: ActivityApplyLeaveBinding
    private lateinit var auth: FirebaseAuth

    private var selectedImageBitmap: Bitmap? = null
    private var attachmentUrl: String? = null

    private lateinit var type: String
    private val now = System.currentTimeMillis()

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Convert Uri to Bitmap using MediaStore
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                selectedImageBitmap = bitmap
                val name = uri.lastPathSegment ?: "Selected file"
                binding.tvAttachmentName.text = name
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyLeaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        type = when (binding.leaveTypeGroup.checkedButtonId) {
            com.example.project.R.id.btn_casual -> "Casual"
            com.example.project.R.id.btn_sick -> "Sick"
            else -> ""
        }

        binding.startDate.setOnClickListener { showDatePicker(true) }
        binding.endDate.setOnClickListener { showDatePicker(false) }

        binding.tvbackArrow.setOnClickListener { finish() }

        binding.leaveTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                type = when (checkedId) {
                    com.example.project.R.id.btn_casual -> "Casual"
                    com.example.project.R.id.btn_sick -> "Sick"
                    else -> ""
                }
            }
        }

        binding.btnAttachFile.setOnClickListener {
            pickFileLauncher.launch("image/*") // Select images only
        }

        binding.applyBtn.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                if (selectedImageBitmap != null) {
                    uploadToCloudinary(uid)
                } else {
                    submitLeaveRequest(uid, null)
                }
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a Date")
            .build()
        datePicker.show(supportFragmentManager, "DATE_PICKER")
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate))
            if (isStartDate) {
                binding.startDate.setText(formatted)
            } else {
                binding.endDate.setText(formatted)
            }
        }
    }

    private fun uploadToCloudinary(uid: String) {
        val bitmap = selectedImageBitmap ?: return
        val file = File.createTempFile("upload_", ".jpg", cacheDir)

        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        val thread = Thread {
            try {
                val cloudName = "dyx5wsual"
                val uploadPreset = "android_upload"
                val boundary = "Boundary-${System.currentTimeMillis()}"

                val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                val writer = DataOutputStream(connection.outputStream)
                val lineEnd = "\r\n"

                // Add upload_preset
                writer.writeBytes("--$boundary$lineEnd")
                writer.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"$lineEnd$lineEnd")
                writer.writeBytes(uploadPreset)
                writer.writeBytes(lineEnd)

                // Add file
                writer.writeBytes("--$boundary$lineEnd")
                writer.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"$lineEnd")
                writer.writeBytes("Content-Type: image/jpeg$lineEnd$lineEnd")

                val inputStream = FileInputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    writer.write(buffer, 0, bytesRead)
                }
                inputStream.close()

                writer.writeBytes(lineEnd)
                writer.writeBytes("--$boundary--$lineEnd")
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (responseStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    responseStream.close()

                    val json = JSONObject(response.toString())
                    val url = json.getString("secure_url")

                    runOnUiThread {
                        attachmentUrl = url
                        submitLeaveRequest(uid, attachmentUrl)
                    }
                } else {
                    runOnUiThread {
                        showCustomToast("Cloudinary upload failed: ${connection.responseMessage}", R.layout.error_toast)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {

                    showCustomToast("Cloudinary upload failed: ${e.message}", R.layout.error_toast)
                }
            }
        }
        thread.start()
    }



    private fun submitLeaveRequest(uid: String, attachmentUrl: String?) {
        val startDate = binding.startDate.text.toString()
        val endDate = binding.endDate.text.toString()
        val note = binding.tvNote.text.toString()

        if (startDate.isEmpty() || endDate.isEmpty() || type.isEmpty() || note.isEmpty()) {
            showCustomToast("Please fill in all fields", R.layout.error_toast)
            return
        }

        val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val leave = Leave(currentDateStr, startDate, endDate, type, note, "Pending", attachmentUrl)

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("leaves")
            .push()
            .setValue(leave)
            .addOnSuccessListener {
                // Prepare the result data
                val resultIntent = Intent()
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener {
                showCustomToast("Failed to submit leave request", R.layout.error_toast)
            }
    }
}
