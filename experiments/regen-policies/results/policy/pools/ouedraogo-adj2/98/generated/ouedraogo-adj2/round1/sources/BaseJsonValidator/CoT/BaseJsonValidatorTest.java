package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link BaseJsonValidator}.  The tests use a very small concrete
 * implementation of the abstract class ( {@code TestValidator} ) that simply
 * returns an empty validation result.  The purpose of the tests is to verify
 * the behaviour of the public and protected utility methods that are implemented
 * in {@code BaseJsonValidator} itself.
 */
public class BaseJsonValidatorTest {

    /** a very small concrete validator used only for testing */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // For the purpose of these tests we never need real validation results.
            return Collections.emptySet();
        }
    }

    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private ObjectNode schemaNode;
    private TestValidator validator;
    private ValidationContext dummyContext;

    @Before
    public void setUp() {
        // Minimal objects required by the constructor
        schemaLocation = SchemaLocation.of("http://example.com/schema");
        evaluationPath = new JsonNodePath(PathType.LEGACY);
        schemaNode = JsonNodeFactory.instance.objectNode();
        // a very simple ValidationContext – the constructor arguments are allowed to be null
        // because the tests that use the context (fetchSubSchemaNode, atRoot) are not executed.
        dummyContext = new ValidationContext(null, null, null);

        validator = new TestValidator(schemaLocation,
                evaluationPath,
                schemaNode,
                null,                     // no parent schema
                ValidatorTypeCode.TYPE,    // keyword = "type"
                dummyContext);
    }

    /** -----------------------------------------------------------------------
     *  getKeyword()
     *  ----------------------------------------------------------------------- */
    @Test
    public void testGetKeyword() {
        assertEquals("type", validator.getKeyword());
    }

    /** -----------------------------------------------------------------------
     *  getSchemaNode()
     *  ----------------------------------------------------------------------- */
    @Test
    public void testGetSchemaNode() {
        // the node we passed to the constructor should be returned unchanged
        assertSame(schemaNode, validator.getSchemaNode());
        // add a property and verify that the returned node reflects it
        schemaNode.put("foo", "bar");
        assertEquals("bar", validator.getSchemaNode().get("foo").asText());
    }

    /** -----------------------------------------------------------------------
     *  getParentSchema() – when no parent is supplied the method should return null
     *  ----------------------------------------------------------------------- */
    @Test
    public void testGetParentSchemaWhenNull() {
        assertNull(validator.getParentSchema());
    }

    /** -----------------------------------------------------------------------
     *  getEvaluationParentSchema() – falls back to the lexical parent schema
     *  ----------------------------------------------------------------------- */
    @Test
    public void testGetEvaluationParentSchemaFallsBackToParent() {
        // evaluationParentSchema is not set (null) in the validator created by the public ctor
        // therefore the method must return the lexical parent, which is also null here.
        assertNull(validator.getEvaluationParentSchema());
    }

    /** -----------------------------------------------------------------------
     *  getSchemaLocation()
     *  ----------------------------------------------------------------------- */
    @Test
    public void testGetSchemaLocation() {
        assertSame(schemaLocation, validator.getSchemaLocation());
    }

    /** -----------------------------------------------------------------------
     *  getEvaluationPath()
     *  ----------------------------------------------------------------------- */
    @Test
    public void testGetEvaluationPath() {
        assertSame(evaluationPath, validator.getEvaluationPath());
    }

    /** -----------------------------------------------------------------------
     *  toString() – should return the last segment of the evaluation path
     *  ----------------------------------------------------------------------- */
    @Test
    public void testToStringUsesLastPathSegment() {
        // build a path like /properties/name
        JsonNodePath path = new JsonNodePath(PathType.LEGACY)
                .append("properties")
                .append("name");
        // create a new validator that uses this path
        TestValidator v = new TestValidator(schemaLocation, path, schemaNode, null,
                ValidatorTypeCode.TYPE, dummyContext);
        // the last segment is "name"
        assertEquals("name", v.toString());
    }

    /** -----------------------------------------------------------------------
     *  equals(double,double) – normal values, close values and far values
     *  ----------------------------------------------------------------------- */
    @Test
    public void testEqualsCloseValues() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertTrue(BaseJsonValidator.equals(-5.123456789, -5.123456789001));
    }

    @Test
    public void testEqualsFarValues() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
        assertFalse(BaseJsonValidator.equals(-2.0, 2.0));
    }

    @Test
    public void testEqualsWithNaNAndInfinity() {
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
        assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertTrue(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    /** -----------------------------------------------------------------------
     *  preloadJsonSchemas() – verifies that each supplied schema has its
     *  validators initialized.  The test uses a stub JsonSchema that records
     *  whether {@code initializeValidators()} has been called.
     *  ----------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        // a tiny stub that extends JsonSchema just to record the call
        class StubSchema extends JsonSchema {
            boolean initialized = false;

            StubSchema() {
                super(Collections.emptyList(), false, false, null, null,
                        false, JsonNodeFactory.instance.objectNode(),
                        dummyContext, null, null, null,
                        null, null, null, null, null, null);
            }

            @Override
            public void initializeValidators() {
                initialized = true;
            }
        }

        StubSchema s1 = new StubSchema();
        StubSchema s2 = new StubSchema();

        validator.preloadJsonSchemas(java.util.Arrays.asList(s1, s2));

        assertTrue(s1.initialized);
        assertTrue(s2.initialized);
    }

    /** -----------------------------------------------------------------------
     *  fetchSubSchemaNode() – when {@code suppressSubSchemaRetrieval} is true
     *  the method must return {@code null} without delegating to the private
     *  {@code obtainSubSchemaNode} helper.
     *  ----------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        // create a validator with the flag set to true
        TestValidator suppressed = new TestValidator(schemaLocation,
                evaluationPath,
                schemaNode,
                null,
                ValidatorTypeCode.TYPE,
                dummyContext) {
            {
                // using an instance initializer to set the protected flag
                // (the field is final, therefore we must use a subclass that calls the
                // protected copy‑constructor which allows us to set the flag)
            }
        };
        // Unfortunately the flag is final and can only be set via the protected
        // copy‑constructor.  To keep the test simple we instantiate the validator
        // through that constructor directly.
        suppressed = new TestValidator(
                true,                         // suppressSubSchemaRetrieval
                schemaNode,
                dummyContext,
                null, null, null,
                null, null, null, null, null, null);
        assertNull(suppressed.fetchSubSchemaNode(dummyContext));
    }
}
