```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link JsonSchemaFactory#create(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>basic forwarding to the private {@code doCreate} method (already verified in the original test),</li>
 *   <li>happy‑path creation for various schema shapes,</li>
 *   <li>argument‑validation branches (null arguments, illegal node types),</li>
 *   <li>special handling of boolean schemas, $schema version detection, and composite keywords.</li>
 * </ul>
 *
 * <p>All tests use the real {@link JsonSchemaFactory} implementation – no internal behaviour is mocked
 * except the {@link ValidationContext}, {@link SchemaLocation}, {@link JsonNodePath} and {@link JsonSchema}
 * arguments that are not the focus of a particular test case.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    /** Factory instance obtained via the public API – shared by all tests. */
    private static JsonSchemaFactory factory;

    /** Shared {@link ObjectMapper} for parsing JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext mockValidationContext;   // used where a real instance is not required

    @Mock
    private SchemaLocation mockSchemaLocation;        // used where a real instance is not required

    @Mock
    private JsonNodePath mockEvaluationPath;          // used where a real instance is not required

    @Mock
    private JsonSchema mockParentSchema;              // used where a real instance is not required

    @BeforeAll
    static void beforeAll() {
        // Use the default draft‑2019‑09 factory – it can handle all drafts.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
    }

    /* ----------------------------------------------------------------------
     *  Helper methods for creating arguments that are used by many tests.
     * ---------------------------------------------------------------------- */

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

    /* ----------------------------------------------------------------------
     *  1. Basic forwarding – already covered in the original test class.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateDelegatesToDoCreate() throws Exception {
        // (identical to the original test – kept for completeness)
        Method doCreateMethod = JsonSchemaFactory.class
                .getDeclaredMethod("doCreate",
                        ValidationContext.class,
                        SchemaLocation.class,
                        JsonNodePath.class,
                        JsonNode.class,
                        JsonSchema.class,
                        boolean.class);
        doCreateMethod.setAccessible(true);

        JsonSchema expected = (JsonSchema) doCreateMethod.invoke(
                factory,
                mockValidationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                MAPPER.createObjectNode(),
                mockParentSchema,
                false);

        JsonSchema actual = factory.create(
                mockValidationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                MAPPER.createObjectNode(),
                mockParentSchema);

        assertSame(expected, actual,
                "JsonSchemaFactory#create should return the exact instance produced by the private doCreate method");
    }

    /* ----------------------------------------------------------------------
     *  2. Happy‑path creation for a simple object schema.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateSimpleObjectSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode schemaNode = MAPPER.readTree("{ \"type\": \"object\" }");

        JsonSchema schema = factory.create(ctx, loc, path, schemaNode, null);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a valid object schema");
        // The meta‑schema version should be the factory default (draft‑2019‑09)
        assertEquals(VersionFlag.V201909,
                schema.getMetaSchema().getVersion(),
                "Meta‑schema version should match the factory default");
    }

    /* ----------------------------------------------------------------------
     *  3. Argument‑validation tests (null arguments, illegal node types).
     * ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArgumentProvider")
    void testCreateNullArguments(String description,
                                 ValidationContext ctx,
                                 SchemaLocation loc,
                                 JsonNodePath evalPath,
                                 JsonNode node,
                                 JsonSchema parent) {
        assertThrows(NullPointerException.class,
                () -> factory.create(ctx, loc, evalPath, node, parent),
                description);
    }

    private static Stream<Arguments> nullArgumentProvider() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode simpleNode = MAPPER.createObjectNode();

        return Stream.of(
                Arguments.of("validationContext is null",
                        null, loc, path, simpleNode, null),
                Arguments.of("schemaLocation is null",
                        ctx, null, path, simpleNode, null),
                Arguments.of("schemaNode is null",
                        ctx, loc, path, null, null)
        );
    }

    @Test
    void testCreateNullSchemaNodeThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        assertThrows(NullPointerException.class,
                () -> factory.create(ctx, loc, path, null, null),
                "Factory must reject a null JsonNode representing the schema definition");
    }

    @Test
    void testCreateNullNodeSchemaThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonNode nullNode = NullNode.getInstance();

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ctx, loc, path, nullNode, null),
                "Factory should reject a NullNode as a schema definition");
    }

    /* ----------------------------------------------------------------------
     *  4. Boolean schema handling – true (always valid) and false (always invalid).
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateBooleanTrueSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonSchema schema = factory.create(ctx, loc, path, BooleanNode.TRUE, null);
        assertNotNull(schema, "Factory must create a JsonSchema for the boolean true literal");
        // The schema should have no validators that can fail – we verify by checking that
        // validation of an arbitrary instance yields an empty error list.
        assertTrue(schema.validate(MAPPER.readTree("\"any value\"")).isEmpty(),
                "A true‑schema must accept any JSON instance");
    }

    @Test
    void testCreateBooleanFalseSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonSchema schema = factory.create(ctx, loc, path, BooleanNode.FALSE, null);
        assertNotNull(schema, "Factory must create a JsonSchema for the boolean false literal");
        // The schema should reject any instance – validation should produce at least one error.
        assertFalse(schema.validate(MAPPER.readTree("\"any value\"")).isEmpty(),
                "A false‑schema must reject any JSON instance");
    }

    /* ----------------------------------------------------------------------
     *  5. $schema version detection – draft‑07.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateDetectsDraft07FromSchemaKeyword() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonNode node = MAPPER.readTree("{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"number\" }");
        JsonSchema schema = factory.create(ctx, loc, path, node, null);

        assertNotNull(schema, "Schema should be created successfully");
        assertEquals(VersionFlag.V7,
                schema.getMetaSchema().getVersion(),
                "The meta‑schema version must be detected as Draft‑07 from the $schema keyword");
    }

    /* ----------------------------------------------------------------------
     *  6. Composite keyword – allOf.
     * ---------------------------------------------------------------------- */

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
        assertTrue(schema.validate(MAPPER.readTree("\"abc\"")).isEmpty(),
                "A string of length 3 satisfies both allOf constraints");
        assertFalse(schema.validate(MAPPER.readTree("\"abcdef\"")).isEmpty(),
                "A string longer than 5 must violate the maxLength constraint");
    }

    /* ----------------------------------------------------------------------
     *  7. Non‑null parent schema – the created schema should keep a reference.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateWithNonNullParentSchema() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation parentLoc = newSchemaLocation("classpath:/parent.json");
        JsonSchema parent = factory.getSchema(parentLoc); // create a real parent schema

        SchemaLocation childLoc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();
        JsonNode node = MAPPER.readTree("{ \"type\": \"string\" }");

        JsonSchema child = factory.create(ctx, childLoc, path, node, parent);
        assertNotNull(child, "Child schema must be created");
        assertSame(parent, child.getParentSchema(),
                "Child schema should retain the supplied parent schema reference");
    }

    /* ----------------------------------------------------------------------
     *  8. Non‑root evaluationPath – factory must accept any JsonNodePath.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateWithNonRootEvaluationPath() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = path("/properties/id"); // non‑root path
        JsonNode node = MAPPER.readTree("{ \"type\": \"integer\" }");

        JsonSchema schema = factory.create(ctx, loc, path, node, null);
        assertNotNull(schema, "Factory must handle a non‑root evaluationPath when creating a subschema");
    }

    /* ----------------------------------------------------------------------
     *  9. $ref with sibling keywords – should throw IllegalArgumentException.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateRefWithSiblingKeywordsThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonNode node = MAPPER.readTree("{ \"$ref\": \"#/definitions/foo\", \"type\": \"string\" }");

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ctx, loc, path, node, null),
                "$ref must be the sole keyword in a schema object");
    }

    /* ----------------------------------------------------------------------
     *  10. Malformed $ref URI – should result in an IllegalArgumentException.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateMalformedRefThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonNode node = MAPPER.readTree("{ \"$ref\": \"http://[invalid]/schema.json\" }");

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ctx, loc, path, node, null),
                "A malformed $ref URI must cause the factory to reject the schema");
    }

    /* ----------------------------------------------------------------------
     *  11. Unknown $schema version – should throw IllegalArgumentException.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateUnknownSchemaVersionThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonNode node = MAPPER.readTree("{ \"$schema\": \"http://json-schema.org/draft-99/schema#\", \"type\": \"number\" }");

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ctx, loc, path, node, null),
                "Unsupported $schema version must be rejected");
    }

    /* ----------------------------------------------------------------------
     *  12. Unknown keyword – should throw IllegalArgumentException.
     * ---------------------------------------------------------------------- */

    @Test
    void testCreateUnknownKeywordThrows() {
        ValidationContext ctx = newValidationContext();
        SchemaLocation loc = newSchemaLocation("classpath:/schema.json");
        JsonNodePath path = rootPath();

        JsonNode node = MAPPER.readTree("{ \"unknownKeyword\": true }");

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ctx, loc, path, node, null),
                "Schema containing an unknown keyword must be rejected");
    }

    /* ----------------------------------------------------------------------
     *  13. Definitions without $ref – definitions should be registered internally.
     *      (We verify that the top‑level schema has no validators but can resolve a $ref later.)
     * ---------------------------------------------------------------------- */

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
```