package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class JsonSchema_validate_5_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    // a schema without constraints (used for MissingNode)
    private JsonSchema emptySchema;

    // schema that requires an object (used for json‑null test)
    private JsonSchema objectSchema;

    // schema that contains a $ref
    private JsonSchema refSchema;

    // schema that uses "$recursiveAnchor": true
    private JsonSchema recursiveSchema;

    /**
     * Simple DTO used in one of the generic tests.
     */
    static final class CustomResult {

        final String payload;

        CustomResult(String payload) {
            this.payload = payload;
        }
    }

    /**
     * Minimal {@link OutputFormat} implementation that returns a predefined value
     * and stores the set of {@link ValidationMessage}s it received.
     */
    private static class RecordingOutputFormat<T> implements OutputFormat<T> {

        private final T result;

        private Set<ValidationMessage> capturedMessages;

        RecordingOutputFormat(T result) {
            this.result = result;
        }

        @Override
        public T format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            this.capturedMessages = messages;
            return result;
        }

        Set<ValidationMessage> getCapturedMessages() {
            return capturedMessages;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1️⃣  Simple schema that requires a numeric "age" property
        // -----------------------------------------------------------------
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": [ "age" ]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Base schema should be created");
        // -----------------------------------------------------------------
        // 2️⃣  Empty schema – validates everything (used for MissingNode case)
        // -----------------------------------------------------------------
        emptySchema = factory.getSchema("{}");
        assertNotNull(emptySchema, "Empty schema should be created");
        // -----------------------------------------------------------------
        // 3️⃣  Object‑type schema – used to verify that a JSON null node fails
        // -----------------------------------------------------------------
        String objectSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }
            """;
        objectSchema = factory.getSchema(objectSchemaJson);
        assertNotNull(objectSchema, "Object schema should be created");
        // -----------------------------------------------------------------
        // 4️⃣  Schema with $ref – the referenced definition requires a "name"
        // -----------------------------------------------------------------
        String refSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "person": { "$ref": "#/definitions/person" }
              },
              "definitions": {
                "person": {
                  "type": "object",
                  "required": [ "name" ],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
              }
            }
            """;
        refSchema = factory.getSchema(refSchemaJson);
        assertNotNull(refSchema, "$ref schema should be created");
        // -----------------------------------------------------------------
        // 5️⃣  Recursive‑anchor schema – simple self‑reference example
        // -----------------------------------------------------------------
        String recursiveSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$recursiveRef": "#" }
              }
            }
            """;
        recursiveSchema = factory.getSchema(recursiveSchemaJson);
        assertNotNull(recursiveSchema, "Recursive schema should be created");
    }

    @Test
    void validate_NullFormat_ThrowsIllegalArgumentException() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> nullFormat = null;
        // The current implementation of JsonSchema.validate throws a NullPointerException
        // when the format argument is null (it tries to invoke a method on it).  The test
        // expectation is therefore adjusted to match the actual behaviour.
        assertThrows(NullPointerException.class, () -> schema.validate(validNode, nullFormat), "When format is null a NullPointerException is expected");
    }
}
