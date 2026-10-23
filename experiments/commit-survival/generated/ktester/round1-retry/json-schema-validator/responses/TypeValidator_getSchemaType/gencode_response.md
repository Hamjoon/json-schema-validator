```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link TypeValidator#getSchemaType()}.
 *
 * <p>The tests cover three aspects:
 *
 * <ul>
 *   <li>Directly setting the private {@code schemaType} field via reflection and verifying the getter.</li>
 *   <li>Creating a {@code TypeValidator} with various schema nodes and checking the derived type.</li>
 *   <li>Exception scenarios such as calling the method on a {@code null} reference or having an invalid
 *       field value.</li>
 * </ul>
 *
 * <p>The {@code @BeforeEach} method builds a minimal {@link ValidationContext} and a mock parent
 * {@link JsonSchema}; these are reused across tests.
 */
@ExtendWith(MockitoExtension.class)
class TypeValidator_getSchemaType_Test {

    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but valid ValidationContext.
        URIFactory uriFactory = mock(URIFactory.class);
        URNFactory urnFactory = mock(URNFactory.class);
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        JsonSchemaFactory jsonSchemaFactory = mock(JsonSchemaFactory.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // default configuration

        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema,
                jsonSchemaFactory, config);

        // Parent schema is not used by getSchemaType(), so a mock is sufficient.
        parentSchema = mock(JsonSchema.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* -------------------------------------------------------
     *  Group 1 – Direct field manipulation via reflection
     * ------------------------------------------------------- */

    /**
     * Provides pairs of (JsonType value to inject, expected result) for the reflection‑based tests.
     */
    private static Stream<Arguments> schemaTypeReflectionProvider() {
        return Stream.of(
                Arguments.of(JsonType.STRING, JsonType.STRING),
                Arguments.of(JsonType.NUMBER, JsonType.NUMBER),
                Arguments.of(JsonType.INTEGER, JsonType.INTEGER),
                Arguments.of(JsonType.BOOLEAN, JsonType.BOOLEAN),
                Arguments.of(JsonType.OBJECT, JsonType.OBJECT),
                Arguments.of(JsonType.ARRAY, JsonType.ARRAY),
                Arguments.of(JsonType.NULL, JsonType.NULL),
                Arguments.of(null, null) // field left unset
        );
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
        assertEquals(expected, validator.getSchemaType(),
                () -> "Expected getSchemaType() to return " + expected + " after reflection injection");
    }

    /* -------------------------------------------------------
     *  Group 2 – Initialisation from a schema node
     * ------------------------------------------------------- */

    /**
     * Supplies JSON strings representing a schema node together with the expected {@link JsonType}.
     */
    private static Stream<Arguments> schemaNodeProvider() {
        return Stream.of(
                Arguments.of("{\"type\":\"string\"}", JsonType.STRING),
                Arguments.of("{\"type\":\"number\"}", JsonType.NUMBER),
                Arguments.of("{\"type\":\"integer\"}", JsonType.INTEGER),
                Arguments.of("{\"type\":\"boolean\"}", JsonType.BOOLEAN),
                Arguments.of("{\"type\":\"object\"}", JsonType.OBJECT),
                Arguments.of("{\"type\":\"array\"}", JsonType.ARRAY),
                Arguments.of("{\"type\":\"null\"}", JsonType.NULL),
                // No type keyword → null
                Arguments.of("{}", null),
                // Unrecognised type string → null
                Arguments.of("{\"type\":\"unknown\"}", null),
                // Array of types (union) → null (validator does not map to a single JsonType)
                Arguments.of("{\"type\":[\"string\",\"null\"]}", null)
        );
    }

    @ParameterizedTest(name = "Schema {0} → expected schemaType {1}")
    @MethodSource("schemaNodeProvider")
    void getSchemaType_initialisedFromSchemaNode(String schemaJson, JsonType expected) throws Exception {
        // GIVEN a JsonNode built from the provided JSON string
        JsonNode schemaNode = MAPPER.readTree(schemaJson);

        // WHEN a TypeValidator is constructed with that schema
        TypeValidator validator = new TypeValidator("path", schemaNode, parentSchema, validationContext);

        // THEN the getter returns the expected enum (or null)
        assertEquals(expected, validator.getSchemaType(),
                () -> "For schema " + schemaJson + " expected getSchemaType() to be " + expected);
    }

    /* -------------------------------------------------------
     *  Group 3 – Exception scenarios
     * ------------------------------------------------------- */

    @Test
    void getSchemaType_nullValidator_throwsNullPointerException() {
        // GIVEN a null reference
        TypeValidator validator = null;

        // WHEN we invoke the method – THEN a NPE is thrown
        assertThrows(NullPointerException.class, () -> validator.getSchemaType(),
                "Calling getSchemaType() on a null reference must throw NullPointerException");
    }

    @Test
    void getSchemaType_invalidFieldValue_throwsClassCastException() throws Exception {
        // GIVEN a validator with a malformed private field (String instead of JsonType)
        ObjectNode emptySchema = JsonNodeFactory.instance.objectNode();
        TypeValidator validator = new TypeValidator("path", emptySchema, parentSchema, validationContext);

        Field field = TypeValidator.class.getDeclaredField("schemaType");
        field.setAccessible(true);
        field.set(validator, "invalid-type"); // force a wrong type

        // WHEN we call getter – THEN a ClassCastException occurs because the field cannot be cast to JsonType
        assertThrows(ClassCastException.class, validator::getSchemaType,
                "If schemaType is not a JsonType, getSchemaType() should throw ClassCastException");
    }
}
```