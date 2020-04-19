package com.example.myapplication.credential

import com.amazonaws.auth.AWSCredentials

class AmazonCredentials() : AWSCredentials {

    override fun getAWSAccessKeyId(): String {
        return "AWSAccessKeyId"
    }

    override fun getAWSSecretKey(): String {
        return "AWSSecretKey"
    }
}