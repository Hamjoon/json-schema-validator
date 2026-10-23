```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PropertiesValidator#getSchemas()}.
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_getSchemas_Test {

    // ----- Mocks ------------------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    @Mock
    JsonSchema childSchema; // the schema that will be returned by ValidationContext#newSchema

    // ----- System under test -----------------------------------------------
    private PropertiesValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Prepare a simple schema node that contains two properties: "a" and "b".
        String schemaJson = """
                {
                  "properties": {
                    "a": { "type": "string" },
                    "b": { "type": "integer" }
                  }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(schemaJson);

        // Stub the ValidationContext so that each call to newSchema returns the same mock.
        // The real implementation would create a new JsonSchema for each property,
        // but for the purpose of this unit test we only need to verify that the map is
        // populated with the expected keys and that the returned objects are the ones
        // supplied by the mock.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema);

        // Instantiate the validator under test.
        validator = new PropertiesValidator("testPath", schemaNode, parentSchema, validationContext);
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
    void testGetSchemasReturnsMapWithAllDeclaredProperties() {
        // Act
        Map<String, JsonSchema> schemas = validator.getSchemas();

        // Assert – the map must contain exactly the two property names defined in the JSON.
        assertNotNull(schemas, "The schemas map should never be null");
        assertEquals(2, schemas.size(), "There should be two entries in the schemas map");
        assertTrue(schemas.containsKey("a"), "Map should contain property 'a'");
        assertTrue(schemas.containsKey("b"), "Map should contain property 'b'");

        // The values stored in the map must be the objects returned by the mocked ValidationContext.
        assertSame(childSchema, schemas.get("a"), "Schema for 'a' should be the mock returned by ValidationContext");
        assertSame(childSchema, schemas.get("b"), "Schema for 'b' should be the mock returned by ValidationContext");

        // Verify that ValidationContext#newSchema was invoked exactly twice – once per property.
        verify(validationContext, times(2))
                .newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
    }
}
```