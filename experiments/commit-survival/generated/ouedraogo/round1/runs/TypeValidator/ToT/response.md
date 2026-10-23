###Test START##
```java
/**
 * JUnit 4 test suite for {@link com.networknt.schema.TypeValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct detection of the schema type</li>
 *   <li>Equality checks between a JsonNode and the declared schema type</li>
 *   <li>Successful validation when the node matches the schema type</li>
 *   <li>Failure validation when the node does not match the schema type</li>
 *   <li>Delegation to {@link UnionTypeValidator} when the schema type is a UNION</li>
 *   <li>The “hack” that records evaluated items / properties when the schemaPath ends with "/type"</li>
 * </ul>
 *
 * Mockito is used to mock the heavy‑weight collaborators
 * {@link com.networknt.schema.ValidationContext},
 * {@link com.networknt.schema.ExecutionContext},
 * {@link com.networknt.schema.CollectorContext},
 * {@link com.networknt.schema.JsonSchema} and
 * {@link com.networknt.schema.UnionTypeValidator}.
 *
 * The Jackson {@link com.fasterxml.jackson.databind.ObjectMapper} is used to create
 * real {@link com.fasterxml.jackson.databind.JsonNode} instances for the schema and
 * the instance being validated.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TypeValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Before
    public void setUp() {
        // The ValidationContext is asked for its Config inside TypeValidator;
        // we can safely return null for the purpose of these tests.
        when(validationContext.getConfig()).thenReturn(null);
        // ExecutionContext returns a CollectorContext that we can inspect.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<String>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<String>());
    }

    /* ---------------------------------------------------------------------- */
    /*  getSchemaType()                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getSchemaType_ShouldReturnCorrectJsonType() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        TypeValidator validator = new TypeValidator("some/path/type", schemaNode, parentSchema, validationContext);
        assertEquals(JsonType.STRING, validator.getSchemaType());
    }

    /* ---------------------------------------------------------------------- */
    /*  equalsToSchemaType()                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void equalsToSchemaType_MatchingNode_ReturnsTrue() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"integer\"}");
        TypeValidator validator = new TypeValidator("some/path/type", schemaNode, parentSchema, validationContext);

        JsonNode instanceNode = MAPPER.readTree("123"); // integer node
        assertTrue(validator.equalsToSchemaType(instanceNode));
    }

    @Test
    public void equalsToSchemaType_NonMatchingNode_ReturnsFalse() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"boolean\"}");
        TypeValidator validator = new TypeValidator("some/path/type", schemaNode, parentSchema, validationContext);

        JsonNode instanceNode = MAPPER.readTree("\"not a boolean\""); // string node
        assertFalse(validator.equalsToSchemaType(instanceNode));
    }

    /* ---------------------------------------------------------------------- */
    /*  validate() – happy path                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void validate_NodeMatchesSchema_ReturnsEmptySet() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"array\"}");
        TypeValidator validator = new TypeValidator("some/path/type", schemaNode, parentSchema, validationContext);

        JsonNode instanceNode = MAPPER.readTree("[1,2,3]"); // array node
        Set<ValidationMessage> messages = validator.validate(executionContext, instanceNode,
                instanceNode, "$");
        assertTrue(messages.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  validate() – type mismatch                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void validate_NodeDoesNotMatchSchema_ReturnsValidationMessage() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        TypeValidator validator = new TypeValidator("some/path/type", schemaNode, parentSchema, validationContext);

        JsonNode instanceNode = MAPPER.readTree("\"I am a string\""); // string node, not object
        Set<ValidationMessage> messages = validator.validate(executionContext, instanceNode,
                instanceNode, "$.value");

        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        // The message is built by BaseJsonValidator – we just verify the placeholders.
        assertTrue(msg.getMessage().contains("object"));
        assertTrue(msg.getMessage().contains("string"));
        assertEquals("$.value", msg.getPath());
    }

    /* ---------------------------------------------------------------------- */
    /*  validate() – UNION type delegation                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void validate_UnionSchema_DelegatesToUnionTypeValidator() throws Exception {
        // Schema with a UNION type: ["string","boolean"]
        JsonNode schemaNode = MAPPER.readTree("{\"type\":[\"string\",\"boolean\"]}");
        TypeValidator validator = new TypeValidator("some/path/type", schemaNode, parentSchema, validationContext);

        // Replace the real UnionTypeValidator with a Mockito mock
        UnionTypeValidator mockUnionValidator = mock(UnionTypeValidator.class);
        Set<ValidationMessage> fakeResult = Collections.singleton(
                ValidationMessage.builder()
                        .type(ValidatorTypeCode.TYPE.getErrorCodeKey())
                        .path("$.union")
                        .message("union error")
                        .build()
        );
        when(mockUnionValidator.validate(any(), any(), any(), any())).thenReturn(fakeResult);

        // Use reflection to inject the mock (the field is private)
        java.lang.reflect.Field field = TypeValidator.class.getDeclaredField("unionTypeValidator");
        field.setAccessible(true);
        field.set(validator, mockUnionValidator);

        JsonNode instanceNode = MAPPER.readTree("\"any string\""); // any node – we don't care
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode,
                instanceNode, "$.union");

        // Verify delegation
        verify(mockUnionValidator, times(1))
                .validate(executionContext, instanceNode, instanceNode, "$.union");
        assertEquals(fakeResult, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  validate() – evaluated items / properties “hack”                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void validate_WhenSchemaPathEndsWithType_AndRootIsArray_AddsEvaluatedItem() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        // Note the schemaPath ends with "/type" to trigger the hack
        TypeValidator validator = new TypeValidator("properties/name/type", schemaNode, parentSchema, validationContext);

        JsonNode rootArray = MAPPER.readTree("[\"a\",\"b\"]"); // root is an array
        JsonNode instanceNode = MAPPER.readTree("\"a\"");

        validator.validate(executionContext, instanceNode, rootArray, "$[0]");

        // The collector should have recorded the evaluated item path
        verify(collectorContext, times(1)).getEvaluatedItems();
        Set<String> evaluatedItems = collectorContext.getEvaluatedItems();
        assertTrue(evaluatedItems.contains("$[0]"));
    }

    @Test
    public void validate_WhenSchemaPathEndsWithType_AndRootIsObject_AddsEvaluatedProperty() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"integer\"}");
        TypeValidator validator = new TypeValidator("properties/age/type", schemaNode, parentSchema, validationContext);

        JsonNode rootObject = MAPPER.readTree("{\"age\":30}");
        JsonNode instanceNode = MAPPER.readTree("30");

        validator.validate(executionContext, instanceNode, rootObject, "$.age");

        // The collector should have recorded the evaluated property path
        verify(collectorContext, times(1)).getEvaluatedProperties();
        Set<String> evaluatedProps = collectorContext.getEvaluatedProperties();
        assertTrue(evaluatedProps.contains("$.age"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor – defensive checks (null schemaNode)                       */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void constructor_NullSchemaNode_ThrowsException() {
        // The real implementation does not guard against null, but a NPE will be
        // thrown when TypeFactory attempts to read the node – we assert that
        // such a failure is propagated.
        new TypeValidator("path/type", null, parentSchema, validationContext);
    }
}
```
###Test END##