package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.serialization.JsonMapperFactory;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    // Collaborators required by the JsonSchema constructor
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonSchemaFactory jsonSchemaFactory;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    // Maps used inside ValidationContext – we provide simple concrete instances
    private ConcurrentHashMap<String, JsonSchema> schemaResources;

    private ConcurrentHashMap<String, JsonSchema> dynamicAnchors;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the maps that ValidationContext will return
        schemaResources = new ConcurrentHashMap<>();
        dynamicAnchors = new ConcurrentHashMap<>();
        // Stub the behaviour of ValidationContext used by the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(schemaResources);
        when(validationContext.getDynamicAnchors()).thenReturn(dynamicAnchors);
        // The constructor asks for a resolved $id – we keep it simple and return null
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // Meta‑schema methods that are consulted for anchors – return null to avoid registration
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // initialise the maps that ValidationContext will hand out
        schemaResources = new ConcurrentHashMap<>();
        dynamicAnchors = new ConcurrentHashMap<>();
        // stub the minimal behaviour required by the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(schemaResources);
        when(validationContext.getDynamicAnchors()).thenReturn(dynamicAnchors);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // meta‑schema anchor handling – we simply return null to avoid side‑effects
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid cross‑test contamination
        schemaResources.clear();
        dynamicAnchors.clear();
        schemaResources.clear();
        dynamicAnchors.clear();
    }

    // -------------------------------------------------------------------------
    private JsonSchema createSchema(JsonNode schemaNode, SchemaLocation location, JsonNodePath path, JsonSchema parent, boolean suppress) {
        return JsonSchema.from(validationContext, location, path, schemaNode, parent, suppress);
    }

    @Test
    void testFromCreatesJsonSchemaInstance() {
        // Given – all collaborators are already prepared in @BeforeEach
        // When – invoke the package‑private factory method
        JsonSchema result = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // Then – a non‑null JsonSchema should be returned
        assertNotNull(result, "JsonSchema.from should return a non‑null instance");
        // Additional sanity checks (optional)
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema should retain the supplied ValidationContext");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with all non‑null arguments (suppressSubSchemaRetrieval = false)")
    void constructionAllNonNull() {
        // real objects (no need to mock these for the purpose of the test)
        ValidationContext realCtx = new ValidationContext();
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNodePath evalPath = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(realCtx, loc, evalPath, node, null, false);
        assertNotNull(schema, "Factory must return a non‑null JsonSchema");
        assertSame(realCtx, schema.getValidationContext(), "Returned schema should retain the supplied ValidationContext");
        assertNull(schema.findAncestor(), "When no parent is supplied, findAncestor() must return null");
        // The identifier of a schema is derived from its SchemaLocation – we expect it to match
        assertEquals(loc.getLocation(), schema.getId(), "Schema id should be the same as the supplied SchemaLocation");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with parent schema (suppressSubSchemaRetrieval = true)")
    void constructionWithParentSuppressTrue() {
        // parent schema (created with the same mocked context)
        SchemaLocation parentLoc = new SchemaLocation("classpath:/parent-schema.json");
        JsonNodePath parentPath = new JsonNodePath("$");
        JsonNode parentNode = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        JsonSchema parent = createSchema(parentNode, parentLoc, parentPath, null, false);
        // child schema
        SchemaLocation childLoc = new SchemaLocation("classpath:/child-schema.json");
        JsonNodePath childPath = new JsonNodePath("$.child");
        JsonNode childNode = JsonMapperFactory.getInstance().readTree("{\"type\":\"string\"}");
        JsonSchema child = createSchema(childNode, childLoc, childPath, parent, true);
        assertNotNull(child);
        assertSame(parent, child.findAncestor(), "findAncestor() must return the supplied parent schema");
        // The suppress flag influences $ref resolution – we verify via getRefSchema later
        assertNull(child.getRefSchema(JsonNodePath.of("#/definitions/name")), "With suppressSubSchemaRetrieval = true, getRefSchema should return null");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null ValidationContext should throw NPE")
    void nullValidationContext() {
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> JsonSchema.from(null, loc, path, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with empty schema node (no validation keywords)")
    void emptyObjectSchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/empty-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode emptyNode = JsonMapperFactory.getInstance().readTree("{}");
        JsonSchema schema = createSchema(emptyNode, loc, path, null, false);
        List<JsonValidator> validators = schema.getValidators();
        assertNotNull(validators, "Validators list must be non‑null");
        assertTrue(validators.isEmpty(), "An empty object schema should yield an empty validators list");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null JsonNode (malformed JSON) should throw IllegalArgumentException")
    void malformedSchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/bad-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, loc, path, null, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null SchemaLocation should throw NPE")
    void nullSchemaLocation() {
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, null, path, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null EvaluationPath should throw NPE")
    void nullEvaluationPath() {
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, loc, null, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null parent and suppressSubSchemaRetrieval = true")
    void nullParentSuppressTrue() {
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"type\":\"string\"}");
        JsonSchema schema = createSchema(node, loc, path, null, true);
        assertNotNull(schema);
        assertNull(schema.findAncestor(), "findAncestor() must be null when no parent is supplied");
        // suppress flag prevents $ref resolution – we verify that getRefSchema returns null
        assertNull(schema.getRefSchema(JsonNodePath.of("#/any")), "With suppress flag true, getRefSchema should return null");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with array schema node should yield empty validators")
    void arraySchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/array-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode arrayNode = JsonMapperFactory.getInstance().readTree("[\"a\",\"b\"]");
        JsonSchema schema = createSchema(arrayNode, loc, path, null, false);
        assertNotNull(schema);
        assertTrue(schema.getValidators().isEmpty(), "Array schema node does not contain validation keywords, validators list must be empty");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with boolean schema node (true) should succeed")
    void booleanSchemaNodeTrue() {
        SchemaLocation loc = new SchemaLocation("classpath:/bool-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode boolNode = JsonMapperFactory.getInstance().readTree("true");
        JsonSchema schema = createSchema(boolNode, loc, path, null, false);
        assertNotNull(schema);
        assertTrue(schema.getValidators().isEmpty(), "Boolean true schema should result in an empty validators list");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with parent schema and suppressSubSchemaRetrieval = false")
    void parentSchemaSuppressFalse() {
        // parent
        SchemaLocation parentLoc = new SchemaLocation("classpath:/parent-schema.json");
        JsonNodePath parentPath = new JsonNodePath("$");
        JsonNode parentNode = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        JsonSchema parent = createSchema(parentNode, parentLoc, parentPath, null, false);
        // child
        SchemaLocation childLoc = new SchemaLocation("classpath:/child-schema.json");
        JsonNodePath childPath = new JsonNodePath("$.child");
        JsonNode childNode = JsonMapperFactory.getInstance().readTree("{\"type\":\"integer\"}");
        JsonSchema child = createSchema(childNode, childLoc, childPath, parent, false);
        assertNotNull(child);
        assertSame(parent, child.findAncestor(), "findAncestor() must return the supplied parent schema");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("$ref resolution works when suppressSubSchemaRetrieval = false")
    void refResolutionNotSuppressed() {
        SchemaLocation loc = new SchemaLocation("classpath:/ref-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"$ref\":\"#/definitions/name\",\"definitions\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = createSchema(node, loc, path, null, false);
        // Resolve the reference
        JsonSchema refSchema = schema.getRefSchema(JsonNodePath.of("#/definitions/name"));
        assertNotNull(refSchema, "Reference should be resolved when suppression is false");
        // The resolved schema must have a type validator reporting "string"
        assertEquals("string", refSchema.getTypeValidator().getType(), "Resolved schema's type validator should report type \"string\"");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("$ref resolution is disabled when suppressSubSchemaRetrieval = true")
    void refResolutionSuppressed() {
        SchemaLocation loc = new SchemaLocation("classpath:/ref-schema-suppressed.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"$ref\":\"#/definitions/name\",\"definitions\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = createSchema(node, loc, path, null, true);
        JsonSchema refSchema = schema.getRefSchema(JsonNodePath.of("#/definitions/name"));
        assertNull(refSchema, "When suppressSubSchemaRetrieval is true, getRefSchema should return null");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with empty SchemaLocation should throw IllegalArgumentException")
    void emptySchemaLocation() {
        SchemaLocation emptyLoc = new SchemaLocation("");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"type\":\"object\"}");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, emptyLoc, path, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with numeric schema node should throw IllegalArgumentException")
    void numericSchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/numeric-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode numericNode = JsonMapperFactory.getInstance().readTree("123");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, loc, path, numericNode, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Resolving malformed $ref fragment should throw IllegalArgumentException")
    void malformedRefFragment() {
        SchemaLocation loc = new SchemaLocation("classpath:/bad-ref-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"$ref\":\"#/definitions/@@invalid@@\",\"definitions\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = createSchema(node, loc, path, null, false);
        // Attempt to resolve the malformed fragment
        assertThrows(IllegalArgumentException.class, () -> schema.getRefSchema(JsonNodePath.of("#/definitions/@@invalid@@")));
    }
}
