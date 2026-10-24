package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

public class BaseJsonValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
    private ObjectMapper mapper;
    private ValidationContext validationContext;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        // ValidationContext can be constructed with null components for the purpose of these unit tests.
        validationContext = new ValidationContext(null, null, null, null, null);
    }

    /**
     * Minimal concrete implementation of the abstract BaseJsonValidator.
     */
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.TRUE, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    /**
     * Subclass of JsonSchema that records whether {@code initializeValidators()} was invoked.
     */
    private static class TestJsonSchema extends JsonSchema {

        boolean initialized = false;

        public TestJsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode) {
            super(validationContext, baseUri, schemaNode);
        }

        @Override
        public void initializeValidators() {
            initialized = true;
        }
    }

    @Test
    public void testEqualsTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertFalse(BaseJsonValidator.equals(1.0001, 1.0));
    }

    @Test
    public void testDebugDoesNotThrow() {
        JsonNode node = mapper.createObjectNode().put("key", "value");
        JsonNode root = mapper.createObjectNode();
        // Should simply log without throwing any exception.
        BaseJsonValidator.debug(logger, node, root, "$");
    }

    @Test
    public void testGetNodeFieldType() {
        // parent schema containing a "type" field
        JsonNode parentSchemaNode = mapper.createObjectNode().put("type", "object");
        JsonSchema parentSchema = new JsonSchema(validationContext, URI.create("urn:test"), parentSchemaNode);
        JsonNode childSchemaNode = mapper.createObjectNode();
        DummyValidator validator = new DummyValidator("#/properties/name", childSchemaNode, parentSchema, validationContext);
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemas() {
        JsonNode dummyNode = mapper.createObjectNode();
        TestJsonSchema schema1 = new TestJsonSchema(validationContext, URI.create("urn:test1"), dummyNode);
        TestJsonSchema schema2 = new TestJsonSchema(validationContext, URI.create("urn:test2"), dummyNode);

        DummyValidator validator = new DummyValidator("#", dummyNode, null, validationContext);
        validator.preloadJsonSchemas(Arrays.asList(schema1, schema2));

        assertTrue(schema1.initialized);
        assertTrue(schema2.initialized);
    }

    @Test
    public void testPathMethods() {
        JsonNode dummyNode = mapper.createObjectNode();
        DummyValidator validator = new DummyValidator("#", dummyNode, null, validationContext);

        // Default PathType is PathType.DEFAULT
        assertEquals(PathType.DEFAULT, validator.getPathType());

        // atRoot should delegate to PathType.DEFAULT.getRoot()
        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());

        // atPath with String token
        String childPath = validator.atPath("$", "name");
        assertEquals(PathType.DEFAULT.append("$", "name"), childPath);

        // atPath with index
        String indexedPath = validator.atPath("$", 2);
        assertEquals(PathType.DEFAULT.append("$", 2), indexedPath);
    }

    @Test
    public void testCheckDiscriminatorMatchNullValue() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = mapper.createObjectNode(); // empty discriminator
        JsonSchema dummySchema = new JsonSchema(validationContext, URI.create("urn:test"), mapper.createObjectNode());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicit() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = mapper.createObjectNode(); // no "mapping"
        // schemaPath ends with "/myType"
        JsonSchema schema = new JsonSchema(validationContext, "/root/myType", URI.create("urn:test"), mapper.createObjectNode(), null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "myType", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicit() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Build explicit mapping: "typeA" -> "/schemas/typeA"
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("typeA", "/schemas/typeA");
        ObjectNode discriminator = mapper.createObjectNode();
        discriminator.set("mapping", mapping);

        JsonSchema schema = new JsonSchema(validationContext, "/schemas/typeA", URI.create("urn:test"), mapper.createObjectNode(), null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "typeA", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchNoMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // No mapping and schemaPath does not end with the discriminator value
        ObjectNode discriminator = mapper.createObjectNode();
        JsonSchema schema = new JsonSchema(validationContext, "/schemas/other", URI.create("urn:test"), mapper.createObjectNode(), null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "typeB", schema);
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsOnPropertyNameRedefinition() {
        // Root discriminator (already registered)
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ctx.registerDiscriminator("/test", rootDiscriminator);

        // Schema discriminator that tries to redefine "propertyName"
        ObjectNode schemaDiscriminator = mapper.createObjectNode();
        schemaDiscriminator.set("propertyName", mapper.getNodeFactory().textNode("type"));
        JsonNode schemaNode = mapper.createObjectNode().set("discriminator", schemaDiscriminator);
        JsonSchema schema = new JsonSchema(validationContext, "/test", URI.create("urn:test"), schemaNode, null);

        // This call must throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "/test");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappings() {
        // Root discriminator with a mapping entry "a" -> "#/def/a"
        ObjectNode rootMapping = mapper.createObjectNode();
        rootMapping.put("a", "#/def/a");
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        rootDiscriminator.set("mapping", rootMapping);

        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ctx.registerDiscriminator("/test", rootDiscriminator);

        // Schema discriminator with a different mapping entry "b" -> "#/def/b"
        ObjectNode schemaMapping = mapper.createObjectNode();
        schemaMapping.put("b", "#/def/b");
        ObjectNode schemaDiscriminator = mapper.createObjectNode();
        schemaDiscriminator.set("mapping", schemaMapping);

        JsonNode schemaNode = mapper.createObjectNode().set("discriminator", schemaDiscriminator);
        JsonSchema schema = new JsonSchema(validationContext, "/test", URI.create("urn:test"), schemaNode, null);

        // Merge should happen without exception
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "/test");

        // Verify that both mappings are present
        ObjectNode merged = ctx.getDiscriminatorForPath("/test");
        assertNotNull(merged);
        JsonNode mergedMapping = merged.get("mapping");
        assertNotNull(mergedMapping);
        assertEquals("#/def/a", mergedMapping.get("a").asText());
        assertEquals("#/def/b", mergedMapping.get("b").asText());
    }
}
