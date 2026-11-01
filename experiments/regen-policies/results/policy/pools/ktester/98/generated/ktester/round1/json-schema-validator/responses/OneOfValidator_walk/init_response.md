```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /*** Mocks used for every test ***/
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instanceLocation;
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;

    /*** Two child schemas that will be returned from the ValidationContext ***/
    @Mock JsonSchema childSchema1;
    @Mock JsonSchema childSchema2;

    /*** The validator under test ***/
    OneOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Prepare a dummy schema node that represents a "oneOf" array.
        // The actual content of the array is irrelevant because we stub
        // ValidationContext#newSchema to return our mocks.
        // -----------------------------------------------------------------
        JsonNode schemaNode = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getObjectMapper()
                .createArrayNode()
                .addObject()   // first subschema
                .addObject();  // second subschema

        // -----------------------------------------------------------------
        // Stub ValidationContext so that each call to newSchema returns a
        // mocked JsonSchema. The order of calls matches the order in which
        // the constructor iterates over the array elements.
        // -----------------------------------------------------------------
        when(validationContext.newSchema(any(SchemaLocation.class),
                any(JsonNodePath.class),
                any(JsonNode.class),
                any(JsonSchema.class)))
                .thenReturn(childSchema1)   // for index 0
                .thenReturn(childSchema2);  // for index 1

        // -----------------------------------------------------------------
        // Build the OneOfValidator instance. The constructor will invoke
        // validationContext.newSchema twice and fill the private `schemas`
        // list with the two mocked child schemas.
        // -----------------------------------------------------------------
        validator = new OneOfValidator(schemaLocation,
                evaluationPath,
                schemaNode,
                null,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * When {@code shouldValidateSchema} is {@code true} the {@code walk}
     * method must delegate to {@code validate(..., true)} and return the
     * messages produced by that call.
     */
    @Test
    void walkShouldValidateAndReturnMessages() {
        // ---------------------------------------------------------------
        // Prepare a spy so we can stub the protected `validate` method.
        // ---------------------------------------------------------------
        OneOfValidator spyValidator = spy(validator);

        // Create a dummy set of ValidationMessage objects.
        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expectedMessages = new HashSet<>(Collections.singleton(dummyMessage));

        // Stub the protected validate method (the overload with the boolean flag).
        doReturn(expectedMessages).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        // ---------------------------------------------------------------
        // Invoke the method under test.
        // ---------------------------------------------------------------
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, node, rootNode,
                instanceLocation, true);

        // ---------------------------------------------------------------
        // Verify that the returned set is exactly the one produced by
        // the stubbed validate method.
        // ---------------------------------------------------------------
        assertSame(expectedMessages, actual, "walk should return the messages from validate");
    }

    /**
     * When {@code shouldValidateSchema} is {@code false} the validator must
     * walk each child schema but must not collect any messages itself.
     */
    @Test
    void walkShouldDelegateToChildrenWhenNotValidating() {
        // ---------------------------------------------------------------
        // Call walk with shouldValidateSchema = false.
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                instanceLocation, false);

        // ---------------------------------------------------------------
        // No messages are collected by OneOfValidator itself.
        // ---------------------------------------------------------------
        assertTrue(result.isEmpty(), "Result set should be empty when not validating the schema");

        // ---------------------------------------------------------------
        // Verify that each child schema received a walk call with the same
        // arguments that were passed to the validator.
        // ---------------------------------------------------------------
        verify(childSchema1, times(1)).walk(executionContext, node, rootNode, instanceLocation, false);
        verify(childSchema2, times(1)).walk(executionContext, node, rootNode, instanceLocation, false);
    }
}
```