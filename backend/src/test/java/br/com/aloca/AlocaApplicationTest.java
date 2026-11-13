package br.com.aloca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** A aplicação sobe conectada a um PostgreSQL em container (Testcontainers). */
@SpringBootTest
@Import(PostgresTestcontainer.class)
class AlocaApplicationTest {

    @Test
    void contextoCarrega() {
        // Vazio de propósito: o teste falha se o contexto do Spring (beans, banco, segurança) não subir.
    }
}
