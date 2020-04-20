# TranslationApplication
Translation application for android using AWS services and Kotlin. Thesis project.

Amazon Web Services:
* Translate
* Polly
* Cognito


### How to build app?
1. After clone repository open whole project in Android Studio
2. Fill your own credentials in classes (credential package) 

Example:

```
override fun getAWSAccessKeyId(): String {
        return "This is my AWS AccessKeyId"
    }
    
override fun getAWSSecretKey(): String {
        return "This is my AWSSecretKey"
    }
```
3. Run app on the Android Emulator or deploy on your own smartphone
