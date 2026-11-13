package br.com.aloca.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class NotificacaoDtos {
    private NotificacaoDtos() {}

    public record NotificacaoDto(Long id, String mensagem, String codigoReserva, LocalDateTime criadaEm, boolean lida) {}

    public record Notificacoes(long naoLidas, List<NotificacaoDto> itens) {}
}
