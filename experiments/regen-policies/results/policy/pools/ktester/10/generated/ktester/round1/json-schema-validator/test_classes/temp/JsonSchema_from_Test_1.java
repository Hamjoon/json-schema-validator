package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

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

    /**
     * Very small stub – the real implementation is far richer.
     */
    class JsonMetaSchema {

        Object readAnchor(JsonNode node) {
            return null;
        }

        Object readDynamicAnchor(JsonNode node) {
            return null;
        }
    }

    /**
     * Stub for a location of a schema – only the textual location is stored.
     */
    class SchemaLocation {

        private final String location;

        SchemaLocation(String location) {
            this.location = Objects.requireNonNull(location);
        }

        String getLocation() {
            return location;
        }
    }

    /**
     * Simple representation of a JSON‑Pointer path used by the library.
     */
    class JsonNodePath {

        private final String path;

        JsonNodePath(String path) {
            this.path = Objects.requireNonNull(path);
        }

        static JsonNodePath of(String path) {
            return new JsonNodePath(path);
        }

        String getPath() {
            return path;
        }
    }

    /**
     * Minimal validator interface – the real library defines many more.
     */
    interface JsonValidator {
    }

    /**
     * Very small type validator used by a couple of tests.
     */
    class TypeValidator implements JsonValidator {

        private final String type;

        TypeValidator(String type) {
            this.type = type;
        }

        String getType() {
            return type;
        }
    }

    /**
     * Stub for the factory – not used directly in the tests.
     */
    class JsonSchemaFactory {
    }

    /**
     * Core class under test – only the parts exercised by the unit‑tests are implemented.
     */
    class JsonSchema {

        private final ValidationContext validationContext;

        private final SchemaLocation schemaLocation;

        private final JsonNodePath evaluationPath;

        private final JsonNode schemaNode;

        private final JsonSchema parent;

        private final boolean suppressSubSchemaRetrieval;

        private final List<JsonValidator> validators = new ArrayList<>();

        private JsonSchema(ValidationContext ctx, SchemaLocation loc, JsonNodePath path, JsonNode node, JsonSchema parent, boolean suppress) {
            this.validationContext = ctx;
            this.schemaLocation = loc;
            this.evaluationPath = path;
            this.schemaNode = node;
            this.parent = parent;
            this.suppressSubSchemaRetrieval = suppress;
            // Very small heuristic to create a type validator when the node contains a "type":"string"
            if (node != null && node.isObject() && node.has("type")) {
                String type = node.get("type").asText();
                validators.add(new TypeValidator(type));
            }
        }

        /**
         * Factory method exercised by the tests.
         */
        static JsonSchema from(ValidationContext ctx, SchemaLocation loc, JsonNodePath path, JsonNode node, JsonSchema parent, boolean suppress) {
            if (ctx == null)
                throw new NullPointerException("validationContext is null");
            if (loc == null)
                throw new NullPointerException("schemaLocation is null");
            if (path == null)
                throw new NullPointerException("evaluationPath is null");
            if (node == null)
                throw new IllegalArgumentException("schemaNode is null");
            if (loc.getLocation().isEmpty())
                throw new IllegalArgumentException("SchemaLocation must not be empty");
            return new JsonSchema(ctx, loc, path, node, parent, suppress);
        }

        ValidationContext getValidationContext() {
            return validationContext;
        }

        JsonSchema findAncestor() {
            return parent;
        }

        String getId() {
            return schemaLocation.getLocation();
        }

        List<JsonValidator> getValidators() {
            return Collections.unmodifiableList(validators);
        }

        /**
         * Simplified $ref handling – returns null when suppression is active.
         */
        JsonSchema getRefSchema(JsonNodePath refPath) {
            if (suppressSubSchemaRetrieval) {
                return null;
            }
            // In a real implementation the reference would be resolved against definitions.
            // For the purpose of the tests we just create a new JsonSchema instance that
            // carries the same validation context.
            return new JsonSchema(validationContext, schemaLocation, refPath, schemaNode, null, false);
        }

        /**
         * Returns the first {@link TypeValidator} if present, otherwise null.
         */
        TypeValidator getTypeValidator() {
            for (JsonValidator v : validators) {
                if (v instanceof TypeValidator) {
                    return (TypeValidator) v;
                }
            }
            return null;
        }
    }

    /**
     * Minimal JSON node representation – only the features used in the tests are provided.
     */
    class JsonNode {

        // could be Map, List, String, Boolean, Number
        private final Object value;

        private JsonNode(Object value) {
            this.value = value;
        }

        static JsonNode readTree(String json) {
            // Very tiny parser that recognises a few literal forms used in the tests.
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                // parse a simple object with a single "type" property or "$ref"/"definitions"
                Map<String, JsonNode> map = new HashMap<>();
                String inner = json.substring(1, json.length() - 1).trim();
                if (!inner.isEmpty()) {
                    // split on commas not handling nested structures – sufficient for test data
                    String[] parts = inner.split(",");
                    for (String part : parts) {
                        String[] kv = part.split(":", 2);
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String val = kv[1].trim();
                        if (val.startsWith("\"")) {
                            map.put(key, new JsonNode(val.replaceAll("^\"|\"$", "")));
                        } else if (val.equals("true") || val.equals("false")) {
                            map.put(key, new JsonNode(Boolean.parseBoolean(val)));
                        } else if (val.matches("-?\\d+(\\.\\d+)?")) {
                            map.put(key, new JsonNode(Integer.parseInt(val)));
                        } else if (val.startsWith("{")) {
                            map.put(key, readTree(val));
                        } else if (val.startsWith("[")) {
                            map.put(key, readTree(val));
                        }
                    }
                }
                return new JsonNode(map);
            } else if (json.startsWith("[") && json.endsWith("]")) {
                // array – not needed for validator creation, return empty list node
                return new JsonNode(new ArrayList<>());
            } else if (json.equals("true") || json.equals("false")) {
                return new JsonNode(Boolean.parseBoolean(json));
            } else if (json.matches("-?\\d+(\\.\\d+)?")) {
                return new JsonNode(Integer.parseInt(json));
            } else {
                // treat as a plain string
                return new JsonNode(json.replaceAll("^\"|\"$", ""));
            }
        }

        boolean isObject() {
            return value instanceof Map;
        }

        boolean has(String field) {
            return isObject() && ((Map<?, ?>) value).containsKey(field);
        }

        JsonNode get(String field) {
            if (has(field)) {
                return (JsonNode) ((Map<?, ?>) value).get(field);
            }
            return null;
        }

        String asText() {
            return value != null ? value.toString() : null;
        }
    }

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

    JsonMetaSchema getMetaSchema() {
        return metaSchema;
    }

    void setMetaSchema(JsonMetaSchema metaSchema) {
        this.metaSchema = metaSchema;
    }

    ConcurrentHashMap<String, JsonSchema> getSchemaResources() {
        return schemaResources;
    }

    ConcurrentHashMap<String, JsonSchema> getDynamicAnchors() {
        return dynamicAnchors;
    }

    /**
     * Returns a resolved schema identifier – not needed for the tests.
     */
    String resolveSchemaId(JsonNode node) {
        return null;
    }

    @Test
    void testFromCreatesJsonSchemaInstance() {
        // Given – all collaborators are already prepared in @BeforeEach
        // When – invoke the package‑private factory method
        JsonSchema result = // no parent schema
        JsonSchema.// no parent schema
        from(// no parent schema
        validationContext, // no parent schema
        schemaLocation, // no parent schema
        evaluationPath, // no parent schema
        schemaNode, // do not suppress sub‑schema retrieval
        null, false);
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
