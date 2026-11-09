package br.com.aloca.servico;

import br.com.aloca.api.dto.DisponibilidadeDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.BloqueioRepositorio;
import br.com.aloca.repositorio.RecursoRepositorio;
import br.com.aloca.repositorio.ReservaRepositorio;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pesquisa de recursos e horários livres (RF-03). Tela 03. */
@Service
public class DisponibilidadeService {

    public static final String DISPONIVEL = "Disponível";
    public static final String OCUPADO = "Ocupado";
    public static final String MANUTENCAO = "Em manutenção";
    private static final Duration JANELA_MINIMA = Duration.ofMinutes(30);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final RecursoRepositorio recursos;
    private final ReservaRepositorio reservas;
    private final BloqueioRepositorio bloqueios;
    private final Clock relogio;

    public DisponibilidadeService(RecursoRepositorio recursos, ReservaRepositorio reservas,
                                  BloqueioRepositorio bloqueios, Clock relogio) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.bloqueios = bloqueios;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Disponibilidade consultar(LocalDate data, TipoRecurso tipo, String busca, Integer capacidadeMinima) {
        LocalDate dia = data == null ? LocalDate.now(relogio) : data;
        List<RecursoDisponibilidade> todos = calcular(dia);

        long disponiveis = todos.stream().filter(r -> DISPONIVEL.equals(r.status())).count();
        Resumo resumo = new Resumo(disponiveis, todos.size() - disponiveis, todos.size());

        String termo = busca == null ? "" : busca.trim().toLowerCase(PT_BR);
        List<RecursoDisponibilidade> filtrados = todos.stream()
            .filter(r -> tipo == null || r.tipo().equals(tipo.name()))
            .filter(r -> capacidadeMinima == null || r.capacidade() >= capacidadeMinima)
            .filter(r -> termo.isEmpty() || (r.nome() + " " + r.localizacao()).toLowerCase(PT_BR).contains(termo))
            .toList();
        return new Disponibilidade(dia, resumo, filtrados);
    }

    @Transactional(readOnly = true)
    public long disponiveisHoje() {
        return calcular(LocalDate.now(relogio)).stream().filter(r -> DISPONIVEL.equals(r.status())).count();
    }

    private List<RecursoDisponibilidade> calcular(LocalDate dia) {
        Map<Long, List<Intervalo>> ocupacoes = new HashMap<>();
        for (Reserva r : reservas.doDia(dia, StatusReserva.OCUPAM_AGENDA)) {
            Intervalo i = new Intervalo(r.getInicio(), r.getFim(), "Reservado · " + r.getAtividade());
            ocupacoes.computeIfAbsent(r.getEspaco().getId(), k -> new ArrayList<>()).add(i);
            r.getMateriais().forEach(m -> ocupacoes.computeIfAbsent(m.getId(), k -> new ArrayList<>()).add(i));
        }
        for (Bloqueio b : bloqueios.noIntervalo(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())) {
            LocalTime inicio = b.getInicio().toLocalDate().isBefore(dia) ? LocalTime.MIN : b.getInicio().toLocalTime();
            LocalTime fim = b.getFim().toLocalDate().isAfter(dia) ? LocalTime.MAX.withNano(0) : b.getFim().toLocalTime();
            ocupacoes.computeIfAbsent(b.getRecurso().getId(), k -> new ArrayList<>())
                .add(new Intervalo(inicio, fim, "Manutenção · " + b.getMotivo()));
        }

        LocalDateTime agora = LocalDateTime.now(relogio);
        return recursos.findByAtivoTrueOrderByNome().stream()
            .map(r -> montar(r, dia, agora, ocupacoes.getOrDefault(r.getId(), List.of())))
            .toList();
    }

    private RecursoDisponibilidade montar(Recurso r, LocalDate dia, LocalDateTime agora, List<Intervalo> ocupacoes) {
        List<Intervalo> ordenadas = ocupacoes.stream().sorted(Comparator.comparing(Intervalo::inicio)).toList();

        // A busca de janelas livres começa na abertura ou, se o dia é hoje, no próximo múltiplo de 5 minutos.
        LocalTime cursor = r.getAbertura();
        if (dia.isBefore(agora.toLocalDate())) {
            cursor = r.getFechamento();
        } else if (dia.equals(agora.toLocalDate())) {
            LocalTime agoraArredondado = arredondar(agora.toLocalTime());
            if (agoraArredondado.isAfter(cursor)) cursor = agoraArredondado;
        }

        List<Intervalo> livres = new ArrayList<>();
        for (Intervalo o : ordenadas) {
            if (o.inicio().isAfter(cursor)) {
                LocalTime fimJanela = o.inicio().isBefore(r.getFechamento()) ? o.inicio() : r.getFechamento();
                adicionarJanela(livres, cursor, fimJanela);
            }
            if (o.fim().isAfter(cursor)) cursor = o.fim();
        }
        adicionarJanela(livres, cursor, r.getFechamento());

        boolean ocupadoAgora = dia.equals(agora.toLocalDate()) && ordenadas.stream()
            .anyMatch(o -> !agora.toLocalTime().isBefore(o.inicio()) && agora.toLocalTime().isBefore(o.fim()));
        boolean manutencaoNoDia = ordenadas.stream().anyMatch(o -> o.motivo().startsWith("Manutenção")
            && !o.inicio().isAfter(r.getAbertura()) && !o.fim().isBefore(r.getFechamento()));

        String status;
        if (manutencaoNoDia) status = MANUTENCAO;
        else if (ocupadoAgora || livres.isEmpty()) status = OCUPADO;
        else status = DISPONIVEL;

        Intervalo proxima = livres.isEmpty() ? null : livres.getFirst();
        return new RecursoDisponibilidade(r.getId(), r.getNome(), r.getTipo().name(), r.getTipo().rotulo(),
            r.getLocalizacao(), r.getCapacidade(), r.isRestrito(), status,
            proxima == null ? null : proxima.inicio(), proxima == null ? null : proxima.fim(), ordenadas, livres);
    }

    private static void adicionarJanela(List<Intervalo> livres, LocalTime inicio, LocalTime fim) {
        if (Duration.between(inicio, fim).compareTo(JANELA_MINIMA) >= 0) {
            livres.add(new Intervalo(inicio, fim, "Livre"));
        }
    }

    private static LocalTime arredondar(LocalTime t) {
        int minutos = t.getHour() * 60 + t.getMinute();
        int arredondado = ((minutos + 4) / 5) * 5;
        return arredondado >= 24 * 60 ? LocalTime.MAX.withNano(0) : LocalTime.of(arredondado / 60, arredondado % 60);
    }
}
