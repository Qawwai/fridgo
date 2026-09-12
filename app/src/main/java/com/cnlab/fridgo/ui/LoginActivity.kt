package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnlab.fridgo.data.SessionStore
import com.cnlab.fridgo.data.ShopRepository
import com.cnlab.fridgo.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/**
 * The app's entry gate. Authenticates against DummyJSON's user directory and,
 * on success, persists the user in [SessionStore] and continues into the app.
 * If a saved session already exists we skip straight through. A low-key "browse
 * as guest" link keeps the app usable if the network/login is unavailable.
 *
 * Demo credentials (emilys / emilyspass) are pre-filled so the flow is one tap.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Already signed in? Go straight in.
        if (SessionStore.isLoggedIn()) { goHome(); return }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill demo credentials for easy testing.
        binding.editUsername.setText("emilys")
        binding.editPassword.setText("emilyspass")

        binding.btnLogin.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            Anim.bounce(binding.btnLogin) { attemptLogin() }
        }
        binding.btnGuest.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            goHome()
        }
    }

    private fun attemptLogin() {
        val username = binding.editUsername.text.toString().trim()
        val password = binding.editPassword.text.toString()
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter your username and password.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val user = ShopRepository().login(username, password)
            setLoading(false)
            if (user == null) {
                Toast.makeText(this@LoginActivity, "Login failed — check your credentials.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            SessionStore.save(this@LoginActivity, user)
            SoundManager.play(SoundManager.Fx.SUCCESS)
            Toast.makeText(this@LoginActivity, "Welcome, ${user.firstName}!", Toast.LENGTH_SHORT).show()
            goHome()
        }
    }

    /** Enter the app, resetting the task so login isn't on the back stack. */
    private fun goHome() {
        startActivity(
            Intent(this, FridgeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
