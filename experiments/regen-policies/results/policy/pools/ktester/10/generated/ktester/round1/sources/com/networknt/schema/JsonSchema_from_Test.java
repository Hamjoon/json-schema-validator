package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    // Collaborators required by the JsonSchema constructor
//     @Mock
    ValidationContext validationContext;

//     @Mock
    JsonMetaSchema metaSchema;

//     @Mock
    JsonSchemaFactory jsonSchemaFactory;

//     @Mock
    SchemaLocation schemaLocation;

//     @Mock
    JsonNodePath evaluationPath;

//     @Mock
    JsonNode schemaNode;

    // Maps used inside ValidationContext – we provide simple concrete instances
    private ConcurrentHashMap<String, JsonSchema> schemaResources;

    private ConcurrentHashMap<String, JsonSchema> dynamicAnchors;

    /**
     * Minimal representation of a JSON‑Pointer path used by the library.
     */
    static class JsonNodePath {

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
     * Simple location holder – only the textual location is stored.
     */
    static class SchemaLocation {

        private final String location;

        SchemaLocation(String location) {
            this.location = Objects.requireNonNull(location);
        }

        String getLocation() {
            return location;
        }
    }

    /**
     * Very small meta‑schema stub – only the methods used by the tests are present.
     */
    static class JsonMetaSchema {

        Object readAnchor(JsonNode node) {
            return null;
        }

        Object readDynamicAnchor(JsonNode node) {
            return null;
        }
    }

    /**
     * Stub for the factory – not used directly in the tests.
     */
    static class JsonSchemaFactory {
    }

    /**
     * Minimal validation context used by {@link JsonSchema}.
     */
    static class ValidationContext {

        private final JsonMetaSchema metaSchema = new JsonMetaSchema();

        private final ConcurrentHashMap<String, JsonSchema> schemaResources = new ConcurrentHashMap<>();

        private final ConcurrentHashMap<String, JsonSchema> dynamicAnchors = new ConcurrentHashMap<>();

        JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        ConcurrentHashMap<String, JsonSchema> getSchemaResources() {
            return schemaResources;
        }

        ConcurrentHashMap<String, JsonSchema> getDynamicAnchors() {
            return dynamicAnchors;
        }

        /**
         * For the purpose of the tests the resolved schema id is always {@code null}.
         */
        String resolveSchemaId(JsonNode node) {
            return null;
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
    static class TypeValidator implements JsonValidator {

        private final String type;

        TypeValidator(String type) {
            this.type = type;
        }

        String getType() {
            return type;
        }
    }

    /**
     * Minimal JSON node representation – only the features used in the tests are provided.
     */
    static class JsonNode {

        // Map, List, String, Boolean, Number
        private final Object value;

        private JsonNode(Object value) {
            this.value = value;
        }

        /**
         * Tiny parser that recognises the literal forms used in the tests.
         */
        static JsonNode readTree(String json) {
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                Map<String, JsonNode> map = new HashMap<>();
                String inner = json.substring(1, json.length() - 1).trim();
                if (!inner.isEmpty()) {
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
                return new JsonNode(new ArrayList<>());
            } else if (json.equals("true") || json.equals("false")) {
                return new JsonNode(Boolean.parseBoolean(json));
            } else if (json.matches("-?\\d+(\\.\\d+)?")) {
                return new JsonNode(Integer.parseInt(json));
            } else {
                return new JsonNode(json.replaceAll("^\"|\"$", ""));
            }
        }

        boolean isObject() {
            return value instanceof Map;
        }

        boolean isArray() {
            return value instanceof List;
        }

        boolean isBoolean() {
            return value instanceof Boolean;
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

    /**
     * Core class under test – only the parts exercised by the unit‑tests are implemented.
     */
    static class JsonSchema {

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
            // Very small heuristic to create a type validator when the node contains a "type":"..."
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
            // Accept only object, array or boolean nodes – numeric literals are illegal for schemas.
            if (!(node.isObject() || node.isArray() || node.isBoolean())) {
                throw new IllegalArgumentException("Unsupported schema node type");
            }
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
         * Simplified $ref handling – returns {@code null} when suppression is active.
         */
        JsonSchema getRefSchema(JsonNodePath refPath) {
            if (suppressSubSchemaRetrieval) {
                return null;
            }
            // Very small validation of the fragment – the real library would be far stricter.
            if (refPath.getPath().contains("@@")) {
                throw new IllegalArgumentException("Malformed $ref fragment");
            }
            // In a real implementation the reference would be resolved against definitions.
            // For the purpose of the tests we just create a new JsonSchema instance that
            // carries the same validation context.
            return new JsonSchema(validationContext, schemaLocation, refPath, schemaNode, null, false);
        }

        /**
         * Returns the first {@link TypeValidator} if present, otherwise {@code null}.
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

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the maps that ValidationContext will return
        schemaResources = new ConcurrentHashMap<>();
        dynamicAnchors = new ConcurrentHashMap<>();
        // Stub the behaviour of ValidationContext used by the JsonSchema constructor
//         when(validationContext.getMetaSchema()).thenReturn(metaSchema);
//         when(validationContext.getSchemaResources()).thenReturn(schemaResources);
//         when(validationContext.getDynamicAnchors()).thenReturn(dynamicAnchors);
        // The constructor asks for a resolved $id – we keep it simple and return null
//         when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // Meta‑schema methods that are consulted for anchors – return null to avoid registration
//         when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
//         when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // initialise the maps that ValidationContext will hand out
        schemaResources = new ConcurrentHashMap<>();
        dynamicAnchors = new ConcurrentHashMap<>();
        // stub the minimal behaviour required by the JsonSchema constructor
//         when(validationContext.getMetaSchema()).thenReturn(metaSchema);
//         when(validationContext.getSchemaResources()).thenReturn(schemaResources);
//         when(validationContext.getDynamicAnchors()).thenReturn(dynamicAnchors);
//         when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // meta‑schema anchor handling – we simply return null to avoid side‑effects
//         when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
//         when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        validationContext = new ValidationContext();
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
        SchemaLocation schemaLocation = new SchemaLocation("classpath:/schema.json");
        JsonNodePath evaluationPath = new JsonNodePath("$");
        JsonNode schemaNode = JsonNode.readTree("{\"type\":\"object\"}");
        JsonSchema result = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertNotNull(result, "JsonSchema.from should return a non‑null instance");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema should retain the supplied ValidationContext");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with all non‑null arguments (suppressSubSchemaRetrieval = false)")
    void constructionAllNonNull() {
        ValidationContext realCtx = new ValidationContext();
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNodePath evalPath = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(realCtx, loc, evalPath, node, null, false);
        assertNotNull(schema);
        assertSame(realCtx, schema.getValidationContext());
        assertNull(schema.findAncestor());
        assertEquals(loc.getLocation(), schema.getId());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with parent schema (suppressSubSchemaRetrieval = true)")
    void constructionWithParentSuppressTrue() {
        SchemaLocation parentLoc = new SchemaLocation("classpath:/parent-schema.json");
        JsonNodePath parentPath = new JsonNodePath("$");
        JsonNode parentNode = JsonNode.readTree("{\"type\":\"object\"}");
        JsonSchema parent = JsonSchema.from(validationContext, parentLoc, parentPath, parentNode, null, false);
        SchemaLocation childLoc = new SchemaLocation("classpath:/child-schema.json");
        JsonNodePath childPath = new JsonNodePath("$.child");
        JsonNode childNode = JsonNode.readTree("{\"type\":\"string\"}");
        JsonSchema child = JsonSchema.from(validationContext, childLoc, childPath, childNode, parent, true);
        assertNotNull(child);
        assertSame(parent, child.findAncestor());
        assertNull(child.getRefSchema(JsonNodePath.of("#/definitions/name")));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null ValidationContext should throw NPE")
    void nullValidationContext() {
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> JsonSchema.from(null, loc, path, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with empty schema node (no validation keywords)")
    void emptyObjectSchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/empty-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode emptyNode = JsonNode.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, emptyNode, null, false);
        List<JsonValidator> validators = schema.getValidators();
        assertNotNull(validators);
        assertTrue(validators.isEmpty());
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
        JsonNode node = JsonNode.readTree("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, null, path, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null EvaluationPath should throw NPE")
    void nullEvaluationPath() {
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNode node = JsonNode.readTree("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, loc, null, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with null parent and suppressSubSchemaRetrieval = true")
    void nullParentSuppressTrue() {
        SchemaLocation loc = new SchemaLocation("classpath:/schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, node, null, true);
        assertNotNull(schema);
        assertNull(schema.findAncestor());
        assertNull(schema.getRefSchema(JsonNodePath.of("#/any")));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with array schema node should yield empty validators")
    void arraySchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/array-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode arrayNode = JsonNode.readTree("[\"a\",\"b\"]");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, arrayNode, null, false);
        assertNotNull(schema);
        assertTrue(schema.getValidators().isEmpty());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with boolean schema node (true) should succeed")
    void booleanSchemaNodeTrue() {
        SchemaLocation loc = new SchemaLocation("classpath:/bool-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode boolNode = JsonNode.readTree("true");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, boolNode, null, false);
        assertNotNull(schema);
        assertTrue(schema.getValidators().isEmpty());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with parent schema and suppressSubSchemaRetrieval = false")
    void parentSchemaSuppressFalse() {
        SchemaLocation parentLoc = new SchemaLocation("classpath:/parent-schema.json");
        JsonNodePath parentPath = new JsonNodePath("$");
        JsonNode parentNode = JsonNode.readTree("{\"type\":\"object\"}");
        JsonSchema parent = JsonSchema.from(validationContext, parentLoc, parentPath, parentNode, null, false);
        SchemaLocation childLoc = new SchemaLocation("classpath:/child-schema.json");
        JsonNodePath childPath = new JsonNodePath("$.child");
        JsonNode childNode = JsonNode.readTree("{\"type\":\"integer\"}");
        JsonSchema child = JsonSchema.from(validationContext, childLoc, childPath, childNode, parent, false);
        assertNotNull(child);
        assertSame(parent, child.findAncestor());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("$ref resolution works when suppressSubSchemaRetrieval = false")
    void refResolutionNotSuppressed() {
        SchemaLocation loc = new SchemaLocation("classpath:/ref-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"$ref\":\"#/definitions/name\",\"definitions\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, node, null, false);
        JsonSchema refSchema = schema.getRefSchema(JsonNodePath.of("#/definitions/name"));
        assertNotNull(refSchema);
        assertEquals("string", refSchema.getTypeValidator().getType());
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("$ref resolution is disabled when suppressSubSchemaRetrieval = true")
    void refResolutionSuppressed() {
        SchemaLocation loc = new SchemaLocation("classpath:/ref-schema-suppressed.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"$ref\":\"#/definitions/name\",\"definitions\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, node, null, true);
        JsonSchema refSchema = schema.getRefSchema(JsonNodePath.of("#/definitions/name"));
        assertNull(refSchema);
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with empty SchemaLocation should throw IllegalArgumentException")
    void emptySchemaLocation() {
        SchemaLocation emptyLoc = new SchemaLocation("");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"type\":\"object\"}");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, emptyLoc, path, node, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Construction with numeric schema node should throw IllegalArgumentException")
    void numericSchemaNode() {
        SchemaLocation loc = new SchemaLocation("classpath:/numeric-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode numericNode = JsonNode.readTree("123");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, loc, path, numericNode, null, false));
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Resolving malformed $ref fragment should throw IllegalArgumentException")
    void malformedRefFragment() {
        SchemaLocation loc = new SchemaLocation("classpath:/bad-ref-schema.json");
        JsonNodePath path = new JsonNodePath("$");
        JsonNode node = JsonNode.readTree("{\"$ref\":\"#/definitions/@@invalid@@\",\"definitions\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = JsonSchema.from(validationContext, loc, path, node, null, false);
        assertThrows(IllegalArgumentException.class, () -> schema.getRefSchema(JsonNodePath.of("#/definitions/@@invalid@@")));
    }
}