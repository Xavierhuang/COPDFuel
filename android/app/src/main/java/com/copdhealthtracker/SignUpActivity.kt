package com.copdhealthtracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.databinding.ActivitySignUpBinding
import com.copdhealthtracker.utils.AppApplication

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var needsConfirmation = false
    private var signUpEmail = ""
    private var signUpPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.signupError.visibility = View.GONE
        binding.confirmCodeContainer.visibility = View.GONE
        binding.btnUseDifferentEmail.visibility = View.GONE

        binding.btnSubmit.setOnClickListener { submit() }
        binding.btnUseDifferentEmail.setOnClickListener {
            needsConfirmation = false
            signUpEmail = ""
            signUpPassword = ""
            binding.confirmCodeContainer.visibility = View.GONE
            binding.btnUseDifferentEmail.visibility = View.GONE
            binding.nameLayout.visibility = View.VISIBLE
            binding.emailLayout.visibility = View.VISIBLE
            binding.passwordLayout.visibility = View.VISIBLE
            binding.confirmPasswordLayout.visibility = View.VISIBLE
            binding.signupSubtitle.text = getString(R.string.signup_subtitle_create)
            binding.btnSubmit.text = getString(R.string.signup_button_sign_up)
            binding.code.text?.clear()
            binding.signupError.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun submit() {
        binding.signupError.visibility = View.GONE
        binding.signupError.text = ""

        if (needsConfirmation) {
            confirmCode()
            return
        }

        val name = binding.name.text?.toString()?.trim().orEmpty()
        val email = binding.email.text?.toString()?.trim() ?: ""
        val password = binding.password.text?.toString() ?: ""
        val confirm = binding.confirmPassword.text?.toString() ?: ""

        when {
            email.isEmpty() -> binding.signupError.text = "Email is required."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.signupError.text = "Email is not valid."
            password.isEmpty() -> binding.signupError.text = "Password is required."
            password.length < 8 -> binding.signupError.text = "Password must be at least 8 characters."
            password != confirm -> binding.signupError.text = "Passwords do not match."
            else -> doSignUp(email, password, name.ifEmpty { null })
        }
        if (binding.signupError.text.isNotEmpty()) binding.signupError.visibility = View.VISIBLE
    }

    private fun doSignUp(email: String, password: String, name: String?) {
        binding.btnSubmit.isEnabled = false
        (application as AppApplication).copdAuth.signUp(email, password, name) { result ->
            binding.btnSubmit.isEnabled = true
            result.fold(
                onSuccess = { requiresConfirmation ->
                    signUpEmail = email
                    signUpPassword = password
                    if (requiresConfirmation) {
                        needsConfirmation = true
                        binding.nameLayout.visibility = View.GONE
                        binding.emailLayout.visibility = View.GONE
                        binding.passwordLayout.visibility = View.GONE
                        binding.confirmPasswordLayout.visibility = View.GONE
                        binding.confirmCodeContainer.visibility = View.VISIBLE
                        binding.btnUseDifferentEmail.visibility = View.VISIBLE
                        binding.signupSubtitle.text = getString(R.string.signup_subtitle_confirm, email)
                        binding.btnSubmit.text = getString(R.string.signup_button_confirm)
                    } else {
                        signInAndFinish(email, password)
                    }
                },
                onFailure = { e ->
                    binding.signupError.text = e.message ?: "Sign up failed"
                    binding.signupError.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun confirmCode() {
        val code = binding.code.text?.toString()?.trim() ?: ""
        when {
            code.isEmpty() -> {
                binding.signupError.text = "Verification code is required."
                binding.signupError.visibility = View.VISIBLE
            }
            code.length != 6 || !code.all { it.isDigit() } -> {
                binding.signupError.text = "Verification code must be 6 digits."
                binding.signupError.visibility = View.VISIBLE
            }
            else -> {
                binding.btnSubmit.isEnabled = false
                (application as AppApplication).copdAuth.confirmSignUp(signUpEmail, code) { result ->
                    binding.btnSubmit.isEnabled = true
                    result.fold(
                        onSuccess = { signInAndFinish(signUpEmail, signUpPassword) },
                        onFailure = { e ->
                            binding.signupError.text = e.message ?: "Confirmation failed"
                            binding.signupError.visibility = View.VISIBLE
                        }
                    )
                }
            }
        }
    }

    private fun signInAndFinish(email: String, password: String) {
        (application as AppApplication).copdAuth.signIn(email, password) { result ->
            result.fold(
                onSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onFailure = { e ->
                    binding.signupError.text = e.message ?: "Sign in failed"
                    binding.signupError.visibility = View.VISIBLE
                }
            )
        }
    }
}
