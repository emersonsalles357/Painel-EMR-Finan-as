package br.com.emr.emrfinancas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Clock;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EmrFinancasApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmrFinancasApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
