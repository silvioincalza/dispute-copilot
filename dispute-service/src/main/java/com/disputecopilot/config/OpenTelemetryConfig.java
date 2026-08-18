package com.disputecopilot.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> genAiObservationCustomizer() {
        return registry -> registry.observationConfig().observationFilter(context -> {
            context.addLowCardinalityKeyValue(KeyValue.of("service", "dispute-service"));
            if (context.getName() != null && context.getName().toLowerCase().contains("chat")) {
                context.addLowCardinalityKeyValue(KeyValue.of("gen_ai.system", "openai"));
                context.addLowCardinalityKeyValue(KeyValue.of("gen_ai.operation.name", "dispute_resolution"));
            }
            return context;
        });
    }
}
