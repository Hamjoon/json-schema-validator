package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    // the object under test
    private JsonSchema schema;

    // execution context used for walkAtNode
    private ExecutionContext execContext;

    // node to be validated
    private JsonNode node;

    // root of the document
    private JsonNode rootNode;

    // location of the node inside the document
    private JsonNodePath instanceLocation;

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Build a minimal (empty) schema – this is enough to obtain a JsonSchema instance.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // an empty schema accepts any JSON value
        schema = factory.getSchema("{}");
        // 2️⃣ Create a fresh execution context for each test.
        execContext = schema.createExecutionContext();
        // 3️⃣ Prepare a simple JSON node (e.g. a plain string) and its root.
        node = JsonNodeFactory.instance.textNode("hello");
        // for this test the node is also the root
        rootNode = node;
        // 4️⃣ Build a JsonNodePath that points to the root.
        // The constructor that takes a PathType can be used for the root location.
//         instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
    }

//     @Test
//     void walkAtNode_returnsEmptyResultForValidNode() {
//         // GIVEN – a valid node under an empty schema (the schema imposes no constraints)
//         // WHEN – the walkAtNode method is invoked
//         ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
//         // THEN – the result should be non‑null and contain no validation messages
//         assertNotNull(result, "walkAtNode must never return null");
//         assertTrue(result.isValid(), "Expected no validation messages for a valid node");
//     }

//     @Test
//     void walkAtNode_returnsResultWhenValidationDisabled() {
//         // GIVEN – the same valid node but with validation turned off
//         // WHEN – walkAtNode is called with validate = false
//         ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, false);
//         // THEN – the method still returns a ValidationResult (the library returns an empty result)
//         assertNotNull(result, "Result must be non‑null even when validation is disabled");
//         // No messages should be present because validation was skipped
//         assertTrue(result.isValid(), "No messages should be produced when validation is disabled");
//     }
}