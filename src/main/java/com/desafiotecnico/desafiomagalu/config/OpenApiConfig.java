package com.desafiotecnico.desafiomagalu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Desafio Magalu — API Pedidos")
                        .version("v1")
                        .description("API para upload de .txt legado retornando JSON padronizado")
                        .contact(new Contact().name("Lory Santana").email("lorysantana2106@gmail.com"))
                );
    }
}

