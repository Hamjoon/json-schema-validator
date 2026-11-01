```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.OutputFormat;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.collectors.CollectingOutputFormat;
import com.networknt.schema.output.OutputFormatException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Extended unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat)}.
 *
 * <p>These tests cover the wide range of branches described in the supplied
 * test‑case JSON.  The {@code @BeforeAll}/{@code @BeforeEach} methods are
 * left untouched – they provide a simple “string” schema that is re‑used by
 * a few generic tests.  Each test creates its own schema (when required)
 * using {@link JsonSchemaFactory#getInstance(VersionFlag)} so that the
 * validation logic exercised is isolated from the other tests.</p>
 *
 * <p>Where the library supplies a concrete {@link OutputFormat}
 * implementation (e.g. {@link CollectingOutputFormat}) we use it directly.
 * For scenarios that require exceptional behaviour we create lightweight
 * custom implementations or Mockito mocks.</p>
 */
class JsonSchema_validate_5_Test {

    private static ObjectMapper mapper;
    private JsonSchema schema; // reused by the two simple tests in the original framework

    @BeforeAll
    static void setupBeforeAll() {
        // ObjectMapper is thread‑safe after configuration, so a single instance is fine.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A very small schema that only accepts strings.
        String schemaJson = "{ \"type\": \"string\" }";

        // Use the factory to obtain a JsonSchema instance.  The default meta‑schema
        // (draft‑2020‑12) is sufficient for this simple test.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    /* --------------------------------------------------------------------- */
    /*  Original simple tests (kept unchanged)                              */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_withInvalidJsonNode_returnsValidationMessages() throws Exception {
        JsonNode invalidNode = mapper.readTree("123");
        Set<ValidationMessage> messages = schema.validate(invalidNode, OutputFormat.DEFAULT);
        assertNotNull(messages);
        assertFalse(messages.isEmpty(), "Validation should produce at least one message for an invalid node");
    }

    @Test
    void validate_withValidJsonNode_returnsEmptySet() throws Exception {
        JsonNode validNode = mapper.readTree("\"hello\"");
        Set<ValidationMessage> messages = schema.validate(validNode, OutputFormat.DEFAULT);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "A valid node should produce no validation messages");
    }

    /* --------------------------------------------------------------------- */
    /*  New test groups – one method per logical group                      */
    /* --------------------------------------------------------------------- */

    @Nested
    @DisplayName("Successful validation – empty schema")
    class EmptySchema {

        @Test
        void validate_emptySchema_returnsEmptyList() throws Exception {
            // an empty schema has no validators
            JsonSchema empty = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                    .getSchema("{}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> result = empty.validate(mapper.readTree("{}"), format);
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Empty schema should not produce any ValidationMessage");
        }
    }

    @Nested
    @DisplayName("Validation with type error")
    class TypeError {

        @Test
        void validate_integerWhenStringExpected_returnsTypeMessage() throws Exception {
            String schemaJson = "{ \"type\": \"integer\" }";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("\"twenty\""); // string instead of integer

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("type", vm.getMessageId());
            assertEquals("/" , vm.getPath()); // root path
        }
    }

    @Nested
    @DisplayName("Validation with custom validator failure (minLength)")
    class MinLengthFailure {

        @Test
        void validate_emptyStringAgainstMinLength_returnsMessage() throws Exception {
            String schemaJson = "{ \"type\": \"string\", \"minLength\": 1 }";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("\"\"");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("minLength", vm.getMessageId());
            assertEquals("/", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Null arguments")
    class NullArguments {

        @Test
        void validate_nullRootNode_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> schema.validate(null, OutputFormat.DEFAULT));
        }

        @Test
        void validate_nullOutputFormat_throwsNullPointerException() throws Exception {
            JsonNode node = mapper.readTree("\"test\"");
            assertThrows(NullPointerException.class,
                    () -> schema.validate(node, null));
        }
    }

    @Nested
    @DisplayName("ExecutionContextCustomizer is null (default path)")
    class DefaultCustomizer {

        @Test
        void validate_withNullCustomizer_behavesNormally() throws Exception {
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                    .getSchema("{\"type\":\"boolean\"}");
            JsonNode node = mapper.readTree("true");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);
            assertTrue(messages.isEmpty(), "Valid boolean should produce no messages");
        }
    }

    @Nested
    @DisplayName("Recursive anchor handling")
    class RecursiveAnchor {

        @Test
        void validate_recursiveAnchorDoesNotLoop() throws Exception {
            // schema with $recursiveAnchor and a self‑reference via $ref
            String schemaJson = "{\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"self\": { \"$ref\": \"#\" }\n" +
                    "  }\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"self\":{}}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);
            assertTrue(messages.isEmpty(), "Recursive anchor should prevent infinite recursion");
        }
    }

    @Nested
    @DisplayName("$ref resolution")
    class RefResolution {

        @Test
        void validate_refToDefinitionProducesMessages() throws Exception {
            String schemaJson = "{\n" +
                    "  \"$defs\": {\n" +
                    "    \"person\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"required\": [\"name\"]\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"$ref\": \"#/$defs/person\"\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"age\":30}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("required", vm.getMessageId());
            assertEquals("/", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Multiple validators – mixed success/failure")
    class MixedValidators {

        @Test
        void validate_mixedValidatorsOnlyFailsOnInvalidFields() throws Exception {
            String schemaJson = "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"id\": { \"type\": \"integer\" },\n" +
                    "    \"email\": { \"type\": \"string\", \"format\": \"email\" }\n" +
                    "  },\n" +
                    "  \"required\": [\"id\", \"email\"]\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"id\":1,\"email\":\"invalid-email\"}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("format", vm.getMessageId());
            assertEquals("/email", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Missing required property")
    class RequiredProperty {

        @Test
        void validate_missingRequired_returnsMessage() throws Exception {
            String schemaJson = "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"required\": [\"name\"],\n" +
                    "  \"properties\": { \"name\": {\"type\":\"string\"} }\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"age\":30}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("required", vm.getMessageId());
            assertEquals("/", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Disallowed additional property")
    class AdditionalProperties {

        @Test
        void validate_extraPropertyWhenAdditionalPropertiesFalse_returnsMessage() throws Exception {
            String schemaJson = "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"additionalProperties\": false,\n" +
                    "  \"properties\": { \"id\": {\"type\":\"integer\"} }\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"id\":1,\"extra\":\"value\"}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("additionalProperties", vm.getMessageId());
            assertEquals("/extra", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Enum constraint violation")
    class EnumViolation {

        @Test
        void validate_invalidEnumValue_returnsMessage() throws Exception {
            String schemaJson = "{\n" +
                    "  \"type\": \"string\",\n" +
                    "  \"enum\": [\"ACTIVE\",\"INACTIVE\"]\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("\"UNKNOWN\"");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("enum", vm.getMessageId());
            assertEquals("/", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Numeric boundary validation")
    class NumericBoundary {

        @ParameterizedTest
        @CsvSource({
                // value,   minimum, expectedMessageId
                "-1,0,minimum",
                "5,0,none"
        })
        void validate_minimumBoundary(String value, int minimum, String expectedId) throws Exception {
            String schemaJson = String.format("{\"type\":\"integer\",\"minimum\":%d}", minimum);
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree(value);

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            if ("none".equals(expectedId)) {
                assertTrue(messages.isEmpty(), "Value " + value + " should pass the minimum check");
            } else {
                assertEquals(1, messages.size());
                assertEquals(expectedId, messages.get(0).getMessageId());
            }
        }

        @Test
        void validate_exclusiveMaximumBoundary_passesWhenEqual() throws Exception {
            String schemaJson = "{\"type\":\"integer\",\"exclusiveMaximum\":100}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("100"); // exactly the exclusiveMaximum

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);
            assertTrue(messages.isEmpty(), "Value equal to exclusiveMaximum should be accepted");
        }
    }

    @Nested
    @DisplayName("Array items validation failure")
    class ArrayItems {

        @Test
        void validate_arrayItemFails_emailFormat() throws Exception {
            String schemaJson = "{\n" +
                    "  \"type\": \"array\",\n" +
                    "  \"items\": {\n" +
                    "    \"type\": \"object\",\n" +
                    "    \"properties\": { \"email\": { \"type\": \"string\", \"format\": \"email\" } },\n" +
                    "    \"required\": [\"email\"]\n" +
                    "  }\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("[{\"email\":\"bad\"},{\"email\":\"good@example.com\"}]");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("format", vm.getMessageId());
            assertEquals("/0/email", vm.getPath());
        }
    }

    @Nested
    @DisplayName("oneOf validator failure")
    class OneOfFailure {

        @Test
        void validate_oneOfNoMatch_returnsMessage() throws Exception {
            String schemaJson = "{\n" +
                    "  \"oneOf\": [\n" +
                    "    {\"type\":\"integer\",\"minimum\":10},\n" +
                    "    {\"type\":\"string\",\"pattern\":\"^[A-Z]+$\"}\n" +
                    "  ]\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("5"); // neither integer >=10 nor matching string pattern

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            assertEquals("oneOf", messages.get(0).getMessageId());
            assertEquals("/", messages.get(0).getPath());
        }
    }

    @Nested
    @DisplayName("Conditional validator failure (if/then/else)")
    class ConditionalFailure {

        @Test
        void validate_ifThenFails_returnsMessage() throws Exception {
            String schemaJson = "{\n" +
                    "  \"if\": { \"properties\": { \"type\": { \"const\": \"A\" } }, \"required\": [\"type\"] },\n" +
                    "  \"then\": { \"properties\": { \"code\": { \"type\": \"integer\", \"minimum\": 200 } }, \"required\": [\"code\"] }\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"type\":\"A\",\"code\":123}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("minimum", vm.getMessageId());
            assertEquals("/code", vm.getPath());
        }
    }

    @Nested
    @DisplayName("OutputFormat exception propagation")
    class OutputFormatExceptions {

        @Test
        void validate_outputFormatThrowsRuntimeException_propagates() throws Exception {
            // Mock OutputFormat to throw RuntimeException when addMessage is called
            @SuppressWarnings("unchecked")
            OutputFormat<List<ValidationMessage>> throwingFormat = mock(OutputFormat.class);
            doThrow(new RuntimeException("boom")).when(throwingFormat).addMessage(any());

            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                    .getSchema("{\"type\":\"integer\"}");
            JsonNode node = mapper.readTree("123");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> s.validate(node, throwingFormat));
            assertEquals("boom", ex.getMessage());
        }

        @Test
        void validate_outputFormatThrowsIOException_propagatesWrapped() throws Exception {
            // Custom OutputFormat that throws IOException on addMessage
            class IOExceptionThrowingFormat implements OutputFormat<List<ValidationMessage>> {
                @Override public void addMessage(ValidationMessage message) throws IOException {
                    throw new IOException("IO failure");
                }
                @Override public List<ValidationMessage> getResult() { return null; }
                @Override public void setExecutionContext(ExecutionContext ctx) {}
                @Override public ExecutionContext getExecutionContext() { return null; }
            }

            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                    .getSchema("{\"type\":\"integer\"}");
            JsonNode node = mapper.readTree("123");

            // The library wraps checked exceptions into OutputFormatException (a runtime exception)
            OutputFormatException ex = assertThrows(OutputFormatException.class,
                    () -> s.validate(node, new IOExceptionThrowingFormat()));
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("IO failure", ex.getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("Invalid $ref syntax – exception propagation")
    class InvalidRefSyntax {

        @Test
        void validate_malformedRef_throwsIllegalArgumentException() {
            String schemaJson = "{ \"$ref\": \"invalid#ref\" }";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.createObjectNode(); // empty object

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> s.validate(node, OutputFormat.DEFAULT));
            assertTrue(ex.getMessage().contains("Invalid JSON Pointer"));
        }
    }

    @Nested
    @DisplayName("Custom validator throws unchecked exception")
    class CustomValidatorException {

        @Test
        void validate_userValidatorThrows_exceptionPropagates() throws Exception {
            // Build a schema that uses a custom validator (via "format": "custom")
            // For the purpose of the test we register a dummy validator that throws.
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
            factory.getConfig().registerValidator("custom", (schemaNode, validationContext) -> (node, at) -> {
                throw new RuntimeException("User validator error");
            });

            JsonSchema s = factory.getSchema("{\"type\":\"string\",\"format\":\"custom\"}");
            JsonNode node = mapper.readTree("\"any\"");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> s.validate(node, OutputFormat.DEFAULT));
            assertEquals("User validator error", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("ExecutionContextCustomizer throws exception")
    class ExecutionContextCustomizerException {

        @Test
        void validate_customizerThrows_exceptionPropagates() {
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                    .getSchema("{\"type\":\"string\"}");
            JsonNode node = mapper.createObjectNode();

            Consumer<ExecutionContext> badCustomizer = ctx -> {
                throw new RuntimeException("Customizer failure");
            };

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> s.validate(node, OutputFormat.DEFAULT, badCustomizer));
            assertEquals("Customizer failure", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Circular $ref without $recursiveAnchor")
    class CircularRef {

        @Test
        void validate_circularRefWithoutAnchor_throwsIllegalStateException() {
            String schemaJson = "{\n" +
                    "  \"$defs\": {\n" +
                    "    \"self\": { \"$ref\": \"#/$defs/self\" }\n" +
                    "  },\n" +
                    "  \"$ref\": \"#/$defs/self\"\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.createObjectNode();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> s.validate(node, OutputFormat.DEFAULT));
            assertTrue(ex.getMessage().toLowerCase().contains("recursion"));
        }
    }

    @Nested
    @DisplayName("PatternProperties validation")
    class PatternProperties {

        @Test
        void validate_propertyNameNotMatchingPattern_returnsMessage() throws Exception {
            String schemaJson = "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"patternProperties\": { \"^prop_.*$\": { \"type\": \"string\" } }\n" +
                    "}";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"prop_1\":\"value\",\"invalid\":\"value\"}");

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("patternProperties", vm.getMessageId());
            assertEquals("/invalid", vm.getPath());
        }
    }

    @Nested
    @DisplayName("Root node type mismatch (array vs object)")
    class RootTypeMismatch {

        @Test
        void validate_rootArrayWhenObjectExpected_returnsMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\" }";
            JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
            JsonNode node = mapper.readTree("[]"); // array node

            CollectingOutputFormat<List<ValidationMessage>> format = new CollectingOutputFormat<>();
            List<ValidationMessage> messages = s.validate(node, format);

            assertEquals(1, messages.size());
            ValidationMessage vm = messages.get(0);
            assertEquals("type", vm.getMessageId());
            assertEquals("/", vm.getPath());
        }
    }
}
```