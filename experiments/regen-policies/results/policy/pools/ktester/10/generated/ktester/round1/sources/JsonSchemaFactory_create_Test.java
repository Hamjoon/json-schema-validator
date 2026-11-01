package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    /**
     * Instance of the factory under test – created via the public API.
     */
    private static JsonSchemaFactory factory;

    /**
     * A minimal JSON object used as the schema node argument.
     */
    private static JsonNode emptySchemaNode;

    @Mock
    private ValidationContext // mocked – its internal behaviour is not exercised here
    validationContext;

    @Mock
    private SchemaLocation // mocked – only identity is required
    schemaLocation;

    @Mock
    private JsonNodePath // mocked – only identity is required
    evaluationPath;

    @Mock
    private JsonSchema // mocked – only identity is required
    parentSchema;

    /**
     * Shared {@link ObjectMapper} for parsing JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext // used where a real instance is not required
    mockValidationContext;

    @Mock
    private SchemaLocation // used where a real instance is not required
    mockSchemaLocation;

    @Mock
    private JsonNodePath // used where a real instance is not required
    mockEvaluationPath;

    @Mock
    private JsonSchema // used where a real instance is not required
    mockParentSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a factory instance with the default configuration.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        // Create a simple, empty JSON object that can safely be passed to the factory.
        ObjectMapper mapper = new ObjectMapper();
        emptySchemaNode = mapper.createObjectNode();
        // Use the default draft‑2019‑09 factory – it can handle all drafts.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
    }

    private static ValidationContext newValidationContext() {
        // The library provides a public no‑arg constructor.
        return new ValidationContext();
    }

    private static SchemaLocation newSchemaLocation(String uri) {
        return new SchemaLocation(uri);
    }

    private static JsonNodePath rootPath() {
        return JsonNodePath.ROOT;
    }

    private static JsonNodePath path(String jsonPointer) {
        return JsonNodePath.of(jsonPointer);
    }

    private static Stream<Arguments> nullArgumentProvider() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode simpleNode = MAPPER.createObjectNode();
        return Stream.of(Arguments.of("validationContext is null", null, loc, path, simpleNode, null), Arguments.of("schemaLocation is null", ctx, null, path, simpleNode, null), Arguments.of("schemaNode is null", ctx, loc, path, null, null));
    }

    /**
     * Verifies that {@code create(...)} returns exactly the same {@link JsonSchema}
     * instance that the private {@code doCreate(...)} method would return.
     *
     * The test uses reflection to invoke {@code doCreate(...)} directly, then
     * compares the result with the one obtained via the public {@code create(...)} method.
     */
    @Test
    void testCreateDelegatesToDoCreate() throws Exception {
        // ----------------------------------------------------------------------
        // GIVEN – a factory and the required arguments (all mocked except the schema node)
        // ----------------------------------------------------------------------
        // (All arguments are already prepared by the @BeforeAll / @Mock annotations.)
        // ----------------------------------------------------------------------
        // WHEN – invoke the private doCreate method via reflection
        // ----------------------------------------------------------------------
        Method doCreateMethod = JsonSchemaFactory.class.getDeclaredMethod("doCreate", ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        doCreateMethod.setAccessible(true);
        JsonSchema expected = (JsonSchema) doCreateMethod.invoke(factory, validationContext, schemaLocation, evaluationPath, emptySchemaNode, parentSchema, // same flag that the public method uses
        false);
        // ----------------------------------------------------------------------
        // AND – invoke the public create method
        // ----------------------------------------------------------------------
        JsonSchema actual = factory.create(validationContext, schemaLocation, evaluationPath, emptySchemaNode, parentSchema);
        // ----------------------------------------------------------------------
        // THEN – both references must be identical
        // ----------------------------------------------------------------------
        assertSame(expected, actual, "JsonSchemaFactory#create should return the exact instance produced by the private doCreate method");
    }

    @Test
    void testCreateSimpleObjectSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode schemaNode = MAPPER.readTree("{ \"type\": \"object\" }");
        JsonSchema schema = factory.create(ctx, loc, path, schemaNode, null);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a valid object schema");
        // The meta‑schema version should be the factory default (draft‑2019‑09)
        assertEquals(VersionFlag.V201909, schema.getMetaSchema().getVersion(), "Meta‑schema version should match the factory default");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArgumentProvider")
    void testCreateNullArguments(String description, ValidationContext ctx, SchemaLocation loc, JsonNodePath evalPath, JsonNode node, JsonSchema parent) {
        assertThrows(NullPointerException.class, () -> factory.create(ctx, loc, evalPath, node, parent), description);
    }

    @Test
    void testCreateNullSchemaNodeThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        assertThrows(NullPointerException.class, () -> factory.create(ctx, loc, path, null, null), "Factory must reject a null JsonNode representing the schema definition");
    }

    @Test
    void testCreateNullNodeSchemaThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode nullNode = NullNode.getInstance();
        assertThrows(IllegalArgumentException.class, () -> factory.create(ctx, loc, path, nullNode, null), "Factory should reject a NullNode as a schema definition");
    }

    @Test
    void testCreateBooleanTrueSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonSchema schema = factory.create(ctx, loc, path, BooleanNode.TRUE, null);
        assertNotNull(schema, "Factory must create a JsonSchema for the boolean true literal");
        // The schema should have no validators that can fail – we verify by checking that
        // validation of an arbitrary instance yields an empty error list.
        assertTrue(schema.validate(MAPPER.readTree("\"any value\"")).isEmpty(), "A true‑schema must accept any JSON instance");
    }

    @Test
    void testCreateBooleanFalseSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonSchema schema = factory.create(ctx, loc, path, BooleanNode.FALSE, null);
        assertNotNull(schema, "Factory must create a JsonSchema for the boolean false literal");
        // The schema should reject any instance – validation should produce at least one error.
        assertFalse(schema.validate(MAPPER.readTree("\"any value\"")).isEmpty(), "A false‑schema must reject any JSON instance");
    }

    @Test
    void testCreateDetectsDraft07FromSchemaKeyword() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"number\" }");
        JsonSchema schema = factory.create(ctx, loc, path, node, null);
        assertNotNull(schema, "Schema should be created successfully");
        assertEquals(VersionFlag.V7, schema.getMetaSchema().getVersion(), "The meta‑schema version must be detected as Draft‑07 from the $schema keyword");
    }

    @Test
    void testCreateAllOfCompositeSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"allOf\": [ { \"type\": \"string\" }, { \"maxLength\": 5 } ] }");
        JsonSchema schema = factory.create(ctx, loc, path, node, null);
        assertNotNull(schema, "AllOf composite schema must be created");
        // The schema should contain an AllOfValidator – we verify by checking that a valid
        // instance passes and an invalid instance fails.
        assertTrue(schema.validate(MAPPER.readTree("\"abc\"")).isEmpty(), "A string of length 3 satisfies both allOf constraints");
        assertFalse(schema.validate(MAPPER.readTree("\"abcdef\"")).isEmpty(), "A string longer than 5 must violate the maxLength constraint");
    }

    @Test
    void testCreateWithNonNullParentSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation parentLoc = newSchemaLocation("classpath:/parent.json");
        // create a real parent schema
        JsonSchema parent = factory.getSchema(parentLoc);
        SchemaLocation childLoc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"type\": \"string\" }");
        JsonSchema child = factory.create(ctx, childLoc, path, node, parent);
        assertNotNull(child, "Child schema must be created");
        assertSame(parent, child.getParentSchema(), "Child schema should retain the supplied parent schema reference");
    }

    @Test
    void testCreateWithNonRootEvaluationPath() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        // non‑root path
        JsonNodePath path = path("/properties/id");
        JsonNode node = MAPPER.readTree("{ \"type\": \"integer\" }");
        JsonSchema schema = factory.create(ctx, loc, path, node, null);
        assertNotNull(schema, "Factory must handle a non‑root evaluationPath when creating a subschema");
    }

    @Test
    void testCreateRefWithSiblingKeywordsThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"$ref\": \"#/definitions/foo\", \"type\": \"string\" }");
        assertThrows(IllegalArgumentException.class, () -> factory.create(ctx, loc, path, node, null), "$ref must be the sole keyword in a schema object");
    }

    @Test
    void testCreateMalformedRefThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"$ref\": \"http://[invalid]/schema.json\" }");
        assertThrows(IllegalArgumentException.class, () -> factory.create(ctx, loc, path, node, null), "A malformed $ref URI must cause the factory to reject the schema");
    }

    @Test
    void testCreateUnknownSchemaVersionThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"$schema\": \"http://json-schema.org/draft-99/schema#\", \"type\": \"number\" }");
        assertThrows(IllegalArgumentException.class, () -> factory.create(ctx, loc, path, node, null), "Unsupported $schema version must be rejected");
    }

    @Test
    void testCreateUnknownKeywordThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"unknownKeyword\": true }");
        assertThrows(IllegalArgumentException.class, () -> factory.create(ctx, loc, path, node, null), "Schema containing an unknown keyword must be rejected");
    }

    @Test
    void testCreateDefinitionsWithoutRef() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"definitions\": { \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 0 } } }");
        JsonSchema schema = factory.create(ctx, loc, path, node, null);
        assertNotNull(schema, "Schema with definitions must be created");
        // Resolve the definition via a $ref to ensure it was registered.
        JsonNode refNode = MAPPER.readTree("{ \"$ref\": \"#/definitions/positiveInteger\" }");
        JsonSchema refSchema = factory.create(ctx, loc, path, refNode, null);
        assertNotNull(refSchema, "Schema created from $ref to a definition must be resolvable");
    }
}
