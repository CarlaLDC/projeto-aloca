package br.com.aloca.config;

import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.*;
import java.time.*;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reservas de demonstração: histórico das últimas três semanas (para os relatórios),
 * reservas de hoje e dos próximos dias, conflitos evitados e notificações.
 * As datas são relativas ao dia atual para as telas sempre terem conteúdo.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "aloca.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosReservas implements ApplicationRunner {

    private static final String SISTEMA = "Sistema";

    private final ReservaRepositorio reservas;
    private final RecursoRepositorio recursos;
    private final ProfessorRepositorio professores;
    private final UsuarioRepositorio usuarios;
    private final ConflitoRegistradoRepositorio conflitos;
    private final NotificacaoRepositorio notificacoes;
    private final Clock relogio;

    private LocalDateTime agora;

    public DadosReservas(ReservaRepositorio reservas, RecursoRepositorio recursos, ProfessorRepositorio professores,
                         UsuarioRepositorio usuarios, ConflitoRegistradoRepositorio conflitos,
                         NotificacaoRepositorio notificacoes, Clock relogio) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.professores = professores;
        this.usuarios = usuarios;
        this.conflitos = conflitos;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (reservas.count() > 0 || recursos.count() == 0 || usuarios.count() == 0) return;
        agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);
        LocalDate hoje = agora.toLocalDate();

        Usuario ana = usuario("ana.martins@aloca.edu.br");
        Usuario mariana = usuario("mariana.costa@aloca.edu.br");
        Usuario roberto = usuario("roberto.almeida@aloca.edu.br");

        Recurso sala204 = recurso("Sala 204"), sala108 = recurso("Sala 108"), reunioes = recurso("Sala de reuniões");
        Recurso auditorio = recurso("Auditório"), lab02 = recurso("Laboratório de informática 02");
        Recurso lab03 = recurso("Laboratório de informática 03");
        Recurso projetor04 = recurso("Projetor 04"), epson = recurso("Projetor multimídia Epson");
        Recurso notebook08 = recurso("Notebook 08"), dell = recurso("Notebook Dell · patrimônio 0482");
        Recurso robotica = recurso("Kit de robótica educacional"), som = recurso("Caixa de som amplificada");

        Professor andre = professor("André Lima"), lucas = professor("Lucas Oliveira");
        Professor ricardo = professor("Ricardo Alves"), juliana = professor("Juliana Lima");

        // ---------------------------------------------------------- histórico das últimas semanas
        Recurso[] espacos = {sala204, sala108, lab02, lab03, auditorio, reunioes};
        Professor[] docentes = {andre, lucas, ricardo, juliana};
        String[] atividades = {"Aula de Programação Web", "Aula de Cálculo", "Oficina de robótica", "Reunião pedagógica",
            "Projeto integrador", "Monitoria"};
        Random aleatorio = new Random(42);
        for (int dia = 21; dia >= 1; dia--) {
            LocalDate d = hoje.minusDays(dia);
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            int quantidade = 2 + aleatorio.nextInt(3);
            for (int i = 0; i < quantidade; i++) {
                Recurso espaco = espacos[(dia + i) % espacos.length];
                LocalTime ini = LocalTime.of(8 + i * 4, 0);
                Professor prof = espaco == reunioes ? null : docentes[(dia + i) % docentes.length];
                List<Recurso> mats = i == 0 ? List.of(projetor04) : List.of();
                Reserva r = reserva(atividades[(dia + i) % atividades.length], d, ini, ini.plusHours(2), espaco, prof,
                    mats, i % 2 == 0 ? ana : mariana, d.minusDays(2).atTime(10, 0));
                aprovar(r, espaco.isRestrito() ? roberto : null, d.minusDays(2).atTime(11, 0));
                r.mudarStatus(StatusReserva.EM_USO, SISTEMA, null, "Em uso", "Período da reserva iniciado", r.inicioEm());
                r.mudarStatus(StatusReserva.CONCLUIDA, SISTEMA, null, "Concluída", "Período da reserva encerrado", r.fimEm());
            }
            if (dia % 4 == 0) conflito(d, lab02, ana);
            if (dia % 6 == 0) conflito(d, sala204, mariana);
        }

        // ---------------------------------------------------------- hoje e próximos dias
        Reserva aula = reserva("Aula de Programação Web", hoje, LocalTime.of(9, 0), LocalTime.of(11, 0), lab02, andre,
            List.of(projetor04), ana, agora.minusDays(1).withHour(16).withMinute(42));
        aprovar(aula, roberto, agora.minusDays(1).withHour(17).withMinute(5));

        Reserva integrador = reserva("Projeto integrador", hoje, LocalTime.of(14, 0), LocalTime.of(16, 0), sala204, lucas,
            List.of(notebook08), ana, agora.minusDays(2));
        aprovar(integrador, null, agora.minusDays(2));

        Reserva apresentacao = reserva("Apresentação final", hoje, LocalTime.of(19, 0), LocalTime.of(21, 0), auditorio,
            andre, List.of(som), ana, agora.minusDays(3));
        aprovar(apresentacao, roberto, agora.minusDays(3).plusHours(2));

        Reserva reuniao = reserva("Reunião de planejamento", hoje, LocalTime.of(8, 0), LocalTime.of(9, 30), reunioes, null,
            List.of(dell), mariana, agora.minusDays(1));
        aprovar(reuniao, null, agora.minusDays(1));

        Reserva workshop = reserva("Workshop de robótica", hoje.plusDays(1), LocalTime.of(14, 0), LocalTime.of(17, 0),
            lab02, ricardo, List.of(robotica), ana, agora.minusMinutes(25));
        notificar(roberto, "Nova solicitação " + workshop.getCodigo() + " aguarda sua aprovação.", workshop);

        Reserva colegiado = reserva("Reunião de colegiado", hoje.plusDays(2), LocalTime.of(10, 0), LocalTime.of(12, 0),
            auditorio, null, List.of(epson), mariana, agora.minusHours(3));
        notificar(roberto, "Nova solicitação " + colegiado.getCodigo() + " aguarda sua aprovação.", colegiado);

        Reserva quimica = reserva("Aula prática de Química", hoje.plusDays(3), LocalTime.of(8, 0), LocalTime.of(10, 0),
            sala108, juliana, List.of(epson), mariana, agora.minusDays(1));
        aprovar(quimica, null, agora.minusDays(1));

        Reserva calculo = reserva("Aula de Cálculo", hoje.plusDays(3), LocalTime.of(19, 0), LocalTime.of(21, 0), sala108,
            lucas, List.of(), ana, agora.minusHours(5));
        aprovar(calculo, null, agora.minusHours(5));

        Reserva cancelada = reserva("Reserva de Auditório", hoje.minusDays(2), LocalTime.of(10, 0), LocalTime.of(12, 0),
            auditorio, null, List.of(), ana, agora.minusDays(6));
        cancelada.mudarStatus(StatusReserva.CANCELADA, ana.getNome(), ana, "Cancelada", "Atividade remarcada",
            agora.minusDays(4));

        Reserva rejeitada = reserva("Oficina de robótica", hoje.plusDays(5), LocalTime.of(15, 0), LocalTime.of(17, 0),
            lab02, ricardo, List.of(robotica), mariana, agora.minusDays(1));
        rejeitada.setMotivoRejeicao("Kit em revisão nesta semana");
        rejeitada.mudarStatus(StatusReserva.REJEITADA, roberto.getNome(), roberto, "Rejeitada",
            "Kit em revisão nesta semana", agora.minusHours(20));

        notificar(ana, "Reserva " + aula.getCodigo() + " aprovada.", aula);
        notificar(ana, "Reserva " + integrador.getCodigo() + " confirmada.", integrador);
        notificar(mariana, "Sua solicitação " + rejeitada.getCodigo() + " foi rejeitada: Kit em revisão nesta semana", rejeitada);
    }

    // -------------------------------------------------------------- auxiliares

    private Usuario usuario(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow();
    }

    private Recurso recurso(String nome) {
        return recursos.findAllByOrderByNome().stream().filter(r -> r.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private Professor professor(String nome) {
        return professores.findAllByOrderByNome().stream().filter(p -> p.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private Reserva reserva(String atividade, LocalDate data, LocalTime inicio, LocalTime fim, Recurso espaco,
                            Professor professor, List<Recurso> materiais, Usuario solicitante, LocalDateTime criadaEm) {
        Reserva r = reservas.save(new Reserva(atividade, data, inicio, fim, espaco, professor, materiais, solicitante, criadaEm));
        r.setCodigo("RES-%04d".formatted(r.getId()));
        r.registrar(new HistoricoReserva(r, criadaEm, solicitante.getNome(), solicitante, "Solicitada", "Reserva criada",
            null, StatusReserva.SOLICITADA));
        return r;
    }

    private void aprovar(Reserva r, Usuario responsavel, LocalDateTime quando) {
        if (responsavel == null) {
            r.mudarStatus(StatusReserva.APROVADA, SISTEMA, null, "Aprovada",
                "Recursos sem restrição: aprovada automaticamente", quando);
        } else {
            r.mudarStatus(StatusReserva.APROVADA, responsavel.getNome(), responsavel, "Aprovada", "Alocação validada", quando);
        }
        r.registrar(new HistoricoReserva(r, quando, SISTEMA, null, "Notificação",
            "Enviada a " + r.getSolicitante().getNome() + " · simulação", r.getStatus(), r.getStatus()));
    }

    private void conflito(LocalDate data, Recurso recurso, Usuario quem) {
        conflitos.save(new ConflitoRegistrado(UUID.randomUUID().toString(), data.minusDays(1).atTime(15, 0), data,
            recurso, null, quem));
    }

    private void notificar(Usuario u, String mensagem, Reserva r) {
        notificacoes.save(new Notificacao(u, mensagem, r.getCodigo(), agora.minusMinutes(30)));
    }
}
