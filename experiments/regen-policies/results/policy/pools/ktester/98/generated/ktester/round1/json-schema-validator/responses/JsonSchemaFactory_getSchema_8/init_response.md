```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    /** Factory used for all tests – created once because it is thread‑safe. */
    private static JsonSchemaFactory factory;

    /** Minimal JSON schema used as input for the focal method. */
    private static JsonNode simpleSchemaNode;

    /** Mocked configuration – the factory does not inspect the mock, it only forwards it. */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Use the public factory method – it builds a fully‑initialised instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // A tiny, valid JSON‑Schema document.
        ObjectMapper mapper = new ObjectMapper();
        simpleSchemaNode = mapper.readTree("{\"type\":\"object\"}");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that previous interactions do not interfere.
        reset(mockConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to dispose of; the factory holds no external resources that need explicit shutdown.
    }

    @Test
    void getSchema_withJsonNodeAndConfig_returnsJsonSchema() {
        // GIVEN – a factory, a simple schema node and a (mocked) configuration
        // WHEN
        JsonSchema result = factory.getSchema(simpleSchemaNode, mockConfig);
        // THEN
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        // The returned JsonSchema should contain the same schema node that we supplied.
        // JsonSchema exposes the original node via getSchemaNode() – this is part of the public API.
        assertEquals(simpleSchemaNode, result.getSchemaNode(),
                "The JsonSchema should carry the original schema node");
        // Verify that the configuration object was passed through without being altered.
        verifyNoInteractions(mockConfig);
    }

    @Test
    void getSchema_withNullJsonNode_throwsNullPointerException() {
        // GIVEN – a null JsonNode
        JsonNode nullNode = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullNode, mockConfig),
                "Calling getSchema with a null JsonNode should result in NPE");
    }
}
```