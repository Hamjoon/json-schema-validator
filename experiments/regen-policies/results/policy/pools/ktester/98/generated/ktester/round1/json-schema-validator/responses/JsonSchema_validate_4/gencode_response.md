```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.utils.JsonNodes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(JsonNode, ExecutionContextCustomizer)}.
 *
 * The tests cover null‑parameter handling, normal validation (both success and failure),
 * custom {@link ExecutionContextCustomizer} effects, locale handling, numeric boundaries,
 * enum/pattern validation, array constraints, $ref handling, recursive anchors, and error
 * propagation from the customizer or from malformed schemas.
 *
 * The test class follows the provided framework – the {@code @BeforeAll}/{@code @AfterAll}
 * methods are left untouched and only additional static schemas are added.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    /* --------------------------------------------------------------------- */
    /*  Shared test fixtures                                                 */
    /* --------------------------------------------------------------------- */
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

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        /* ---------- basic schema (already used in the starter test) ---------- */
        String basic = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";
        basicSchema = factory.getSchema(basic);

        /* ---------- numeric schema (min 0, max 100) ---------- */
        String numeric = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"value\":{ \"type\":\"number\", \"minimum\":0, \"maximum\":100 }"
                + "},"
                + "\"required\":[\"value\"]"
                + "}";
        numericSchema = factory.getSchema(numeric);

        /* ---------- enum & pattern schema ---------- */
        String enumPattern = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"status\":{ \"type\":\"string\", \"enum\":[\"ACTIVE\",\"INACTIVE\",\"PENDING\"] },"
                + "   \"email\":{ \"type\":\"string\", \"pattern\":\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\" }"
                + "},"
                + "\"required\":[\"status\",\"email\"]"
                + "}";
        enumPatternSchema = factory.getSchema(enumPattern);

        /* ---------- array schema ---------- */
        String array = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"tags\":{"
                + "       \"type\":\"array\","
                + "       \"minItems\":1,"
                + "       \"maxItems\":5,"
                + "       \"items\":{ \"type\":\"string\", \"enum\":[\"red\",\"green\",\"blue\",\"yellow\"] }"
                + "   }"
                + "},"
                + "\"required\":[\"tags\"]"
                + "}";
        arraySchema = factory.getSchema(array);

        /* ---------- $ref schema (person definition) ---------- */
        String ref = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"person\":{ \"$ref\":\"#/definitions/Person\" }"
                + "},"
                + "\"definitions\":{"
                + "   \"Person\":{"
                + "       \"type\":\"object\","
                + "       \"properties\":{"
                + "           \"name\":{ \"type\":\"string\" },"
                + "           \"age\":{ \"type\":\"integer\" }"
                + "       },"
                + "       \"required\":[\"name\",\"age\"],"
                + "       \"additionalProperties\":false"
                + "   }"
                + "},"
                + "\"required\":[\"person\"]"
                + "}";
        refSchema = factory.getSchema(ref);

        /* ---------- recursive anchor schema ---------- */
        String recursive = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"$recursiveAnchor\":true,"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"node\":{ \"$ref\":\"#\" },"
                + "   \"value\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"value\"]"
                + "}";
        recursiveSchema = factory.getSchema(recursive);

        /* ---------- malformed $ref (invalid percent‑encoding) ---------- */
        String malformedRef = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"bad\":{ \"$ref\":\"%ZZ\" }"
                + "}"
                + "}";
        malformedRefSchema = factory.getSchema(malformedRef);

        /* ---------- unresolvable $ref ---------- */
        String unresolvableRef = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"unknown\":{ \"$ref\":\"#/definitions/Unknown\" }"
                + "}"
                + "}";
        unresolvableRefSchema = factory.getSchema(unresolvableRef);

        /* ---------- invalid validator type ---------- */
        String invalidValidator = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"field\":{ \"type\":\"unknownType\" }"
                + "}"
                + "}";
        invalidValidatorSchema = factory.getSchema(invalidValidator);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test teardown required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up.
    }

    /* --------------------------------------------------------------------- */
    /*  1. Null parameter handling                                            */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_nullRootNode_andNullCustomizer_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> basicSchema.validate(null, null),
                "Both arguments null – should throw NPE");
    }

    @Test
    void validate_nullRootNode_withCustomizer_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
        assertThrows(NullPointerException.class,
                () -> basicSchema.validate(null, customizer),
                "Null rootNode with a customizer – should throw NPE");
    }

    @Test
    void validate_nonNullRootNode_withNullCustomizer_throwsNPE() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        assertThrows(NullPointerException.class,
                () -> basicSchema.validate(node, null),
                "Null customizer with a valid node – should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Basic valid / invalid validation (already covered)               */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_validNode_callsCustomizerAndReturnsEmptySet(@Mock ExecutionContextCustomizer customizer) throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> messages = basicSchema.validate(validNode, customizer);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
        verify(customizer, times(1)).customize(any(ExecutionContext.class));
    }

    @Test
    void validate_invalidNode_callsCustomizerAndReturnsMessages(@Mock ExecutionContextCustomizer customizer) throws Exception {
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        Set<ValidationMessage> messages = basicSchema.validate(invalidNode, customizer);
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        verify(customizer, times(1)).customize(any(ExecutionContext.class));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Customizer exception propagation                                    */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_customizerThrowsException_propagatesRuntimeException(@Mock ExecutionContextCustomizer customizer) {
        doThrow(new RuntimeException("custom error")).when(customizer).customize(any(ExecutionContext.class));
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "Test");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> basicSchema.validate(node, customizer),
                "Customizer exception must propagate");
        assertEquals("custom error", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  4. Locale / MessageSource customization                               */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_localeFrench_returnsFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
        // customizer sets locale to French
        doAnswer(invocation -> {
            ExecutionContext ctx = invocation.getArgument(0);
            ctx.setLocale(Locale.FRENCH);
            return null;
        }).when(customizer).customize(any(ExecutionContext.class));

        // schema that requires age >= 0 (reuse basic schema but add age constraint)
        String schemaJson = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"age\":{ \"type\":\"integer\", \"minimum\":0 }"
                + "},"
                + "\"required\":[\"age\"]"
                + "}";
        JsonSchema ageSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);

        JsonNode node = mapper.readTree("{\"age\":-1}");
        Set<ValidationMessage> messages = ageSchema.validate(node, customizer);
        assertFalse(messages.isEmpty(), "Expected validation error for negative age");
        ValidationMessage msg = messages.iterator().next();
        // The French message contains the word "doit" (as per library localisation)
        assertTrue(msg.getMessage().toLowerCase(Locale.FRENCH).contains("doit"),
                "Message should be localized to French");
    }

    /* --------------------------------------------------------------------- */
    /*  5. Numeric boundary validation                                        */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("numericBoundaryProvider")
    void validate_numericBoundaries(JsonNode node, boolean expectValid) {
        Set<ValidationMessage> messages = numericSchema.validate(node, ctx -> {});
        if (expectValid) {
            assertTrue(messages.isEmpty(), "Expected no messages for boundary value: " + node);
        } else {
            assertFalse(messages.isEmpty(), "Expected validation error for value: " + node);
        }
    }

    private static Stream<Arguments> numericBoundaryProvider() throws Exception {
        ObjectMapper om = new ObjectMapper();
        return Stream.of(
                Arguments.of(om.readTree("{\"value\":0}"), true),
                Arguments.of(om.readTree("{\"value\":100}"), true),
                Arguments.of(om.readTree("{\"value\":-0.0001}"), false),
                Arguments.of(om.readTree("{\"value\":100.01}"), false)
        );
    }

    /* --------------------------------------------------------------------- */
    /*  6. Enum and pattern validation                                        */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_enumAndPattern_validValues() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, ctx -> {});
        assertTrue(messages.isEmpty(), "Both enum and pattern should be satisfied");
    }

    @Test
    void validate_enumAndPattern_invalidEnum() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"UNKNOWN\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, ctx -> {});
        assertFalse(messages.isEmpty(), "Invalid enum value must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("status")),
                "Message should reference the 'status' property");
    }

    @Test
    void validate_enumAndPattern_invalidPattern() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"invalid-email@@example..com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, ctx -> {});
        assertFalse(messages.isEmpty(), "Invalid email pattern must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("email")),
                "Message should reference the 'email' property");
    }

    /* --------------------------------------------------------------------- */
    /*  7. Array constraints                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_array_valid() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"green\",\"blue\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, ctx -> {});
        assertTrue(messages.isEmpty(), "Array satisfies minItems, maxItems and enum constraints");
    }

    @Test
    void validate_array_belowMinItems() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, ctx -> {});
        assertFalse(messages.isEmpty(), "Empty array should violate minItems");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("tags")),
                "Message should reference the 'tags' property");
    }

    @Test
    void validate_array_invalidEnumItem() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"purple\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, ctx -> {});
        assertFalse(messages.isEmpty(), "Second element not in enum must be reported");
        // The path includes the index of the offending element
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("/tags/1")),
                "Message should point to the second array element");
    }

    /* --------------------------------------------------------------------- */
    /*  8. $ref handling (valid & additionalProperties)                       */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_ref_validObject() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40}}");
        Set<ValidationMessage> messages = refSchema.validate(node, ctx -> {});
        assertTrue(messages.isEmpty(), "Valid $ref object should produce no messages");
    }

    @Test
    void validate_ref_additionalProperty() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40,\"extra\":\"value\"}}");
        Set<ValidationMessage> messages = refSchema.validate(node, ctx -> {});
        assertFalse(messages.isEmpty(), "Additional property should be flagged");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("extra")),
                "Message should reference the unexpected 'extra' property");
    }

    /* --------------------------------------------------------------------- */
    /*  9. Recursive anchor handling                                          */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_recursiveAnchor_validDeepStructure() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"node\":{\"value\":\"leaf\"}}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, ctx -> {});
        assertTrue(messages.isEmpty(), "Deep recursive structure should validate without error");
    }

    @Test
    void validate_recursiveAnchor_typeMismatch() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"value\":123}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, ctx -> {});
        assertFalse(messages.isEmpty(), "Type mismatch at deepest level must be reported");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("value")),
                "Message should reference the 'value' property");
    }

    /* --------------------------------------------------------------------- */
    /* 10. ExecutionContext attribute null key                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_customizerSetsNullAttributeKey_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
        doAnswer(invocation -> {
            ExecutionContext ctx = invocation.getArgument(0);
            ctx.setAttribute(null, "value"); // expected to throw NPE inside the context
            return null;
        }).when(customizer).customize(any(ExecutionContext.class));

        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        assertThrows(NullPointerException.class,
                () -> basicSchema.validate(node, customizer),
                "Setting a null attribute key should propagate NPE");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Malformed $ref decoding error                                      */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_malformedRef_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("{}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> malformedRefSchema.validate(node, ctx -> {}),
                "Malformed percent‑encoding in $ref must cause IllegalArgumentException");
        // The exception originates from URLDecoder.decode; ensure the message mentions decoding
        assertTrue(ex.getMessage().toLowerCase().contains("illegal"),
                "Exception message should indicate illegal argument");
    }

    /* --------------------------------------------------------------------- */
    /* 12. Unresolvable $ref leads to JsonSchemaException                     */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_unresolvableRef_throwsJsonSchemaException() throws Exception {
        JsonNode node = mapper.readTree("{\"unknown\":{}}");
        assertThrows(JsonSchemaException.class,
                () -> unresolvableRefSchema.validate(node, ctx -> {}),
                "Unresolvable $ref should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    /* 13. Invalid validator type configuration                               */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_invalidValidatorType_throwsIllegalStateException() throws Exception {
        JsonNode node = mapper.readTree("{\"field\":\"value\"}");
        assertThrows(IllegalStateException.class,
                () -> invalidValidatorSchema.validate(node, ctx -> {}),
                "Unknown validator type must cause IllegalStateException");
    }

    /* --------------------------------------------------------------------- */
    /* 14. Custom ExecutionContext effects (fail‑fast) – simplified check      */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_failFastStopsAfterFirstError(@Mock ExecutionContextCustomizer customizer) {
        // Simulate a customizer that enables fail‑fast mode
        doAnswer(invocation -> {
            ExecutionContext ctx = invocation.getArgument(0);
            ctx.setFailFast(true);
            return null;
        }).when(customizer).customize(any(ExecutionContext.class));

        // Schema with two independent errors (name wrong type, age missing)
        String schemaJson = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{ \"type\":\"string\" },"
                + "   \"age\":{ \"type\":\"integer\" }"
                + "},"
                + "\"required\":[\"name\",\"age\"]"
                + "}";
        JsonSchema failFastSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);

        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", 123); // wrong type, age missing
        Set<ValidationMessage> messages = failFastSchema.validate(node, customizer);
        // With fail‑fast, we expect exactly one message
        assertEquals(1, messages.size(), "Fail‑fast should stop after the first validation error");
    }
}
```