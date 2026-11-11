package br.com.aloca.repositorio;

import br.com.aloca.dominio.Movimentacao;
import br.com.aloca.dominio.StatusMovimentacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepositorio extends JpaRepository<Movimentacao, Long> {

    Optional<Movimentacao> findByCodigo(String codigo);

    List<Movimentacao> findByStatusNotOrderByRetiradaPrevistaDesc(StatusMovimentacao status);

    List<Movimentacao> findByReservaId(Long reservaId);
}
