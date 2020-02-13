package com.healthfitness.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.databinding.DataBindingUtil
import com.healthfitness.R
import com.healthfitness.databinding.ActivitySplashScreenBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.healthfitness.activity.pedometer.Pedometer

class SplashScreenActivity : AppCompatActivity() {

    lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this@SplashScreenActivity,
            R.layout.activity_splash_screen
        )

        val account = GoogleSignIn.getLastSignedInAccount(this)
        Handler().postDelayed({
            if (account != null) {
                val start = Intent(this@SplashScreenActivity, Pedometer::class.java)
                startActivity(start)
                finish()
            } else {
                val start = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                startActivity(start)
                finish()
            }
        }, 3000)
    }
}

