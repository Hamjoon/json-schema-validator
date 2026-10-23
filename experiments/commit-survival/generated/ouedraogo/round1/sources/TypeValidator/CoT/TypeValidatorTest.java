package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TypeValidator}.
 *
 * These tests cover:
 *   • basic functionality of each public method,
 *   • edge‑cases such as UNION types, null nodes,
 *   • the special handling when the schemaPath ends with "/type".
 */
@RunWith(MockitoJUnitRunner.class)
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
        // ExecutionContext#getCollectorContext() returns our mocked collector.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // ValidationContext#getConfig() is used by TypeFactory – we can return a default config.
        when(validationContext.getConfig()).thenReturn(new ValidationConfig());
    }

    /* -------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------- */

    @Test
    public void testGetSchemaType_string() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        assertEquals(JsonType.STRING, validator.getSchemaType());
    }

    @Test
    public void testEqualsToSchemaType_matching() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"number\"}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        JsonNode numberNode = MAPPER.readTree("123");
        assertTrue(validator.equalsToSchemaType(numberNode));
    }

    @Test
    public void testEqualsToSchemaType_notMatching() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"boolean\"}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        JsonNode stringNode = MAPPER.readTree("\"hello\"");
        assertFalse(validator.equalsToSchemaType(stringNode));
    }

    @Test
    public void testValidate_matchingType_returnsEmptySet() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"array\"}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        JsonNode arrayNode = MAPPER.readTree("[1,2,3]");
        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "$");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testValidate_mismatchingType_returnsMessage() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        JsonNode arrayNode = MAPPER.readTree("[1,2]");
        Set<ValidationMessage> result = validator.validate(executionContext, arrayNode, arrayNode, "$.data");

        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();

        assertEquals("$.data", msg.getPath());
        assertEquals(JsonType.ARRAY.toString(), msg.getArguments().get(0));   // actual type
        assertEquals(JsonType.OBJECT.toString(), msg.getArguments().get(1)); // expected type
    }

    /* -------------------------------------------------
     *  EDGE‑CASE TESTS
     * ------------------------------------------------- */

    @Test
    public void testValidate_unionType_delegatesToUnionValidator() throws Exception {
        // Schema with a union type (array of types)
        JsonNode schemaNode = MAPPER.readTree("{\"type\":[\"string\",\"number\"]}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        // Replace the real UnionTypeValidator with a mock using reflection
        UnionTypeValidator mockUnion = mock(UnionTypeValidator.class);
        Set<ValidationMessage> mockResult = Collections.singleton(new ValidationMessage.Builder()
                .path("$.union")
                .message("union ok")
                .build());
        when(mockUnion.validate(any(), any(), any(), any())).thenReturn(mockResult);

        // inject mock
        java.lang.reflect.Field field = TypeValidator.class.getDeclaredField("unionTypeValidator");
        field.setAccessible(true);
        field.set(validator, mockUnion);

        JsonNode node = MAPPER.readTree("\"any\"");
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$.union");

        assertSame(mockResult, result);
        verify(mockUnion, times(1)).validate(eq(executionContext), eq(node), eq(node), eq("$.union"));
    }

    @Test
    public void testValidate_schemaPathEndsWithType_rootIsArray_recordsEvaluatedItem() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        TypeValidator validator = new TypeValidator("#/properties/name/type", schemaNode, null, validationContext);

        JsonNode rootArray = MAPPER.readTree("[\"a\",\"b\"]");
        JsonNode node = rootArray.get(0); // first element

        // before validation the set should be empty
        assertTrue(collectorContext.getEvaluatedItems().isEmpty());

        validator.validate(executionContext, node, rootArray, "$[0]");

        // after validation the location must have been added
        assertTrue(collectorContext.getEvaluatedItems().contains("$[0]"));
    }

    @Test
    public void testValidate_schemaPathEndsWithType_rootIsObject_recordsEvaluatedProperty() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"number\"}");
        TypeValidator validator = new TypeValidator("#/properties/age/type", schemaNode, null, validationContext);

        JsonNode rootObject = MAPPER.readTree("{\"age\":30}");
        JsonNode node = rootObject.get("age");

        assertTrue(collectorContext.getEvaluatedProperties().isEmpty());

        validator.validate(executionContext, node, rootObject, "$.age");

        assertTrue(collectorContext.getEvaluatedProperties().contains("$.age"));
    }

    @Test
    public void testValidate_schemaPathNotEndingWithType_noEvaluationRecorded() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"boolean\"}");
        TypeValidator validator = new TypeValidator("#/properties/active", schemaNode, null, validationContext);

        JsonNode root = MAPPER.readTree("{\"active\":true}");
        JsonNode node = root.get("active");

        validator.validate(executionContext, node, root, "$.active");

        assertTrue(collectorContext.getEvaluatedItems().isEmpty());
        assertTrue(collectorContext.getEvaluatedProperties().isEmpty());
    }

    @Test
    public void testEqualsToSchemaType_nullNode_returnsFalse() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"null\"}");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, null, validationContext);

        assertFalse(validator.equalsToSchemaType(null));
    }

    @Test
    public void testValidate_schemaWithoutType_doesNotThrow() throws Exception {
        // Schema that does NOT contain a "type" keyword – TypeFactory will return null.
        JsonNode schemaNode = MAPPER.readTree("{\"maximum\":10}");
        TypeValidator validator = new TypeValidator("#/noType", schemaNode, null, validationContext);

        JsonNode node = MAPPER.readTree("5");

        // The validator should simply return an empty set (no type check to perform)
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");
        assertTrue(result.isEmpty());
    }
}
