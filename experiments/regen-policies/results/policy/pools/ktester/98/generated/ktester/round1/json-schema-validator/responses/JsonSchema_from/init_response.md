```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the static {@code JsonSchema.from(...)} factory method.
 *
 * <p>The tests verify that the method correctly creates a {@link JsonSchema}
 * instance using the supplied arguments without accessing any private members
 * of the target class.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonNode schemaNode;

    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonSchema mockParentSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal ValidationContext – we use the default meta‑schema
        // (draft‑2020‑12) and a default JsonSchemaFactory.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config);

        // A tiny schema node – an empty object is sufficient for construction.
        schemaNode = MAPPER.createObjectNode();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void from_WithParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                schemaNode,
                mockParentSchema,
                false);

        // Assert
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(),
                "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_WithoutParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                schemaNode,
                null,               // no parent schema
                true);              // test the suppressSubSchemaRetrieval flag as true

        // Assert
        assertNotNull(result, "JsonSchema.from should not return null when parent is null");
        assertSame(validationContext, result.getValidationContext(),
                "The created JsonSchema must expose the same ValidationContext that was supplied");
    }
}
```