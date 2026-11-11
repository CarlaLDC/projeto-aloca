package br.com.aloca;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Banco real dos testes de integração: MySQL em contêiner Docker (Testcontainers).
 * O @ServiceConnection liga o datasource do Spring a este contêiner.
 * Persistência e concorrência são verificadas no mesmo banco usado em produção.
 */
@TestConfiguration(proxyBeanMethods = false)
public class BancoMySqlTeste {

    static final DockerImageName IMAGEM = DockerImageName.parse("mysql:8.4");

    @Bean
    @ServiceConnection
    MySQLContainer mysql() {
        return new MySQLContainer(IMAGEM)
            .withDatabaseName("aloca")
            .withUsername("aloca")
            .withPassword("aloca")
            // Mesmo fuso e charset da aplicação (nomes com acento).
            .withCommand("--default-time-zone=-03:00", "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_0900_ai_ci");
    }
}
