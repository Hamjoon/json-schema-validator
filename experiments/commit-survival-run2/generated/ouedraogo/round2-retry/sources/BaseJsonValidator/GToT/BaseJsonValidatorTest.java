/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (constructors, getters, path helpers, validation delegation, etc.)</li>
 *   <li>Static protected helpers (equals, debug, discriminator handling)</li>
 *   <li>Edge‑case and exception scenarios identified from the source code</li>
 * </ul>
 *
 * The test class uses Mockito to create lightweight stubs for the many
 * dependencies (JsonNode, JsonSchema, ValidationContext, ExecutionContext,
 * DiscriminatorContext, etc.).  A concrete subclass {@code TestValidator}
 * is defined inside the test file to make the abstract class instantiable.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Helper classes & test doubles
     * --------------------------------------------------------------------- */

    /** Minimal {@link PathType} implementation used for path‑related tests. */
    private static class SimplePathType implements PathType {
        @Override public String getRoot() { return "/"; }
        @Override public String append(String current, String token) { return current + "/" + token; }
        @Override public String append(String current, int index) { return current + "[" + index + "]"; }
    }

    /** Concrete subclass of the abstract {@link BaseJsonValidator}. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                     JsonNode schemaNode,
                     JsonSchema parentSchema,
                     ValidatorTypeCode validatorType,
                     ValidationContext validationContext,
                     boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        /** The 4‑argument overload required by {@link BaseJsonValidator#validate(ExecutionContext, JsonNode)}. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            // Simple implementation – just return an empty set.
            return Collections.emptySet();
        }
    }

    /* --------------------------------------------------------------------- *
     *  Mocks used by many tests
     * --------------------------------------------------------------------- */

    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidationContext mockValidationContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private ExecutionContext mockExecutionContext;
    @Mock private Logger mockLogger;
    @Mock private ValidationContext.DiscriminatorContext mockDiscriminatorContext;
    @Mock private JsonSchemaFactory mockJsonSchemaFactory;
    @Mock private URIFactory mockURIFactory;

    private final String SCHEMA_PATH = "/mySchema";
    private final ValidatorTypeCode VALIDATOR_TYPE = ValidatorTypeCode.MAX_LENGTH; // any enum value works

    @Before
    public void setUp() {
        // Default behaviour for the validation context configuration
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(new SimplePathType());
        when(mockValidationContext.getURIFactory()).thenReturn(mockURIFactory);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);
    }

    /* --------------------------------------------------------------------- *
     *  1. Public getters & constructor behaviour
     * --------------------------------------------------------------------- */

    @Test
    public void testConstructorAndGetters() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);

        assertEquals("Schema path getter failed", SCHEMA_PATH, validator.getSchemaPath());
        assertSame("Schema node getter failed", mockSchemaNode, validator.getSchemaNode());
        assertSame("Parent schema getter failed", mockParentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- *
     *  2. fetchSubSchemaNode – suppression flag and missing “id”
     * --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, true);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull("When suppression flag is true fetchSubSchemaNode must return null", result);
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoIdField() {
        // schemaNode without an "id" field -> obtainSubSchemaNode returns null
        when(mockSchemaNode.get("id")).thenReturn(null);

        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull("When schemaNode has no \"id\" field fetchSubSchemaNode must return null", result);
    }

    /* --------------------------------------------------------------------- *
     *  3. validate(ExecutionContext, JsonNode) delegating to the 4‑arg overload
     * --------------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToFourArgMethod() {
        TestValidator validator = spy(new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false));

        JsonNode instanceNode = mock(JsonNode.class);
        Set<ValidationMessage> expected = Collections.singleton(new ValidationMessage("test", "msg"));
        // Force the 4‑arg overload to return a known set
        doReturn(expected).when(validator).validate(eq(mockExecutionContext), eq(instanceNode), eq(instanceNode), anyString());

        Set<ValidationMessage> actual = validator.validate(mockExecutionContext, instanceNode);
        assertSame("validate() should delegate to the 4‑arg method", expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  4. getNodeFieldType – handling of missing/empty type field
     * --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldTypeWhenPresent() {
        JsonNode typeNode = mock(JsonNode.class);
        when(typeNode.asText()).thenReturn("string");
        when(mockParentSchema.getSchemaNode()).thenReturn(mockSchemaNode);
        when(mockSchemaNode.get("type")).thenReturn(typeNode);

        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);

        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenAbsent() {
        when(mockParentSchema.getSchemaNode()).thenReturn(mockSchemaNode);
        when(mockSchemaNode.get("type")).thenReturn(null);

        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);

        assertNull("Missing type field should result in null", validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- *
     *  5. preloadJsonSchemas – ensure initializeValidators() is invoked
     * --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemasCallsInitialize() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(schemaA, schemaB);

        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);
        validator.preloadJsonSchemas(list);

        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }

    /* --------------------------------------------------------------------- *
     *  6. Path helpers (atRoot, atPath)
     * --------------------------------------------------------------------- */

    @Test
    public void testPathHelpers() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);

        assertEquals("/", validator.atRoot());

        String childPath = validator.atPath("/", "child");
        assertEquals("/child", childPath);

        String indexedPath = validator.atPath("/", 3);
        assertEquals("/[3]", indexedPath);
    }

    /* --------------------------------------------------------------------- *
     *  7. Static protected helpers – equals
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsNearThreshold() {
        assertTrue("Numbers within 1e‑12 should be equal",
                BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertFalse("Numbers further apart should not be equal",
                BaseJsonValidator.equals(1.0, 1.0000000001));
    }

    /* --------------------------------------------------------------------- *
     *  8. Static protected helpers – debug (ensures no exception)
     * --------------------------------------------------------------------- */

    @Test
    public void testDebugDoesNotThrow() {
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        BaseJsonValidator.debug(mockLogger, node, root, "somePath");
        verify(mockLogger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("somePath"));
    }

    /* --------------------------------------------------------------------- *
     *  9. Discriminator handling – various branches
     * --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNull() {
        // When discriminatorPropertyValue is null the context must be marked as a match.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext, null, null, null);
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFound() {
        ObjectNode discriminator = mock(ObjectNode.class);
        ObjectNode mapping = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(mapping);
        // mapping: {"cat":"#/definitions/cat"}
        JsonNode mappingValue = mock(JsonNode.class);
        when(mappingValue.asText()).thenReturn("#/definitions/cat");
        when(mapping.fields()).thenReturn(Collections.singletonMap("cat", mappingValue).entrySet().iterator());

        JsonSchema schema = mock(JsonSchema.class);
        // schema path matches mapping value
        when(schema.schemaPath).thenReturn("#/definitions/cat");

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "cat", schema);

        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingFound() {
        ObjectNode discriminator = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(null);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/pets/cat"); // ends with "cat"

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "cat", schema);

        verify(mockDiscriminatorContext).markMatch();
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsOnPropertyNameRedefinition() {
        // Build a root discriminator already present in the context
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("propertyName")).thenReturn(null); // root has no propertyName
        context.registerDiscriminator("/root", rootDiscriminator);

        // Schema that attempts to add a new propertyName
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/root/child");
        ObjectNode schemaNode = mock(ObjectNode.class);
        when(schema.schemaNode).thenReturn(schemaNode);
        ObjectNode discriminatorOnSchema = mock(ObjectNode.class);
        when(schemaNode.get("discriminator")).thenReturn(discriminatorOnSchema);
        when(discriminatorOnSchema.get("propertyName")).thenReturn(mock(JsonNode.class)); // non‑null

        // The method should throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(context, rootDiscriminator, schema, "at ");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsWithoutOverride() {
        // Context already holds a root discriminator with a mapping {"dog":"#/dog"}
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        ObjectNode rootMapping = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(rootMapping);
        when(rootMapping.fields()).thenReturn(
                Collections.singletonMap("dog", mock(JsonNode.class)).entrySet().iterator());

        // Register root discriminator
        context.registerDiscriminator("/root", rootDiscriminator);

        // Schema adds a new mapping {"cat":"#/cat"} but does not clash
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/root/child");
        ObjectNode schemaNode = mock(ObjectNode.class);
        when(schema.schemaNode).thenReturn(schemaNode);
        ObjectNode discriminatorOnSchema = mock(ObjectNode.class);
        when(schemaNode.get("discriminator")).thenReturn(discriminatorOnSchema);
        when(discriminatorOnSchema.get("propertyName")).thenReturn(null);
        ObjectNode schemaMapping = mock(ObjectNode.class);
        when(discriminatorOnSchema.get("mapping")).thenReturn(schemaMapping);

        JsonNode catMappingValue = mock(JsonNode.class);
        when(catMappingValue.asText()).thenReturn("#/cat");
        when(schemaMapping.fields()).thenReturn(
                Collections.singletonMap("cat", catMappingValue).entrySet().iterator());

        // Perform merge
        BaseJsonValidator.registerAndMergeDiscriminator(context, rootDiscriminator, schema, "at ");

        // Verify that the new entry was added to the root mapping
        verify(rootMapping).set(eq("cat"), eq(catMappingValue));
        // No exception should be thrown
    }

    /* --------------------------------------------------------------------- *
     *  10. Helper for noExplicitDiscriminatorKeyOverride
     * --------------------------------------------------------------------- */

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsFalseWhenOverrideExists() {
        JsonNode mapping = mock(JsonNode.class);
        JsonNode entry = mock(JsonNode.class);
        when(entry.asText()).thenReturn("/parent/path");
        when(mapping.fields()).thenReturn(
                Collections.singletonMap("key", entry).entrySet().iterator());

        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("/parent/path");

        boolean result = BaseJsonValidator.noExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertFalse("Override should be detected", result);
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsTrueWhenNoOverride() {
        JsonNode mapping = mock(JsonNode.class);
        JsonNode entry = mock(JsonNode.class);
        when(entry.asText()).thenReturn("/some/other/path");
        when(mapping.fields()).thenReturn(
                Collections.singletonMap("key", entry).entrySet().iterator());

        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("/parent/path");

        boolean result = BaseJsonValidator.noExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertTrue("No override should return true", result);
    }

    /* --------------------------------------------------------------------- *
     *  11. getPathType exposure – ensure the constructor respects the config
     * --------------------------------------------------------------------- */

    @Test
    public void testGetPathTypeReturnsConfiguredInstance() {
        SimplePathType customPathType = new SimplePathType();
        when(mockConfig.getPathType()).thenReturn(customPathType);

        TestValidator validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, VALIDATOR_TYPE, mockValidationContext, false);

        assertSame(customPathType, validator.getPathType());
    }
}
