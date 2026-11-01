```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /*** Mocks and test fixtures ***/
    @Mock
    ValidationContext validationContext;          // used by the constructor

    @Mock
    SchemaLocation schemaLocation;               // constructor argument – not used directly

    @Mock
    JsonNodePath evaluationPath;                 // constructor argument – not used directly

    @Mock
    JsonSchema parentSchema;                     // constructor argument – can be null, mocked for safety

    JsonNode schemaNode;                         // the “oneOf” array node supplied to the constructor
    ExecutionContext executionContext;           // real instance – lightweight
    JsonNode instanceNode;                       // node being validated (mocked)
    JsonNode rootNode;                           // root of the document (mocked)
    JsonNodePath instanceLocation;               // location of the instance (mocked)

    OneOfValidator validator;                    // system under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a simple array node that mimics a “oneOf” definition with two sub‑schemas
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(mapper.createObjectNode()); // first child schema (empty – content irrelevant)
        arrayNode.add(mapper.createObjectNode()); // second child schema
        this.schemaNode = arrayNode;

        // ExecutionContext can be a real object – it has no side effects for this test
        this.executionContext = new ExecutionContext();

        // Mock the JsonNode arguments used by walk()
        this.instanceNode = mock(JsonNode.class);
        this.rootNode = mock(JsonNode.class);
        this.instanceLocation = mock(JsonNodePath.class);

        // Stub ValidationContext.newSchema(...) so that the constructor can fill the internal
        // “schemas” list with mock JsonSchema objects.
        JsonSchema mockSubSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         any(JsonNode.class),
                                         any(JsonSchema.class)))
                .thenReturn(mockSubSchema);

        // The constructor will call newSchema twice (array size = 2) – the same mock is fine.
        // Build the validator under test.
        this.validator = new OneOfValidator(schemaLocation,
                                            evaluationPath,
                                            schemaNode,
                                            parentSchema,
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

    /*** Test when shouldValidateSchema == true – validate() must be invoked and its result returned ***/
    @Test
    void walk_ShouldValidateSchema_InvokesValidateAndReturnsMessages() {
        // Arrange – spy the validator so we can stub validate()
        OneOfValidator spyValidator = spy(this.validator);

        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expectedMessages = new LinkedHashSet<>();
        expectedMessages.add(mockMessage);

        // Stub the protected validate(...) method (declared in BaseJsonValidator)
        doReturn(expectedMessages).when(spyValidator)
                .validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));

        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext,
                                                          instanceNode,
                                                          rootNode,
                                                          instanceLocation,
                                                          true); // shouldValidateSchema = true

        // Assert
        assertSame(expectedMessages, actual, "walk should return the set produced by validate()");
        // Verify that validate was called exactly once with the same arguments
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));
    }

    /*** Test when shouldValidateSchema == false – each child schema’s walk() must be invoked ***/
    @Test
    void walk_ShouldNotValidateSchema_DelegatesToChildSchemas() {
        // Arrange – the validator already contains two mock child schemas (created in @BeforeEach)
        // Retrieve them via reflection (allowed because they are package‑private in the test)
        // but we avoid accessing private fields; instead we rely on the behaviour of the
        // mocked ValidationContext which always returns the same mock JsonSchema instance.
        JsonSchema childSchema = validationContext.newSchema(null, null, null, null);
        // Stub the child schema’s walk() to return an empty set (the return value is ignored)
        when(childSchema.walk(any(ExecutionContext.class),
                              any(JsonNode.class),
                              any(JsonNode.class),
                              any(JsonNodePath.class),
                              eq(false)))
                .thenReturn(Collections.emptySet());

        // Act
        Set<ValidationMessage> result = validator.walk(executionContext,
                                                       instanceNode,
                                                       rootNode,
                                                       instanceLocation,
                                                       false); // shouldValidateSchema = false

        // Assert – result should be empty because we never added messages
        assertTrue(result.isEmpty(), "When shouldValidateSchema is false, walk should return an empty set");

        // Verify that each child schema’s walk() was invoked exactly once
        verify(childSchema, times(2))
                .walk(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation), eq(false));
    }
}
```