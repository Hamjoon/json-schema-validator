package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static PrefixItemsValidator_validate_Test.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
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

    private static final ThreadLocal<InvocationRecord> LAST_INVOKE = new ThreadLocal<>();

    /**
     * Argument matcher helpers – they simply return null because the
     *  framework does not use the values for matching.
     */
    static class ArgumentMatchers {

        static <T> T any() {
            return null;
        }

        static String anyString() {
            return null;
        }

        static <T> T any(Class<T> cls) {
            return null;
        }

        static <T> T eq(T value) {
            return null;
        }
    }

    /**
     * Empty extension class to keep the original annotation valid.
     */
    static class MockitoExtension {
    }

    /**
     * Record of a method invocation on a mock.
     */
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

    /**
     * The handler that stores stubs and returns them on later calls.
     */
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
                if (rt == boolean.class)
                    return false;
                if (rt == byte.class)
                    return (byte) 0;
                if (rt == short.class)
                    return (short) 0;
                if (rt == int.class)
                    return 0;
                if (rt == long.class)
                    return 0L;
                if (rt == char.class)
                    return '\0';
                if (rt == float.class)
                    return 0f;
                if (rt == double.class)
                    return 0d;
            }
            return null;
        }

        void addStub(String methodName, Object... returns) {
            Queue<Object> q = new ArrayDeque<>(Arrays.asList(returns));
            stubs.put(methodName, q);
        }
    }

    /**
     * Fluent API returned by {@code when(...)}.
     */
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
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode instanceNode, JsonNode rootNode, String at) {
            return results.getOrDefault(instanceNode, Collections.emptySet());
        }

        // The remaining abstract methods are not used by the tests.
        @Override
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at, boolean isParent) {
            return null;
        }

        @Override
        public List<JsonSchema> getTupleSchema() {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---- mock the ValidationContext and its behaviour -----------------
        validationContext = mock(ValidationContext.class);
        parentSchema = mock(JsonSchema.class);
        // schemaNode that drives the constructor – a non‑empty array node (2 elements)
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        // first tuple schema
        schemaNode.add(JsonNodeFactory.instance.objectNode());
        // second tuple schema
        schemaNode.add(JsonNodeFactory.instance.objectNode());
        // each call to newSchema should return a fresh mock JsonSchema
        JsonSchema firstTuple = mock(JsonSchema.class);
        JsonSchema secondTuple = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(firstTuple, secondTuple);
        // instantiate the validator (the constructor builds tupleSchema list)
        validator = new PrefixItemsValidator("testPath", schemaNode, parentSchema, validationContext);
        tupleSchemas = validator.getTupleSchema();
        // sanity check – the constructor should have produced exactly two tuple schemas
        assertEquals(2, tupleSchemas.size());
        // ---- mock ExecutionContext and CollectorContext -------------------
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        evaluatedItems = new ArrayList<>();
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /**
     * Helper to create a validator with a given number of tuple schemas (all mocked).
     */
    private PrefixItemsValidator createValidatorWithTupleCount(int count, List<JsonSchema> mocksOut) {
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < count; i++) {
            schemaNode.add(JsonNodeFactory.instance.objectNode());
        }
        // Prepare mocks that will be returned by ValidationContext.newSchema
        JsonSchema[] mocks = new JsonSchema[count];
        for (int i = 0; i < count; i++) {
            mocks[i] = mock(JsonSchema.class);
        }
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mocks[0], Arrays.copyOfRange(mocks, 1, mocks.length));
        PrefixItemsValidator v = new PrefixItemsValidator("path", schemaNode, parentSchema, validationContext);
        if (mocksOut != null) {
            mocksOut.addAll(v.getTupleSchema());
        }
        return v;
    }

    private static Stream<Arguments> lessThanTupleSchemaProvider() {
        return Stream.of(Arguments.of("[1, \"a\"]", "/data", Set.of("/data/0", "/data/1"), Collections.emptySet()), Arguments.of("[5, \"bad\"]", "/payload", Set.of("/data/0"), Set.of("error")));
    }

    static <T> OngoingStubbing<T> when(T methodCall) {
        InvocationRecord rec = LAST_INVOKE.get();
        if (rec == null) {
            throw new IllegalStateException("No method invocation captured for stubbing");
        }
        return new OngoingStubbing<>(rec);
    }

    /**
     * When the node is not an array the validator must return an empty error set.
     */
    @Test
    void testValidateNonArrayReturnsEmpty() {
        JsonNode nonArrayNode = mock(JsonNode.class);
        when(nonArrayNode.isArray()).thenReturn(false);
        Set<ValidationMessage> result = validator.validate(executionContext, nonArrayNode, nonArrayNode, "");
        assertTrue(result.isEmpty(), "Expected no validation messages for non‑array input");
        assertTrue(evaluatedItems.isEmpty(), "No items should be recorded as evaluated");
    }

    /**
     * All tuple schemas succeed – no errors and both items are marked as evaluated.
     */
    @Test
    void testValidateArrayAllValid() {
        // ---- prepare an array node with two elements --------------------
        JsonNode element0 = mock(JsonNode.class);
        JsonNode element1 = mock(JsonNode.class);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(element0);
        arrayNode.add(element1);
        // stub the tuple schemas to return empty error sets
        when(tupleSchemas.get(0).validate(any(), eq(element0), any(), anyString())).thenReturn(Collections.emptySet());
        when(tupleSchemas.get(1).validate(any(), eq(element1), any(), anyString())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "");
        assertTrue(result.isEmpty(), "No validation errors expected");
        // the validator records the evaluated path for each successful item
        assertEquals(Set.of("/0", "/1"), new HashSet<>(evaluatedItems), "Both items should be recorded as evaluated");
    }

    /**
     * The second tuple schema returns an error – only the first item is recorded as evaluated.
     */
    @Test
    void testValidateArrayWithErrors() {
        // ---- prepare an array node with two elements --------------------
        JsonNode element0 = mock(JsonNode.class);
        JsonNode element1 = mock(JsonNode.class);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(element0);
        arrayNode.add(element1);
        // first tuple schema succeeds
        when(tupleSchemas.get(0).validate(any(), eq(element0), any(), anyString())).thenReturn(Collections.emptySet());
        // second tuple schema returns a single ValidationMessage
        ValidationMessage errorMsg = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = Collections.singleton(errorMsg);
        when(tupleSchemas.get(1).validate(any(), eq(element1), any(), anyString())).thenReturn(errorSet);
        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "");
        // the result should contain the error from the second tuple schema
        assertEquals(1, result.size(), "Exactly one validation error expected");
        assertTrue(result.contains(errorMsg), "The returned set should contain the mocked error message");
        // only the first item (which succeeded) should be added to evaluated items
        assertEquals(Set.of("/0"), new HashSet<>(evaluatedItems), "Only the first item should be recorded as evaluated");
    }

    /**
     * Empty tupleSchema list – even an array should produce no errors and no evaluated items.
     */
    @Test
    void testValidateEmptyTupleSchema() throws Exception {
        // Build a validator whose constructor receives an empty schema array
        // no elements
        ArrayNode emptySchemaNode = JsonNodeFactory.instance.arrayNode();
        PrefixItemsValidator emptyValidator = new PrefixItemsValidator("path", emptySchemaNode, parentSchema, validationContext);
        // Prepare a simple array node
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,2]");
        Set<ValidationMessage> result = emptyValidator.validate(executionContext, arrayNode, arrayNode, "/items");
        assertTrue(result.isEmpty(), "No errors expected when tupleSchema is empty");
        assertTrue(evaluatedItems.isEmpty(), "No evaluated items should be recorded");
    }

    /**
     * Array length < tupleSchema size, all items validate successfully.
     */
    @Test
    void testArraySizeLessThanTupleSchema_AllValid() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        // tupleSchema size = 3
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        // array with only 2 elements
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,\"a\"]");
        // stub each tuple schema to succeed
        when(schemas.get(0).validate(any(), eq(arrayNode.get(0)), any(), anyString())).thenReturn(Collections.emptySet());
        when(schemas.get(1).validate(any(), eq(arrayNode.get(1)), any(), anyString())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/data");
        assertTrue(result.isEmpty(), "No validation errors expected");
        assertEquals(Set.of("/data/0", "/data/1"), new HashSet<>(evaluatedItems), "Both array items should be recorded as evaluated");
    }

    /**
     * Array length < tupleSchema size, second schema returns an error.
     */
    @Test
    void testArraySizeLessThanTupleSchema_WithErrors() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[5,\"bad\"]");
        // first schema succeeds
        when(schemas.get(0).validate(any(), eq(arrayNode.get(0)), any(), anyString())).thenReturn(Collections.emptySet());
        // second schema fails
        ValidationMessage err = mock(ValidationMessage.class);
        when(schemas.get(1).validate(any(), eq(arrayNode.get(1)), any(), anyString())).thenReturn(Set.of(err));
        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/payload");
        assertEquals(1, result.size(), "One validation error expected");
        assertTrue(result.contains(err), "Error should be propagated");
        assertEquals(Set.of("/payload/0"), new HashSet<>(evaluatedItems), "Only the successful first item should be recorded");
    }

    /**
     * Array length > tupleSchema size, all validated items succeed.
     */
    @Test
    void testArraySizeGreaterThanTupleSchema_AllValid() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        // tupleSchema size = 3
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        // array with 4 elements
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[10,\"x\",true,null]");
        // stub first three schemas to succeed
        for (int i = 0; i < 3; i++) {
            when(schemas.get(i).validate(any(), eq(arrayNode.get(i)), any(), anyString())).thenReturn(Collections.emptySet());
        }
        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/array");
        assertTrue(result.isEmpty(), "No errors expected");
        assertEquals(Set.of("/array/0", "/array/1", "/array/2"), new HashSet<>(evaluatedItems), "Only the first three items (matching tupleSchema size) should be recorded");
    }

    /**
     * Array length > tupleSchema size, first schema fails, others succeed.
     */
    @Test
    void testArraySizeGreaterThanTupleSchema_WithErrorInFirstSchema() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[\"invalid\",2,3]");
        // first schema fails
        ValidationMessage err = mock(ValidationMessage.class);
        when(schemas.get(0).validate(any(), eq(arrayNode.get(0)), any(), anyString())).thenReturn(Set.of(err));
        // remaining schemas succeed
        for (int i = 1; i < 3; i++) {
            when(schemas.get(i).validate(any(), eq(arrayNode.get(i)), any(), anyString())).thenReturn(Collections.emptySet());
        }
        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/list");
        assertEquals(1, result.size(), "Exactly one error expected");
        assertTrue(result.contains(err), "Error from first schema should be present");
        // evaluated items should contain only the successful second and third items
        assertEquals(Set.of("/list/1", "/list/2"), new HashSet<>(evaluatedItems), "Successful items after the failing one should still be recorded");
    }

    /**
     * Verify that the generated JSON‑Pointer path matches the expected pattern.
     */
    @Test
    void testAtPathConstructionVerification() throws Exception {
        // Re‑use the default validator (2 tuple schemas)
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,2]");
        when(tupleSchemas.get(0).validate(any(), eq(arrayNode.get(0)), any(), anyString())).thenReturn(Collections.emptySet());
        when(tupleSchemas.get(1).validate(any(), eq(arrayNode.get(1)), any(), anyString())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "/parent/child");
        assertTrue(result.isEmpty(), "No errors expected");
        assertEquals(Set.of("/parent/child/0", "/parent/child/1"), new HashSet<>(evaluatedItems), "Paths must be built using the supplied base 'at' value");
    }

    /**
     * A null JsonNode is treated as non‑array – no exception, empty error set.
     */
    @Test
    void testNullNodeHandledGracefully() {
        Set<ValidationMessage> result = validator.validate(executionContext, null, null, "/");
        assertTrue(result.isEmpty(), "Null node should yield empty validation result");
        assertTrue(evaluatedItems.isEmpty(), "No evaluated items should be recorded for null node");
    }

    /**
     * Passing a null ExecutionContext should cause a NullPointerException.
     */
    @Test
    void testNullExecutionContext() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(1));
        assertThrows(NullPointerException.class, () -> validator.validate(null, arrayNode, arrayNode, "/"), "Null ExecutionContext must trigger NPE");
    }

    /**
     * ExecutionContext returning a null CollectorContext leads to NPE.
     */
    @Test
    void testNullCollectorContext() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getCollectorContext()).thenReturn(null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.textNode("x"));
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, arrayNode, arrayNode, "/"), "Null CollectorContext must trigger NPE");
    }

    /**
     * CollectorContext returning null evaluatedItems collection triggers NPE.
     */
    @Test
    void testNullEvaluatedItems() {
        CollectorContext cc = mock(CollectorContext.class);
        when(cc.getEvaluatedItems()).thenReturn(null);
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getCollectorContext()).thenReturn(cc);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.booleanNode(true));
        // The validator will attempt to add to a null collection
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, arrayNode, arrayNode, "/array"), "Null evaluated items collection must trigger NPE");
    }

    /**
     * Internal tupleSchema set to null should cause NPE when accessed.
     */
    @Test
    void testNullTupleSchema() throws Exception {
        // Use reflection to set the private field to null
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        f.set(validator, null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(5));
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, arrayNode, arrayNode, "/field"), "Null tupleSchema field must cause NPE");
    }

    /**
     * tupleSchema contains a null element – invoking validate on it throws NPE.
     */
    @Test
    void testNullTupleSchemaElement() throws Exception {
        // Replace the first element with null via reflection
        List<JsonSchema> list = validator.getTupleSchema();
        // now list[0] == null
        list.set(0, null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(42));
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, arrayNode, arrayNode, "/value"), "Null element inside tupleSchema must cause NPE");
    }

    /**
     * A tuple schema throws an unchecked exception – the validator propagates it.
     */
    @Test
    void testTupleSchemaValidateThrowsException() {
        // make first tuple schema throw IllegalStateException
        when(tupleSchemas.get(0).validate(any(), any(), any(), anyString())).thenThrow(new IllegalStateException("boom"));
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.textNode("trigger"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(executionContext, arrayNode, arrayNode, "/prop"), "Exception from tuple schema should be propagated");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Passing a null 'at' argument leads to NPE inside atPath().
     */
    @Test
    void testNullAtParameter() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.booleanNode(true));
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, arrayNode, arrayNode, null), "Null 'at' parameter must cause NPE");
    }

    @ParameterizedTest
    @MethodSource("lessThanTupleSchemaProvider")
    void parameterizedArraySizeLessThanTupleSchema(String jsonArray, String at, Set<String> expectedEvaluated, Set<String> errorMarker) throws Exception {
        // Build validator with 3 tuple schemas
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree(jsonArray);
        // Stub schemas: first two entries correspond to array elements
        when(schemas.get(0).validate(any(), eq(arrayNode.get(0)), any(), anyString())).thenReturn(errorMarker.contains("error") ? Set.of(mock(ValidationMessage.class)) : Collections.emptySet());
        if (arrayNode.size() > 1) {
            when(schemas.get(1).validate(any(), eq(arrayNode.get(1)), any(), anyString())).thenReturn(Collections.emptySet());
        }
        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, at);
        if (errorMarker.isEmpty()) {
            assertTrue(result.isEmpty(), "No errors expected");
        } else {
            assertEquals(1, result.size(), "One error expected");
        }
        assertEquals(expectedEvaluated, new HashSet<>(evaluatedItems), "Evaluated items mismatch");
    }

    @SuppressWarnings("unchecked")
    static <T> T mock(Class<T> clazz) {
        InvocationHandler handler = new MockHandler();
        if (clazz.isInterface()) {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
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
}
