###Test START##
```java
/**
 * JUnit 4 test suite for {@link com.networknt.schema.TypeValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and {@code getSchemaType()}</li>
 *   <li>{@code equalsToSchemaType()} – matching and non‑matching nodes</li>
 *   <li>{@code validate()} – successful validation, type mismatch, and evaluation tracking</li>
 *   <li>Union type handling – delegating to {@link com.networknt.schema.UnionTypeValidator}</li>
 * </ul>
 *
 * The test uses real Jackson {@code JsonNode} objects to avoid mocking static
 * methods in {@code TypeFactory}.  Mockito is used for the surrounding
 * {@code JsonSchema}, {@code ValidationContext}, {@code ExecutionContext}
 * and {@code CollectorContext} collaborators.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TypeValidatorTest {

    private static final String SCHEMA_PATH = "#/properties/name/type";

    private ObjectMapper mapper = new ObjectMapper();

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
        // ValidationContext#getConfig() is used inside JsonNodeUtil.
        // Returning null is acceptable for the default behaviour of the library.
        when(validationContext.getConfig()).thenReturn(null);

        // ExecutionContext#getCollectorContext() is required for the evaluation tracking logic.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /**
     * Helper that builds a TypeValidator for the given schema JSON.
     */
    private TypeValidator buildValidator(String schemaJson) throws Exception {
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // Use a generic schema path that ends with "/type" to trigger the evaluation tracking logic.
        return new TypeValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
    }

    @Test
    public void testGetSchemaType_String() throws Exception {
        TypeValidator validator = buildValidator("{\"type\":\"string\"}");
        assertEquals(JsonType.STRING, validator.getSchemaType());
    }

    @Test
    public void testEqualsToSchemaType_Matching() throws Exception {
        TypeValidator validator = buildValidator("{\"type\":\"integer\"}");
        JsonNode matchingNode = new IntNode(42);
        assertTrue("Node should match the schema type integer", validator.equalsToSchemaType(matchingNode));
    }

    @Test
    public void testEqualsToSchemaType_NotMatching() throws Exception {
        TypeValidator validator = buildValidator("{\"type\":\"boolean\"}");
        JsonNode nonMatchingNode = new TextNode("not a boolean");
        assertFalse("Node should NOT match the schema type boolean", validator.equalsToSchemaType(nonMatchingNode));
    }

    @Test
    public void testValidate_MatchingType_NoEvaluationTracking() throws Exception {
        // Schema path does NOT end with "/type" – evaluation tracking should be skipped.
        String schemaPath = "#/properties/name";
        JsonNode schemaNode = mapper.readTree("{\"type\":\"number\"}");
        TypeValidator validator = new TypeValidator(schemaPath, schemaNode, parentSchema, validationContext);

        JsonNode node = new DoubleNode(3.14);
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$.price");

        assertTrue("Validation should succeed for matching type", result.isEmpty());
        // No interaction with collector context because the schemaPath does not end with "/type".
        verifyZeroInteractions(collectorContext);
    }

    @Test
    public void testValidate_TypeMismatch_ReturnsMessage() throws Exception {
        TypeValidator validator = buildValidator("{\"type\":\"array\"}");

        // Provide a non‑array node (object) to trigger a type mismatch.
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$.data");

        assertEquals("Exactly one validation message should be produced", 1, result.size());
        ValidationMessage message = result.iterator().next();

        // The message should contain the location, the actual node type and the expected schema type.
        assertTrue(message.getMessage().contains("$.data"));
        assertTrue(message.getMessage().contains(JsonType.OBJECT.toString()));
        assertTrue(message.getMessage().contains(JsonType.ARRAY.toString()));
    }

    @Test
    public void testValidate_EvaluatedProperty_Added() throws Exception {
        // schemaPath ends with "/type" (as defined by SCHEMA_PATH constant)
        TypeValidator validator = buildValidator("{\"type\":\"string\"}");

        // Prepare a root object node (so the validator should add the location to evaluatedProperties)
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("name", new TextNode("John"));

        // The node being validated is the value of the property.
        JsonNode node = new TextNode("John");

        // Prepare a mutable set that the mock collector will expose.
        Set<String> evaluatedProps = new HashSet<>();
        when(collectorContext.getEvaluatedProperties()).thenReturn(evaluatedProps);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());

        Set<ValidationMessage> result = validator.validate(executionContext, node, rootNode, "$.name");

        assertTrue("Validation should succeed", result.isEmpty());
        assertTrue("The property location should be recorded", evaluatedProps.contains("$.name"));
    }

    @Test
    public void testValidate_EvaluatedItem_Added() throws Exception {
        TypeValidator validator = buildValidator("{\"type\":\"integer\"}");

        // Root is an array, so the location should be added to evaluatedItems.
        ArrayNode rootArray = mapper.createArrayNode();
        rootArray.add(10);
        rootArray.add(20);

        JsonNode node = new IntNode(10);

        Set<String> evaluatedItems = new HashSet<>();
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        Set<ValidationMessage> result = validator.validate(executionContext, node, rootArray, "$[0]");

        assertTrue("Validation should succeed", result.isEmpty());
        assertTrue("The item location should be recorded", evaluatedItems.contains("$[0]"));
    }

    @Test
    public void testValidate_UnionType_DelegatesToUnionValidator() throws Exception {
        // Build a validator that would normally create a UnionTypeValidator (type is an array).
        TypeValidator validator = buildValidator("{\"type\":[\"string\",\"null\"]}");

        // Replace the internally created UnionTypeValidator with a mock.
        UnionTypeValidator mockUnionValidator = mock(UnionTypeValidator.class);
        Set<ValidationMessage> mockResult = Collections.singleton(mock(ValidationMessage.class));

        when(mockUnionValidator.validate(any(ExecutionContext.class),
                                          any(JsonNode.class),
                                          any(JsonNode.class),
                                          anyString()))
                .thenReturn(mockResult);

        // Use reflection to inject the mock.
        Field unionField = TypeValidator.class.getDeclaredField("unionTypeValidator");
        unionField.setAccessible(true);
        unionField.set(validator, mockUnionValidator);

        // Also force the schemaType to UNION (the constructor already did this, but we set it explicitly for safety).
        Field typeField = TypeValidator.class.getDeclaredField("schemaType");
        typeField.setAccessible(true);
        typeField.set(validator, JsonType.UNION);

        JsonNode node = new TextNode("any");
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$.union");

        // Verify delegation.
        verify(mockUnionValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(node), eq("$.union"));
        assertSame("Result should be exactly what the mock UnionTypeValidator returned", mockResult, result);
    }
}
```
###Test END##