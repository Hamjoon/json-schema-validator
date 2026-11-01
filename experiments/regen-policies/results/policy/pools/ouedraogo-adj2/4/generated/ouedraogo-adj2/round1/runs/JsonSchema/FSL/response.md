###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

/**
 * Unit tests for {@link JsonSchema}. The tests instantiate a {@link JsonSchema}
 * with a minimal {@link ValidationContext} implementation that avoids the heavy
 * validator machinery.  Only the public API that does not depend on the full
 * validation engine is exercised.
 */
public class JsonSchemaTest {

    /**
     * A very small {@link ValidationContext} that returns {@code null} for every
     * {@link #newValidator} call.  This prevents the creation of real validator
     * instances which would require many additional library classes.
     */
    private static class MinimalValidationContext extends ValidationContext {
        public MinimalValidationContext(JsonMetaSchema metaSchema) {
            // The factories and config are not used by the methods we test, therefore we
            // can safely pass {@code null}.
            super(null, null, metaSchema, null, null);
        }

        @Override
        public JsonValidator newValidator(SchemaLocation schemaLocation,
                                          JsonNodePath evaluationPath,
                                          String keyword,
                                          JsonNode schemaNode,
                                          JsonSchema parentSchema) {
            // Returning {@code null} means no validator will be created – the
            // JsonSchema will simply have an empty validator list.
            return null;
        }

        @Override
        public JsonSchema newSchema(SchemaLocation schemaLocation,
                                    JsonNodePath evaluationPath,
                                    JsonNode schemaNode,
                                    JsonSchema parentSchema) {
            // Use the real factory method to create a child schema – it will in turn
            // use this same MinimalValidationContext.
            return JsonSchema.from(this, schemaLocation, evaluationPath, null, schemaNode, parentSchema, false);
        }
    }

    /**
     * Helper method that creates a {@link JsonSchema} instance from a JSON fragment.
     *
     * @param json the JSON representation of the schema
     * @return a {@link JsonSchema} built with the minimal validation context
     * @throws Exception if the JSON cannot be parsed
     */
    private JsonSchema createSchema(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(json);
        MinimalValidationContext ctx = new MinimalValidationContext(JsonMetaSchema.getV4());

        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/schema");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        // No parent schema and no current URI for the top‑level schema.
        return JsonSchema.from(ctx, schemaLocation, rootPath, null, schemaNode, null, false);
    }

    @Test
    public void testGetCurrentUriWhenNoneProvided() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        Assert.assertNull("Current URI should be null when not supplied", schema.getCurrentUri());
    }

    @Test
    public void testIsDynamicAnchorDefault() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        Assert.assertFalse("Dynamic anchor flag should be false by default", schema.isDynamicAnchor());
    }

    @Test
    public void testToStringProducesExpectedFormat() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"number\"}");
        String expectedPrefix = "\"" + schema.getEvaluationPath() + "\" : ";
        String toString = schema.toString();
        Assert.assertTrue("toString should start with the evaluation path", toString.startsWith(expectedPrefix));
        Assert.assertTrue("toString should contain the original schema JSON", toString.contains("\"type\":\"number\""));
    }

    @Test
    public void testFindAncestorReturnsSelfWhenNoParent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}");
        JsonSchema ancestor = schema.findAncestor();
        Assert.assertSame("Ancestor of a root schema should be the schema itself", schema, ancestor);
    }

    @Test
    public void testFindLexicalRootReturnsSelfWhenNoIdKeyword() throws Exception {
        // The schema does not contain an "$id" keyword, therefore the lexical root
        // is the schema itself.
        JsonSchema schema = createSchema("{\"type\":\"null\"}");
        JsonSchema lexicalRoot = schema.findLexicalRoot();
        Assert.assertSame("Lexical root should be the schema itself when no $id is present", schema, lexicalRoot);
    }

    @Test
    public void testHasRequiredValidatorAndHasTypeValidatorWhenAbsent() throws Exception {
        // The schema does not declare "required" or "type" keywords.
        JsonSchema schema = createSchema("{\"properties\":{}}");
        Assert.assertFalse("hasRequiredValidator should be false when no required keyword", schema.hasRequiredValidator());
        Assert.assertFalse("hasTypeValidator should be false when no type keyword", schema.hasTypeValidator());
    }

    @Test
    public void testHasRequiredValidatorAndHasTypeValidatorWhenPresent() throws Exception {
        // The schema contains both "required" and "type".  The minimal validation
        // context returns {@code null} for validators, but the flags are still set
        // during the parsing phase.
        JsonSchema schema = createSchema("{\"type\":\"string\",\"required\":[\"name\"]}");
        // The flags are populated lazily when getValidators() is invoked.
        // Trigger the lazy loading.
        schema.getValidators();

        Assert.assertTrue("hasRequiredValidator should be true when required keyword is present", schema.hasRequiredValidator());
        Assert.assertTrue("hasTypeValidator should be true when type keyword is present", schema.hasTypeValidator());

        // The actual TypeValidator instance is not created because our MinimalValidationContext
        // returns {@code null} for newValidator, therefore getTypeValidator() should still
        // return {@code null}.
        Assert.assertNull("getTypeValidator should return null because no concrete validator was created",
                schema.getTypeValidator());
    }

    @Test
    public void testInitializeValidatorsDoesNotThrowWhenNoValidators() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"integer\"}");
        // No validators are created (newValidator returns null), but initializeValidators
        // must handle the empty list gracefully.
        schema.initializeValidators();
        // After initialization the internal flag should be true.
        // There is no public accessor, but calling it again must not change behaviour.
        schema.initializeValidators(); // second call should be a no‑op
    }

    @Test
    public void testCreateChildSchemaCreatesChildWithParentLink() throws Exception {
        JsonSchema parent = createSchema("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        // The child schema is the definition of the "age" property.
        JsonNodePath childPath = parent.getEvaluationPath().append("properties").append("age");
        SchemaLocation childLocation = parent.getSchemaLocation().append("properties").append("age");
        JsonNode childNode = parent.getSchemaNode().get("properties").get("age");

        JsonSchema child = parent.createChildSchema(childLocation, childNode);
        Assert.assertNotNull("Child schema should not be null", child);
        Assert.assertSame("Child schema's parent should be the original schema", parent, child.getParentSchema());
        Assert.assertEquals("Child schema location should match the supplied location", childLocation, child.getSchemaLocation());
        Assert.assertEquals("Child schema node should match the supplied node", childNode, child.getSchemaNode());
    }

    @Test
    public void testGetValidatorsReturnsEmptyListWhenNoConcreteValidators() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        // Because MinimalValidationContext returns null for newValidator, the schema
        // ends up with an empty validator list.
        Assert.assertTrue("Validator list should be empty", schema.getValidators().isEmpty());
    }

    @Test
    public void testGetCurrentUriWithAbsoluteId() throws Exception {
        // The schema contains an absolute "$id".  The combineCurrentUriWithIds method
        // will attempt to resolve the id against the supplied currentUri (null), which
        // results in the id being used as the new currentUri.
        JsonSchema schema = createSchema("{\"$id\":\"http://example.com/base\",\"type\":\"object\"}");
        // Trigger parsing so that the id handling runs.
        schema.getValidators();

        URI current = schema.getCurrentUri();
        Assert.assertNotNull("Current URI should be set when $id is present", current);
        Assert.assertEquals("http://example.com/base", current.toString());
    }

    @Test
    public void testIsDynamicAnchorWhenRecursiveAnchorTrue() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":true}");
        // The flag is evaluated during parsing.
        schema.getValidators();
        Assert.assertTrue("Dynamic anchor should be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchorWhenRecursiveAnchorFalse() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":false}");
        schema.getValidators();
        Assert.assertFalse("Dynamic anchor should be false when $recursiveAnchor is false", schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testInvalidRecursiveAnchorThrowsException() throws Exception {
        // $recursiveAnchor must be a boolean; using a string should trigger an exception.
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":\"notABoolean\"}");
        // The exception is thrown while the schema is being parsed (read method).
        schema.getValidators();
    }
}
###Test END##