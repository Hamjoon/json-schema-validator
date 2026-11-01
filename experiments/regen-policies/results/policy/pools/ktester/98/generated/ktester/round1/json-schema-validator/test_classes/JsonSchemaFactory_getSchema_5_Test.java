package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /**
     * Factory shared by all test methods.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the standard factory for Draft‑2020‑12. No customisation is required for the
        // simple schemas used in these tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up; the factory does not hold external resources that need explicit closing.
        factory = null;
    }

    /**
     * Provides empty and whitespace‑only strings for the parameterised test above.
     */
    private static Stream<String> emptyOrBlankProvider() {
        return Stream.of(// completely empty
        "", // spaces
        "   ", // other whitespace characters
        "\t\n\r", // mixed whitespace
        "  \t \n  ");
    }

    /**
     * Simple happy‑path test: a valid JSON schema string should produce a non‑null JsonSchema.
     */
    @Test
    void getSchema_validJsonString_returnsJsonSchema() {
        // GIVEN a minimal JSON schema that validates a string value
        String schema = "{ \"type\": \"string\" }";
        // WHEN the factory parses the schema string
        JsonSchema jsonSchema = factory.getSchema(schema);
        // THEN a JsonSchema instance is returned and it is not null
        assertNotNull(jsonSchema, "JsonSchema should not be null for a valid schema string");
        // The concrete type is implementation‑specific; we only assert the contract.
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object must implement JsonSchema");
    }

    /**
     * Verify that an ill‑formed JSON string results in an IOException.
     */
    @Test
    void getSchema_invalidJsonString_throwsIOException() {
        // GIVEN an invalid JSON document (missing closing brace)
        String malformedSchema = "{ \"type\": \"string\" ";
        // WHEN/THEN the factory propagates an IOException
        assertThrows(IOException.class, () -> factory.getSchema(malformedSchema));
    }

    /**
     * Ensure that a null argument is rejected with a NullPointerException.
     */
    @Test
    void getSchema_nullString_throwsNullPointerException() {
        // GIVEN a null reference
        String nullSchema = null;
        // WHEN/THEN the factory should throw NPE (the implementation checks for null)
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullSchema));
    }

    /**
     * Parameterised test for inputs that are empty or contain only whitespace.
     * The factory is expected to reject such inputs, typically with an
     * {@link IllegalArgumentException} (or a custom {@code JsonSchemaFactoryException}).
     */
    @ParameterizedTest(name = "[{index}] emptyOrBlank=\"{0}\"")
    @MethodSource("emptyOrBlankProvider")
    void getSchema_emptyOrBlankString_throwsIllegalArgumentException(String input) {
        // WHEN/THEN an exception is thrown because the content cannot be parsed
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(input));
    }

    /**
     * Test that a syntactically correct JSON schema containing an unknown
     * type keyword fails during schema creation.  The concrete exception type
     * depends on the library version; we assert that *some* runtime exception
     * is thrown.
     */
    @Test
    void getSchema_unknownTypeKeyword_throwsRuntimeException() {
        // GIVEN a JSON schema where the type keyword is not part of the spec
        String schemaWithUnknownType = "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"unknown\" } } }";
        // WHEN/THEN the factory should raise an exception during schema processing
        assertThrows(RuntimeException.class, () -> factory.getSchema(schemaWithUnknownType));
    }
}
