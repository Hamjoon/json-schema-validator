package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnyOfValidator walk() behaviour")
class AnyOfValidator_walk_Test {

    /**
     * Mocks used in every test **
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ExecutionContext executionContext;

    @Mock
    JsonNode node;

    @Mock
    JsonNode rootNode;

    /**
     * The validator under test **
     */
    AnyOfValidator validator;

    /**
     * Helper schemas that will be returned from ValidationContext#newSchema **
     */
    @Mock
    JsonSchema subschema1;

    @Mock
    JsonSchema subschema2;

    /**
     * A simple schema node representing an array with two empty object schemas **
     */
    static final ArrayNode ANY_OF_SCHEMA_NODE = JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode()).add(JsonNodeFactory.instance.objectNode());

    /**
     * A reusable empty set of validation messages used in several tests.
     */
    private static final Set<ValidationMessage> EMPTY_MESSAGES = Collections.emptySet();

    /**
     * A simple validator that overrides {@code validate} so we can control its result.
     */
    private static class TestAnyOfValidator extends AnyOfValidator {

        private final Set<ValidationMessage> stubResult;

        private boolean validateCalled = false;

        TestAnyOfValidator(Set<ValidationMessage> stubResult) {
            // an empty array node – the constructor will not try to create subschemas
            super("anyOfPath", JsonNodeFactory.instance.arrayNode(), null, null);
            this.stubResult = stubResult;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            validateCalled = true;
            return stubResult;
        }

        boolean wasValidateCalled() {
            return validateCalled;
        }
    }

    /**
     * InvocationHandler that records calls to {@code walk}.
     */
    private static class WalkRecorder implements InvocationHandler {

        boolean walkCalled = false;

        boolean flagReceived = false;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("walk".equals(method.getName())) {
                walkCalled = true;
                // signature: walk(ExecutionContext, JsonNode, JsonNode, String, boolean)
                flagReceived = (boolean) args[4];
                return Collections.emptySet();
            }
            // For all other methods return a sensible default (null / empty)
            if (method.getReturnType().isPrimitive()) {
                if (method.getReturnType() == boolean.class) {
                    return false;
                } else if (method.getReturnType() == int.class || method.getReturnType() == long.class || method.getReturnType() == short.class || method.getReturnType() == byte.class) {
                    return 0;
                } else if (method.getReturnType() == char.class) {
                    return '\0';
                } else if (method.getReturnType() == float.class || method.getReturnType() == double.class) {
                    return 0.0;
                }
            }
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the ValidationContext mock to supply two subschemas when the AnyOfValidator is built
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subschema1, subschema2);
        // Build the validator instance (real instance, not a mock)
        validator = new AnyOfValidator("anyOfPath", ANY_OF_SCHEMA_NODE, parentSchema, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference
        reset(validationContext, parentSchema, executionContext, node, rootNode, subschema1, subschema2);
    }

    /**
     * Helper to create a {@link JsonSchema} proxy that records walk invocations.
     */
    private static JsonSchema createWalkRecorderProxy(WalkRecorder recorder) {
        return (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(), new Class[] { JsonSchema.class }, recorder);
    }

    /**
     * Utility to replace the private {@code schemas} field of an {@link AnyOfValidator}.
     */
    private static void setSchemasField(AnyOfValidator validator, List<JsonSchema> schemas) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, schemas);
    }

    @Test
    void walk_ShouldValidateSchema_WhenFlagTrue() {
        // arrange
        Set<ValidationMessage> expected = new LinkedHashSet<>();
        expected.add(ValidationMessage.builder().message("anyOf validation failed").path("$").type("anyOf").build());
        TestAnyOfValidator validator = new TestAnyOfValidator(expected);
        // act
        Set<ValidationMessage> actual = validator.walk(null, null, null, "$", true);
        // assert
        assertSame(expected, actual, "walk(true) must return exactly what validate() returns");
        assertTrue(validator.wasValidateCalled(), "validate() should have been invoked");
    }

    @Test
    void walk_ShouldDelegateToSubschemas_WhenFlagFalse() {
        // arrange – create two walk‑recording proxies
        WalkRecorder rec1 = new WalkRecorder();
        WalkRecorder rec2 = new WalkRecorder();
        JsonSchema sub1 = createWalkRecorderProxy(rec1);
        JsonSchema sub2 = createWalkRecorderProxy(rec2);
        // Build a validator with an empty schema node (no subschemas created by the constructor)
        AnyOfValidator validator = new AnyOfValidator("anyOfPath", JsonNodeFactory.instance.arrayNode(), null, null);
        // inject our two stub schemas
        setSchemasField(validator, Arrays.asList(sub1, sub2));
        // act
        Set<ValidationMessage> result = validator.walk(null, null, null, "$", false);
        // assert – each stub's walk should have been called with shouldValidateSchema = false
        assertTrue(rec1.walkCalled, "first subschema walk should be invoked");
        assertFalse(rec1.flagReceived, "first subschema should receive shouldValidateSchema = false");
        assertTrue(rec2.walkCalled, "second subschema walk should be invoked");
        assertFalse(rec2.flagReceived, "second subschema should receive shouldValidateSchema = false");
        // the validator itself returns an empty set when delegating
        assertEquals(EMPTY_MESSAGES, result, "When delegating, walk should return an empty set");
    }

    @Test
    void walk_WhenShouldValidateSchemaFalse_NoSchemas_ReturnsEmptySet() {
        // arrange – empty array node
        ArrayNode emptyArray = JsonNodeFactory.instance.arrayNode();
        AnyOfValidator validator = new AnyOfValidator("anyOfPath", emptyArray, null, null);
        // act
        Set<ValidationMessage> result = validator.walk(null, null, null, "/empty", false);
        // assert
        assertTrue(result.isEmpty(), "Empty anyOf should result in an empty validation set");
    }

    @Test
    void walk_WhenShouldValidateSchemaFalse_SchemasListIsNull_ReturnsEmptySet() throws Exception {
        // arrange
        AnyOfValidator validator = new AnyOfValidator("anyOfPath", JsonNodeFactory.instance.arrayNode(), null, null);
        // corrupt internal state
        setSchemasField(validator, null);
        // act
        Set<ValidationMessage> result = validator.walk(null, null, null, "/nullSchemaList", false);
        // assert
        assertTrue(result.isEmpty(), "Null internal schemas list should be treated as empty and return an empty set");
    }

    @Test
    void walk_WhenShouldValidateSchemaFalse_SchemasListContainsNull_ThrowsNPE() throws Exception {
        // arrange
        AnyOfValidator validator = new AnyOfValidator("anyOfPath", JsonNodeFactory.instance.arrayNode(), null, null);
        List<JsonSchema> listWithNull = new ArrayList<>(Collections.singletonList(null));
        setSchemasField(validator, listWithNull);
        // act & assert
        assertThrows(NullPointerException.class, () -> validator.walk(null, null, null, "/nullElement", false), "A null element in the schemas list should cause a NullPointerException");
    }
}
