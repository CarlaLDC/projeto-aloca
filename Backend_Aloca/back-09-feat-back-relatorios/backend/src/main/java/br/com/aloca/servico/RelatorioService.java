package br.com.aloca.servico;

import br.com.aloca.api.dto.RelatorioDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.ConflitoRegistradoRepositorio;
import br.com.aloca.repositorio.RecursoRepositorio;
import br.com.aloca.repositorio.ReservaRepositorio;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tela 11 · Relatórios (RF-12): utilização, carga horária e conflitos evitados por período e recurso.
 * Utilização = horas reservadas / horas de funcionamento (segunda a sábado) no período.
 */
@Service
public class RelatorioService {

    private final ReservaRepositorio reservas;
    private final RecursoRepositorio recursos;
    private final ConflitoRegistradoRepositorio conflitos;
    private final Clock relogio;

    public RelatorioService(ReservaRepositorio reservas, RecursoRepositorio recursos,
                            ConflitoRegistradoRepositorio conflitos, Clock relogio) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.conflitos = conflitos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Relatorio gerar(LocalDate inicioPedido, LocalDate fimPedido) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate inicio = inicioPedido != null ? inicioPedido : hoje.minusDays(29);
        LocalDate fim = fimPedido != null ? fimPedido : hoje;
        if (fim.isBefore(inicio)) {
            throw RegraNegocioException.invalida("A data final do período deve ser igual ou posterior à inicial.");
        }
        if (Period.between(inicio, fim).toTotalMonths() >= 12) {
            throw RegraNegocioException.invalida("Escolha um período de até 12 meses.");
        }

        List<Reserva> doPeriodo = reservas.noPeriodo(inicio, fim, StatusReserva.USO_CONFIRMADO);
        List<ConflitoRegistrado> conflitosDoPeriodo = conflitos.registradosEntre(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
        long diasUteis = inicio.datesUntil(fim.plusDays(1)).filter(d -> d.getDayOfWeek() != DayOfWeek.SUNDAY).count();

        Map<Long, Double> horasPorRecurso = new HashMap<>();
        Map<Long, Long> reservasPorRecurso = new HashMap<>();
        Map<Long, Double> horasPorProfessor = new HashMap<>();
        Map<Long, Long> reservasPorProfessor = new HashMap<>();
        Map<Long, Professor> professores = new HashMap<>();
        double horasTotais = 0;

        for (Reserva r : doPeriodo) {
            double horas = Duration.between(r.getInicio(), r.getFim()).toMinutes() / 60.0;
            horasTotais += horas;
            List<Recurso> usados = new ArrayList<>(r.getMateriais());
            usados.add(r.getEspaco());
            for (Recurso u : usados) {
                horasPorRecurso.merge(u.getId(), horas, Double::sum);
                reservasPorRecurso.merge(u.getId(), 1L, Long::sum);
            }
            if (r.getProfessor() != null) {
                professores.put(r.getProfessor().getId(), r.getProfessor());
                horasPorProfessor.merge(r.getProfessor().getId(), horas, Double::sum);
                reservasPorProfessor.merge(r.getProfessor().getId(), 1L, Long::sum);
            }
        }

        Map<Long, Long> conflitosPorRecurso = conflitosDoPeriodo.stream()
            .filter(c -> c.getRecurso() != null)
            .collect(Collectors.groupingBy(c -> c.getRecurso().getId(), Collectors.counting()));
        long conflitosEvitados = conflitosDoPeriodo.stream().map(ConflitoRegistrado::getTentativa).distinct().count();

        double horasEspacos = 0;
        double capacidadeEspacos = 0;
        List<LinhaRecurso> linhas = new ArrayList<>();
        for (Recurso r : recursos.findAllByOrderByNome()) {
            double horas = horasPorRecurso.getOrDefault(r.getId(), 0.0);
            long qtd = reservasPorRecurso.getOrDefault(r.getId(), 0L);
            long conf = conflitosPorRecurso.getOrDefault(r.getId(), 0L);
            if (!r.isAtivo() && horas == 0 && conf == 0) continue;
            double disponiveis = diasUteis * Duration.between(r.getAbertura(), r.getFechamento()).toMinutes() / 60.0;
            if (r.getTipo().espaco()) {
                horasEspacos += horas;
                capacidadeEspacos += disponiveis;
            }
            linhas.add(new LinhaRecurso(r.getId(), r.getNome(), r.getTipo().rotulo(), percentual(horas, disponiveis),
                arredondar(horas), qtd, conf));
        }
        linhas.sort(Comparator.comparingInt(LinhaRecurso::utilizacao).reversed().thenComparing(LinhaRecurso::nome));

        List<LinhaProfessor> linhasProfessor = professores.values().stream()
            .map(p -> new LinhaProfessor(p.getId(), p.getNome(), arredondar(horasPorProfessor.get(p.getId())),
                reservasPorProfessor.get(p.getId())))
            .sorted(Comparator.comparingDouble(LinhaProfessor::horas).reversed())
            .toList();

        return new Relatorio(inicio, fim, percentual(horasEspacos, capacidadeEspacos), arredondar(horasTotais),
            conflitosEvitados, doPeriodo.size(), linhas, linhasProfessor);
    }

    /** CSV com separador ";" e BOM para abrir corretamente no Excel. */
    public String csv(Relatorio rel) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(linha("Recurso", "Tipo", "Utilização (%)", "Horas alocadas", "Reservas", "Conflitos evitados"));
        for (LinhaRecurso l : rel.recursos()) {
            sb.append(linha(l.nome(), l.tipoRotulo(), String.valueOf(l.utilizacao()), formatar(l.horas()),
                String.valueOf(l.reservas()), String.valueOf(l.conflitos())));
        }
        sb.append(linha("Total do período " + rel.inicio() + " a " + rel.fim(), "",
            String.valueOf(rel.utilizacaoEspacos()), formatar(rel.horasAlocadas()), String.valueOf(rel.totalReservas()),
            String.valueOf(rel.conflitosEvitados())));
        return sb.toString();
    }

    private static String linha(String... valores) {
        return Arrays.stream(valores).map(v -> "\"" + v.replace("\"", "\"\"") + "\"")
            .collect(Collectors.joining(";")) + "\r\n";
    }

    private static String formatar(double v) {
        return String.valueOf(v).replace('.', ',');
    }

    private static int percentual(double parte, double total) {
        return total <= 0 ? 0 : (int) Math.round(Math.min(100, parte * 100 / total));
    }

    private static double arredondar(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
