package com.example.project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.User
import com.example.project.databinding.ActivitySignupBinding
import com.example.project.function.function.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var back: ImageView
    private lateinit var binding: ActivitySignupBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseRef = FirebaseDatabase.getInstance().getReference("users")
        auth = FirebaseAuth.getInstance()

        back = findViewById(R.id.arrowLeft)

        back.setOnClickListener {
            finish()
        }

        binding.loginLink.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        binding.signupBtn.setOnClickListener {
            val firstName = binding.nameInput.text.toString().trim()
            val lastName = binding.lastNameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordTxt.text.toString()
            val confirmPassword = binding.confirmPasswordTxt.text.toString()
            val isCheckboxChecked = binding.privacyChekbox.isChecked
            val role = "user"

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (isValidEmail(email)) {
                    if (password == confirmPassword) {
                        if (!isCheckboxChecked) {
                            this.showCustomToast("Please accept the privacy policy", R.layout.error_toast)
                            return@setOnClickListener
                        }

                        val user = User(firstName, lastName, email, password, role)
                        insertUser(user)

                    } else {
                        this.showCustomToast("Passwords do not match", R.layout.error_toast)
                    }
                } else {
                    this.showCustomToast("Invalid Email", R.layout.error_toast)
                }
            } else {
                this.showCustomToast("Please fill all fields", R.layout.error_toast)
            }
        }
    }

    private fun insertUser(user: User) {
        auth.createUserWithEmailAndPassword(user.email?: "", user.password?: "")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        firebaseRef.child(uid).setValue(user)
                            .addOnCompleteListener {
                                this.showCustomToast("User registered successfully", R.layout.success_toast)
                                val intent = Intent(this, Login::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                this.showCustomToast("Failed to save user data", R.layout.error_toast)
                            }
                    } else {
                        this.showCustomToast("Registration failed: No user ID", R.layout.error_toast)
                    }
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Registration failed"
                    this.showCustomToast(errorMsg, R.layout.error_toast)
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
