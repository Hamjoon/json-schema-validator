```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    /** Simple concrete implementation used only for testing the protected method. */
    static class TestValidator extends BaseJsonValidator {
        TestValidator(JsonSchema parentSchema) {
            // All other arguments are not needed for the getNodeFieldType() logic.
            super(null, null, null, parentSchema, null, null);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Not used in these tests.
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // given
        JsonSchema parentSchema = mock(JsonSchema.class);
        JsonNode parentSchemaNode = mock(JsonNode.class);
        JsonNode typeNode = mock(JsonNode.class);

        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchemaNode.get("type")).thenReturn(typeNode);
        when(typeNode.asText()).thenReturn("string");

        TestValidator validator = new TestValidator(parentSchema);

        // when
        String result = validator.getNodeFieldType();

        // then
        assertEquals("string", result);
    }

    @Test
    void getNodeFieldType_returnsNullWhenNoTypeField() {
        // given
        JsonSchema parentSchema = mock(JsonSchema.class);
        JsonNode parentSchemaNode = mock(JsonNode.class);

        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchemaNode.get("type")).thenReturn(null);

        TestValidator validator = new TestValidator(parentSchema);

        // when
        String result = validator.getNodeFieldType();

        // then
        assertNull(result);
    }
}
```