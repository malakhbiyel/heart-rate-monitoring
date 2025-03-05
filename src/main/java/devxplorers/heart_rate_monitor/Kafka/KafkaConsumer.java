package devxplorers.heart_rate_monitor.Kafka;

import devxplorers.heart_rate_monitor.HeartRate.HeartRateData;
import devxplorers.heart_rate_monitor.HeartRate.HeartRateRepository;
import devxplorers.heart_rate_monitor.Twilio.TwilioService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;


@Service
public class KafkaConsumer {

    @Autowired
    private HeartRateRepository heartRateRepository;

    @Autowired
    private TwilioService twilioService;

    private static final int HIGH_HEART_RATE = 100;
    private static final int LOW_HEART_RATE = 60;
    private static final String TEST_PHONE_NUMBER = "+33784828928";
    private static final int WINDOW_SIZE = 10;

    private Queue<Integer> recentHeartRates = new LinkedList<>();
    private int sum = 0;

    // Date format to parse the timestamp from LogReader
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    @KafkaListener(topics = "heart_rate", groupId = "heart_rate_group")
    public void consume(String message) {
        log.info("Message reçu et traité : {}", message);
        try {
            // Format attendu : "Time: 2025-03-03T12:30:45.123, Heart Rate: 85"
            String[] parts = message.split(", Heart Rate: ");
            if (parts.length != 2) {
                System.err.println("❌ Format de message invalide : " + message);
                return;
            }

            String timeString = parts[0].replace("Time: ", "").trim();
            int heartRate = Integer.parseInt(parts[1].trim());

            // Ajouter ".000" si la date ne contient pas de millisecondes
            if (timeString.length() == 19) { // Format sans millisecondes
                timeString += ".000"; // Ajout des millisecondes par défaut
            }

            // Convertir la chaîne de timestamp en Date (avec millisecondes)
            Date timestamp;
            try {
                timestamp = dateFormat.parse(timeString); // Utilisation du format avec précision (millisecondes)
            } catch (ParseException e) {
                System.err.println("❌ Erreur de parsing du timestamp : " + timeString);
                return;
            }

            // Créer un objet HeartRateData avec le timestamp extrait
            HeartRateData data = new HeartRateData(heartRate, timestamp);
            heartRateRepository.save(data);

            System.out.println("📥 Stocké dans Elasticsearch : " + heartRate + " BPM à " + timestamp);

            // Mettre à jour la fenêtre mobile pour les calculs de fréquence cardiaque
            recentHeartRates.add(heartRate);
            sum += heartRate;
            if (recentHeartRates.size() > WINDOW_SIZE) {
                sum -= recentHeartRates.poll();
            }

            double average = sum / (double) recentHeartRates.size();
            double deviation = calculateStandardDeviation(recentHeartRates, average);

            System.out.println("📊 Moyenne fréquence cardiaque : " + average + ", Écart type : " + deviation);

            // Vérification pour une fréquence cardiaque anormale
            if (heartRate >= HIGH_HEART_RATE || heartRate <= LOW_HEART_RATE) {
                String alertMessage = "⚠️ Alerte : Fréquence cardiaque anormale détectée! (" + heartRate + " BPM)";
                twilioService.sendSms(TEST_PHONE_NUMBER, alertMessage);
                System.out.println("📩 Notification envoyée via Twilio!");
            }

            if (deviation > 15) {
                String alertMessage = "⚠️ Alerte : Fréquence cardiaque instable détectée! (Déviation élevée)";
                twilioService.sendSms(TEST_PHONE_NUMBER, alertMessage);
                System.out.println("📩 Notification envoyée pour instabilité cardiaque!");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Erreur de parsing de la fréquence cardiaque : " + message);
        }
    }

    private double calculateStandardDeviation(Queue<Integer> heartRates, double average) {
        double sumOfSquares = 0.0;
        for (int rate : heartRates) {
            sumOfSquares += Math.pow(rate - average, 2);
        }
        return Math.sqrt(sumOfSquares / heartRates.size());
    }
}



