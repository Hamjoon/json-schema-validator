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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>This test class creates a concrete {@code DummyValidator} that extends the abstract
 * {@code BaseJsonValidator} so that protected and package‑private members can be exercised.
 * Mockito is used to stub out heavy collaborators such as {@link ValidationContext},
 * {@link JsonSchema}, {@link ExecutionContext} and {@link Logger}.</p>
 *
 * <p>The suite covers:</p>
 * <ul>
 *   <li>Utility methods {@code equals} and {@code debug}</li>
 *   <li>Discriminator handling – implicit & explicit mappings, fallback logic and error cases</li>
 *   <li>Sub‑schema retrieval (including the suppression flag)</li>
 *   <li>Accessor methods and path building utilities</li>
 *   <li>Pre‑loading of schemas</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // -----------------------------------------------------------------------
    // Test helpers
    // -----------------------------------------------------------------------

    /** Minimal concrete implementation of {@link BaseJsonValidator}. */
    private static class DummyValidator extends BaseJsonValidator {
        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Simple stub – always return empty set.
            return Collections.emptySet();
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.DiscriminatorContext discriminatorContext;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonSchema childSchema;
    @Mock private JsonSchemaFactory schemaFactory;
    @Mock private URIFactory uriFactory;
    @Mock private Logger logger;
    @Mock private ExecutionContext executionContext;

    private ObjectNode simpleDiscriminator;
    private DummyValidator validator;

    @Before
    public void setUp() throws Exception {
        // Default configuration for ValidationContext – we only need the pieces that are accessed.
        Config config = mock(Config.class);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(config.getPathType()).thenReturn(PathType.DEFAULT);
        when(validationContext.getJsonSchemaFactory()).thenReturn(schemaFactory);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getDiscriminatorContext()).thenReturn(discriminatorContext);

        // A dummy parent schema that holds a simple "type" field.
        ObjectNode parentSchemaNode = MAPPER.createObjectNode().put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchema.schemaPath).thenReturn("#/parent");

        // Build a minimal validator instance.
        validator = new DummyValidator("#/dummy",
                MAPPER.createObjectNode(),
                parentSchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);

        // Simple discriminator without explicit mapping – used by many tests.
        simpleDiscriminator = MAPPER.createObjectNode();
        simpleDiscriminator.put("propertyName", "type");
    }

    // -----------------------------------------------------------------------
    // 1. equals(double,double)
    // -----------------------------------------------------------------------
    @Test
    public void equals_shouldReturnTrueWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(0.123456789012, 0.123456789013));
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
    }

    @Test
    public void equals_shouldReturnFalseOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(0.0, 1e-10));
        assertFalse(BaseJsonValidator.equals(100.0, 100.0 + 1e-9));
    }

    // -----------------------------------------------------------------------
    // 2. debug(Logger,JsonNode,JsonNode,String)
    // -----------------------------------------------------------------------
    @Test
    public void debug_shouldLogCorrectMessage() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        String at = "$.data";

        BaseJsonValidator.debug(logger, node, root, at);
        verify(logger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq(at));
    }

    // -----------------------------------------------------------------------
    // 3. checkDiscriminatorMatch – various branches
    // -----------------------------------------------------------------------
    @Test
    public void checkDiscriminatorMatch_nullPropertyValue_marksMatch() {
        // When discriminator property value is null we immediately mark a match.
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                simpleDiscriminator, null, childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_implicitMappingMatch_marksMatch() {
        // No explicit mapping – rely on implicit path suffix.
        when(childSchema.schemaPath).thenReturn("#/components/schemas/Animal");
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                simpleDiscriminator, "Animal", childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_explicitMappingMatch_marksMatch() {
        // Build explicit mapping: "dog" -> "#/components/schemas/Dog"
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("dog", "#/components/schemas/Dog");
        simpleDiscriminator.set("mapping", mapping);
        when(childSchema.schemaPath).thenReturn("#/components/schemas/Dog");

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                simpleDiscriminator, "dog", childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_explicitNoMatch_implicitFallback() {
        // Mapping present but does not match; implicit fallback should be evaluated
        // because the mapping does NOT override the parent schema path.
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("cat", "#/components/schemas/Cat");
        simpleDiscriminator.set("mapping", mapping);
        when(childSchema.schemaPath).thenReturn("#/components/schemas/Dog");
        when(childSchema.schemaPath).thenReturn("#/components/schemas/Dog");
        // No explicit match, but implicit path ends with the discriminator value.
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                simpleDiscriminator, "Dog", childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_explicitNoMatch_andExplicitOverride_preventsImplicit() {
        // Mapping exists and explicitly overrides the parent schema path – implicit fallback must NOT happen.
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("dog", "#/components/schemas/OtherDog");
        simpleDiscriminator.set("mapping", mapping);
        when(childSchema.schemaPath).thenReturn("#/components/schemas/Dog");
        // The mapping overrides the parent path for "dog", therefore no match should be recorded.
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                simpleDiscriminator, "dog", childSchema);
        verify(discriminatorContext, never()).markMatch();
    }

    // -----------------------------------------------------------------------
    // 4. registerAndMergeDiscriminator – error and merge scenarios
    // -----------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void registerAndMergeDiscriminator_redefiningPropertyName_throwsException() {
        // Simulate root discriminator already registered for the same schema path.
        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(simpleDiscriminator);
        // Schema that also defines a discriminator with a different propertyName.
        ObjectNode newDiscriminator = MAPPER.createObjectNode()
                .put("propertyName", "newType");
        JsonSchema schema = mock(JsonSchema.class);
        ObjectNode schemaNode = MAPPER.createObjectNode().set("discriminator", newDiscriminator);
        when(schema.schemaNode).thenReturn(schemaNode);
        when(schema.schemaPath).thenReturn("#/components/schemas/Animal");

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                simpleDiscriminator, schema, "at ");
    }

    @Test
    public void registerAndMergeDiscriminator_mergesMappingsWithoutRedefinition() {
        // Root discriminator already has a mapping for "cat".
        ObjectNode rootMapping = MAPPER.createObjectNode().put("cat", "#/components/schemas/Cat");
        simpleDiscriminator.set("mapping", rootMapping);

        // Child schema adds a new mapping for "dog".
        ObjectNode childMapping = MAPPER.createObjectNode().put("dog", "#/components/schemas/Dog");
        ObjectNode childDiscriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", childMapping);
        JsonSchema child = mock(JsonSchema.class);
        ObjectNode childSchemaNode = MAPPER.createObjectNode().set("discriminator", childDiscriminator);
        when(child.schemaNode).thenReturn(childSchemaNode);
        when(child.schemaPath).thenReturn("#/components/schemas/Dog");

        // No existing discriminator for this path – register should succeed.
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(null);

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                simpleDiscriminator, child, "at ");

        // Verify that the merged mapping now contains both entries.
        ObjectNode merged = (ObjectNode) simpleDiscriminator.get("mapping");
        assertEquals("#/components/schemas/Cat", merged.get("cat").asText());
        assertEquals("#/components/schemas/Dog", merged.get("dog").asText());

        // Verify that the discriminator was finally registered.
        verify(discriminatorContext).registerDiscriminator(eq(child.schemaPath), eq(simpleDiscriminator));
    }

    // -----------------------------------------------------------------------
    // 5. fetchSubSchemaNode – suppression flag and successful retrieval
    // -----------------------------------------------------------------------
    @Test
    public void fetchSubSchemaNode_whenSuppressed_returnsNull() {
        DummyValidator suppressed = new DummyValidator("#/s",
                MAPPER.createObjectNode(),
                parentSchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                true);
        assertNull(suppressed.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void fetchSubSchemaNode_obtainsSubSchema() throws Exception {
        // Schema node contains an "id" field with a valid URI.
        ObjectNode schemaNode = MAPPER.createObjectNode().put("id", "http://example.com/schema");
        DummyValidator v = new DummyValidator("#/s", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);

        URI fakeUri = new URI("http://example.com/schema");
        when(uriFactory.create("http://example.com/schema")).thenReturn(fakeUri);
        JsonSchema fetched = mock(JsonSchema.class);
        when(schemaFactory.getSchema(fakeUri, null)).thenReturn(fetched);

        assertSame(fetched, v.fetchSubSchemaNode(validationContext));
    }

    // -----------------------------------------------------------------------
    // 6. Accessor methods (schemaPath, schemaNode, parentSchema)
    // -----------------------------------------------------------------------
    @Test
    public void accessorMethods_returnConstructorValues() {
        assertEquals("#/dummy", validator.getSchemaPath());
        assertNotNull(validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
    }

    // -----------------------------------------------------------------------
    // 7. getNodeFieldType – reads "type" from parent schema
    // -----------------------------------------------------------------------
    @Test
    public void getNodeFieldType_returnsParentType() {
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void getNodeFieldType_returnsNullWhenAbsent() {
        when(parentSchema.getSchemaNode()).thenReturn(MAPPER.createObjectNode()); // no "type"
        assertNull(validator.getNodeFieldType());
    }

    // -----------------------------------------------------------------------
    // 8. preloadJsonSchemas – ensure initializeValidators is invoked
    // -----------------------------------------------------------------------
    @Test
    public void preloadJsonSchemas_initializesAllSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Set<JsonSchema> set = new HashSet<>();
        set.add(s1);
        set.add(s2);

        validator.preloadJsonSchemas(set);
        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    // -----------------------------------------------------------------------
    // 9. Path utilities (getPathType, atRoot, atPath)
    // -----------------------------------------------------------------------
    @Test
    public void pathUtilities_buildCorrectJsonPointer() {
        // Default PathType uses "$" as root and "/" as separator.
        assertEquals("$", validator.atRoot());
        assertEquals("$.foo", validator.atPath("$", "foo"));
        assertEquals("$.array[2]", validator.atPath("$", 2));
    }

    @Test
    public void getPathType_returnsConfiguredInstance() {
        // The mock config returns PathType.DEFAULT – verify it is propagated.
        assertSame(PathType.DEFAULT, validator.getPathType());
    }
}
```
###Test END##