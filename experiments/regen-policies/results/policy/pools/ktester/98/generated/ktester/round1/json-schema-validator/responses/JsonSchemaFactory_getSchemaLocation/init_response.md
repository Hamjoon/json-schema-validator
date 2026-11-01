```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for the protected {@code getSchemaLocation} method of {@link JsonSchemaFactory}.
 *
 * The method simply returns the supplied {@link SchemaLocation} or {@link SchemaLocation#DOCUMENT}
 * when the argument is {@code null}.  Because the method is {@code protected}, the tests use
 * reflection to invoke it.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLocation_Test {

    private JsonSchemaFactory factory;
    private Method getSchemaLocationMethod;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a factory instance using the public API.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        // Obtain a reference to the protected method via reflection and make it accessible.
        getSchemaLocationMethod = JsonSchemaFactory.class
                .getDeclaredMethod("getSchemaLocation", SchemaLocation.class);
        getSchemaLocationMethod.setAccessible(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void getSchemaLocation_returnsDocumentWhenArgumentIsNull() throws Exception {
        // Act
        SchemaLocation result = (SchemaLocation) getSchemaLocationMethod.invoke(factory, (Object) null);

        // Assert
        assertEquals(SchemaLocation.DOCUMENT, result,
                "When the argument is null the method should return SchemaLocation.DOCUMENT");
    }

    @Test
    void getSchemaLocation_returnsSameInstanceWhenArgumentIsNonNull() throws Exception {
        // Arrange
        AbsoluteIri iri = new AbsoluteIri("http://example.com/schema");
        SchemaLocation input = new SchemaLocation(iri);

        // Act
        SchemaLocation result = (SchemaLocation) getSchemaLocationMethod.invoke(factory, input);

        // Assert
        assertSame(input, result,
                "When a non‑null SchemaLocation is supplied the method should return the same instance");
    }
}
```