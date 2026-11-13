package br.com.aloca;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/** Nos testes, lê as respostas como UTF-8 (o JSON não informa charset e o padrão do MockMvc é ISO-8859-1). */
@TestConfiguration(proxyBeanMethods = false)
public class MockMvcUtf8 {

    @Bean
    MockMvcBuilderCustomizer utf8() {
        return builder -> builder.defaultResponseCharacterEncoding(StandardCharsets.UTF_8);
    }
}
