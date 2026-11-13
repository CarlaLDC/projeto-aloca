package br.com.aloca.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final br.com.aloca.repositorio.UsuarioRepositorio usuarios;
    public SecurityConfig(br.com.aloca.repositorio.UsuarioRepositorio usuarios) { this.usuarios=usuarios; }


    private static final String NAO_AUTENTICADO =
        "{\"mensagem\":\"Sua sessão expirou ou você não entrou no sistema. Entre novamente.\"}";
    private static final String SEM_PERMISSAO =
        "{\"mensagem\":\"Seu perfil não tem permissão para esta ação.\"}";

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API sem sessão/cookies: autenticação só por token Bearer
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // console do H2
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/recuperar-senha").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // O perfil é conferido pela URL antes de validar o corpo (os controllers também usam @PreAuthorize).
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/aprovacoes/**", "/api/movimentacoes/**").hasRole("RESPONSAVEL")
                .requestMatchers("/api/relatorios/**").hasAnyRole("RESPONSAVEL", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/reservas", "/api/reservas/*/cancelamento")
                    .hasAnyRole("SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.PUT, "/api/reservas/*").hasAnyRole("SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR", "PROFESSOR")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorPerfis()))
                .authenticationEntryPoint((req, res, ex) -> responder(res, 401, NAO_AUTENTICADO))
                .accessDeniedHandler((req, res, ex) -> responder(res, 403, SEM_PERMISSAO)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> responder(res, 401, NAO_AUTENTICADO))
                .accessDeniedHandler((req, res, ex) -> responder(res, 403, SEM_PERMISSAO)));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(TokenService tokens) {
        return tokens.decoder();
    }

    /** Converte a claim "perfis" do token em papéis ROLE_SOLICITANTE, ROLE_RESPONSAVEL etc. */
    private org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt,
            org.springframework.security.authentication.AbstractAuthenticationToken> conversorPerfis() {
        return jwt -> {
            try {
                var u=usuarios.findById(Long.parseLong(jwt.getSubject())).filter(x->x.isAtivo()).orElseThrow();
                Number versao=jwt.getClaim("versao");
                if (versao==null || versao.longValue()!=u.getVersaoToken()) throw new IllegalArgumentException();
                var autoridades=u.getPerfis().stream().map(p->new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+p.name())).toList();
                return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt,autoridades);
            } catch (RuntimeException e) {
                throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("invalid_token");
            }
        };
    }

    private static void responder(jakarta.servlet.http.HttpServletResponse res, int status, String corpo)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(corpo);
    }
}
