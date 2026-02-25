package com.copdhealthtracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.databinding.ActivityLoginBinding
import com.copdhealthtracker.utils.AppApplication

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginError.visibility = View.GONE
        binding.btnSignIn.setOnClickListener { doSignIn() }
        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun doSignIn() {
        val email = binding.email.text?.toString()?.trim() ?: ""
        val password = binding.password.text?.toString() ?: ""
        if (email.isEmpty() || password.isEmpty()) {
            binding.loginError.text = "Enter email and password"
            binding.loginError.visibility = View.VISIBLE
            return
        }
        binding.loginError.visibility = View.GONE
        binding.btnSignIn.isEnabled = false
        (application as AppApplication).copdAuth.signIn(email, password) { result ->
            binding.btnSignIn.isEnabled = true
            result.fold(
                onSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onFailure = { e ->
                    binding.loginError.text = e.message ?: "Sign in failed"
                    binding.loginError.visibility = View.VISIBLE
                }
            )
        }
    }
}
