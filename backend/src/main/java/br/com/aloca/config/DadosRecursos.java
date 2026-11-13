package br.com.aloca.config;

import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Salas, laboratórios, equipamentos, professores e um bloqueio de manutenção de demonstração. */
@Component
@Order(2)
@ConditionalOnProperty(name = "aloca.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosRecursos implements ApplicationRunner {

    private final RecursoRepositorio recursos;
    private final ProfessorRepositorio professores;
    private final BloqueioRepositorio bloqueios;
    private final UsuarioRepositorio usuarios;
    private final Clock relogio;

    public DadosRecursos(RecursoRepositorio recursos, ProfessorRepositorio professores, BloqueioRepositorio bloqueios,
                         UsuarioRepositorio usuarios, Clock relogio) {
        this.recursos = recursos;
        this.professores = professores;
        this.bloqueios = bloqueios;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (recursos.count() > 0) return;
        LocalTime abre = LocalTime.of(7, 0), fecha = LocalTime.of(22, 0);
        LocalTime abreMat = LocalTime.of(8, 0), fechaMat = LocalTime.of(21, 0);

        // A ordem define os ids usados nos testes (1 = Sala 204, 5 = Laboratório 02, 8 = Projetor 04...).
        recurso("Sala 204", TipoRecurso.SALA, null, "Bloco A · 2º andar", 40, false, abre, fecha);
        recurso("Sala 108", TipoRecurso.SALA, null, "Bloco B · 1º andar", 24, false, abre, fecha);
        recurso("Sala de reuniões", TipoRecurso.SALA, null, "Bloco B · 2º andar", 12, false, abre, fecha);
        recurso("Auditório", TipoRecurso.SALA, null, "Bloco A · Térreo", 100, true, abre, fecha);
        recurso("Laboratório de informática 02", TipoRecurso.LABORATORIO, null, "Bloco C · 1º andar", 32, true, abre, fecha);
        recurso("Laboratório de informática 03", TipoRecurso.LABORATORIO, null, "Bloco C · 1º andar", 32, false, abre, fecha);
        Recurso labQuimica = recurso("Laboratório de química", TipoRecurso.LABORATORIO, null, "Bloco C · 2º andar", 20, true, abre, fecha);
        recurso("Projetor 04", TipoRecurso.EQUIPAMENTO, "Equipamentos", "Central de materiais", 1, false, abreMat, fechaMat);
        recurso("Projetor multimídia Epson", TipoRecurso.EQUIPAMENTO, "Equipamentos", "Central de materiais", 1, false, abreMat, fechaMat);
        recurso("Notebook 08", TipoRecurso.EQUIPAMENTO, "Informática", "Central de materiais", 1, false, abreMat, fechaMat);
        recurso("Notebook Dell · patrimônio 0482", TipoRecurso.EQUIPAMENTO, "Informática", "Central de materiais", 1, false, abreMat, fechaMat);
        recurso("Kit de robótica educacional", TipoRecurso.EQUIPAMENTO, "Equipamentos", "Central de materiais", 1, true, abreMat, fechaMat);
        recurso("Caixa de som amplificada", TipoRecurso.EQUIPAMENTO, "Áudio e vídeo", "Central de materiais", 1, false, abreMat, fechaMat);

        Usuario andre = usuarios.findByEmailIgnoreCase("andre.lima@aloca.edu.br").orElse(null);
        professores.save(new Professor("André Lima", "Desenvolvimento de sistemas", andre));
        professores.save(new Professor("Lucas Oliveira", "Matemática", null));
        professores.save(new Professor("Ricardo Alves", "Robótica educacional", null));
        Usuario juliana = usuarios.findByEmailIgnoreCase("juliana.lima@aloca.edu.br").orElse(null);
        professores.save(new Professor("Juliana Lima", "Química", juliana));

        Usuario patricia = usuarios.findByEmailIgnoreCase("patricia.souza@aloca.edu.br").orElse(null);
        LocalDate hoje = LocalDate.now(relogio);
        bloqueios.save(new Bloqueio(labQuimica, hoje.atStartOfDay(), hoje.plusDays(2).atStartOfDay(),
            "Manutenção da capela de exaustão", patricia));
    }

    private Recurso recurso(String nome, TipoRecurso tipo, String categoria, String local, int capacidade,
                            boolean restrito, LocalTime abre, LocalTime fecha) {
        return recursos.save(new Recurso(nome, tipo, categoria, local, capacidade, restrito, abre, fecha));
    }
}
