/**
 * JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * The tests are written from the perspective of three “experts” that each
 * contribute a test case for a particular method.  The final file therefore
 * contains one comprehensive test for every public / protected method as well
 * as the most important static helpers (including edge‑cases and error
 * handling).
 *
 * Dependencies that are difficult to instantiate (e.g. {@link ValidationContext},
 * {@link JsonSchema}, {@link Logger}) are mocked with Mockito.
 *
 * The concrete class {@link TestValidator} is a minimal concrete subclass of the
 * abstract {@code BaseJsonValidator}; it simply returns an empty {@code Set}
 * from {@code validate(...)} so that we can instantiate the base class.
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /* --------------------------------------------------------------------- */
    /*  Mocks used by many test cases                                         */
    /* --------------------------------------------------------------------- */
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ValidationContext.URIFactory uriFactory;
    @Mock private JsonSchemaFactory schemaFactory;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonSchema subSchema;
    @Mock private Logger logger;
    @Mock private DiscriminatorContext discriminatorContext;

    private ObjectMapper mapper = new ObjectMapper();

    /* --------------------------------------------------------------------- */
    /*  Helper concrete validator (required because BaseJsonValidator is   */
    /*  abstract)                                                            */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parent,
                      ValidatorTypeCode validatorType, ValidationContext vc) {
            super(schemaPath, schemaNode, parent, validatorType, vc);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // trivial implementation – the tests never call the real validation
            return Collections.emptySet();
        }
    }

    private TestValidator validator;

    @Before
    public void setUp() {
        // default behaviour for the validation context hierarchy
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getJsonSchemaFactory()).thenReturn(schemaFactory);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(config.getPathType()).thenReturn(PathType.DEFAULT); // use the library default

        // parent schema must return a node; we keep it very small
        ObjectNode parentNode = mapper.createObjectNode();
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);
        when(parentSchema.getSchemaPath()).thenReturn("#/parent");

        validator = new TestValidator("#/test", mapper.createObjectNode(),
                                      parentSchema, ValidatorTypeCode.UNKNOWN, validationContext);
    }

    /* --------------------------------------------------------------------- */
    /*  1. equals(double,double) – tolerance handling                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertTrue(BaseJsonValidator.equals(-2.0, -2.0 + 5e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10)); // exceeds 1e‑12
    }

    /* --------------------------------------------------------------------- */
    /*  2. obtainSubSchemaNode – various branches (private static)        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testObtainSubSchemaNodeWhenIdMissing() throws Exception {
        // schema node without "id"
        ObjectNode schemaNode = mapper.createObjectNode();
        Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode",
                                                            JsonNode.class, ValidationContext.class);
        m.setAccessible(true);
        Object result = m.invoke(null, schemaNode, validationContext);
        assertNull(result);
    }

    @Test
    public void testObtainSubSchemaNodeInvalidURI() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("id", ":// malformed uri");
        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException());

        Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode",
                                                            JsonNode.class, ValidationContext.class);
        m.setAccessible(true);
        Object result = m.invoke(null, schemaNode, validationContext);
        assertNull(result);
    }

    @Test
    public void testObtainSubSchemaNodeValidReturnsSchema() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("id", "http://example.com/schema.json");

        URI fakeUri = new URI("http://example.com/schema.json");
        when(uriFactory.create("http://example.com/schema.json")).thenReturn(fakeUri);
        when(schemaFactory.getSchema(fakeUri, config)).thenReturn(subSchema);

        Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode",
                                                            JsonNode.class, ValidationContext.class);
        m.setAccessible(true);
        Object result = m.invoke(null, schemaNode, validationContext);
        assertSame(subSchema, result);
    }

    /* --------------------------------------------------------------------- */
    /*  3. debug(Logger,JsonNode,JsonNode,String) – logger invocation       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDebugLogsCorrectMessage() {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        JsonNode root = mapper.createObjectNode().put("root", true);
        BaseJsonValidator.debug(logger, node, root, "/path");
        verify(logger, times(1)).debug(eq("validate( {}, {}, {})"),
                eq(node), eq(root), eq("/path"));
    }

    /* --------------------------------------------------------------------- */
    /*  4. checkDiscriminatorMatch – null property value marks match        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchNullPropertyValueMarksMatch() {
        ObjectNode discriminator = mapper.createObjectNode();
        // No mapping needed – null signals “any”
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator, null, subSchema);
        verify(discriminatorContext, times(1)).markMatch();
    }

    /* --------------------------------------------------------------------- */
    /*  5. checkDiscriminatorMatch – implicit mapping (no "mapping")        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchImplicitMapping() {
        // discriminator without explicit mapping
        ObjectNode discriminator = mapper.createObjectNode();

        // schema path ends with the discriminator property value
        when(subSchema.schemaPath).thenReturn("#/components/schemas/Car");
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator, "Car", subSchema);
        verify(discriminatorContext, times(1)).markMatch();
    }

    /* --------------------------------------------------------------------- */
    /*  6. checkDiscriminatorMatch – explicit mapping match                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchExplicitMapping() {
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("Bike", "#/components/schemas/Bike");
        discriminator.set("mapping", mapping);

        when(subSchema.schemaPath).thenReturn("#/components/schemas/Bike");
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator, "Bike", subSchema);
        verify(discriminatorContext, times(1)).markMatch();
    }

    /* --------------------------------------------------------------------- */
    /*  7. registerAndMergeDiscriminator – redefinition of propertyName   */
    /* --------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRedefinePropertyNameThrows() {
        // Context already has a discriminator for the path
        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(mapper.createObjectNode());

        // Schema that contains a discriminator with its own propertyName
        ObjectNode schemaNode = mapper.createObjectNode();
        ObjectNode discriminatorNode = mapper.createObjectNode();
        discriminatorNode.put("propertyName", "type");
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(schemaNode);
        when(schema.schemaPath).thenReturn("#/schemas/Derived");

        // root discriminator (empty mapping)
        ObjectNode rootDiscriminator = mapper.createObjectNode();

        // should throw because propertyName is already defined upstream
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator, schema, "at ");
    }

    /* --------------------------------------------------------------------- */
    /*  8. registerAndMergeDiscriminator – merge two non‑conflicting maps */
    /* --------------------------------------------------------------------- */
    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappings() {
        // Existing root discriminator with one mapping
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        ObjectNode rootMapping = mapper.createObjectNode();
        rootMapping.put("Cat", "#/schemas/Cat");
        rootDiscriminator.set("mapping", rootMapping);

        // Nested schema with an additional mapping
        ObjectNode nestedDiscriminator = mapper.createObjectNode();
        ObjectNode nestedMapping = mapper.createObjectNode();
        nestedMapping.put("Dog", "#/schemas/Dog");
        nestedDiscriminator.set("mapping", nestedMapping);

        JsonSchema nestedSchema = mock(JsonSchema.class);
        when(nestedSchema.schemaNode).thenReturn(nestedDiscriminator);
        when(nestedSchema.schemaPath).thenReturn("#/schemas/Dog");

        // Context returns the already‑registered root discriminator for the path
        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(rootDiscriminator);

        // Perform merge – should not throw
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator, nestedSchema, "at ");

        // Verify that the new entry has been added
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals("#/schemas/Cat", merged.get("Cat").asText());
        assertEquals("#/schemas/Dog", merged.get("Dog").asText());

        // Finally the context should have been asked to register the (now merged) discriminator
        verify(discriminatorContext, times(1)).registerDiscriminator(anyString(), eq(rootDiscriminator));
    }

    /* --------------------------------------------------------------------- */
    /*  9. fetchSubSchemaNode – suppressed retrieval                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator suppressed = new TestValidator("#/test", mapper.createObjectNode(),
                parentSchema, ValidatorTypeCode.UNKNOWN,
                validationContext) {
            {
                // force suppress flag via reflection (constructor argument false by default)
                this.suppressSubSchemaRetrieval = true;
            }
        };
        assertNull(suppressed.fetchSubSchemaNode(validationContext));
    }

    /* --------------------------------------------------------------------- */
    /* 10. getNodeFieldType – reads "type" from parent schema node           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldType() {
        // parent schema node contains a "type" field
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenMissing() {
        // parent schema node without a "type" field
        when(parentSchema.getSchemaNode()).thenReturn(mapper.createObjectNode());
        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /* 11. preloadJsonSchemas – each schema must be initialized            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemasInitializesAll() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> coll = Arrays.asList(s1, s2);
        validator.preloadJsonSchemas(coll);
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /* 12. Path handling – atRoot, atPath(String) and atPath(int)           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPathHelpersWithDefaultPathType() {
        // PathType.DEFAULT (provided by the library) uses '/' as separator.
        String root = validator.atRoot();
        assertNotNull(root);
        assertTrue(root.startsWith("/"));

        String child = validator.atPath(root, "prop");
        assertEquals(root + "/prop", child);

        String indexed = validator.atPath(root, 2);
        assertEquals(root + "/2", indexed);
    }

    /* --------------------------------------------------------------------- */
    /* 13. getters – schemaPath, schemaNode, parentSchema                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGettersReturnConstructorValues() {
        assertEquals("#/test", validator.getSchemaPath());
        assertNotNull(validator.getSchemaNode()); // empty ObjectNode created in setUp
        assertSame(parentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- */
    /* 14. validate(ExecutionContext, JsonNode) – delegate to overload       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testValidateDelegatesToThreeArgVersion() {
        ExecutionContext exec = mock(ExecutionContext.class);
        JsonNode node = mapper.createObjectNode().put("x", 1);

        // Spy on the validator to capture the internal call
        TestValidator spy = Mockito.spy(validator);
        spy.validate(exec, node);
        // three‑arg version is protected – we verify it was invoked via reflection
        try {
            Method m = BaseJsonValidator.class.getDeclaredMethod("validate",
                    ExecutionContext.class, JsonNode.class, JsonNode.class, String.class);
            m.setAccessible(true);
            verify(spy, times(1)).validate(eq(exec), eq(node), eq(node), anyString());
        } catch (NoSuchMethodException e) {
            fail("Expected protected validate method not found");
        }
    }
}
