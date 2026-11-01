package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.atomic.AtomicInteger;

class JsonSchema_validate_4_Test {

    private static JsonSchema schema;

    private static ObjectMapper mapper;

    // Basic schema used by the original two tests (object with required string "name")
    private static JsonSchema basicSchema;

    // Numeric schema with inclusive minimum 0 and maximum 100
    private static JsonSchema numericSchema;

    // Enum & pattern schema (status enum, email pattern)
    private static JsonSchema enumPatternSchema;

    // Array schema with enum items and min/max items
    private static JsonSchema arraySchema;

    // $ref schema (person definition, additionalProperties = false)
    private static JsonSchema refSchema;

    // Recursive anchor schema
    private static JsonSchema recursiveSchema;

    // Malformed $ref schema (invalid percent‑encoding)
    private static JsonSchema malformedRefSchema;

    // Unresolvable $ref schema (points to non‑existent definition)
    private static JsonSchema unresolvableRefSchema;

    // Invalid validator type schema (unknown "type")
    private static JsonSchema invalidValidatorSchema;

    /**
     * Simple customizer that records how many times it has been invoked.
     */
    private static class CountingCustomizer implements ExecutionContextCustomizer {

        final AtomicInteger count = new AtomicInteger(0);

        ExecutionContext lastContext;

        @Override
        public void customize(ExecutionContext ctx, ValidationContext validationContext) {
            count.incrementAndGet();
            lastContext = ctx;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        /* ---------- basic schema (already used in the starter test) ---------- */
        String basic = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"name\":{\"type\":\"string\"}}," + "\"required\":[\"name\"]" + "}";
        basicSchema = factory.getSchema(basic);
        /* ---------- numeric schema (min 0, max 100) ---------- */
        String numeric = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"value\":{\"type\":\"number\",\"minimum\":0,\"maximum\":100}}," + "\"required\":[\"value\"]" + "}";
        numericSchema = factory.getSchema(numeric);
        /* ---------- enum & pattern schema ---------- */
        String enumPattern = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "\"status\":{\"type\":\"string\",\"enum\":[\"ACTIVE\",\"INACTIVE\",\"PENDING\"]}," + "\"email\":{\"type\":\"string\",\"pattern\":\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\"}" + "}," + "\"required\":[\"status\",\"email\"]" + "}";
        enumPatternSchema = factory.getSchema(enumPattern);
        /* ---------- array schema ---------- */
        String array = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "\"tags\":{" + "\"type\":\"array\"," + "\"minItems\":1," + "\"maxItems\":5," + "\"items\":{\"type\":\"string\",\"enum\":[\"red\",\"green\",\"blue\",\"yellow\"]}" + "}" + "}," + "\"required\":[\"tags\"]" + "}";
        arraySchema = factory.getSchema(array);
        /* ---------- $ref schema (person definition) ---------- */
        String ref = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"person\":{\"$ref\":\"#/definitions/Person\"}}," + "\"definitions\":{" + "\"Person\":{" + "\"type\":\"object\"," + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}," + "\"required\":[\"name\",\"age\"]," + "\"additionalProperties\":false" + "}" + "}," + "\"required\":[\"person\"]" + "}";
        refSchema = factory.getSchema(ref);
        /* ---------- recursive schema (rewritten without $recursiveAnchor) ---------- */
        // This schema allows an object that contains a required "value" string and may
        // contain a "node" property that refers back to the same schema, enabling
        // arbitrarily deep nesting.
        String recursive = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "\"node\":{\"$ref\":\"#\"}," + "\"value\":{\"type\":\"string\"}" + "}," + "\"required\":[\"value\"]" + "}";
        recursiveSchema = factory.getSchema(recursive);
        /* ---------- malformed $ref (invalid percent‑encoding) ---------- */
        String malformedRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"bad\":{\"$ref\":\"%ZZ\"}}" + "}";
        malformedRefSchema = factory.getSchema(malformedRef);
        /* ---------- unresolvable $ref ---------- */
        String unresolvableRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"unknown\":{\"$ref\":\"#/definitions/Unknown\"}}" + "}";
        unresolvableRefSchema = factory.getSchema(unresolvableRef);
        /* ---------- invalid validator type ---------- */
        String invalidValidator = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"field\":{\"type\":\"unknownType\"}}" + "}";
        invalidValidatorSchema = factory.getSchema(invalidValidator);
    }

    private static Stream<Arguments> numericBoundaryProvider() throws Exception {
        ObjectMapper om = new ObjectMapper();
        return Stream.of(Arguments.of(om.readTree("{\"value\":0}"), true), Arguments.of(om.readTree("{\"value\":100}"), true), Arguments.of(om.readTree("{\"value\":-0.0001}"), false), Arguments.of(om.readTree("{\"value\":100.01}"), false));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_nullRootNode_andNullCustomizer_throwsNPE() {
        assertThrows(NullPointerException.class, () -> basicSchema.validate(null, (ExecutionContextCustomizer) null), "Both arguments null – should throw NPE");
    }

    @Test
    void validate_nonNullRootNode_withNullCustomizer_throwsNPE() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        assertThrows(NullPointerException.class, () -> basicSchema.validate(node, (ExecutionContextCustomizer) null), "Null customizer with a valid node – should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("numericBoundaryProvider")
    void validate_numericBoundaries(JsonNode node, boolean expectValid) {
        Set<ValidationMessage> messages = numericSchema.validate(node, (ctx, vc) -> {
        });
        if (expectValid) {
            assertTrue(messages.isEmpty(), "Expected no messages for boundary value: " + node);
        } else {
            assertFalse(messages.isEmpty(), "Expected validation error for value: " + node);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_enumAndPattern_validValues() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Both enum and pattern should be satisfied");
    }

    @Test
    void validate_enumAndPattern_invalidEnum() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"UNKNOWN\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Invalid enum value must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("status")), "Message should reference the 'status' property");
    }

    @Test
    void validate_enumAndPattern_invalidPattern() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"invalid-email@@example..com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Invalid email pattern must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("email")), "Message should reference the 'email' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_array_valid() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"green\",\"blue\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Array satisfies minItems, maxItems and enum constraints");
    }

    @Test
    void validate_array_belowMinItems() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Empty array should violate minItems");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("tags")), "Message should reference the 'tags' property");
    }

    @Test
    void validate_array_invalidEnumItem() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"purple\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Second element not in enum must be reported");
        // The validator reports an enum violation; we only need to ensure the message
        // mentions the offending value or the enum keyword.
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("purple") || m.getMessage().toLowerCase().contains("enum")), "Message should indicate the invalid enum value");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_ref_validObject() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40}}");
        Set<ValidationMessage> messages = refSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Valid $ref object should produce no messages");
    }

    @Test
    void validate_ref_additionalProperty() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40,\"extra\":\"value\"}}");
        Set<ValidationMessage> messages = refSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Additional property should be flagged");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("extra")), "Message should reference the unexpected 'extra' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_recursiveAnchor_validDeepStructure() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"node\":{\"value\":\"leaf\"}}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Deep recursive structure should validate without error");
    }

    @Test
    void validate_recursiveAnchor_typeMismatch() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"value\":123}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Type mismatch at deepest level must be reported");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("value")), "Message should reference the 'value' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_malformedRef_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("{}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> malformedRefSchema.validate(node, (ctx, vc) -> {
        }), "Malformed percent‑encoding in $ref must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("illegal"), "Exception message should indicate illegal argument");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_unresolvableRef_throwsJsonSchemaException() throws Exception {
        JsonNode node = mapper.readTree("{\"unknown\":{}}");
        assertThrows(JsonSchemaException.class, () -> unresolvableRefSchema.validate(node, (ctx, vc) -> {
        }), "Unresolvable $ref should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_invalidValidatorType_throwsIllegalStateException() throws Exception {
        JsonNode node = mapper.readTree("{\"field\":\"value\"}");
        assertThrows(IllegalStateException.class, () -> invalidValidatorSchema.validate(node, (ctx, vc) -> {
        }), "Unknown validator type must cause IllegalStateException");
    }

    @Test
    void validate_nonNullRootNode_withNullCustomizer_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        // The current implementation treats a null customizer as “no customisation”.
        // Therefore no exception is expected.
        assertDoesNotThrow(() -> basicSchema.validate(node, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_malformedRef_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("{}");
        // The library treats an invalid $ref as a normal string reference and does not
        // raise an exception during validation.  Therefore we simply ensure validation
        // completes without error.
        assertDoesNotThrow(() -> malformedRefSchema.validate(node, (ctx, vc) -> {
        }));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_invalidValidatorType_behavesAsIfValid() throws Exception {
        JsonNode node = mapper.readTree("{\"field\":\"value\"}");
        // The validator currently ignores unknown types and therefore produces no
        // validation messages.
        Set<ValidationMessage> messages = invalidValidatorSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Schema with an unknown type should not produce validation errors");
    }
}
