package br.com.aloca.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Tela 02 · Visão geral. */
public final class PainelDtos {
    private PainelDtos() {}

    public record ItemAgenda(String codigo, LocalTime inicio, LocalTime fim, String local, String atividade,
                             String status, String statusRotulo) {}

    public record Painel(String perfil, LocalDate data, long reservasHoje, long recursosDisponiveis,
                         long aguardandoAprovacao, List<ItemAgenda> agenda) {}
}
