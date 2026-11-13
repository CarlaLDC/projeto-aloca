package br.com.aloca.servico;

import br.com.aloca.api.dto.ReservaDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import br.com.aloca.servico.RegrasDeHorario.Periodo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de reserva: criação, alteração, cancelamento, conflitos e histórico.
 * Cobre RF-04 a RF-06, RF-10 e RF-11. As decisões do responsável ficam em {@link AprovacaoService}.
 */
@Service
public class ReservaService {

    private static final String SISTEMA = "Sistema";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final long SEM_ID = -1L;

    private final ReservaRepositorio reservas;
    private final RecursoRepositorio recursos;
    private final ProfessorRepositorio professores;
    private final BloqueioRepositorio bloqueios;
    private final ConflitoRegistradoRepositorio conflitosRegistrados;
    private final ApplicationEventPublisher eventos;
    private final NotificacaoService notificacoes;
    private final Mapeador mapeador;
    private final Clock relogio;

    public ReservaService(ReservaRepositorio reservas, RecursoRepositorio recursos, ProfessorRepositorio professores,
                          BloqueioRepositorio bloqueios, ConflitoRegistradoRepositorio conflitosRegistrados,
                          ApplicationEventPublisher eventos, NotificacaoService notificacoes, Mapeador mapeador,
                          Clock relogio) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.professores = professores;
        this.bloqueios = bloqueios;
        this.conflitosRegistrados = conflitosRegistrados;
        this.eventos = eventos;
        this.notificacoes = notificacoes;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    /** Resultado de criar ou alterar: ou a reserva gravada, ou o conflito que impediu a gravação. */
    public sealed interface Resultado permits Gravada, EmConflito {}
    public record Gravada(ReservaDetalhe reserva) implements Resultado {}
    public record EmConflito(ConflitoResposta conflito) implements Resultado {}

    /** Recursos já carregados (e travados) de um pedido. */
    private record Selecao(Recurso espaco, Professor professor, List<Recurso> materiais) {
        List<Long> recursoIds() {
            return Stream.concat(Stream.of(espaco), materiais.stream()).map(Recurso::getId).toList();
        }
        List<Recurso> todosRecursos() {
            return Stream.concat(Stream.of(espaco), materiais.stream()).toList();
        }
    }

    /** Conflito interno: guarda as entidades para registrar o "conflito evitado". */
    private record Colisao(Recurso recurso, Professor professor, ItemConflito item) {}

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public OpcoesReserva opcoes(Usuario usuario) {
        List<Recurso> ativos = recursos.findByAtivoTrueOrderByNome();
        Long professorDoUsuario = usuario.getPerfis().contains(Perfil.PROFESSOR)
            ? professores.findByUsuarioId(usuario.getId()).filter(Professor::isAtivo).map(Professor::getId).orElse(null)
            : null;
        return new OpcoesReserva(
            ativos.stream().filter(r -> r.getTipo().espaco()).map(mapeador::recurso).toList(),
            professores.findByAtivoTrueOrderByNome().stream().map(mapeador::professor).toList(),
            ativos.stream().filter(r -> !r.getTipo().espaco()).map(mapeador::recurso).toList(),
            professorDoUsuario);
    }

    /** Tela 06: reservas que o usuário pediu e, se for professor, as reservas em que ele está alocado. */
    @Transactional(readOnly = true)
    public List<ReservaResumo> minhas(Usuario usuario) {
        Map<Long, Reserva> unicas = new LinkedHashMap<>();
        reservas.findBySolicitanteIdOrderByDataDescInicioDesc(usuario.getId()).forEach(r -> unicas.put(r.getId(), r));
        if (usuario.possui(Perfil.PROFESSOR)) {
            reservas.doProfessor(usuario.getId()).forEach(r -> unicas.putIfAbsent(r.getId(), r));
        }
        return unicas.values().stream()
            .sorted(Comparator.comparing(Reserva::getData).thenComparing(Reserva::getInicio).reversed())
            .map(mapeador::resumo)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReservaDetalhe detalhe(String codigo, Usuario usuario) {
        Reserva r = buscar(codigo);
        if (!podeVer(r, usuario)) {
            throw RegraNegocioException.proibida("Você não tem acesso a esta reserva.");
        }
        return mapeador.detalhe(r, podeGerenciar(r, usuario));
    }

    // ------------------------------------------------------------------ criar / alterar / cancelar

    @Transactional
    public Resultado criar(ReservaRequest pedido, Usuario usuario) {
        validarHorario(pedido);
        Selecao sel = carregarETravar(pedido);
        validarRecursos(pedido, sel);

        List<Colisao> colisoes = colisoes(pedido.data(), pedido.inicio(), pedido.fim(), sel, SEM_ID);
        if (!colisoes.isEmpty()) {
            return emConflito(pedido, sel, colisoes, usuario, SEM_ID);
        }

        LocalDateTime agora = agora();
        Reserva r = reservas.save(new Reserva(pedido.atividade().trim(), pedido.data(), pedido.inicio(), pedido.fim(),
            sel.espaco(), sel.professor(), sel.materiais(), usuario, agora));
        r.setCodigo("RES-%04d".formatted(r.getId()));
        r.registrar(new HistoricoReserva(r, agora, usuario.getNome(), usuario, "Solicitada", "Reserva criada",
            null, StatusReserva.SOLICITADA));

        if (r.exigeAprovacao()) {
            notificacoes.notificarPerfil(Perfil.RESPONSAVEL,
                "Nova solicitação " + r.getCodigo() + " aguarda sua aprovação.", r);
            notificarSolicitante(r, "Solicitação " + r.getCodigo() + " enviada. Ela aguarda aprovação do responsável.", agora);
        } else {
            r.mudarStatus(StatusReserva.APROVADA, SISTEMA, null, "Aprovada",
                "Recursos sem restrição: aprovada automaticamente", agora);
            eventos.publishEvent(new ReservaEventos.Aprovada(r));
            notificarSolicitante(r, "Reserva " + r.getCodigo() + " confirmada.", agora);
        }
        return new Gravada(mapeador.detalhe(r, true));
    }

    @Transactional
    public Resultado alterar(String codigo, ReservaRequest pedido, Usuario usuario) {
        Reserva r = buscar(codigo);
        exigirGerenciavel(r, usuario, "alterar");
        validarHorario(pedido);
        Selecao sel = carregarETravar(pedido);
        validarRecursos(pedido, sel);

        List<Colisao> colisoes = colisoes(pedido.data(), pedido.inicio(), pedido.fim(), sel, r.getId());
        if (!colisoes.isEmpty()) {
            return emConflito(pedido, sel, colisoes, usuario, r.getId());
        }

        String mudancas = descreverMudancas(r, pedido, sel);
        if (mudancas.isEmpty()) {
            return new Gravada(mapeador.detalhe(r, true));
        }
        LocalDateTime agora = agora();
        StatusReserva antes = r.getStatus();
        r.alterarDados(pedido.atividade().trim(), pedido.data(), pedido.inicio(), pedido.fim(),
            sel.espaco(), sel.professor(), sel.materiais());

        if (r.exigeAprovacao()) {
            // Alterar uma reserva restrita exige nova análise do responsável.
            r.mudarStatus(StatusReserva.SOLICITADA, usuario.getNome(), usuario, "Alterada",
                mudancas + (antes == StatusReserva.APROVADA ? " · requer nova aprovação" : ""), agora);
            notificacoes.notificarPerfil(Perfil.RESPONSAVEL,
                "A solicitação " + r.getCodigo() + " foi alterada e aguarda aprovação.", r);
        } else {
            r.mudarStatus(antes, usuario.getNome(), usuario, "Alterada", mudancas, agora);
            if (antes == StatusReserva.SOLICITADA) {
                r.mudarStatus(StatusReserva.APROVADA, SISTEMA, null, "Aprovada",
                    "Recursos sem restrição: aprovada automaticamente", agora);
            }
        }
        eventos.publishEvent(new ReservaEventos.Alterada(r));
        notificarSolicitante(r, "Reserva " + r.getCodigo() + " alterada.", agora);
        return new Gravada(mapeador.detalhe(r, true));
    }

    @Transactional
    public ReservaDetalhe cancelar(String codigo, String motivo, Usuario usuario) {
        Reserva r = buscar(codigo);
        exigirGerenciavel(r, usuario, "cancelar");
        LocalDateTime agora = agora();
        r.mudarStatus(StatusReserva.CANCELADA, usuario.getNome(), usuario, "Cancelada", motivo.trim(), agora);
        eventos.publishEvent(new ReservaEventos.Cancelada(r));
        notificarSolicitante(r, "Reserva " + r.getCodigo() + " cancelada.", agora);
        return mapeador.detalhe(r, false);
    }

    /**
     * Ausência confirmada pelo responsável após 15 minutos do início e antes do encerramento.
     * Reservas com material já retirado não podem ser marcadas como ausência.
     */
    @Transactional
    public ReservaDetalhe marcarNaoComparecimento(String codigo, String motivo, Usuario responsavel) {
        Reserva r = buscar(codigo);
        if (!responsavel.possui(Perfil.RESPONSAVEL)) {
            throw RegraNegocioException.proibida("Somente o responsável pode registrar não comparecimento.");
        }
        LocalDateTime agora = agora();
        // Mesma regra do registro pela tela de aprovações (CicloDeVidaReserva): de 15 min após o início
        // até o fim, para reservas aprovadas ou em uso.
        CicloDeVidaReserva.impedimentoParaNaoComparecimento(r.getStatus(), agora, r.inicioEm(), r.fimEm())
            .ifPresent(motivoRecusa -> { throw RegraNegocioException.conflito(motivoRecusa); });
        // Ouvintes recusam a alteração se houve retirada de material.
        eventos.publishEvent(new ReservaEventos.AntesDeModificar(r));
        r.mudarStatus(StatusReserva.NAO_COMPARECEU, responsavel.getNome(), responsavel,
            "Não compareceu", motivo.trim(), agora);
        eventos.publishEvent(new ReservaEventos.Cancelada(r));
        notificarSolicitante(r, "Ausência registrada na reserva " + r.getCodigo() + ": " + motivo.trim(), agora);
        return mapeador.detalhe(r, false);
    }

    // ------------------------------------------------------------------ ciclo automático

    /** Marca reservas aprovadas como "Em uso" quando começam e "Concluída" quando terminam. */
    @Transactional
    public int atualizarCiclo() {
        LocalDateTime agora = agora();
        int alteradas = 0;
        for (Reserva r : reservas.ateData(agora.toLocalDate(), EnumSet.of(StatusReserva.APROVADA, StatusReserva.EM_USO))) {
            var proximo = CicloDeVidaReserva.proximoPeloRelogio(r.getStatus(), agora, r.inicioEm(), r.fimEm());
            if (proximo.isPresent()) {
                boolean concluida = proximo.get() == StatusReserva.CONCLUIDA;
                r.mudarStatus(proximo.get(), SISTEMA, null, concluida ? "Concluída" : "Em uso",
                    concluida ? "Período da reserva encerrado" : "Período da reserva iniciado", agora);
                alteradas++;
            }
        }
        return alteradas;
    }

    // ------------------------------------------------------------------ regras internas

    private void validarHorario(ReservaRequest p) {
        RegrasDeHorario.validarPedido(p.data(), p.inicio(), p.fim(), agora());
        if (p.materiais().size() != new HashSet<>(p.materiais()).size()) {
            throw RegraNegocioException.invalida("O mesmo material foi selecionado mais de uma vez.");
        }
    }

    /** Carrega os recursos e trava suas linhas até o fim da transação (RF-06). */
    private Selecao carregarETravar(ReservaRequest p) {
        List<Long> ids = Stream.concat(Stream.of(p.espacoId()), p.materiais().stream()).distinct().toList();
        Map<Long, Recurso> porId = recursos.travar(ids).stream()
            .collect(Collectors.toMap(Recurso::getId, r -> r));
        if (porId.size() != ids.size()) {
            throw RegraNegocioException.naoEncontrada("Um dos recursos selecionados não foi encontrado.");
        }
        Professor professor = null;
        if (p.professorId() != null) {
            professor = professores.travar(p.professorId())
                .orElseThrow(() -> RegraNegocioException.naoEncontrada("Professor não encontrado."));
        }
        List<Recurso> materiais = p.materiais().stream().map(porId::get).toList();
        return new Selecao(porId.get(p.espacoId()), professor, materiais);
    }

    /** Trava sala, materiais e professor da reserva até o fim da transação. */
    public void travarRecursos(Reserva r) {
        List<Long> ids = Stream.concat(Stream.of(r.getEspaco()), r.getMateriais().stream()).map(Recurso::getId).toList();
        recursos.travar(ids);
        if (r.getProfessor() != null) {
            professores.travar(r.getProfessor().getId());
        }
    }

    private void validarRecursos(ReservaRequest p, Selecao sel) {
        Recurso espaco = sel.espaco();
        if (!espaco.getTipo().espaco()) {
            throw RegraNegocioException.invalida("Selecione uma sala ou laboratório como local da reserva.");
        }
        for (Recurso r : sel.todosRecursos()) {
            if (!r.isAtivo()) {
                throw RegraNegocioException.invalida(r.getNome() + " está desativado e não pode ser reservado.");
            }
        }
        for (Recurso m : sel.materiais()) {
            if (m.getTipo().espaco()) {
                throw RegraNegocioException.invalida(m.getNome() + " não é um material. Selecione-o como local da reserva.");
            }
        }
        if (sel.professor() != null && !sel.professor().isAtivo()) {
            throw RegraNegocioException.invalida(sel.professor().getNome() + " está desativado e não pode ser alocado.");
        }
        if (!RegrasDeHorario.dentroDoFuncionamento(new Periodo(p.inicio(), p.fim()), espaco.getAbertura(), espaco.getFechamento())) {
            throw RegraNegocioException.invalida(espaco.getNome() + " funciona das " + espaco.getAbertura().format(HORA)
                + " às " + espaco.getFechamento().format(HORA) + ".");
        }
    }

    /** Reservas ativas e bloqueios que se sobrepõem ao período para sala, materiais ou professor. */
    private List<Colisao> colisoes(LocalDate data, LocalTime inicio, LocalTime fim, Selecao sel, long ignorarId) {
        List<Colisao> lista = new ArrayList<>();
        Set<Long> ids = new HashSet<>(sel.recursoIds());

        for (Bloqueio b : bloqueios.sobrepostos(ids, data.atTime(inicio), data.atTime(fim))) {
            LocalTime bi = b.getInicio().toLocalDate().isBefore(data) ? LocalTime.MIN : b.getInicio().toLocalTime();
            LocalTime bf = b.getFim().toLocalDate().isAfter(data) ? LocalTime.MAX : b.getFim().toLocalTime();
            lista.add(new Colisao(b.getRecurso(), null, new ItemConflito("Manutenção", b.getRecurso().getNome(),
                b.getRecurso().getLocalizacao(), bi.withNano(0), bf.withNano(0), b.getMotivo(), null,
                RegrasDeHorario.minutosSobrepostos(new Periodo(inicio, fim), new Periodo(bi, bf)))));
        }

        long professorId = sel.professor() == null ? SEM_ID : sel.professor().getId();
        for (Reserva r : reservas.conflitantes(data, inicio, fim, ids, professorId, ignorarId, StatusReserva.OCUPAM_AGENDA)) {
            long minutos = RegrasDeHorario.minutosSobrepostos(new Periodo(inicio, fim), new Periodo(r.getInicio(), r.getFim()));
            for (Recurso usado : Stream.concat(Stream.of(r.getEspaco()), r.getMateriais().stream()).toList()) {
                if (ids.contains(usado.getId())) {
                    lista.add(new Colisao(usado, null, new ItemConflito(usado.getTipo().espaco() ? "Sala" : "Material",
                        usado.getNome(), usado.getLocalizacao(), r.getInicio(), r.getFim(), r.getAtividade(),
                        r.getSolicitante().getNome(), minutos)));
                }
            }
            if (r.getProfessor() != null && r.getProfessor().getId() == professorId) {
                lista.add(new Colisao(null, r.getProfessor(), new ItemConflito("Professor", r.getProfessor().getNome(),
                    r.getEspaco().getNome(), r.getInicio(), r.getFim(), r.getAtividade(),
                    r.getSolicitante().getNome(), minutos)));
            }
        }
        return lista;
    }

    /** Descrição dos conflitos atuais de uma reserva já gravada (vazia quando está tudo livre). */
    public List<String> problemas(Reserva r) {
        Selecao sel = new Selecao(r.getEspaco(), r.getProfessor(), List.copyOf(r.getMateriais()));
        List<String> problemas = new ArrayList<>();
        for (Recurso recurso : sel.todosRecursos()) {
            if (!recurso.isAtivo()) {
                problemas.add(recurso.getNome() + " está desativado e não pode ser alocado");
            }
        }
        if (sel.professor() != null && !sel.professor().isAtivo()) {
            problemas.add(sel.professor().getNome() + " está desativado e não pode ser alocado");
        }
        colisoes(r.getData(), r.getInicio(), r.getFim(), sel, r.getId()).stream()
            .map(c -> descrever(c.item())).forEach(problemas::add);
        return List.copyOf(problemas);
    }

    private Resultado emConflito(ReservaRequest p, Selecao sel, List<Colisao> colisoes, Usuario usuario, long ignorarId) {
        String tentativa = UUID.randomUUID().toString();
        LocalDateTime agora = agora();
        colisoes.stream()
            .map(c -> c.recurso() != null ? "R" + c.recurso().getId() : "P" + c.professor().getId())
            .distinct()
            .forEach(chave -> {
                Colisao c = colisoes.stream()
                    .filter(x -> chave.equals(x.recurso() != null ? "R" + x.recurso().getId() : "P" + x.professor().getId()))
                    .findFirst().orElseThrow();
                conflitosRegistrados.save(new ConflitoRegistrado(tentativa, agora, p.data(), c.recurso(), c.professor(), usuario));
            });

        List<ItemConflito> itens = colisoes.stream().map(Colisao::item).toList();
        String mensagem = "Este horário não está disponível: " + descrever(itens.getFirst()) + ".";
        return new EmConflito(new ConflitoResposta(mensagem, p.atividade(), p.data(), p.inicio(), p.fim(),
            p.espacoId(), p.professorId(), p.materiais(), itens, alternativas(p, sel, colisoes, ignorarId)));
    }

    /**
     * Até 3 sugestões sem conflito: o mesmo local em outro horário do dia,
     * o mesmo horário nos próximos dias e outro local do mesmo tipo no mesmo horário.
     */
    private List<Alternativa> alternativas(ReservaRequest p, Selecao sel, List<Colisao> colisoes, long ignorarId) {
        Duration duracao = Duration.between(p.inicio(), p.fim());
        Recurso espaco = sel.espaco();
        List<Alternativa> sugestoes = new ArrayList<>();

        // 1) Mesmo local, mesmo dia, horário mais próximo do pedido.
        RegrasDeHorario.horariosAlternativos(espaco.getAbertura(), espaco.getFechamento(), duracao,
                Duration.ofMinutes(30), p.inicio()).stream()
            .filter(t -> livre(p.data(), t, t.plus(duracao), sel, ignorarId))
            .findFirst()
            .ifPresent(t -> sugestoes.add(alternativa(p.data(), t, t.plus(duracao), espaco, "Mesmo local, outro horário")));

        // 2) Mesmo local e horário nos próximos dias (exceto domingo).
        for (int i = 1; i <= 14 && sugestoes.size() < 2; i++) {
            LocalDate dia = p.data().plusDays(i);
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            if (livre(dia, p.inicio(), p.fim(), sel, ignorarId)) {
                sugestoes.add(alternativa(dia, p.inicio(), p.fim(), espaco, "Mesmo local, outro dia"));
                break;
            }
        }

        // 3) Outro local do mesmo tipo, com capacidade igual ou maior, no mesmo horário.
        //    Só adianta quando todos os conflitos são do próprio local: professor ou material ocupado
        //    continua ocupado em qualquer sala. Sem este corte, cada sala ativa era testada no banco
        //    sob a trava do pedido, e o 409 de professor/material demorava ~0,9 s (DEF-PERF-001).
        if (!somenteOLocalConflita(colisoes, espaco)) {
            return sugestoes.stream().limit(3).toList();
        }
        recursos.findByAtivoTrueOrderByNome().stream()
            .filter(r -> r.getTipo() == espaco.getTipo() && !r.getId().equals(espaco.getId()))
            .filter(r -> r.getCapacidade() >= espaco.getCapacidade())
            .filter(r -> RegrasDeHorario.dentroDoFuncionamento(new Periodo(p.inicio(), p.fim()), r.getAbertura(), r.getFechamento()))
            .filter(r -> livre(p.data(), p.inicio(), p.fim(), new Selecao(r, sel.professor(), sel.materiais()), ignorarId))
            .findFirst()
            .ifPresent(r -> sugestoes.add(alternativa(p.data(), p.inicio(), p.fim(), r, "Outro local, mesmo horário")));

        return sugestoes.stream().limit(3).toList();
    }

    private static boolean somenteOLocalConflita(List<Colisao> colisoes, Recurso espaco) {
        return colisoes.stream().allMatch(c -> c.recurso() != null && c.recurso().getId().equals(espaco.getId()));
    }

    private boolean livre(LocalDate data, LocalTime inicio, LocalTime fim, Selecao sel, long ignorarId) {
        LocalDateTime agora = agora();
        if (data.atTime(inicio).isBefore(agora)) return false;
        return colisoes(data, inicio, fim, sel, ignorarId).isEmpty();
    }

    private Alternativa alternativa(LocalDate data, LocalTime inicio, LocalTime fim, Recurso espaco, String motivo) {
        return new Alternativa(data, inicio, fim, espaco.getId(), espaco.getNome(), espaco.getLocalizacao(), motivo);
    }

    private static String descrever(ItemConflito c) {
        String periodo = c.inicio().format(HORA) + "–" + c.fim().format(HORA);
        return switch (c.tipo()) {
            case "Manutenção" -> c.recurso() + " está em manutenção (" + periodo + ")";
            case "Professor" -> c.recurso() + " já está alocado(a) em outra atividade (" + periodo + ")";
            default -> c.recurso() + " já está reservado(a) das " + periodo;
        };
    }

    private String descreverMudancas(Reserva r, ReservaRequest p, Selecao sel) {
        List<String> mudancas = new ArrayList<>();
        if (!r.getAtividade().equals(p.atividade().trim())) mudancas.add("atividade");
        if (!r.getData().equals(p.data())) mudancas.add("data " + r.getData().format(DATA) + " → " + p.data().format(DATA));
        if (!r.getInicio().equals(p.inicio()) || !r.getFim().equals(p.fim())) {
            mudancas.add("horário " + r.getInicio().format(HORA) + "–" + r.getFim().format(HORA) + " → "
                + p.inicio().format(HORA) + "–" + p.fim().format(HORA));
        }
        if (!r.getEspaco().getId().equals(sel.espaco().getId())) {
            mudancas.add("local " + r.getEspaco().getNome() + " → " + sel.espaco().getNome());
        }
        if (!Objects.equals(r.getProfessor() == null ? null : r.getProfessor().getId(),
                sel.professor() == null ? null : sel.professor().getId())) {
            mudancas.add("professor");
        }
        Set<Long> antes = r.getMateriais().stream().map(Recurso::getId).collect(Collectors.toSet());
        Set<Long> depois = sel.materiais().stream().map(Recurso::getId).collect(Collectors.toSet());
        if (!antes.equals(depois)) mudancas.add("materiais");
        if (mudancas.isEmpty()) return "";
        String texto = String.join("; ", mudancas);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private void exigirGerenciavel(Reserva r, Usuario usuario, String acao) {
        if (!r.getSolicitante().getId().equals(usuario.getId())) {
            throw RegraNegocioException.proibida("Você só pode " + acao + " reservas feitas por você.");
        }
        CicloDeVidaReserva.impedimentoParaGerenciar(r.getStatus(), agora(), r.inicioEm()).ifPresent(motivo -> {
            throw RegraNegocioException.conflito(motivo);
        });
        eventos.publishEvent(new ReservaEventos.AntesDeModificar(r));
    }

    private boolean podeVer(Reserva r, Usuario u) {
        return r.getSolicitante().getId().equals(u.getId())
            || u.possui(Perfil.RESPONSAVEL) || u.possui(Perfil.ADMINISTRADOR)
            || (r.getProfessor() != null && r.getProfessor().getUsuario() != null
                && r.getProfessor().getUsuario().getId().equals(u.getId()));
    }

    private boolean podeGerenciar(Reserva r, Usuario u) {
        return r.getSolicitante().getId().equals(u.getId()) && r.getStatus().gerenciavel() && agora().isBefore(r.inicioEm());
    }

    public void notificarSolicitante(Reserva r, String mensagem, LocalDateTime agora) {
        notificacoes.notificar(r.getSolicitante(), mensagem, r);
        r.registrar(new HistoricoReserva(r, agora, SISTEMA, null, "Notificação",
            "Enviada a " + r.getSolicitante().getNome() + " · simulação", r.getStatus(), r.getStatus()));
    }

    public Reserva buscar(String codigo) {
        return reservas.findByCodigo(codigo)
            .orElseThrow(() -> RegraNegocioException.naoEncontrada("Reserva " + codigo + " não encontrada."));
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }
}
