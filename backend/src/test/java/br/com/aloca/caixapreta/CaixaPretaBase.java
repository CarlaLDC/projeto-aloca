package br.com.aloca.caixapreta;

import br.com.aloca.PostgresTestcontainer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Suíte de API em CAIXA-PRETA: a aplicação sobe inteira, com servidor HTTP real numa porta aleatória
 * e PostgreSQL em container. Os testes não usam MockMvc, repositórios nem classes internas: só enviam
 * requisições HTTP e conferem status, corpo JSON e cabeçalhos, como um cliente externo faria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainer.class)
abstract class CaixaPretaBase {

    static final String ANA = "ana.martins@aloca.edu.br";
    static final String MARIANA = "mariana.costa@aloca.edu.br";
    static final String ROBERTO = "roberto.almeida@aloca.edu.br";
    static final String PATRICIA = "patricia.souza@aloca.edu.br";
    static final String ANDRE = "andre.lima@aloca.edu.br";
    static final long SALA_204 = 1, SALA_108 = 2, SALA_REUNIOES = 3, AUDITORIO = 4, LAB_03 = 6;

    private static final AtomicInteger proximoDia = new AtomicInteger(1500);

    @LocalServerPort int porta;
    ClienteHttp api;

    @BeforeEach
    void conectar() { api = new ClienteHttp("http://localhost:" + porta); }

    static LocalDate diaLivre() {
        LocalDate d;
        do { d = LocalDate.now().plusDays(proximoDia.getAndIncrement()); } while (d.getDayOfWeek() == DayOfWeek.SUNDAY);
        return d;
    }

    static String reserva(LocalDate dia, String inicio, String fim, long espaco) {
        return "{\"atividade\":\"Caixa-preta\",\"data\":\"%s\",\"inicio\":\"%s\",\"fim\":\"%s\",\"espacoId\":%d}"
            .formatted(dia, inicio, fim, espaco);
    }
}
