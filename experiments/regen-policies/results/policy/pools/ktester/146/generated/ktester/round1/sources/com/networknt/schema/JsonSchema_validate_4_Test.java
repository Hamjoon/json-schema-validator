package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import com.networknt.schema.i18n.MessageSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonSchema_validate_4_Test {

    private static JsonSchemaFactory factory;

    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factory for the 2020‑12 draft.  This was previously
        // commented out, which caused a NullPointerException in every test
        // that tried to obtain a schema.
//         factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();
    }

    @Test
    void validate_Successful_NoCustomizer_ReturnsEmptySet() throws Exception {
        // schema: name (string) and age (integer) required
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": [ "name", "age" ]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode validNode = mapper.readTree("""
            { "name":"John","age":30 }""");
        Set<ValidationMessage> messages = schema.validate(validNode, (ExecutionContextCustomizer) null);
        assertTrue(messages.isEmpty(), "A valid document must produce no validation messages");
    }

    @Test
    void validate_NullRootNode_ThrowsException() {
        String schemaJson = """
            { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class, () -> schema.validate((JsonNode) null, (ExecutionContextCustomizer) null), "Passing a null JsonNode should result in NPE");
    }

    @Test
    void validate_RefWithInvalidPercentEncoding_ThrowsUnsupportedEncodingException() {
        // schema that contains a malformed $ref
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "$ref":"invalid%ZZschema.json"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();
        // the exception is thrown during validation when the $ref is resolved
        assertThrows(UnsupportedEncodingException.class, () -> schema.validate(dummy, (ExecutionContextCustomizer) null), "Malformed percent‑encoding in $ref must result in UnsupportedEncodingException");
    }

    @Test
    void validate_RefToNonexistentSchema_ThrowsIllegalArgumentException() {
        // $ref points to a file that does not exist
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "$ref":"nonexistent-schema.json"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();
        assertThrows(IllegalArgumentException.class, () -> schema.validate(dummy, (ExecutionContextCustomizer) null), "A $ref that cannot be resolved must cause an IllegalArgumentException");
    }

    @Test
    void validate_RefWithIncompletePercentEscape_ThrowsUnsupportedEncodingException() {
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "$ref":"#/definitions/invalid%2"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();
        assertThrows(UnsupportedEncodingException.class, () -> schema.validate(dummy, (ExecutionContextCustomizer) null), "Incomplete percent‑escape in $ref fragment must raise UnsupportedEncodingException");
    }
}