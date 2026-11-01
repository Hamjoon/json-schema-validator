```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The test builds a {@link JsonSchema} instance using the package‑private {@code JsonSchema.from(...)}
 * factory method. All collaborators required by the constructor are mocked, keeping the
 * focus on the public {@code walkAtNode} method.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    // -----------------------------------------------------------------------
    // Mocks required for constructing a JsonSchema instance
    // -----------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory jsonSchemaFactory;
    @Mock SchemaValidatorsConfig config;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;          // the schema definition node
    @Mock JsonNode instanceNode;        // node that will be walked
    @Mock JsonNode rootNode;            // root of the instance document
    @Mock JsonNodePath instanceLocation; // location of the instance node
    @Mock ExecutionContext executionContext;

    // -----------------------------------------------------------------------
    // Object under test
    // -----------------------------------------------------------------------
    JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mock behaviour required by the JsonSchema constructor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources())
                .thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors())
                .thenReturn(new ConcurrentHashMap<>());

        // Meta‑schema methods used during construction.
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);

        // Build the JsonSchema instance via the package‑private factory method.
        // The parent schema is null and sub‑schema retrieval is not suppressed.
        jsonSchema = JsonSchema.from(validationContext,
                                    schemaLocation,
                                    evaluationPath,
                                    schemaNode,
                                    null,
                                    false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code walkAtNode} executes without throwing and returns a
     * non‑null {@link ValidationResult}. The internal walk logic is exercised
     * with all collaborators mocked, so the focus is on the method contract.
     */
    @Test
    void walkAtNode_returnsNonNullResult() {
        // Act
        ValidationResult result = jsonSchema.walkAtNode(
                executionContext,
                instanceNode,
                rootNode,
                instanceLocation,
                true); // request validation while walking

        // Assert
        assertNotNull(result, "walkAtNode should return a ValidationResult instance");
    }

    /**
     * Same as the previous test but with validation disabled. The method should
     * still return a {@link ValidationResult} (which may be empty).
     */
    @Test
    void walkAtNode_withoutValidation_returnsNonNullResult() {
        ValidationResult result = jsonSchema.walkAtNode(
                executionContext,
                instanceNode,
                rootNode,
                instanceLocation,
                false); // walk without performing validation

        assertNotNull(result, "walkAtNode should return a ValidationResult even when validation is disabled");
    }
}
```