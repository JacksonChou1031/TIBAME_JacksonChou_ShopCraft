package com.jackson.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI ecommerceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ecommerce MVP API")
                        .version("v1")
                        .description("Interview demo marketplace API. Write requests require the X-XSRF-TOKEN header."))
                .components(new Components().addSecuritySchemes("cookieAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("ECOMMERCE_AUTH")
                        .description("JWT authentication cookie issued by POST /api/v1/auth/login"))
                        .addSchemas("ApiErrorResponse", new Schema<>()
                                .type("object")
                                .description("Standard error returned by the API")
                                .addProperty("timestamp", new StringSchema().format("date-time"))
                                .addProperty("status", new IntegerSchema().example(400))
                                .addProperty("code", new StringSchema().example("VALIDATION_ERROR"))
                                .addProperty("message", new StringSchema().example("email: must be a well-formed email address"))
                                .addProperty("path", new StringSchema().example("/api/v1/auth/register"))));
    }

    @Bean
    OpenApiCustomizer csrfHeaderDocumentation() {
        return openAPI -> openAPI.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    boolean writeOperation = switch (method) {
                        case POST, PUT, PATCH, DELETE -> true;
                        default -> false;
                    };
                    if (writeOperation) {
                        operation.addParametersItem(new Parameter()
                                .name("X-XSRF-TOKEN")
                                .in("header")
                                .required(true)
                                .description("Required for every write request; obtain it from GET /api/v1/auth/csrf"));
                    }
                    if (operation.getRequestBody() != null || operation.getSecurity() != null) {
                        addErrorResponse(operation, "400", "Validation or business request error");
                    }
                    if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                        addErrorResponse(operation, "401", "Authentication is required");
                        addErrorResponse(operation, "403", "The current role is not allowed");
                    }
                }));
    }

    private void addErrorResponse(Operation operation, String status, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.addApiResponse(status, errorResponse(description));
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))));
    }
}
