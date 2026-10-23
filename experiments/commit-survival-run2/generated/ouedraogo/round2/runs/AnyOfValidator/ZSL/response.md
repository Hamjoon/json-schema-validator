###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static com.networknt.schema.ValidatorTypeCode.REQUIRED;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnyOfValidator}.  The tests use Mockito to mock the
 * heavy‑weight collaborators (ValidationContext, JsonSchema, ExecutionContext,
 * CollectorContext, Scope, ValidatorState, etc.) and focus on the control
 * flow inside {@code validate()}, {@code walk()} and {@code preloadJsonSchema()}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // ------------------------------------------------------------------------
    // Common test fixtures
    // ------------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private CollectorContext.Scope grandParentScope;
    @Mock private CollectorContext.Scope parentScope;
    @Mock private CollectorContext.Scope childScope;
    @Mock private ValidatorState validatorState;
    @Mock private JsonSchema parentSchema;   // not really used
    @Mock private TypeValidator typeValidator;

    private final JsonNodeFactory factory = JsonNodeFactory.instance;
    private ArrayNode anyOfArrayNode;   // the "anyOf" array node passed to the constructor

    @Before
    public void setUp() {
        // ---- common stubbing for the ValidationContext ----------------------
        when(validationContext.getConfig()).thenReturn(config);
        // By default discriminators are disabled – individual tests can override.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // ---- collector context ------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, childScope);

        // ---- validator state --------------------------------------------------
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // ---- a minimal anyOf array (two empty schema objects) ----------------
        anyOfArrayNode = factory.arrayNode();
        anyOfArrayNode.add(factory.objectNode()); // element 0
        anyOfArrayNode.add(factory.objectNode()); // element 1
    }

    // ------------------------------------------------------------------------
    // Helper to build an AnyOfValidator with a list of mocked JsonSchema objects
    // ------------------------------------------------------------------------
    private AnyOfValidator buildValidator(JsonSchema... schemas) {
        // make the ValidationContext create the supplied schemas in order
        Answer<JsonSchema> answer = new Answer<JsonSchema>() {
            private int idx = 0;
            @Override public JsonSchema answer(InvocationOnMock invocation) {
                return schemas[idx++];
            }
        };
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenAnswer(answer);

        return new AnyOfValidator("anyOfPath", anyOfArrayNode, parentSchema, validationContext);
    }

    // ------------------------------------------------------------------------
    // 1. Type‑mismatch branch – the validator should add a type error and skip
    //    validation of the underlying schema.
    // ------------------------------------------------------------------------
    @Test
    public void validate_typeMismatch_addsTypeErrorAndSkipsSchema() {
        // ----- mock a schema that reports a TypeValidator --------------------
        JsonSchema mockedSchema = mock(JsonSchema.class);
        when(mockedSchema.hasTypeValidator()).thenReturn(true);
        when(mockedSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        // node type does NOT match the schema type
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);

        // ----- build the validator -----------------------------------------
        AnyOfValidator validator = buildValidator(mockedSchema);

        // ----- run validation ------------------------------------------------
        JsonNode node = factory.textNode("some text");
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // ----- we expect exactly one error (the type error) -----------------
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        // The message type is the schema type's string representation
        assertEquals(JsonType.STRING.toString(), msg.getType());

        // ----- ensure that the underlying schema.validate() was never called --
        verify(mockedSchema, never()).validate(any(), any(), any(), anyString());
    }

    // ------------------------------------------------------------------------
    // 2. Successful validation (no discriminator).  The validator should
    //    return an empty set and mark the node as matched.
    // ------------------------------------------------------------------------
    @Test
    public void validate_successful_noDiscriminator_returnsEmptyAndMarksMatched() {
        JsonSchema goodSchema = mock(JsonSchema.class);
        when(goodSchema.hasTypeValidator()).thenReturn(false);
        when(goodSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(goodSchema);

        JsonNode node = factory.objectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // Expect no errors
        assertTrue(result.isEmpty());

        // The validator must have set the matched‑node flag to true
        verify(validatorState).setMatchedNode(true);
    }

    // ------------------------------------------------------------------------
    // 3. Multiple subschemas – one passes, the other fails with a "required"
    //    error.  The final result must be empty because required‑only errors
    //    are stripped when at least one subschema validates.
    // ------------------------------------------------------------------------
    @Test
    public void validate_oneValid_oneRequiredError_returnsEmpty() {
        // ----- schema that validates successfully ----------------------------
        JsonSchema validSchema = mock(JsonSchema.class);
        when(validSchema.hasTypeValidator()).thenReturn(false);
        when(validSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // ----- schema that fails with a required‑property error ---------------
        JsonSchema failingSchema = mock(JsonSchema.class);
        when(failingSchema.hasTypeValidator()).thenReturn(false);
        ValidationMessage requiredMsg = ValidationMessage.builder()
                .type(REQUIRED.getValue())
                .message("required property missing")
                .path("$")
                .build();
        when(failingSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(requiredMsg));

        AnyOfValidator validator = buildValidator(validSchema, failingSchema);

        JsonNode node = factory.objectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // Because a subschema succeeded, the required‑error must be stripped.
        assertTrue(result.isEmpty());
    }

    // ------------------------------------------------------------------------
    // 4. walk() – when shouldValidateSchema == true it must delegate to
    //    validate(); when false it must invoke walk on each subschema and
    //    return an empty set.
    // ------------------------------------------------------------------------
    @Test
    public void walk_delegatesToValidateWhenFlagTrue() {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(schema);

        JsonNode node = factory.objectNode();
        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", true);

        // The result must be exactly what validate() would have returned (empty)
        assertTrue(result.isEmpty());
    }

    @Test
    public void walk_callsSubschemasWhenFlagFalse() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        AnyOfValidator validator = buildValidator(schema1, schema2);

        JsonNode node = factory.objectNode();
        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);

        // verify that walk(false) was invoked on each subschema
        verify(schema1).walk(eq(executionContext), eq(node), eq(node), eq("$"), eq(false));
        verify(schema2).walk(eq(executionContext), eq(node), eq(node), eq("$"), eq(false));

        // The method must always return an empty set when shouldValidateSchema is false
        assertTrue(result.isEmpty());
    }

    // ------------------------------------------------------------------------
    // 5. preloadJsonSchema() – ensure that the protected helper in
    //    BaseJsonValidator is called for each subschema.
    // ------------------------------------------------------------------------
    @Test
    public void preloadJsonSchema_callsPreloadForAllSubschemas() {
        // Create two dummy schemas
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        // Subclass the validator only to expose the protected method
        AnyOfValidator validator = new AnyOfValidator("anyOfPath", anyOfArrayNode, parentSchema, validationContext) {
            final List<JsonSchema> captured = new ArrayList<>();

            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                captured.addAll(schemas);
            }

            List<JsonSchema> getCaptured() {
                return captured;
            }
        };

        // replace the automatically created schemas with our mocks
        // (the constructor already called newSchema – we replace the list)
        // Use reflection to inject the mocks (simpler than trying to control newSchema)
        try {
            java.lang.reflect.Field field = AnyOfValidator.class.getDeclaredField("schemas");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) field.get(validator);
            list.clear();
            list.add(schemaA);
            list.add(schemaB);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act
        validator.preloadJsonSchema();

        // Verify that our overridden method saw both schemas
        @SuppressWarnings("unchecked")
        List<JsonSchema> captured = ((AnyOfValidator) validator).getCaptured();
        assertEquals(2, captured.size());
        assertTrue(captured.containsAll(Arrays.asList(schemaA, schemaB)));
    }
}
```
###Test END##