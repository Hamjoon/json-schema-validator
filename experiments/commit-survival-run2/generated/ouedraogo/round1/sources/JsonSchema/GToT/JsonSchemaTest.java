package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchema}.
 *
 * The tests are written from the perspective of three “experts”:
 *   1. Public‑API extraction & basic happy‑path tests.
 *   2. Edge‑case / exception handling identification.
 *   3. Integration of the edge‑case tests with the basic ones.
 *
 * All tests run in the same package (com.networknt.schema) so that
 * package‑private members (e.g. {@code JsonSchema.from}) are accessible.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    /** Common test fixtures */
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private Config config;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private JsonValidator validator;

    @Captor
    private ArgumentCaptor<String> schemaPathCaptor;

    private JsonSchema schema;   // the instance under test

    @Before
    public void setUp() throws Exception {
        // --------------------------------------------------------------------
        // Basic stubbing that is required by many tests
        // --------------------------------------------------------------------
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // By default the validator factory returns null (no validator) – this yields an empty validator map.
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(null);
        when(validationContext.activeDialect()).thenReturn(java.util.Optional.empty());
    }

    // ------------------------------------------------------------------------
    // 1. Basic public‑method tests (happy path)
    // ------------------------------------------------------------------------

    @Test
    public void testGetCurrentUri_NoId_ReturnsProvidedUri() {
        URI base = URI.create("http://example.com/schema.json");
        ObjectNode schemaNode = mapper.createObjectNode(); // empty – no $id

        // resolveSchemaId returns null → combineCurrentUriWithIds returns the original URI
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);

        schema = new JsonSchema(validationContext, "#", base, schemaNode, null, false);
        assertEquals(base, schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUri_FragmentNoContext_ReturnsNull() {
        // No base URI (null) and schema contains a fragment id "#/defs/foo"
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$id", "#/defs/foo");

        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("#/defs/foo");

        // Pass null as currentUri – the private helper should detect the fragment‑without‑context case
        schema = new JsonSchema(validationContext, "#", null, schemaNode, null, false);
        assertNull("Current URI should be null when id is a fragment and there is no base URI", schema.getCurrentUri());
    }

    @Test
    public void testInitializeValidators_CallsPreloadExactlyOnce() {
        // Build a schema node that will produce a validator (e.g. a simple "type": "string")
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "string");

        // The factory must return a real validator mock for the "type" keyword
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator);

        // Ensure the validator reports no errors
        when(validator.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Build the JsonSchema instance
        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), schemaNode, null, false);

        // First call – validators are lazy‑loaded, so preloadJsonSchema should be invoked now
        schema.initializeValidators();
        verify(validator, times(1)).preloadJsonSchema();

        // Second call – the method must be idempotent, no further invocation
        schema.initializeValidators();
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void testValidate_EmptySchema_ReturnsNoErrors() {
        // Empty object – no validators are created
        ObjectNode schemaNode = mapper.createObjectNode();

        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), schemaNode, null, false);
        JsonNode data = mapper.createObjectNode(); // any data

        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue("Validation of an empty schema must succeed", errors.isEmpty());
    }

    @Test
    public void testHasRequiredValidator_FalseWhenAbsent() {
        ObjectNode schemaNode = mapper.createObjectNode(); // no "required"
        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), schemaNode, null, false);
        assertFalse(schema.hasRequiredValidator());
    }

    @Test
    public void testIsDynamicAnchor_TrueWhenRecursiveAnchorTrue() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$recursiveAnchor", true);
        // The validator factory is not used for $recursiveAnchor, so we can keep default stubbing
        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), schemaNode, null, false);
        assertTrue("Dynamic anchor flag must be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testToString_Format() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "integer");
        schema = new JsonSchema(validationContext, "#/properties/age", URI.create("http://example.com"), schemaNode, null, false);
        String expected = "\"#/properties/age\" : " + schemaNode.toString();
        assertEquals(expected, schema.toString());
    }

    // ------------------------------------------------------------------------
    // 2. Edge‑case / exception handling tests
    // ------------------------------------------------------------------------

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_InvalidId_ThrowsJsonSchemaException() {
        // Simulate an invalid URI that causes the URIFactory to throw IllegalArgumentException
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$id", "ht!tp://[invalid]");

        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("ht!tp://[invalid]");
        when(uriFactory.create(any(URI.class), eq("ht!tp://[invalid]")))
                .thenThrow(new IllegalArgumentException("Bad URI"));

        // Config must return a ResourceBundle with a dummy message for the exception constructor
        ResourceBundle dummyBundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) { return "Invalid $id"; }
            @Override
            public java.util.Enumeration<String> getKeys() { return java.util.Collections.emptyEnumeration(); }
        };
        when(config.getResourceBundle()).thenReturn(dummyBundle);

        // Construction should trigger the exception
        new JsonSchema(validationContext, "#", URI.create("http://example.com"), schemaNode, null, false);
    }

    @Test
    public void testValidateAndCollect_LoadsCollectorsWhenConfigured() {
        // Build a schema that returns a validator which produces a single error
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "string");

        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator);
        ValidationMessage dummyMessage = ValidationMessage.of("type", ValidatorTypeCode.TYPE, "type error");
        when(validator.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singleton(dummyMessage)));

        // Enable collector loading in the config
        when(config.doLoadCollectors()).thenReturn(true);
        CollectorContext collectorContext = mock(CollectorContext.class);
        ExecutionContext execCtx = new ExecutionContext(collectorContext);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getExecutionContextSupplier()).thenReturn(null);

        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), schemaNode, null, false);
        ValidationResult result = schema.validateAndCollect(execCtx, mapper.readTree("\"any\""));
        assertFalse("ValidationResult should contain errors", result.getValidationMessages().isEmpty());

        // Verify that collectorContext.loadCollectors() was invoked because config.doLoadCollectors() == true
        verify(collectorContext, times(1)).loadCollectors();
    }

    // ------------------------------------------------------------------------
    // 3. Integration of basic & edge cases into a single test suite
    // ------------------------------------------------------------------------

    @Test
    public void testCreateChildSchema_ProducesIndependentInstance() {
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("type", "object");
        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), parentNode, null, false);

        ObjectNode childNode = mapper.createObjectNode();
        childNode.put("type", "string");
        JsonSchema child = schema.createChildSchema("#/properties/name", childNode);

        assertNotSame("Child schema must be a different instance", schema, child);
        assertEquals("#/properties/name", child.getSchemaPath());
        assertEquals(childNode, child.getSchemaNode());
    }

    @Test
    public void testFindAncestor_AtRootReturnsSelf() {
        ObjectNode rootNode = mapper.createObjectNode();
        schema = new JsonSchema(validationContext, "#", URI.create("http://example.com"), rootNode, null, false);
        assertSame("Root schema should be its own ancestor", schema, schema.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_StopsAtSchemaWithId() {
        // parent schema with $id
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("$id", "http://example.com/parent");
        JsonSchema parent = new JsonSchema(validationContext, "#", URI.create("http://example.com/parent.json"),
                parentNode, null, false);
        // child without $id
        ObjectNode childNode = mapper.createObjectNode();
        JsonSchema child = new JsonSchema(validationContext, "#/properties/child", URI.create("http://example.com/parent.json"),
                childNode, parent, false);

        assertSame("Lexical root should be the ancestor that has an $id", parent, child.findLexicalRoot());
    }
}
