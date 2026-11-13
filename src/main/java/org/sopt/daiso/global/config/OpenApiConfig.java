package org.sopt.daiso.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Map;
import org.sopt.daiso.global.exception.ErrorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI daisoOpenApi() {
        return new OpenAPI()
                .info(new Info().title("다이소 API").version("v1")
                        .description("인증 없이 사용하는 실습 API입니다. ID는 조회 결과의 값을 사용하세요. "
                                + "좋아요·매장 찜은 공유 상태이며, Try it out으로 실행한 변경은 실제 저장됩니다."))
                .servers(List.of(new Server().url("/")))
                .tags(List.of(new Tag().name("상품"), new Tag().name("매장")))
                .externalDocs(new ExternalDocumentation().description("API 계약")
                        .url("https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/blob/dev/docs/api/api-contract.md"))
                .components(new Components()
                        .addResponses("InternalServerError", error(ErrorType.INTERNAL_SERVER_ERROR))
                        .addResponses("ProductNotFound", error(ErrorType.PRODUCT_NOT_FOUND))
                        .addResponses("StoreNotFound", error(ErrorType.STORE_NOT_FOUND))
                        .addResponses("StorageUnavailable", error(ErrorType.STORAGE_UNAVAILABLE))
                        .addResponses("MethodNotAllowed", error(ErrorType.METHOD_NOT_ALLOWED))
                        .addResponses("UnsupportedMediaType", error(ErrorType.UNSUPPORTED_MEDIA_TYPE)));
    }

    private ApiResponse error(ErrorType type) {
        ApiResponse response = new ApiResponse().description(type.getMessage()).content(new Content()
                .addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                        .example(Map.of("code", type.getCode(), "message", type.getMessage()))));
        if (type == ErrorType.METHOD_NOT_ALLOWED) {
            response.addHeaderObject("Allow", new Header().description("허용되는 HTTP 메서드")
                    .schema(new Schema<String>().type("string")));
        } else if (type == ErrorType.UNSUPPORTED_MEDIA_TYPE) {
            response.addHeaderObject("Accept", new Header().description("지원하는 요청 본문의 Content-Type")
                    .schema(new Schema<String>().type("string")));
        }
        return response;
    }
}
