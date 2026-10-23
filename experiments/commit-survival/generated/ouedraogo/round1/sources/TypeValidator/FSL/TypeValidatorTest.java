package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link TypeValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct detection of the schema type from the schema node.</li>
 *   <li>Successful validation when the instance type matches the schema type.</li>
 *   <li>Failure validation when the instance type does not match the schema type.</li>
 *   <li>Proper handling of UNION types – delegation to {@link UnionTypeValidator}.</li>
 *   <li>Side‑effects on the {@link CollectorContext} when the validator is created for a “/type” keyword.</li>
 * </ul>
 *
 * All tests are written with JUnit 4 and Mockito.
 */
public class TypeValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // The collector context is obtained from the execution context.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // The collector context returns mutable sets that we can inspect.
        when(collectorContext.getEvaluatedItems()).thenReturn(new java.util.HashSet<String>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new java.util.HashSet<String>());
    }

    /**
     * Helper that builds a {@link TypeValidator} for a given schema JSON and path.
     */
    private TypeValidator buildValidator(String schemaJson, String schemaPath) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // The parent schema is not needed for the behaviour under test, so we pass null.
        return new TypeValidator(schemaPath, schemaNode, null, validationContext);
    }

    @Test
    public void testGetSchemaTypeReturnsCorrectValue() throws Exception {
        TypeValidator validator = buildValidator("{\"type\":\"string\"}", "/properties/name/type");
        assertEquals(JsonType.STRING, validator.getSchemaType());

        validator = buildValidator("{\"type\":\"integer\"}", "/properties/age/type");
        assertEquals(JsonType.INTEGER, validator.getSchemaType());

        validator = buildValidator("{\"type\":[\"string\",\"null\"]}", "/properties/optional/type");
        assertEquals(JsonType.UNION, validator.getSchemaType());
    }

    @Test
    public void testValidateWhenNodeMatchesSchemaType() throws Exception {
        // Schema expects a string.
        TypeValidator validator = buildValidator("{\"type\":\"string\"}", "/properties/name/type");

        JsonNode instanceNode = MAPPER.readTree("\"hello\"");   // a JSON string
        JsonNode rootNode = MAPPER.readTree("[\"hello\", \"world\"]"); // root is an array
        String at = "/0";

        Set<ValidationMessage> messages = validator.validate(executionContext, instanceNode, rootNode, at);
        // No validation errors expected.
        assertTrue(messages.isEmpty());

        // Because the schemaPath ends with "/type" and the root is an array,
        // the validator should have recorded the evaluated item.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(collectorContext).getEvaluatedItems(); // just to ensure the getter was called
        assertTrue(collectorContext.getEvaluatedItems().contains(at));
    }

    @Test
    public void testValidateWhenNodeDoesNotMatchSchemaType() throws Exception {
        // Schema expects an integer.
        TypeValidator validator = buildValidator("{\"type\":\"integer\"}", "/properties/age/type");

        JsonNode instanceNode = MAPPER.readTree("\"not-an-integer\""); // a JSON string, wrong type
        JsonNode rootNode = MAPPER.readTree("{\"age\":\"not-an-integer\"}"); // root is an object
        String at = "/age";

        Set<ValidationMessage> messages = validator.validate(executionContext, instanceNode, rootNode, at);
        // One validation message should be produced.
        assertEquals(1, messages.size());

        ValidationMessage message = messages.iterator().next();
        // The message should contain the expected and actual types.
        assertTrue(message.getMessage().contains(JsonType.STRING.toString()));
        assertTrue(message.getMessage().contains(JsonType.INTEGER.toString()));
        // The message path should be the supplied “at” location.
        assertEquals(at, message.getPath());

        // Even though the type is wrong, the evaluator still records the property because
        // the schemaPath ends with "/type" and the root is an object.
        assertTrue(collectorContext.getEvaluatedProperties().contains(at));
    }

    @Test
    public void testValidateUnionDelegatesToUnionTypeValidator() throws Exception {
        // Build a validator for a UNION type (string | null).
        TypeValidator validator = buildValidator("{\"type\":[\"string\",\"null\"]}", "/properties/optional/type");

        // Replace the real UnionTypeValidator with a Mockito mock.
        UnionTypeValidator mockUnionValidator = mock(UnionTypeValidator.class);
        Set<ValidationMessage> mockResult = Collections.singleton(new ValidationMessage.Builder()
                .path("/optional")
                .message("union validation mock")
                .build());

        when(mockUnionValidator.validate(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(mockResult);

        // Inject the mock via reflection.
        Field unionField = TypeValidator.class.getDeclaredField("unionTypeValidator");
        unionField.setAccessible(true);
        unionField.set(validator, mockUnionValidator);

        // Prepare dummy nodes.
        JsonNode instanceNode = MAPPER.readTree("\"any string\"");
        JsonNode rootNode = MAPPER.readTree("{\"optional\":\"any string\"}");
        String at = "/optional";

        // Execute validation.
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, at);

        // Verify delegation.
        verify(mockUnionValidator, times(1))
                .validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(at));

        // The result should be exactly what the mock returned.
        assertSame(mockResult, result);
    }

    @Test
    public void testEqualsToSchemaTypeUsesJsonNodeUtil() throws Exception {
        // Use a simple string schema.
        TypeValidator validator = buildValidator("{\"type\":\"string\"}", "/type");

        JsonNode matchingNode = MAPPER.readTree("\"match\"");
        JsonNode nonMatchingNode = MAPPER.readTree("123");

        // The real JsonNodeUtil implementation should be exercised.
        assertTrue(validator.equalsToSchemaType(matchingNode));
        assertFalse(validator.equalsToSchemaType(nonMatchingNode));
    }
}
