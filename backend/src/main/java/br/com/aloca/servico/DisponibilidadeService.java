package br.com.aloca.servico;

import br.com.aloca.api.dto.DisponibilidadeDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.BloqueioRepositorio;
import br.com.aloca.repositorio.ProfessorRepositorio;
import br.com.aloca.repositorio.RecursoRepositorio;
import br.com.aloca.repositorio.ReservaRepositorio;
import br.com.aloca.servico.RegrasDeHorario.Periodo;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pesquisa de recursos e professores e seus horários livres (RF-03). Tela 03. */
@Service
public class DisponibilidadeService {

    public static final String DISPONIVEL = "Disponível";
    public static final String OCUPADO = "Ocupado";
    public static final String MANUTENCAO = "Em manutenção";
    private static final Duration JANELA_MINIMA = Duration.ofMinutes(30);
    /** Expediente usado para a agenda dos professores. */
    public static final LocalTime INICIO_EXPEDIENTE = LocalTime.of(7, 0);
    public static final LocalTime FIM_EXPEDIENTE = LocalTime.of(22, 0);

    private final RecursoRepositorio recursos;
    private final ReservaRepositorio reservas;
    private final BloqueioRepositorio bloqueios;
    private final ProfessorRepositorio professores;
    private final Clock relogio;

    public DisponibilidadeService(RecursoRepositorio recursos, ReservaRepositorio reservas,
                                  BloqueioRepositorio bloqueios, ProfessorRepositorio professores, Clock relogio) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.bloqueios = bloqueios;
        this.professores = professores;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Disponibilidade consultar(LocalDate data, TipoRecurso tipo, String busca, Integer capacidadeMinima) {
        LocalDate dia = data == null ? LocalDate.now(relogio) : data;
        List<RecursoDisponibilidade> todos = calcular(dia);

        long disponiveis = todos.stream().filter(r -> DISPONIVEL.equals(r.status())).count();
        Resumo resumo = new Resumo(disponiveis, todos.size() - disponiveis, todos.size());

        List<RecursoDisponibilidade> filtrados = todos.stream()
            .filter(r -> tipo == null || r.tipo().equals(tipo.name()))
            .filter(r -> capacidadeMinima == null || r.capacidade() >= capacidadeMinima)
            .filter(r -> Textos.contem(r.nome() + " " + r.localizacao(), busca))
            .toList();
        return new Disponibilidade(dia, resumo, filtrados);
    }

    /**
     * Professores ativos com a agenda do dia. {@code competencia} filtra pela competência e
     * {@code busca}, pelo nome ou pela competência; os dois ignoram maiúsculas e acentos.
     */
    @Transactional(readOnly = true)
    public DisponibilidadeProfessores consultarProfessores(LocalDate data, String competencia, String busca) {
        LocalDate dia = data == null ? LocalDate.now(relogio) : data;
        LocalDateTime agora = LocalDateTime.now(relogio);

        Map<Long, List<Intervalo>> agenda = new HashMap<>();
        for (Reserva r : reservas.doDia(dia, StatusReserva.OCUPAM_AGENDA)) {
            if (r.getProfessor() != null) {
                agenda.computeIfAbsent(r.getProfessor().getId(), k -> new ArrayList<>())
                    .add(new Intervalo(r.getInicio(), r.getFim(), "Reservado · " + r.getAtividade() + " · " + r.getEspaco().getNome()));
            }
        }

        List<ProfessorDisponibilidade> todos = professores.findByAtivoTrueOrderByNome().stream()
            .map(p -> {
                List<Intervalo> ocupacoes = agenda.getOrDefault(p.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Intervalo::inicio)).toList();
                Situacao s = situacao(INICIO_EXPEDIENTE, FIM_EXPEDIENTE, dia, agora, ocupacoes);
                Intervalo proxima = s.livres().isEmpty() ? null : s.livres().getFirst();
                return new ProfessorDisponibilidade(p.getId(), p.getNome(), p.getCompetencia(),
                    s.ocupadoAgora() || s.livres().isEmpty() ? OCUPADO : DISPONIVEL,
                    proxima == null ? null : proxima.inicio(), proxima == null ? null : proxima.fim(),
                    ocupacoes, s.livres());
            })
            .toList();

        long disponiveis = todos.stream().filter(p -> DISPONIVEL.equals(p.status())).count();
        List<ProfessorDisponibilidade> filtrados = todos.stream()
            .filter(p -> Textos.contem(p.competencia(), competencia))
            .filter(p -> Textos.contem(p.nome() + " " + p.competencia(), busca))
            .toList();
        return new DisponibilidadeProfessores(dia, new Resumo(disponiveis, todos.size() - disponiveis, todos.size()), filtrados);
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

        Situacao situacao = situacao(r.getAbertura(), r.getFechamento(), dia, agora, ordenadas);
        List<Intervalo> livres = situacao.livres();
        boolean ocupadoAgora = situacao.ocupadoAgora();
        boolean manutencaoNoDia = ordenadas.stream().anyMatch(o -> o.motivo().startsWith("Manutenção")
            && RegrasDeHorario.cobre(new Periodo(o.inicio(), o.fim()), r.getAbertura(), r.getFechamento()));

        String status;
        if (manutencaoNoDia) status = MANUTENCAO;
        else if (ocupadoAgora || livres.isEmpty()) status = OCUPADO;
        else status = DISPONIVEL;

        Intervalo proxima = livres.isEmpty() ? null : livres.getFirst();
        return new RecursoDisponibilidade(r.getId(), r.getNome(), r.getTipo().name(), r.getTipo().rotulo(),
            r.getLocalizacao(), r.getCapacidade(), r.isRestrito(), status,
            proxima == null ? null : proxima.inicio(), proxima == null ? null : proxima.fim(), ordenadas, livres);
    }

    /** Janelas livres e se está ocupado agora, para um expediente e as ocupações do dia. */
    private record Situacao(List<Intervalo> livres, boolean ocupadoAgora) {}

    private static Situacao situacao(LocalTime abertura, LocalTime fechamento, LocalDate dia, LocalDateTime agora,
                                     List<Intervalo> ocupacoes) {
        // A busca de janelas livres começa na abertura ou, se o dia é hoje, no próximo múltiplo de 5 minutos.
        LocalTime inicioBusca = abertura;
        if (dia.isBefore(agora.toLocalDate())) {
            inicioBusca = fechamento;
        } else if (dia.equals(agora.toLocalDate())) {
            LocalTime agoraArredondado = RegrasDeHorario.arredondarParaCima(agora.toLocalTime(), 5);
            if (agoraArredondado.isAfter(inicioBusca)) inicioBusca = agoraArredondado;
        }
        List<Periodo> periodos = ocupacoes.stream().map(o -> new Periodo(o.inicio(), o.fim())).toList();
        List<Intervalo> livres = RegrasDeHorario.janelasLivres(inicioBusca, fechamento, periodos, JANELA_MINIMA)
            .stream().map(j -> new Intervalo(j.inicio(), j.fim(), "Livre")).toList();
        boolean ocupadoAgora = dia.equals(agora.toLocalDate())
            && periodos.stream().anyMatch(o -> RegrasDeHorario.contem(o, agora.toLocalTime()));
        return new Situacao(livres, ocupadoAgora);
    }
}
