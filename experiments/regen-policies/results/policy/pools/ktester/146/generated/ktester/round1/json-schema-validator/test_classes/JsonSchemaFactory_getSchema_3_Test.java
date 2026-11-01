package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import java.util.Set;
import java.util.stream.Stream;
import com.networknt.schema.JsonSchemaFactory.Builder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the supplied version flag.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    private static Stream<Arguments> provideUnsupportedPrimitiveRoots() {
        return Stream.of(Arguments.of("123"), Arguments.of("\"string\""));
    }

    private static Stream<Arguments> provideInvalidTypeValues() {
        return Stream.of(Arguments.of("{\"type\": \"invalidtype\"}"), Arguments.of("{\"type\": [\"string\", \"invalidtype\"]}"));
    }

    private static Stream<Arguments> provideInvalidRequiredValues() {
        return Stream.of(Arguments.of("{\"required\": \"name\"}"), Arguments.of("{\"required\": [123, true]}"));
    }

    private static Stream<Arguments> provideInvalidNumericConstraints() {
        return Stream.of(Arguments.of("{\"minimum\": \"abc\"}"), Arguments.of("{\"multipleOf\": 0}"), Arguments.of("{\"maxLength\": -5}"));
    }

    private static Stream<Arguments> provideInvalidCombinatorSchemas() {
        return Stream.of(Arguments.of("{\"allOf\": {\"type\": \"string\"}}"), Arguments.of("{\"anyOf\": \"string\"}"), Arguments.of("{\"oneOf\": 123}"), Arguments.of("{\"not\": []}"), Arguments.of("{\"if\": []}"), Arguments.of("{\"then\": \"string\"}"), Arguments.of("{\"else\": 123}"));
    }

    /**
     * Verify that {@code getSchema(JsonNode)} returns a non‑null {@link JsonSchema}
     * instance when a valid schema node is supplied.
     */
    @Test
    void getSchema_validJsonNode_returnsJsonSchema() throws Exception {
        // a minimal JSON‑Schema that accepts any string
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "The factory should return a non‑null JsonSchema");
        // The returned schema should be able to validate a matching instance.
        JsonNode instance = mapper.readTree("\"hello\"");
        assertTrue(jsonSchema.validate(instance).isEmpty(), "A valid instance should produce no validation errors");
    }

    /**
     * Verify that a {@code null} argument is rejected with a {@link NullPointerException}.
     */
    @Test
    void getSchema_nullJsonNode_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> factory.getSchema((JsonNode) null), "Passing null should result in NullPointerException");
    }

    /**
     * Verify that a well‑formed object schema returns a non‑null {@link JsonSchema}
     * and that the schema can validate a conforming instance.
     */
    @Test
    void getSchema_objectWithProperty_returnsJsonSchema() throws Exception {
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
        // instance that satisfies the schema
        JsonNode instance = mapper.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> errors = jsonSchema.validate(instance);
        assertTrue(errors.isEmpty(), "Valid instance must produce no validation errors");
    }

    /**
     * Empty object schema – should still produce a usable {@link JsonSchema}
     * that imposes no constraints.
     */
    @Test
    void getSchema_emptyObject_returnsJsonSchema() throws Exception {
        JsonNode schemaNode = mapper.readTree("{}");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Empty schema must still produce a JsonSchema");
        // any instance is valid – e.g. a simple number
        JsonNode instance = mapper.readTree("123");
        assertTrue(jsonSchema.validate(instance).isEmpty(), "Empty schema should accept any instance");
    }

    /**
     * JSON array as a root schema – the factory must accept it and produce a
     * {@link JsonSchema}.  The exact internal representation is not asserted;
     * we only verify that validation works for a matching instance.
     */
    @Test
    void getSchema_rootArray_returnsJsonSchema() throws Exception {
        String schemaJson = "[ { \"type\": \"string\" }, { \"type\": \"number\" } ]";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Array root schema must produce a JsonSchema");
        // According to the spec the array is treated as a schema with "type":"array"
        // and "items" derived from the first element.
        JsonNode validInstance = mapper.readTree("[\"abc\"]");
        assertTrue(jsonSchema.validate(validInstance).isEmpty(), "Array schema should validate a matching instance");
    }

    /**
     * Schema that explicitly declares its meta‑schema via the $schema keyword.
     * The factory should honour the declaration – we only check that the
     * schema is created successfully.
     */
    @Test
    void getSchema_withExplicitMetaSchema_returnsJsonSchema() throws Exception {
        String schemaJson = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"string\" }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Schema with $schema keyword must be created");
    }

    @Test
    void getSchema_booleanTrue_returnsAlwaysValidSchema() throws Exception {
        JsonNode schemaNode = mapper.readTree("true");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Boolean true schema must produce a JsonSchema");
        // any instance should be valid
        JsonNode anyInstance = mapper.readTree("{\"foo\":123}");
        assertTrue(jsonSchema.validate(anyInstance).isEmpty(), "True schema must accept any instance");
    }

    @Test
    void getSchema_booleanFalse_returnsAlwaysInvalidSchema() throws Exception {
        JsonNode schemaNode = mapper.readTree("false");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Boolean false schema must produce a JsonSchema");
        // any instance should produce at least one validation error
        JsonNode anyInstance = mapper.readTree("{\"foo\":123}");
        Set<ValidationMessage> errors = jsonSchema.validate(anyInstance);
        assertFalse(errors.isEmpty(), "False schema must reject any instance");
    }

    @ParameterizedTest(name = "[{index}] primitive root ''{0}'' should throw JsonSchemaException")
    @MethodSource("provideUnsupportedPrimitiveRoots")
    void getSchema_unsupportedPrimitiveRoot_throwsJsonSchemaException(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Numeric or string literals are not valid schema documents");
    }

    @Test
    void getSchema_unknownSchemaVersion_throwsIllegalArgumentException() throws Exception {
        String schemaJson = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaNode), "Unsupported $schema version should cause IllegalArgumentException");
    }

    @Test
    void getSchema_internalRef_resolvesThroughReference() throws Exception {
        String schemaJson = "{ \"$id\": \"#root\", \"definitions\": { \"name\": { \"type\": \"string\" } }, \"$ref\": \"#/definitions/name\" }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Schema with internal $ref must be created");
        // The resolved schema should accept a string and reject a number
        JsonNode valid = mapper.readTree("\"abc\"");
        JsonNode invalid = mapper.readTree("123");
        assertTrue(jsonSchema.validate(valid).isEmpty(), "Resolved $ref schema should accept a string");
        assertFalse(jsonSchema.validate(invalid).isEmpty(), "Resolved $ref schema should reject a number");
    }

    @Test
    void getSchema_typeNull_allowsOnlyNull() throws Exception {
        String schemaJson = "{ \"type\": \"null\" }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Schema with type null must be created");
        JsonNode nullNode = mapper.readTree("null");
        JsonNode nonNull = mapper.readTree("\"not null\"");
        assertTrue(jsonSchema.validate(nullNode).isEmpty(), "Null instance must be accepted");
        assertFalse(jsonSchema.validate(nonNull).isEmpty(), "Non‑null instance must be rejected");
    }

    @Test
    void getSchema_emptyEnum_isAlwaysInvalid() throws Exception {
        String schemaJson = "{ \"enum\": [] }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Schema with empty enum must be created");
        JsonNode anyInstance = mapper.readTree("\"anything\"");
        assertFalse(jsonSchema.validate(anyInstance).isEmpty(), "Empty enum schema must reject every instance");
    }

    @Test
    void getSchema_unknownKeyword_isIgnored() throws Exception {
        String schemaJson = "{ \"type\": \"object\", \"unknownKeyword\": 42 }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Schema with unknown keyword should still be created");
        JsonNode valid = mapper.readTree("{}");
        assertTrue(jsonSchema.validate(valid).isEmpty(), "Unknown keyword must be ignored and not affect validation");
    }

    @Test
    void getSchema_invalidId_throwsIllegalArgumentException() throws Exception {
        String schemaJson = "{\"$id\": \"http://example .com\", \"type\": \"object\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaNode), "Malformed $id should cause IllegalArgumentException");
    }

    @Test
    void getSchema_invalidRef_throwsIllegalArgumentException() throws Exception {
        String schemaJson = "{\"$ref\": \"http://example .com#/def\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaNode), "Malformed $ref should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] invalid type value ''{0}'' should throw JsonSchemaException")
    @MethodSource("provideInvalidTypeValues")
    void getSchema_invalidTypeKeyword_throwsJsonSchemaException(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Invalid type keyword must be rejected");
    }

    @ParameterizedTest(name = "[{index}] invalid required ''{0}'' should throw JsonSchemaException")
    @MethodSource("provideInvalidRequiredValues")
    void getSchema_invalidRequiredKeyword_throwsJsonSchemaException(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Invalid required keyword must be rejected");
    }

    @Test
    void getSchema_invalidProperties_throwsJsonSchemaException() throws Exception {
        String schemaJson = "{\"properties\": \"notAnObject\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Non‑object 'properties' must cause JsonSchemaException");
    }

    @Test
    void getSchema_invalidDefinitions_throwsJsonSchemaException() throws Exception {
        String schemaJson = "{\"definitions\": []}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Non‑object 'definitions' must cause JsonSchemaException");
    }

    @Test
    void getSchema_malformedSchemaUri_throwsIllegalArgumentException() throws Exception {
        String schemaJson = "{\"$schema\": \"http://example .com/draft-07/schema#\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaNode), "Malformed $schema URI must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] invalid numeric constraint ''{0}'' should throw JsonSchemaException")
    @MethodSource("provideInvalidNumericConstraints")
    void getSchema_invalidNumericConstraints_throwsJsonSchemaException(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Invalid numeric constraint must be rejected");
    }

    @Test
    void getSchema_invalidAdditionalProperties_throwsJsonSchemaException() throws Exception {
        String schemaJson = "{\"additionalProperties\": 123}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Non‑boolean/object 'additionalProperties' must cause JsonSchemaException");
    }

    @Test
    void getSchema_invalidPatternProperties_throwsJsonSchemaException() throws Exception {
        String schemaJson = "{\"patternProperties\": \"notAnObject\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Non‑object 'patternProperties' must cause JsonSchemaException");
    }

    @Test
    void getSchema_invalidDependencies_throwsJsonSchemaException() throws Exception {
        String schemaJson = "{\"dependencies\": []}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Non‑object 'dependencies' must cause JsonSchemaException");
    }

    @ParameterizedTest(name = "[{index}] invalid combinator ''{0}'' should throw JsonSchemaException")
    @MethodSource("provideInvalidCombinatorSchemas")
    void getSchema_invalidCombinatorKeywords_throwsJsonSchemaException(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaNode), "Invalid combinator keyword format must be rejected");
    }
}
