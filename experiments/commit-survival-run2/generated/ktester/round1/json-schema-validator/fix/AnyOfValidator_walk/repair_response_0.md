```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>
 * The original test suite relied on Mockito for creating mocks and verifying
 * interactions. The project does not include Mockito, therefore the tests have
 * been rewritten to use simple hand‑crafted stubs and Java reflection.
 * </p>
 */
@DisplayName("AnyOfValidator walk() behaviour")
class AnyOfValidator_walk_Test {

    /** A simple validator that overrides {@code validate} so we can control its result. */
    private static class TestAnyOfValidator extends AnyOfValidator {
        private final Set<ValidationMessage> stubResult;
        private boolean validateCalled = false;

        TestAnyOfValidator(Set<ValidationMessage> stubResult) {
            // an empty array node – the constructor will not try to create subschemas
            super("anyOfPath", JsonNodeFactory.instance.arrayNode(), null, null);
            this.stubResult = stubResult;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            validateCalled = true;
            return stubResult;
        }

        boolean wasValidateCalled() {
            return validateCalled;
        }
    }

    /** InvocationHandler that records calls to {@code walk}. */
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
                } else if (method.getReturnType() == int.class
                        || method.getReturnType() == long.class
                        || method.getReturnType() == short.class
                        || method.getReturnType() == byte.class) {
                    return 0;
                } else if (method.getReturnType() == char.class) {
                    return '\0';
                } else if (method.getReturnType() == float.class
                        || method.getReturnType() == double.class) {
                    return 0.0;
                }
            }
            return null;
        }
    }

    /** Helper to create a {@link JsonSchema} proxy that records walk invocations. */
    private static JsonSchema createWalkRecorderProxy(WalkRecorder recorder) {
        return (JsonSchema) Proxy.newProxyInstance(
                JsonSchema.class.getClassLoader(),
                new Class[]{JsonSchema.class},
                recorder);
    }

    /** Utility to replace the private {@code schemas} field of an {@link AnyOfValidator}. */
    private static void setSchemasField(AnyOfValidator validator, List<JsonSchema> schemas) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, schemas);
    }

    /** A reusable empty set of validation messages used in several tests. */
    private static final Set<ValidationMessage> EMPTY_MESSAGES = Collections.emptySet();

    @Test
    @DisplayName("walk(true) delegates to validate() and returns its result")
    void walk_ShouldValidateSchema_WhenFlagTrue() {
        // arrange
        Set<ValidationMessage> expected = new LinkedHashSet<>();
        expected.add(ValidationMessage.builder()
                .message("anyOf validation failed")
                .path("$")
                .type("anyOf")
                .build());

        TestAnyOfValidator validator = new TestAnyOfValidator(expected);

        // act
        Set<ValidationMessage> actual = validator.walk(null, null, null, "$", true);

        // assert
        assertSame(expected, actual, "walk(true) must return exactly what validate() returns");
        assertTrue(validator.wasValidateCalled(), "validate() should have been invoked");
    }

    @Test
    @DisplayName("walk(false) delegates to each subschema's walk()")
    void walk_ShouldDelegateToSubschemas_WhenFlagFalse() throws Exception {
        // arrange – create two walk‑recording proxies
        WalkRecorder rec1 = new WalkRecorder();
        WalkRecorder rec2 = new WalkRecorder();
        JsonSchema sub1 = createWalkRecorderProxy(rec1);
        JsonSchema sub2 = createWalkRecorderProxy(rec2);

        // Build a validator with an empty schema node (no subschemas created by the constructor)
        AnyOfValidator validator = new AnyOfValidator("anyOfPath",
                JsonNodeFactory.instance.arrayNode(),
                null,
                null);

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
    @DisplayName("walk(false) on validator built from an empty anyOf array returns empty set")
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
    @DisplayName("walk(false) with internal schemas list set to null returns empty set")
    void walk_WhenShouldValidateSchemaFalse_SchemasListIsNull_ReturnsEmptySet() throws Exception {
        // arrange
        AnyOfValidator validator = new AnyOfValidator("anyOfPath",
                JsonNodeFactory.instance.arrayNode(),
                null,
                null);
        // corrupt internal state
        setSchemasField(validator, null);

        // act
        Set<ValidationMessage> result = validator.walk(null, null, null, "/nullSchemaList", false);

        // assert
        assertTrue(result.isEmpty(),
                "Null internal schemas list should be treated as empty and return an empty set");
    }

    @Test
    @DisplayName("walk(false) with a null element inside schemas list throws NPE")
    void walk_WhenShouldValidateSchemaFalse_SchemasListContainsNull_ThrowsNPE() throws Exception {
        // arrange
        AnyOfValidator validator = new AnyOfValidator("anyOfPath",
                JsonNodeFactory.instance.arrayNode(),
                null,
                null);
        List<JsonSchema> listWithNull = new ArrayList<>(Collections.singletonList(null));
        setSchemasField(validator, listWithNull);

        // act & assert
        assertThrows(NullPointerException.class,
                () -> validator.walk(null, null, null, "/nullElement", false),
                "A null element in the schemas list should cause a NullPointerException");
    }
}
```