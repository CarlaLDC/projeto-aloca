package br.com.aloca.caixapreta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

/** Caixa-preta · pedidos simultâneos por HTTP real para a mesma sala e horário (RF-06, RNF-02). */
@DisplayName("Caixa-preta: pedidos simultâneos")
class ConcorrenciaCaixaPretaTest extends CaixaPretaBase {

    private static final int PEDIDOS = 10;

    @RepeatedTest(3)
    @DisplayName("10 pedidos simultâneos para a mesma sala e horário: exatamente 1 aceito (201) e 9 recusados (409)")
    void somenteUmAceito() throws Exception {
        LocalDate dia = diaLivre();
        List<String> tokens = List.of(api.token(ANA), api.token(MARIANA), api.token(ROBERTO), api.token(PATRICIA));
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(PEDIDOS);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            String token = tokens.get(i % tokens.size());
            respostas.add(pool.submit(() -> {
                largada.await();
                return api.post("/api/reservas", token, reserva(dia, "14:00", "15:00", SALA_204)).status();
            }));
        }
        largada.countDown();
        List<Integer> status = new ArrayList<>();
        for (Future<Integer> f : respostas) status.add(f.get());
        pool.shutdown();

        assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        assertThat(status).filteredOn(s -> s == 409).hasSize(PEDIDOS - 1);
    }
}
