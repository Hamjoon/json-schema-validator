```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit‑tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * <p>The tests use a real {@link JsonSchema} instance created via {@link JsonSchemaFactory}
 * so that the internal {@code OneOfValidator} is exercised without accessing any private
 * members.  Two scenarios are covered:</p>
 *
 * <ul>
 *   <li>A value that matches exactly one sub‑schema – validation must succeed.</li>
 *   <li>A value that matches more than one sub‑schema – validation must fail with a
 *       {@code oneOf} message that lists the matching indexes.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factory once for the whole test suite.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up required.
    }

    /**
     * The instance matches only the first subschema (type = integer) and therefore
     * the {@code oneOf} validator must succeed – i.e. no {@link ValidationMessage}s
     * are returned.
     */
    @Test
    void validate_whenExactlyOneSubSchemaMatches_thenNoMessages() throws Exception {
        // language=JSON
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "integer" },
                    { "minimum": 0 }
                  ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // The value -5 is an integer but does NOT satisfy the second subschema (minimum 0).
        JsonNode node = mapper.readTree("-5");

        Set<ValidationMessage> messages = schema.validate(node, InputFormat.JSON);
        assertTrue(messages.isEmpty(),
                "Expected no validation messages because exactly one subschema matches");
    }

    /**
     * The instance matches both subschemas (integer and minimum) which violates the
     * {@code oneOf} constraint.  The validator must return a single {@code oneOf}
     * message that lists the matching indexes {@code 0, 1}.
     */
    @Test
    void validate_whenMoreThanOneSubSchemaMatches_thenOneOfMessageReturned() throws Exception {
        // language=JSON
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "integer" },
                    { "minimum": 0 }
                  ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // The value 5 satisfies both subschemas.
        JsonNode node = mapper.readTree("5");

        Set<ValidationMessage> messages = schema.validate(node, InputFormat.JSON);
        assertEquals(1, messages.size(),
                "Exactly one validation message should be produced for a oneOf violation");

        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType(), "Message type must be 'oneOf'");
        assertEquals("$", vm.getInstanceLocation().toString(), "Instance location should be root ($)");
        assertEquals("$.oneOf", vm.getEvaluationPath().toString(),
                "Evaluation path should point to the oneOf keyword");
        assertTrue(vm.getMessage().contains("indexes '0, 1'"),
                "Message should contain the list of matching schema indexes");
    }
}
```