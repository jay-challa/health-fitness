package com.healthfitness.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.healthfitness.R
import com.healthfitness.databinding.ActivityHomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.request.SensorRequest

import com.google.android.gms.fitness.data.DataType
import android.content.IntentSender.SendIntentException
import android.widget.Toast
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.request.OnDataPointListener
import java.util.concurrent.TimeUnit


class HomeActivity : AppCompatActivity(), View.OnClickListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    var REQUEST_OAUTH = 1001
    lateinit var binding: ActivityHomeBinding
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@HomeActivity, R.layout.activity_home)
        binding.topBarLayout.logout.setOnClickListener(this)

        initGoogleClient()
    }

    fun initGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)  // Required for SensorsApi calls
            .useDefaultAccount()
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


    }

    override fun onConnectionFailed(result: ConnectionResult) {
        if (result.getErrorCode() === FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {
            try {
                result.startResolutionForResult(this, REQUEST_OAUTH)
            } catch (e: SendIntentException) {
            }

        }
    }

    override fun onConnected(p0: Bundle?) {
        try {
            Fitness.SensorsApi.add(
                mGoogleApiClient,
                SensorRequest.Builder()
                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                    .setSamplingRate(1, TimeUnit.MINUTES)  // sample once per minute
                    .build()
            ) {
                for (field in it.getDataType().getFields()) {
                    var value = it.getValue(field);
                    Toast.makeText(this@HomeActivity, field.name + " - " + value, Toast.LENGTH_LONG)
                        .show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConnectionSuspended(p0: Int) {

    }
    override fun onClick(p0: View?) {
        when (p0!!.id) {
            R.id.logout -> {
                mGoogleSignInClient.signOut()
                val start = Intent(this@HomeActivity, LoginActivity::class.java)
                start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(start)
            }
        }
    }
}
