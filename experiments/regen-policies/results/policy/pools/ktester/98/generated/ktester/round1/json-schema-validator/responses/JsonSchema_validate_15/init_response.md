```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchema_validate_15_Test {

    /** Shared schema used by all test cases */
    private static JsonSchema schema;

    /** Input format used for the tests (JSON string) */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /** Simple {@link OutputFormat} implementation that materialises the set of
     *  {@link ValidationMessage}s into a {@link List}. */
    private static final OutputFormat<List<ValidationMessage>> LIST_OUTPUT_FORMAT =
            messages -> new ArrayList<>(messages);

    /** No‑op customiser – required by the method signature but does not alter the
     *  execution context. */
    private static final ExecutionContextCustomizer NO_OP_CUSTOMIZER = ctx -> {
        // intentionally left blank
    };

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that expects an object with a string property “name”.
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";

        // Build a factory for the 2020‑12 draft (the same version used in the
        // existing test suite) and obtain a JsonSchema instance.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));

        // No special configuration required for these tests.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        schema = factory.getSchema(schemaData, config);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the current test suite – the schema is immutable
        // and can be left for garbage collection.
        schema = null;
    }

    @Test
    void validate_invalidInput_returnsValidationMessages() {
        // Input violates the schema: “name” is an integer, not a string.
        String badInput = "{\"name\":1}";

        List<ValidationMessage> messages = schema.validate(
                badInput,
                INPUT_FORMAT,
                LIST_OUTPUT_FORMAT,
                NO_OP_CUSTOMIZER);

        // The schema should produce exactly one validation error about the type.
        assertEquals(1, messages.size(), "Expected a single validation message");
        ValidationMessage vm = messages.get(0);
        assertTrue(vm.getMessage().toLowerCase().contains("type"),
                "Message should mention a type problem, but was: " + vm.getMessage());
    }

    @Test
    void validate_validInput_returnsEmptyList() {
        // Input conforms to the schema.
        String goodInput = "{\"name\":\"John\"}";

        List<ValidationMessage> messages = schema.validate(
                goodInput,
                INPUT_FORMAT,
                LIST_OUTPUT_FORMAT,
                NO_OP_CUSTOMIZER);

        // No validation errors expected.
        assertTrue(messages.isEmpty(), "Expected no validation messages for valid input");
    }

    @Test
    void validate_withCustomExecutionContextCustomizer_isInvoked() {
        // Verify that the customiser is actually called by toggling a flag on the
        // execution context.  We use a simple mutable holder for the side‑effect.
        final boolean[] customiserInvoked = {false};

        ExecutionContextCustomizer customizer = ctx -> customiserInvoked[0] = true;

        String input = "{\"name\":\"Alice\"}";
        schema.validate(input, INPUT_FORMAT, LIST_OUTPUT_FORMAT, customizer);

        assertTrue(customiserInvoked[0], "ExecutionContextCustomizer should have been invoked");
    }
}
```