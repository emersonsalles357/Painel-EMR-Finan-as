package br.com.emr.emrfinancas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

import java.time.Clock;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EmrFinancasApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(EmrFinancasApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(512));
        application.run(args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
