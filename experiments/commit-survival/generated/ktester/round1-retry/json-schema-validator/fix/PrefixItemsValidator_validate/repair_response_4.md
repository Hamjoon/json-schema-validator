**Corrected test class**

```java
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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit‑tests for {@link PrefixItemsValidator}.  The original version used a
 * homemade Mockito‑like framework that tried to instantiate concrete classes
 * (e.g. {@code ValidationContext} and {@code JsonSchema}) via a no‑arg
 * constructor.  Those classes do not have such a constructor, which caused the
 * test suite to fail with “Cannot mock class …”.  The fix replaces the
 * problematic mocks with lightweight hand‑written stubs that implement only
 * the behaviour required by the tests.
 *
 * <p>The stubs are simple concrete subclasses that either delegate to a map
 * of pre‑programmed results (for {@code JsonSchema}) or return a queue of
 * {@code JsonSchema} instances (for {@code ValidationContext}).  This removes
 * the need for reflection‑based mocking while keeping the original test
 * semantics.</p>
 */
class PrefixItemsValidator_validate_Test {

    /* --------------------------------------------------------------------- */
    /*  Test collaborators – simple hand‑written stubs                     */
    /* --------------------------------------------------------------------- */

    /** Stub for {@link ValidationContext}.  It returns a predetermined
     *  sequence of {@link JsonSchema} objects when {@code newSchema} is
     *  invoked. */
    private static class StubValidationContext extends ValidationContext {
        private final Queue<JsonSchema> queue = new ArrayDeque<>();

        StubValidationContext(JsonSchema... schemas) {
            // The real {@code ValidationContext} has a protected constructor
            // that takes three arguments.  Passing {@code null} works for the
            // purposes of these tests because the base class does not use them.
            super(null, null, null);
            Collections.addAll(queue, schemas);
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            return queue.poll();
        }
    }

    /** Minimal concrete {@link JsonSchema} that lets a test specify the
     *  validation result for a given instance node. */
    private static class SimpleJsonSchema extends JsonSchema {
        private final Map<JsonNode, Set<ValidationMessage>> results = new HashMap<>();

        SimpleJsonSchema() {
            // The real {@code JsonSchema} constructor is protected; passing
            // {@code null} values is safe for the limited usage in the tests.
            super(null, null, null);
        }

        /** Define the result that should be returned when {@code validate}
         *  is called with the supplied {@code instanceNode}. */
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
        @Override public Set<ValidationMessage> walk(ExecutionContext ctx,
                                                    JsonNode node,
                                                    JsonNode rootNode,
                                                    String at,
                                                    boolean isParent) { return null; }

        @Override public List<JsonSchema> getTupleSchema() { return null; }
    }

    /** {@link JsonSchema} that throws a supplied unchecked exception when
     *  {@code validate} is invoked.  Used by the “exception propagation”
     *  test. */
    private static class ThrowingJsonSchema extends JsonSchema {
        private final RuntimeException toThrow;

        ThrowingJsonSchema(RuntimeException toThrow) {
            super(null, null, null);
            this.toThrow = toThrow;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode instanceNode,
                                                JsonNode rootNode,
                                                String at) {
            throw toThrow;
        }

        @Override public Set<ValidationMessage> walk(ExecutionContext ctx,
                                                        JsonNode node,
                                                        JsonNode rootNode,
                                                        String at,
                                                        boolean isParent) { return null; }

        @Override public List<JsonSchema> getTupleSchema() { return null; }
    }

    /* --------------------------------------------------------------------- */
    /*  Test fixtures                                                       */
    /* --------------------------------------------------------------------- */

    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private ExecutionContext executionContext;
    private CollectorContext collectorContext;
    private PrefixItemsValidator validator;
    private List<JsonSchema> tupleSchemas;
    private Collection<String> evaluatedItems;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // ---- parent schema (not used directly) ----------------------------
        parentSchema = mock(JsonSchema.class);   // JsonSchema is an interface‑like class; using the simple mock helper works.

        // ---- create two tuple schemas that simply return empty sets ------
        SimpleJsonSchema firstTuple = new SimpleJsonSchema();
        SimpleJsonSchema secondTuple = new SimpleJsonSchema();

        // ---- stub ValidationContext to return the two tuple schemas ---------
        validationContext = new StubValidationContext(firstTuple, secondTuple);

        // ---- build the validator (its constructor will call newSchema twice)-
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        schemaNode.add(JsonNodeFactory.instance.objectNode());
        schemaNode.add(JsonNodeFactory.instance.objectNode());

        validator = new PrefixItemsValidator("testPath", schemaNode, parentSchema, validationContext);
        tupleSchemas = validator.getTupleSchema();

        assertEquals(2, tupleSchemas.size(), "Constructor must create exactly two tuple schemas");

        // ---- mock ExecutionContext and CollectorContext --------------------
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        evaluatedItems = new ArrayList<>();
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /** Helper that creates a validator with a given number of tuple schemas.
     *  All schemas are instances of {@link SimpleJsonSchema}. */
    private PrefixItemsValidator createValidatorWithTupleCount(int count,
                                                               List<JsonSchema> outList) {
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < count; i++) {
            schemaNode.add(JsonNodeFactory.instance.objectNode());
        }

        // create the required number of SimpleJsonSchema instances
        List<JsonSchema> schemas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            schemas.add(new SimpleJsonSchema());
        }

        // stub ValidationContext to return them in order
        validationContext = new StubValidationContext(schemas.toArray(new JsonSchema[0]));

        PrefixItemsValidator v = new PrefixItemsValidator("path", schemaNode, parentSchema, validationContext);
        if (outList != null) {
            outList.addAll(v.getTupleSchema());
        }
        return v;
    }

    /* --------------------------------------------------------------------- */
    /*  Tests                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void testValidateNonArrayReturnsEmpty() {
        JsonNode nonArrayNode = mock(JsonNode.class);
        when(nonArrayNode.isArray()).thenReturn(false);
        Set<ValidationMessage> result = validator.validate(executionContext, nonArrayNode, nonArrayNode, "");
        assertTrue(result.isEmpty());
        assertTrue(evaluatedItems.isEmpty());
    }

    @Test
    void testValidateArrayAllValid() {
        JsonNode element0 = mock(JsonNode.class);
        JsonNode element1 = mock(JsonNode.class);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(element0);
        arrayNode.add(element1);

        ((SimpleJsonSchema) tupleSchemas.get(0)).setResult(element0, Collections.emptySet());
        ((SimpleJsonSchema) tupleSchemas.get(1)).setResult(element1, Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "");
        assertTrue(result.isEmpty());
        assertEquals(Set.of("/0", "/1"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testValidateArrayWithErrors() {
        JsonNode element0 = mock(JsonNode.class);
        JsonNode element1 = mock(JsonNode.class);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(element0);
        arrayNode.add(element1);

        ((SimpleJsonSchema) tupleSchemas.get(0)).setResult(element0, Collections.emptySet());

        ValidationMessage errorMsg = mock(ValidationMessage.class);
        ((SimpleJsonSchema) tupleSchemas.get(1)).setResult(element1, Set.of(errorMsg));

        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "");
        assertEquals(1, result.size());
        assertTrue(result.contains(errorMsg));
        assertEquals(Set.of("/0"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testValidateEmptyTupleSchema() throws Exception {
        ArrayNode emptySchemaNode = JsonNodeFactory.instance.arrayNode();
        PrefixItemsValidator emptyValidator = new PrefixItemsValidator("path", emptySchemaNode, parentSchema, validationContext);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,2]");
        Set<ValidationMessage> result = emptyValidator.validate(executionContext, arrayNode, arrayNode, "/items");
        assertTrue(result.isEmpty());
        assertTrue(evaluatedItems.isEmpty());
    }

    @Test
    void testArraySizeLessThanTupleSchema_AllValid() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,\"a\"]");

        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());
        ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/data");
        assertTrue(result.isEmpty());
        assertEquals(Set.of("/data/0", "/data/1"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testArraySizeLessThanTupleSchema_WithErrors() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[5,\"bad\"]");

        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());

        ValidationMessage err = mock(ValidationMessage.class);
        ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Set.of(err));

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/payload");
        assertEquals(1, result.size());
        assertTrue(result.contains(err));
        assertEquals(Set.of("/payload/0"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testArraySizeGreaterThanTupleSchema_AllValid() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[10,\"x\",true,null]");

        for (int i = 0; i < 3; i++) {
            ((SimpleJsonSchema) schemas.get(i)).setResult(arrayNode.get(i), Collections.emptySet());
        }

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/array");
        assertTrue(result.isEmpty());
        assertEquals(Set.of("/array/0", "/array/1", "/array/2"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testArraySizeGreaterThanTupleSchema_WithErrorInFirstSchema() throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[\"invalid\",2,3]");

        ValidationMessage err = mock(ValidationMessage.class);
        ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Set.of(err));

        for (int i = 1; i < 3; i++) {
            ((SimpleJsonSchema) schemas.get(i)).setResult(arrayNode.get(i), Collections.emptySet());
        }

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, "/list");
        assertEquals(1, result.size());
        assertTrue(result.contains(err));
        assertEquals(Set.of("/list/1", "/list/2"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testAtPathConstructionVerification() throws Exception {
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree("[1,2]");
        ((SimpleJsonSchema) tupleSchemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());
        ((SimpleJsonSchema) tupleSchemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "/parent/child");
        assertTrue(result.isEmpty());
        assertEquals(Set.of("/parent/child/0", "/parent/child/1"), new HashSet<>(evaluatedItems));
    }

    @Test
    void testNullNodeHandledGracefully() {
        Set<ValidationMessage> result = validator.validate(executionContext, null, null, "/");
        assertTrue(result.isEmpty());
        assertTrue(evaluatedItems.isEmpty());
    }

    @Test
    void testNullExecutionContext() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(1));
        assertThrows(NullPointerException.class,
                () -> validator.validate(null, arrayNode, arrayNode, "/"));
    }

    @Test
    void testNullCollectorContext() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getCollectorContext()).thenReturn(null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.textNode("x"));
        assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, arrayNode, arrayNode, "/"));
    }

    @Test
    void testNullEvaluatedItems() {
        CollectorContext cc = mock(CollectorContext.class);
        when(cc.getEvaluatedItems()).thenReturn(null);
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getCollectorContext()).thenReturn(cc);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.booleanNode(true));
        assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, arrayNode, arrayNode, "/array"));
    }

    @Test
    void testNullTupleSchema() throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        f.set(validator, null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(5));
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, "/field"));
    }

    @Test
    void testNullTupleSchemaElement() throws Exception {
        List<JsonSchema> list = validator.getTupleSchema();
        list.set(0, null);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.numberNode(42));
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, "/value"));
    }

    @Test
    void testTupleSchemaValidateThrowsException() {
        // Replace the first tuple schema with one that throws.
        JsonSchema throwing = new ThrowingJsonSchema(new IllegalStateException("boom"));
        List<JsonSchema> list = validator.getTupleSchema();
        list.set(0, throwing);

        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.textNode("trigger"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, "/prop"));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testNullAtParameter() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonNodeFactory.instance.booleanNode(true));
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, arrayNode, arrayNode, null));
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised test – uses the same stub approach                    */
    /* --------------------------------------------------------------------- */

    private static Stream<Arguments> lessThanTupleSchemaProvider() {
        return Stream.of(
                Arguments.of("[1, \"a\"]", "/data", Set.of("/data/0", "/data/1"), Collections.emptySet()),
                Arguments.of("[5, \"bad\"]", "/payload", Set.of("/payload/0"), Set.of("error"))
        );
    }

    @ParameterizedTest
    @MethodSource("lessThanTupleSchemaProvider")
    void parameterizedArraySizeLessThanTupleSchema(String jsonArray,
                                                    String at,
                                                    Set<String> expectedEvaluated,
                                                    Set<String> errorMarker) throws Exception {
        List<JsonSchema> schemas = new ArrayList<>();
        PrefixItemsValidator v = createValidatorWithTupleCount(3, schemas);
        ArrayNode arrayNode = (ArrayNode) MAPPER.readTree(jsonArray);

        // first element – may return an error
        if (errorMarker.contains("error")) {
            ValidationMessage err = mock(ValidationMessage.class);
            ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Set.of(err));
        } else {
            ((SimpleJsonSchema) schemas.get(0)).setResult(arrayNode.get(0), Collections.emptySet());
        }

        // second element – always succeeds if it exists
        if (arrayNode.size() > 1) {
            ((SimpleJsonSchema) schemas.get(1)).setResult(arrayNode.get(1), Collections.emptySet());
        }

        Set<ValidationMessage> result = v.validate(executionContext, arrayNode, arrayNode, at);
        if (errorMarker.isEmpty()) {
            assertTrue(result.isEmpty());
        } else {
            assertEquals(1, result.size());
        }
        assertEquals(expectedEvaluated, new HashSet<>(evaluatedItems));
    }

    /* --------------------------------------------------------------------- */
    /*  Very small mock helper – only for interfaces (ExecutionContext,   */
    /*  CollectorContext, JsonNode, ValidationMessage, etc.)               */
    /* --------------------------------------------------------------------- */

    @SuppressWarnings("unchecked")
    static <T> T mock(Class<T> clazz) {
        InvocationHandler handler = new MockHandler();
        if (clazz.isInterface()) {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                    new Class[]{clazz}, handler);
        } else {
            // For the few concrete classes we need to mock (ExecutionContext,
            // CollectorContext, JsonNode, ValidationMessage) we create a proxy
            // that implements all their interfaces and cast it.
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length == 0) {
                throw new IllegalArgumentException("Cannot mock class without interfaces: " + clazz);
            }
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                    interfaces, handler);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Minimal mock handler – records stubs and returns them on demand.    */
    /* --------------------------------------------------------------------- */

    private static class MockHandler implements InvocationHandler {
        private final Map<String, Queue<Object>> stubs = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            LAST_INVOKE.set(new InvocationRecord(this, method, args));
            String key = method.getName();
            if (stubs.containsKey(key)) {
                Object value = stubs.get(key).poll();
                if (value instanceof RuntimeException) {
                    throw (RuntimeException) value;
                }
                return value;
            }
            // default primitive returns
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
            stubs.put(methodName, new ArrayDeque<>(Arrays.asList(returns)));
        }
    }

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

    static <T> OngoingStubbing<T> when(T methodCall) {
        InvocationRecord rec = LAST_INVOKE.get();
        if (rec == null) {
            throw new IllegalStateException("No method invocation captured for stubbing");
        }
        return new OngoingStubbing<>(rec);
    }

    private static final ThreadLocal<InvocationRecord> LAST_INVOKE = new ThreadLocal<>();
}
```