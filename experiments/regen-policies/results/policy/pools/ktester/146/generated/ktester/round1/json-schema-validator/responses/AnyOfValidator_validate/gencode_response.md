```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link AnyOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * <p>The test suite covers the behaviour described in the JSON test‑case document, including
 * short‑circuiting, walk flag handling, null handling and various error conditions.</p>
 *
 * <p>All external collaborators are mocked so the tests focus solely on the logic inside
 * {@code AnyOfValidator}.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /** Shared mapper for building JSON nodes. */
    private static ObjectMapper mapper;

    /** The validator under test – created per‑test to allow different configurations. */
    private AnyOfValidator validator;

    /** Mocked collaborators. */
    @Mock private ValidationContext validationContext;
    @Mock private SchemaLocation schemaLocation;
    @Mock private JsonNodePath evaluationPath;
    @Mock private JsonNodePath instanceLocation;

    /** Two child schemas used in most tests – their behaviour is re‑configured per test. */
    @Mock private JsonSchema subschemaA;
    @Mock private JsonSchema subschemaB;

    @BeforeAll
    static void beforeAll() {
        mapper = new ObjectMapper();
    }

    /**
     * Helper subclass that allows us to control the result of {@code canShortCircuit()}.
     * The protected {@code validate(...,boolean)} method is also exposed via a public wrapper
     * for the walk‑flag tests.
     */
    private static class TestableAnyOfValidator extends AnyOfValidator {
        private final boolean shortCircuit;

        TestableAnyOfValidator(boolean shortCircuit,
                               SchemaLocation schemaLocation,
                               JsonNodePath evaluationPath,
                               JsonNode schemaNode,
                               ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, null, validationContext);
            this.shortCircuit = shortCircuit;
        }

        @Override
        protected boolean canShortCircuit() {
            return shortCircuit;
        }

        @Override
        protected boolean canShortCircuit(ExecutionContext executionContext) {
            return shortCircuit;
        }

        /** Expose the protected validate method that accepts the walk flag. */
        Set<ValidationMessage> validateWithWalk(ExecutionContext ec,
                                                  JsonNode node,
                                                  JsonNode rootNode,
                                                  JsonNodePath path,
                                                  boolean walk) {
            return super.validate(ec, node, rootNode, path, walk);
        }
    }

    /**
     * Common set‑up for the majority of tests – builds a two‑element anyOf schema
     * and configures the {@link ValidationContext} mock to return {@code subschemaA}
     * and {@code subschemaB} for the two elements.
     */
    @BeforeEach
    void beforeEach() throws Exception {
        // anyOf schema with two placeholder objects
        String anyOf = "[{\"type\":\"string\"}, {\"type\":\"integer\"}]";
        JsonNode schemaNode = mapper.readTree(anyOf);

        // The validator will request child schemas via ValidationContext#newSchema.
        when(validationContext.newSchema(any(SchemaLocation.class),
                                          any(JsonNodePath.class),
                                          any(JsonNode.class),
                                          any(JsonSchema.class)))
                .thenReturn(subschemaA)   // first element
                .thenReturn(subschemaB);  // second element

        // By default both subschemas succeed – individual tests will re‑configure.
        when(subschemaA.validate(any(ExecutionContext.class), any(JsonNode.class),
                                 any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());
        when(subschemaB.validate(any(ExecutionContext.class), any(JsonNode.class),
                                 any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());

        // Default validator – short‑circuit disabled (the base class returns false).
        validator = new TestableAnyOfValidator(false, schemaLocation, evaluationPath,
                schemaNode, validationContext);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods for building JSON nodes used in the parameterized tests
     * --------------------------------------------------------------------- */

    private static JsonNode node(String json) throws Exception {
        return mapper.readTree(json);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for the “anyOf validation succeeds” groups
     * --------------------------------------------------------------------- */

    private static Stream<Arguments> successWithoutShortCircuitProvider() throws Exception {
        return Stream.of(
                // first subschema succeeds (string)
                Arguments.of(node("{\"type\":\"string\",\"value\":\"hello\"}"),
                             Collections.emptySet(),
                             Collections.singleton(mock(ValidationMessage.class))),
                // second subschema succeeds (integer)
                Arguments.of(node("{\"type\":\"integer\",\"value\":5}"),
                             Collections.singleton(mock(ValidationMessage.class)),
                             Collections.emptySet())
        );
    }

    @ParameterizedTest(name = "[{index}] anyOf succeeds without short‑circuit – node={0}")
    @MethodSource("successWithoutShortCircuitProvider")
    void validate_Succeeds_WhenAtLeastOneSubschemaValidates_WithoutShortCircuit(JsonNode node,
                                                                              Set<ValidationMessage> firstResult,
                                                                              Set<ValidationMessage> secondResult) throws Exception {
        ExecutionContext ctx = new ExecutionContext();

        // configure the two subschemas according to the parameters
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(firstResult);
        when(subschemaB.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(secondResult);

        // invoke the public validate method (walk flag = false)
        Set<ValidationMessage> result = validator.validate(ctx, node, node, instanceLocation);

        // because at least one subschema succeeded, the final set must be empty
        assertTrue(result.isEmpty(), "AnyOf should succeed when any subschema validates");

        // verify that BOTH subschemas were evaluated (short‑circuit is OFF)
        verify(subschemaA, times(1)).validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class));
        verify(subschemaB, times(1)).validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class));
    }

    private static Stream<Arguments> successWithShortCircuitProvider() throws Exception {
        return Stream.of(
                // first subschema succeeds – short‑circuit should stop after it
                Arguments.of(node("{\"type\":\"string\",\"value\":\"short\"}"),
                             Collections.emptySet(),
                             Collections.singleton(mock(ValidationMessage.class))),
                // second subschema succeeds – validator must evaluate first (fail) then stop after second
                Arguments.of(node("{\"type\":\"integer\",\"value\":42}"),
                             Collections.singleton(mock(ValidationMessage.class)),
                             Collections.emptySet())
        );
    }

    @ParameterizedTest(name = "[{index}] anyOf succeeds with short‑circuit – node={0}")
    @MethodSource("successWithShortCircuitProvider")
    void validate_Succeeds_WithShortCircuit(JsonNode node,
                                             Set<ValidationMessage> firstResult,
                                             Set<ValidationMessage> secondResult) throws Exception {
        // Build a validator that *does* short‑circuit.
        String anyOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode schemaNode = mapper.readTree(anyOf);
        AnyOfValidator shortCircuitValidator = new TestableAnyOfValidator(true,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();

        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(firstResult);
        when(subschemaB.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(secondResult);

        Set<ValidationMessage> result = shortCircuitValidator.validate(ctx, node, node, instanceLocation);
        assertTrue(result.isEmpty(), "AnyOf should succeed when a subschema validates (short‑circuit on)");

        // Verify short‑circuit behaviour:
        //   * If the first subschema succeeded, the second must NOT be invoked.
        //   * If the first failed, the second must be invoked exactly once.
        if (firstResult.isEmpty()) {
            verify(subschemaA, times(1)).validate(eq(ctx), any(), any(), any());
            verify(subschemaB, never()).validate(any(), any(), any(), any());
        } else {
            verify(subschemaA, times(1)).validate(eq(ctx), any(), any(), any());
            verify(subschemaB, times(1)).validate(eq(ctx), any(), any(), any());
        }
    }

    /* --------------------------------------------------------------------- *
     *  Tests for failure when all subschemas reject the instance
     * --------------------------------------------------------------------- */

    @Test
    void validate_Fails_WhenAllSubschemasReject() {
        ExecutionContext ctx = new ExecutionContext();

        ValidationMessage msgA = mock(ValidationMessage.class);
        ValidationMessage msgB = mock(ValidationMessage.class);
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.singleton(msgA));
        when(subschemaB.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.singleton(msgB));

        JsonNode node = mapper.createObjectNode().put("type", "boolean").put("value", true);
        Set<ValidationMessage> result = validator.validate(ctx, node, node, instanceLocation);

        assertFalse(result.isEmpty(), "AnyOf must return errors when every subschema fails");
        assertEquals(2, result.size(), "One message per failing subschema is expected");
    }

    /* --------------------------------------------------------------------- *
     *  Null / missing instance node handling
     * --------------------------------------------------------------------- */

    @Test
    void validate_ReturnsEmptySet_WhenInstanceNodeIsNull() {
        ExecutionContext ctx = new ExecutionContext();
        // According to the spec anyOf is ignored for absent nodes.
        Set<ValidationMessage> result = validator.validate(ctx, null, null, instanceLocation);
        assertTrue(result.isEmpty(), "Validator should treat a null node as successful (anyOf ignored)");
    }

    /* --------------------------------------------------------------------- *
     *  Walk flag true – deep validation
     * --------------------------------------------------------------------- */

    @Test
    void validate_WithWalkFlagTrue_PerformsDeepValidation() throws Exception {
        // Build a validator that contains two subschemas (both succeed for the deep node)
        String anyOf = "[{\"type\":\"object\"},{\"type\":\"object\"}]";
        JsonNode schemaNode = mapper.readTree(anyOf);
        TestableAnyOfValidator walkValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();

        JsonNode deepNode = mapper.readTree("{\"type\":\"object\",\"inner\":{\"type\":\"string\",\"value\":\"deep\"}}");
        JsonNode rootNode = deepNode; // same for this test

        // Both subschemas succeed – empty sets
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());
        when(subschemaB.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());

        // Invoke the protected validate method via the public wrapper.
        Set<ValidationMessage> result = walkValidator.validateWithWalk(ctx, deepNode, rootNode,
                instanceLocation, true);

        assertTrue(result.isEmpty(), "Deep validation with walk=true should succeed when subschemas succeed");
    }

    /* --------------------------------------------------------------------- *
     *  Empty anyOf array – must always fail
     * --------------------------------------------------------------------- */

    @Test
    void validate_EmptyAnyOfArray_AlwaysFails() throws Exception {
        JsonNode emptyArray = mapper.readTree("[]");
        AnyOfValidator emptyValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, emptyArray, validationContext);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mapper.readTree("{\"type\":\"string\",\"value\":\"anything\"}");

        Set<ValidationMessage> result = emptyValidator.validate(ctx, node, node, instanceLocation);
        assertFalse(result.isEmpty(), "AnyOf with an empty array must produce a validation error");
        // The exact message is implementation‑specific; we only assert that at least one exists.
        assertEquals(1, result.size(), "Exactly one error message is expected for empty anyOf");
    }

    /* --------------------------------------------------------------------- *
     *  Single‑element anyOf – passing instance
     * --------------------------------------------------------------------- */

    @Test
    void validate_SingleSubschema_PassingInstance() throws Exception {
        String single = "[{\"type\":\"number\",\"multipleOf\":0.01}]";
        JsonNode schemaNode = mapper.readTree(single);
        AnyOfValidator singleValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();

        // Configure the sole subschema (subschemaA) to succeed.
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());

        JsonNode node = mapper.readTree("{\"type\":\"number\",\"value\":3.14}");
        Set<ValidationMessage> result = singleValidator.validate(ctx, node, node, instanceLocation);
        assertTrue(result.isEmpty(), "Single subschema that validates should make anyOf succeed");
    }

    /* --------------------------------------------------------------------- *
     *  Single‑element anyOf – failing instance
     * --------------------------------------------------------------------- */

    @Test
    void validate_SingleSubschema_FailingInstance() throws Exception {
        String single = "[{\"type\":\"number\",\"minimum\":0}]";
        JsonNode schemaNode = mapper.readTree(single);
        AnyOfValidator singleValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();

        ValidationMessage msg = mock(ValidationMessage.class);
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.singleton(msg));

        JsonNode node = mapper.readTree("{\"type\":\"number\",\"value\":-5}");
        Set<ValidationMessage> result = singleValidator.validate(ctx, node, node, instanceLocation);
        assertFalse(result.isEmpty(), "Failure of the sole subschema must be propagated");
        assertEquals(1, result.size(), "Exactly the messages from the single subschema are expected");
    }

    /* --------------------------------------------------------------------- *
     *  anyOf with mixed array/object subschemas and short‑circuit enabled
     * --------------------------------------------------------------------- */

    @Test
    void validate_MixedSubschemas_ArrayInstance_ShortCircuitEnabled() throws Exception {
        // anyOf contains: (1) array schema, (2) string enum schema
        String mixed = "[{\"type\":\"array\",\"minItems\":2},{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}]";
        JsonNode schemaNode = mapper.readTree(mixed);
        AnyOfValidator mixedValidator = new TestableAnyOfValidator(true,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();

        // First subschema (array) succeeds → empty set
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());
        // Second subschema (string) would fail, but should never be called because of short‑circuit.
        when(subschemaB.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        JsonNode arrayNode = mapper.readTree("[\"a\",\"b\",\"c\"]");
        Set<ValidationMessage> result = mixedValidator.validate(ctx, arrayNode, arrayNode, instanceLocation);
        assertTrue(result.isEmpty(), "Array instance should satisfy the first subschema and short‑circuit");

        // Verify that the second subschema was NOT evaluated.
        verify(subschemaB, never()).validate(any(), any(), any(), any());
    }

    /* --------------------------------------------------------------------- *
     *  Validation where rootNode differs from node (child instance)
     * --------------------------------------------------------------------- */

    @Test
    void validate_RootNodeDiffersFromNode_UsesSuppliedNode() throws Exception {
        // anyOf with two simple type schemas
        String anyOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode schemaNode = mapper.readTree(anyOf);
        AnyOfValidator childValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();

        // First subschema fails, second succeeds.
        when(subschemaA.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));
        when(subschemaB.validate(eq(ctx), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class)))
                .thenReturn(Collections.emptySet());

        JsonNode childNode = mapper.readTree("{\"type\":\"string\",\"value\":\"inner\"}");
        JsonNode rootNode = mapper.readTree("{\"outer\": {\"type\":\"string\",\"value\":\"inner\"}}");

        Set<ValidationMessage> result = childValidator.validate(ctx, childNode, rootNode,
                new JsonNodePath("$.outer"));
        assertTrue(result.isEmpty(), "Validator must use the supplied node for validation even when root differs");
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – null ExecutionContext
     * --------------------------------------------------------------------- */

    @Test
    void validate_Throws_NullPointerException_WhenExecutionContextIsNull() {
        JsonNode node = mapper.createObjectNode();
        assertThrows(NullPointerException.class,
                () -> validator.validate(null, node, node, instanceLocation),
                "A null ExecutionContext should cause NPE");
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – null instanceLocation
     * --------------------------------------------------------------------- */

    @Test
    void validate_Throws_IllegalArgumentException_WhenInstanceLocationIsNull() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mapper.createObjectNode();
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(ctx, node, node, null),
                "A null JsonNodePath should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – anyOf array contains null element
     * --------------------------------------------------------------------- */

    @Test
    void validate_Throws_NullPointerException_WhenAnyOfContainsNullElement() throws Exception {
        // anyOf array with a null element
        JsonNode malformed = mapper.readTree("[null]");
        AnyOfValidator malformedValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, malformed, validationContext);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mapper.createObjectNode();

        // The ValidationContext will be asked for a schema for the null element;
        // we make it return null to trigger the NPE inside the validator.
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> malformedValidator.validate(ctx, node, node, instanceLocation),
                "Null element inside anyOf array should result in NPE");
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – anyOf array contains non‑schema primitive
     * --------------------------------------------------------------------- */

    @Test
    void validate_Throws_ClassCastException_WhenAnyOfContainsPrimitive() throws Exception {
        // anyOf array with a numeric primitive instead of an object schema
        JsonNode primitiveArray = mapper.readTree("[1]");
        // Construction itself may already fail; we attempt validation to capture the exception.
        AnyOfValidator primitiveValidator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, primitiveArray, validationContext);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mapper.createObjectNode();

        // The validator will try to treat the primitive as a JsonSchema – we simulate that by
        // making the ValidationContext return a mock that will later cause a ClassCastException.
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenThrow(ClassCastException.class);

        assertThrows(ClassCastException.class,
                () -> primitiveValidator.validate(ctx, node, node, instanceLocation),
                "Non‑schema element in anyOf should cause ClassCastException");
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – rootNode null while node non‑null
     * --------------------------------------------------------------------- */

    @Test
    void validate_Throws_NullPointerException_WhenRootNodeIsNullAndNodeIsPresent() throws Exception {
        String anyOf = "[{\"type\":\"array\"}]";
        JsonNode schemaNode = mapper.readTree(anyOf);
        AnyOfValidator validator = new TestableAnyOfValidator(false,
                schemaLocation, evaluationPath, schemaNode, validationContext);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mapper.readTree("[1,2,3]");

        assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, node, null, instanceLocation),
                "Null rootNode with a non‑null instance should cause NPE");
    }
}
```