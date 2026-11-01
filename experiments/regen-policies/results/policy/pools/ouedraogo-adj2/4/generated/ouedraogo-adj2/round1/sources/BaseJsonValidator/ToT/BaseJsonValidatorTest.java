package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Utility static methods (equals, debug)</li>
 *   <li>Discriminator handling (match detection, explicit/implicit mapping, merging, redefinition errors)</li>
 *   <li>Instance behaviour (fetchSubSchemaNode, getNodeFieldType, atRoot, toString)</li>
 * </ul>
 *
 * A minimal concrete subclass {@code TestValidator} is used so that the abstract
 * {@code BaseJsonValidator} can be instantiated without pulling in the full
 * validation engine.
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple concrete implementation of the abstract BaseJsonValidator. */
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

        /** No‑op validator – always returns an empty set of validation messages. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    /** Helper to create a dummy {@link JsonSchema} used in discriminator tests. */
    private static class DummySchema extends BaseJsonValidator {

        DummySchema(SchemaLocation schemaLocation, JsonNode schemaNode) {
            super(schemaLocation,
                  new JsonNodePath(PathType.DEFAULT),
                  schemaNode,
                  null,
                  ValidatorTypeCode.TYPE,
                  null);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    private ValidationContext.DiscriminatorContext discriminatorContext;

    @Before
    public void setUp() {
        discriminatorContext = new ValidationContext.DiscriminatorContext();
    }

    /* -------------------------------------------------------------
     *  Static utility methods
     * ------------------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertTrue(BaseJsonValidator.equals(-0.0000000000005, 0.0));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.0000001));
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN)); // NaN never equals
    }

    @Test
    public void testDebugDoesNotThrow() {
        Logger logger = NOPLogger.NOP_LOGGER; // SLF4J no‑op logger
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().set("root", node);
        JsonNodePath path = new JsonNodePath(PathType.DEFAULT).append("root");
        // Should simply execute without exception
        BaseJsonValidator.debug(logger, node, root, path);
    }

    /* -------------------------------------------------------------
     *  Discriminator matching
     * ------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValueMarksMatch() {
        ObjectNode discriminator = MAPPER.createObjectNode(); // empty discriminator
        DummySchema schema = new DummySchema(SchemaLocation.of("http://example.com#/Test"), null);
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, null, schema);
        assertTrue(discriminatorContext.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        // Build discriminator with explicit mapping
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("myKey", "#/components/schemas/Target");
        ObjectNode discriminator = MAPPER.createObjectNode().set("mapping", mapping);

        // Schema whose fragment matches the mapping value
        SchemaLocation location = SchemaLocation.of("http://example.com#/components/schemas/Target");
        DummySchema schema = new DummySchema(location, null);

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator,
                "myKey",
                schema);
        assertTrue(discriminatorContext.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ImplicitMappingMatch() {
        // No explicit mapping – rely on implicit match (fragment name)
        ObjectNode discriminator = MAPPER.createObjectNode(); // no "mapping"
        // Fragment name is "Implicit"
        SchemaLocation location = SchemaLocation.of("http://example.com#/Implicit");
        DummySchema schema = new DummySchema(location, null);

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator,
                "Implicit",
                schema);
        assertTrue(discriminatorContext.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_NoMatch() {
        // Discriminator with mapping that does NOT match the supplied value
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("otherKey", "#/components/schemas/Other");
        ObjectNode discriminator = MAPPER.createObjectNode().set("mapping", mapping);

        SchemaLocation location = SchemaLocation.of("http://example.com#/components/schemas/Target");
        DummySchema schema = new DummySchema(location, null);

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator,
                "myKey",
                schema);
        assertFalse(discriminatorContext.isDiscriminatorMatchFound());
    }

    /* -------------------------------------------------------------
     *  Discriminator registration & merging
     * ------------------------------------------------------------- */

    @Test
    public void testRegisterAndMergeDiscriminator_MergesMappingsSuccessfully() {
        // Root discriminator already registered with mapping {"A":"#/A"}
        ObjectNode rootMapping = MAPPER.createObjectNode().put("A", "#/A");
        ObjectNode rootDiscriminator = MAPPER.createObjectNode().set("mapping", rootMapping);
        SchemaLocation rootLoc = SchemaLocation.of("http://example.com#/Root");
        discriminatorContext.registerDiscriminator(rootLoc, rootDiscriminator);

        // Child schema introduces a new mapping {"B":"#/B"}
        ObjectNode childMapping = MAPPER.createObjectNode().put("B", "#/B");
        ObjectNode childDiscriminator = MAPPER.createObjectNode().set("mapping", childMapping);
        SchemaLocation childLoc = SchemaLocation.of("http://example.com#/Root/Child");
        DummySchema childSchema = new DummySchema(childLoc, MAPPER.createObjectNode()
                .set("discriminator", childDiscriminator));

        // Perform merge
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator,
                childSchema,
                new JsonNodePath(PathType.DEFAULT));

        // After merge the root discriminator should contain both entries
        ObjectNode merged = discriminatorContext.getDiscriminatorForPath(rootLoc);
        assertNotNull(merged);
        assertTrue(merged.has("mapping"));
        JsonNode mergedMapping = merged.get("mapping");
        assertEquals("#/A", mergedMapping.get("A").asText());
        assertEquals("#/B", mergedMapping.get("B").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_ThrowsOnMappingRedefinition() {
        // Root mapping {"A":"#/A"}
        ObjectNode rootMapping = MAPPER.createObjectNode().put("A", "#/A");
        ObjectNode rootDiscriminator = MAPPER.createObjectNode().set("mapping", rootMapping);
        SchemaLocation rootLoc = SchemaLocation.of("http://example.com#/Root");
        discriminatorContext.registerDiscriminator(rootLoc, rootDiscriminator);

        // Child mapping attempts to redefine "A" with a different value
        ObjectNode childMapping = MAPPER.createObjectNode().put("A", "#/Different");
        ObjectNode childDiscriminator = MAPPER.createObjectNode().set("mapping", childMapping);
        SchemaLocation childLoc = SchemaLocation.of("http://example.com#/Root/Child");
        DummySchema childSchema = new DummySchema(childLoc, MAPPER.createObjectNode()
                .set("discriminator", childDiscriminator));

        // This should raise JsonSchemaException because of redefinition
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator,
                childSchema,
                new JsonNodePath(PathType.DEFAULT));
    }

    /* -------------------------------------------------------------
     *  Instance behaviour
     * ------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNode_SuppressedReturnsNull() {
        // Create a validator with suppressSubSchemaRetrieval = true via the protected constructor
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                null,
                null,
                ValidatorTypeCode.TYPE,
                null) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   JsonNodePath instanceLocation) {
                return Collections.emptySet();
            }

            // expose the protected flag through a subclass method for the test
            @Override
            protected JsonSchema fetchSubSchemaNode(ValidationContext vc) {
                // force the flag to true for this test
                return super.fetchSubSchemaNode(vc);
            }
        };
        // Using reflection to set the private flag (since the constructor we used sets it to false)
        // However the flag is final; we cannot change it. Instead we use the protected constructor
        // that allows us to set it directly.
        // So we instantiate via the protected constructor that accepts the flag:
        TestValidator suppressed = new TestValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                null,
                null,
                ValidatorTypeCode.TYPE,
                null) {
            {
                // using an instance initializer to call the other constructor that sets the flag
            }
        };
        // The public constructor we used does NOT set the flag; therefore we need to call the
        // protected constructor that includes the flag. To keep the test simple we instead
        // instantiate via reflection:
        try {
            java.lang.reflect.Constructor<TestValidator> ctor =
                    TestValidator.class.getDeclaredConstructor(SchemaLocation.class,
                            JsonNodePath.class, JsonNode.class, JsonSchema.class,
                            ValidatorTypeCode.class, ValidationContext.class, boolean.class);
            ctor.setAccessible(true);
            TestValidator v = ctor.newInstance(
                    SchemaLocation.of("http://example.com#/test"),
                    new JsonNodePath(PathType.DEFAULT),
                    null,
                    null,
                    ValidatorTypeCode.TYPE,
                    null,
                    true); // suppressSubSchemaRetrieval = true
            assertNull(v.fetchSubSchemaNode(null));
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testGetNodeFieldType_ReturnsTypeWhenPresent() {
        // Parent schema with a "type" property
        ObjectNode parentSchemaNode = MAPPER.createObjectNode().put("type", "object");
        DummySchema parent = new DummySchema(SchemaLocation.of("http://example.com#/Parent"), parentSchemaNode);

        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                null,
                parent,
                ValidatorTypeCode.TYPE,
                null);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldType_ReturnsNullWhenAbsent() {
        DummySchema parent = new DummySchema(SchemaLocation.of("http://example.com#/Parent"),
                MAPPER.createObjectNode()); // no "type"

        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                null,
                parent,
                ValidatorTypeCode.TYPE,
                null);

        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testAtRootReturnsEmptyPathWithDefaultPathType() {
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                null,
                null,
                ValidatorTypeCode.TYPE,
                null);
        JsonNodePath root = validator.atRoot();
        assertEquals(0, root.getNameCount());
        assertEquals(PathType.DEFAULT, root.getPathType());
    }

    @Test
    public void testToStringReturnsLastSegmentOfEvaluationPath() {
        JsonNodePath path = new JsonNodePath(PathType.DEFAULT)
                .append("first")
                .append("second")
                .append("last");
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com#/test"),
                path,
                null,
                null,
                ValidatorTypeCode.TYPE,
                null);
        assertEquals("last", validator.toString());
    }
}
