package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    private static ObjectMapper mapper;

    private JsonSchema schema;

    /**
     * Returns the raw {@link Set} of {@link ValidationMessage}s.
     */
    static class CollectingOutputFormat implements OutputFormat<Set<ValidationMessage>> {

        static final CollectingOutputFormat INSTANCE = new CollectingOutputFormat();

        private CollectingOutputFormat() {
        }

        @Override
        public Set<ValidationMessage> apply(Set<ValidationMessage> messages) {
            return messages;
        }
    }

    /**
     * Throws a {@link RuntimeException} when {@code apply} is called.
     */
    static class ExceptionThrowingOutputFormat implements OutputFormat<Object> {

        static final ExceptionThrowingOutputFormat INSTANCE = new ExceptionThrowingOutputFormat();

        private ExceptionThrowingOutputFormat() {
        }

        @Override
        public Object apply(Set<ValidationMessage> messages) {
            throw new RuntimeException("forced exception from OutputFormat");
        }
    }

    /**
     * Returns {@code null} – used to verify that the validator rejects a null result.
     */
    static class NullReturningOutputFormat implements OutputFormat<Object> {

        static final NullReturningOutputFormat INSTANCE = new NullReturningOutputFormat();

        private NullReturningOutputFormat() {
        }

        @Override
        public Object apply(Set<ValidationMessage> messages) {
            return null;
        }
    }

    /**
     * Converts the validation messages into an {@code Integer} count.
     */
    static class ErrorCountOutputFormat implements OutputFormat<Integer> {

        static final ErrorCountOutputFormat INSTANCE = new ErrorCountOutputFormat();

        private ErrorCountOutputFormat() {
        }

        @Override
        public Integer apply(Set<ValidationMessage> messages) {
            return messages.size();
        }
    }

    /**
     * Simple customizer that flips a flag – used for the “interface” test.
     */
    static class DummyExecutionContextCustomizer implements ExecutionContextCustomizer {

        @Override
        public void customize(ExecutionContext ctx) {
            ctx.setValidateAll(true);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // a very simple default schema – individual tests replace it with a
        // schema that matches the scenario they are exercising
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        schema = factory.getSchema(schemaJson);
    }

    /**
     * Validate a correct JSON node – expect no validation messages.
     */
    @Test
    void validate_validObject_returnsEmptyMessages() throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"test\",\"age\":30}");
        // using the default output format and no custom execution context
        Set<ValidationMessage> messages = schema.validate(validNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid object");
    }

    /**
     * Validate an incorrect JSON node – expect at least one validation message.
     */
    @Test
    void validate_invalidNode_returnsMessages() throws Exception {
        JsonNode invalidNode = mapper.readTree("\"just a string\"");
        Set<ValidationMessage> messages = schema.validate(invalidNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        assertFalse(messages.isEmpty(), "Expected validation messages for a non‑object JSON node");
    }

    /**
     * Validate with a custom ExecutionContextCustomizer that does nothing.
     */
    @Test
    void validate_withNoOpCustomizer_behavesSameAsNull() throws Exception {
        JsonNode validNode = mapper.readTree("{\"foo\":\"bar\"}");
        ExecutionContextCustomizer noOp = executionContext -> {
            // intentionally left blank – no modifications
        };
        Set<ValidationMessage> messages = schema.validate(validNode, OutputFormat.DEFAULT, noOp);
        assertTrue(messages.isEmpty(), "No‑op customizer should not affect validation outcome");
    }

    @Test
    void validate_nullRootNode_nullFormat_nullCustomizer_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schema.validate(null, null, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_nullRootNode_validFormat_nullCustomizer_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schema.validate(null, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_validRootNode_nullFormat_nullCustomizer_throwsNPE() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        assertThrows(NullPointerException.class, () -> schema.validate(node, null, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_validObject_defaultCustomizer_returnsEmptySet() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "Expected no validation messages");
    }

    @Test
    void validate_validObject_failFastCustomizer_returnsEmptySet() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        ExecutionContextCustomizer customizer = ctx -> ctx.setFailFast(true);
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, customizer);
        assertTrue(result.isEmpty(), "Fail‑fast on a valid document should still yield no messages");
    }

    @Test
    void validate_invalidType_returnsAtLeastOneMessage() throws Exception {
        JsonNode node = mapper.readTree("{\"age\":\"not-a-number\"}");
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertFalse(result.isEmpty(), "Type error should produce a validation message");
    }

    @Test
    void validate_missingRequired_failFast_returnsSingleMessage() throws Exception {
        // schema with a required property
        String schemaJson = "{ \"type\":\"object\", \"required\":[\"name\"] }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        // missing required "name"
        JsonNode node = mapper.readTree("{}");
        ExecutionContextCustomizer customizer = ctx -> {
            ctx.setFailFast(true);
            // both true – failFast should win
            ctx.setValidateAll(true);
        };
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, customizer);
        assertEquals(1, result.size(), "Fail‑fast should stop after the first error");
    }

    @Test
    void validate_withNullCustomizer_usesDefaultContext() throws Exception {
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "Default context should produce no messages for a valid object");
    }

    @Test
    void validate_withInterfaceCustomizer_appliesFlag() throws Exception {
        // schema that requires a property so we can see the flag effect
        String schemaJson = "{ \"type\":\"object\", \"required\":[\"name\"] }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, new DummyExecutionContextCustomizer());
        // the dummy customizer sets validateAll=true – still no errors
        assertTrue(result.isEmpty(), "Custom flag should not introduce errors");
    }

    @Test
    void validate_withLambdaCustomizer_appliesValidateAll() throws Exception {
        // same schema as above
        String schemaJson = "{ \"type\":\"object\", \"required\":[\"name\"] }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        ExecutionContextCustomizer customizer = ctx -> ctx.setValidateAll(true);
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, customizer);
        assertTrue(result.isEmpty(), "ValidateAll flag alone should not cause failures");
    }

    @Test
    void validate_recursiveAnchor_doesNotOverflow() throws Exception {
        // schema with $recursiveAnchor and a self‑reference
        String schemaJson = "{\n" + "  \"$recursiveAnchor\": true,\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"self\": { \"$ref\": \"#\" }\n" + "  }\n" + "}";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"self\":{\"self\":{}}}");
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "Recursive anchor should be handled without StackOverflowError");
    }

    @Test
    void validate_withoutWalkListenerRunner_keywordRunnerRemainsNull() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        // No customizer – runner stays null
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty());
        // reflectively verify the field is still null
        var field = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        field.setAccessible(true);
        assertNull(field.get(schema), "keywordWalkListenerRunner should be null when not set");
    }

    @Test
    void validate_withCustomWalkListenerRunner_invokesRunner(@Mock WalkListenerRunner mockRunner) throws Exception {
        // inject a mock runner via reflection
        var field = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        field.setAccessible(true);
        field.set(schema, mockRunner);
        JsonNode node = mapper.readTree("{\"a\":1}");
        ExecutionContextCustomizer customizer = ctx -> ctx.setWalkListenerRunner(new DefaultKeywordWalkListenerRunner());
        // The customizer overwrites the runner – we verify that the mock's
        // run method is called during validation.
        schema.validate(node, CollectingOutputFormat.INSTANCE, customizer);
        verify(mockRunner, atLeastOnce()).run(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void validate_outputFormatThrowsRuntimeException_propagates() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        assertThrows(RuntimeException.class, () -> schema.validate(node, ExceptionThrowingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_validObject_errorCountOutputFormat_returnsZero() throws Exception {
        JsonNode node = mapper.readTree("{\"x\":10}");
        Integer count = schema.validate(node, ErrorCountOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertEquals(0, count);
    }

    @Test
    void validate_invalidObject_errorCountOutputFormat_returnsOne() throws Exception {
        // schema that requires an integer property "age"
        String schemaJson = "{ \"type\":\"object\", \"properties\":{ \"age\":{ \"type\":\"integer\" } }, \"required\":[\"age\"] }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"age\":\"not-an-int\"}");
        Integer count = schema.validate(node, ErrorCountOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertEquals(1, count);
    }

    @Test
    void validate_customizerThrowsIllegalStateException_propagates() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new IllegalStateException("customizer failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(node, CollectingOutputFormat.INSTANCE, badCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_customizerThrowsCheckedException_wrappedInRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        ExecutionContextCustomizer badCustomizer = ctx -> {
            try {
                throw new IOException("checked exception");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, CollectingOutputFormat.INSTANCE, badCustomizer));
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void validate_refValidObject_resolvesAndValidates() throws Exception {
        String schemaJson = "{\n" + "  \"$id\": \"http://example.com/person.schema.json\",\n" + "  \"$defs\": {\n" + "    \"person\": {\n" + "      \"type\": \"object\",\n" + "      \"properties\": {\n" + "        \"id\": { \"type\": \"integer\" },\n" + "        \"name\": { \"type\": \"string\" }\n" + "      },\n" + "      \"required\": [\"id\", \"name\"]\n" + "    }\n" + "  },\n" + "  \"$ref\": \"#/$defs/person\"\n" + "}";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode valid = mapper.readTree("{\"id\":1,\"name\":\"Alice\"}");
        Set<ValidationMessage> result = schema.validate(valid, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "Valid $ref instance should produce no messages");
    }

    @Test
    void validate_refInvalidObject_reportsErrors() throws Exception {
        String schemaJson = "{\n" + "  \"$defs\": {\n" + "    \"person\": {\n" + "      \"type\": \"object\",\n" + "      \"properties\": {\n" + "        \"id\": { \"type\": \"integer\" },\n" + "        \"name\": { \"type\": \"string\" }\n" + "      },\n" + "      \"required\": [\"id\", \"name\"]\n" + "    }\n" + "  },\n" + "  \"$ref\": \"#/$defs/person\"\n" + "}";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode invalid = mapper.readTree("{\"id\":\"not-an-int\",\"name\":123}");
        Set<ValidationMessage> result = schema.validate(invalid, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertFalse(result.isEmpty(), "Invalid $ref instance should produce validation messages");
    }

    @Test
    void validate_validArrayOfIntegers_passes() throws Exception {
        String schemaJson = "{ \"type\":\"array\", \"items\": { \"type\":\"integer\" } }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode array = mapper.readTree("[1,2,3]");
        Set<ValidationMessage> result = schema.validate(array, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "Array of integers should be valid");
    }

    @Test
    void validate_invalidArrayMixedTypes_fails() throws Exception {
        String schemaJson = "{ \"type\":\"array\", \"items\": { \"type\":\"integer\" } }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode array = mapper.readTree("[1,\"two\",3]");
        Set<ValidationMessage> result = schema.validate(array, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertFalse(result.isEmpty(), "Array containing a non‑integer should produce errors");
    }

    @Test
    void validate_schemaPreSetWalkListenerRunner_isUsed(@Mock WalkListenerRunner mockRunner) throws Exception {
        // set the mock runner directly on the schema instance
        var field = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        field.setAccessible(true);
        field.set(schema, mockRunner);
        JsonNode node = mapper.readTree("{\"a\":1}");
        schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        verify(mockRunner, atLeastOnce()).run(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void validate_multipleErrors_failFastOverridesValidateAll() throws Exception {
        String schemaJson = "{\n" + "  \"type\":\"object\",\n" + "  \"properties\":{\n" + "    \"age\":{ \"type\":\"integer\" },\n" + "    \"name\":{ \"type\":\"string\" }\n" + "  },\n" + "  \"required\":[\"age\",\"name\"]\n" + "}";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"age\":\"string\",\"name\":null}");
        ExecutionContextCustomizer customizer = ctx -> {
            ctx.setValidateAll(true);
            ctx.setFailFast(true);
        };
        Set<ValidationMessage> result = schema.validate(node, CollectingOutputFormat.INSTANCE, customizer);
        assertEquals(1, result.size(), "Fail‑fast should stop after the first error even when validateAll is true");
    }

    @Test
    void validate_emptyObject_missingRequiredProperty_reportsError() throws Exception {
        String schemaJson = "{ \"type\":\"object\", \"required\":[\"name\"] }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode empty = mapper.readTree("{}");
        Set<ValidationMessage> result = schema.validate(empty, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null);
        assertEquals(1, result.size(), "Missing required property should yield exactly one message");
    }

    @Test
    void validate_schemaWithMalformedId_throwsIllegalArgumentException() {
        String schemaJson = "{ \"$id\":\"http://example.com/schema%ZZ\", \"type\":\"object\" }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();
        assertThrows(IllegalArgumentException.class, () -> schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_schemaWithMalformedId_customizerDoesNotHideException() {
        String schemaJson = "{ \"$id\":\"http://example.com/schema%ZZ\", \"type\":\"object\" }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();
        ExecutionContextCustomizer customizer = ctx -> ctx.setValidateAll(true);
        assertThrows(IllegalArgumentException.class, () -> schema.validate(node, CollectingOutputFormat.INSTANCE, customizer));
    }

    @Test
    void validate_schemaWithMalformedRef_throwsIllegalArgumentException() {
        String schemaJson = "{ \"$ref\":\"#/definitions/Invalid%GRef\" }";
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();
        assertThrows(IllegalArgumentException.class, () -> schema.validate(node, CollectingOutputFormat.INSTANCE, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_schemaWithDuplicateKeys_throwsIllegalStateException() {
        // Jackson silently overwrites duplicate keys, but JsonSchema’s own parser
        // detects the problem and throws IllegalStateException.
        String schemaJson = "{ \"type\":\"object\", \"type\":\"string\" }";
        assertThrows(IllegalStateException.class, () -> JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaJson));
    }

    @Test
    void validate_outputFormatReturnsNull_throwsNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        assertThrows(NullPointerException.class, () -> schema.validate(node, NullReturningOutputFormat.INSTANCE, (ExecutionContextCustomizer) null));
    }
}
