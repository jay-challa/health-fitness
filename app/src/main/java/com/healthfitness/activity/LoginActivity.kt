package com.healthfitness.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.SignInButton
import com.healthfitness.R
import com.healthfitness.databinding.ActivityLoginBinding
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task

import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.healthfitness.activity.pedometer.Pedometer

class LoginActivity : AppCompatActivity(), View.OnClickListener {


    lateinit var binding: ActivityLoginBinding
    lateinit var mGoogleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@LoginActivity, R.layout.activity_login)
        binding.signInButton.setSize(SignInButton.SIZE_STANDARD)
        binding.signInButton.setOnClickListener(this)

        initGoogleClient()
    }

    fun initGoogleClient(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }


    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.sign_in_button -> {
                signIn()
            }
        }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUI(account)
        } catch (e: ApiException) {
            Log.w("TAG", "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if(account!=null){
            val start = Intent(this@LoginActivity, Pedometer::class.java)
            startActivity(start)
            finish()
        }else{
            shoeSnackbar("Login failed , please try again ..")
        }
    }

    fun shoeSnackbar(message: String) {
        Snackbar.make(binding!!.root, "" + message, Snackbar.LENGTH_LONG)
            .setAction("", null)
            .show()
    }

}
