```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link OneOfValidator#preloadJsonSchema()}.
 *
 * The test verifies that:
 * <ul>
 *   <li>each {@link JsonSchema} contained in the {@code OneOfValidator} has its
 *       {@code initializeValidators()} method invoked, and</li>
 *   <li>the protected {@code canShortCircuit()} method is called exactly once
 *       (the result is cached inside the validator).</li>
 * </ul>
 *
 * All collaborators are mocked – no real schema parsing or I/O is performed.
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    /** Mocks used in the tests. */
    @Mock ValidationContext validationContext;

    /** Two mock schemas that will be returned by the mocked {@link ValidationContext}. */
    @Mock JsonSchema childSchema1;
    @Mock JsonSchema childSchema2;

    /** The validator under test. */
    OneOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---- 1. Prepare a minimal JSON schema node that represents a "oneOf" array ----
        // The actual content of the child objects is irrelevant for this test.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree("[{}, {}]"); // two empty subschemas

        // ---- 2. Build the required constructor arguments -------------------------
        // AbsoluteIri and PathType are part of the library; use simple values.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri,
                new JsonNodePath(PathType.JSON_POINTER));

        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // ---- 3. Configure the ValidationContext mock to return our child schemas ----
        // The constructor of OneOfValidator iterates over the array and calls
        // validationContext.newSchema(...) for each element.
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         any(JsonNode.class),
                                         any(JsonSchema.class)))
                .thenReturn(childSchema1, childSchema2);

        // ---- 4. Instantiate the validator ----------------------------------------
        validator = new OneOfValidator(schemaLocation,
                                       evaluationPath,
                                       schemaNode,
                                       null,               // parent schema – not needed here
                                       validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, childSchema1, childSchema2);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void preloadJsonSchema_initializesAllChildSchemas_andCallsCanShortCircuit() {
        // Spy the validator so we can verify the protected method call.
        OneOfValidator spyValidator = spy(validator);

        // ---- 5. Execute the method under test ------------------------------------
        spyValidator.preloadJsonSchema();

        // ---- 6. Verify interactions -----------------------------------------------
        // Each child schema must have its initialise method invoked exactly once.
        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();

        // The protected helper canShortCircuit() must be called once.
        verify(spyValidator, times(1)).canShortCircuit();

        // No other unexpected interactions.
        verifyNoMoreInteractions(childSchema1, childSchema2, validationContext);
    }
}
```