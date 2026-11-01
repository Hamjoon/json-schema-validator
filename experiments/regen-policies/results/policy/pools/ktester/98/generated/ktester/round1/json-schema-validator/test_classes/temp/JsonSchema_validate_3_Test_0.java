package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchema_validate_3_Test {

    private static ObjectMapper mapper;

    private JsonSchema schema;

    /* Null handling                                                          */
    @Nested
    class NullHandling {

        @Test
        void whenBothParametersNull_thenThrowNPE() {
            assertThrows(NullPointerException.class, () -> schema.validate(null, (Consumer<ExecutionContext>) null));
        }

        @Test
        void whenRootNodeNullAndCustomizerPresent_thenThrowNPE() {
            Consumer<ExecutionContext> customizer = ctx -> {
                /* no‑op */
            };
            assertThrows(NullPointerException.class, () -> schema.validate(null, customizer));
        }

        @Test
        void whenCustomizerNullAndRootValid_thenReturnEmptySet() throws Exception {
            // Simple schema that allows any object.
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"any\":\"value\"}");
            Set<ValidationMessage> messages = schema.validate(node, (Consumer<ExecutionContext>) null);
            assertTrue(messages.isEmpty(), "A null customizer should behave like a no‑op");
        }
    }

    /* Execution customizer effects                                            */
    @Nested
    class ExecutionCustomizerEffects {

        @Test
        void whenCustomizerThrowsIllegalStateException_thenPropagate() throws Exception {
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"a\":1}");
            Consumer<ExecutionContext> badCustomizer = ctx -> {
                throw new IllegalStateException("customizer failure");
            };
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(node, badCustomizer));
            assertEquals("customizer failure", ex.getMessage());
        }

        @Test
        void whenCustomizerThrowsRuntimeException_thenPropagate() throws Exception {
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"a\":1}");
            Consumer<ExecutionContext> badCustomizer = ctx -> {
                throw new RuntimeException("unexpected");
            };
            RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, badCustomizer));
            assertEquals("unexpected", ex.getMessage());
        }

        @Test
        void whenCustomizerThrowsCheckedExceptionWrappedInRuntime_thenPropagate() throws Exception {
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"a\":1}");
            Consumer<ExecutionContext> customizer = ctx -> {
                try {
                    throw new java.io.IOException("io fail");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, customizer));
            assertTrue(ex.getCause() instanceof java.io.IOException);
            assertEquals("io fail", ex.getCause().getMessage());
        }
    }

    /* Successful validation                                                   */
    @Nested
    class SuccessfulValidation {

        @Test
        void whenObjectSatisfiesSchema_thenEmptySet() throws Exception {
            String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": { \"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"} },\n" + "  \"required\": [\"name\"]\n" + "}";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertTrue(msgs.isEmpty(), "Valid object should produce no validation messages");
        }

        @Test
        void whenArrayConformsToSchema_thenEmptySet() throws Exception {
            String schemaJson = "{ \"type\": \"array\", \"items\": { \"type\": \"string\" } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("[\"a\",\"b\",\"c\"]");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertTrue(msgs.isEmpty(), "Valid array should produce no validation messages");
        }
    }

    /* Validation failures                                                    */
    @Nested
    class ValidationFailures {

        @Test
        void whenPropertyHasWrongType_thenTypeMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"age\": {\"type\":\"integer\"} } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"age\":\"thirty\"}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            assertEquals("/age", vm.getPath());
        }

        @Test
        void whenRequiredPropertyMissing_thenRequiredMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": {\"type\":\"string\"} }, \"required\": [\"name\"] }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("required", vm.getType());
            // root path for missing required
            assertEquals("", vm.getPath());
        }

        @Test
        void whenAdditionalPropertiesNotAllowed_thenAdditionalPropertiesMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"additionalProperties\": false }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"extra\":true}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("additionalProperties", vm.getType());
            assertEquals("", vm.getPath());
        }

        @Test
        void whenRootNodeTypeMismatch_thenTypeMessageAtRoot() throws Exception {
            String schemaJson = "{ \"type\": \"object\" }";
            schema = createSchema(schemaJson);
            JsonNode arrayNode = mapper.readTree("[1,2,3]");
            Set<ValidationMessage> msgs = schema.validate(arrayNode, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            assertEquals("", vm.getPath());
        }

        @Test
        void whenRootNodePrimitiveStringMismatch_thenTypeMessageAtRoot() throws Exception {
            String schemaJson = "{ \"type\": \"object\" }";
            schema = createSchema(schemaJson);
            JsonNode stringNode = mapper.readTree("\"just a string\"");
            Set<ValidationMessage> msgs = schema.validate(stringNode, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            assertEquals("", vm.getPath());
        }

        @Test
        void whenPatternDoesNotMatch_thenFormatMessage() throws Exception {
            // Using the built‑in "format":"email" validator.
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"email\": { \"type\": \"string\", \"format\": \"email\" } } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"email\":\"not-an-email\"}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("format", vm.getType());
            assertEquals("/email", vm.getPath());
        }

        @Test
        void whenEnumViolation_thenEnumMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"status\": { \"type\": \"string\", \"enum\": [\"ACTIVE\",\"INACTIVE\"] } } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"status\":\"UNKNOWN\"}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("enum", vm.getType());
            assertEquals("/status", vm.getPath());
        }

        @Test
        void whenOneOfNotSatisfied_thenOneOfMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"value\": { \"oneOf\": [ {\"type\":\"integer\"}, {\"type\":\"string\"} ] } } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"value\":true}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("oneOf", vm.getType());
            assertEquals("/value", vm.getPath());
        }

        @Test
        void whenArrayItemHasWrongType_thenItemTypeMessage() throws Exception {
            String schemaJson = "{ \"type\": \"array\", \"items\": { \"type\": \"string\" } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("[\"a\",2,\"c\"]");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            // index 1 is the offending element
            assertEquals("/1", vm.getPath());
        }

        @Test
        void whenDepthExceedsMaximum_thenMaxDepthMessage() throws Exception {
            // The library’s default max depth is 20; we create a deeper structure.
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"level1\": { \"$ref\": \"#\" } } }";
            // The schema itself does not use a recursive anchor, so the validator will stop at maxDepth.
            schema = createSchema(schemaJson);
            // Build a deep JSON object (depth 25).
            StringBuilder sb = new StringBuilder("{");
            for (int i = 1; i <= 25; i++) {
                sb.append("\"level").append(i).append("\":{");
            }
            sb.append("\"leaf\":{}");
            for (int i = 1; i <= 25; i++) {
                sb.append("}");
            }
            sb.append("}");
            JsonNode deepNode = mapper.readTree(sb.toString());
            Set<ValidationMessage> msgs = schema.validate(deepNode, ctx -> {
            });
            assertFalse(msgs.isEmpty(), "Expected at least one validation message for maxDepth");
            boolean hasMaxDepth = msgs.stream().anyMatch(m -> "maxDepth".equals(m.getType()));
            assertTrue(hasMaxDepth, "One of the messages should be of type 'maxDepth'");
        }
    }

    /* Recursive anchor handling                                              */
    @Nested
    class RecursiveAnchorHandling {

        @Test
        void whenRecursiveAnchorResolvesCorrectly_thenEmptySet() throws Exception {
            String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$recursiveAnchor\": true,\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"self\": { \"$recursiveRef\": \"#\" }\n" + "  }\n" + "}";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"self\":{}}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertTrue(msgs.isEmpty(), "Recursive anchor with simple self‑reference should validate successfully");
        }

        @Test
        void whenRecursiveAnchorExceedsDepth_thenMaxDepthMessage() throws Exception {
            String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$recursiveAnchor\": true,\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"self\": { \"$recursiveRef\": \"#\" }\n" + "  }\n" + "}";
            schema = createSchema(schemaJson);
            // Build a deeply nested self‑reference (depth 30)
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < 30; i++) {
                sb.append("\"self\":{");
            }
            sb.append("\"leaf\":{}}");
            for (int i = 0; i < 30; i++) {
                sb.append("}");
            }
            JsonNode deepNode = mapper.readTree(sb.toString());
            Set<ValidationMessage> msgs = schema.validate(deepNode, ctx -> {
            });
            assertFalse(msgs.isEmpty(), "Deep recursion should trigger a maxDepth validation message");
            boolean hasMaxDepth = msgs.stream().anyMatch(m -> "maxDepth".equals(m.getType()));
            assertTrue(hasMaxDepth, "Expected a 'maxDepth' message");
        }
    }

    /* Invalid schema definitions                                             */
    @Nested
    class InvalidSchemaDefinitions {

        @Test
        void whenSchemaHasUnknownType_thenIllegalArgumentException() {
            String schemaJson = "{ \"type\": \"unknownType\" }";
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> createSchema(schemaJson));
            assertTrue(ex.getMessage().toLowerCase().contains("unknown"));
        }

        @Test
        void whenSchemaHasUnsupportedKeyword_thenUnsupportedOperationException() {
            String schemaJson = "{ \"type\": \"object\", \"fooValidator\": true }";
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> createSchema(schemaJson));
            assertTrue(ex.getMessage().toLowerCase().contains("fooValidator"));
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper is thread‑safe; initialise once for the whole test class.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Simple schema that requires a string property "name".
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";
        // Use the factory to obtain a JsonSchema instance – this respects all
        // internal constructor requirements without us having to build the
        // complex dependency graph manually.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
        this.schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
        mapper = null;
    }

    /**
     * Helper to build a {@link JsonSchema} from a JSON string.
     */
    private static JsonSchema createSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    private static Stream<Arguments> provideNullCustomizerValidObject() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode valid = om.readTree("{\"name\":\"Alice\",\"age\":25}");
        return Stream.of(Arguments.of(valid, null, "Null customizer with a valid object should produce empty set"));
    }

    @Test
    void validate_WithConsumerCustomizer_ReturnsExpectedMessages() throws Exception {
        // JSON instance where "name" is an integer – violates the schema.
        String jsonData = "{ \"name\": 1 }";
        JsonNode rootNode = mapper.readTree(jsonData);
        // A no‑op customizer; we only need to exercise the overload.
        Consumer<ExecutionContext> customizer = ctx -> {
            // No modifications required for this test.
        };
        Set<ValidationMessage> messages = schema.validate(rootNode, customizer);
        // The schema should produce exactly one validation error (type mismatch).
        assertEquals(1, messages.size(), "Expected a single validation error");
        ValidationMessage message = messages.iterator().next();
        // The default message contains the word "type" for a type mismatch.
        assertTrue(message.getMessage().toLowerCase().contains("type"), "Validation message should indicate a type problem");
    }

    /* Schema resource root flag (simple smoke test)                         */
    @Test
    void whenSchemaIsResourceRoot_thenValidatesSuccessfully() throws Exception {
        String schemaJson = "{\n" + "  \"$id\": \"http://example.com/schema\",\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": { \"id\": { \"type\": \"string\", \"format\": \"uri\" } }\n" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"id\":\"http://example.com/item\"}");
        Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
        });
        assertTrue(msgs.isEmpty(), "Schema marked as resource root should validate normally");
    }

    /* Malformed $ref handling                                                */
    @Test
    void whenSchemaContainsMalformedRef_thenUnsupportedEncodingException() throws Exception {
        String schemaJson = "{ \"$ref\": \"%ZZ\" }";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");
        // The exception is thrown during validation when URLDecoder.decode is invoked.
        assertThrows(UnsupportedEncodingException.class, () -> schema.validate(node, ctx -> {
        }));
    }

    /* Infinite recursion without recursive anchor                           */
    @Test
    void whenCircularRefWithoutRecursiveAnchor_thenStackOverflowError() {
        String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } },\n" + "  \"$ref\": \"#/definitions/self\"\n" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");
        assertThrows(StackOverflowError.class, () -> schema.validate(node, ctx -> {
        }));
    }

    /* Parameterized tests for groups that share the same structure           */
    @ParameterizedTest(name = "{index} => root={0}, expected={2}")
    @MethodSource("provideNullCustomizerValidObject")
    void whenNullCustomizerAndValidObject_thenEmptySet(JsonNode rootNode, Consumer<ExecutionContext> customizer, String description) {
        // Schema that requires a non‑null "name" string.
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": {\"type\":\"string\"} }, \"required\": [\"name\"] }";
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(rootNode, customizer);
        assertTrue(msgs.isEmpty(), description);
    }
}
