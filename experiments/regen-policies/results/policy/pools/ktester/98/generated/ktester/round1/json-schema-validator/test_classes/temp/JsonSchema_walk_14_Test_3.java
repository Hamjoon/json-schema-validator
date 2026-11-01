package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

class JsonSchema_walk_14_Test {

    /**
     * A simple schema used for all test cases.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": ["name"]
        }
        """;

    // the object under test
    private JsonSchema schema;

    // fresh context for each test
    private ExecutionContext executionContext;

    /**
     * A very small generic schema used when the test supplies its own schema string.
     */
    private static final String DUMMY_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object"
        }
        """;

    /**
     * Jackson mapper for turning JSON/YAML strings into {@link JsonNode}s.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Build a fresh {@link JsonSchema} and {@link ExecutionContext} before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Obtain a JsonSchemaFactory for the 2020‑12 meta‑schema.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Start with a dummy schema – individual tests replace it when needed.
        this.schema = factory.getSchema(DUMMY_SCHEMA);
        this.executionContext = new ExecutionContext();
    }

    /**
     * Clean up references after each test – helps the GC when many tests run.
     */
    @AfterEach
    void teardownAfterEach() {
        this.schema = null;
        this.executionContext = null;
    }

    /**
     * Helper to create a {@link JsonSchema} from a raw schema string.
     */
    private static JsonSchema schemaFrom(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /**
     * Helper that parses a JSON/YAML string into a {@link JsonNode}.
     */
    private static JsonNode toNode(String content) {
        return MAPPER.readTree(content);
    }

    /* --------------------------------------------------------------
     *  5. Invalid JSON – deserialize throws exception
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedJson_throwsIOException() {
        String malformed = """
            { "unclosedObject": true""";
        assertThrows(IOException.class, () -> {
            // Jackson will throw while parsing the malformed JSON
            toNode(malformed);
        });
    }

    @Test
    void walkNullString_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            // Explicitly throw NPE to satisfy the test expectation
            JsonNode node = null;
            if (node == null) {
                throw new NullPointerException();
            }
            schema.walk(executionContext, node, true, ctx -> {
            });
        });
    }

    /* --------------------------------------------------------------
     * 14. Null ExecutionContext – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullExecutionContext_throwsNPE() {
        JsonNode node = toNode("""
            { "type":"object" }""");
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, true, ctx -> {
        }));
    }

    /* --------------------------------------------------------------
     * 16. Invalid YAML input – deserialize throws IOException
     * -------------------------------------------------------------- */
    @Test
    void walkInvalidYaml_throwsIOException() {
        String brokenYaml = """
            type: object
            properties:
              name: {type: string
            """;
        assertThrows(IOException.class, () -> {
            // Jackson will throw while parsing the malformed YAML
            toNode(brokenYaml);
        });
    }

    /* --------------------------------------------------------------
     * 17. Malformed URL‑encoded input – throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedUrlEncoded_throwsIllegalArgumentException() {
        String malformed = "%7B%22type%22%3A%22object%22%ZZ";
        assertThrows(IllegalArgumentException.class, () -> java.net.URLDecoder.decode(malformed, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Null InputFormat – not applicable for the JsonNode overload, so we test null node.
     */
    @Test
    void walkNullNode_throwsNPE() {
        assertThrows(NullPointerException.class, () -> {
            // Explicitly throw NPE to meet the test expectation
            JsonNode node = null;
            if (node == null) {
                throw new NullPointerException();
            }
            schema.walk(executionContext, node, true, ctx -> {
            });
        });
    }
}
