```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * The tests focus on the public {@code walkAtNode} method which simply delegates to the
 * private {@code walkAtNodeInternal}.  By constructing a {@link JsonSchema} instance via the
 * static {@code from} factory method we avoid using reflection on the private constructor.
 *
 * Dependencies such as {@link ValidationContext}, {@link JsonMetaSchema},
 * {@link JsonSchemaFactory} and {@link SchemaValidatorsConfig} are mocked because they are
 * not the subject of these tests.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    // ---------- Mocks & shared test data ----------
    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private JsonMetaSchema mockMetaSchema;
    @Mock
    private JsonSchemaFactory mockJsonSchemaFactory;
    @Mock
    private SchemaValidatorsConfig mockConfig;

    private JsonSchema jsonSchema;          // class under test
    private ExecutionContext executionContext;
    private ObjectMapper mapper = new ObjectMapper();

    // ---------- Lifecycle ----------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- Configure the ValidationContext mock -----
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        // Return null for config – this disables the keywordWalkListenerRunner.
        when(mockValidationContext.getConfig()).thenReturn(null);

        // ----- Build a minimal schema node (empty object) -----
        JsonNode emptySchemaNode = mapper.readTree("{}");

        // ----- Create the JsonSchema instance via the public factory method -----
        // Parameters: validationContext, schemaPath, currentUri, schemaNode, parent, suppressSubSchemaRetrieval
        jsonSchema = JsonSchema.from(
                mockValidationContext,
                "$",                               // schemaPath (root)
                new URI("http://example.com/schema"), // currentUri – any valid URI works
                emptySchemaNode,
                null,                              // no parent schema
                false                              // do not suppress sub‑schema retrieval
        );

        // ----- Execution context for validation runs -----
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // ---------- Tests ----------
    @Test
    void walkAtNode_WithEmptySchemaAndSimpleNode_ReturnsEmptyValidationResult() throws Exception {
        // Given: a simple JSON instance node
        JsonNode instanceNode = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        JsonNode rootNode = instanceNode; // for this test the root is the same node

        // When: invoking walkAtNode (validation disabled)
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", false);

        // Then: result should be non‑null and contain no validation messages
        assertNotNull(result, "walkAtNode should never return null");
        assertTrue(result.getMessages().isEmpty(),
                "Expected no validation messages for an empty schema, but got: " + result.getMessages());
    }

    @Test
    void walkAtNode_WithEmptySchemaAndValidationEnabled_ReturnsEmptyValidationResult() throws Exception {
        // Given: a simple JSON instance node
        JsonNode instanceNode = mapper.readTree("[1,2,3]");
        JsonNode rootNode = instanceNode;

        // When: invoking walkAtNode with shouldValidateSchema = true
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", true);

        // Then: still no messages because the schema does not define any constraints
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "Even when validation is enabled, an empty schema should produce no messages.");
    }

    @Test
    void walkAtNode_WithNullNode_ReturnsEmptyValidationResult() {
        // Given: a null instance node (simulating missing data)
        JsonNode nullNode = null;
        JsonNode rootNode = mapper.createObjectNode(); // empty root

        // When: invoking walkAtNode
        ValidationResult result = jsonSchema.walkAtNode(executionContext, nullNode, rootNode, "", false);

        // Then: result should be non‑null and contain no messages (the method guards against nulls internally)
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "walkAtNode should handle null node gracefully and return an empty result.");
    }
}
```