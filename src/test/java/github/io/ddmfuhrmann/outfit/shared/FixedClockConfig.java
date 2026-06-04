package github.io.ddmfuhrmann.outfit.shared;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class FixedClockConfig {

    public static final Instant FIXED_INSTANT = Instant.parse("2025-06-04T00:00:00Z");

    @Bean
    public Clock clock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
