package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class JsonSchemaFactory_getSchemaLocation_Test {

    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory instance using the public builder API.
        // The default meta‑schema configuration is sufficient for this test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        factory = null;
    }

    /**
     * Supplies the test data for {@link #getSchemaLocation_behaviour}.
     *
     * @return a stream of arguments: (input, expected, description)
     */
    private static Stream<Arguments> schemaLocationProvider() {
        return Stream.of(// Non‑null, non‑default constant – should be returned unchanged.
        Arguments.of(SchemaLocation.SCHEMA, SchemaLocation.SCHEMA, "When a non‑null SchemaLocation (SCHEMA) is supplied, the same instance should be returned."), // Null input – should fall back to DOCUMENT.
        Arguments.of(null, SchemaLocation.DOCUMENT, "When the supplied SchemaLocation is null, the method should fall back to DOCUMENT."), // The default constant itself – still returned unchanged.
        Arguments.of(SchemaLocation.DOCUMENT, SchemaLocation.DOCUMENT, "When the supplied SchemaLocation is the default constant DOCUMENT, it should be returned unchanged."));
    }

    @Test
    void getSchemaLocation_returnsSameInstanceWhenNonNull() {
        // GIVEN a non‑null SchemaLocation (use the documented DOCUMENT constant)
        SchemaLocation given = SchemaLocation.DOCUMENT;
        // WHEN the method is invoked
        SchemaLocation result = factory.getSchemaLocation(given);
        // THEN the same instance is returned
        assertSame(given, result, "When a non‑null SchemaLocation is supplied, the same instance should be returned.");
    }

    @Test
    void getSchemaLocation_returnsDocumentWhenNull() {
        // GIVEN a null argument
        SchemaLocation given = null;
        // WHEN the method is invoked
        SchemaLocation result = factory.getSchemaLocation(given);
        // THEN the default DOCUMENT constant is returned
        assertSame(SchemaLocation.DOCUMENT, result, "When null is supplied, the method should return SchemaLocation.DOCUMENT.");
    }

    /**
     * Simple sanity check for the {@code DOCUMENT} constant – already covered
     * by the original test but kept for explicitness.
     */
    @Test
    void getSchemaLocation_returnsSameInstanceWhenNonNull_Document() {
        // GIVEN a non‑null SchemaLocation (DOCUMENT)
        SchemaLocation given = SchemaLocation.DOCUMENT;
        // WHEN the method is invoked
        SchemaLocation result = factory.getSchemaLocation(given);
        // THEN the same instance is returned
        assertSame(given, result, "When a non‑null SchemaLocation is supplied, the same instance should be returned.");
    }

    /**
     * Parameterized test that drives all scenarios described in the JSON test case
     * definition. It checks both the true and false branches of the ternary operator.
     *
     * @param input    the argument passed to {@code getSchemaLocation}
     * @param expected the expected return value
     * @param description textual description for debugging purposes
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("schemaLocationProvider")
    void getSchemaLocation_behaviour(SchemaLocation input, SchemaLocation expected, String description) {
        // WHEN
        SchemaLocation actual = factory.getSchemaLocation(input);
        // THEN
        assertSame(expected, actual, () -> "Failed scenario: " + description);
    }
}
