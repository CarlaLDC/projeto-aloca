package br.com.aloca.seguranca;

import br.com.aloca.dominio.Perfil;
import br.com.aloca.dominio.Usuario;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Emite e valida os tokens JWT (HS256) usados pelo front-end. */
@Service
public class TokenService {

    private final SecretKey chave;
    private final Duration validade;
    private final JwtEncoder encoder;

    public TokenService(@Value("${aloca.jwt.segredo}") String segredo,
                        @Value("${aloca.jwt.validade:PT8H}") Duration validade) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("aloca.jwt.segredo precisa ter pelo menos 32 caracteres");
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
        this.validade = validade;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    public Token emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("aloca")
            .subject(String.valueOf(usuario.getId()))
            .issuedAt(agora)
            .expiresAt(expira)
            .claim("nome", usuario.getNome())
            .claim("versao", usuario.getVersaoToken())
            .claim("perfis", usuario.getPerfis().stream().map(Perfil::name).sorted().toList())
            .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new Token(valor, expira);
    }

    public JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public record Token(String valor, Instant expiraEm) {}
}
