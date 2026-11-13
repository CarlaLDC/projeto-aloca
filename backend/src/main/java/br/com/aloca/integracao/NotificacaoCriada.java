package br.com.aloca.integracao;

/** Publicado quando o sistema cria uma notificação para um usuário. */
public record NotificacaoCriada(String email, String nome, String mensagem, String codigoReserva) {}
