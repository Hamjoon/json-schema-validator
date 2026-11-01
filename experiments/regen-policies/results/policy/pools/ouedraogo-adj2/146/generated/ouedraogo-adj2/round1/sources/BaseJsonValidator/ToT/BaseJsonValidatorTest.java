/**
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * <p>The tests focus on the concrete behaviour that can be exercised without
 * requiring the full JSON‑Schema validation engine.  A minimal concrete
 * implementation {@code SimpleValidator} is provided that extends the abstract
 * {@code BaseJsonValidator} and implements the {@code validate(...)} method
 * with a trivial no‑op implementation.</p>
 *
 * <p>Only classes, constructors and methods that appear in the supplied
 * source code (or its listed dependencies) are used – no mocking frameworks
 * or external libraries are involved.</p>
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test class for {@link BaseJsonValidator}.
 */
@RunWith(JUnit4.class)
public class BaseJsonValidatorTest {

    /** Simple concrete validator used for the tests. */
    private static class SimpleValidator extends BaseJsonValidator {

        SimpleValidator(SchemaLocation schemaLocation,
                        JsonNodePath evaluationPath,
                        JsonNode schemaNode,
                        JsonSchema parentSchema,
                        ValidatorTypeCode validatorType,
                        ValidationContext validationContext,
                        boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validatorType, validationContext,
                    suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // No validation – always succeeds.
            return Collections.emptySet();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);

    private JsonNode emptyNode;
    private JsonNode typeNode;
    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;

    @Before
    public void setUp() {
        // basic JSON nodes
        emptyNode = JsonNodeFactory.instance.objectNode();

        ObjectNode typeObject = JsonNodeFactory.instance.objectNode();
        typeObject.put("type", "object");
        typeNode = typeObject;

        // a very small ValidationContext – meta‑schema and factory are not needed for the
        // methods under test (they either ignore it or only read the config).
        validationContext = new ValidationContext(null, null, new SchemaValidatorsConfig());

        // a generic location and path – they are required by the constructor but not used
        // in the tested behaviour.
        schemaLocation = SchemaLocation.DOCUMENT;
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
    }

    /**
     * Test the {@code equals(double,double)} helper with values that are
     * within the tolerance and values that are outside the tolerance.
     */
    @Test
    public void testEqualsHelper() {
        // values that differ less than 1e‑12 must be considered equal
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        // values that differ more than the tolerance must be considered not equal
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
        // also test negative numbers
        assertTrue(BaseJsonValidator.equals(-5.123456789, -5.123456789 + 5e-13));
        assertFalse(BaseJsonValidator.equals(-5.123456789, -5.123456789 + 1e-9));
    }

    /**
     * Verify that {@code debug(...)} does not throw any exception when the
     * logger is not in debug mode.  The method internally checks the execution
     * config; the default {@link ExecutionContext} has debugging disabled, so
     * the code path that builds the debug string is never executed.
     */
    @Test
    public void testDebugDoesNotThrow() {
        ExecutionContext ctx = new ExecutionContext(); // default config, debug disabled
        // The method is static; we simply invoke it and expect no exception.
        BaseJsonValidator.debug(logger, ctx, emptyNode, emptyNode, evaluationPath);
    }

    /**
     * When {@code suppressSubSchemaRetrieval} is true the method must return
     * {@code null} without touching the {@link ValidationContext}.
     */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        SimpleValidator validator = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                emptyNode,
                null,
                ValidatorTypeCode.TYPE,
                validationContext,
                true   // suppressSubSchemaRetrieval
        );
        assertNull("Sub‑schema retrieval should be suppressed", validator.fetchSubSchemaNode(validationContext));
    }

    /**
     * Verify that {@code getKeyword()} returns the keyword value supplied by
     * the {@link ValidatorTypeCode} used during construction.
     */
    @Test
    public void testGetKeyword() {
        SimpleValidator validator = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                emptyNode,
                null,
                ValidatorTypeCode.TYPE,   // the keyword is "type"
                validationContext,
                false
        );
        assertEquals("type", validator.getKeyword());
    }

    /**
     * {@code getNodeFieldType()} walks to the parent schema and extracts the
     * {@code type} field if present.  The test builds a parent validator whose
     * schema node contains {@code "type":"object"} and confirms that the child
     * validator correctly reads it.
     */
    @Test
    public void testGetNodeFieldType() {
        // parent validator with a schema node containing a "type" field
        SimpleValidator parent = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                typeNode,                // {"type":"object"}
                null,
                ValidatorTypeCode.TYPE,
                validationContext,
                false
        );

        // child validator whose parent is the above validator
        SimpleValidator child = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                emptyNode,
                parent,
                ValidatorTypeCode.TYPE,
                validationContext,
                false
        );

        assertEquals("object", child.getNodeFieldType());
    }

    /**
     * The {@code atRoot()} method returns a singleton {@link JsonNodePath}
     * instance that matches the {@link PathType} configured in the
     * {@link ValidationContext}.  The test sets the path type to each of the
     * three supported values and asserts that the returned instance is the
     * expected singleton.
     */
    @Test
    public void testAtRootForAllPathTypes() {
        // JSON_POINTER
        SchemaValidatorsConfig configPointer = new SchemaValidatorsConfig();
        configPointer.setPathType(PathType.JSON_POINTER);
        ValidationContext ctxPointer = new ValidationContext(null, null, configPointer);
        SimpleValidator vPointer = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                emptyNode,
                null,
                ValidatorTypeCode.TYPE,
                ctxPointer,
                false
        );
        assertSame(JsonNodePathJsonPointer.getInstance(), vPointer.atRoot());

        // LEGACY
        SchemaValidatorsConfig configLegacy = new SchemaValidatorsConfig();
        configLegacy.setPathType(PathType.LEGACY);
        ValidationContext ctxLegacy = new ValidationContext(null, null, configLegacy);
        SimpleValidator vLegacy = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                emptyNode,
                null,
                ValidatorTypeCode.TYPE,
                ctxLegacy,
                false
        );
        assertSame(JsonNodePathLegacy.getInstance(), vLegacy.atRoot());

        // JSON_PATH
        SchemaValidatorsConfig configPath = new SchemaValidatorsConfig();
        configPath.setPathType(PathType.JSON_PATH);
        ValidationContext ctxPath = new ValidationContext(null, null, configPath);
        SimpleValidator vPath = new SimpleValidator(
                schemaLocation,
                evaluationPath,
                emptyNode,
                null,
                ValidatorTypeCode.TYPE,
                ctxPath,
                false
        );
        assertSame(JsonNodePathJsonPath.getInstance(), vPath.atRoot());
    }

    /**
     * {@code toString()} should return the last segment of the evaluation
     * path (index {@code -1}).  The test creates a path with several
     * elements and checks the string representation.
     */
    @Test
    public void testToStringUsesEvaluationPath() {
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER)
                .append("properties")
                .append("address")
                .append("street");
        SimpleValidator validator = new SimpleValidator(
                schemaLocation,
                path,
                emptyNode,
                null,
                ValidatorTypeCode.TYPE,
                validationContext,
                false
        );
        // The last element is "street"
        assertEquals("street", validator.toString());
    }
}
