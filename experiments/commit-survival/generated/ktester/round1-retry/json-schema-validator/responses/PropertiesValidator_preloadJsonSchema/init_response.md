```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PropertiesValidator#preloadJsonSchema()}.
 *
 * The test verifies that the method delegates to {@code preloadJsonSchemas}
 * which in turn calls {@code preloadJsonSchema()} on each {@link JsonSchema}
 * instance stored in the internal {@code schemas} map.
 *
 * A {@link ValidationContext} mock is used so that the {@code newSchema}
 * calls performed in the {@link PropertiesValidator} constructor return
 * controllable {@link JsonSchema} mocks. After invoking {@code preloadJsonSchema}
 * we assert that each mock received exactly one call to its own
 * {@code preloadJsonSchema()} method.
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_preloadJsonSchema_Test {

    /** Shared Jackson mapper for creating {@link JsonNode}s from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Mocked validation context supplied to the {@link PropertiesValidator} constructor. */
    @Mock
    ValidationContext validationContext;

    /** Holds the {@link JsonSchema} mocks that are created by the {@code newSchema} stub. */
    private final List<JsonSchema> createdSchemas = new ArrayList<>();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub ValidationContext#newSchema to return a fresh mock JsonSchema for each call
        // and keep a reference to the mock for later verification.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(invocation -> {
                    JsonSchema mockSchema = mock(JsonSchema.class);
                    createdSchemas.add(mockSchema);
                    return mockSchema;
                });
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the list of created schema mocks for the next test method.
        createdSchemas.clear();
        // Clear interactions on the mocked ValidationContext.
        clearInvocations(validationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void testPreloadJsonSchemaInvokesPreloadOnEachSchema() throws Exception {
        // GIVEN – a simple schema node containing two properties
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "first": { "type": "string" },
                    "second": { "type": "integer" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);

        // WHEN – constructing the validator (the constructor populates the internal map)
        PropertiesValidator validator = new PropertiesValidator(
                "testSchema",          // schemaPath
                schemaNode,           // schemaNode
                null,                 // parentSchema
                validationContext     // mocked ValidationContext
        );

        // Sanity check – the internal map should contain exactly the two mocks we created.
        assertEquals(2, validator.getSchemas().size(),
                "Validator should have created a JsonSchema mock for each property");

        // AND – invoking preloadJsonSchema()
        validator.preloadJsonSchema();

        // THEN – each created JsonSchema mock must have received a single call to preloadJsonSchema()
        for (JsonSchema mockSchema : createdSchemas) {
            verify(mockSchema, times(1)).preloadJsonSchema();
        }

        // Also verify that no extra interactions occurred on the mocks.
        verifyNoMoreInteractions(createdSchemas.toArray());
    }
}
```