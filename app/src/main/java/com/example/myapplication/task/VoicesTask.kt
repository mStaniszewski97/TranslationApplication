package com.example.myapplication.task

import android.os.AsyncTask
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.services.polly.AmazonPollyPresigningClient
import com.amazonaws.services.polly.model.DescribeVoicesRequest
import com.amazonaws.services.polly.model.DescribeVoicesResult
import com.amazonaws.services.polly.model.Voice

class VoicesTask : AsyncTask<CognitoCachingCredentialsProvider, Void, MutableList<Voice>>() {

    private lateinit var voices: MutableList<Voice>

    override fun doInBackground(vararg params: CognitoCachingCredentialsProvider?): MutableList<Voice> {
        val credentialsProvider = params.first()
        val client = AmazonPollyPresigningClient(credentialsProvider)
        val describeVoicesRequest = DescribeVoicesRequest()
        val describeVoicesResult: DescribeVoicesResult = client.describeVoices(describeVoicesRequest)
        return describeVoicesResult.voices
    }

    override fun onPostExecute(result: MutableList<Voice>?) {
        super.onPostExecute(result)
        if (result != null) {
            voices = result
        }
    }
}