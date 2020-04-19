package com.example.myapplication

import CustomProgressBar
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest
import com.amazonaws.services.polly.model.Voice
import com.amazonaws.services.translate.AmazonTranslateAsyncClient
import com.amazonaws.services.translate.model.TranslateTextRequest
import com.amazonaws.services.translate.model.TranslateTextResult
import com.example.myapplication.credential.AmazonCredentials
import com.example.myapplication.credential.CognitoCredentials
import com.example.myapplication.task.AmazonPollyTask
import com.example.myapplication.task.VoicesTask
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val LOG_TAG = MainActivity::class.java.simpleName
    private var awsCredentials: AmazonCredentials = AmazonCredentials()
    private lateinit var cognitoCredentials: CognitoCredentials
    private lateinit var voices: MutableList<Voice>
    private lateinit var playButton: Button
    private lateinit var micButton: Button
    private lateinit var swapButton: Button
    private lateinit var translateSpinner: Spinner
    private lateinit var resultSpinner: Spinner
    private lateinit var translateText: TextView
    private lateinit var responseText: TextView
    private lateinit var synthesizeText: EditText
    private lateinit var speechResult: String
    private val progressBar = CustomProgressBar()
    private lateinit var textToTranslate: EditText
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioURL: URL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)
        hideActionBar()
        initialize()

        micButton = findViewById(R.id.micButton)
        micButton.setOnClickListener { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSpeechInput()
        }; }

        swapButton = findViewById(R.id.swapButton)
        swapButton.setOnClickListener { swapAction() }

        playButton = findViewById(R.id.playButton)
        playButton.setOnClickListener {
            if (this::audioURL.isInitialized){
                playSynthesizer(audioURL)
            }}

        val translateButton = findViewById<Button>(R.id.translateButton)
        translateButton.setOnClickListener {
            progressBar.show(this)
            Handler().postDelayed({
                translateButtonAction()
                progressBar.dialog.dismiss()
            }, 500)
        }
    }

    private fun initialize() {
        cognitoCredentials =
            CognitoCredentials(
                applicationContext
            )
        val voicesTask = VoicesTask()
        mediaPlayer = MediaPlayer()
        voices = voicesTask.execute(cognitoCredentials.credentialsProvider).get()

        translateSpinner = findViewById(R.id.translateSpinner)
        resultSpinner = findViewById(R.id.resultSpinner)
        textToTranslate = findViewById(R.id.textToTranslate)
        synthesizeText = findViewById(R.id.synthesizeText)
        val adapter = ArrayAdapter.createFromResource(this, R.array.country_arrays, R.layout.spinner_item)

        translateSpinner.adapter = adapter
        resultSpinner.adapter = adapter

        val defaultResponseLanguage = resources.getString(R.string.defaultResponseLanguage)
        val indexOfResponseLanguage = resources.getStringArray(R.array.country_arrays).indexOf(defaultResponseLanguage)
        resultSpinner.setSelection(indexOfResponseLanguage)
        resources.getStringArray(R.array.country_arrays).forEach { country ->
            if (Locale.getDefault().language == getTranslateLanguageCode(country)){
                val indexOf = resources.getStringArray(R.array.country_arrays).indexOf(country)
                translateSpinner.setSelection(indexOf)
            }
        }

        translateSpinner.onItemSelectedListener = this
        resultSpinner.onItemSelectedListener = this
    }

    private fun translateButtonAction() {
        val text = textToTranslate.text.toString()

        var translate = translateText(awsCredentials, text, getTranslateLanguageCode(translateSpinner.selectedItem as String), getTranslateLanguageCode(resultSpinner.selectedItem as String))
        if (text.equals(translate, ignoreCase = true)){
            translate = translateText(awsCredentials, text, "auto", getTranslateLanguageCode(resultSpinner.selectedItem as String))
        }
        synthesizeText.setText(translate)
        polly(translate)
    }

    private fun translateText(
        awsCredentials: AWSCredentials,
        textToTranslate: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        var result = "Sorry, the text has not been translated"
        val translateAsyncClient = AmazonTranslateAsyncClient(awsCredentials)
        val translateTextRequest = TranslateTextRequest()
            .withText(textToTranslate)
            .withSourceLanguageCode(sourceLanguage)
            .withTargetLanguageCode(targetLanguage)
        val translateTextAsync = translateAsyncClient.translateTextAsync(
            translateTextRequest,
            object : AsyncHandler<TranslateTextRequest?, TranslateTextResult?> {
                override fun onError(exception: Exception) {
                    Log.e(
                        LOG_TAG,
                        "Error occurred in translating the text: " + exception.localizedMessage
                    )
                    result = "Error occurred in translating the text"
                }

                override fun onSuccess(
                    request: TranslateTextRequest?,
                    translateTextResult: TranslateTextResult?
                ) {
                    Log.d(LOG_TAG, "Original Text: " + request?.text)
                    Log.d(LOG_TAG, "Translated Text: " + translateTextResult?.translatedText)
                    result = translateTextResult?.translatedText.toString()
                }
            })
        do {
            Thread.sleep(1000)
        } while (!translateTextAsync.isDone || mediaPlayer.isPlaying)
        return result
    }

    private fun polly(text: String) {
        val synthesizeSpeechPresignRequest: SynthesizeSpeechPresignRequest = SynthesizeSpeechPresignRequest()
                .withText(text)
                .withVoiceId(getVoiceByLanguageCode(getPollyLanguageCode(resultSpinner.selectedItem as String)).id)
                .withOutputFormat(OutputFormat.Mp3)

        audioURL = AmazonPollyTask()
            .execute(cognitoCredentials.credentialsProvider, synthesizeSpeechPresignRequest).get()

        playSynthesizer(audioURL)
    }

    private fun playSynthesizer(audioURL: URL) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(audioURL.toString())
        } catch (exception: java.lang.Exception) {
            Log.e(LOG_TAG, "Unable to set data source for the media player! " + exception.message)
        }

        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener { mediaPlayer.start() }
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun swapAction() {
        val translateLanguage = translateSpinner.selectedItemPosition
        translateSpinner.setSelection(resultSpinner.selectedItemPosition)
        resultSpinner.setSelection(translateLanguage)

        val textToSwap = synthesizeText.text
        synthesizeText.text = textToTranslate.text
        textToTranslate.text = textToSwap
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(getPollyLanguageCode(translateSpinner.selectedItem as String)))

        try {
            startActivityForResult(intent, 10)
        } catch (exception: java.lang.Exception) {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT)
                .show()
            Log.e(LOG_TAG, "Speech recognize not working " + exception.message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (result != null) {
                        speechResult = result.first()
                        textToTranslate.setText(speechResult)
                        Log.i("Speech result", speechResult)
                    }
                }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val translateButton = findViewById<Button>(R.id.translateButton)
        translateText = findViewById(R.id.translateText)
        responseText = findViewById(R.id.responseText)
        val text = parent?.getItemAtPosition(position).toString()
        if (parent != null && parent == translateSpinner) {
            translateText.text = text
            textToTranslate.hint = getHint(text)
            translateButton.text = getTranslateButtonText(text)
        }
        if (parent != null && parent == resultSpinner) {
            responseText.text = text
        }
    }

    private fun getHint(text: String): String? {
        resources.getStringArray(R.array.country_arrays).forEach { country ->
            if (country == text){
                val indexOfHint = 1
                val identifier = resources.getIdentifier(country, "array", packageName)
                return resources.getStringArray(identifier)[indexOfHint]
            }
        }
        return resources.getString(R.string.defaultHint)
    }

    private fun getTranslateButtonText(text: String): String? {
        resources.getStringArray(R.array.country_arrays).forEach { country ->
            if (country == text){
                val identifier = resources.getIdentifier(country, "array", packageName)
                return resources.getStringArray(identifier).last()
            }
        }
        return resources.getString(R.string.defaultButtonLanguage)
    }

    private fun getVoiceByLanguageCode(languageCode: String): Voice {
        return voices.first { voice -> voice.languageCode == languageCode }
    }

    private fun getPollyLanguageCode(spinner: String): String {
        val identifier = resources.getIdentifier(spinner, "array", packageName)
        return resources.getStringArray(identifier).first()
    }

    private fun getTranslateLanguageCode(spinner: String): String {
        if (spinner == "Auto"){
            return "auto"
        }
        val languageCode = getPollyLanguageCode(spinner)
        return languageCode.substring(0, languageCode.indexOf("-"))
    }
}
