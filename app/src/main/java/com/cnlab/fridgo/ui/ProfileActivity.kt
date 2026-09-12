package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.CartStore
import com.cnlab.fridgo.data.SessionStore
import com.cnlab.fridgo.databinding.ActivityProfileBinding

/**
 * The store "profile" screen from the MAP slide. Shows the signed-in user (avatar,
 * name, email) with quick links to the cart, or — when signed out — a prompt to
 * sign in. Re-reads [SessionStore] in onResume so it reflects a just-completed
 * login.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Profile"

        binding.btnSignIn.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.rowCart.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            startActivity(Intent(this, CartActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign out")
                .setMessage("Sign out of the store?")
                .setPositiveButton("Sign out") { _, _ ->
                    SessionStore.clear(this)
                    startActivity(
                        Intent(this, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val user = SessionStore.current()
        val signedIn = user != null

        binding.signedInGroup.visibility = if (signedIn) View.VISIBLE else View.GONE
        binding.signedOutGroup.visibility = if (signedIn) View.GONE else View.VISIBLE

        if (user != null) {
            binding.textName.text = user.fullName
            binding.textEmail.text = user.email
            binding.textUsername.text = "@${user.username}"
            Glide.with(binding.avatar)
                .load(user.image)
                .placeholder(R.drawable.bg_circle)
                .circleCrop()
                .into(binding.avatar)
        }
        binding.textCartCount.text = "${CartStore.count()} item${if (CartStore.count() == 1) "" else "s"}"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
