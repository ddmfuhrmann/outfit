package github.io.ddmfuhrmann.outfit.shared.infrastructure.web;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonConfig {

    /**
     * Serializes Long/long as JSON strings to prevent precision loss in JavaScript clients.
     * TSIDs exceed Number.MAX_SAFE_INTEGER (2^53-1); JS JSON.parse silently corrupts them as numbers.
     * Jackson deserializes string-to-Long on the way back in, so Java clients are unaffected.
     */
    @Bean
    Module longAsStringModule() {
        var module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return module;
    }
}
