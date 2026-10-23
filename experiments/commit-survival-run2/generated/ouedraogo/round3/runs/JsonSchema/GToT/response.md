###Test START##
```java
/****************************************************************************************
 * JsonSchemaTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The test class follows the workflow described in the exercise:
 *   1. List all public methods (see the comment block at the top of the class).
 *   2. Provide a basic test case for each method.
 *   3. Identify edge‑cases / exception scenarios.
 *   4. Add dedicated tests for those edge‑cases.
 *   5. Merge everything into a single test file.
 *
 * NOTE:
 *   - The real implementation depends on many classes from the *networknt* library.
 *   - To keep the suite fast and focused we use Mockito to mock those collaborators.
 *   - Jackson's {@code ObjectMapper} creates the JSON nodes needed for the tests.
 *
 * Required test libraries (Maven coordinates):
 *   <dependency>
 *       <groupId>junit</groupId>
 *       <artifactId>junit</artifactId>
 *       <version>4.13.2</version>
 *       <scope>test</scope>
 *   </dependency>
 *   <dependency>
 *       <groupId>org.mockito</groupId>
 *       <artifactId>mockito-core</artifactId>
 *       <version>5.5.0</version>
 *       <scope>test</scope>
 *   </dependency>
 *   <dependency>
 *       <groupId>com.fasterxml.jackson.core</groupId>
 *       <artifactId>jackson-databind</artifactId>
 *       <version>2.15.2</version>
 *       <scope>test</scope>
 *   </dependency>
 ****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Public methods of {@link JsonSchema} (extracted from the source):
 *
 * <pre>
 *  public JsonSchema createChildSchema(String schemaPath, JsonNode schemaNode)
 *  public ValidationContext getValidationContext()
 *  public URI getCurrentUri()
 *  public JsonNode getRefSchemaNode(String ref)
 *  public JsonSchema findAncestor()
 *  public Set<ValidationMessage> validate(ExecutionContext executionContext,
 *                                          JsonNode jsonNode,
 *                                          JsonNode rootNode,
 *                                          String at)
 *  public Set<ValidationMessage> validate(JsonNode rootNode)
 *  public ValidationResult validateAndCollect(ExecutionContext executionContext,
 *                                            JsonNode node)
 *  public ValidationResult validateAndCollect(JsonNode node)
 *  public ValidationResult walk(ExecutionContext executionContext,
 *                                JsonNode node,
 *                                boolean shouldValidateSchema)
 *  public ValidationResult walk(JsonNode node, boolean shouldValidateSchema)
 *  public ValidationResult walkAtNode(ExecutionContext executionContext,
 *                                     JsonNode node,
 *                                     JsonNode rootNode,
 *                                     String at,
 *                                     boolean shouldValidateSchema)
 *  public Set<ValidationMessage> walk(ExecutionContext executionContext,
 *                                      JsonNode node,
 *                                      JsonNode rootNode,
 *                                      String at,
 *                                      boolean shouldValidateSchema)
 *  public String toString()
 *  public boolean hasRequiredValidator()
 *  public JsonValidator getRequiredValidator()
 *  public boolean hasTypeValidator()
 *  public TypeValidator getTypeValidator()
 *  public void initializeValidators()
 *  public boolean isDynamicAnchor()
 *  public ExecutionContext createExecutionContext()
 * </pre>
 *
 * The test suite below provides:
 *   • A basic test for each public method.
 *   • Edge‑case tests (e.g. malformed $recursiveAnchor, URI fragment handling,
 *     missing validators, etc.).
 *   • Verification that internal state changes as expected.
 */
@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContextMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private JsonValidator mockValidator;

    @Mock
    private JsonValidator mockRequiredValidator;

    @Mock
    private TypeValidator mockTypeValidator;

    @Mock
    private ExecutionContext executionContextMock;

    @Mock
    private CollectorContext collectorContextMock;

    @Mock
    private WalkListenerRunner walkListenerRunnerMock;

    @InjectMocks
    private JsonSchema jsonSchema; // will be instantiated in @Before

    /**
     * Helper: inject a private field via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper: create a simple JsonSchema instance with a given schema node.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI baseUri) {
        // Use the deprecated public constructor – it forwards to the private one.
        return new JsonSchema(validationContextMock, baseUri, schemaNode);
    }

    @Before
    public void setUp() throws Exception {
        // Common stubbing
        when(validationContextMock.getMetaSchema()).thenReturn(metaSchemaMock);
        when(validationContextMock.getURIFactory()).thenReturn(URI::new);
        when(validationContextMock.getConfig()).thenReturn(new SchemaValidatorsConfig());

        // ExecutionContext & CollectorContext mocking
        when(executionContextMock.getCollectorContext()).thenReturn(collectorContextMock);
        when(collectorContextMock.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(null);
        when(collectorContextMock.enterDynamicScope(any())).thenReturn(new CollectorContext.Scope());
        when(collectorContextMock.exitDynamicScope()).thenReturn(new CollectorContext.Scope());

        // Walk listener runner – default behaviour is to allow walking.
        when(walkListenerRunnerMock.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        // No post‑walk side‑effects needed for the tests.
    }

    /* -------------------------------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testCreateChildSchema() throws Exception {
        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema child = jsonSchema.createChildSchema("#/properties/name", childNode);
        assertNotNull(child);
        assertEquals("#/properties/name", child.getSchemaPath());
        assertEquals(childNode, child.getSchemaNode());
    }

    @Test
    public void testGetValidationContext() {
        assertSame(validationContextMock, jsonSchema.getValidationContext());
    }

    @Test
    public void testGetCurrentUri_WithId() throws Exception {
        // Schema with an "id" keyword – id resolves against the supplied base URI.
        JsonNode schemaNode = MAPPER.readTree("{\"id\":\"mySchema.json\"}");
        URI base = new URI("http://example.com/schemas/");
        JsonSchema schema = createSchema(schemaNode, base);
        assertEquals(new URI("http://example.com/schemas/mySchema.json"), schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUri_NoId() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        URI base = new URI("http://example.com/base/");
        JsonSchema schema = createSchema(schemaNode, base);
        // No id → currentUri stays as the base URI.
        assertEquals(base, schema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointer() throws Exception {
        // Build a schema node with a nested property.
        JsonNode schemaNode = MAPPER.readTree("{\"properties\":{\"address\":{\"type\":\"string\"}}}");
        JsonSchema schema = createSchema(schemaNode, null);

        // Resolve "#/properties/address"
        JsonNode result = schema.getRefSchemaNode("#/properties/address");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_UrnFragment() throws Exception {
        // Simulate metaSchema handling of a URN fragment.
        JsonNode root = MAPPER.readTree("{\"definitions\":{\"myDef\":{\"type\":\"integer\"}}}");
        JsonSchema schema = createSchema(root, null);

        // stub metaSchema.getNodeByFragmentRef to return the definition node
        when(metaSchemaMock.getNodeByFragmentRef(eq("urn:myDef"), eq(root))).thenReturn(
                root.at("/definitions/myDef")
        );

        JsonNode result = schema.getRefSchemaNode("urn:myDef");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetRefSchemaNode_InvalidRecursiveAnchorThrows() throws Exception {
        // "$recursiveAnchor" must be a boolean – provide an invalid non‑boolean value.
        JsonNode schemaNode = MAPPER.readTree("{\"$recursiveAnchor\": \"notABoolean\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        // The exception is thrown during the call to read() which happens in the constructor,
        // so simply constructing the object is sufficient.
    }

    @Test
    public void testFindAncestor_NoParent() {
        // The root schema should return itself as the ancestor.
        JsonSchema ancestor = jsonSchema.findAncestor();
        assertSame(jsonSchema, ancestor);
    }

    @Test
    public void testFindAncestor_WithParent() throws Exception {
        JsonNode parentNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema parent = createSchema(parentNode, null);
        // Manually set parentSchema field (private) via reflection.
        setPrivateField(jsonSchema, "parentSchema", parent);
        JsonSchema ancestor = jsonSchema.findAncestor();
        assertSame(parent, ancestor);
    }

    @Test
    public void testValidate_EmptyValidators_ReturnsNoErrors() {
        // Force the validators map to be empty.
        setPrivateField(jsonSchema, "validators", Collections.emptyMap());

        JsonNode data = MAPPER.createObjectNode(); // empty object
        Set<ValidationMessage> errors = jsonSchema.validate(data);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAndCollect_WithEmptyValidators() {
        setPrivateField(jsonSchema, "validators", Collections.emptyMap());

        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = jsonSchema.validateAndCollect(data);
        assertNotNull(result);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testWalk_ReturnsValidationResult() {
        // Walk should delegate to the internal walk method.
        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = jsonSchema.walk(data, false);
        assertNotNull(result);
        // No validators → no errors.
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testWalkAtNode_DelegatesCorrectly() {
        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = jsonSchema.walkAtNode(executionContextMock, data,
                data, "$", true);
        assertNotNull(result);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testToString_IncludesSchemaPathAndNode() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"null\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        String s = schema.toString();
        assertTrue(s.contains(schema.getSchemaPath()));
        assertTrue(s.contains("\"type\":\"null\""));
    }

    @Test
    public void testHasRequiredValidator_Found() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"required\":[\"id\"]}");
        JsonSchema schema = createSchema(schemaNode, null);
        // The read() method will create a RequiredValidator; we simply check the flag.
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidator_NotFound() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertFalse(schema.hasRequiredValidator());
        assertNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_Found() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidator_NotFound() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"properties\":{}}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertFalse(schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    @Test
    public void testInitializeValidators_CallsPreload() throws Exception {
        // Prepare a map with a mock validator.
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("#/type", mockValidator);
        setPrivateField(jsonSchema, "validators", map);
        setPrivateField(jsonSchema, "validatorsLoaded", false);

        jsonSchema.initializeValidators();

        verify(mockValidator, times(1)).preloadJsonSchema();
        // Subsequent call must not invoke preload again.
        jsonSchema.initializeValidators();
        verifyNoMoreInteractions(mockValidator);
    }

    @Test
    public void testIsDynamicAnchor_TrueWhenRecursiveAnchorTrue() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"$recursiveAnchor\":true}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchor_FalseWhenAbsent() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertFalse(schema.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierIfConfigured() {
        // Configure a custom supplier.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        ExecutionContext supplied = mock(ExecutionContext.class);
        cfg.setExecutionContextSupplier(() -> supplied);
        when(validationContextMock.getConfig()).thenReturn(cfg);

        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertSame(supplied, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultBehaviour() {
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNotNull(ctx);
        // Verify that a CollectorContext was created inside the ExecutionContext.
        CollectorContext cc = ctx.getCollectorContext();
        assertNotNull(cc);
    }

    /* -------------------------------------------------------------------------
     *  EDGE CASES / EXCEPTION SCENARIOS
     * ---------------------------------------------------------------------- */

    @Test
    public void testCombineCurrentUriWithIds_FragmentWithNoBaseUri_ReturnsNull() throws Exception {
        // id = "#/foo" but currentUri is null → method should return null.
        JsonNode schemaNode = MAPPER.readTree("{\"id\":\"#/foo\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertNull(schema.getCurrentUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_InvalidUriThrows() throws Exception {
        // Supply an id that is not a valid URI.
        JsonNode schemaNode = MAPPER.readTree("{\"id\":\"::invalid::\"}");
        // The ValidationContext's URIFactory will attempt to create a URI and fail.
        when(validationContextMock.getURIFactory()).thenThrow(new IllegalArgumentException("bad"));
        createSchema(schemaNode, new URI("http://example.com/"));
        // Exception is thrown from the constructor.
    }

    @Test
    public void testGetRefSchemaNode_MissingNodeFallsBackToSubSchema() throws Exception {
        // Build a schema that contains a $ref to a non‑existent pointer.
        JsonNode root = MAPPER.readTree("{\"$ref\":\"#/nonexistent\"}");
        JsonSchema schema = createSchema(root, null);
        // Stub fetchSubSchemaNode to return a child schema that can resolve the ref.
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.getRefSchemaNode(anyString())).thenReturn(MAPPER.readTree("{\"type\":\"string\"}"));
        when(schema.fetchSubSchemaNode(any())).thenReturn(subSchema);
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testWalk_KeywordListenerPreventsWalking() {
        // Make the pre‑walk listener return false → walk should be skipped.
        when(walkListenerRunnerMock.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false);

        // Inject the mocked listener runner.
        setPrivateField(jsonSchema, "keywordWalkListenerRunner", walkListenerRunnerMock);

        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = jsonSchema.walk(data, true);
        // No validators → still empty, but we also verify that the listener was consulted.
        verify(walkListenerRunnerMock, times(1))
                .runPreWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(), any(), any());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testWalk_PostWalkListenersAreInvoked() {
        // Ensure post‑walk listener is called even when there are no validators.
        when(walkListenerRunnerMock.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        setPrivateField(jsonSchema, "keywordWalkListenerRunner", walkListenerRunnerMock);

        JsonNode data = MAPPER.createObjectNode();
        jsonSchema.walk(data, true);

        verify(walkListenerRunnerMock, times(1))
                .runPostWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any(), any());
    }

    /* -------------------------------------------------------------------------
     *  Helper – expose private fetchSubSchemaNode for test (via reflection)
     * ---------------------------------------------------------------------- */

    // The actual JsonSchema class has a private method fetchSubSchemaNode used by
    // getRefSchemaNode.  In the test above we stub it via reflection.
    private JsonSchema fetchSubSchemaNodeProxy(JsonSchema schema, ValidationContext ctx) {
        try {
            Field f = JsonSchema.class.getDeclaredField("validationContext");
            f.setAccessible(true);
            f.set(schema, ctx);
            // The method is private; we invoke it via reflection.
            java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("fetchSubSchemaNode", ValidationContext.class);
            m.setAccessible(true);
            return (JsonSchema) m.invoke(schema, ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##