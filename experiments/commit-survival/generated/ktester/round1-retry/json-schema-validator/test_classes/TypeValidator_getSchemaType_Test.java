package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TypeValidator_getSchemaType_Test {

    private ValidationContext validationContext;

    private JsonSchema parentSchema;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but valid ValidationContext.
        URIFactory uriFactory = org.mockito.Mockito.mock(URIFactory.class);
        URNFactory urnFactory = org.mockito.Mockito.mock(URNFactory.class);
        JsonMetaSchema metaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory jsonSchemaFactory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        // default configuration
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, jsonSchemaFactory, config);
        // Parent schema is not used by getSchemaType(), so a mock is sufficient.
        parentSchema = org.mockito.Mockito.mock(JsonSchema.class);
    }

    /**
     * Provides pairs of (JsonType value to inject, expected result) for the reflection‑based tests.
     */
    private static Stream<Arguments> schemaTypeReflectionProvider() {
        return Stream.of(Arguments.of(JsonType.STRING, JsonType.STRING), Arguments.of(JsonType.NUMBER, JsonType.NUMBER), Arguments.of(JsonType.INTEGER, JsonType.INTEGER), Arguments.of(JsonType.BOOLEAN, JsonType.BOOLEAN), Arguments.of(JsonType.OBJECT, JsonType.OBJECT), Arguments.of(JsonType.ARRAY, JsonType.ARRAY), Arguments.of(JsonType.NULL, JsonType.NULL), // field left unset
        Arguments.of(null, null));
    }

    /**
     * Supplies JSON strings representing a schema node together with the expected {@link JsonType}.
     */
    private static Stream<Arguments> schemaNodeProvider() {
        return Stream.of(Arguments.of("{\"type\":\"string\"}", JsonType.STRING), Arguments.of("{\"type\":\"number\"}", JsonType.NUMBER), Arguments.of("{\"type\":\"integer\"}", JsonType.INTEGER), Arguments.of("{\"type\":\"boolean\"}", JsonType.BOOLEAN), Arguments.of("{\"type\":\"object\"}", JsonType.OBJECT), Arguments.of("{\"type\":\"array\"}", JsonType.ARRAY), Arguments.of("{\"type\":\"null\"}", JsonType.NULL), // No type keyword → null
        Arguments.of("{}", null), // Unrecognised type string → null
        Arguments.of("{\"type\":\"unknown\"}", null), // Array of types (union) → null (validator does not map to a single JsonType)
        Arguments.of("{\"type\":[\"string\",\"null\"]}", null));
    }

    @Test
    void getSchemaType_returnsStringWhenSchemaSpecifiesString() {
        // GIVEN a schema node that defines type "string"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");
        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);
        // THEN the schema type reported by the validator matches JsonType.STRING
        assertEquals(JsonType.STRING, validator.getSchemaType(), "Expected schema type to be STRING for a schema node with type \"string\"");
    }

    @Test
    void getSchemaType_returnsIntegerWhenSchemaSpecifiesInteger() {
        // GIVEN a schema node that defines type "integer"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "integer");
        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);
        // THEN the schema type reported by the validator matches JsonType.INTEGER
        assertEquals(JsonType.INTEGER, validator.getSchemaType(), "Expected schema type to be INTEGER for a schema node with type \"integer\"");
    }

    @Test
    void getSchemaType_returnsUnionWhenSchemaSpecifiesArrayOfTypes() {
        // GIVEN a schema node that defines a union type ["string","null"]
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.putArray("type").add("string").add("null");
        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);
        // THEN the validator reports UNION as the schema type
        assertEquals(JsonType.UNION, validator.getSchemaType(), "Expected schema type to be UNION for a schema node with an array of types");
    }

    @Test
    void getSchemaType_returnsNullWhenSchemaNodeHasNoTypeKeyword() {
        // GIVEN a schema node without a "type" keyword
        // empty object
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        // WHEN a TypeValidator is constructed
        TypeValidator validator = new TypeValidator("testPath", schemaNode, parentSchema, validationContext);
        // THEN the schema type is null (the TypeValidator does not set a type)
        assertNull(validator.getSchemaType(), "Expected schema type to be null when the schema node does not contain a \"type\" keyword");
    }

    @ParameterizedTest(name = "schemaType set to {0} → getSchemaType() returns {1}")
    @MethodSource("schemaTypeReflectionProvider")
    void getSchemaType_reflectionSetsField_correctly(JsonType injected, JsonType expected) throws Exception {
        // GIVEN a validator constructed with an empty schema node
        ObjectNode emptySchema = JsonNodeFactory.instance.objectNode();
        TypeValidator validator = new TypeValidator("path", emptySchema, parentSchema, validationContext);
        // WHEN we force‑set the private field via reflection
        Field field = TypeValidator.class.getDeclaredField("schemaType");
        field.setAccessible(true);
        field.set(validator, injected);
        // THEN the getter returns the value we injected (or null)
        assertEquals(expected, validator.getSchemaType(), () -> "Expected getSchemaType() to return " + expected + " after reflection injection");
    }

    @ParameterizedTest(name = "Schema {0} → expected schemaType {1}")
    @MethodSource("schemaNodeProvider")
    void getSchemaType_initialisedFromSchemaNode(String schemaJson, JsonType expected) throws Exception {
        // GIVEN a JsonNode built from the provided JSON string
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // WHEN a TypeValidator is constructed with that schema
        TypeValidator validator = new TypeValidator("path", schemaNode, parentSchema, validationContext);
        // THEN the getter returns the expected enum (or null)
        assertEquals(expected, validator.getSchemaType(), () -> "For schema " + schemaJson + " expected getSchemaType() to be " + expected);
    }

    @Test
    void getSchemaType_nullValidator_throwsNullPointerException() {
        // GIVEN a null reference
        TypeValidator validator = null;
        // WHEN we invoke the method – THEN a NPE is thrown
        assertThrows(NullPointerException.class, () -> validator.getSchemaType(), "Calling getSchemaType() on a null reference must throw NullPointerException");
    }

    @Test
    void getSchemaType_invalidFieldValue_throwsClassCastException() throws Exception {
        // GIVEN a validator with a malformed private field (String instead of JsonType)
        ObjectNode emptySchema = JsonNodeFactory.instance.objectNode();
        TypeValidator validator = new TypeValidator("path", emptySchema, parentSchema, validationContext);
        Field field = TypeValidator.class.getDeclaredField("schemaType");
        field.setAccessible(true);
        // force a wrong type
        field.set(validator, "invalid-type");
        // WHEN we call getter – THEN a ClassCastException occurs because the field cannot be cast to JsonType
        assertThrows(ClassCastException.class, validator::getSchemaType, "If schemaType is not a JsonType, getSchemaType() should throw ClassCastException");
    }
}
