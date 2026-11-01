package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.net.URI;
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

    private static class DummyValidator implements JsonValidator {

        private final Set<ValidationMessage> result;

        DummyValidator(Set<ValidationMessage> result) {
            this.result = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonSchema schema) {
            return result;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---------- 1. Prepare ExecutionContext ----------
        // simple ctor, uses default CollectorContext
        executionContext = new ExecutionContext();
        // ---------- 2. Prepare a minimal schema (empty object) ----------
        ObjectNode schemaNode = mapper.createObjectNode();
        // ---------- 3. Create the JsonSchema instance ----------
        // Use the public factory to avoid dealing with ValidationContext directly.
        jsonSchema = JsonSchemaFactory.getInstance().getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    private static Stream<Arguments> requiredValidatorProvider() {
        // Example provider – not used in the current set of tests but shows
        // how a parameterised test could be wired.
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
        // inject a required validator that always succeeds
        setField(jsonSchema, "requiredValidator", new DummyValidator(Collections.emptySet()));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(result.isSuccess(), "When the required validator passes the result must be successful");
        assertTrue(result.getMessages().isEmpty(), "A successful validation should not contain any messages");
    }

    @Test
    void testValidateAndCollect_RequiredValidator_Fails() {
        JsonNode node = mapper.createObjectNode();
        ValidationMessage msg = ValidationMessage.of("", "required property missing");
        setField(jsonSchema, "requiredValidator", new DummyValidator(Set.of(msg)));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertFalse(result.isSuccess(), "When the required validator reports an error the result must be a failure");
        assertFalse(result.getMessages().isEmpty(), "Failure should contain at least one ValidationMessage");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Succeeds() {
        JsonNode node = mapper.readTree("42");
        setField(jsonSchema, "typeValidator", new DummyValidator(Collections.emptySet()));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(result.isSuccess(), "When the type validator passes the result must be successful");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Fails() {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        ValidationMessage msg = ValidationMessage.of("", "type mismatch");
        setField(jsonSchema, "typeValidator", new DummyValidator(Set.of(msg)));
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertFalse(result.isSuccess(), "When the type validator reports an error the result must be a failure");
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
        assertTrue(result.isSuccess(), "Dynamic anchor handling should not cause a failure when no validators fire");
    }

    @Test
    void testValidateAndCollect_WalkListenerRunnerIsInvoked() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        // Provide a dummy WalkListenerRunner that does nothing.
        WalkListenerRunner dummyRunner = new WalkListenerRunner() {

            @Override
            public void walk(JsonNode node, JsonSchema schema, ExecutionContext ctx) {
                // no‑op
            }
        };
        setField(jsonSchema, "keywordWalkListenerRunner", dummyRunner);
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertNotNull(result, "Result must be non‑null even when a WalkListenerRunner is present");
        // No further verification needed – the test ensures injection does not break execution.
    }

    @ParameterizedTest
    @MethodSource("requiredValidatorProvider")
    void parameterizedRequiredValidatorTest(String json, boolean expectSuccess) throws Exception {
        JsonNode node = mapper.readTree(json);
        if (expectSuccess) {
            setField(jsonSchema, "requiredValidator", new DummyValidator(Collections.emptySet()));
        } else {
            ValidationMessage msg = ValidationMessage.of("", "missing required");
            setField(jsonSchema, "requiredValidator", new DummyValidator(Set.of(msg)));
        }
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertEquals(expectSuccess, result.isSuccess(), "Parameterized required‑validator test failed for input: " + json);
    }
}
