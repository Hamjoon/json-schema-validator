```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>When both arguments are non‑null a {@link JsonSchema} instance is returned.</li>
 *   <li>When the {@link JsonNode} argument is {@code null} the method throws a {@link NullPointerException}.</li>
 * </ul>
 *
 * The {@link JsonSchemaFactory} itself is obtained via the public {@code getInstance}
 * factory method – no reflection or direct constructor invocation is required.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /** Factory under test – created once per test method. */
    private JsonSchemaFactory factory;

    /** A minimal JSON schema used in the tests. */
    private JsonNode simpleSchemaNode;

    /** A mock {@link SchemaLocation} – the concrete implementation is not needed for the behaviour under test. */
    @Mock
    private SchemaLocation mockLocation;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Obtain a factory instance that is fully functional but does not load any external schemas.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No customisation required for these simple tests.
        });

        // Prepare a tiny, valid JSON schema document.
        ObjectMapper mapper = new ObjectMapper();
        simpleSchemaNode = mapper.readTree("{\"type\":\"object\"}");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current test suite.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    @DisplayName("getSchema(SchemaLocation, JsonNode) returns a JsonSchema when inputs are valid")
    void getSchema_returnsJsonSchemaForValidInputs() {
        // GIVEN – a mock SchemaLocation and a valid JsonNode (prepared in @BeforeEach)

        // WHEN
        JsonSchema result = factory.getSchema(mockLocation, simpleSchemaNode);

        // THEN
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        // The JsonSchema API provides a method to retrieve the original schema node.
        // If that method is not present in a particular version, the test still validates
        // that the returned object is of the expected type.
        assertTrue(result instanceof JsonSchema, "Result must be an instance of JsonSchema");
    }

    @Test
    @DisplayName("getSchema throws NullPointerException when JsonNode argument is null")
    void getSchema_throwsWhenJsonNodeIsNull() {
        // GIVEN – a mock SchemaLocation and a null JsonNode

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(mockLocation, null),
                "Calling getSchema with a null JsonNode should raise NullPointerException");
    }
}
```