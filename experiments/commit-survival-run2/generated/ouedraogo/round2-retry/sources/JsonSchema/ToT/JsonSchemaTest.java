/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written from the perspective of three “experts”.  Each
 * expert contributes one test that exercises a distinct public behaviour
 * of the class – normal usage, edge‑cases and error handling – and the
 * suite is assembled together at the end.
 *
 * The tests use **Mockito** to isolate {@code JsonSchema} from the rest of
 * the library (validation context, validators, meta‑schema, …).  Only the
 * parts that are required for the behaviour under test are stubbed.
 *
 * The suite covers:
 *   • construction and URI handling
 *   • $ref resolution (local fragment and external fragment)
 *   • detection of $recursiveAnchor (dynamic anchor)
 *   • presence of required‑ and type‑validators
 *   • lazy loading of validators via {@code initializeValidators()}
 *   • normal validation flow (errors are collected)
 *   • execution‑context creation (custom supplier)
 *
 * The tests are completely self‑contained – they do not read any external
 * files and they run with JUnit‑4 (the only required external dependency
 * is Mockito).
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.utils.StringUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link JsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    // -----------------------------------------------------------------
    // Common test fixtures
    // -----------------------------------------------------------------
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private URIFactory uriFactory;          // inner interface used by ValidationContext

    @Mock
    private JsonValidator genericValidator; // generic stub validator

    @Mock
    private JsonValidator requiredValidator; // validator for “required”

    @Mock
    private TypeValidator typeValidator;      // validator for “type”

    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // basic wiring of the validation context
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        // the factory just concatenates the base and the id (very simple behaviour)
        when(uriFactory.create(any(URI.class), anyString()))
                .thenAnswer(inv -> new URI(((URI) inv.getArgument(0)).toString() + inv.getArgument(1)));

        // By default the config does not provide a custom execution‑context supplier
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.isCustomMessageSupported()).thenReturn(false);
    }

    // -----------------------------------------------------------------
    // Helper – builds a JsonSchema instance with the supplied schema node
    // -----------------------------------------------------------------
    private JsonSchema buildSchema(JsonNode schemaNode, URI currentUri) {
        // ValidationContext#resolveSchemaId is used by JsonSchema to discover an "id"
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(
                schemaNode.has("id") ? schemaNode.get("id").asText() : null);

        // The ValidationContext creates a validator for each keyword that appears.
        // We return a generic mock for any keyword that we do not care about,
        // and specific mocks for the keywords that the tests need to assert.
        Answer<JsonValidator> validatorAnswer = inv -> {
            String keyword = inv.getArgument(1);
            if ("required".equals(keyword)) {
                return requiredValidator;
            }
            if ("type".equals(keyword)) {
                return typeValidator;
            }
            // any other keyword – return a generic mock (may be null for unknown)
            return genericValidator;
        };
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenAnswer(validatorAnswer);

        // Build the JsonSchema using the private constructor (via the static factory)
        return JsonSchema.from(validationContext, "#", currentUri, schemaNode, null, false);
    }

    // -----------------------------------------------------------------
    // 1️⃣  Expert A – normal construction and $ref resolution (local fragment)
    // -----------------------------------------------------------------
    @Test
    public void testGetRefSchemaNode_withLocalFragment() {
        // schema:
        // {
        //   "properties": {
        //     "name": { "type": "string" }
        //   }
        // }
        ObjectNode root = mapper.createObjectNode();
        ObjectNode properties = mapper.createObjectNode();
        ObjectNode name = mapper.createObjectNode();
        name.put("type", "string");
        properties.set("name", name);
        root.set("properties", properties);

        JsonSchema schema = buildSchema(root, URI.create("http://example.com/schema.json"));

        // Resolve the fragment "#/properties/name"
        JsonNode refNode = schema.getRefSchemaNode("#/properties/name");

        assertNotNull("Ref node must be resolved", refNode);
        assertTrue("Resolved node must be an object", refNode.isObject());
        assertEquals("string", refNode.get("type").asText());
    }

    // -----------------------------------------------------------------
    // 2️⃣  Expert B – external $ref handling (via meta‑schema) and $recursiveAnchor
    // -----------------------------------------------------------------
    @Test
    public void testGetRefSchemaNode_withExternalFragment_andRecursiveAnchor() throws URISyntaxException {
        // Build a minimal schema that contains $recursiveAnchor = true
        ObjectNode root = mapper.createObjectNode();
        root.put("$recursiveAnchor", true);
        // No other keywords – we only care about the anchor flag

        // Mock the meta‑schema to return a node when the fragment is resolved
        ObjectNode externalNode = mapper.createObjectNode();
        externalNode.put("type", "integer");
        when(metaSchema.getNodeByFragmentRef(eq("#/external"), any(JsonNode.class)))
                .thenReturn(externalNode);

        JsonSchema schema = buildSchema(root, URI.create("http://example.com/base#"));

        // The anchor flag should be set
        assertTrue("Schema must recognise $recursiveAnchor as dynamic", schema.isDynamicAnchor());

        // Resolve an external fragment (the meta‑schema path handling)
        JsonNode refNode = schema.getRefSchemaNode("#/external");

        assertNotNull(refNode);
        assertEquals("integer", refNode.get("type").asText());
    }

    // -----------------------------------------------------------------
    // 3️⃣  Expert C – validator initialisation, required/type detection and validation flow
    // -----------------------------------------------------------------
    @Test
    public void testInitializeValidators_andValidate_flow() {
        // schema containing required and type keywords
        ObjectNode root = mapper.createObjectNode();
        root.putArray("required").add("id");
        root.put("type", "object");

        // Make the generic validator return a non‑empty error set
        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = new HashSet<>(Collections.singleton(dummyMessage));
        when(genericValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(errorSet);

        JsonSchema schema = buildSchema(root, URI.create("urn:test:schema"));

        // --- part A : presence of required / type validators -----------------
        assertTrue("required validator must be detected", schema.hasRequiredValidator());
        assertSame(requiredValidator, schema.getRequiredValidator());

        assertTrue("type validator must be detected", schema.hasTypeValidator());
        assertSame(typeValidator, schema.getTypeValidator());

        // --- part B : lazy loading – initialiseValidators should trigger preload
        // preloadJsonSchema() is a void method – we verify it was called exactly once
        schema.initializeValidators();
        verify(genericValidator, times(1)).preloadJsonSchema();
        verify(requiredValidator, times(1)).preloadJsonSchema();
        verify(typeValidator, times(1)).preloadJsonSchema();

        // calling a second time must not cause a second preload
        schema.initializeValidators();
        verify(genericValidator, times(1)).preloadJsonSchema(); // unchanged
        verify(requiredValidator, times(1)).preloadJsonSchema();
        verify(typeValidator, times(1)).preloadJsonSchema();

        // --- part C : validate() should collect the error from genericValidator
        JsonNode data = mapper.createObjectNode(); // empty data – triggers the generic validator
        Set<ValidationMessage> errors = schema.validate(data);
        assertEquals("Exactly one error must be returned", 1, errors.size());
        assertTrue(errors.contains(dummyMessage));
    }

    // -----------------------------------------------------------------
    // 4️⃣  Expert A – execution‑context creation with custom supplier
    // -----------------------------------------------------------------
    @Test
    public void testCreateExecutionContext_usesCustomSupplierWhenPresent() {
        // custom supplier that returns a pre‑configured ExecutionContext
        CollectorContext collector = new CollectorContext(true, true);
        ExecutionContext supplied = new ExecutionContext(collector);
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(supplied);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);

        JsonSchema schema = buildSchema(mapper.createObjectNode(), URI.create("http://example.com/ctx"));
        ExecutionContext ctx = schema.createExecutionContext();

        assertSame("The supplied ExecutionContext must be returned", supplied, ctx);
        verify(supplier, times(1)).get();
    }

    // -----------------------------------------------------------------
    // 5️⃣  Expert B – edge case: id that is a fragment with no base URI
    // -----------------------------------------------------------------
    @Test
    public void testConstructor_withFragmentIdAndNullBaseUri_returnsNullCurrentUri() throws URISyntaxException {
        // schema node contains an id that is only a fragment
        ObjectNode root = mapper.createObjectNode();
        root.put("id", "#/localFragment");

        // currentUri is null – this mimics the situation where the schema file
        // does not have a base URI (e.g. a schema loaded from a string)
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root, null, false);

        // According to the private helper isUriFragmentWithNoContext(),
        // the resulting currentUri must be null.
        assertNull("When the id is a fragment and no base uri is supplied, currentUri must be null",
                schema.getCurrentUri());
    }

    // -----------------------------------------------------------------
    // 6️⃣  Expert C – walk() delegate (pre‑walk listener short‑circuit)
    // -----------------------------------------------------------------
    @Test
    public void testWalk_preWalkListenerCanSkipKeyword() {
        // Build a schema with a dummy keyword "x‑skip"
        ObjectNode root = mapper.createObjectNode();
        root.put("x-skip", true);

        // Prepare a mock walker that would be called if the listener does NOT skip
        JsonSchemaWalker mockWalker = mock(JsonSchemaWalker.class);
        when(genericValidator).thenReturn(null); // not used here

        // The keywordWalkListenerRunner is created inside the JsonSchema constructor.
        // We replace it with a stub that forces the pre‑walk listener to return true (skip)
        JsonSchema schema = buildSchema(root, URI.create("http://example.com/walk"));
        // inject a stub runner
        WalkListenerRunner stubRunner = mock(WalkListenerRunner.class);
        when(stubRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(), any(ValidationContext.class),
                any(JsonSchemaFactory.class))).thenReturn(true); // true → skip walking
        schema.keywordWalkListenerRunner = stubRunner;

        // The walker map is populated via the internal getValidators() call.
        // We manually put our mock walker for the keyword path.
        schema.getValidators().put(schema.getSchemaPath() + "/x-skip", mockWalker);

        // Execute walk – the pre‑walk listener should skip, therefore the walker
        // must never be invoked.
        JsonNode data = mapper.createObjectNode();
        ValidationResult result = schema.walk(data, false);
        assertTrue("No validation messages should be produced when walk is skipped", result.getMessages().isEmpty());
        verify(mockWalker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
    }

    // -----------------------------------------------------------------
    // Helper – simple stub for ValidationMessage (the real class is final)
    // -----------------------------------------------------------------
    // The library’s ValidationMessage is final and has no public constructor.
    // For the purpose of the tests we only need an object identity, therefore
    // we mock it.  The mock is sufficient because the test never inspects the
    // contents of the message – it only checks collection size / presence.
}
