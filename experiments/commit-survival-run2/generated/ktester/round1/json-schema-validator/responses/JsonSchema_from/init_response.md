```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    // Common test data
    private static final String SCHEMA_PATH = "$.definitions.test";
    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");

    @Mock
    ValidationContext validationContext;               // mocked dependency
    @Mock
    JsonMetaSchema metaSchema;                         // returned by validationContext.getMetaSchema()
    @Mock
    JsonSchema parentSchema;                           // parent schema (can be null in tests)
    @Mock
    SchemaValidatorsConfig config;                     // configuration object inside ValidationContext
    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext; // for OpenAPI3 style discriminators

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these unit tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing that is common to most tests
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to avoid cross‑test interference
        clearInvocations(validationContext, metaSchema, parentSchema, config, discriminatorContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that the static {@code from} method creates a non‑null {@link JsonSchema}
     * instance when the {@link ValidationContext} does not provide a {@link SchemaValidatorsConfig}
     * (i.e. {@code getConfig()} returns {@code null}).
     */
    @Test
    void from_WithNullConfig_ReturnsJsonSchema() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(null);
        JsonNode schemaNode = mock(JsonNode.class);

        // Act
        JsonSchema result = JsonSchema.from(validationContext, SCHEMA_PATH, CURRENT_URI, schemaNode, parentSchema, false);

        // Assert
        assertNotNull(result, "JsonSchema.from should never return null");
        // No further interaction verification is required – the constructor should simply complete.
    }

    /**
     * Verify that when a non‑null {@link SchemaValidatorsConfig} is supplied,
     * the constructor queries the configuration for keyword walk listeners.
     */
    @Test
    void from_WithNonNullConfig_InvokesKeywordWalkListenerSetup() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        JsonNode schemaNode = mock(JsonNode.class);

        // Act
        JsonSchema result = JsonSchema.from(validationContext, SCHEMA_PATH, CURRENT_URI, schemaNode, parentSchema, false);

        // Assert
        assertNotNull(result, "JsonSchema.from should produce a JsonSchema instance");
        // Verify that the config was consulted for the keyword‑listener map
        verify(config, times(1)).getKeywordWalkListenersMap();
        // Since discriminators are disabled, no further discriminator interaction should happen
        verify(config, never()).isOpenAPI3StyleDiscriminators();
    }

    /**
     * Verify the OpenAPI‑3 style discriminator registration path.
     * When the configuration enables discriminators and the schema node contains a
     * {@code discriminator} property, the {@link ValidationContext.DiscriminatorContext}
     * should be asked to register it.
     */
    @Test
    void from_WithOpenApiDiscriminators_RegistersDiscriminator() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discriminatorContext);

        // Build a minimal ObjectNode that contains a "discriminator" field
        ObjectNode schemaNode = new ObjectNode(JsonNodeFactory.instance);
        ObjectNode discriminatorNode = new ObjectNode(JsonNodeFactory.instance);
        discriminatorNode.put("propertyName", "type");
        schemaNode.set("discriminator", discriminatorNode);

        // Act
        JsonSchema result = JsonSchema.from(validationContext, SCHEMA_PATH, CURRENT_URI, schemaNode, parentSchema, false);

        // Assert
        assertNotNull(result, "JsonSchema.from should still return a JsonSchema instance");
        // Verify that the discriminator registration was attempted
        verify(discriminatorContext, times(1))
                .registerDiscriminator(eq(SCHEMA_PATH), eq(discriminatorNode));
    }
}
```