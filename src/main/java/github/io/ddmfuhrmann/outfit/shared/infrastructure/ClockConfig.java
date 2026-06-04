package github.io.ddmfuhrmann.outfit.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("!test")
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
