package br.com.aloca;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Ambiente realista: os testes de integração usam um PostgreSQL de verdade, em container
 * (Testcontainers), em vez de um banco em memória. A persistência, as consultas de conflito
 * e o teste de concorrência (dupla reserva) rodam no mesmo tipo de banco usado em produção.
 *
 * Requer Docker em execução na máquina (no GitHub Actions o runner ubuntu-latest já tem).
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainer {

    public static final String IMAGEM = "postgres:17-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(IMAGEM);
    }
}
