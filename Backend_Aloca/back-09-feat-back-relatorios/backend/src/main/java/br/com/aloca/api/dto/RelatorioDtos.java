package br.com.aloca.api.dto;

import java.time.LocalDate;
import java.util.List;

/** Tela 11 · Relatórios. */
public final class RelatorioDtos {
    private RelatorioDtos() {}

    public record LinhaRecurso(Long id, String nome, String tipoRotulo, int utilizacao, double horas,
                               long reservas, long conflitos) {}

    public record LinhaProfessor(Long id, String nome, double horas, long reservas) {}

    public record Relatorio(LocalDate inicio, LocalDate fim, int utilizacaoEspacos, double horasAlocadas,
                            long conflitosEvitados, long totalReservas, List<LinhaRecurso> recursos,
                            List<LinhaProfessor> professores) {}
}
