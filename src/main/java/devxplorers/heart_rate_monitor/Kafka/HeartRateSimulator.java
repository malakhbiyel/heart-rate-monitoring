package devxplorers.heart_rate_monitor.Kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class HeartRateSimulator {
    @Autowired
    private Producer heartRateProducer;

    private final Random random = new Random();

    @Scheduled(fixedRate = 1000) // Generate heart rate every second
    public void simulateHeartRate() {
        int heartRate = 60 + random.nextInt(40); // Random heart rate between 60 and 100
        heartRateProducer.sendHeartRate(heartRate);
    }
}
