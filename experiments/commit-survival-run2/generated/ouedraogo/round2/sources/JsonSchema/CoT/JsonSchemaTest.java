/*****************************************************************************************
 * JsonSchemaTest.java
 *
 * This JUnit‑4 test class provides comprehensive unit tests for the public API of
 * {@link com.networknt.schema.JsonSchema}.  The tests are written in the same package
 * (com.networknt.schema) so that package‑private factory methods can be used.
 *
 * The file is organised in the following sections:
 *
 * 1.  Public method list (extracted from the source code)
 * 2.  Basic test cases – one test per public method that checks the normal behaviour
 * 3.  Edge‑case / exception tests – scenarios that exercise special branches and error
 *     handling inside the class
 * 4.  Helper methods and Mockito set‑up
 *
 * The test class uses Mockito for all heavy dependencies (ValidationContext, JsonMetaSchema,
 * JsonValidator, SchemaValidatorsConfig, URIFactory, etc.).  A real Jackson {@link JsonNode}
 * tree is created with an {@link com.fasterxml.jackson.databind.ObjectMapper} where a
 * concrete JSON structure is needed.
 *
 * The test class can be compiled and executed with JUnit‑4 (e.g. `mvn test` or an IDE).
 *****************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;

/**
 * 1️⃣  PUBLIC METHODS OF {@link JsonSchema}
 *
 * -------------------------------------------------------------------------------
 *  Signature                                                            | Return
 * -------------------------------------------------------------------------------
 * static JsonSchema from(ValidationContext, String, URI, JsonNode,
 *                         JsonSchema, boolean)                     : JsonSchema
 * public JsonSchema(ValidationContext, URI, JsonNode)                : -
 * public JsonSchema(ValidationContext, String, URI, JsonNode,
 *                   JsonSchema)                                      : -
 * public JsonSchema(ValidationContext, URI, JsonNode,
 *                   boolean)                                         : -
 * public JsonSchema createChildSchema(String, JsonNode)              : JsonSchema
 * public URI getCurrentUri()                                         : URI
 * public JsonNode getRefSchemaNode(String)                           : JsonNode
 * public JsonSchema findAncestor()                                   : JsonSchema
 * public Set<ValidationMessage> validate(JsonNode)                  : Set<ValidationMessage>
 * public ValidationResult validateAndCollect(JsonNode)               : ValidationResult
 * public ValidationResult walk(JsonNode, boolean)                    : ValidationResult
 * public ValidationResult walk(ExecutionContext, JsonNode,
 *                              boolean)                              : ValidationResult
 * public ValidationResult walkAtNode(ExecutionContext, JsonNode,
 *                                    JsonNode, String, boolean)    : ValidationResult
 * public void initializeValidators()                                 : void
 * public boolean isDynamicAnchor()                                    : boolean
 * public ExecutionContext createExecutionContext()                    : ExecutionContext
 * public boolean hasRequiredValidator()                               : boolean
 * public JsonValidator getRequiredValidator()                         : JsonValidator
 * public boolean hasTypeValidator()                                  : boolean
 * public TypeValidator getTypeValidator()                            : TypeValidator
 * public String toString()                                            : String
 * -------------------------------------------------------------------------------
 *
 * 2️⃣  BASIC TEST CASES – one per public method (happy path)
 * 3️⃣  EDGE‑CASE / EXCEPTION TESTS
 *
 * All tests are written in JUnit‑4 and use Mockito for dependency isolation.
 *****************************************************************************************/

@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    // -------------------------------------------------------------------------
    // Mocks for heavy collaborators
    // -------------------------------------------------------------------------
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private JsonValidator anyValidator;          // generic validator used in many tests

    @Mock
    private JsonValidator preloadableValidator; // validator used to verify preloadJsonSchema()

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // -----------------------------------------------------------------
        // Default behaviour for the validation context and its collaborators
        // -----------------------------------------------------------------
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenReturn(anyValidator);
        when(uriFactory.create(any(URI.class), anyString())).thenAnswer(invocation -> {
            URI base = invocation.getArgument(0);
            String id = invocation.getArgument(1);
            // simple concatenation – sufficient for the tests
            return new URI(base == null ? id : base.toString() + id);
        });

        // Config defaults – custom messages are disabled unless a test explicitly turns them on
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        when(config.getExecutionContextSupplier()).thenReturn(null);
    }

    // -------------------------------------------------------------------------
    // 2️⃣ BASIC TEST CASES
    // -------------------------------------------------------------------------

    @Test
    public void testFactoryMethod_fromCreatesInstance() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        assertNotNull(schema);
        assertEquals("#", schema.schemaPath);
    }

    @Test
    public void testCreateChildSchema() throws Exception {
        JsonNode parentNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema parent = JsonSchema.from(validationContext, "#", null, parentNode, null, false);
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");

        // the ValidationContext is expected to create a new schema for the child
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parent)))
                .thenReturn(mock(JsonSchema.class));

        JsonSchema child = parent.createChildSchema("/properties/name", childNode);
        assertNotNull(child);
        verify(validationContext).newSchema(eq("/properties/name"), eq(childNode), eq(parent));
    }

    @Test
    public void testGetCurrentUri_nullWhenNoIdAndNoBase() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointer() throws Exception {
        // schema: { "a": { "b": 42 } }
        JsonNode schemaNode = mapper.readTree("{\"a\":{\"b\":42}}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);

        JsonNode result = schema.getRefSchemaNode("#/a/b");
        assertNotNull(result);
        assertTrue(result.isInt());
        assertEquals(42, result.intValue());
    }

    @Test
    public void testFindAncestor_returnsRootWhenNoParent() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        JsonSchema ancestor = schema.findAncestor();
        assertSame(schema, ancestor);
    }

    @Test
    public void testValidateReturnsEmptySetWhenAllValidatorsPass() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);

        // make the single validator return an empty error set
        when(anyValidator.validate(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode instance = mapper.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> errors = schema.validate(instance);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAndCollectReturnsResultWithNoErrors() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        when(anyValidator.validate(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode instance = mapper.readTree("{\"x\":1}");
        ValidationResult result = schema.validateAndCollect(instance);
        assertTrue(result.getMessages().isEmpty());
        assertNotNull(result.getExecutionContext());
    }

    @Test
    public void testWalkDelegatesToWalkInternal() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        when(anyValidator.validate(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode instance = mapper.readTree("{\"a\":1}");
        ValidationResult result = schema.walk(instance, false);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testInitializeValidatorsCallsPreload() throws Exception {
        // Build a schema that contains a validator that we can verify preload is called on
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);

        // replace the lazily‑created validator map with one that contains our mock
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class),
                any(JsonSchema.class), anyString()))
                .thenReturn(preloadableValidator);
        // force the lazy initialisation
        schema.getValidators(); // triggers read()

        // reset interactions on the mock so we can verify only the preload call
        reset(preloadableValidator);
        schema.initializeValidators();
        verify(preloadableValidator, times(1)).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchor_defaultFalse() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        assertFalse(schema.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_returnsNonNull() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        ExecutionContext ec = schema.createExecutionContext();
        assertNotNull(ec);
        assertNotNull(ec.getCollectorContext());
    }

    @Test
    public void testHasRequiredValidator_andGetter() throws Exception {
        // schema containing a "required" keyword – the read() method will set requiredValidator
        JsonNode schemaNode = mapper.readTree("{\"required\":[\"name\"]}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        // make the factory return a mock that also implements RequiredValidator (any JsonValidator works)
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenReturn(anyValidator);
        // force validator creation
        schema.getValidators();

        assertTrue(schema.hasRequiredValidator());
        assertSame(anyValidator, schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_andGetter() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        // the factory must return a TypeValidator – we can use a mock that also implements it
        TypeValidator typeValidatorMock = mock(TypeValidator.class);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenReturn(typeValidatorMock);
        schema.getValidators();

        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidatorMock, schema.getTypeValidator());
    }

    @Test
    public void testToStringContainsPathAndNode() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"null\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "/myPath", null, schemaNode, null, false);
        String str = schema.toString();
        assertTrue(str.contains("\"/myPath\""));
        assertTrue(str.contains("\"type\""));
    }

    // -------------------------------------------------------------------------
    // 3️⃣ EDGE‑CASE / EXCEPTION TESTS
    // -------------------------------------------------------------------------

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithInvalidIdThrowsJsonSchemaException() throws Exception {
        // Force resolveSchemaId to return a non‑null id
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn("bad%id");
        // Make the URIFactory throw IllegalArgumentException when trying to create the URI
        when(uriFactory.create(any(URI.class), eq("bad%id"))).thenThrow(new IllegalArgumentException());

        JsonNode schemaNode = mapper.readTree("{\"id\":\"bad%id\"}");
        // Using the package‑private static factory to trigger the combine logic
        JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
    }

    @Test(expected = JsonSchemaException.class)
    public void testReadDetectsNonBooleanRecursiveAnchor() throws Exception {
        // Schema with $recursiveAnchor set to a non‑boolean value – should throw during read()
        JsonNode schemaNode = mapper.readTree("{\"$recursiveAnchor\":\"notABoolean\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        // invoke validation which lazily reads the schema and therefore hits the error
        schema.validate(mapper.readTree("{}"));
    }

    @Test
    public void testGetRefSchemaNode_HandlesUrlEncodedPointer() throws Exception {
        // Schema: { "a b": { "c": 5 } } – note the space that will be URL‑encoded as %20
        JsonNode schemaNode = mapper.readTree("{\"a b\":{\"c\":5}}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);

        JsonNode result = schema.getRefSchemaNode("#/a%20b/c");
        assertNotNull(result);
        assertEquals(5, result.intValue());
    }

    @Test
    public void testGetRefSchemaNode_FallsBackToMetaSchemaWhenFragmentRef() throws Exception {
        // Simulate a fragment reference that the meta‑schema can resolve.
        JsonNode schemaNode = mapper.readTree("{\"definitions\":{\"myDef\":{\"type\":\"string\"}}}");
        when(metaSchema.getNodeByFragmentRef(eq("#/definitions/myDef"), any(JsonNode.class)))
                .thenReturn(schemaNode.at("/definitions/myDef"));

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        JsonNode result = schema.getRefSchemaNode("#/definitions/myDef");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_HandlesMissingNodeGracefully() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"a\":1}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        // No such path – should return null (the implementation returns null after the fallback)
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNull(result);
    }

    @Test
    public void testValidateCollectsErrorsWhenValidatorFails() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);

        // Create a validator that returns a non‑empty error set
        ValidationMessage error = ValidationMessage.of("type", ValidatorTypeCode.TYPE, new MessageFormat("{0} error"), "type");
        Set<ValidationMessage> errorSet = new LinkedHashSet<>();
        errorSet.add(error);
        when(anyValidator.validate(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString()))
                .thenReturn(errorSet);

        JsonNode instance = mapper.readTree("{\"notAnObject\":[]}");
        ValidationResult result = schema.validateAndCollect(instance);
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().contains(error));
    }

    // -------------------------------------------------------------------------
    // Helper methods (private) – not part of the public test contract
    // -------------------------------------------------------------------------

    /**
     * Utility that creates a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema schemaFromJson(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        return JsonSchema.from(validationContext, "#", null, node, null, false);
    }
}
