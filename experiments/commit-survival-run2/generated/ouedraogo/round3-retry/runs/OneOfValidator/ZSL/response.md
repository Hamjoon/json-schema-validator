###Test START##
```java
/*****************************************************************************************
 *  OneOfValidatorTest.java
 *
 *  Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 *  The tests use Mockito to mock the complex schema validation infrastructure
 *  (ExecutionContext, CollectorContext, JsonSchema, ValidationContext, etc.).
 *
 *  Scenarios covered:
 *   • Exactly one child schema validates successfully.
 *   • No child schema validates.
 *   • More than one child schema validates (error short‑circuit).
 *   • Fail‑fast mode (exception is thrown).
 *   • walk(...) with shouldValidateSchema = true/false.
 *   • preloadJsonSchema() invokes initialise on all child schemas.
 *
 *  The test class is deliberately self‑contained – it does not depend on any
 *  external JSON files and builds the minimal JSON structures required for the
 *  validator using Jackson’s ObjectMapper.
 *
 *****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OneOfValidatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope mockScope;               // used for both enter/exit scopes
    @Mock private JsonSchema parentSchema;        // not used directly in tests

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // CollectorContext basic contract
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // Scope.mergeWith(..) is a void method – we just ignore it.
        doNothing().when(mockScope).mergeWith(any(Scope.class));
    }

    /**
     * Helper to create a OneOfValidator instance whose child schemas are the supplied mocks.
     */
    private OneOfValidator createValidator(JsonNode schemaNode, List<JsonSchema> childSchemas) {
        // validationContext.newSchema(..) will be called once per child schema.
        for (int i = 0; i < childSchemas.size(); i++) {
            when(validationContext.newSchema(anyString(), eq(schemaNode.get(i)), any(JsonSchema.class)))
                    .thenReturn(childSchemas.get(i));
        }

        // Construct the validator – the constructor will populate the internal schema list.
        return new OneOfValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    @Test
    public void testExactlyOneSchemaMatches() {
        // --------------------------------------------------------------
        // Arrange – two child schemas: first fails, second succeeds.
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode())   // child 0
                .add(mapper.createObjectNode()); // child 1

        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // schema0 returns a non‑empty error set
        Set<ValidationMessage> errors0 = Collections.singleton(
                ValidationMessage.builder().message("error0").path("$").build());
        when(schema0.validate(any(), any(), any(), anyString())).thenReturn(errors0);
        when(schema0.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(errors0);

        // schema1 validates successfully (empty error set)
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // state configuration – walk disabled, matchedNode true after successful validation
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        OneOfValidator validator = createValidator(schemaNode, Arrays.asList(schema0, schema1));

        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.readTree("{\"a\":1}"), // node (doesn't matter)
                mapper.readTree("{\"a\":1}"),
                "$");

        // --------------------------------------------------------------
        // Assert – no errors, matchedNode flag set to true
        // --------------------------------------------------------------
        assertTrue("When exactly one schema matches, result must be empty", result.isEmpty());
        verify(validatorState).setMatchedNode(true);
    }

    @Test
    public void testNoSchemaMatches() {
        // --------------------------------------------------------------
        // Arrange – both child schemas produce errors.
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());

        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        ValidationMessage vm0 = ValidationMessage.builder().message("error0").path("$").build();
        ValidationMessage vm1 = ValidationMessage.builder().message("error1").path("$").build();

        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(vm0));
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(vm1));

        // state reports that walk is disabled and that a node was matched (so the validator
        // will count the schema as “valid” only when the error set is empty – which never happens).
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = createValidator(schemaNode, Arrays.asList(schema0, schema1));

        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.readTree("{\"b\":2}"),
                mapper.readTree("{\"b\":2}"),
                "$.b");

        // --------------------------------------------------------------
        // Assert – a OneOf error plus the child errors must be present.
        // --------------------------------------------------------------
        assertEquals("Two child errors + one OneOf error expected", 3, result.size());

        // The OneOf error message contains the count of matching schemas (0)
        boolean containsCount0 = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("OneOf validation message should contain the count '0'", containsCount0);
    }

    @Test
    public void testMultipleSchemasMatch() {
        // --------------------------------------------------------------
        // Arrange – both child schemas succeed (empty error sets).
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());

        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = createValidator(schemaNode, Arrays.asList(schema0, schema1));

        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.readTree("{\"c\":3}"),
                mapper.readTree("{\"c\":3}"),
                "$.c");

        // --------------------------------------------------------------
        // Assert – OneOf error with count 2, no child errors.
        // --------------------------------------------------------------
        assertEquals("Exactly one OneOf error expected when >1 schemas match", 1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertTrue("Message should indicate 2 matching schemas", vm.getMessage().contains("2"));
    }

    @Test
    public void testFailFastThrowsException() throws Exception {
        // --------------------------------------------------------------
        // Arrange – zero matching schemas and failFast enabled.
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode());

        JsonSchema schema0 = mock(JsonSchema.class);
        ValidationMessage vm0 = ValidationMessage.builder().message("error0").path("$").build();
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(vm0));

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = createValidator(schemaNode, Collections.singletonList(schema0));

        // set the protected failFast flag via reflection
        Field ff = OneOfValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // --------------------------------------------------------------
        // Expect exception
        // --------------------------------------------------------------
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("OneOf");

        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        validator.validate(executionContext,
                mapper.readTree("{\"d\":4}"),
                mapper.readTree("{\"d\":4}"),
                "$.d");
    }

    @Test
    public void testWalkShouldValidateSchemaTrue() {
        // --------------------------------------------------------------
        // Arrange – validator with a single child schema.
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode());

        JsonSchema child = mock(JsonSchema.class);
        when(child.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = createValidator(schemaNode, Collections.singletonList(child));

        // --------------------------------------------------------------
        // Act – walk with shouldValidateSchema = true
        // --------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext,
                mapper.readTree("{\"e\":5}"),
                mapper.readTree("{\"e\":5}"),
                "$.e",
                true);

        // --------------------------------------------------------------
        // Assert – walk delegates to validate, thus result is empty.
        // --------------------------------------------------------------
        assertTrue(result.isEmpty());
        verify(child).validate(any(), any(), any(), anyString());
    }

    @Test
    public void testWalkShouldValidateSchemaFalse() {
        // --------------------------------------------------------------
        // Arrange – two child schemas.
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());

        JsonSchema child0 = mock(JsonSchema.class);
        JsonSchema child1 = mock(JsonSchema.class);

        // walk on children should be called (validation disabled)
        when(child0.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());
        when(child1.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());

        OneOfValidator validator = createValidator(schemaNode, Arrays.asList(child0, child1));

        // --------------------------------------------------------------
        // Act – walk with shouldValidateSchema = false
        // --------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext,
                mapper.readTree("{\"f\":6}"),
                mapper.readTree("{\"f\":6}"),
                "$.f",
                false);

        // --------------------------------------------------------------
        // Assert – no validation messages and child walks were invoked.
        // --------------------------------------------------------------
        assertTrue(result.isEmpty());
        verify(child0).walk(any(), any(), any(), anyString(), eq(false));
        verify(child1).walk(any(), any(), any(), anyString(), eq(false));
    }

    @Test
    public void testPreloadJsonSchemaInitialisesChildren() {
        // --------------------------------------------------------------
        // Arrange – three child schemas.
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.createArrayNode()
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());

        JsonSchema child0 = mock(JsonSchema.class);
        JsonSchema child1 = mock(JsonSchema.class);
        JsonSchema child2 = mock(JsonSchema.class);

        OneOfValidator validator = createValidator(schemaNode, Arrays.asList(child0, child1, child2));

        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        validator.preloadJsonSchema();

        // --------------------------------------------------------------
        // Assert – each child schema's initializeValidators() was called.
        // --------------------------------------------------------------
        verify(child0).initializeValidators();
        verify(child1).initializeValidators();
        verify(child2).initializeValidators();
    }
}
```
###Test END##