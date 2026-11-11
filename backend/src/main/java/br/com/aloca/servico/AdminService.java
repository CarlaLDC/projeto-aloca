package br.com.aloca.servico;

import br.com.aloca.api.dto.AdminDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de recursos e professores (RF-02) e bloqueios de manutenção (RF-08). */
@Service
public class AdminService {

    private final RecursoRepositorio recursos;
    private final ProfessorRepositorio professores;
    private final BloqueioRepositorio bloqueios;
    private final Clock relogio;

    public AdminService(RecursoRepositorio recursos, ProfessorRepositorio professores, BloqueioRepositorio bloqueios,
                        Clock relogio) {
        this.recursos = recursos;
        this.professores = professores;
        this.bloqueios = bloqueios;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------------- recursos

    @Transactional(readOnly = true)
    public List<RecursoDto> listarRecursos() {
        return recursos.findAllByOrderByNome().stream().map(AdminService::dto).toList();
    }

    @Transactional
    public RecursoDto criarRecurso(RecursoRequest p) {
        validarHorario(p);
        return dto(recursos.save(new Recurso(p.nome().trim(), p.tipo(), p.categoria(), p.localizacao().trim(),
            p.capacidade(), p.ehRestrito(), p.abertura(), p.fechamento())));
    }

    @Transactional
    public RecursoDto alterarRecurso(Long id, RecursoRequest p) {
        validarHorario(p);
        Recurso r = recurso(id);
        r.atualizar(p.nome().trim(), p.tipo(), p.categoria(), p.localizacao().trim(), p.capacidade(), p.ehRestrito(),
            p.abertura(), p.fechamento());
        return dto(r);
    }

    /** Desativa em vez de apagar, para preservar o histórico das reservas. */
    @Transactional
    public RecursoDto definirAtivo(Long id, boolean ativo) {
        Recurso r = recurso(id);
        r.setAtivo(ativo);
        return dto(r);
    }

    // ---------------------------------------------------------------- professores

    @Transactional(readOnly = true)
    public List<ProfessorDto> listarProfessores() {
        return professores.findAllByOrderByNome().stream().map(AdminService::dto).toList();
    }

    @Transactional
    public ProfessorDto criarProfessor(ProfessorRequest p) {
        return dto(professores.save(new Professor(p.nome().trim(), p.competencia().trim(), null)));
    }

    @Transactional
    public ProfessorDto alterarProfessor(Long id, ProfessorRequest p) {
        Professor prof = professor(id);
        prof.atualizar(p.nome().trim(), p.competencia().trim());
        return dto(prof);
    }

    @Transactional
    public ProfessorDto definirProfessorAtivo(Long id, boolean ativo) {
        Professor prof = professor(id);
        prof.setAtivo(ativo);
        return dto(prof);
    }

    // ---------------------------------------------------------------- bloqueios

    @Transactional(readOnly = true)
    public List<BloqueioDto> listarBloqueios() {
        return bloqueios.findAllByOrderByInicioDesc().stream().map(AdminService::dto).toList();
    }

    /** Bloqueia o recurso. Reservas já existentes no período não são canceladas automaticamente. */
    @Transactional
    public BloqueioDto bloquear(BloqueioRequest p, Usuario admin) {
        if (!p.fim().isAfter(p.inicio())) {
            throw RegraNegocioException.invalida("O fim do bloqueio deve ser posterior ao início.");
        }
        if (p.fim().isBefore(LocalDateTime.now(relogio))) {
            throw RegraNegocioException.invalida("O bloqueio precisa terminar no futuro.");
        }
        Recurso r = recurso(p.recursoId());
        return dto(bloqueios.save(new Bloqueio(r, p.inicio(), p.fim(), p.motivo().trim(), admin)));
    }

    @Transactional
    public void removerBloqueio(Long id) {
        if (!bloqueios.existsById(id)) {
            throw RegraNegocioException.naoEncontrada("Bloqueio não encontrado.");
        }
        bloqueios.deleteById(id);
    }

    // ---------------------------------------------------------------- apoio

    private static void validarHorario(RecursoRequest p) {
        if (!p.fechamento().isAfter(p.abertura())) {
            throw RegraNegocioException.invalida("O fechamento deve ser posterior à abertura.");
        }
    }

    private Recurso recurso(Long id) {
        return recursos.findById(id).orElseThrow(() -> RegraNegocioException.naoEncontrada("Recurso não encontrado."));
    }

    private Professor professor(Long id) {
        return professores.findById(id).orElseThrow(() -> RegraNegocioException.naoEncontrada("Professor não encontrado."));
    }

    private static RecursoDto dto(Recurso r) {
        return new RecursoDto(r.getId(), r.getNome(), r.getTipo().name(), r.getCategoria(), r.getLocalizacao(),
            r.getCapacidade(), r.isRestrito(), r.isAtivo(), r.getAbertura(), r.getFechamento());
    }

    private static ProfessorDto dto(Professor p) {
        return new ProfessorDto(p.getId(), p.getNome(), p.getCompetencia(), p.isAtivo());
    }

    private static BloqueioDto dto(Bloqueio b) {
        return new BloqueioDto(b.getId(), b.getRecurso().getId(), b.getRecurso().getNome(), b.getInicio(), b.getFim(),
            b.getMotivo(), b.getCriadoPor() == null ? null : b.getCriadoPor().getNome());
    }
}
