/*****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}
 *
 * The tests are written from the point of view of three “experts” that each
 * contribute one test case for every public (or important private) behaviour.
 * The suite therefore covers:
 *
 *   • URI handling (combineCurrentUriWithIds, fragment handling, error cases)
 *   • $ref resolution (getRefSchemaNode)
 *   • Ancestor / lexical root navigation
 *   • Presence of special validators (required, type, $recursiveAnchor)
 *   • Lazy loading of validators (initializeValidators)
 *   • Execution‑context creation
 *
 * Mockito is used to stub the low‑level {@link ValidationContext} where we need
 * to provoke error conditions that are hard to reach via the public factory
 * API.
 *
 * All tests are written for JUnit‑4 (no JUnit‑5 features are used) and can be
 * run with any standard JUnit runner.
 *****************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive test class for {@link JsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper to read a JSON string into a {@link JsonNode}. */
    private static JsonNode json(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** -----------------------------------------------------------------------
     *  1. URI handling – combineCurrentUriWithIds
     * ----------------------------------------------------------------------- */
    @Test
    public void combineCurrentUriWithIds_NoId_ReturnsOriginalUri() throws Exception {
        // Schema without an $id keyword.
        JsonNode schemaNode = json("{\"type\":\"object\"}");

        // Build a ValidationContext using the factory (real implementation).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode, new URI("http://example.com/base.json"));

        // The currentUri stored inside the schema must be exactly the one we passed.
        assertEquals(new URI("http://example.com/base.json"), schema.getCurrentUri());
    }

    @Test
    public void combineCurrentUriWithIds_WithId_CombinesCorrectly() throws Exception {
        // Schema defines an $id that is a relative reference.
        JsonNode schemaNode = json("{\"$id\":\"sub/schema.json\",\"type\":\"object\"}");

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode, new URI("http://example.com/base.json"));

        // Expected URI = base + relative id
        URI expected = new URI("http://example.com/sub/schema.json");
        assertEquals(expected, schema.getCurrentUri());
    }

    @Test
    public void combineCurrentUriWithIds_FragmentWithNoBaseUri_ReturnsNull() throws Exception {
        // $id is a fragment only – without a base URI the method must return null.
        JsonNode schemaNode = json("{\"$id\":\"#foo\",\"type\":\"object\"}");

        // Pass a null base URI (the factory does not support this directly,
        // therefore we invoke the private method via reflection).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema dummy = factory.getSchema(json("{}")); // dummy instance just for reflection

        Method combineMethod = JsonSchema.class.getDeclaredMethod(
                "combineCurrentUriWithIds", URI.class, JsonNode.class);
        combineMethod.setAccessible(true);
        URI result = (URI) combineMethod.invoke(dummy, null, schemaNode);
        assertNull(result);
    }

    @Test(expected = JsonSchemaException.class)
    public void combineCurrentUriWithIds_InvalidId_ThrowsJsonSchemaException() throws Exception {
        // Create a mock ValidationContext that returns an illegal URI as $id.
        ValidationContext ctx = Mockito.mock(ValidationContext.class);
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        Mockito.when(ctx.getMetaSchema()).thenReturn(meta);
        Mockito.when(ctx.resolveSchemaId(Mockito.any(JsonNode.class))).thenReturn("http://[invalid]");

        // The URIFactory used by the context must throw IllegalArgumentException.
        URIFactory uriFactory = Mockito.mock(URIFactory.class);
        Mockito.when(uriFactory.create(Mockito.any(URI.class), Mockito.anyString()))
               .thenThrow(new IllegalArgumentException("bad URI"));
        Mockito.when(ctx.getURIFactory()).thenReturn(uriFactory);
        Mockito.when(ctx.getConfig()).thenReturn(new SchemaValidatorsConfig());

        // Build a JsonSchema using the private constructor (via reflection)
        // because the public factory does not allow us to inject the mock.
        Method ctor = JsonSchema.class.getDeclaredMethod(
                "from", ValidationContext.class, String.class, URI.class, JsonNode.class,
                JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonSchema schema = (JsonSchema) ctor.invoke(null, ctx, "#", null,
                json("{\"$id\":\"http://[invalid]\"}"), null, false);

        // Trigger combineCurrentUriWithIds by calling a public method that forces
        // the constructor logic to run – getCurrentUri lazily accesses the field.
        schema.getCurrentUri();
    }

    /** -----------------------------------------------------------------------
     *  2. Static helpers – uriRefersToSubschema & isUriFragmentWithNoContext
     * ----------------------------------------------------------------------- */
    @Test
    public void uriRefersToSubschema_DetectsFragmentCorrectly() throws Exception {
        Method method = JsonSchema.class.getDeclaredMethod(
                "uriRefersToSubschema", URI.class, String.class);
        method.setAccessible(true);

        URI uriWithFragment = new URI("http://example.com/schema.json#foo");
        boolean result = (boolean) method.invoke(null, uriWithFragment, "#");
        assertTrue(result);
    }

    @Test
    public void isUriFragmentWithNoContext_TrueWhenFragmentAndNoBase() throws Exception {
        Method method = JsonSchema.class.getDeclaredMethod(
                "isUriFragmentWithNoContext", URI.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(null, null, "#/defs");
        assertTrue(result);
    }

    /** -----------------------------------------------------------------------
     *  3. $ref resolution – getRefSchemaNode
     * ----------------------------------------------------------------------- */
    @Test
    public void getRefSchemaNode_ResolvesInternalReference() {
        // Schema with a definition and a top‑level $ref.
        String schemaStr = "{"
                + "\"$id\":\"http://example.com/root.json\","
                + "\"definitions\":{"
                + "   \"addr\":{ \"type\":\"string\" }"
                + "},"
                + "\"$ref\":\"#/definitions/addr\""
                + "}";
        JsonNode schemaNode = json(schemaStr);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode, URI.create("http://example.com/root.json"));

        JsonNode refNode = schema.getRefSchemaNode("#/definitions/addr");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    /** -----------------------------------------------------------------------
     *  4. Ancestor navigation – findAncestor & createChildSchema
     * ----------------------------------------------------------------------- */
    @Test
    public void findAncestor_ReturnsRootSchema() {
        // Root schema
        JsonNode rootNode = json("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema root = factory.getSchema(rootNode);

        // Child schema (a property schema)
        JsonSchema child = root.createChildSchema("#/properties/name", rootNode.get("properties").get("name"));

        // The child’s ancestor should be the root schema.
        assertSame(root, child.findAncestor());
    }

    /** -----------------------------------------------------------------------
     *  5. Presence of special validators (required, type, $recursiveAnchor)
     * ----------------------------------------------------------------------- */
    @Test
    public void hasRequiredValidator_DetectedCorrectly() {
        JsonNode schemaNode = json("{\"type\":\"object\",\"required\":[\"id\"]}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void hasTypeValidator_DetectedCorrectly() {
        JsonNode schemaNode = json("{\"type\":\"array\"}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void isDynamicAnchor_TrueWhenRecursiveAnchorIsTrue() {
        JsonNode schemaNode = json("{\"$recursiveAnchor\":true}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertTrue(schema.isDynamicAnchor());
    }

    /** -----------------------------------------------------------------------
     *  6. Lazy loading of validators – initializeValidators
     * ----------------------------------------------------------------------- */
    @Test
    public void initializeValidators_LoadsAllValidators() throws Exception {
        JsonNode schemaNode = json("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);

        // At this point validators have not been loaded.
        Field loadedField = JsonSchema.class.getDeclaredField("validatorsLoaded");
        loadedField.setAccessible(true);
        assertFalse((boolean) loadedField.get(schema));

        // Call the public method.
        schema.initializeValidators();

        // Verify the flag is now true.
        assertTrue((boolean) loadedField.get(schema));
    }

    /** -----------------------------------------------------------------------
     *  7. Execution‑context creation – createExecutionContext
     * ----------------------------------------------------------------------- */
    @Test
    public void createExecutionContext_ReturnsNonNullContext() {
        JsonNode schemaNode = json("{\"type\":\"null\"}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);

        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }

    /** -----------------------------------------------------------------------
     *  8. toString – human readable representation
     * ----------------------------------------------------------------------- */
    @Test
    public void toString_IncludesPathAndNode() {
        JsonNode schemaNode = json("{\"type\":\"boolean\"}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        String str = schema.toString();

        assertTrue(str.startsWith("\"#\""));
        assertTrue(str.contains("\"type\":\"boolean\""));
    }

    /** -----------------------------------------------------------------------
     *  9. walk / validate – basic happy‑path (no errors)
     * ----------------------------------------------------------------------- */
    @Test
    public void validate_ValidDataReturnsEmptyErrorSet() {
        JsonNode schemaNode = json("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}");
        JsonNode data = json("{\"id\":10}");

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void validate_InvalidDataReturnsErrors() {
        JsonNode schemaNode = json("{\"type\":\"object\",\"required\":[\"name\"]}");
        JsonNode data = json("{\"age\":30}");

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        Set<ValidationMessage> errors = schema.validate(data);
        // At least one error – missing required property.
        assertFalse(errors.isEmpty());
        boolean found = errors.stream()
                .anyMatch(v -> v.getMessage().contains("required"));
        assertTrue(found);
    }

    /** -----------------------------------------------------------------------
     *  10. walk – ensure listeners are invoked without throwing
     * ----------------------------------------------------------------------- */
    @Test
    public void walk_WithNoListenersExecutesWithoutException() {
        JsonNode schemaNode = json("{\"type\":\"string\"}");
        JsonNode data = json("\"hello\"");

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaNode);
        ValidationResult result = schema.walk(data, false);
        assertTrue(result.getValidationMessages().isEmpty());
    }
}
