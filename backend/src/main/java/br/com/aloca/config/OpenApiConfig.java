package br.com.aloca.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação da API (RF-13), gerada a partir do código pelo springdoc:
 * <ul>
 *   <li>{@code /v3/api-docs}: especificação OpenAPI em JSON;</li>
 *   <li>{@code /swagger-ui.html}: página para consultar e testar os endpoints.</li>
 * </ul>
 * Para testar rotas protegidas no Swagger: faça POST /api/auth/login, copie o {@code token}
 * e cole no botão "Authorize".
 */
@Configuration
public class OpenApiConfig {

    public static final String JWT = "jwt";

    @Bean
    OpenAPI documentacaoAloca() {
        return new OpenAPI()
            .info(new Info()
                .title("Aloca · API de reservas")
                .version("0.1.0")
                .description("""
                    Reservas de salas, professores e materiais sem conflito de horário.

                    **Autenticação:** todas as rotas, exceto o login, exigem `Authorization: Bearer <token>`.
                    O token vem de `POST /api/auth/login` e vale 8 horas.

                    **Perfis:** Solicitante, Responsável, Administrador e Professor. Cada rota informa quem pode usá-la;
                    sem permissão a resposta é **403**, sem login é **401**.

                    **Erros:** sempre no formato `{"mensagem": "...", "campos": {...}}`, com texto pronto para mostrar ao usuário.
                    Conflitos de horário respondem **409** com os conflitos e até 3 alternativas livres.

                    **Datas e horas:** `yyyy-MM-dd` e `HH:mm`, no horário de Brasília."""))
            .components(new Components().addSecuritySchemes(JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token devolvido por POST /api/auth/login")))
            .addSecurityItem(new SecurityRequirement().addList(JWT));
    }
}
