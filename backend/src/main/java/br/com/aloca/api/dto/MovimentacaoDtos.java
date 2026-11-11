package br.com.aloca.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Tela 09 · Retiradas e devoluções. */
public final class MovimentacaoDtos {
    private MovimentacaoDtos() {}

    public record MovimentacaoDto(String codigo, String reserva, String item, String categoria, String responsavel,
                                  String destino, LocalDateTime retiradaPrevista, LocalDateTime devolucaoPrevista,
                                  LocalDateTime retiradaEm, LocalDateTime devolucaoEm, String status,
                                  String statusRotulo, String protocolo) {}

    public record Movimentacoes(long aguardandoRetirada, long emUso, long devolucoesPendentes,
                                List<MovimentacaoDto> movimentacoes) {}
}
