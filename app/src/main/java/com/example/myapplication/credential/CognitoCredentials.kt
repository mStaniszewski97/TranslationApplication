package com.example.myapplication.credential

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions

class CognitoCredentials(applicationContext: Context) {
    var credentialsProvider = CognitoCachingCredentialsProvider(
        applicationContext,
        "CognitoIdentityPoolId", // Identity pool ID
        Regions.EU_WEST_1 // Region
    )
}