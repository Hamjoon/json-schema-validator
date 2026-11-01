```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#hasTypeValidator()} covering a wide range of scenarios.
 *
 * <p>The test class follows the provided framework – the {@code @BeforeAll},
 * {@code @BeforeEach}, {@code @AfterEach} and {@code @AfterAll} methods are left
 * untouched.  Each test method focuses on a single logical group described in the
 * JSON test‑case specification.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_hasTypeValidator_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default draft‑2020‑12 meta‑schema; no customisation needed for these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {});
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Factory does not hold external resources that need explicit disposal.
    }

    /* --------------------------------------------------------------------- *
     *  Existing simple cases (kept unchanged for completeness)            *
     * --------------------------------------------------------------------- */

    /**
     * A schema that contains a top‑level {@code "type"} keyword should report
     * that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsPresent_returnsTrue() {
        // GIVEN a schema JSON string with a top‑level type definition
        String schemaWithType = "{ \"type\": \"object\" }";

        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithType);

        // THEN hasTypeValidator() must return true
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be true when a top‑level \"type\" keyword is present");
    }

    /**
     * A schema that does not contain a top‑level {@code "type"} keyword should
     * report that it does not have a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsAbsent_returnsFalse() {
        // GIVEN a schema JSON string without a top‑level type definition
        String schemaWithoutType = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";

        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithoutType);

        // THEN hasTypeValidator() must return false
        assertFalse(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be false when no top‑level \"type\" keyword is present");
    }

    /**
     * A schema whose {@code "type"} keyword is an array (union type) should also
     * report that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsUnion_returnsTrue() {
        // GIVEN a schema with a union type definition
        String schemaWithUnionType = "{ \"type\": [\"string\", \"null\"] }";

        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithUnionType);

        // THEN hasTypeValidator() must return true
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be true when \"type\" is a union array");
    }

    /* --------------------------------------------------------------------- *
     *  1. Spy‑based tests – stubbing the protected getTypeValidator() method *
     * --------------------------------------------------------------------- */

    /**
     * When the protected {@code getTypeValidator()} returns a non‑null mock,
     * {@code hasTypeValidator()} must return {@code true}.
     */
    @Test
    void hasTypeValidator_spyWithNonNullTypeValidator_returnsTrue() {
        // Create a real instance (using a minimal schema) and spy it.
        JsonSchema real = factory.getSchema("{\"type\":\"string\"}");
        JsonSchema spy = spy(real);

        // Stub the protected method to return a dummy (non‑null) TypeValidator.
        TypeValidator dummy = mock(TypeValidator.class);
        doReturn(dummy).when(spy).getTypeValidator();

        // The public method should now see the stubbed value.
        assertTrue(spy.hasTypeValidator(),
                "hasTypeValidator() should be true when getTypeValidator() is stubbed to a non‑null value");
    }

    /**
     * When the protected {@code getTypeValidator()} returns {@code null},
     * {@code hasTypeValidator()} must return {@code false}.
     */
    @Test
    void hasTypeValidator_spyWithNullTypeValidator_returnsFalse() {
        JsonSchema real = factory.getSchema("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema spy = spy(real);

        // Stub to return null.
        doReturn(null).when(spy).getTypeValidator();

        assertFalse(spy.hasTypeValidator(),
                "hasTypeValidator() should be false when getTypeValidator() is stubbed to null");
    }

    /* --------------------------------------------------------------------- *
     *  2. Real schema tests involving $ref resolution                         *
     * --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_refPointsToSchemaWithType_returnsTrue() {
        // referenced schema (definition) contains a type
        String rootSchemaJson = "{\n" +
                "  \"$ref\": \"#/definitions/ref\",\n" +
                "  \"definitions\": {\n" +
                "    \"ref\": { \"type\": \"integer\" }\n" +
                "  }\n" +
                "}";

        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertTrue(schema.hasTypeValidator(),
                "hasTypeValidator() should be true when $ref resolves to a schema containing a top‑level \"type\"");
    }

    @Test
    void hasTypeValidator_refPointsToSchemaWithoutType_returnsFalse() {
        String rootSchemaJson = "{\n" +
                "  \"$ref\": \"#/definitions/ref\",\n" +
                "  \"definitions\": {\n" +
                "    \"ref\": { \"properties\": { \"age\": { \"type\": \"number\" } } }\n" +
                "  }\n" +
                "}";

        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertFalse(schema.hasTypeValidator(),
                "hasTypeValidator() should be false when $ref resolves to a schema that lacks a top‑level \"type\"");
    }

    /* --------------------------------------------------------------------- *
     *  3. Edge cases – empty type array, invalid type value, etc.           *
     * --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_emptyTypeArray_returnsFalse() {
        String schemaJson = "{ \"type\": [] }";

        JsonSchema schema = factory.getSchema(schemaJson);
        assertFalse(schema.hasTypeValidator(),
                "An empty \"type\" array should be treated as no usable TypeValidator");
    }

    @Test
    void hasTypeValidator_invalidTypeValue_throwsJsonSchemaException() {
        String schemaJson = "{ \"type\": 123 }";

        // The factory validates the schema during creation and should throw.
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaJson),
                "A non‑string/non‑array \"type\" value must cause JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     *  4. Null reference handling                                            *
     * --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException() {
        JsonSchema schema = null;
        assertThrows(NullPointerException.class,
                schema::hasTypeValidator,
                "Calling hasTypeValidator() on a null reference must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  5. Circular $ref leading to StackOverflowError                        *
     * --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_circularRef_throwsStackOverflowError() {
        String circularSchema = "{\n" +
                "  \"$ref\": \"#/definitions/circle\",\n" +
                "  \"definitions\": {\n" +
                "    \"circle\": { \"$ref\": \"#/definitions/circle\" }\n" +
                "  }\n" +
                "}";

        // The recursion occurs during schema construction / validation.
        assertThrows(StackOverflowError.class,
                () -> {
                    JsonSchema schema = factory.getSchema(circularSchema);
                    // Trigger the lazy creation of the TypeValidator.
                    schema.hasTypeValidator();
                },
                "A self‑referencing $ref should eventually cause StackOverflowError");
    }

    /* --------------------------------------------------------------------- *
     *  6. Creating a JsonSchema with a null JsonNode via reflection           *
     * --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException() throws Exception {
        // Obtain the (package‑private) constructor that the factory uses internally.
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);

        // Pass null for the JsonNode argument – all other arguments are also null
        // because they are not needed for this particular test.
        JsonSchema schema = ctor.newInstance(null, null, null, null, null, false);

        // The internal logic expects a non‑null schema node; calling the public
        // method should therefore raise NPE.
        assertThrows(NullPointerException.class,
                schema::hasTypeValidator,
                "Creating a JsonSchema with a null JsonNode should cause NullPointerException when hasTypeValidator() is invoked");
    }

    /* --------------------------------------------------------------------- *
     *  7. Parameterised test for multiple‑type arrays (already covered)     *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "type array [{0}] -> expected {1}")
    @CsvSource({
            "'[\"string\",\"null\"]', true",
            "'[]', false"
    })
    void hasTypeValidator_typeArrayVariants(String typeArrayJson, boolean expected) {
        String schemaJson = "{ \"type\": " + typeArrayJson + " }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertEquals(expected, schema.hasTypeValidator(),
                "Unexpected result for schema with type array: " + typeArrayJson);
    }
}
```