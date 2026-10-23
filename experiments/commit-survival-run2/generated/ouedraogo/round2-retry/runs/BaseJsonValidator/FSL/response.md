###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.slf4j.Logger;

/**
 * Unit tests for {@link BaseJsonValidator}.  Because the class is abstract we
 * provide a tiny concrete implementation that delegates all abstract behaviour
 * to simple stubs.  The tests focus on the protected and private static helpers,
 * the constructor wiring and the small public helpers.
 */
public class BaseJsonValidatorTest {

    /** a very small concrete validator used only for testing */
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
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }

        /* expose the protected static methods for testing */
        static void callCheckDiscriminatorMatch(DiscriminatorContext ctx,
                                                ObjectNode discriminator,
                                                String propValue,
                                                JsonSchema schema) {
            BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, propValue, schema);
        }

        static void callRegisterAndMergeDiscriminator(DiscriminatorContext ctx,
                                                      ObjectNode discriminator,
                                                      JsonSchema schema,
                                                      String at) {
            BaseJsonValidator.registerAndMergeDiscriminator(ctx, discriminator, schema, at);
        }

        static boolean callEquals(double a, double b) {
            return BaseJsonValidator.equals(a, b);
        }

        static void callDebug(Logger logger, JsonNode node, JsonNode root, String at) {
            BaseJsonValidator.debug(logger, node, root, at);
        }
    }

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private PathType pathType;
    @Mock private Logger logger;
    @Mock private DiscriminatorContext discriminatorContext;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonSchema childSchema;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // default config behaviour
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(config.getPathType()).thenReturn(pathType);
        when(pathType.getRoot()).thenReturn("$");
        when(pathType.append(anyString(), anyString())).thenAnswer(inv ->
                inv.getArgument(0) + "." + inv.getArgument(1));
        when(pathType.append(anyString(), anyInt())).thenAnswer(inv ->
                inv.getArgument(0) + "[" + inv.getArgument(1) + "]");
        when(parentSchema.getSchemaNode()).thenReturn(mapper.createObjectNode());
        when(parentSchema.getSchemaPath()).thenReturn("#/parent");
        when(childSchema.getSchemaNode()).thenReturn(mapper.createObjectNode());
        when(childSchema.getSchemaPath()).thenReturn("#/child");
    }

    /* --------------------------------------------------------------------- */
    /* Constructor / basic accessors                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructor_initialisesAllFields() {
        ObjectNode schemaNode = mapper.createObjectNode().put("type", "string");
        TestValidator validator = new TestValidator("#/test", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE_STRING, validationContext, false);

        assertEquals("#/test", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());

        // path type comes from config
        assertSame(pathType, validator.getPathType());
    }

    /* --------------------------------------------------------------------- */
    /* fetchSubSchemaNode                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        ObjectNode schemaNode = mapper.createObjectNode();
        TestValidator validator = new TestValidator("#/s", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE_OBJECT, validationContext, true);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void fetchSubSchemaNode_returnsNullWhenNoIdField() {
        // schemaNode without an "id" field -> obtainSubSchemaNode will return null
        ObjectNode schemaNode = mapper.createObjectNode().put("type", "object");
        TestValidator validator = new TestValidator("#/s", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE_OBJECT, validationContext, false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    /* --------------------------------------------------------------------- */
    /* equals (numeric tolerance)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void equals_considersCloseValuesEqual() {
        assertTrue(TestValidator.callEquals(1.000000000001, 1.0));
        assertTrue(TestValidator.callEquals(-0.0000000000005, 0.0));
    }

    @Test
    public void equals_considersDistantValuesDifferent() {
        assertFalse(TestValidator.callEquals(1.0, 1.1));
        assertFalse(TestValidator.callEquals(0.0, 1e-10 + 1e-12));
    }

    /* --------------------------------------------------------------------- */
    /* debug – should forward to logger with proper message format           */
    /* --------------------------------------------------------------------- */

    @Test
    public void debug_forwardsMessageToLogger() {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        JsonNode root = mapper.createObjectNode().put("root", true);
        TestValidator.callDebug(logger, node, root, "$.path");

        verify(logger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("$.path"));
    }

    /* --------------------------------------------------------------------- */
    /* getNodeFieldType – reads "type" from the parent schema node            */
    /* --------------------------------------------------------------------- */

    @Test
    public void getNodeFieldType_returnsTypeWhenPresent() {
        ObjectNode parentNode = mapper.createObjectNode().put("type", "integer");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        TestValidator validator = new TestValidator("#/p", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE_INTEGER, validationContext, false);
        assertEquals("integer", validator.getNodeFieldType());
    }

    @Test
    public void getNodeFieldType_returnsNullWhenAbsent() {
        when(parentSchema.getSchemaNode()).thenReturn(mapper.createObjectNode()); // empty
        TestValidator validator = new TestValidator("#/p", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE_ANY, validationContext, false);
        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /* preloadJsonSchemas – each schema must have initializeValidators called */
    /* --------------------------------------------------------------------- */

    @Test
    public void preloadJsonSchemas_initializesAllSuppliedSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> coll = Arrays.asList(s1, s2);

        TestValidator validator = new TestValidator("#/x", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE_ANY, validationContext, false);
        validator.preloadJsonSchemas(coll);

        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /* Path handling (atRoot / atPath)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void atRoot_usesPathTypeRoot() {
        TestValidator validator = new TestValidator("#/r", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE_ANY, validationContext, false);
        assertEquals("$", validator.atRoot());
    }

    @Test
    public void atPath_stringToken_appendsCorrectly() {
        TestValidator validator = new TestValidator("#/r", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE_ANY, validationContext, false);
        assertEquals("$.foo", validator.atPath("$", "foo"));
    }

    @Test
    public void atPath_intIndex_appendsCorrectly() {
        TestValidator validator = new TestValidator("#/r", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE_ANY, validationContext, false);
        assertEquals("$.items[3]", validator.atPath("$", 3));
    }

    /* --------------------------------------------------------------------- */
    /* Discriminator handling – checkDiscriminatorMatch                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void checkDiscriminatorMatch_nullPropertyValue_marksMatch() {
        // discriminatorPropertyValue == null => markMatch should be called immediately
        TestValidator.callCheckDiscriminatorMatch(discriminatorContext,
                mapper.createObjectNode(), null, childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_implicitMappingMatch_marksMatch() {
        // No explicit mapping, implicit match when schemaPath ends with "/foo"
        when(childSchema.getSchemaPath()).thenReturn("#/definitions/foo");
        ObjectNode discriminator = mapper.createObjectNode(); // no "mapping"
        TestValidator.callCheckDiscriminatorMatch(discriminatorContext,
                discriminator, "foo", childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_explicitMappingMatch_marksMatch() {
        // explicit mapping {"foo": "#/defs/bar"} matches child schema path
        ObjectNode mapping = mapper.createObjectNode()
                .put("foo", "#/defs/bar");
        ObjectNode discriminator = mapper.createObjectNode().set("mapping", mapping);
        when(childSchema.getSchemaPath()).thenReturn("#/defs/bar");

        TestValidator.callCheckDiscriminatorMatch(discriminatorContext,
                discriminator, "foo", childSchema);
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void checkDiscriminatorMatch_noExplicitMatch_fallsBackToImplicit_whenNoOverride() {
        // explicit mapping does NOT contain the value, but does not override parent schema path
        // -> implicit match should be attempted.
        ObjectNode mapping = mapper.createObjectNode()
                .put("bar", "#/defs/other");
        ObjectNode discriminator = mapper.createObjectNode().set("mapping", mapping);
        when(childSchema.getSchemaPath()).thenReturn("#/defs/foo");
        when(parentSchema.getSchemaPath()).thenReturn("#/parent");

        // set up a discriminator context that reports NO match yet
        when(discriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        // no explicit key overrides parent schema path
        TestValidator.callCheckDiscriminatorMatch(discriminatorContext,
                discriminator, "foo", childSchema);
        // after implicit check the context should be marked as match
        verify(discriminatorContext).markMatch();
    }

    @Test
    public void noExplicitDiscriminatorKeyOverride_detectsOverride() {
        // mapping contains a value equal to the parent schema path -> should return false
        ObjectNode mapping = mapper.createObjectNode()
                .put("foo", "#/parent"); // same as parent schema path
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getSchemaPath()).thenReturn("#/parent");

        // invoke the private static method via reflection
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("noExplicitDiscriminatorKeyOverride", JsonNode.class, JsonSchema.class);
            m.setAccessible(true);
            boolean result = (boolean) m.invoke(null, mapping, parent);
            assertFalse(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Discriminator handling – registerAndMergeDiscriminator                */
    /* --------------------------------------------------------------------- */

    @Test
    public void registerAndMergeDiscriminator_simpleRegistration() {
        // No discriminator on schema, just registration
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(null);

        TestValidator.callRegisterAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator, childSchema, "at:");

        verify(discriminatorContext).registerDiscriminator(eq(childSchema.getSchemaPath()), eq(rootDiscriminator));
    }

    @Test
    public void registerAndMergeDiscriminator_mergesMappings_withoutOverride() {
        // Simulate a root discriminator already present with a mapping {"a":"#A"}
        ObjectNode rootDiscriminator = mapper.createObjectNode()
                .set("mapping", mapper.createObjectNode().put("a", "#A"));
        // Simulate a nested schema that adds a new mapping {"b":"#B"}
        ObjectNode nestedDiscriminator = mapper.createObjectNode()
                .set("mapping", mapper.createObjectNode().put("b", "#B"));

        // make the context think a discriminator already exists for the child schema path
        when(discriminatorContext.getDiscriminatorForPath(childSchema.getSchemaPath()))
                .thenReturn(rootDiscriminator);

        // call the merge routine
        TestValidator.callRegisterAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator, childSchema, "at:");

        // after merging, rootDiscriminator should contain both entries a and b
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals("#A", merged.get("a").asText());
        assertEquals("#B", merged.get("b").asText());

        verify(discriminatorContext).registerDiscriminator(eq(childSchema.getSchemaPath()), eq(rootDiscriminator));
    }

    @Test(expected = JsonSchemaException.class)
    public void registerAndMergeDiscriminator_throwsWhenMappingRedefined() {
        // root mapping has "a":"#A"
        ObjectNode rootDiscriminator = mapper.createObjectNode()
                .set("mapping", mapper.createObjectNode().put("a", "#A"));
        // nested schema tries to redefine "a" with a different value "#B"
        ObjectNode nestedDiscriminator = mapper.createObjectNode()
                .set("mapping", mapper.createObjectNode().put("a", "#B"));

        when(discriminatorContext.getDiscriminatorForPath(childSchema.getSchemaPath()))
                .thenReturn(rootDiscriminator);

        // invoke – should throw JsonSchemaException because of redefinition
        TestValidator.callRegisterAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator, childSchema, "at:");
    }

    /* --------------------------------------------------------------------- */
    /* Helper to invoke private static method (noExplicitDiscriminatorKeyOverride) */
    /* --------------------------------------------------------------------- */

    // The method is already exercised via reflection in the test above,
    // no extra helper needed.
}
```
###Test END##