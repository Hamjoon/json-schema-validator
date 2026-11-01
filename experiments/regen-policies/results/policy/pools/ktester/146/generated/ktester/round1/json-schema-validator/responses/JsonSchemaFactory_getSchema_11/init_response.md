```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

class JsonSchemaFactory_getSchema_11_Test {

    private JsonSchemaFactory factory;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Create a factory instance that uses the 2020‑12 draft.  The default
         * configuration is sufficient for the simple schemas used in the
         * tests, therefore we rely on the static factory method.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void getSchema_withJsonFormat_returnsValidSchema() throws Exception {
        // GIVEN a simple JSON schema and the JSON input format
        String jsonSchema = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;

        // WHEN the factory creates a JsonSchema from the string
        JsonSchema schema = factory.getSchema(jsonSchema, InputFormat.JSON);

        // THEN the schema object is not null
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance");

        // AND a valid instance validates without errors
        JsonNode validInstance = MAPPER.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> errors = schema.validate(validInstance);
        assertTrue(errors.isEmpty(), "A valid JSON instance must produce no validation messages");

        // AND an invalid instance produces at least one validation message
        JsonNode invalidInstance = MAPPER.readTree("{\"name\":123}");
        Set<ValidationMessage> invalidErrors = schema.validate(invalidInstance);
        assertFalse(invalidErrors.isEmpty(), "An invalid JSON instance must produce validation messages");
    }

    @Test
    void getSchema_withYamlFormat_returnsValidSchema() throws Exception {
        // GIVEN a simple YAML schema and the YAML input format
        String yamlSchema = """
                type: object
                properties:
                  age:
                    type: integer
                required:
                  - age
                """;

        // WHEN the factory creates a JsonSchema from the YAML string
        JsonSchema schema = factory.getSchema(yamlSchema, InputFormat.YAML);

        // THEN the schema object is not null
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance for YAML input");

        // AND a valid instance validates without errors
        JsonNode validInstance = MAPPER.readTree("{\"age\":30}");
        Set<ValidationMessage> errors = schema.validate(validInstance);
        assertTrue(errors.isEmpty(), "A valid YAML‑derived schema must accept a correct JSON instance");

        // AND an invalid instance produces validation errors
        JsonNode invalidInstance = MAPPER.readTree("{\"age\":\"thirty\"}");
        Set<ValidationMessage> invalidErrors = schema.validate(invalidInstance);
        assertFalse(invalidErrors.isEmpty(), "A wrong type should be reported by the validator");
    }

    @Test
    void getSchema_withEmptyString_throwsIllegalArgumentException() {
        // GIVEN an empty schema string
        String emptySchema = "";

        // WHEN the factory is asked to parse it, THEN an IllegalArgumentException is expected
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(emptySchema, InputFormat.JSON),
                "Empty schema strings should result in an IllegalArgumentException");
    }

    @Test
    void getSchema_withNullString_throwsNullPointerException() {
        // GIVEN a null schema reference
        String nullSchema = null;

        // WHEN the factory is asked to parse it, THEN a NullPointerException is expected
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullSchema, InputFormat.JSON),
                "Null schema strings should result in a NullPointerException");
    }
}
```