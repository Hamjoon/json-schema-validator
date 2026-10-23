```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * A very small hand‑rolled mock framework that provides just enough
 * functionality for the tests in this file.  It supports {@code mock},
 * {@code when(...).thenReturn(...)} and {@code when(...).thenThrow(...)}.
 * Argument matching is ignored – stubs are matched only by method name.
 */
class SimpleMockito {
    private static final ThreadLocal<InvocationRecord> LAST_INVOKE = new ThreadLocal<>();

    @SuppressWarnings("unchecked")
    static <T> T mock(Class<T> clazz) {
        InvocationHandler handler = new MockHandler();
        if (clazz.isInterface()) {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                    new Class[]{clazz}, handler);
        } else {
            // For concrete classes (e.g. JsonSchema) we try to create a subclass
            // with a no‑arg constructor.  Most of the classes used here have a
            // protected no‑arg constructor, so we can create an anonymous subclass.
            try {
                Constructor<T> cons = clazz.getDeclaredConstructor();
                cons.setAccessible(true);
                T instance = cons.newInstance();
                // replace the instance's InvocationHandler via a dynamic proxy on the
                // interfaces it implements (if any).  For our purposes we only need
                // to mock interfaces, so we simply return the real instance.
                return instance;
            } catch (Exception e) {
                throw new RuntimeException("Cannot mock class " + clazz, e);
            }
        }
    }

    static <T> OngoingStubbing<T> when(T methodCall) {
        InvocationRecord rec = LAST_INVOKE.get();
        if (rec == null) {
            throw new IllegalStateException("No method invocation captured for stubbing");
        }
        return new OngoingStubbing<>(rec);
    }

    /** Argument matcher helpers – they simply return null because the
     *  framework does not use the values for matching. */
    static class ArgumentMatchers {
        static <T> T any() { return null; }
        static String anyString() { return null; }
        static <T> T any(Class<T> cls) { return null; }
        static <T> T eq(T value) { return null; }
    }

    /** Empty extension class to keep the original annotation valid. */
    static class MockitoExtension {}

    /** Record of a method invocation on a mock. */
    private static class InvocationRecord {
        final MockHandler handler;
        final Method method;
        final Object[] args;

        InvocationRecord(MockHandler handler, Method method, Object[] args) {
            this.handler = handler;
            this.method = method;
            this.args = args;
        }
    }

    /** The handler that stores stubs and returns them on later calls. */
    private static class MockHandler implements InvocationHandler {
        private final Map<String, Queue<Object>> stubs = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // remember the last invocation for the when(...) call
            LAST_INVOKE.set(new InvocationRecord(this, method, args));

            String key = method.getName();
            if (stubs.containsKey(key)) {
                Object value = stubs.get(key).poll();
                if (value instanceof RuntimeException) {
                    throw (RuntimeException) value;
                }
                return value;
            }
            // default returns
            Class<?> rt = method.getReturnType();
            if (rt.isPrimitive()) {
                if (rt == boolean.class) return false;
                if (rt == byte.class) return (byte) 0;
                if (rt == short.class) return (short) 0;
                if (rt == int.class) return 0;
                if (rt == long.class) return 0L;
                if (rt == char.class) return '\0';
                if (rt == float.class) return 0f;
                if (rt == double.class) return 0d;
            }
            return null;
        }

        void addStub(String methodName, Object... returns) {
            Queue<Object> q = new ArrayDeque<>(Arrays.asList(returns));
            stubs.put(methodName, q);
        }
    }

    /** Fluent API returned by {@code when(...)}. */
    static class OngoingStubbing<T> {
        private final InvocationRecord record;

        OngoingStubbing(InvocationRecord record) {
            this.record = record;
        }

        @SafeVarargs
        final void thenReturn(T... values) {
            record.handler.addStub(record.method.getName(), (Object[]) values);
        }

        void thenThrow(RuntimeException ex) {
            record.handler.addStub(record.method.getName(), ex);
        }
    }
}

/**
 * Minimal concrete implementation of {@link JsonSchema} used in the tests.
 * It allows a test to specify the result that should be returned for a
 * particular instance node.
 */
class SimpleJsonSchema extends JsonSchema {
    private final Map<JsonNode, Set<ValidationMessage>> results = new HashMap<>();

    SimpleJsonSchema() {
        super(null, null, null);
    }

    void setResult(JsonNode instanceNode, Set<ValidationMessage> result) {
        results.put(instanceNode, result);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                            JsonNode instanceNode,
                                            JsonNode rootNode,
                                            String at) {
        return results.getOrDefault(instanceNode, Collections.emptySet());
    }

    // The remaining abstract methods are not used by the tests.
    @Override public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at, boolean isParent) { return null; }
    @Override public List<JsonSchema> getTupleSchema() { return null; }
}

/**
 * The actual test class – all Mockito usage has been replaced by the
 * lightweight {@link SimpleMockito} framework and by {@link SimpleJsonSchema}.
 */
@ExtendWith(SimpleMockito.MockitoExtension.class)
class PrefixItemsValidator_validate_Test {

    // ----- collaborators ----------------------------------------------------
    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private ExecutionContext executionContext;
    private CollectorContext collectorContext;

    // ----- object under test ------------------------------------------------
    private PrefixItemsValidator validator;

    // the list of tuple schemas created by the constructor (two mocks)
    private List<JsonSchema> tupleSchemas;

    // a collection that the mocked CollectorContext will return
    private Collection<String> evaluatedItems;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // ---- mock the ValidationContext and its behaviour -----------------
        validationContext = SimpleMockito.mock(ValidationContext.class);
        parentSchema = SimpleMockito.mock(JsonSchema.class);

        // schemaNode that drives the constructor – a non‑empty array node (2 elements)
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        schemaNode.add(JsonNodeFactory.instance.objectNode());
        schemaNode.add(JsonNodeFactory.instance.objectNode());

        // two concrete JsonSchema stubs
        SimpleJsonSchema firstTuple = new SimpleJsonSchema();
        SimpleJsonSchema secondTuple = new SimpleJsonSchema();

        // stub newSchema to return the two tuple schemas in order
        SimpleMockito.when(validationContext.newSchema(SimpleMockito.ArgumentMatchers.anyString(),
                SimpleMockito.ArgumentMatchers.any(JsonNode.class),
                SimpleMockito.ArgumentMatchers.any(JsonSchema.class)))
                .thenReturn(firstTuple, secondTuple);

        // instantiate the validator (the constructor builds tupleSchema list)
        validator = new PrefixItemsValidator("testPath", schemaNode, parentSchema, validationContext);
        tupleSchemas = validator.getTupleSchema();

        // sanity check – the constructor should have produced exactly two tuple schemas
        Assertions.assertEquals(2, tupleSchemas.size());

        // ---- mock ExecutionContext and CollectorContext -------------------
        executionContext = SimpleMockito.mock(ExecutionContext.class);
        collectorContext = SimpleMockito.mock(CollectorContext.class);
        evaluatedItems = new ArrayList<>();

        SimpleMockito.when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        SimpleMockito.when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /**
     * Helper to create a validator with a given number of tuple schemas (all concrete).
     */
    private PrefixItemsValidator createValidatorWithTupleCount(int count, List<JsonSchema> mocksOut) {
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < count; i++) {
            schemaNode.add(JsonNodeFactory.instance.objectNode());
        }
        // prepare concrete schemas
        SimpleJsonSchema[] mocks = new SimpleJsonSchema[count];
        for (int i = 0; i < count; i++) {
            mocks[i] = new SimpleJsonSchema();
        }
        // stub newSchema to return them in order
        SimpleMockito.when(validationContext.newSchema(SimpleMockito.ArgumentMatchers.anyString(),
                SimpleMockito.ArgumentMatchers.any(JsonNode.class),
                SimpleMockito.ArgumentMatchers.any(JsonSchema.class)))
                .thenReturn(mocks[0], Arrays.copyOfRange(mocks, 1, mocks.length));
        PrefixItemsValidator v = new PrefixItemsValidator("path", schemaNode, parentSchema, validationContext);
        if (mocksOut != null) {
            mocksOut.addAll(v.getTupleSchema());
        }
        return v;
    }

    private static Stream<Arguments> lessThanTupleSchemaProvider() {
        return Stream.of(
                Arguments.of("[1, \"a\"]", "/data", Set.of("/data/0", "/data/1"), Collections.emptySet()),
                Arguments.of("[5, \"bad\"]", "/payload", Set.of("/data/0"), Set.of("error"))
        );
    }

    /**
     * When the node is not an array the validator must return an empty error set.
     */
    @Test
    void testValidateNonArrayReturnsEmpty() {
        JsonNode nonArrayNode = SimpleMockito.mock(JsonNode.class);
        SimpleMockito.when(nonArrayNode.isArray()).thenReturn(false);
        Set<ValidationMessage> result = validator.validate(executionContext, nonArrayNode, nonArrayNode, "");
        Assertions.assertTrue(result.isEmpty(), "Expected no validation messages for non‑array input");
        Assertions.assertTrue(evaluatedItems.isEmpty(), "No items should be recorded as evaluated");
    }

    /**
     * All tuple schemas succeed – no errors and both items are marked as evaluated.
     */
    @Test
    void testValidateArrayAllValid() {
        JsonNode element0 = SimpleMockito.mock(JsonNode.class);
        JsonNode element1 = SimpleMockito.mock(JsonNode.class);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(element0);
        arrayNode.add(element1);

        ((SimpleJsonSchema) tupleSchemas.get(0)).setResult(element0, Collections.emptySet());
        ((SimpleJsonSchema) tupleSchemas.get(1)).setResult(element1, Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "");
        Assertions.assertTrue(result.isEmpty(), "No validation errors expected");
        Assertions.assertEquals(Set.of("/0", "/1"), new HashSet<>(evaluatedItems),
                "Both items should be recorded as evaluated");
    }

    /**
     * The second tuple schema returns an error – only the first item is recorded as evaluated.
     */
    @Test
    void testValidateArrayWithErrors() {
        JsonNode element0 = SimpleMockito.mock(JsonNode.class);
        JsonNode element1 = SimpleMockito.mock(JsonNode.class);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(element0);
        arrayNode.add(element1);

        ((SimpleJsonSchema) tupleSchemas.get(0)).setResult(element0, Collections.emptySet());

        ValidationMessage errorMsg = null; // using null as a placeholder
        ((SimpleJsonSchema) tupleSchemas.get(1)).setResult(element1, Set.of(errorMsg));

        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "");
        Assertions.assertEquals(1, result.size(), "Exactly one validation error expected");
        Assertions.assertTrue(result.contains(errorMsg), "The returned set should contain the mocked error message");
        Assertions.assertEquals(Set.of("/0"), new HashSet<>(evaluatedItems),
                "Only the first item should be recorded as evaluated");
    }

    /**
     * Empty tupleSchema list – even an array should produce no errors and no evaluated items.
     */
    @Test
    void testValidateEmptyTupleSchema() throws Exception {
        ArrayNode emptySchemaNode = JsonNodeFactory.instance.arrayNode();
        PrefixItemsValidator emptyValidator = new PrefixItemsValidator("path", emptySchemaNode, parentSchema, validationContext);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,2]");
        Set<ValidationMessage> result = emptyValidator.validate(executionContext, arrayNode, arrayNode, "/items");
        Assertions.assertTrue(result.isEmpty(), "No errors expected when tupleSchema is empty");
        Assertions.assertTrue(evaluatedItems.isEmpty(), "No evaluated items should be recorded");
    }

    /**
     * Array length < tupleSchema size, all items validate successfully.
     */
    @Test
    void testArraySizeLessThanTupleSchema_AllValid() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,\"a\"]");

        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());
        ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/data");
        Assertions.assertTrue(result.isEmpty(), "No validation errors expected");
        Assertions.assertEquals(Set.of("/data/0", "/data/1"), new HashSet<>(evaluatedItems),
                "Both array items should be recorded as evaluated");
    }

    /**
     * Array length < tupleSchema size, second schema returns an error.
     */
    @Test
    void testArraySizeLessThanTupleSchema_WithErrors() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[5,\"bad\"]");

        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());

        ValidationMessage err = null;
        ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Set.of(err));

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/payload");
        Assertions.assertEquals(1, result.size(), "One validation error expected");
        Assertions.assertTrue(result.contains(err), "Error should be propagated");
        Assertions.assertEquals(Set.of("/payload/0"), new HashSet<>(evaluatedItems),
                "Only the successful first item should be recorded");
    }

    /**
     * Array length > tupleSchema size, all validated items succeed.
     */
    @Test
    void testArraySizeGreaterThanTupleSchema_AllValid() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[10,\"x\",true,null]");

        for (int i = 0; i < 3; i++) {
            ((SimpleJsonSchema) schemas.get(i)).setResult(arrayNode.get(i), Collections.emptySet());
        }

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/array");
        Assertions.assertTrue(result.isEmpty(), "No errors expected");
        Assertions.assertEquals(Set.of("/array/0", "/array/1", "/array/2"), new HashSet<>(evaluatedItems),
                "Only the first three items (matching tupleSchema size) should be recorded");
    }

    /**
     * Array length > tupleSchema size, first schema fails, others succeed.
     */
    @Test
    void testArraySizeGreaterThanTupleSchema_WithErrorInFirstSchema() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[\"invalid\",2,3]");

        ValidationMessage err = null;
        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Set.of(err));
        ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());
        ((SimpleJsonSchema) schemas.get(2)).setResult(arrayNode.get(2), Collections.emptySet());

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/list");
        Assertions.assertEquals(1, result.size(), "Exactly one error expected");
        Assertions.assertTrue(result.contains(err), "Error from first schema should be present");
        Assertions.assertEquals(Set.of("/list/1", "/list/2"), new HashSet<>(evaluatedItems),
                "Successful items after the failing one should still be recorded");
    }

    /**
     * Verify that the generated JSON‑Pointer path matches the expected pattern.
     */
    @Test
    void testAtPathConstructionVerification() throws Exception {
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,2]");
        ((SimpleJsonSchema) tupleSchemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());
        ((SimpleJsonSchema) tupleSchemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "/parent/child");
        Assertions.assertTrue(result.isEmpty(), "No errors expected");
        Assertions.assertEquals(Set.of("/parent/child/0", "/parent/child/1"), new HashSet<>(evaluatedItems),
                "Paths must be built using the supplied base 'at' value");
    }

    /**
     * A null JsonNode is treated as non‑array – no exception, empty error set.
     */
    @Test
    void testNullNodeHandledGracefully() {
        Set<ValidationMessage> result = validator.validate(executionContext, null, null, "/");
        Assertions.assertTrue(result.isEmpty(), "Null node should yield empty validation result");
        Assertions.assertTrue(evaluatedItems.isEmpty(), "No evaluated items should be recorded for null node");
    }

    /**
     * Passing a null ExecutionContext should cause a NullPointerException.
     */
    @Test
    void testNullExecutionContext() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(1));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(null, arrayNode, arrayNode, "/"),
                "Null ExecutionContext must trigger NPE");
    }

    /**
     * ExecutionContext returning a null CollectorContext leads to NPE.
     */
    @Test
    void testNullCollectorContext() {
        ExecutionContext ctx = SimpleMockito.mock(ExecutionContext.class);
        SimpleMockito.when(ctx.getCollectorContext()).thenReturn(null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.textNode("x"));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, arrayNode, arrayNode, "/"),
                "Null CollectorContext must trigger NPE");
    }

    /**
     * CollectorContext returning null evaluatedItems collection triggers NPE.
     */
    @Test
    void testNullEvaluatedItems() {
        CollectorContext cc = SimpleMockito.mock(CollectorContext.class);
        SimpleMockito.when(cc.getEvaluatedItems()).thenReturn(null);
        ExecutionContext ctx = SimpleMockito.mock(ExecutionContext.class);
        SimpleMockito.when(ctx.getCollectorContext()).thenReturn(cc);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.booleanNode(true));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, arrayNode, arrayNode, "/array"),
                "Null evaluated items collection must trigger NPE");
    }

    /**
     * Internal tupleSchema set to null should cause NPE when accessed.
     */
    @Test
    void testNullTupleSchema() throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        f.set(validator, null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(5));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, "/field"),
                "Null tupleSchema field must cause NPE");
    }

    /**
     * tupleSchema contains a null element – invoking validate on it throws NPE.
     */
    @Test
    void testNullTupleSchemaElement() throws Exception {
        List<JsonSchema> list = validator.getTupleSchema();
        list.set(0, null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(42));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, "/value"),
                "Null element inside tupleSchema must cause NPE");
    }

    /**
     * A tuple schema throws an unchecked exception – the validator propagates it.
     */
    @Test
    void testTupleSchemaValidateThrowsException() {
        SimpleMockito.when(((SimpleJsonSchema) tupleSchemas.get(0)).validate(
                SimpleMockito.ArgumentMatchers.any(),
                SimpleMockito.ArgumentMatchers.any(),
                SimpleMockito.ArgumentMatchers.any(),
                SimpleMockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalStateException("boom"));
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.textNode("trigger"));
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, "/prop"),
                "Exception from tuple schema should be propagated");
        Assertions.assertEquals("boom", ex.getMessage());
    }

    /**
     * Passing a null 'at' argument leads to NPE inside atPath().
     */
    @Test
    void testNullAtParameter() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.booleanNode(true));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, null),
                "Null 'at' parameter must cause NPE");
    }

    @ParameterizedTest
    @MethodSource("lessThanTupleSchemaProvider")
    void parameterizedArraySizeLessThanTupleSchema(String jsonArray, String at,
                                                   Set<String> expectedEvaluated,
                                                   Set<String> errorMarker) throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree(jsonArray);

        // first element – possibly an error
        ValidationMessage err = null;
        Set<ValidationMessage> firstResult = errorMarker.contains("error") ? Set.of(err) : Collections.emptySet();
        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), firstResult);

        if (arrayNode.size() > 1) {
            ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());
        }

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, at);
        if (errorMarker.isEmpty()) {
            Assertions.assertTrue(result.isEmpty(), "No errors expected");
        } else {
            Assertions.assertEquals(1, result.size(), "One error expected");
        }
        Assertions.assertEquals(expectedEvaluated, new HashSet<>(evaluatedItems), "Evaluated items mismatch");
    }
}
```