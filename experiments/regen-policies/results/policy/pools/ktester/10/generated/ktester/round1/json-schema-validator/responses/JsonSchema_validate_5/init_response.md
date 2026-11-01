```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.OutputFormat;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_5_Test {

    /** Shared objects that can be reused across test methods. */
    private static JsonSchemaFactory schemaFactory;
    private static JsonMapperFactory mapperFactory;

    /** Instance under test. */
    private JsonSchema jsonSchema;

    /** Sample JSON instance that conforms to the schema. */
    private JsonNode validInstance;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factories once for the whole test suite.
        schemaFactory = JsonSchemaFactory.getInstance();
        mapperFactory = JsonMapperFactory.getInstance();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN: a simple JSON schema that requires an object with a single
        //        string property called "name".
        // --------------------------------------------------------------------
        String schemaJson = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;

        JsonNode schemaNode = mapperFactory.readTree(schemaJson);
        // JsonSchemaFactory builds a JsonSchema instance using the public API.
        jsonSchema = schemaFactory.getSchema(schemaNode);

        // --------------------------------------------------------------------
        // AND: a valid JSON document that satisfies the schema.
        // --------------------------------------------------------------------
        String validJson = """
                {
                  "name": "John Doe"
                }
                """;
        validInstance = mapperFactory.readTree(validJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test case.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to dispose globally.
    }

    @Test
    void shouldReturnEmptyResultWhenInstanceIsValid() {
        // --------------------------------------------------------------------
        // WHEN: the validate(JsonNode, OutputFormat) method is invoked with the
        //       default output format.
        // --------------------------------------------------------------------
        Set<ValidationMessage> result = jsonSchema.validate(validInstance, OutputFormat.DEFAULT);

        // --------------------------------------------------------------------
        // THEN: the validation result must be empty (i.e., no errors).
        // --------------------------------------------------------------------
        assertNotNull(result, "Result set should never be null");
        assertTrue(result.isEmpty(), "Expected no validation errors for a valid instance");
    }
}
```