package com.kraft.lotto.infra.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kraft.lotto.infra.config.KraftFcmProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "kraft.fcm.enabled", havingValue = "true")
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    @Bean
    public FirebaseApp firebaseApp(KraftFcmProperties props) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream credentials = new FileInputStream(props.getCredentialsPath())) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentials))
                        .build();
                log.info("fcm FirebaseApp initializing credentialsPath={}", props.getCredentialsPath());
                return FirebaseApp.initializeApp(options);
            }
        }
        log.info("fcm FirebaseApp already initialized — reusing existing instance");
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}
