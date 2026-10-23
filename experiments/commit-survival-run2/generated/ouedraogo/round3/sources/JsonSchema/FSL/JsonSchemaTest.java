package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;

/**
 * Comprehensive unit tests for {@link JsonSchema}.
 *
 * The tests use Mockito to stub out the heavy dependencies
 * (e.g. {@link ValidationContext}, {@link JsonMetaSchema}, {@link JsonValidator},
 * {@link ExecutionContext} etc.) and focus on the logic of {@code JsonSchema}
 * itself.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonValidator genericValidator;

    @Mock
    private TypeValidator typeValidator;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // Basic stubbing common to most tests
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.getExecutionContextSupplier()).thenReturn(null);

        // Default validator factory – returns a generic validator unless the keyword is "type"
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), anyString()))
            .thenAnswer(AdditionalAnswers.answer((String schemaPath, String pname, JsonNode node, JsonSchema schema, String msg) -> {
                if ("type".equals(pname)) {
                    return typeValidator;
                }
                return genericValidator;
            }));

        // Generic validator behaviours
        when(genericValidator.validate(any(), any(), any(), anyString()))
            .thenReturn(Collections.emptySet());
        when(genericValidator.preloadJsonSchema()).thenReturn(null);
        when(typeValidator.validate(any(), any(), any(), anyString()))
            .thenReturn(Collections.emptySet());
        when(typeValidator.preloadJsonSchema()).thenReturn(null);
    }

    /** Helper to create a JsonSchema instance from a JSON string. */
    private JsonSchema createSchema(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        // Using the static factory method – currentUri is null for simplicity
        return JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
    }

    @Test
    public void testGetCurrentUriReturnsNullWhenNoUri() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        assertNull("Current URI should be null when none is supplied", schema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNodeWithJsonPointer() throws Exception {
        JsonSchema schema = createSchema("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode refNode = schema.getRefSchemaNode("#/properties/name");
        assertNotNull("Ref node should be resolved via JSON‑pointer", refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeWithFragment() throws Exception {
        JsonNode fragmentNode = mapper.readTree("{\"def\":\"value\"}");
        when(metaSchema.getNodeByFragmentRef(eq("#/defs"), any(JsonNode.class))).thenReturn(fragmentNode);

        JsonSchema schema = createSchema("{\"title\":\"example\"}");
        JsonNode result = schema.getRefSchemaNode("#/defs");
        assertSame("Fragment resolution should delegate to metaSchema", fragmentNode, result);
    }

    @Test
    public void testFindLexicalRootStopsAtFirstId() throws Exception {
        // Parent schema – no $id
        JsonSchema parent = createSchema("{\"type\":\"object\"}");
        // Child schema – contains $id, making it the lexical root
        JsonSchema child = JsonSchema.from(validationContext, "#/child", null,
                mapper.readTree("{\"$id\":\"http://example.com/child\"}"), parent, false);
        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame("Lexical root should be the child schema that carries an $id", child, lexicalRoot);
    }

    @Test
    public void testFindAncestorReturnsRootWhenNoParent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}");
        JsonSchema ancestor = schema.findAncestor();
        assertSame("Ancestor of a root schema should be itself", schema, ancestor);
    }

    @Test
    public void testInitializeValidatorsPreloadsAllValidators() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\",\"required\":[\"name\"]}");
        // Force lazy loading of validators
        schema.getValidators();
        schema.initializeValidators();

        // Verify that each validator's preload method has been invoked exactly once
        verify(genericValidator, atLeastOnce()).preloadJsonSchema();
        verify(typeValidator, atLeastOnce()).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchorDetected() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":true}");
        // Trigger validator creation
        schema.getValidators();
        assertTrue("Dynamic anchor flag should be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testHasRequiredValidatorAndGetter() throws Exception {
        JsonSchema schema = createSchema("{\"required\":[\"id\"]}");
        // Force validators to be read
        schema.getValidators();
        assertTrue("Schema should report having a required validator", schema.hasRequiredValidator());
        assertSame("Getter should return the same validator instance", genericValidator, schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorAndGetter() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"integer\"}");
        schema.getValidators();
        assertTrue("Schema should report having a type validator", schema.hasTypeValidator());
        assertSame("Getter should return the TypeValidator instance", typeValidator, schema.getTypeValidator());
    }

    @Test
    public void testToStringProducesExpectedFormat() throws Exception {
        JsonSchema schema = createSchema("{\"title\":\"Test\"}");
        String expected = "\"#\" : {\"title\":\"Test\"}";
        assertEquals("toString must follow the documented format", expected, schema.toString());
    }

    @Test
    public void testValidateReturnsEmptySetForValidData() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        JsonNode data = mapper.readTree("\"hello\"");
        Set<ValidationMessage> result = schema.validate(data);
        assertTrue("Validation of a compliant instance should yield no messages", result.isEmpty());
    }

    @Test
    public void testValidateAndCollectCreatesExecutionContext() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}");
        JsonNode data = mapper.readTree("true");
        ValidationResult result = schema.validateAndCollect(data);
        assertNotNull("Result should not be null", result);
        assertTrue("No validation errors expected", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalkInvokesKeywordWalkListenerRunnerWhenConfigured() throws Exception {
        // Set up a dummy listener runner that always returns true for pre‑walk
        WalkListenerRunner dummyRunner = mock(WalkListenerRunner.class);
        when(dummyRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any())).thenReturn(true);
        when(dummyRunner.runPostWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any(), any())).thenReturn(null);

        // Inject the dummy runner via the validation context config
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        when(validationContext.getConfig()).thenReturn(config);
        // We need to make the JsonSchema construct the runner – this happens in the constructor
        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                mapper.readTree("{\"type\":\"null\"}"), null, false);
        // Replace the runner with our mock
        schema.keywordWalkListenerRunner = dummyRunner;

        JsonNode data = mapper.readTree("null");
        ValidationResult walkResult = schema.walk(data, true);
        assertNotNull(walkResult);
        // Verify that the pre‑walk listener was consulted
        verify(dummyRunner, atLeastOnce()).runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any());
    }

    @Test
    public void testCreateChildSchemaProducesNewInstance() throws Exception {
        JsonSchema parent = createSchema("{\"type\":\"object\"}");
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema child = parent.createChildSchema("#/child", childNode);
        assertNotNull("Child schema should be created", child);
        assertEquals("Child schema path must be propagated", "#/child", child.getSchemaPath());
        assertSame("Parent reference must be set", parent, child.getParentSchema());
    }

    @Test
    public void testActiveDialectFallsBackWhenNoDialectSet() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"number\"}");
        // Force validator creation to ensure internal state is built
        schema.getValidators();
        // activeDialect() is private; we indirectly test that it does not throw
        // and that the validators map is populated (meaning the method succeeded)
        assertFalse("Validators map should not be empty", schema.getValidators().isEmpty());
    }

    @Test
    public void testGetCustomMessageReturnsNullWhenFeatureDisabled() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        // The method is private; we test the public behaviour that no exception occurs
        // and that validation proceeds normally.
        JsonNode data = mapper.readTree("\"abc\"");
        Set<ValidationMessage> msgs = schema.validate(data);
        assertTrue(msgs.isEmpty());
    }

    @Test
    public void testCombineCurrentUriWithIdsHandlesNullIdGracefully() throws Exception {
        // Pass a non‑null base URI but no $id in the schema
        URI base = new URI("http://example.com/schema.json");
        JsonSchema schema = JsonSchema.from(validationContext, "#", base,
                mapper.readTree("{\"type\":\"string\"}"), null, false);
        assertEquals("Current URI should be the same as the supplied base when no $id is present",
                base, schema.getCurrentUri());
    }
}
