package com.example.budgettracker.ui.lock

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.budgettracker.R

object DeviceLock {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canLock(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val keyguard = context.getSystemService(android.app.KeyguardManager::class.java)
            keyguard?.isDeviceSecure == true
        }
    }

    fun credentialIntent(activity: FragmentActivity): Intent? {
        val keyguard = activity.getSystemService(android.app.KeyguardManager::class.java) ?: return null
        return keyguard.createConfirmDeviceCredentialIntent(
            activity.getString(R.string.app_lock_title),
            activity.getString(R.string.app_lock_reason)
        )
    }

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ): BiometricPrompt {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.app_lock_title))
            .setSubtitle(activity.getString(R.string.app_lock_reason))
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
        return prompt
    }
}
