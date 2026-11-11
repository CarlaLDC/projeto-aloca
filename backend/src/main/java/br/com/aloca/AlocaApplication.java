package br.com.aloca;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlocaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlocaApplication.class, args);
    }

    /** Relógio único da aplicação, no horário de Brasília por padrão. */
    @Bean
    Clock relogio(@Value("${aloca.fuso-horario:America/Sao_Paulo}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
