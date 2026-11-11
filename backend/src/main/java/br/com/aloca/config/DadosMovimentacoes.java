package br.com.aloca.config;

import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.MovimentacaoRepositorio;
import br.com.aloca.repositorio.ReservaRepositorio;
import br.com.aloca.repositorio.UsuarioRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimentações de demonstração para as reservas aprovadas com materiais:
 * as já concluídas aparecem devolvidas; as de hoje que já começaram aparecem retiradas.
 */
@Component
@Order(4)
@ConditionalOnProperty(name = "aloca.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosMovimentacoes implements ApplicationRunner {

    private final MovimentacaoRepositorio movimentacoes;
    private final ReservaRepositorio reservas;
    private final UsuarioRepositorio usuarios;
    private final Clock relogio;

    public DadosMovimentacoes(MovimentacaoRepositorio movimentacoes, ReservaRepositorio reservas,
                              UsuarioRepositorio usuarios, Clock relogio) {
        this.movimentacoes = movimentacoes;
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (movimentacoes.count() > 0) return;
        LocalDateTime agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);
        Usuario responsavel = usuarios.comPerfil(Perfil.RESPONSAVEL).stream().findFirst().orElse(null);
        var status = EnumSet.of(StatusReserva.APROVADA, StatusReserva.EM_USO, StatusReserva.CONCLUIDA);
        reservas.findAll().stream()
            .filter(r -> status.contains(r.getStatus()) && !r.getMateriais().isEmpty())
            .sorted((a, b) -> a.getId().compareTo(b.getId()))
            .forEach(r -> {
                for (Recurso material : r.getMateriais()) {
                    Movimentacao m = movimentacoes.save(new Movimentacao(r, material));
                    m.setCodigo("MOV-%04d".formatted(m.getId()));
                    LocalDateTime retirada = r.inicioEm().minusMinutes(10);
                    if (r.getStatus() == StatusReserva.CONCLUIDA) {
                        m.registrarRetirada(responsavel, retirada);
                        m.registrarDevolucao(responsavel, r.fimEm().plusMinutes(5), "DEV-%04d".formatted(m.getId()));
                    } else if (!agora.isBefore(r.inicioEm().minusMinutes(30))) {
                        m.registrarRetirada(responsavel, retirada.isBefore(agora) ? retirada : agora);
                    }
                }
            });
    }
}
