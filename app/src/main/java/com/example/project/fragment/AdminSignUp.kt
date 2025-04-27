package com.example.project.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.project.R
import com.example.project.activities.AdminHomeActivity
import com.example.project.data.User
import com.example.project.databinding.FragmentAdminSignUpBinding
import com.example.project.function.function.showCustomToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AdminSignUp : Fragment() {

    lateinit var binding: FragmentAdminSignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminSignUpBinding.inflate(inflater, container, false)

        firebaseRef = FirebaseDatabase.getInstance().getReference("users")
        auth = FirebaseAuth.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                            requireContext().showCustomToast(
                                "Please accept the privacy policy",
                                R.layout.error_toast
                            )
                            return@setOnClickListener
                        }

                        val user = User(firstName, lastName, email, password, role)
                        insertUser(user)

                    } else {
                        requireContext().showCustomToast(
                            "Passwords do not match",
                            R.layout.error_toast
                        )
                    }
                } else {
                    requireContext().showCustomToast("Invalid Email", R.layout.error_toast)
                }
            } else {
                requireContext().showCustomToast("Please fill all fields", R.layout.error_toast)
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun insertUser(user: User) {
        auth.createUserWithEmailAndPassword(user.email ?: "", user.password ?: "")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        firebaseRef.child(uid).setValue(user)
                            .addOnCompleteListener {
                                user.lastName?.let { it0 -> showSuccessDialog(it0) }
                                clearInputFields()
                            }
                            .addOnFailureListener {
                                requireContext().showCustomToast(
                                    "Failed to save user data",
                                    R.layout.error_toast
                                )
                            }
                    } else {
                        requireContext().showCustomToast(
                            "Registration failed: No user ID",
                            R.layout.error_toast
                        )
                    }
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Registration failed"
                    requireContext().showCustomToast(errorMsg, R.layout.error_toast)
                }
            }
    }

    @SuppressLint("MissingInflatedId")
    private fun showSuccessDialog(lastName: String) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_register, null)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(view)

        val userTitle = view.findViewById<TextView>(R.id.userTitle)
        userTitle.text = "$lastName registered successfully"

        view.findViewById<MaterialButton>(R.id.home_btn).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), AdminHomeActivity::class.java))
            requireActivity().finish()
        }

        dialog.show()
    }

    private fun clearInputFields() {
        binding.nameInput.text?.clear()
        binding.lastNameInput.text?.clear()
        binding.emailInput.text?.clear()
        binding.passwordTxt.text?.clear()
        binding.confirmPasswordTxt.text?.clear()
        binding.confirmPasswordInput.isActivated = false
        binding.privacyChekbox.isChecked = false
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}