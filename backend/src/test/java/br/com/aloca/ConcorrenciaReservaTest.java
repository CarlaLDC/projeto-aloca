package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aloca.api.dto.ReservaDtos.ReservaRequest;
import br.com.aloca.dominio.Usuario;
import br.com.aloca.repositorio.UsuarioRepositorio;
import br.com.aloca.servico.ReservaService;
import br.com.aloca.servico.ReservaService.Gravada;
import br.com.aloca.servico.ReservaService.Resultado;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RF-06 / RNF-02: duas solicitações simultâneas para o mesmo recurso e horário,
 * no banco real (MySQL em contêiner). Exatamente uma deve ser aceita.
 */
class ConcorrenciaReservaTest extends ApiTesteBase {

    @Autowired ReservaService reservas;
    @Autowired UsuarioRepositorio usuarios;

    @RepeatedTest(5)
    void duasSolicitacoesSimultaneasResultamEmExatamenteUmaAceita() throws Exception {
        Usuario ana = usuarios.findByEmailIgnoreCase(ANA).orElseThrow();
        Usuario mariana = usuarios.findByEmailIgnoreCase(MARIANA).orElseThrow();
        LocalDate dia = diaLivre();
        ReservaRequest pedido = new ReservaRequest("Disputa", dia, LocalTime.of(10, 0), LocalTime.of(12, 0),
            SALA_204, null, List.of(PROJETOR_04));

        CyclicBarrier largada = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Resultado>> futuros = new ArrayList<>();
            for (Usuario u : List.of(ana, mariana)) {
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return reservas.criar(pedido, u);
                }));
            }
            long aceitas = 0;
            for (Future<Resultado> f : futuros) {
                if (f.get(30, TimeUnit.SECONDS) instanceof Gravada) aceitas++;
            }
            assertThat(aceitas).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
