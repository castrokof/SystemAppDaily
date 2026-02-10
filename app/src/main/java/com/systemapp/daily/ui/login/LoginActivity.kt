package com.systemapp.daily.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.systemapp.daily.databinding.ActivityLoginBinding
import com.systemapp.daily.ui.home.HomeActivity
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Si ya está logueado, ir directo al Home
        if (sessionManager.isLoggedIn) {
            navigateToHome()
            return
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            doLogin()
        }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doLogin()
                true
            } else false
        }
    }

    private fun doLogin() {
        val usuario = binding.etUsuario.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        viewModel.login(usuario, password)
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true

                    val user = result.data
                    if (user.apiToken != null) {
                        sessionManager.saveLoginData(
                            apiToken = user.apiToken,
                            userId = user.id,
                            nombre = user.nombre,
                            usuario = user.usuario,
                            email = user.email,
                            empresa = user.empresa
                        )
                        Toast.makeText(this, "Bienvenido, ${user.nombre}", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    } else {
                        Toast.makeText(this, "Error: el servidor no devolvió un token", Toast.LENGTH_SHORT).show()
                    }
                }
                is NetworkResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
