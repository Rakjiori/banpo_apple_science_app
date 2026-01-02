package com.example.banpoapple.ui.login

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.banpoapple.BuildConfig
import com.example.banpoapple.auth.LoginViewModel
import com.example.banpoapple.auth.LoginViewModelFactory
import com.example.banpoapple.databinding.ActivityLoginBinding
import com.example.banpoapple.di.AppContainer
import com.example.banpoapple.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(AppContainer.authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            viewModel.login(
                binding.editUsername.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty()
            )
        }

        binding.buttonSignup.setOnClickListener {
            val signupUrl = "${BuildConfig.BASE_URL}signup"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(signupUrl)))
        }

        binding.buttonOpenWeb.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.BASE_URL)))
        }

        binding.editPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.buttonLogin.performClick()
                true
            } else {
                false
            }
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    binding.buttonLogin.isEnabled = !state.isLoading
                    binding.textError.isVisible = state.errorMessage != null
                    binding.textError.text = state.errorMessage

                    if (state.isLoggedIn) {
                        Toast.makeText(this@LoginActivity, "로그인되었습니다.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
