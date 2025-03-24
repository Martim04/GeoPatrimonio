package com.example.projeto

import android.app.Application
import com.google.firebase.FirebaseApp
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}