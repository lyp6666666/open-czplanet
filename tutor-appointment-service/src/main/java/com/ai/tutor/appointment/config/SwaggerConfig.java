package com.ai.tutor.appointment.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/**
 * Swagger UI 页面
 * 👉 http://localhost:8080/swagger-ui.html
 *
 * OpenAPI JSON
 * 👉 http://localhost:8080/v3/api-docs
 */

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Tutor Appointment 接口文档")
                        .version("v1.0")
                        .description("AI Tutor 预约系统接口文档")
                        .contact(new Contact()
                                .name("lyp")
                                .url("http://www.lypsblog.top")
                                .email("yipenglu3@gmail.com")));
    }
}
