###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public instance methods (including inherited ones) and their basic functionality.</li>
 *   <li>Static helper methods (equals, debug, discriminator handling).</li>
 *   <li>Edge cases and exception‑free error handling such as null inputs,
 *       empty schema nodes, and tolerance limits for double comparison.</li>
 * </ul>
 *
 * <p>Because {@code BaseJsonValidator} is abstract a minimal concrete subclass
 * {@code TestValidator} is defined inside the test file.  It supplies a trivial
 * implementation of {@code validate(...)} required by the abstract class.
 *
 * <p>No mocking frameworks are used – all objects are created through the
 * public constructors supplied by the library.
 */
public class BaseJsonValidatorTest {

    /** Simple concrete subclass used for testing the abstract class. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            // No real validation – return an empty set.
            return Collections.emptySet();
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private JsonNode schemaNode;
    private TestValidator validator;

    @Before
    public void setUp() {
        // Simple schema location and evaluation path.
        schemaLocation = SchemaLocation.of("http://example.com/schema");
        evaluationPath = new JsonNodePath(PathType.DEFAULT);
        // Minimal schema node – an empty JSON object.
        schemaNode = MAPPER.createObjectNode();

        // ValidationContext is optional – we pass null to keep the test lightweight.
        validator = new TestValidator(schemaLocation,
                                      evaluationPath,
                                      schemaNode,
                                      null,                     // parentSchema = null
                                      ValidatorTypeCode.TRUE,   // any validator code works
                                      null);                    // validationContext = null
    }

    /* --------------------------------------------------------------------- *
     *  Public instance methods (basic functionality)
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaLocation() {
        assertEquals("SchemaLocation should be the one supplied to the constructor",
                     schemaLocation, validator.getSchemaLocation());
    }

    @Test
    public void testGetEvaluationPath() {
        assertEquals("EvaluationPath should be the one supplied to the constructor",
                     evaluationPath, validator.getEvaluationPath());
    }

    @Test
    public void testGetKeyword() {
        // ValidatorTypeCode.TRUE implements Keyword – its value is "true".
        assertEquals("Keyword should be the value of the supplied ValidatorTypeCode",
                     "true", validator.getKeyword());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame("SchemaNode getter must return the exact instance given at construction",
                   schemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        assertNull("Parent schema is null because we passed null to the constructor", validator.getParentSchema());
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoId() {
        // The schema node does not contain an "id" property – fetchSubSchemaNode must return null.
        assertNull("fetchSubSchemaNode should return null when there is no $id", validator.fetchSubSchemaNode(null));
    }

    @Test
    public void testValidateConvenienceMethod() {
        // The convenience overload should delegate correctly and return an empty set.
        ExecutionContext ctx = new ExecutionContext();
        JsonNode data = MAPPER.createObjectNode(); // empty data node
        Set<ValidationMessage> messages = validator.validate(ctx, data);
        assertTrue("Validator returns an empty set for the no‑op implementation", messages.isEmpty());
    }

    @Test
    public void testValidateWithFormat() {
        // Simple format that just returns the size of the validation messages set.
        ExecutionContext ctx = new ExecutionContext();
        JsonNode data = MAPPER.createObjectNode();

        OutputFormat<Integer> format = new OutputFormat<Integer>() {
            @Override
            public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
                // nothing to customise for this test
            }

            @Override
            public Integer format(Set<ValidationMessage> messages,
                                  ExecutionContext executionContext,
                                  ValidationContext validationContext) {
                return messages.size(); // should be 0 for our no‑op validator
            }
        };

        Integer result = validator.validate(ctx, data, format);
        assertEquals("Format should receive the empty message set", Integer.valueOf(0), result);
    }

    @Test
    public void testValidateWithFormatAndCustomizer() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode data = MAPPER.createObjectNode();

        OutputFormat<String> format = new OutputFormat<String>() {
            @Override
            public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
                // no customisation needed
            }

            @Override
            public String format(Set<ValidationMessage> messages,
                                 ExecutionContext executionContext,
                                 ValidationContext validationContext) {
                return messages.isEmpty() ? "OK" : "FAIL";
            }
        };

        ExecutionCustomizer customizer = new ExecutionCustomizer() {
            @Override
            public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
                // No operation – just verify that the method is called without error.
            }
        };

        String result = validator.validate(ctx, data, format, customizer);
        assertEquals("Result should be OK because the validator produces no messages", "OK", result);
    }

    @Test
    public void testAtRootCreatesPathWithDefaultType() {
        JsonNodePath root = validator.atRoot();
        assertNotNull("Root path must not be null", root);
        // The root path should have zero name elements.
        assertEquals("Root path should have no elements", 0, root.getNameCount());
    }

    @Test
    public void testToStringUsesEvaluationPath() {
        // The default JsonNodePath with no elements returns null for getName(-1).
        // In this case BaseJsonValidator.toString() should also return null.
        assertNull("toString() should return null when evaluationPath has no name at -1", validator.toString());

        // Create a validator with a non‑empty path and verify the string representation.
        JsonNodePath nonEmptyPath = new JsonNodePath(PathType.DEFAULT).append("root").append("child");
        TestValidator v = new TestValidator(schemaLocation,
                                            nonEmptyPath,
                                            schemaNode,
                                            null,
                                            ValidatorTypeCode.TRUE,
                                            null);
        assertEquals("toString() must return the last segment of the evaluation path",
                     "child", v.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Static helper methods
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        double a = 1.000000000001;
        double b = 1.000000000002;
        assertTrue("Numbers within 1e‑12 tolerance should be considered equal", BaseJsonValidator.equals(a, b));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        double a = 1.0;
        double b = 1.0 + 1e-10; // larger than 1e‑12
        assertFalse("Numbers outside the tolerance must not be equal", BaseJsonValidator.equals(a, b));
    }

    @Test
    public void testDebugDoesNotThrow() {
        Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        JsonNode node = MAPPER.createObjectNode();
        JsonNode root = MAPPER.createObjectNode();
        JsonNodePath path = new JsonNodePath(PathType.DEFAULT);
        // The method only logs – verify that it does not raise an exception.
        BaseJsonValidator.debug(logger, node, root, path);
    }

    /* --------------------------------------------------------------------- *
     *  Discriminator handling – edge cases and normal flow
     * --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNull() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode disc = JsonNodeFactory.instance.objectNode(); // empty discriminator
        BaseJsonValidator.checkDiscriminatorMatch(ctx, disc, null, null);
        assertTrue("When property value is null the context must be marked as a match",
                   ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchWithExplicitMappingMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Build discriminator JSON: { "mapping": { "dog": "#/components/schemas/Dog" } }
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode()
                .set("mapping", mapping);

        // Create a dummy JsonSchema whose location fragment equals "Dog"
        SchemaLocation schemaLoc = SchemaLocation.of("http://example.com#/components/schemas/Dog");
        JsonNodePath dummyPath = new JsonNodePath(PathType.DEFAULT);
        JsonNode dummySchemaNode = JsonNodeFactory.instance.objectNode();
        TestValidator dummySchema = new TestValidator(schemaLoc, dummyPath, dummySchemaNode,
                                                      null, ValidatorTypeCode.TRUE, null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", dummySchema);
        assertTrue("Explicit mapping that matches should set match flag", ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchWithExplicitMappingNoMatchAndImplicitMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Explicit mapping does NOT contain the value "cat"
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode()
                .set("mapping", mapping);

        // Dummy schema with fragment "cat"
        SchemaLocation schemaLoc = SchemaLocation.of("http://example.com#/components/schemas/cat");
        JsonNodePath dummyPath = new JsonNodePath(PathType.DEFAULT);
        JsonNode dummySchemaNode = JsonNodeFactory.instance.objectNode();
        TestValidator dummySchema = new TestValidator(schemaLoc, dummyPath, dummySchemaNode,
                                                      null, ValidatorTypeCode.TRUE, null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", dummySchema);
        // No explicit match, but implicit match (fragment equals value) should set the flag.
        assertTrue("Implicit match should be used when explicit mapping does not match",
                   ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchWithNoMappingAndImplicitMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Discriminator without a "mapping" field.
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();

        // Schema fragment equals "bird"
        SchemaLocation schemaLoc = SchemaLocation.of("http://example.com#/components/schemas/bird");
        JsonNodePath dummyPath = new JsonNodePath(PathType.DEFAULT);
        JsonNode dummySchemaNode = JsonNodeFactory.instance.objectNode();
        TestValidator dummySchema = new TestValidator(schemaLoc, dummyPath, dummySchemaNode,
                                                      null, ValidatorTypeCode.TRUE, null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "bird", dummySchema);
        assertTrue("Implicit match should be detected when mapping is absent", ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideTrueWhenNoOverride() {
        // Mapping that does NOT reference the parent schema fragment.
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("dog", "#/components/schemas/Dog");
        // Parent schema fragment is "Cat"
        SchemaLocation parentLoc = SchemaLocation.of("http://example.com#/components/schemas/Cat");
        JsonNodePath dummyPath = new JsonNodePath(PathType.DEFAULT);
        JsonNode dummyNode = JsonNodeFactory.instance.objectNode();
        TestValidator parentSchema = new TestValidator(parentLoc, dummyPath, dummyNode,
                                                      null, ValidatorTypeCode.TRUE, null);

        boolean result = invokeNoExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertTrue("Should return true when no explicit key overrides the parent fragment", result);
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideFalseWhenOverrideExists() {
        // Mapping that references the parent schema fragment ("Cat").
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("cat", "#/components/schemas/Cat");
        // Parent schema fragment is "Cat"
        SchemaLocation parentLoc = SchemaLocation.of("http://example.com#/components/schemas/Cat");
        JsonNodePath dummyPath = new JsonNodePath(PathType.DEFAULT);
        JsonNode dummyNode = JsonNodeFactory.instance.objectNode();
        TestValidator parentSchema = new TestValidator(parentLoc, dummyPath, dummyNode,
                                                      null, ValidatorTypeCode.TRUE, null);

        boolean result = invokeNoExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertFalse("Should return false when an explicit mapping overrides the parent fragment", result);
    }

    /**
     * Helper that uses reflection to invoke the private static method
     * {@code noExplicitDiscriminatorKeyOverride} – required because the method
     * is private but we still need to test its logic.
     */
    private static boolean invokeNoExplicitDiscriminatorKeyOverride(JsonNode mapping,
                                                                     JsonSchema parentSchema) {
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("noExplicitDiscriminatorKeyOverride", JsonNode.class, JsonSchema.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(null, mapping, parentSchema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Edge cases for fetchSubSchemaNode (invalid $id handling)
     * --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeWithInvalidId() {
        // Create a schema node with an "id" that is not a valid URI.
        ObjectNode schemaWithBadId = JsonNodeFactory.instance.objectNode()
                .put("id", "not a valid uri");

        // Use a ValidationContext that supplies a real URIFactory – we can reuse
        // the default factories from the library via a minimal config.
        // For the purpose of this test we pass null for validationContext,
        // because obtainSubSchemaNode catches IllegalArgumentException and returns null.
        TestValidator v = new TestValidator(schemaLocation,
                                            evaluationPath,
                                            schemaWithBadId,
                                            null,
                                            ValidatorTypeCode.TRUE,
                                            null);
        assertNull("fetchSubSchemaNode must return null when the $id is not a valid URI",
                   v.fetchSubSchemaNode(null));
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressFlagIsTrue() {
        // Build a validator with the suppressSubSchemaRetrieval flag forced to true.
        // This is achieved by using the protected constructor that accepts the flag.
        ValidationContext dummyContext = null; // not needed for this test
        BaseJsonValidator suppressingValidator = new BaseJsonValidator(schemaLocation,
                                                                       evaluationPath,
                                                                       schemaNode,
                                                                       null,
                                                                       ValidatorTypeCode.TRUE,
                                                                       ValidatorTypeCode.TRUE,
                                                                       dummyContext,
                                                                       true) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                    JsonNode node,
                                                    JsonNode rootNode,
                                                    JsonNodePath instanceLocation) {
                return Collections.emptySet();
            }
        };
        assertNull("When suppressSubSchemaRetrieval is true, fetchSubSchemaNode must return null",
                   suppressingValidator.fetchSubSchemaNode(null));
    }
}
```
###Test END##