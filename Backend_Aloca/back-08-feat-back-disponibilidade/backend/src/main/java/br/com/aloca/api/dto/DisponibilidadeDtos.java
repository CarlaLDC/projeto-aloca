package br.com.aloca.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Tela 03 · Disponibilidade. */
public final class DisponibilidadeDtos {
    private DisponibilidadeDtos() {}

    public record Intervalo(LocalTime inicio, LocalTime fim, String motivo) {}

    public record RecursoDisponibilidade(Long id, String nome, String tipo, String tipoRotulo, String localizacao,
                                         int capacidade, boolean restrito, String status, LocalTime livreDe,
                                         LocalTime livreAte, List<Intervalo> ocupacoes, List<Intervalo> janelasLivres) {}

    public record Resumo(long disponiveis, long ocupados, long total) {}

    public record Disponibilidade(LocalDate data, Resumo resumo, List<RecursoDisponibilidade> recursos) {}
}
