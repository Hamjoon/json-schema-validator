package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLocation_Test {

    private JsonSchemaFactory factory;

    private Method getSchemaLocationMethod;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a factory instance using the public API.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Obtain a reference to the protected method via reflection and make it accessible.
        getSchemaLocationMethod = JsonSchemaFactory.class.getDeclaredMethod("getSchemaLocation", SchemaLocation.class);
        getSchemaLocationMethod.setAccessible(true);
    }

    /**
     * Supplies all enum constants that are part of the test specification.
     */
    private static Stream<Arguments> enumSchemaLocationProvider() {
        return Stream.of(// Explicit default constant – should be returned unchanged.
        Arguments.of(SchemaLocation.DOCUMENT), // Draft constants – each should be returned unchanged.
        Arguments.of(SchemaLocation.SCHEMA_2020_12), Arguments.of(SchemaLocation.SCHEMA_DRAFT_04), Arguments.of(SchemaLocation.SCHEMA_DRAFT_06), Arguments.of(SchemaLocation.SCHEMA_DRAFT_07), Arguments.of(SchemaLocation.SCHEMA_DRAFT_2019_09));
    }

    @Test
    void getSchemaLocation_returnsDocumentWhenArgumentIsNull() throws Exception {
        // Act
        SchemaLocation result = (SchemaLocation) getSchemaLocationMethod.invoke(factory, (Object) null);
        // Assert
        assertEquals(SchemaLocation.DOCUMENT, result, "When the argument is null the method should return SchemaLocation.DOCUMENT");
    }

    @Test
    void getSchemaLocation_returnsSameInstanceWhenArgumentIsNonNull() throws Exception {
        // Arrange
        AbsoluteIri iri = new AbsoluteIri("http://example.com/schema");
        SchemaLocation input = new SchemaLocation(iri);
        // Act
        SchemaLocation result = (SchemaLocation) getSchemaLocationMethod.invoke(factory, input);
        // Assert
        assertSame(input, result, "When a non‑null SchemaLocation is supplied the method should return the same instance");
    }

    @Test
    void getSchemaLocation_returnsSameInstanceWhenArgumentIsNonNullCustom() throws Exception {
        // Arrange – a non‑enum, custom SchemaLocation created from an AbsoluteIri.
        AbsoluteIri iri = new AbsoluteIri("http://example.com/schema");
        SchemaLocation input = new SchemaLocation(iri);
        // Act
        SchemaLocation result = (SchemaLocation) getSchemaLocationMethod.invoke(factory, input);
        // Assert
        assertSame(input, result, "When a non‑null SchemaLocation is supplied the method should return the same instance");
    }

    /**
     * Parameterized test that verifies the true branch of the ternary operator for
     * every enum constant defined in {@link SchemaLocation}. The method must return
     * the exact same enum instance that was passed in.
     */
    @ParameterizedTest(name = "[{index}] schemaLocation={0}")
    @MethodSource("enumSchemaLocationProvider")
    void getSchemaLocation_returnsSameInstanceForEnumConstants(SchemaLocation input) throws Exception {
        // Act
        SchemaLocation result = (SchemaLocation) getSchemaLocationMethod.invoke(factory, input);
        // Assert
        assertSame(input, result, () -> "Expected the same instance for enum constant " + input);
    }
}
