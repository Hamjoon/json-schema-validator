package com.networknt.schema;
import com.networknt.schema.ValidationContext.DiscriminatorContext;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and basic getters</li>
 *   <li>Utility methods (equals, atRoot, atPath)</li>
 *   <li>Behaviour of {@code fetchSubSchemaNode} when sub‑schema retrieval is suppressed</li>
 *   <li>Discriminator matching logic (null value, implicit and explicit mappings)</li>
 *   <li>Node‑field‑type extraction from a parent {@link JsonSchema}</li>
 * </ul>
 *
 * The concrete {@code TestValidator} class is a minimal concrete subclass of
 * {@link BaseJsonValidator} that implements the abstract {@code validate(...)} method
 * with a no‑op implementation, allowing us to instantiate the abstract base class.
 */
public class BaseJsonValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A tiny concrete subclass used only for testing the protected members of BaseJsonValidator. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        /** Simple implementation that always returns an empty validation set. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }

        /** Expose the protected {@code fetchSubSchemaNode} for testing. */
        JsonSchema exposeFetchSubSchemaNode(ValidationContext ctx) {
            return super.fetchSubSchemaNode(ctx);
        }

        /** Expose the protected static {@code checkDiscriminatorMatch} for testing. */
        static void exposeCheckDiscriminatorMatch(DiscriminatorContext discriminatorContext,
                                                  ObjectNode discriminator,
                                                  String discriminatorPropertyValue,
                                                  JsonSchema schema) {
            BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                                                      discriminator,
                                                      discriminatorPropertyValue,
                                                      schema);
        }
    }

    private JsonNode simpleSchemaNode;
    private JsonSchema parentSchema;
    private ValidationContext dummyContext; // may stay null – BaseJsonValidator tolerates it

    @Before
    public void setUp() {
        // a minimal schema node: { "type": "string" }
        simpleSchemaNode = MAPPER.createObjectNode().put("type", "string");
        // parent schema – we only need a non‑null instance for the type‑field test.
        parentSchema = new JsonSchema(null, "#/parent", URI.create("http://example.com"), simpleSchemaNode, null);
        dummyContext = null; // we do not need a full context for the current tests
    }

    /* --------------------------------------------------------------------- */
    /*  Constructor / getter tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorAndGetters() {
        String schemaPath = "#/properties/name";
        TestValidator validator = new TestValidator(schemaPath,
                                                    simpleSchemaNode,
                                                    parentSchema,
                                                    ValidatorTypeCode.TYPE,
                                                    dummyContext);

        assertEquals(schemaPath, validator.getSchemaPath());
        assertSame(simpleSchemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
    }

    @Test
    public void testConstructorWithNullValues() {
        // All arguments may legally be null (except validatorType which is used only for messages)
        TestValidator validator = new TestValidator(null,
                                                    null,
                                                    null,
                                                    ValidatorTypeCode.TYPE,
                                                    null);
        assertNull(validator.getSchemaPath());
        assertNull(validator.getSchemaNode());
        assertNull(validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- */
    /*  Utility method tests                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsUtility() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    @Test
    public void testPathGeneration() {
        TestValidator validator = new TestValidator("#", simpleSchemaNode, null,
                                                    ValidatorTypeCode.TYPE, null);
        String root = validator.atRoot();
        assertEquals(validator.getPathType().getRoot(), root);

        String child = validator.atPath(root, "child");
        assertEquals(validator.getPathType().append(root, "child"), child);

        String indexed = validator.atPath(root, 3);
        assertEquals(validator.getPathType().append(root, 3), indexed);
    }

    /* --------------------------------------------------------------------- */
    /*  fetchSubSchemaNode tests (suppressed case)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator validator = new TestValidator("#",
                                                    simpleSchemaNode,
                                                    null,
                                                    ValidatorTypeCode.TYPE,
                                                    null,
                                                    true); // suppressSubSchemaRetrieval = true
        JsonSchema result = validator.exposeFetchSubSchemaNode(null);
        assertNull("When sub‑schema retrieval is suppressed the method must return null", result);
    }

    /* --------------------------------------------------------------------- */
    /*  getNodeFieldType tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldTypeWhenPresent() {
        TestValidator validator = new TestValidator("#",
                                                    simpleSchemaNode,
                                                    parentSchema,
                                                    ValidatorTypeCode.TYPE,
                                                    null);
        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenAbsent() {
        JsonNode schemaWithoutType = MAPPER.createObjectNode(); // empty object
        JsonSchema schema = new JsonSchema(null, "#/no-type", URI.create("http://example.com"),
                                            schemaWithoutType, null);
        TestValidator validator = new TestValidator("#",
                                                    schemaWithoutType,
                                                    schema,
                                                    ValidatorTypeCode.TYPE,
                                                    null);
        assertNull("If parent schema does not contain a 'type' field, null must be returned",
                   validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /*  Discriminator matching tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchWithNullPropertyValue() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // empty discriminator
        JsonSchema dummySchema = new JsonSchema(null, "#/any", URI.create("http://example.com"),
                                                MAPPER.createObjectNode(), null);

        TestValidator.exposeCheckDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        assertTrue("When the discriminator property value is null the context must be marked as a match",
                   ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMapping() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // no explicit mapping
        JsonSchema schema = new JsonSchema(null, "#/components/schemas/Dog",
                                          URI.create("http://example.com"),
                                          MAPPER.createObjectNode(),
                                          null);

        // discriminator property value matches the last segment of the schema path
        TestValidator.exposeCheckDiscriminatorMatch(ctx, discriminator, "Dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMapping() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // build discriminator with explicit mapping: "dog" -> "#/components/schemas/Dog"
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("dog", "#/components/schemas/Dog");
        discriminator.set("mapping", mapping);

        JsonSchema schema = new JsonSchema(null, "#/components/schemas/Dog",
                                          URI.create("http://example.com"),
                                          MAPPER.createObjectNode(),
                                          null);

        TestValidator.exposeCheckDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertTrue("Explicit mapping that matches must set the match flag", ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatch() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // explicit mapping does NOT contain the provided property value
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("cat", "#/components/schemas/Cat");
        discriminator.set("mapping", mapping);

        JsonSchema schema = new JsonSchema(null, "#/components/schemas/Dog",
                                          URI.create("http://example.com"),
                                          MAPPER.createObjectNode(),
                                          null);

        TestValidator.exposeCheckDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertFalse("When no explicit (or implicit) mapping matches, the match flag must stay false",
                    ctx.isDiscriminatorMatchFound());
    }

    /* --------------------------------------------------------------------- */
    /*  validate(ExecutionContext, JsonNode) delegating test                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToFourArgMethod() {
        TestValidator validator = new TestValidator("#",
                                                    simpleSchemaNode,
                                                    null,
                                                    ValidatorTypeCode.TYPE,
                                                    null);
        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = MAPPER.createObjectNode().put("dummy", "value");

        // The four‑argument validate method is overridden to return an empty set,
        // so the two‑argument version must also return an empty set.
        Set<ValidationMessage> result = validator.validate(execCtx, node);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
