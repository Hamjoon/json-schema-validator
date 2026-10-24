package com.networknt.schema;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link JsonSchema}.  The tests are grouped according to the
 * public API of the class and cover:
 * <ul>
 *   <li>basic happy‑path behaviour for each public method</li>
 *   <li>edge‑case handling (missing $ref, invalid id URI, recursive anchor, etc.)</li>
 *   <li>exception scenarios that the implementation is expected to raise</li>
 * </ul>
 *
 * The test suite builds real {@link ValidationContext}, {@link JsonSchema},
 * {@link ExecutionContext} and {@link JsonNode} objects – no mocking is used,
 * as required by the task description.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    /** Helper that creates a minimal {@link ValidationContext} used by all tests. */
    private ValidationContext createValidationContext() throws URISyntaxException {
        // All factories have public no‑arg constructors in the current library version.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // any supported draft
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();      // default factory
        config = new SchemaValidatorsConfig();                           // default configuration
        return new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }

    @Before
    public void setUp() throws Exception {
        validationContext = createValidationContext();
    }

    /* ------------------------------------------------------------
     *  1.  Construction helpers / static factory
     * ------------------------------------------------------------ */
    @Test
    public void testStaticFromFactoryCreatesValidSchema() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/base"),
                schemaNode,
                null,
                false);

        assertNotNull(schema);
        assertEquals("http://example.com/base", schema.getCurrentUri().toString());
        assertEquals("#", schema.getSchemaPath());
        assertEquals(schemaNode, schema.getSchemaNode());
    }

    @Test
    public void testDeprecatedConstructorWorks() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = new JsonSchema(validationContext,
                new URI("http://example.com/base"),
                node);
        assertNotNull(schema);
    }

    /* ------------------------------------------------------------
     *  2.  Basic getters / utility methods
     * ------------------------------------------------------------ */
    @Test
    public void testGetCurrentUriWhenNoIdPresent() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"null\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/schema.json"),
                node,
                null,
                false);
        assertEquals(new URI("http://example.com/schema.json"), schema.getCurrentUri());
    }

    @Test
    public void testToStringFormat() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"boolean\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#/myPath",
                new URI("http://example.com/sch"),
                node,
                null,
                false);
        String expected = "\"#/myPath\" : " + node.toString();
        assertEquals(expected, schema.toString());
    }

    /* ------------------------------------------------------------
     *  3.  $ref handling (getRefSchemaNode)
     * ------------------------------------------------------------ */
    @Test
    public void testGetRefSchemaNodeExistingFragment() throws Exception {
        JsonNode node = MAPPER.readTree("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/schema"),
                node,
                null,
                false);
        JsonNode refNode = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeMissingFragmentReturnsNull() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/schema"),
                node,
                null,
                false);
        JsonNode refNode = schema.getRefSchemaNode("#/nonexistent");
        assertNull(refNode);
    }

    /* ------------------------------------------------------------
     *  4.  Recursive anchor handling (dynamicAnchor)
     * ------------------------------------------------------------ */
    @Test
    public void testRecursiveAnchorSetsDynamicAnchorFlag() throws Exception {
        JsonNode node = MAPPER.readTree("{\"$recursiveAnchor\":true}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/ra"),
                node,
                null,
                false);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testRecursiveAnchorInvalidTypeThrowsException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"$recursiveAnchor\":\"yes\"}");
        try {
            JsonSchema.from(validationContext,
                    "#",
                    new URI("http://example.com/ra"),
                    node,
                    null,
                    false);
            fail("Expected JsonSchemaException because $recursiveAnchor is not boolean");
        } catch (JsonSchemaException e) {
            // the message contains the keyword name – verify it is present
            assertTrue(e.getMessage().contains("$recursiveAnchor"));
        }
    }

    /* ------------------------------------------------------------
     *  5.  Required / Type validator detection
     * ------------------------------------------------------------ */
    @Test
    public void testHasRequiredValidatorTrue() throws Exception {
        JsonNode node = MAPPER.readTree("{\"required\":[\"id\"]}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/req"),
                node,
                null,
                false);
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidatorFalse() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/none"),
                node,
                null,
                false);
        assertFalse(schema.hasRequiredValidator());
        assertNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorTrue() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"array\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/type"),
                node,
                null,
                false);
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidatorFalse() throws Exception {
        JsonNode node = MAPPER.readTree("{\"properties\":{}}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/none"),
                node,
                null,
                false);
        assertFalse(schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    /* ------------------------------------------------------------
     *  6.  Child schema creation
     * ------------------------------------------------------------ */
    @Test
    public void testCreateChildSchemaLinksParentCorrectly() throws Exception {
        JsonNode parentNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema parent = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/parent"),
                parentNode,
                null,
                false);

        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema child = parent.createChildSchema("#/child", childNode);

        assertNotNull(child);
        assertEquals(parent, child.getParentSchema());
        assertEquals("#/child", child.getSchemaPath());
        assertEquals(childNode, child.getSchemaNode());
    }

    /* ------------------------------------------------------------
     *  7.  Ancestor / lexical root navigation
     * ------------------------------------------------------------ */
    @Test
    public void testFindAncestorReturnsTopMostSchema() throws Exception {
        JsonNode rootNode = MAPPER.readTree("{\"id\":\"http://example.com/root\"}");
        JsonSchema root = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema child = root.createChildSchema("#/child", childNode);

        // The child’s ancestor should be the root schema
        assertEquals(root, child.findAncestor());
    }

    @Test
    public void testFindLexicalRootStopsAtSchemaHavingId() throws Exception {
        // Root without id
        JsonNode rootNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema root = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        // Intermediate schema with an explicit id keyword
        JsonNode middleNode = MAPPER.readTree("{\"id\":\"http://example.com/middle\"}");
        JsonSchema middle = root.createChildSchema("#/mid", middleNode);

        // Leaf schema
        JsonNode leafNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema leaf = middle.createChildSchema("#/leaf", leafNode);

        // lexical root from leaf should be the schema that has an id (middle)
        assertEquals(middle, leaf.findLexicalRoot());
    }

    /* ------------------------------------------------------------
     *  8.  Validation (happy path and error path)
     * ------------------------------------------------------------ */
    @Test
    public void testValidateValidDataReturnsEmptySet() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/valid"),
                schemaNode,
                null,
                false);

        JsonNode data = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateInvalidDataReturnsMessages() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"integer\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/invalid"),
                schemaNode,
                null,
                false);

        JsonNode data = MAPPER.readTree("\"not an integer\"");
        Set<ValidationMessage> errors = schema.validate(data);
        assertFalse(errors.isEmpty());

        // At least one message must refer to the type keyword
        boolean containsTypeMessage = errors.stream()
                .anyMatch(m -> m.getCode().contains("type"));
        assertTrue(containsTypeMessage);
    }

    /* ------------------------------------------------------------
     *  9.  Validation with collector (validateAndCollect)
     * ------------------------------------------------------------ */
    @Test
    public void testValidateAndCollectReturnsResultObject() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"boolean\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/collect"),
                schemaNode,
                null,
                false);

        JsonNode data = MAPPER.readTree("true");
        ValidationResult result = schema.validateAndCollect(data);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
        assertNotNull(result.getExecutionContext());
        assertNotNull(result.getCollectorContext());
    }

    /* ------------------------------------------------------------
     * 10.  Walk API (no‑op walk when no listeners are registered)
     * ------------------------------------------------------------ */
    @Test
    public void testWalkReturnsEmptyResultWhenSchemaHasNoWalkListeners() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"null\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/walk"),
                schemaNode,
                null,
                false);

        JsonNode data = MAPPER.readTree("null");
        ValidationResult result = schema.walk(data, false);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalkAtNodeInvokesInternalWalkLogic() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"number\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/walkNode"),
                schemaNode,
                null,
                false);

        JsonNode data = MAPPER.readTree("3.14");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walkAtNode(ctx, data, data, ctx.atRoot(), false);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* ------------------------------------------------------------
     * 11.  initializeValidators – should be idempotent and not throw
     * ------------------------------------------------------------ */
    @Test
    public void testInitializeValidatorsCanBeCalledMultipleTimes() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/init"),
                schemaNode,
                null,
                false);

        // First call – should preload validators
        schema.initializeValidators();

        // Second call – must be a no‑op and must not raise any exception
        schema.initializeValidators();
    }

    /* ------------------------------------------------------------
     * 12.  Invalid schema id handling (exception scenario)
     * ------------------------------------------------------------ */
    @Test
    public void testInvalidIdCausesJsonSchemaException() throws Exception {
        // The id keyword value is not a valid URI – factory.create will throw IllegalArgumentException
        JsonNode schemaNode = MAPPER.readTree("{\"id\":\"ht!tp://[invalid]\"}");
        try {
            JsonSchema.from(validationContext,
                    "#",
                    new URI("http://example.com/bad"),
                    schemaNode,
                    null,
                    false);
            fail("Expected JsonSchemaException because the id is not a valid URI");
        } catch (JsonSchemaException e) {
            // Verify that the exception contains the offending id
            assertTrue(e.getMessage().contains("ht!tp://[invalid]"));
        }
    }

    /* ------------------------------------------------------------
     * 13.  ExecutionContext creation – custom supplier not set
     * ------------------------------------------------------------ */
    @Test
    public void testCreateExecutionContextUsesDefaultConstructor() {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                new URI("http://example.com/exec"),
                node,
                null,
                false);
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }
}
