package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public instance methods of {@code BaseJsonValidator}</li>
 *   <li>Static utility methods {@code equals} and {@code debug}</li>
 *   <li>Edge‑case handling such as suppressed sub‑schema retrieval and floating‑point tolerance</li>
 * </ul>
 *
 * A lightweight concrete subclass {@code TestValidator} is defined inside the test file
 * because {@code BaseJsonValidator} is abstract. The subclass implements the
 * {@link JsonValidator#validate(ExecutionContext, JsonNode, JsonNode, String)} method
 * with a trivial implementation that always returns an empty {@code Set}.
 *
 * A minimal {@code SimpleJsonSchema} subclass of {@link JsonSchema} is also provided
 * to allow testing of methods that depend on a parent {@code JsonSchema}
 * without requiring the full schema loading infrastructure.
 */
public class BaseJsonValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
    private static final String SCHEMA_PATH = "#/properties/name";

    /** Simple concrete validator used for the tests. */
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

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            // Trivial validator – always succeeds.
            return Collections.emptySet();
        }
    }

    /** Minimal JsonSchema that allows us to observe initialise‑validator side‑effects. */
    private static class SimpleJsonSchema extends JsonSchema {
        private boolean initialized = false;

        SimpleJsonSchema(JsonNode schemaNode) {
            // The JsonSchema constructor that we use does not require a ValidationContext.
            super(null, URI.create("http://example.com"), schemaNode);
        }

        @Override
        public void initializeValidators() {
            // Mark that the method was invoked.
            initialized = true;
        }

        public boolean isInitialized() {
            return initialized;
        }
    }

    private ObjectNode emptySchemaNode;
    private JsonSchema parentSchema;
    private TestValidator validator;

    @Before
    public void setUp() {
        // an empty schema node for most tests
        emptySchemaNode = JsonNodeFactory.instance.objectNode();

        // a parent schema containing a "type" field – needed for getNodeFieldType()
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.put("type", "string");
        parentSchema = new JsonSchema(null, URI.create("http://example.com/parent"), parentNode);

        validator = new TestValidator(SCHEMA_PATH, emptySchemaNode, parentSchema,
                null, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Public accessor methods                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame(emptySchemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        assertSame(parentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- */
    /*  fetchSubSchemaNode – edge case (suppressed retrieval)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator suppressed = new TestValidator(
                SCHEMA_PATH,
                emptySchemaNode,
                parentSchema,
                null,
                null,
                true);                       // suppressSubSchemaRetrieval = true
        assertNull("When suppression is enabled fetchSubSchemaNode must return null",
                suppressed.fetchSubSchemaNode(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Static utility: equals (floating‑point tolerance)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        double a = 0.1 + 0.2;   // 0.30000000000000004
        double b = 0.3;
        assertTrue("Numbers that differ less than 1e‑12 should be considered equal",
                BaseJsonValidator.equals(a, b));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        assertFalse("Numbers that differ more than 1e‑12 should not be equal",
                BaseJsonValidator.equals(1.0, 1.00000000001));
    }

    /* --------------------------------------------------------------------- */
    /*  Static utility: debug – should never throw                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDebugDoesNotThrow() {
        ObjectNode node = JsonNodeFactory.instance.objectNode().put("key", "value");
        // No assertion – we only verify that the call completes without an exception.
        BaseJsonValidator.debug(logger, node, node, "testPath");
    }

    /* --------------------------------------------------------------------- */
    /*  validate(ExecutionContext, JsonNode) – delegation to overridden method */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToOverriddenMethod() {
        ExecutionContext ctx = new ExecutionContext();
        ObjectNode testNode = JsonNodeFactory.instance.objectNode().put("name", "John");
        Set<ValidationMessage> result = validator.validate(ctx, testNode);
        assertTrue("Our trivial validator always returns an empty set", result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  getNodeFieldType – reads the "type" property from parent schema       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldType() {
        // parentSchema was built in setUp() with type = "string"
        assertEquals("string", validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /*  preloadJsonSchemas – ensure initialiseValidators is invoked          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        SimpleJsonSchema s1 = new SimpleJsonSchema(JsonNodeFactory.instance.objectNode());
        SimpleJsonSchema s2 = new SimpleJsonSchema(JsonNodeFactory.instance.objectNode());

        List<JsonSchema> schemas = Arrays.asList(s1, s2);
        validator.preloadJsonSchemas(schemas);

        assertTrue("s1.initializeValidators() should have been called", s1.isInitialized());
        assertTrue("s2.initializeValidators() should have been called", s2.isInitialized());
    }

    /* --------------------------------------------------------------------- */
    /*  Path handling methods                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPathTypeDefaultsToDefault() {
        assertEquals(PathType.DEFAULT, validator.getPathType());
    }

    @Test
    public void testAtRoot() {
        String root = validator.atRoot();
        assertNotNull("Root path must not be null", root);
        // For the DEFAULT PathType the root is usually "$"
        assertEquals(PathType.DEFAULT.getRoot(), root);
    }

    @Test
    public void testAtPathWithStringToken() {
        String root = validator.atRoot();               // e.g. "$"
        String child = "name";
        String fullPath = validator.atPath(root, child);
        // The exact format depends on PathType.DEFAULT – it should contain the child token
        assertTrue("Full path must contain the child token", fullPath.contains(child));
        // The path must start with the root
        assertTrue("Full path must start with the root", fullPath.startsWith(root));
    }

    @Test
    public void testAtPathWithArrayIndex() {
        String root = validator.atRoot();
        int index = 3;
        String fullPath = validator.atPath(root, index);
        // For a JSON‑Pointer style path the index is appended as "/3"
        assertTrue("Full path must contain the array index", fullPath.endsWith("/" + index));
        assertTrue("Full path must start with the root", fullPath.startsWith(root));
    }
}
