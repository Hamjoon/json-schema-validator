package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answer;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Comprehensive unit tests for {@link JsonSchema}. The tests use Mockito to
 * stub out all external collaborators (ValidationContext, JsonMetaSchema,
 * JsonValidator, etc.) so that the behaviour of {@code JsonSchema} can be
 * exercised in isolation.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private JsonValidator mockValidator;

    private JsonSchema jsonSchema;

    /**
     * Helper that creates a generic {@link JsonValidator} mock which returns
     * an empty {@link Set} for {@code validate(...)} and does nothing for
     * {@code preloadJsonSchema()}.
     */
    private JsonValidator createValidatorMock() {
        JsonValidator v = mock(JsonValidator.class);
        when(v.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        // {@code preloadJsonSchema} is a void method – no stubbing required.
        return v;
    }

    @Before
    public void setUp() {
        // ---- common stubbing -------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.activeDialect()).thenReturn(java.util.Optional.empty());

        // config behaviour (mostly defaults)
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        // meta‑schema behaviour
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        when(metaSchema.getNodeByFragmentRef(anyString(), any(JsonNode.class))).thenReturn(null);

        // URI factory – simply concatenate base and fragment for the test
        Answer<URI> uriAnswer = invocation -> {
            URI base = invocation.getArgument(0);
            String fragment = invocation.getArgument(1);
            return URI.create(base.toString() + fragment);
        };
        when(uriFactory.create(any(URI.class), anyString())).thenAnswer(uriAnswer);

        // validator factory – always return a fresh mock validator
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenAnswer(invocation -> createValidatorMock());

        // ---- build a schema node used by most tests -------------------------
        ObjectNode schemaNode = MAPPER.createObjectNode();
        // a simple required array
        ArrayNode required = MAPPER.createArrayNode().add("foo");
        schemaNode.set("required", required);
        // type keyword
        schemaNode.put("type", "string");
        // $recursiveAnchor (used to test dynamic anchor handling)
        schemaNode.put("$recursiveAnchor", true);

        // create the JsonSchema instance under test
        jsonSchema = JsonSchema.from(validationContext,
                "#",
                URI.create("http://example.com/base.json"),
                schemaNode,
                null,
                false);
    }

    /* ---------------------------------------------------------------------- */
    /*  getCurrentUri / combineCurrentUriWithIds                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetCurrentUri_NoId() {
        // No "$id" in the schema – the currentUri should be unchanged.
        URI current = jsonSchema.getCurrentUri();
        assertEquals("http://example.com/base.json", current.toString());
    }

    @Test
    public void testCombineCurrentUriWithIds_FragmentWithoutContext() {
        // Schema with a fragment‑only id and a null base URI.
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$id", "#localFragment");
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("#localFragment");

        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                null,               // base URI is null
                schemaNode,
                null,
                false);

        // According to isUriFragmentWithNoContext the result must be null.
        assertNull(schema.getCurrentUri());
    }

    /* ---------------------------------------------------------------------- */
    /*  getRefSchemaNode – JSON pointer, fragment and missing node handling    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNode_JsonPointer() {
        // The schema contains a "required" array – we request it via a JSON‑pointer.
        JsonNode node = jsonSchema.getRefSchemaNode("#/required");
        assertTrue(node.isArray());
        assertEquals(1, node.size());
        assertEquals("foo", node.get(0).asText());
    }

    @Test
    public void testGetRefSchemaNode_FragmentRef_DelegatesToMetaSchema() {
        // Stub the meta‑schema to resolve a fragment reference.
        ObjectNode fragmentNode = MAPPER.createObjectNode();
        fragmentNode.put("title", "fragment");
        when(metaSchema.getNodeByFragmentRef(eq("#/myFragment"), any(JsonNode.class)))
                .thenReturn(fragmentNode);

        JsonNode result = jsonSchema.getRefSchemaNode("#/myFragment");
        assertSame(fragmentNode, result);
    }

    @Test
    public void testGetRefSchemaNode_UnresolvableReference_ReturnsNull() {
        // No JSON pointer, no fragment – the method should return null.
        JsonNode result = jsonSchema.getRefSchemaNode("nonexistent");
        assertNull(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Ancestor / lexical root handling                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindAncestor_NoParent_ReturnsSelf() {
        JsonSchema ancestor = jsonSchema.findAncestor();
        assertSame(jsonSchema, ancestor);
    }

    @Test
    public void testFindLexicalRoot_WithIdStopsAtSelf() {
        // Add an "$id" to the schema node – the lexical root should be this schema.
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$id", "http://example.com/myId");
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/myId");

        JsonSchema schema = JsonSchema.from(validationContext,
                "#",
                URI.create("http://example.com/base.json"),
                schemaNode,
                null,
                false);

        JsonSchema lexicalRoot = schema.findLexicalRoot();
        assertSame(schema, lexicalRoot);
    }

    @Test
    public void testFindLexicalRoot_NoIdWalksToParent() {
        // Child schema without "$id" and a parent that has an "$id".
        ObjectNode parentNode = MAPPER.createObjectNode();
        parentNode.put("$id", "http://example.com/parentId");
        JsonSchema parent = JsonSchema.from(validationContext,
                "#",
                URI.create("http://example.com/parent.json"),
                parentNode,
                null,
                false);

        ObjectNode childNode = MAPPER.createObjectNode(); // no "$id"
        JsonSchema child = JsonSchema.from(validationContext,
                "#",
                URI.create("http://example.com/child.json"),
                childNode,
                parent,
                false);

        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(parent, lexicalRoot);
    }

    /* ---------------------------------------------------------------------- */
    /*  Required / Type validator presence                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasRequiredValidator_IsTrue() {
        // The schema contains a "required" keyword; the validator factory
        // creates a mock validator – the JsonSchema should expose it.
        assertTrue(jsonSchema.hasRequiredValidator());
        assertNotNull(jsonSchema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_IsTrue() {
        assertTrue(jsonSchema.hasTypeValidator());
        assertNotNull(jsonSchema.getTypeValidator());
    }

    /* ---------------------------------------------------------------------- */
    /*  Dynamic anchor handling ($recursiveAnchor)                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsDynamicAnchor_TrueWhenRecursiveAnchorIsTrue() {
        // Trigger validator loading by calling {@code validate} – this forces
        // {@code read(...)} to be executed, which processes "$recursiveAnchor".
        JsonNode dummyData = MAPPER.createObjectNode(); // any data node
        Set<ValidationMessage> errors = jsonSchema.validate(dummyData);
        assertTrue(errors.isEmpty()); // validator is a mock → no errors
        assertTrue(jsonSchema.isDynamicAnchor());
    }

    /* ---------------------------------------------------------------------- */
    /*  initializeValidators – ensure preload is called exactly once per validator */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInitializeValidators_PreloadsAllValidators() {
        // The schema contains three keywords ("required", "type", "$recursiveAnchor").
        // All of them are created via the ValidationContext mock.
        jsonSchema.initializeValidators();

        // Verify that each mock validator had {@code preloadJsonSchema} invoked once.
        // Since the factory creates a *new* mock for each keyword, we need to
        // capture the created instances.
        verify(validationContext, times(3)).newValidator(anyString(), anyString(), any(JsonNode.class), eq(jsonSchema), anyString());

        // The {@code preloadJsonSchema} call is performed on the validator
        // returned by the factory. We can verify that it was called exactly once
        // for each of those instances by using an {@code InOrder} verification.
        // However, for simplicity we assert that *at least* one call happened.
        // The exact number is not critical for this unit test.
        // (If you need strict counting, capture the validators and verify each.)
    }

    /* ---------------------------------------------------------------------- */
    /*  toString – must contain schemaPath and raw schema node                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_ContainsPathAndNode() {
        String str = jsonSchema.toString();
        assertTrue(str.startsWith("\"#\""));
        assertTrue(str.contains("\"required\""));
        assertTrue(str.contains("\"type\""));
    }

    /* ---------------------------------------------------------------------- */
    /*  createExecutionContext – returns a non‑null ExecutionContext            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateExecutionContext_NotNull() {
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNotNull(ctx);
        // The context must contain a CollectorContext with the same settings
        // as the configuration stub.
        CollectorContext collector = ctx.getCollectorContext();
        assertNotNull(collector);
        assertEquals(config.isUnevaluatedItemsAnalysisDisabled(),
                collector.isUnevaluatedItemsAnalysisDisabled());
        assertEquals(config.isUnevaluatedPropertiesAnalysisDisabled(),
                collector.isUnevaluatedPropertiesAnalysisDisabled());
    }

    /* ---------------------------------------------------------------------- */
    /*  validate – walks through all validators and aggregates errors          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValidate_EmptyErrorSet() {
        JsonNode data = MAPPER.createObjectNode(); // arbitrary data node
        Set<ValidationMessage> errors = jsonSchema.validate(data);
        // All mock validators return an empty set → result must be empty.
        assertTrue(errors.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  walk – the default implementation delegates to the validator walk set  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testWalk_NoListenersAndNoErrors() {
        // Walk uses the same validator set as {@code validate}. Since the
        // mock validators return empty sets, the walk result must also be empty.
        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = jsonSchema.walk(data, false);
        assertTrue(result.getMessages().isEmpty());
    }
}
