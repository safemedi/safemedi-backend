package com.safemedi.app.sefemedi.domain.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
@ConditionalOnProperty(prefix = "firebase", name = ["enabled"], havingValue = "true")
class FirebaseConfig(
    @param:Value("\${firebase.credentials-path}")
    private val credentialsPath: String,
) {

    @Bean
    fun firebaseApp(): FirebaseApp {
        if (FirebaseApp.getApps().isNotEmpty()) {
            return FirebaseApp.getInstance()
        }
        require(credentialsPath.isNotBlank()) {
            "firebase.credentials-path must be configured when firebase.enabled=true"
        }

        val options = FileInputStream(credentialsPath).use { inputStream ->
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(inputStream))
                .build()
        }

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(
        firebaseApp: FirebaseApp,
    ): FirebaseMessaging {
        return FirebaseMessaging.getInstance(firebaseApp)
    }
}
