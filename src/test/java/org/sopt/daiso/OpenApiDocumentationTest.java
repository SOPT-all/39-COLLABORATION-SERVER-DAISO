package org.sopt.daiso;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.global.response.ErrorResponse;
import org.sopt.daiso.product.dto.ProductLikeResponse;
import org.sopt.daiso.product.dto.ProductListItemResponse;
import org.sopt.daiso.product.dto.ProductListResponse;
import org.sopt.daiso.product.dto.ProductStoreResponse;
import org.sopt.daiso.product.dto.ProductStoresResponse;
import org.sopt.daiso.store.dto.StoreFavoriteResponse;
import org.sopt.daiso.store.entity.BusinessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration")
@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class OpenApiDocumentationTest {
    private static final Map<String, String> APIS = Map.of(
            "/api/v1/products", "get",
            "/api/v1/products/{productId}/like", "patch",
            "/api/v1/products/{productId}/stores", "get",
            "/api/v1/stores/{storeId}/favorite", "patch");

    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper mapper;
    private JsonNode document;

    @BeforeEach
    void loadDocument() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        document = mapper.readTree(json);
    }

    @Test
    void servesOnlyImplementedApisAndUiOnTheSameServer() throws Exception {
        assertThat(document.get("openapi").asString()).startsWith("3.1.");
        assertThat(document.get("paths").propertyNames()).containsExactlyInAnyOrderElementsOf(APIS.keySet());
        APIS.forEach((path, method) -> assertThat(document.get("paths").get(path).propertyNames()).containsExactly(method));
        assertThat(document.get("servers").get(0).get("url").asString()).isEqualTo("/");
        assertThat(document.has("security")).isFalse();
        assertThat(document.get("tags").valueStream().map(tag -> tag.get("name").asString()).toList())
                .containsExactlyInAnyOrder("상품", "매장");
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void documentsStrictRequiredBooleanBodies() {
        Map.of("/api/v1/products/{productId}/like", "liked", "/api/v1/stores/{storeId}/favorite", "favorite")
                .forEach((path, field) -> {
                    JsonNode body = operation(path).get("requestBody");
                    assertThat(body.get("required").asBoolean()).isTrue();
                    JsonNode object = resolve(body.get("content").get("application/json").get("schema"));
                    assertThat(object.get("properties").propertyNames()).containsExactly(field);
                    assertThat(object.get("required").valueStream().map(JsonNode::asString).toList()).containsExactly(field);
                    JsonNode value = object.get("properties").get(field);
                    assertThat(value.get("type").asString()).isEqualTo("boolean");
                    assertThat(value.get("example").isBoolean()).isTrue();
                    assertThat(value.has("default")).isFalse();
                });
    }

    @Test
    void documentsActualIdAndQueryContracts() {
        for (String path : APIS.keySet()) {
            for (JsonNode parameter : operation(path).path("parameters")) {
                String name = parameter.get("name").asString();
                JsonNode schema = parameter.get("schema");
                if (name.endsWith("Id")) {
                    assertThat(parameter.get("in").asString()).isEqualTo("path");
                    assertThat(parameter.get("required").asBoolean()).isTrue();
                    assertThat(schema.get("type").asString()).isEqualTo("integer");
                    assertThat(schema.get("format").asString()).isEqualTo("int64");
                    assertThat(schema.get("minimum").asLong()).isEqualTo(1);
                    assertThat(schema.get("maximum").asLong()).isEqualTo(Long.MAX_VALUE);
                } else {
                    assertThat(parameter.get("in").asString()).isEqualTo("query");
                    assertThat(parameter.get("required").asBoolean()).isFalse();
                    assertThat(schema.get("type").asString()).isEqualTo(name.equals("soldOut") ? "boolean" : "string");
                    if (name.equals("soldOut")) assertThat(schema.get("default").asBoolean()).isTrue();
                }
            }
        }
    }

    @Test
    void keepsConcreteResponseModelsAndAllSerializedFields() {
        var product = new ProductListItemResponse(1001L, "0012345", "수분 마스크팩", "1,000원",
                "https://example.com/product.png", "9999+", false);
        var store = new ProductStoreResponse(201L, "강남본점", "https://example.com/store.png",
                BusinessStatus.CLOSED, true, 1500, 0, false);
        var list = new ProductListResponse(List.of(product));
        var stores = new ProductStoresResponse(1001L, product, List.of(store));
        var liked = new ProductLikeResponse(1001L, true);
        var favorite = new StoreFavoriteResponse(201L, true);
        Map<String, Object> data = Map.of("/api/v1/products", list,
                "/api/v1/products/{productId}/like", liked,
                "/api/v1/products/{productId}/stores", stores,
                "/api/v1/stores/{storeId}/favorite", favorite);
        data.forEach((path, value) -> {
            JsonNode envelope = resolve(media(path, "200").get("schema"));
            assertFields(envelope, ApiResponse.success("성공", value));
            assertFields(resolve(envelope.get("properties").get("data")), value);
        });
        JsonNode listData = dataSchema("/api/v1/products");
        JsonNode productSchema = resolve(listData.get("properties").get("products").get("items"));
        assertFields(productSchema, product);
        JsonNode inventoryData = dataSchema("/api/v1/products/{productId}/stores");
        assertFields(resolve(inventoryData.get("properties").get("product")), product);
        JsonNode storeSchema = resolve(inventoryData.get("properties").get("stores").get("items"));
        assertFields(storeSchema, store);
        assertThat(storeSchema.get("properties").get("distance").get("readOnly").asBoolean()).isTrue();
        assertThat(storeSchema.get("properties").get("businessStatus").get("enum")
                .valueStream().map(JsonNode::asString).toList()).containsExactlyInAnyOrder("OPEN", "CLOSED");
        assertThat(document.get("components").get("schemas").propertyNames())
                .doesNotContain("Product", "Store", "StoreInventory", "ProductStoresRequest", "JsonNode");
    }

    @Test
    void examplesPreserveJsonTypesAndComputedValues() {
        APIS.keySet().forEach(path -> {
            JsonNode example = media(path, "200").get("example");
            assertThat(example.get("code").asString()).isEqualTo("SUCCESS");
            assertThat(example.get("data").isObject()).isTrue();
        });
        JsonNode product = media("/api/v1/products", "200").get("example").at("/data/products/0");
        assertThat(product.get("productNumber").asString()).isEqualTo("0012345");
        assertThat(product.get("price").asString()).isEqualTo("1,000원");
        assertThat(product.get("reviewCount").asString()).isEqualTo("9999+");
        JsonNode store = media("/api/v1/products/{productId}/stores", "200").get("example").at("/data/stores/0");
        assertThat(store.get("distanceMeters").asLong()).isEqualTo(1500);
        assertThat(store.get("distance").asString()).isEqualTo("1.5km");
    }

    @Test
    void errorsHaveOnlyErrorFieldsAndStorageFailureIsScoped() {
        APIS.keySet().forEach(path -> operation(path).get("responses").properties().forEach(response -> {
            if (response.getKey().equals("200")) return;
            JsonNode content = resolve(response.getValue()).get("content").get("application/json");
            assertFields(resolve(content.get("schema")), new ErrorResponse("ERROR", "오류"));
        }));
        assertThat(operation("/api/v1/products/{productId}/stores").get("responses").has("503")).isTrue();
        assertThat(operation("/api/v1/products").get("responses").propertyNames()).containsExactlyInAnyOrder("200", "405", "500");
        assertThat(operation("/api/v1/products/{productId}/like").get("responses").has("503")).isFalse();
    }

    private JsonNode operation(String path) {
        return document.get("paths").get(path).get(APIS.get(path));
    }

    private JsonNode media(String path, String status) {
        return resolve(operation(path).get("responses").get(status)).get("content").get("application/json");
    }

    private JsonNode dataSchema(String path) {
        return resolve(resolve(media(path, "200").get("schema")).get("properties").get("data"));
    }

    private JsonNode resolve(JsonNode node) {
        assertThat(node).as("스키마 또는 응답 참조가 존재해야 한다").isNotNull();
        while (node.has("$ref")) node = document.at(node.get("$ref").asString().substring(1));
        assertThat(node.isMissingNode()).isFalse();
        return node;
    }

    private void assertFields(JsonNode schema, Object response) {
        JsonNode json = mapper.valueToTree(response);
        assertThat(schema.get("properties").propertyNames()).containsExactlyInAnyOrderElementsOf(json.propertyNames());
        assertThat(schema.get("required").valueStream().map(JsonNode::asString).toList()).containsAll(json.propertyNames());
        json.properties().forEach(field -> {
            String expected = field.getValue().isString() ? "string" : field.getValue().isBoolean() ? "boolean"
                    : field.getValue().isIntegralNumber() ? "integer" : field.getValue().isArray() ? "array" : "object";
            assertThat(resolve(schema.get("properties").get(field.getKey())).get("type").asString())
                    .as(field.getKey()).isEqualTo(expected);
        });
    }
}
