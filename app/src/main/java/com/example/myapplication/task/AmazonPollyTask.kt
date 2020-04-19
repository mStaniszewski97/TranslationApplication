package com.example.myapplication.task

import android.os.AsyncTask
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.services.polly.AmazonPollyPresigningClient
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest
import com.example.myapplication.exception.PollyException
import java.net.URL

class AmazonPollyTask : AsyncTask<Any, Void, URL>() {

    override fun doInBackground(vararg params: Any?): URL? {
        val client: AmazonPollyPresigningClient
        val credentialsProvider = params.first()
        val synthesizeSpeechPresignRequest = params.last();
        if (credentialsProvider is CognitoCachingCredentialsProvider
            && synthesizeSpeechPresignRequest is SynthesizeSpeechPresignRequest){
            client = AmazonPollyPresigningClient(credentialsProvider)
            return client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest)
        } else {
            throw PollyException("There is a problem with amazon service called polly")
        }
    }
}