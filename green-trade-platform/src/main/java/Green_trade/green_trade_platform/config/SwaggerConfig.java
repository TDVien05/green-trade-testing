package Green_trade.green_trade_platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;


@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Green trade platform API")
                        .description("API cho hệ thống thương mại điện tử")
                        .version("1.0.0"))
                // Cấu hình JWT Bearer cho Swagger
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                );
    }

    @Bean
    public OperationCustomizer customizeMultipart() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            RequestBody requestBody = operation.getRequestBody();
            if (requestBody != null && requestBody.getContent() != null &&
                    requestBody.getContent().containsKey("multipart/form-data")) {

                // Set schema using new (non-deprecated) style
                Map<String, Schema> properties = new LinkedHashMap<>();
                properties.put("file", new BinarySchema().description("File to upload"));
                properties.put("data", new StringSchema().description("JSON string of the object"));

                Schema<?> multipartSchema = new ObjectSchema();
                multipartSchema.setProperties(properties);
                multipartSchema.setRequired(Arrays.asList("file", "data"));

                MediaType mediaType = requestBody.getContent().get("multipart/form-data");
                mediaType.setSchema(multipartSchema);
            }
            return operation;
        };
    }

}
