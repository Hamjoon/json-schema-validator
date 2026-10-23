###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 *  • Constructor behavior (schema list creation)
 *  • {@code validate(...)} – normal, zero‑match, multi‑match and fail‑fast scenarios
 *  • {@code walk(...)} – both validation‑enabled and validation‑disabled paths
 *  • {@code preloadJsonSchema()} – ensures each child schema is initialized
 *
 * The heavy‑weight collaborators (ExecutionContext, CollectorContext, JsonSchema, etc.)
 * are mocked with Mockito so that the test suite is fast, deterministic and does not
 * depend on the full networknt‑json‑schema library runtime.
 *
 * Author:  Test‑Generation‑Assistant
 * Date:    2026‑10‑23
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;

    /** Helper to create a simple {@link JsonNode} from a JSON literal. */
    private JsonNode node(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    @Before
    public void setUp() {
        // ExecutionContext returns the mocked CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // CollectorContext holds a ValidatorState under the well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // By default walk is disabled
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // By default validation is enabled when walk is called
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // By default a node is considered matched unless a validator flips it
        when(validatorState.hasMatchedNode()).thenReturn(true);
    }

    /* --------------------------------------------------------------
     *  Constructor tests
     * -------------------------------------------------------------- */
    @Test
    public void constructorShouldCreateSchemaListFromArrayNode() throws Exception {
        // a simple array with two schema objects
        JsonNode schemaArray = node("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        OneOfValidator validator = new OneOfValidator(
                "#/properties/foo/oneOf",
                schemaArray,
                mock(JsonSchema.class),
                validationContext);

        // Verify that validationContext#newSchema was called twice with the expected paths
        verify(validationContext, times(2)).newSchema(
                eq("#/properties/foo/oneOf/0"), any(JsonNode.class), any(JsonSchema.class));
        verify(validationContext, times(2)).newSchema(
                eq("#/properties/foo/oneOf/1"), any(JsonNode.class), any(JsonSchema.class));

        // Use reflection to check the private “schemas” collection size
        java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
        assertEquals("Two child schemas should be stored", 2, list.size());
    }

    /* --------------------------------------------------------------
     *  validate(...) – normal (exactly one match)
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldSucceedWhenExactlyOneSchemaMatches() throws Exception {
        // two child schemas – first fails, second succeeds
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        JsonNode dummyNode = node("\"hello\"");

        // schema1 returns a non‑empty error set
        ValidationMessage err1 = mock(ValidationMessage.class);
        when(schema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.singleton(err1));

        // schema2 validates cleanly (empty set) and leaves state.matchedNode true
        when(schema2.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // Build validator with the two mocked schemas
        OneOfValidator validator = buildValidatorWithSchemas(schema1, schema2);

        // Execute validation
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "#/properties/foo");

        // Expect no errors (exactly one schema matched)
        assertTrue("Result should be empty when exactly one schema matches", result.isEmpty());

        // Verify that the validator state was marked as a complex validator and finally
        // setMatchedNode(true) after successful validation
        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).setComplexValidator(true);
        inOrder.verify(validatorState).setMatchedNode(true);
        inOrder.verify(validatorState).setComplexValidator(false);
        inOrder.verify(validatorState).setMatchedNode(true);
    }

    /* --------------------------------------------------------------
     *  validate(...) – zero matches
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldReportErrorWhenNoSchemaMatches() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        JsonNode dummyNode = node("123");

        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        when(schema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.singleton(err1));
        when(schema2.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.singleton(err2));

        OneOfValidator validator = buildValidatorWithSchemas(schema1, schema2);

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "#/properties/bar");

        // Should contain the OneOf error plus both child errors
        assertEquals("Should contain 3 messages (OneOf + 2 child errors)", 3, result.size());

        // Verify that the OneOf error includes the count “0”
        Optional<ValidationMessage> oneOfMsg = result.stream()
                .filter(m -> m.getMessage().contains("0"))
                .findFirst();
        assertTrue("OneOf validation message with count 0 must be present", oneOfMsg.isPresent());

        // Ensure collector context was cleared
        verify(collectorContext).getEvaluatedItems();
        verify(collectorContext).getEvaluatedProperties();
    }

    /* --------------------------------------------------------------
     *  validate(...) – more than one match (short‑circuit)
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldFailWhenMultipleSchemasMatch() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        JsonNode dummyNode = node("\"abc\"");

        // Both schemas validate cleanly
        when(schema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema2.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidatorWithSchemas(schema1, schema2);

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "#/properties/baz");

        // Expect a OneOf error with count 2 and no child errors because validation short‑circuits
        assertEquals("Only the OneOf error should be present", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue("Message must contain count 2", msg.getMessage().contains("2"));
    }

    /* --------------------------------------------------------------
     *  validate(...) – fail‑fast mode
     * -------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void validateShouldThrowWhenFailFastIsEnabled() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        JsonNode dummyNode = node("true");

        // Both schemas succeed → multiple matches trigger OneOf error
        when(schema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema2.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidatorWithSchemas(schema1, schema2);
        // enable fail‑fast via reflection (the field lives in BaseJsonValidator)
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // This call must throw JsonSchemaException
        validator.validate(executionContext, dummyNode, dummyNode, "#/properties/fast");
    }

    /* --------------------------------------------------------------
     *  walk(...) – validation enabled
     * -------------------------------------------------------------- */
    @Test
    public void walkShouldDelegateToValidateWhenShouldValidateIsTrue() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        JsonNode dummyNode = node("null");

        // Build a validator with a single child schema
        OneOfValidator validator = buildValidatorWithSchemas(schema);

        // Stub the internal validate() to return a known set
        Set<ValidationMessage> stubMessages = Collections.singleton(mock(ValidationMessage.class));
        OneOfValidator spyValidator = spy(validator);
        doReturn(stubMessages).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = spyValidator.walk(executionContext, dummyNode,
                dummyNode, "#/prop", true);

        assertSame("walk should return exactly what validate returned", stubMessages, result);
        verify(spyValidator, times(1)).validate(executionContext, dummyNode, dummyNode, "#/prop");
    }

    /* --------------------------------------------------------------
     *  walk(...) – validation disabled (shouldValidate false)
     * -------------------------------------------------------------- */
    @Test
    public void walkShouldInvokeChildWalkWhenShouldValidateIsFalse() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        JsonNode dummyNode = node("[]");

        OneOfValidator validator = buildValidatorWithSchemas(schema1, schema2);

        // Walk should call each child schema's walk but return an empty set
        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode,
                dummyNode, "#/prop", false);

        assertTrue("Result should be empty when shouldValidate is false", result.isEmpty());
        verify(schema1, times(1)).walk(executionContext, dummyNode,
                dummyNode, "#/prop", false);
        verify(schema2, times(1)).walk(executionContext, dummyNode,
                dummyNode, "#/prop", false);
    }

    /* --------------------------------------------------------------
     *  preloadJsonSchema() – child schema initialization
     * -------------------------------------------------------------- */
    @Test
    public void preloadJsonSchemaShouldInitializeAllChildSchemas() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        OneOfValidator validator = buildValidatorWithSchemas(schema1, schema2);
        validator.preloadJsonSchema();

        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------
     *  Helper to build a OneOfValidator with arbitrary child schemas
     * -------------------------------------------------------------- */
    private OneOfValidator buildValidatorWithSchemas(JsonSchema... childSchemas) throws Exception {
        // Create a dummy array node that has the same size as childSchemas
        JsonNode dummyArray = mock(JsonNode.class);
        when(dummyArray.size()).thenReturn(childSchemas.length);
        // When iterated, return a simple object node for each index (content is irrelevant)
        for (int i = 0; i < childSchemas.length; i++) {
            JsonNode childNode = mock(JsonNode.class);
            when(dummyArray.get(i)).thenReturn(childNode);
        }

        // Mock ValidationContext#newSchema to return the supplied child schemas in order
        for (int i = 0; i < childSchemas.length; i++) {
            final int idx = i;
            when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                    .thenAnswer(invocation -> childSchemas[idx]);
        }

        // Build the validator (the constructor will call validationContext.newSchema)
        return new OneOfValidator("#/oneOf", dummyArray,
                mock(JsonSchema.class), validationContext);
    }
}
```
###Test END##