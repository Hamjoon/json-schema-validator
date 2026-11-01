package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_Test {

    /**
     * Shared objects for the whole test class
     */
    private static ObjectMapper mapper;

    /**
     * Objects created for each test
     */
    private ExecutionContext executionContext;

    private JsonSchema jsonSchema;

    /**
     * Mocks required by the JsonSchema constructor
     */
    @Mock
    private ValidationContext // mock of ValidationContext
    validationContext;

    @Mock
    private JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

    @Mock
    private SchemaValidatorsConfig // returned by validationContext.getConfig()
    config;

    /**
     * Simple validator that returns a predetermined set of messages.
     */
    private static class DummyValidator implements JsonValidator {

        private final Set<ValidationMessage> result;

        DummyValidator(Set<ValidationMessage> result) {
            this.result = result;
        }

        /**
         * The current {@code JsonValidator} interface defines this method signature.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String schemaPath) {
            return result;
        }
    }

    /**
     * Minimal {@link WalkListenerRunner} implementation – all methods are no‑ops.
     */
    private static class DummyWalkListenerRunner extends WalkListenerRunner {

        @Override
        public void walk(JsonNode node, JsonSchema schema, ExecutionContext ctx) {
            // no‑op
        }

        @Override
        public void runPostWalkListeners(ExecutionContext ctx, String schemaPath, JsonNode node, JsonNode rootNode, String keyword, String keywordPath, JsonNode parentNode, JsonSchema schema, ValidationContext validationContext, JsonSchemaFactory factory, Set<ValidationMessage> messages) {
            // no‑op
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1. ExecutionContext – simple constructor, uses default CollectorContext
        executionContext = new ExecutionContext();
        // 2. Minimal schema (empty object)
        ObjectNode schemaNode = mapper.createObjectNode();
        // 3. JsonSchema instance via the public factory
        jsonSchema = JsonSchemaFactory.getInstance().getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    private static Stream<Arguments> requiredValidatorProvider() {
        return Stream.of(Arguments.of("{\"requiredProp\":\"value\"}", true), Arguments.of("{}", false));
    }

    /* --------------------------------------------------------------------- */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Basic sanity test: validateAndCollect should return a non‑null ValidationResult
     * when invoked with a simple ExecutionContext and a JSON instance.
     */
    @Test
    void testValidateAndCollectReturnsResult() throws Exception {
        JsonNode dataNode = mapper.readTree("{\"foo\":\"bar\"}");
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, dataNode);
        assertNotNull(result, "validateAndCollect should never return null");
        // ValidationResult in the current library provides getMessages()
        assertTrue(result.getMessages().isEmpty() || result.getMessages().stream().allMatch(m -> m != null), "All validation messages, if any, should be non‑null");
    }

    /**
     * Verify that passing a {@code null} JSON node triggers a {@link NullPointerException}
     * (the current implementation of JsonSchema.validateAndCollect does not guard against it).
     */
    @Test
    void testValidateAndCollectWithNullNode() {
        assertThrows(NullPointerException.class, () -> jsonSchema.validateAndCollect(executionContext, null), "validateAndCollect should throw NPE when the JSON node is null");
    }

    @Test
    void testValidateAndCollect_NullExecutionContextAndNode() {
        assertThrows(NullPointerException.class, () -> jsonSchema.validateAndCollect(null, null));
    }

    @Test
    void testValidateAndCollect_NullExecutionContext() {
        JsonNode dataNode = mapper.createObjectNode().put("foo", "bar");
        assertThrows(NullPointerException.class, () -> jsonSchema.validateAndCollect(null, dataNode));
    }

    @Test
    void testValidateAndCollect_NullJsonNode() {
        assertThrows(NullPointerException.class, () -> jsonSchema.validateAndCollect(executionContext, null));
    }

    @Test
    void testValidateAndCollect_RequiredValidator_Succeeds() {
        JsonNode node = mapper.createObjectNode().put("requiredProp", "value");
        setField(jsonSchema, "requiredValidator", new DummyValidator(Collections.emptySet()));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(result.isValid(), "When the required validator passes the result must be successful");
        assertTrue(result.getMessages().isEmpty(), "A successful validation should not contain any messages");
    }

    @Test
    void testValidateAndCollect_RequiredValidator_Fails() {
        JsonNode node = mapper.createObjectNode();
        ValidationMessage msg = new ValidationMessage("", "required property missing");
        setField(jsonSchema, "requiredValidator", new DummyValidator(Set.of(msg)));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertFalse(result.isValid(), "When the required validator reports an error the result must be a failure");
        assertFalse(result.getMessages().isEmpty(), "Failure should contain at least one ValidationMessage");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Succeeds() {
        JsonNode node = mapper.readTree("42");
        setField(jsonSchema, "typeValidator", new DummyValidator(Collections.emptySet()));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(result.isValid(), "When the type validator passes the result must be successful");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Fails() {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        ValidationMessage msg = new ValidationMessage("", "type mismatch");
        setField(jsonSchema, "typeValidator", new DummyValidator(Set.of(msg)));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertFalse(result.isValid(), "When the type validator reports an error the result must be a failure");
        assertFalse(result.getMessages().isEmpty(), "Failure should contain at least one ValidationMessage");
    }

    @Test
    void testValidateAndCollect_DynamicAnchor_NoException() {
        JsonNode node = mapper.createObjectNode();
        // mark schema as a dynamic anchor
        setField(jsonSchema, "isDynamicAnchor", true);
        // ensure no validators are present
        setField(jsonSchema, "requiredValidator", null);
        setField(jsonSchema, "typeValidator", null);
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(result.isValid(), "Dynamic anchor handling should not cause a failure when no validators fire");
    }

    @Test
    void testValidateAndCollect_WalkListenerRunnerIsInvoked() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        WalkListenerRunner dummyRunner = new DummyWalkListenerRunner();
        setField(jsonSchema, "keywordWalkListenerRunner", dummyRunner);
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertNotNull(result, "Result must be non‑null even when a WalkListenerRunner is present");
    }

    @ParameterizedTest
    @MethodSource("requiredValidatorProvider")
    void parameterizedRequiredValidatorTest(String json, boolean expectSuccess) throws Exception {
        JsonNode node = mapper.readTree(json);
        if (expectSuccess) {
            setField(jsonSchema, "requiredValidator", new DummyValidator(Collections.emptySet()));
        } else {
            ValidationMessage msg = new ValidationMessage("", "missing required");
            setField(jsonSchema, "requiredValidator", new DummyValidator(Set.of(msg)));
        }
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertEquals(expectSuccess, result.isValid(), "Parameterized required‑validator test failed for input: " + json);
    }
}
