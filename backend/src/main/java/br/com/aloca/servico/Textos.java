package br.com.aloca.servico;

import java.text.Normalizer;
import java.util.Locale;

/** Comparação de textos de busca sem diferenciar maiúsculas, acentos e espaços extras. */
public final class Textos {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Textos() {}

    /** "  Matemática  Aplicada" → "matematica aplicada". Nulo vira texto vazio. */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(PT_BR).trim().replaceAll("\\s+", " ");
    }

    /** O texto contém o termo buscado? Termo vazio sempre combina. */
    public static boolean contem(String texto, String termo) {
        String t = normalizar(termo);
        return t.isEmpty() || normalizar(texto).contains(t);
    }
}
