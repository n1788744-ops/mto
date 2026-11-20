package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
  try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    val appCheck = FirebaseAppCheck.getInstance()
    val resId =
      context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
    val recaptchaSiteKey = if (resId != 0) context.getString(resId).trim() else ""

    if (BuildConfig.DEBUG) {
      val persistenceKey = FirebaseApp.getInstance().persistenceKey
      val prefs =
        context.getSharedPreferences("$DEBUG_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
      val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

      if (!debugToken.isNullOrEmpty()) {
        // Persist synchronously before DebugAppCheckProviderFactory reads it
        prefs.edit().putString(DEBUG_SECRET_KEY, debugToken).commit()
        appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d(TAG, "DebugAppCheckProviderFactory installed with Intent debug token.")
      } else if (!prefs.getString(DEBUG_SECRET_KEY, null).isNullOrEmpty()) {
        appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d(TAG, "DebugAppCheckProviderFactory installed with persisted debug token.")
      } else if (recaptchaSiteKey.isNotBlank()) {
        // Standalone device launch without harness debug token: use reCAPTCHA Enterprise
        appCheck.installAppCheckProviderFactory(
          RecaptchaAppCheckProviderFactory.getInstance(recaptchaSiteKey)
        )
        Log.d(TAG, "RecaptchaAppCheckProviderFactory installed for standalone device launch.")
      } else {
        appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.w(TAG, "No debug token or recaptcha_site_key found; falling back to DebugAppCheck.")
      }
    } else {
      if (recaptchaSiteKey.isNotBlank()) {
        appCheck.installAppCheckProviderFactory(
          RecaptchaAppCheckProviderFactory.getInstance(recaptchaSiteKey)
        )
        Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
      } else {
        Log.e(TAG, "recaptcha_site_key resource string not found or blank.")
      }
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize Firebase App Check", e)
  }
}
