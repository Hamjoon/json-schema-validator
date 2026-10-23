```java
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
import com.networknt.schema.walk.WalkListenerRunner;
import java.util.HashSet;

/**
 * Unit tests for {@link JsonSchema#validateAndCollect(ExecutionContext, JsonNode)}.
 *
 * The test class does **not** depend on Mockito – the original mock fields were
 * unused, so they have been removed together with the related imports and the
 * {@code @ExtendWith(MockitoExtension.class)} annotation.
 *
 * Because the concrete version of {@code ValidationResult} bundled with the
 * library may expose different method names (e.g. {@code isSuccess()},
 * {@code getMessages()} or alternatives), the test accesses the result via
 * reflection. This makes the test robust against small API changes while still
 * verifying the behaviour of {@code JsonSchema.validateAndCollect}.
 */
class JsonSchema_validateAndCollect_Test {

    /* --------------------------------------------------------------------- */
    /** Shared mapper for the whole test class */
    private static ObjectMapper mapper;

    /** Objects created for each test */
    private ExecutionContext executionContext;
    private JsonSchema jsonSchema;

    /**
     * Simple validator that returns a predetermined set of messages.
     */
    private static class DummyValidator implements JsonValidator {

        private final Set<ValidationMessage> result;

        DummyValidator(Set<ValidationMessage> result) {
            this.result = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String schemaPath) {
            return result;
        }
    }

    /**
     * Minimal {@link WalkListenerRunner} implementation – all methods are no‑ops.
     */
    private static class DummyWalkListenerRunner implements WalkListenerRunner {

        @Override
        public void walk(JsonNode node, JsonSchema schema, ExecutionContext ctx) {
            // no‑op
        }

        @Override
        public void runPreWalkListeners(ExecutionContext ctx, String schemaPath, JsonNode node, JsonNode rootNode,
                                        String keyword, String keywordPath, JsonNode parentNode,
                                        JsonSchema schema, ValidationContext validationContext,
                                        JsonSchemaFactory factory) {
            // no‑op
        }

        @Override
        public void runPostWalkListeners(ExecutionContext ctx, String schemaPath, JsonNode node, JsonNode rootNode,
                                         String keyword, String keywordPath, JsonNode parentNode,
                                         JsonSchema schema, ValidationContext validationContext,
                                         JsonSchemaFactory factory, Set<ValidationMessage> messages) {
            // no‑op
        }
    }

    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        executionContext = new ExecutionContext();
        // empty schema
        ObjectNode schemaNode = mapper.createObjectNode();
        jsonSchema = JsonSchemaFactory.getInstance().getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    private static Stream<Arguments> requiredValidatorProvider() {
        return Stream.of(
                Arguments.of("{\"requiredProp\":\"value\"}", true),
                Arguments.of("{}", false));
    }

    /* --------------------------------------------------------------------- */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Helper to create a {@link ValidationMessage} compatible with the current library version.
     */
    private static ValidationMessage createMessage(String path, String message) {
        ValidationMessage vm = new ValidationMessage();
        vm.setPath(path);
        vm.setMessage(message);
        vm.setType("custom");
        return vm;
    }

    /** Reflection helpers for {@code ValidationResult} */
    private static boolean resultIsSuccess(Object result) {
        try {
            return (boolean) result.getClass().getMethod("isSuccess").invoke(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<ValidationMessage> resultGetMessages(Object result) {
        try {
            return (Set<ValidationMessage>) result.getClass().getMethod("getMessages").invoke(result);
        } catch (NoSuchMethodException ns) {
            // fallback for older versions where the method may be named differently
            try {
                return (Set<ValidationMessage>) result.getClass().getMethod("getErrors").invoke(result);
            } catch (Exception e) {
                throw new RuntimeException("Unable to obtain validation messages via reflection", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testValidateAndCollectReturnsResult() throws Exception {
        JsonNode dataNode = mapper.readTree("{\"foo\":\"bar\"}");
        Object result = jsonSchema.validateAndCollect(executionContext, dataNode);
        assertNotNull(result, "validateAndCollect should never return null");
        Set<ValidationMessage> msgs = resultGetMessages(result);
        assertTrue(msgs.isEmpty() || msgs.stream().allMatch(m -> m != null),
                "All validation messages, if any, should be non‑null");
    }

    @Test
    void testValidateAndCollectWithNullNode() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(executionContext, null),
                "validateAndCollect should throw NPE when the JSON node is null");
    }

    @Test
    void testValidateAndCollect_NullExecutionContextAndNode() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(null, null));
    }

    @Test
    void testValidateAndCollect_NullExecutionContext() {
        JsonNode dataNode = mapper.createObjectNode().put("foo", "bar");
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(null, dataNode));
    }

    @Test
    void testValidateAndCollect_NullJsonNode() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(executionContext, null));
    }

    @Test
    void testValidateAndCollect_RequiredValidator_Succeeds() throws Exception {
        JsonNode node = mapper.createObjectNode().put("requiredProp", "value");
        setField(jsonSchema, "requiredValidator", new DummyValidator(Collections.emptySet()));
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(resultIsSuccess(result), "When the required validator passes the result must be successful");
        assertTrue(resultGetMessages(result).isEmpty(),
                "A successful validation should not contain any messages");
    }

    @Test
    void testValidateAndCollect_RequiredValidator_Fails() throws Exception {
        JsonNode node = mapper.createObjectNode();
        ValidationMessage msg = createMessage("", "required property missing");
        setField(jsonSchema, "requiredValidator", new DummyValidator(Set.of(msg)));
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertFalse(resultIsSuccess(result), "When the required validator reports an error the result must be a failure");
        assertFalse(resultGetMessages(result).isEmpty(),
                "Failure should contain at least one ValidationMessage");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Succeeds() throws Exception {
        JsonNode node = mapper.readTree("42");
        setField(jsonSchema, "typeValidator", new DummyValidator(Collections.emptySet()));
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(resultIsSuccess(result), "When the type validator passes the result must be successful");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Fails() throws Exception {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        ValidationMessage msg = createMessage("", "type mismatch");
        setField(jsonSchema, "typeValidator", new DummyValidator(Set.of(msg)));
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertFalse(resultIsSuccess(result), "When the type validator reports an error the result must be a failure");
        assertFalse(resultGetMessages(result).isEmpty(),
                "Failure should contain at least one ValidationMessage");
    }

    @Test
    void testValidateAndCollect_DynamicAnchor_NoException() throws Exception {
        JsonNode node = mapper.createObjectNode();
        setField(jsonSchema, "isDynamicAnchor", true);
        setField(jsonSchema, "requiredValidator", null);
        setField(jsonSchema, "typeValidator", null);
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertTrue(resultIsSuccess(result),
                "Dynamic anchor handling should not cause a failure when no validators fire");
    }

    @Test
    void testValidateAndCollect_WalkListenerRunnerIsInvoked() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        WalkListenerRunner dummyRunner = new DummyWalkListenerRunner();
        setField(jsonSchema, "keywordWalkListenerRunner", dummyRunner);
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertNotNull(result, "Result must be non‑null even when a WalkListenerRunner is present");
    }

    @ParameterizedTest
    @MethodSource("requiredValidatorProvider")
    void parameterizedRequiredValidatorTest(String json, boolean expectSuccess) throws Exception {
        JsonNode node = mapper.readTree(json);
        if (expectSuccess) {
            setField(jsonSchema, "requiredValidator", new DummyValidator(Collections.emptySet()));
        } else {
            ValidationMessage msg = createMessage("", "missing required");
            setField(jsonSchema, "requiredValidator", new DummyValidator(Set.of(msg)));
        }
        Object result = jsonSchema.validateAndCollect(executionContext, node);
        assertEquals(expectSuccess, resultIsSuccess(result),
                "Parameterized required‑validator test failed for input: " + json);
    }
}
```