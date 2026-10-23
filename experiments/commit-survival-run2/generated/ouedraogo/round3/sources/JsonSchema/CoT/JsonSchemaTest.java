package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema}. The tests focus on the public API
 * and on edge‑case handling that can be exercised without needing the full
 * validation engine.  All external collaborators are mocked with Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonValidator validator;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // ---- ValidationContext basics -------------------------------------------------
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.activeDialect()).thenReturn(Optional.empty());

        // ---- JsonMetaSchema -----------------------------------------------------------
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        when(metaSchema.getNodeByFragmentRef(anyString(), any(JsonNode.class))).thenReturn(null);

        // ---- URIFactory ---------------------------------------------------------------
        when(uriFactory.create(any(URI.class), anyString())).thenAnswer(invocation -> {
            URI base = invocation.getArgument(0);
            String id = invocation.getArgument(1);
            // simple concatenation – enough for the tests that use it
            return new URI(base.toString() + id);
        });

        // ---- Config -------------------------------------------------------------------
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        // ---- JsonValidator ------------------------------------------------------------
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(validator);
        when(validator.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
    }

    /**
     * Helper to create a JsonSchema instance with a given schema node and optional base URI.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI baseUri) {
        return new JsonSchema(validationContext, "#", baseUri, schemaNode, null, false);
    }

    @Test
    public void testGetCurrentUri_Simple() throws Exception {
        URI base = new URI("http://example.com/schema.json");
        JsonNode emptySchema = mapper.readTree("{}");
        JsonSchema schema = createSchema(emptySchema, base);

        assertEquals("Current URI should be the base URI supplied to the constructor",
                base, schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUri_WithFragmentAndNoContext_ReturnsNull() throws Exception {
        // schemaNode contains an $id that is a fragment only.
        JsonNode schemaNode = mapper.readTree("{\"$id\":\"#/myFragment\"}");
        JsonSchema schema = createSchema(schemaNode, null);

        // According to combineCurrentUriWithIds, a fragment $id with a null currentUri yields null.
        assertNull("Current URI should be null when fragment $id is used without an existing base URI",
                schema.getCurrentUri());
    }

    @Test
    public void testFindAncestor_NoParent_ReturnsSelf() throws Exception {
        JsonNode schemaNode = mapper.readTree("{}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        JsonSchema ancestor = schema.findAncestor();

        assertSame("When there is no parent schema, findAncestor should return the instance itself",
                schema, ancestor);
    }

    @Test
    public void testHasRequiredValidator_Absent() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        // The schema does not contain a "required" keyword.
        assertFalse("hasRequiredValidator should be false when schema has no required keyword",
                schema.hasRequiredValidator());
    }

    @Test
    public void testInitializeValidators_PreloadsValidators() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        // Trigger lazy loading
        schema.getValidators();

        // Now call initializeValidators and verify that preloadJsonSchema() is invoked on each validator.
        schema.initializeValidators();

        verify(validator, times(1)).preloadJsonSchema();
    }

    @Test(expected = JsonSchemaException.class)
    public void testRecursiveAnchorInvalid_ThrowsException() throws Exception {
        // $recursiveAnchor must be a boolean, provide a non‑boolean to provoke the exception.
        JsonNode schemaNode = mapper.readTree("{\"$recursiveAnchor\":\"notABoolean\"}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        // Validation triggers reading of validators which will encounter the invalid $recursiveAnchor.
        schema.validate(mapper.readTree("{}"));
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerResolution() throws Exception {
        // Build a schema containing a property we will reference via JSON pointer.
        JsonNode schemaNode = mapper.readTree("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));

        JsonNode refNode = schema.getRefSchemaNode("#/properties/name");
        assertNotNull("Ref node should be resolved", refNode);
        assertEquals("type", "string", refNode.get("type").asText());
    }

    @Test
    public void testValidate_EmptySchema_ReturnsNoErrors() throws Exception {
        JsonNode emptySchema = mapper.readTree("{}");
        JsonSchema schema = createSchema(emptySchema, new URI("http://example.com/"));
        JsonNode data = mapper.readTree("{\"any\":\"value\"}");

        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue("Validation of an empty schema should produce no errors", errors.isEmpty());
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierIfProvided() throws Exception {
        // Provide a custom ExecutionContext supplier via config
        ExecutionContext suppliedContext = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> suppliedContext);

        JsonNode emptySchema = mapper.readTree("{}");
        JsonSchema schema = createSchema(emptySchema, new URI("http://example.com/"));

        ExecutionContext ctx = schema.createExecutionContext();
        assertSame("createExecutionContext should return the context supplied by the config supplier", suppliedContext, ctx);
    }

    @Test
    public void testToString_IncludesSchemaPathAndNode() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"integer\"}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        String str = schema.toString();

        assertTrue("toString should contain the schema path", str.contains("\"#\""));
        assertTrue("toString should contain the schema node representation", str.contains("\"type\""));
    }

    @Test
    public void testIsDynamicAnchor_DefaultsToFalse() throws Exception {
        JsonNode schemaNode = mapper.readTree("{}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        assertFalse("Dynamic anchor flag should be false when not present in schema", schema.isDynamicAnchor());
    }

    @Test
    public void testHasTypeValidator_TrueWhenTypePresent() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = createSchema(schemaNode, new URI("http://example.com/"));
        // Force validator loading
        schema.getValidators();

        assertTrue("hasTypeValidator should be true when schema contains a type keyword", schema.hasTypeValidator());
        assertNotNull("getTypeValidator should return a non‑null instance", schema.getTypeValidator());
    }
}
