package com.example.project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.UserPrefs
import com.example.project.databinding.ActivityLoginBinding
import com.example.project.function.function.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.arrowLeft.setOnClickListener {
            finish()
        }

        binding.signUpLink.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        binding.signInBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.txPassword.text.toString()

            if (!isValidEmail(email)) {
                this.showCustomToast("Invalid Email", R.layout.error_toast)
                binding.emailInput.text?.clear()
                binding.emailInput.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                this.showCustomToast("Incorrect password", R.layout.error_toast)
                return@setOnClickListener
            }

            // Proceed to login
            firebaseLoginIn(email, password)
        }
    }

    private fun firebaseLoginIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        fetchUserRoleAndNavigate(uid)
                    } else {
                        this.showCustomToast("Login failed: User ID not found", R.layout.error_toast)
                    }
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Login failed"
                    this.showCustomToast("Login failed: $errorMsg", R.layout.error_toast)
                }
            }
    }

    private fun fetchUserRoleAndNavigate(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.child("role").get().addOnSuccessListener { dataSnapshot ->
            val role = dataSnapshot.value.toString()
            UserPrefs.saveUserRole(this, role)

            // Save login state after successful login
            val isLoggedIn = binding.privacyChekbox.isChecked
            UserPrefs.savedIsLoggedIn(this, isLoggedIn)

            println("Role: $role")

            val targetActivity = if (role == "user") HomeActivity::class.java else AdminHomeActivity::class.java
            val intent = Intent(this, targetActivity)
            this.showCustomToast("Login Successful", R.layout.success_toast)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            this.showCustomToast("Failed to retrieve role", R.layout.error_toast)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
