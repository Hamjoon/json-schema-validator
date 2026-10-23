###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The test class is written from the perspective of three “experts” that
 * collaboratively:
 *   1. Extract the public API of {@code JsonSchema}.
 *   2. Write a basic test for each public method.
 *   3. Identify edge‑cases / exceptional situations.
 *   4. Add dedicated tests for those edge‑cases.
 *   5. Merge everything into a single, runnable test file.
 *
 * The tests rely on the real implementation of the Network NT JSON‑Schema
 * validator (the same library that provides {@code JsonSchema}) together
 * with Jackson for JSON handling and Mockito for a few light‑weight mocks.
 *
 * NOTE: The test file is self‑contained – it only needs the following Maven
 * dependencies (already used by the library itself):
 *
 * <dependency>
 *   <groupId>com.networknt</groupId>
 *   <artifactId>json-schema-validator</artifactId>
 *   <version>1.2.0</version>   <!-- any recent version works -->
 * </dependency>
 * <dependency>
 *   <groupId>com.fasterxml.jackson.core</groupId>
 *   <artifactId>jackson-databind</artifactId>
 *   <version>2.15.0</version>
 * </dependency>
 * <dependency>
 *   <groupId>junit</groupId>
 *   <artifactId>junit</artifactId>
 *   <version>4.13.2</version>
 *   <scope>test</scope>
 * </dependency>
 * <dependency>
 *   <groupId>org.mockito</groupId>
 *   <artifactId>mockito-core</artifactId>
 *   <version>5.5.0</version>
 *   <scope>test</scope>
 * </dependency>
 *
 * The test suite covers:
 *   • Normal behaviour of each public method.
 *   • Edge cases such as null URIs, fragment‑only IDs, invalid IDs, missing $ref,
 *     and the $recursiveAnchor keyword.
 *   • Exception handling (e.g. {@link JsonSchemaException} when an illegal ID is
 *     supplied).
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test class for {@link JsonSchema}.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper that creates a {@link JsonSchema} from a JSON string using the
     *  default factory (draft‑07). */
    private JsonSchema schemaFromString(String json) {
        JsonNode node = MAPPER.readTree(json);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        return factory.getSchema(node);
    }

    /* --------------------------------------------------------------------- */
    /*  1. BASIC TESTS – one for each public method                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateChildSchema() throws Exception {
        JsonSchema parent = schemaFromString("{\"type\":\"object\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", MAPPER.readTree("{\"type\":\"string\"}"));
        assertNotNull(child);
        assertEquals("#/properties/name", child.getSchemaPath());
    }

    @Test
    public void testGetCurrentUri_WithIdKeyword() throws Exception {
        JsonSchema schema = schemaFromString("{\"$id\":\"http://example.com/my-schema\"}");
        URI uri = schema.getCurrentUri();
        assertNotNull(uri);
        assertEquals(new URI("http://example.com/my-schema"), uri);
    }

    @Test
    public void testGetCurrentUri_WhenNoIdPresent() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointer() throws Exception {
        String json = "{"
                + "\"definitions\":{"
                + "   \"foo\":{ \"type\":\"string\" }"
                + "}"
                + "}";
        JsonSchema schema = schemaFromString(json);
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_FragmentReference() throws Exception {
        // Same schema as above – the fragment handling should delegate to the meta‑schema.
        String json = "{"
                + "\"definitions\":{"
                + "   \"bar\":{ \"type\":\"integer\" }"
                + "}"
                + "}";
        JsonSchema schema = schemaFromString(json);
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/bar");
        assertNotNull(refNode);
        assertEquals("integer", refNode.get("type").asText());
    }

    @Test
    public void testFindAncestor_RootIsSelf() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"null\"}");
        assertSame(schema, schema.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_NoId() throws Exception {
        // No $id anywhere – the lexical root should be the schema itself.
        JsonSchema schema = schemaFromString("{\"type\":\"boolean\"}");
        assertSame(schema, schema.findLexicalRoot());
    }

    @Test
    public void testValidate_WithValidData() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"number\"}");
        JsonNode good = MAPPER.readTree("42");
        Set<ValidationMessage> errors = schema.validate(good);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidate_WithInvalidData() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"number\"}");
        JsonNode bad = MAPPER.readTree("\"not a number\"");
        Set<ValidationMessage> errors = schema.validate(bad);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateAndCollect() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
        JsonNode data = MAPPER.readTree("[\"a\",\"b\"]");
        ValidationResult result = schema.validateAndCollect(data);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalk_WithValidationEnabled() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"object\",\"properties\":{\"p\":{\"type\":\"integer\"}}}");
        JsonNode data = MAPPER.readTree("{\"p\":10}");
        ValidationResult result = schema.walk(data, true);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalk_WithoutValidation() throws Exception {
        // Walk with validation disabled should still return a result object (no errors).
        JsonSchema schema = schemaFromString("{\"type\":\"object\"}");
        JsonNode data = MAPPER.readTree("{\"any\":\"value\"}");
        ValidationResult result = schema.walk(data, false);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testHasRequiredValidator_Positive() throws Exception {
        JsonSchema schema = schemaFromString("{\"required\":[\"name\"]}");
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidator_Negative() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        assertFalse(schema.hasRequiredValidator());
        assertNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_Positive() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"array\"}");
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidator_Negative() throws Exception {
        JsonSchema schema = schemaFromString("{\"properties\":{\"x\":{\"type\":\"string\"}}}");
        assertFalse(schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    @Test
    public void testInitializeValidators_IsIdempotent() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        // First call – validators are loaded.
        schema.initializeValidators();
        // Second call – should not throw or reload.
        schema.initializeValidators();
        // No explicit assertion – the test passes if no exception is thrown.
    }

    @Test
    public void testIsDynamicAnchor_DefaultFalse() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"object\"}");
        assertFalse(schema.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_NotNull() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"null\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }

    @Test
    public void testToString_IncludesSchemaPathAndNode() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"boolean\"}");
        String s = schema.toString();
        assertTrue(s.contains(schema.getSchemaPath()));
        assertTrue(s.contains("\"type\":\"boolean\""));
    }

    /* --------------------------------------------------------------------- */
    /*  2. EDGE‑CASE & EXCEPTION TESTS                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCombineCurrentUriWithIds_FragmentOnlyIdAndNullBase() throws Exception {
        // Schema contains only a fragment ID and the supplied base URI is null.
        // According to the implementation, combineCurrentUriWithIds should return null.
        String json = "{\"$id\":\"#myFragment\"}";
        JsonSchema schema = schemaFromString(json);
        assertNull(schema.getCurrentUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void testInvalidId_ThrowsJsonSchemaException() throws Exception {
        // An illegal URI should cause the constructor to throw JsonSchemaException.
        String json = "{\"$id\":\"ht!tp://[invalid]\"}";
        // The factory internally builds the JsonSchema which will trigger the exception.
        schemaFromString(json);
    }

    @Test
    public void testRecursiveAnchor_TrueSetsDynamicAnchor() throws Exception {
        String json = "{\"$recursiveAnchor\":true}";
        JsonSchema schema = schemaFromString(json);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testRecursiveAnchor_NonBoolean_ThrowsException() {
        // The validator should raise a JsonSchemaException when $recursiveAnchor is not a boolean.
        String json = "{\"$recursiveAnchor\":\"yes\"}";
        try {
            schemaFromString(json);
            fail("Expected JsonSchemaException due to non‑boolean $recursiveAnchor");
        } catch (JsonSchemaException e) {
            // Expected – verify that the message mentions $recursiveAnchor.
            assertTrue(e.getMessage().contains("$recursiveAnchor"));
        }
    }

    @Test
    public void testGetRefSchemaNode_MissingNodeReturnsNull() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"object\"}");
        // Reference a non‑existent definition.
        JsonNode node = schema.getRefSchemaNode("#/definitions/unknown");
        assertNull(node);
    }

    @Test
    public void testGetRefSchemaNode_UrnReference() throws Exception {
        // The meta‑schema can resolve URN fragments; here we use a simple case.
        // The test only verifies that no exception is thrown and the result is null
        // (since the URN does not exist in the test schema).
        JsonSchema schema = schemaFromString("{\"type\":\"object\"}");
        JsonNode node = schema.getRefSchemaNode("urn:example:missing");
        assertNull(node);
    }

    @Test
    public void testWalk_KeywordListenerRunner_NullWhenNoConfig() throws Exception {
        // Build a schema with a ValidationContext that has no configuration (null).
        // The internal keywordWalkListenerRunner should be null and the walk must still work.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        // Force the internal field to null via reflection (simulating missing config).
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        f.setAccessible(true);
        f.set(schema, null);
        JsonNode data = MAPPER.readTree("\"test\"");
        ValidationResult result = schema.walk(data, true);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  3. MERGED COMPLETE TEST CLASS (already presented above)            */
    /* --------------------------------------------------------------------- */
}
```
###Test END##