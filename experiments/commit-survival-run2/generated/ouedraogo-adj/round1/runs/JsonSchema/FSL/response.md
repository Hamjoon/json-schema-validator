###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JsonSchema}. The tests exercise the public API
 * and, where feasible, the internal behaviour that can be reached via
 * reflection. All objects are created through the real constructors
 * provided by the library – no mocking framework is used.
 */
public class JsonSchemaTest {

    private ValidationContext validationContext;
    private JsonNode emptySchemaNode;
    private JsonSchema schema;

    @Before
    public void setUp() throws Exception {
        // Build the minimal objects required to instantiate a JsonSchema.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV4();   // any meta‑schema works for these tests
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);

        ObjectMapper mapper = new ObjectMapper();
        emptySchemaNode = mapper.readTree("{}"); // an empty JSON object as schema
        // use the package‑private static factory method to create the instance
        schema = JsonSchema.from(validationContext, "#", null, emptySchemaNode, null, false);
    }

    @Test
    public void testGetCurrentUriWhenNoneProvided() {
        // The constructor receives a null currentUri and the schema does not contain an "$id".
        assertNull("Current URI should be null when none is supplied and no $id is present", schema.getCurrentUri());
    }

    @Test
    public void testHasRequiredAndTypeValidatorsInitiallyFalse() {
        assertFalse("Schema should not have a required validator for an empty schema", schema.hasRequiredValidator());
        assertNull("Required validator should be null when not present", schema.getRequiredValidator());

        assertFalse("Schema should not have a type validator for an empty schema", schema.hasTypeValidator());
        assertNull("Type validator should be null when not present", schema.getTypeValidator());
    }

    @Test
    public void testToStringRepresentation() {
        // The default schemaPath is "#" and the node is an empty object.
        String expected = "\"#\" : {}";
        assertEquals("toString() should return the schemaPath and the raw node", expected, schema.toString());
    }

    @Test
    public void testFindAncestorAndLexicalRootWithoutParent() {
        // With no parent supplied the schema is its own ancestor and lexical root.
        assertSame("findAncestor() should return itself when there is no parent", schema, schema.findAncestor());
        assertSame("findLexicalRoot() should return itself when there is no parent and no $id", schema, schema.findLexicalRoot());
    }

    @Test
    public void testCreateChildSchemaCreatesDistinctInstance() {
        JsonNode childNode = new ObjectMapper().readTree("{\"type\":\"string\"}");
        JsonSchema child = schema.createChildSchema("#/properties/name", childNode);
        assertNotNull("Child schema should be created", child);
        assertNotSame("Child schema must be a different instance from the parent", schema, child);
        assertEquals("Child schemaPath must match the argument supplied", "#/properties/name", child.getSchemaPath());
        assertEquals("Child schemaNode must be the node supplied", childNode, child.getSchemaNode());
        assertSame("Child's parent schema must be the original schema", schema, child.getParentSchema());
    }

    @Test
    public void testIsDynamicAnchorDefaultFalse() {
        assertFalse("Dynamic anchor flag should be false when not set in the schema", schema.isDynamicAnchor());
    }

    @Test
    public void testInitializeValidatorsDoesNotThrowAndSetsFlag() throws Exception {
        // The schema has no validators; initialise should succeed and set the internal flag.
        schema.initializeValidators();

        // Use reflection to verify the private flag 'validatorsLoaded' is true.
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        boolean loaded = (Boolean) f.get(schema);
        assertTrue("validatorsLoaded flag must be true after initializeValidators()", loaded);
    }

    @Test
    public void testCreateExecutionContextProvidesCollectorContext() {
        ExecutionContext execCtx = schema.createExecutionContext();
        assertNotNull("ExecutionContext must be created", execCtx);
        assertNotNull("CollectorContext inside ExecutionContext must not be null", execCtx.getCollectorContext());
    }

    @Test
    public void testGetRefSchemaNodeWithSimpleFragment() {
        // A schema that contains a property named "foo".  The fragment "#/properties/foo"
        // should resolve to that node.
        String schemaJson = "{ \"properties\": { \"foo\": { \"type\": \"string\" } } }";
        JsonNode complexNode = new ObjectMapper().readTree(schemaJson);
        JsonSchema complexSchema = JsonSchema.from(validationContext, "#", null, complexNode, null, false);

        JsonNode result = complexSchema.getRefSchemaNode("#/properties/foo");
        assertNotNull("Resolved node must not be null", result);
        assertEquals("Resolved node must contain the expected type keyword", "string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeWithMissingFragmentReturnsNull() {
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Missing fragment should result in null", result);
    }

    @Test
    public void testValidatorSortComparatorOrdering() {
        // Build a list of keys that exercise the special ordering rules.
        List<String> keys = new ArrayList<>();
        keys.add("#/properties");
        keys.add("#/required");
        keys.add("#/patternProperties");
        keys.add("#/unevaluatedItems");
        keys.add("#/unevaluatedProperties");
        keys.add("#/additionalProperties");
        keys.add("#/type");

        // Shuffle to ensure the comparator does the ordering.
        Collections.shuffle(keys);
        Collections.sort(keys, getValidatorSortComparator());

        // Expected order according to the comparator implementation:
        // 1. .../properties
        // 2. .../patternProperties
        // 3. .../type (lexicographically after the special cases)
        // 4. .../required (no special rule)
        // 5. .../additionalProperties (lexicographically)
        // 6. .../unevaluatedProperties (after additionalProperties)
        // 7. .../unevaluatedItems (after unevaluatedProperties)

        List<String> expected = new ArrayList<>();
        expected.add("#/properties");
        expected.add("#/patternProperties");
        expected.add("#/type");
        expected.add("#/required");
        expected.add("#/additionalProperties");
        expected.add("#/unevaluatedProperties");
        expected.add("#/unevaluatedItems");

        assertEquals("Validator keys must be sorted according to the custom comparator", expected, keys);
    }

    /**
     * Helper to obtain the private static VALIDATOR_SORT comparator via reflection.
     */
    private static Comparator<String> getValidatorSortComparator() throws Exception {
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("VALIDATOR_SORT");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Comparator<String> comp = (Comparator<String>) f.get(null);
        return comp;
    }

    @Test
    public void testUriRefersToSubschemaStaticMethod() throws Exception {
        // Access the private static method via reflection.
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);

        // Case 1: originalUri has a fragment and schemaPath is "#" – should be true.
        URI uriWithFragment = new URI("http://example.com/schema.json#foo");
        Boolean result1 = (Boolean) m.invoke(null, uriWithFragment, "#");
        assertTrue("uriRefersToSubschema should be true when fragment present and schemaPath is '#'", result1);

        // Case 2: originalUri without fragment – false.
        URI uriNoFragment = new URI("http://example.com/schema.json");
        Boolean result2 = (Boolean) m.invoke(null, uriNoFragment, "#");
        assertFalse("uriRefersToSubschema should be false when there is no fragment", result2);

        // Case 3: fragment present but schemaPath not blank – false.
        Boolean result3 = (Boolean) m.invoke(null, uriWithFragment, "/properties");
        assertFalse("uriRefersToSubschema should be false when schemaPath is not blank", result3);
    }

    @Test
    public void testIsUriFragmentWithNoContextStaticMethod() throws Exception {
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        m.setAccessible(true);

        // Fragment with no base URI -> true
        Boolean trueResult = (Boolean) m.invoke(null, null, "#/defs/foo");
        assertTrue("Fragment with no base URI should be recognised", trueResult);

        // Non‑fragment with no base URI -> false
        Boolean falseResult = (Boolean) m.invoke(null, null, "http://example.com/schema");
        assertFalse("Non‑fragment with no base URI should be false", falseResult);

        // Fragment with a base URI -> false
        Boolean falseResult2 = (Boolean) m.invoke(null, new URI("http://example.com/base"), "#/defs/bar");
        assertFalse("Fragment with a base URI should be false", falseResult2);
    }

    @Test
    public void testStringUtilsBlankAndNotBlank() {
        assertTrue("StringUtils.isBlank should return true for null", StringUtils.isBlank(null));
        assertTrue("StringUtils.isBlank should return true for empty string", StringUtils.isBlank(""));
        assertTrue("StringUtils.isBlank should return true for whitespace", StringUtils.isBlank("   "));
        assertFalse("StringUtils.isBlank should return false for non‑empty", StringUtils.isBlank("x"));

        assertFalse("StringUtils.isNotBlank should return false for null", StringUtils.isNotBlank(null));
        assertFalse("StringUtils.isNotBlank should return false for empty string", StringUtils.isNotBlank(""));
        assertFalse("StringUtils.isNotBlank should return false for whitespace", StringUtils.isNotBlank("  "));
        assertTrue("StringUtils.isNotBlank should return true for non‑empty", StringUtils.isNotBlank("abc"));
    }
}
###Test END##