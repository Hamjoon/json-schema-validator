```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /** shared test utilities */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** objects created per test */
    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private JsonNode schemaNode;          // an array node that will create child schemas
    private OneOfValidator validator;    // instance under test
    private ExecutionContext execContext;
    private JsonNode dataNode;           // node to be validated
    private JsonNode rootNode;           // root of the document
    private String at;                   // JSON pointer location

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for now
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- mocks & stubs -------------------------------------------------
        validationContext = mock(ValidationContext.class);
        parentSchema = mock(JsonSchema.class);
        execContext = new ExecutionContext();               // real, lightweight
        dataNode = MAPPER.createObjectNode();               // simple empty object
        rootNode = MAPPER.createObjectNode();               // simple empty object
        at = "#";

        // ----- build a schemaNode that contains two child schemas ------------
        ArrayNode arrayNode = MAPPER.createArrayNode();
        // child 0
        ObjectNode child0 = MAPPER.createObjectNode();
        child0.put("type", "string");
        arrayNode.add(child0);
        // child 1
        ObjectNode child1 = MAPPER.createObjectNode();
        child1.put("type", "integer");
        arrayNode.add(child1);
        schemaNode = arrayNode;

        // ----- mock the creation of child JsonSchema objects ------------------
        JsonSchema childSchema0 = mock(JsonSchema.class);
        JsonSchema childSchema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), eq(child0), eq(parentSchema))).thenReturn(childSchema0);
        when(validationContext.newSchema(anyString(), eq(child1), eq(parentSchema))).thenReturn(childSchema1);

        // ----- construct the OneOfValidator (populates its internal list) ---
        validator = new OneOfValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == true the validator must delegate to
     *  its own validate(...) method and return whatever that method
     *  supplies.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaTrue_delegatesToValidate() {
        // arrange – stub validate to return a known set
        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = new HashSet<>();
        expected.add(mockMessage);

        OneOfValidator spyValidator = spy(validator);
        doReturn(expected).when(spyValidator).validate(any(), any(), any(), anyString());

        // act
        Set<ValidationMessage> actual = spyValidator.walk(execContext, dataNode, rootNode, at, true);

        // assert – the set returned by walk must be exactly the set from validate
        assertSame(expected, actual, "walk should return the set produced by validate");
        // also verify that validate was indeed called once
        verify(spyValidator, times(1)).validate(execContext, dataNode, rootNode, at);
    }

    /** --------------------------------------------------------------
     *  When shouldValidateSchema == false the validator must walk each
     *  child schema but must not call its own validate method.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaFalse_walksChildSchemas() {
        // arrange – retrieve the child schemas that were added by the constructor
        // (they are the mocks we returned from ValidationContext.newSchema)
        // we cannot access the private list directly, but we know there are exactly
        // two child schemas and they are the mocks we created earlier.
        JsonSchema childSchema0 = validationContext.newSchema("testPath/0", ((ArrayNode) schemaNode).get(0), parentSchema);
        JsonSchema childSchema1 = validationContext.newSchema("testPath/1", ((ArrayNode) schemaNode).get(1), parentSchema);

        // act
        Set<ValidationMessage> result = validator.walk(execContext, dataNode, rootNode, at, false);

        // assert – walk should return an empty set (no validation performed)
        assertTrue(result.isEmpty(), "Result set must be empty when shouldValidateSchema is false");

        // verify that each child schema's walk method was called exactly once
        verify(childSchema0, times(1)).walk(execContext, dataNode, rootNode, at, false);
        verify(childSchema1, times(1)).walk(execContext, dataNode, rootNode, at, false);
        // ensure that the validator's own validate method was never invoked
        // (using a spy to be safe)
        OneOfValidator spyValidator = spy(validator);
        spyValidator.walk(execContext, dataNode, rootNode, at, false);
        verify(spyValidator, never()).validate(any(), any(), any(), anyString());
    }
}
```