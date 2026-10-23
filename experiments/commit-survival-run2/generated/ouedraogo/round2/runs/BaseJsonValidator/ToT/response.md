###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>Because {@code BaseJsonValidator} is abstract we provide a tiny concrete
 * implementation ( {@code TestValidator} ) that simply returns an empty set of
 * {@link ValidationMessage}s.  All other methods are exercised directly or via
 * reflection where necessary.</p>
 *
 * <p>The tests cover:</p>
 * <ul>
 *   <li>Floating‑point equality helper</li>
 *   <li>Debug logging</li>
 *   <li>Discriminator handling (explicit, implicit, missing mapping, property‑name
 *       redefinition)</li>
 *   <li>Sub‑schema retrieval suppression</li>
 *   <li>Accessor methods</li>
 *   <li>Path construction helpers</li>
 *   <li>Pre‑loading of schemas</li>
 * </ul>
 *
 * <p>All external collaborators are mocked with Mockito so the test suite does
 * not depend on the rest of the library implementation.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /* --------------------------------------------------------------------- */
    /* Helper concrete implementation of the abstract class under test       */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType,
                    validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node) {
            // For the purpose of the unit tests we simply return an empty set.
            return Collections.emptySet();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Common test fixtures                                                 */
    /* --------------------------------------------------------------------- */
    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private ValidationContext.Config mockConfig;

    @Mock
    private URIFactory mockUriFactory;

    @Mock
    private JsonSchemaFactory mockJsonSchemaFactory;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private JsonSchema mockChildSchema;

    @Mock
    private ValidationContext.DiscriminatorContext mockDiscriminatorContext;

    @Mock
    private Logger mockLogger;

    private ObjectNode emptySchemaNode;
    private TestValidator validator;

    @Before
    public void setUp() {
        // Basic empty schema node
        emptySchemaNode = JsonNodeFactory.instance.objectNode();

        // Default configuration – fail‑fast disabled, default path type, etc.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getResourceBundle()).thenReturn(I18nSupport.DEFAULT_RESOURCE_BUNDLE);

        // Stub URI factory – used only for sub‑schema retrieval tests
        when(mockValidationContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);

        // Build the validator instance (no suppression of sub‑schema retrieval)
        validator = new TestValidator("#", emptySchemaNode, mockParentSchema,
                ValidatorTypeCode.UNKNOWN, mockValidationContext, false);
    }

    /* --------------------------------------------------------------------- */
    /* 1. equals(double,double) – floating point tolerance                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEquals_ToleratesFloatingPointErrors() {
        // 0.1 + 0.2 is not exactly 0.3 in binary floating point,
        // but the helper should treat them as equal.
        assertTrue(BaseJsonValidator.equals(0.1 + 0.2, 0.3));

        // Values that differ more than the tolerance must be reported as different.
        assertFalse(BaseJsonValidator.equals(0.1, 0.100000000001));
    }

    /* --------------------------------------------------------------------- */
    /* 2. debug(Logger, JsonNode, JsonNode, String) – logger interaction     */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDebug_LogsCorrectMessage() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("a", 1);
        JsonNode root = JsonNodeFactory.instance.objectNode().put("root", true);
        String at = "myPath";

        BaseJsonValidator.debug(mockLogger, node, root, at);

        // Verify that the logger received exactly one call with the expected pattern.
        verify(mockLogger, times(1)).debug(eq("validate( {}, {}, {})"),
                eq(node), eq(root), eq(at));
    }

    /* --------------------------------------------------------------------- */
    /* 3. checkDiscriminatorMatch – null property value marks a match       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValue_MarksMatch() {
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode(); // empty – mapping not required

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, null, mockChildSchema);

        verify(mockDiscriminatorContext, times(1)).markMatch();
    }

    /* --------------------------------------------------------------------- */
    /* 4. checkDiscriminatorMatch – explicit mapping found                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingFound() {
        // Build discriminator with an explicit mapping: "cat" -> "/schemas/cat"
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("cat", "/schemas/cat");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode()
                .set("mapping", mapping);

        // Child schema whose path matches the mapping value
        when(mockChildSchema.getSchemaPath()).thenReturn("/schemas/cat");

        // The context reports that no match has been found yet.
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "cat", mockChildSchema);

        // The explicit mapping should cause a match.
        verify(mockDiscriminatorContext, times(1)).markMatch();
    }

    /* --------------------------------------------------------------------- */
    /* 5. checkDiscriminatorMatch – explicit missing, implicit matches       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatch_ImplicitMatchWhenExplicitMissing() {
        // Discriminator has a mapping, but it does NOT contain the property value.
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("dog", "/schemas/dog");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode()
                .set("mapping", mapping);

        // Child schema path ends with the property value – implicit match.
        when(mockChildSchema.getSchemaPath()).thenReturn("/schemas/cat");

        // No match yet.
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        // Simulate that the explicit mapping does NOT match the child schema.
        // The method will then fall back to implicit matching.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "cat", mockChildSchema);

        // Implicit match should be recorded.
        verify(mockDiscriminatorContext, times(1)).markMatch();
    }

    /* --------------------------------------------------------------------- */
    /* 6. registerAndMergeDiscriminator – simple registration                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testRegisterAndMergeDiscriminator_SimpleRegistration() {
        // Root discriminator (no existing discriminator for the schema path)
        when(mockDiscriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(null);

        // Schema without its own "discriminator" node – the merge logic should be a no‑op.
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("#/properties/foo");
        when(schema.schemaNode).thenReturn(JsonNodeFactory.instance.objectNode());

        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();

        // Invoke the method – we only care that no exception is thrown and that
        // registerDiscriminator is called once.
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator, schema, "atPath");

        verify(mockDiscriminatorContext, times(1))
                .registerDiscriminator(eq("#/properties/foo"), eq(rootDiscriminator));
    }

    /* --------------------------------------------------------------------- */
    /* 7. registerAndMergeDiscriminator – redefining propertyName throws   */
    /* --------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_RedefinePropertyName_Throws() {
        // Simulate a parent discriminator already registered for the schema path.
        when(mockDiscriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(JsonNodeFactory.instance.objectNode());

        // Child schema defines its own discriminator with a propertyName – illegal.
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type");

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("#/components/schemas/Bar");
        when(schema.schemaNode).thenReturn(JsonNodeFactory.instance.objectNode()
                .set("discriminator", childDiscriminator));

        // Root discriminator is empty – the merge logic will detect the illegal
        // redefinition and throw JsonSchemaException.
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();

        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator, schema, "myPath");
    }

    /* --------------------------------------------------------------------- */
    /* 8. fetchSubSchemaNode – suppression returns null                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNode_SuppressedReturnsNull() {
        // Build a validator with suppression enabled.
        TestValidator suppressed = new TestValidator("#/suppress",
                emptySchemaNode, mockParentSchema,
                ValidatorTypeCode.UNKNOWN, mockValidationContext, true);

        assertNull("When suppression is enabled the method must return null",
                suppressed.fetchSubSchemaNode(mockValidationContext));
    }

    /* --------------------------------------------------------------------- */
    /* 9. fetchSubSchemaNode – normal path resolves sub‑schema (happy path) */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNode_ResolvesSubSchema() throws Exception {
        // Prepare a schema node that contains a distinct "id" field.
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode()
                .put("id", "http://example.com/defs/myDef");

        // Stub URI creation.
        URI fakeUri = new URI("http://example.com/defs/myDef");
        when(mockUriFactory.create("http://example.com/defs/myDef")).thenReturn(fakeUri);
        // Stub JsonSchemaFactory to return a dummy schema.
        JsonSchema dummySchema = mock(JsonSchema.class);
        when(mockJsonSchemaFactory.getSchema(eq(fakeUri), any()))
                .thenReturn(dummySchema);

        TestValidator normal = new TestValidator("#/normal",
                schemaNode, mockParentSchema,
                ValidatorTypeCode.UNKNOWN, mockValidationContext, false);

        JsonSchema result = normal.fetchSubSchemaNode(mockValidationContext);
        assertSame("The fetched sub‑schema must be the one supplied by the factory", dummySchema, result);
    }

    /* --------------------------------------------------------------------- */
    /* 10. getNodeFieldType – reads the parent schema's "type" property      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldType_ReturnsParentType() {
        // Parent schema node contains a "type" field.
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode()
                .put("type", "string");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        // Re‑create validator with the mocked parent schema.
        validator = new TestValidator("#/typeTest", emptySchemaNode,
                mockParentSchema, ValidatorTypeCode.UNKNOWN,
                mockValidationContext, false);

        assertEquals("string", validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /* 11. preloadJsonSchemas – each schema's initializeValidators() called */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemas_InvokesInitializeValidators() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Set<JsonSchema> set = new HashSet<>();
        set.add(schema1);
        set.add(schema2);

        validator.preloadJsonSchemas(set);

        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /* 12. Path helpers – atRoot, atPath(token) and atPath(index)            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPathHelpers() {
        // The default PathType uses "#" as root and '/' as separator.
        assertEquals("#", validator.atRoot());

        String childPath = validator.atPath("#/properties", "name");
        assertEquals("#/properties/name", childPath);

        String indexedPath = validator.atPath("#/items", 3);
        // For the default implementation the index is appended as "[3]".
        assertEquals("#/items[3]", indexedPath);
    }

    /* --------------------------------------------------------------------- */
    /* 13. Accessor methods – getSchemaPath / getSchemaNode / getParentSchema */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAccessors() {
        assertEquals("#", validator.getSchemaPath());
        assertSame(emptySchemaNode, validator.getSchemaNode());
        assertSame(mockParentSchema, validator.getParentSchema());
    }
}
```
###Test END##