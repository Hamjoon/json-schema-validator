```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>delegation to {@code validate(...)} when {@code shouldValidateSchema} is {@code true}
 *       (both non‑empty and empty validation results);</li>
 *   <li>behaviour when {@code shouldValidateSchema} is {@code false} – walking child schemas,
 *       handling an empty {@code schemas} list, and propagating runtime exceptions;</li>
 *   <li>exception propagation for {@code null} arguments that are passed through to {@code validate(...)}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /** shared test utilities */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** objects created per test */
    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private JsonNode schemaNode;          // an array node that will create child schemas
    private OneOfValidator validator;    // instance under test
    private ExecutionContext execContext;
    private JsonNode dataNode;           // node to be validated
    private JsonNode rootNode;           // root of the document
    private String at;                   // JSON pointer location

    @BeforeEach
    void setupBeforeEach() {
        // ----- mocks & stubs -------------------------------------------------
        validationContext = mock(ValidationContext.class);
        parentSchema = mock(JsonSchema.class);
        execContext = new ExecutionContext();               // real, lightweight
        dataNode = MAPPER.createObjectNode();               // simple empty object
        rootNode = MAPPER.createObjectNode();               // simple empty object
        at = "#";

        // ----- build a schemaNode that contains two child schemas ------------
        ArrayNode arrayNode = MAPPER.createArrayNode();
        // child 0
        ObjectNode child0 = MAPPER.createObjectNode();
        child0.put("type", "string");
        arrayNode.add(child0);
        // child 1
        ObjectNode child1 = MAPPER.createObjectNode();
        child1.put("type", "integer");
        arrayNode.add(child1);
        schemaNode = arrayNode;

        // ----- mock the creation of child JsonSchema objects ------------------
        JsonSchema childSchema0 = mock(JsonSchema.class);
        JsonSchema childSchema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), eq(child0), eq(parentSchema))).thenReturn(childSchema0);
        when(validationContext.newSchema(anyString(), eq(child1), eq(parentSchema))).thenReturn(childSchema1);

        // ----- construct the OneOfValidator (populates its internal list) ---
        validator = new OneOfValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == true the validator must delegate to
     *  its own validate(...) method and return whatever that method
     *  supplies.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaTrue_delegatesToValidate() {
        // arrange – stub validate to return a known set
        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = new HashSet<>();
        expected.add(mockMessage);

        OneOfValidator spyValidator = spy(validator);
        doReturn(expected).when(spyValidator).validate(any(), any(), any(), anyString());

        // act
        Set<ValidationMessage> actual = spyValidator.walk(execContext, dataNode, rootNode, at, true);

        // assert – the set returned by walk must be exactly the set from validate
        assertSame(expected, actual, "walk should return the set produced by validate");
        // also verify that validate was indeed called once
        verify(spyValidator, times(1)).validate(execContext, dataNode, rootNode, at);
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == true and the underlying validate()
     *  returns an empty set, walk() must also return an empty set.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaTrue_emptyResult() {
        // arrange – validate returns an empty set
        OneOfValidator spyValidator = spy(validator);
        doReturn(Collections.emptySet()).when(spyValidator).validate(any(), any(), any(), anyString());

        // act
        Set<ValidationMessage> result = spyValidator.walk(execContext, dataNode, rootNode, at, true);

        // assert
        assertTrue(result.isEmpty(), "walk should return an empty set when validate returns empty");
        verify(spyValidator, times(1)).validate(execContext, dataNode, rootNode, at);
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == false and there are no child schemas,
     *  walk() must simply return an empty set without delegating to any child.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaFalse_noChildSchemas_returnsEmptySet() {
        // construct a validator with an empty array node (no child schemas)
        ArrayNode emptyArray = MAPPER.createArrayNode();
        OneOfValidator emptyValidator = new OneOfValidator("emptyPath", emptyArray, parentSchema, validationContext);

        Set<ValidationMessage> result = emptyValidator.walk(execContext, dataNode, rootNode, at, false);

        assertTrue(result.isEmpty(), "Result must be empty when there are no child schemas");
        // No child schema exists, therefore no interactions to verify.
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == false and multiple child schemas exist,
     *  each child's walk method must be invoked exactly once and the result
     *  set must stay empty (OneOfValidator does not aggregate child messages).
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaFalse_multipleChildSchemas() {
        // retrieve the child schema mocks that were created in @BeforeEach
        JsonSchema childSchema0 = validationContext.newSchema("testPath/0",
                ((ArrayNode) schemaNode).get(0), parentSchema);
        JsonSchema childSchema1 = validationContext.newSchema("testPath/1",
                ((ArrayNode) schemaNode).get(1), parentSchema);

        Set<ValidationMessage> result = validator.walk(execContext, dataNode, rootNode, at, false);

        assertTrue(result.isEmpty(), "Result set must be empty for shouldValidateSchema == false");

        // verify delegation
        verify(childSchema0, times(1)).walk(execContext, dataNode, rootNode, at, false);
        verify(childSchema1, times(1)).walk(execContext, dataNode, rootNode, at, false);

        // ensure validator's own validate method was never called
        OneOfValidator spyValidator = spy(validator);
        spyValidator.walk(execContext, dataNode, rootNode, at, false);
        verify(spyValidator, never()).validate(any(), any(), any(), anyString());
    }

    /** --------------------------------------------------------------
     *  If a child schema throws a RuntimeException during its walk call,
     *  OneOfValidator.walk must propagate the exception unchanged.
     * -------------------------------------------------------------- */
    @Test
    void walk_childSchemaThrowsRuntimeException_propagates() {
        // create a validator with a single child that throws
        ObjectNode child = MAPPER.createObjectNode();
        child.put("type", "string");
        ArrayNode singleArray = MAPPER.createArrayNode().add(child);

        JsonSchema throwingChild = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), eq(child), eq(parentSchema))).thenReturn(throwingChild);
        doThrow(new RuntimeException("child failure"))
                .when(throwingChild).walk(execContext, dataNode, rootNode, at, false);

        OneOfValidator validatorWithThrowingChild =
                new OneOfValidator("path/withThrow", singleArray, parentSchema, validationContext);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validatorWithThrowingChild.walk(execContext, dataNode, rootNode, at, false),
                "Exception from child schema should propagate");
        assertEquals("child failure", ex.getMessage());
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == true and the node argument is null,
     *  walk() forwards the call to validate() which (in this test) is stubbed
     *  to throw a NullPointerException. The exception must propagate.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaTrue_nullNode_propagatesException() {
        OneOfValidator spyValidator = spy(validator);
        doThrow(new NullPointerException("node is null"))
                .when(spyValidator).validate(eq(execContext), isNull(), any(), anyString());

        assertThrows(NullPointerException.class,
                () -> spyValidator.walk(execContext, null, rootNode, at, true),
                "NullPointerException should be propagated when node is null");
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == true and the ExecutionContext argument is null,
     *  walk() forwards the call to validate() which is stubbed to throw
     *  a NullPointerException. The exception must propagate.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaTrue_nullExecutionContext_propagatesException() {
        OneOfValidator spyValidator = spy(validator);
        doThrow(new NullPointerException("executionContext is null"))
                .when(spyValidator).validate(isNull(), any(), any(), anyString());

        assertThrows(NullPointerException.class,
                () -> spyValidator.walk(null, dataNode, rootNode, at, true),
                "NullPointerException should be propagated when ExecutionContext is null");
    }

    /** --------------------------------------------------------------
     *  Parameterized test covering the two true‑branch scenarios:
     *  (a) non‑empty validation result, (b) empty validation result.
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideValidateResults")
    void walk_shouldValidateSchemaTrue_parameterized(Set<ValidationMessage> stubResult) {
        OneOfValidator spyValidator = spy(validator);
        doReturn(stubResult).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> actual = spyValidator.walk(execContext, dataNode, rootNode, at, true);
        assertEquals(stubResult, actual, "walk must return exactly what validate() returns");
    }

    private static Stream<Set<ValidationMessage>> provideValidateResults() {
        ValidationMessage msg = mock(ValidationMessage.class);
        return Stream.of(
                Collections.singleton(msg),   // non‑empty
                Collections.emptySet()        // empty
        );
    }
}
```