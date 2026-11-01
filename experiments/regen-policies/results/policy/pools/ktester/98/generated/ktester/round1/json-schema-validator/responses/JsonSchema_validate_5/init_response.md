```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_5_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // a very small schema that requires a numeric "age" property
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  },
                  "required": [ "age" ]
                }
                """;

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up per‑test
    }

    @AfterAll
    static void teardownAfterAll() {
        // no global cleanup required
    }

    @Test
    void validate_ValidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
        // given: a valid JSON node and a mocked OutputFormat
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        when(format.apply(any())).thenReturn("formatted‑ok");

        // when: validate is invoked
        String result = schema.validate(validNode, format);

        // then: the OutputFormat is called with an empty set of messages and the result is returned
        assertEquals("formatted‑ok", result);
        // capture the argument passed to the format – it must be an empty set for a valid instance
        verify(format, times(1)).apply(argThat(messages -> messages != null && messages.isEmpty()));
    }

    @Test
    void validate_InvalidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
        // given: an invalid JSON node (age should be integer) and a mocked OutputFormat
        JsonNode invalidNode = MAPPER.readTree("{\"age\":\"thirty\"}");
        when(format.apply(any())).thenReturn("formatted‑error");

        // when: validate is invoked
        String result = schema.validate(invalidNode, format);

        // then: the OutputFormat receives a non‑empty set of ValidationMessage(s)
        assertEquals("formatted‑error", result);
        verify(format, times(1)).apply(argThat((Set<ValidationMessage> msgs) -> msgs != null && !msgs.isEmpty()));
    }
}
```