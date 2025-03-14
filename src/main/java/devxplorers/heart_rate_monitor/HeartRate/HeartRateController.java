package devxplorers.heart_rate_monitor.HeartRate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/heart-rate")
public class HeartRateController {

    @Autowired
    private HeartRateRepository heartRateRepository;

    @GetMapping("/all")
    public Iterable<HeartRateData> getAllHeartRateData() {
        return heartRateRepository.findAll();
    }
}
