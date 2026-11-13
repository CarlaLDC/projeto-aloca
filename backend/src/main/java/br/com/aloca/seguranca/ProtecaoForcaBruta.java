package br.com.aloca.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proteção contra força bruta no login (RNF-03).
 *
 * <p>Depois de {@code maxTentativas} senhas erradas para o mesmo e-mail dentro de {@code janela},
 * o e-mail fica bloqueado por {@code bloqueio}, mesmo que a senha certa seja informada.
 * Um login certo zera a contagem. E-mails inexistentes são tratados igual, para não revelar
 * quais contas existem.
 *
 * <p>O limite é por e-mail, não por IP: numa instituição muitas pessoas saem pelo mesmo IP,
 * e bloquear o IP impediria todos de entrar. A contagem fica em memória (uma instância do serviço).
 */
@Component
public class ProtecaoForcaBruta {

    private static final Logger log = LoggerFactory.getLogger(ProtecaoForcaBruta.class);
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private record Tentativas(int falhas, Instant primeiraFalha, Instant bloqueadoAte) {}

    private final ConcurrentHashMap<String, Tentativas> porEmail = new ConcurrentHashMap<>();
    private final int maxTentativas;
    private final Duration janela;
    private final Duration bloqueio;
    private final Clock relogio;

    public ProtecaoForcaBruta(@Value("${aloca.login.max-tentativas:5}") int maxTentativas,
                              @Value("${aloca.login.janela:PT15M}") Duration janela,
                              @Value("${aloca.login.bloqueio:PT15M}") Duration bloqueio,
                              Clock relogio) {
        if (maxTentativas < 1) throw new IllegalArgumentException("aloca.login.max-tentativas precisa ser pelo menos 1");
        this.maxTentativas = maxTentativas;
        this.janela = janela;
        this.bloqueio = bloqueio;
        this.relogio = relogio;
    }

    public Duration getBloqueio() {
        return bloqueio;
    }

    /** Quanto tempo falta para o e-mail ser liberado (vazio se não está bloqueado). */
    public Optional<Duration> tempoRestante(String email) {
        Tentativas t = porEmail.get(chave(email));
        Instant agora = relogio.instant();
        if (t == null || t.bloqueadoAte() == null || !agora.isBefore(t.bloqueadoAte())) return Optional.empty();
        return Optional.of(Duration.between(agora, t.bloqueadoAte()));
    }

    /**
     * Registra uma senha errada.
     *
     * @return true se esta falha bloqueou o e-mail
     */
    public boolean registrarFalha(String email) {
        Instant agora = relogio.instant();
        String chave = chave(email);
        Tentativas t = porEmail.compute(chave, (k, atual) -> {
            boolean recomecar = atual == null
                || (atual.bloqueadoAte() != null && !agora.isBefore(atual.bloqueadoAte()))
                || (atual.bloqueadoAte() == null && Duration.between(atual.primeiraFalha(), agora).compareTo(janela) > 0);
            int falhas = recomecar ? 1 : atual.falhas() + 1;
            Instant inicio = recomecar ? agora : atual.primeiraFalha();
            Instant ate = falhas >= maxTentativas ? agora.plus(bloqueio) : null;
            return new Tentativas(falhas, inicio, ate);
        });
        if (porEmail.size() > LIMPAR_ACIMA_DE) limparVencidos(agora);
        boolean bloqueou = t.falhas() == maxTentativas;
        if (bloqueou) {
            log.warn("Login bloqueado por {} após {} tentativas erradas: {}", bloqueio, t.falhas(), mascarar(chave));
        }
        return bloqueou;
    }

    /** Login certo: zera a contagem do e-mail. */
    public void registrarSucesso(String email) {
        porEmail.remove(chave(email));
    }

    /** Libera todos os e-mails (manutenção e testes). */
    public void limpar() {
        porEmail.clear();
    }

    private void limparVencidos(Instant agora) {
        porEmail.entrySet().removeIf(e -> {
            Tentativas t = e.getValue();
            Instant fim = t.bloqueadoAte() != null ? t.bloqueadoAte() : t.primeiraFalha().plus(janela);
            return !agora.isBefore(fim);
        });
    }

    private static String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** "ana.martins@aloca.edu.br" → "an***@aloca.edu.br" (não grava o e-mail inteiro no log). */
    static String mascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 2) return "***" + (arroba >= 0 ? email.substring(arroba) : "");
        return email.substring(0, 2) + "***" + email.substring(arroba);
    }
}
