package com.udacity.project4.authentication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    private val TAG: String = "AuthenticationTag"
    private val AUTH_REQUEST_CODE = 701
    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var editor: SharedPreferences.Editor

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Start LifeCycle")
        if (firebaseAuth.currentUser != null) {
            startActivity(
                Intent(this, RemindersActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Create LifeCycle")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        editor = getSharedPreferences("Authentication", MODE_PRIVATE).edit()

        setupFirebaseAuth()

        binding.emailLogin.setOnClickListener {
            startLoginFlow()
        }

    }

    private fun startLoginFlow() {
        val signInIntent =
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        startActivityForResult(signInIntent, AUTH_REQUEST_CODE)
    }

    private fun setupFirebaseAuth() {
        providers = listOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener {
            if (it.currentUser != null) {
                startActivity(
                    Intent(this, RemindersActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                editor.putBoolean("isLoggedIn", true).apply()
            } else {
                Toast.makeText(this, "Please Try Login Again", Toast.LENGTH_SHORT).show()
                editor.putBoolean("isLoggedIn", false).apply()

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTH_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK){
                Log.d(TAG, "onActivityResult: Success Login ${firebaseAuth.currentUser?.displayName}")
                startActivity(
                    Intent(this, RemindersActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }else{
                Log.d(TAG, "onActivityResult: Error ${response?.error?.message}")
                Toast.makeText(this, "Please Try Login Again", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
