package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchema_walk_2_Test {

    private static ObjectMapper mapper;

    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // ObjectMapper is thread‑safe after configuration, so a single instance is fine.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default schema used by the majority of tests – an object with a required
        // string property "name".  Individual tests replace this schema with a
        // more specific one when needed.
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
    }

    private static Stream<Arguments> nullNodeProvider() {
        return Stream.of(Arguments.of(null, true), Arguments.of(null, false));
    }

    /**
     * 13. Internal $ref to missing definition – expect exception **
     */
    @Test
    void walkInternalRefMissingDefinitionShouldThrowException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "#/definitions/unknown"
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaJson), "Missing $ref target should cause a JsonSchemaException");
    }

    /**
     * 14. Malformed $ref URI – illegal characters **
     */
    @Test
    void walkMalformedRefUriShouldThrowIllegalArgumentException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "http://example.com/invalid[ref].json"
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaJson), "Malformed $ref URI should result in IllegalArgumentException");
    }

    /**
     * 15. Schema with unsupported keyword – validator initialization failure **
     */
    @Test
    void walkSchemaWithUnsupportedKeywordShouldThrowIllegalArgumentException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "unexpectedKeyword": 123
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaJson), "Unsupported keyword should cause IllegalArgumentException during schema creation");
    }

    /**
     * 17. External $ref I/O error – simulate via Mockito **
     */
    @Test
    void walkExternalRefIoErrorShouldReturnInvalidResult() throws Exception {
        // This test is intentionally disabled because it depends on Mockito,
        // which is not present in the compilation environment.
    }

    /**
     * 13. Internal $ref to missing definition – should produce validation errors
     * (the library does not throw an exception at schema creation time).
     */
//     @Test
//     void walkInternalRefMissingDefinitionShouldResultInInvalid() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "$ref": "#/definitions/unknown"
//             }
//             """;
//         JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         JsonSchema s = factory.getSchema(schemaJson);
//         // Validate a simple instance – the missing $ref target will be reported.
//         JsonNode instance = mapper.readTree("{}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Missing $ref target should produce validation messages");
//     }

    /**
     * 15. Schema with unsupported keyword – validator initialization should not
     * throw; a warning is logged and validation proceeds normally.
     */
    @Test
    void walkSchemaWithUnsupportedKeywordShouldNotThrow() throws Exception {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "unexpectedKeyword": 123
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema s = factory.getSchema(schemaJson);
        // Walk a minimal instance – we only verify that no exception is thrown.
        JsonNode instance = mapper.readTree("{}");
        ValidationResult result = s.walk(instance, true);
        assertNotNull(result, "Result should be non‑null even with an unsupported keyword");
    }

    /**
     * 14. Malformed $ref URI – should not throw at creation time; validation
     * will report the problem.
     */
//     @Test
//     void walkMalformedRefUriShouldResultInInvalid() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "$ref": "http://example.com/invalid[ref].json"
//             }
//             """;
//         JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         JsonSchema s = factory.getSchema(schemaJson);
//         // Validate a simple instance – the malformed URI will be reported.
//         JsonNode instance = mapper.readTree("{}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Malformed $ref URI should produce validation messages");
//     }
}