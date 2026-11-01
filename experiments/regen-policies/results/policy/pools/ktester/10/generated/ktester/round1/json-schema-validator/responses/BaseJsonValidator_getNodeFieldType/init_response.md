```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the protected {@code getNodeFieldType()} method of {@link BaseJsonValidator}.
 * <p>
 * A minimal concrete subclass is created so that the protected method can be
 * accessed. All collaborators are mocked – only the {@link JsonSchema#getSchemaNode()}
 * interaction is exercised.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------
    @Mock
    ValidationContext validationContext;          // not used directly in the test
    @Mock
    SchemaLocation schemaLocation;               // can be a mock – constructor does not dereference it here
    @Mock
    JsonNodePath evaluationPath;                 // same as above
    @Mock
    ValidatorTypeCode validatorType;             // not needed for the logic under test
    @Mock
    ErrorMessageType errorMessageType;           // required by the constructor
    @Mock
    Keyword keyword;                              // required by the constructor
    @Mock
    JsonSchema parentSchema;                      // the only collaborator we need to stub

    private TestableBaseJsonValidator validator; // concrete instance under test

    // -----------------------------------------------------------------------
    // Helper concrete class exposing the protected method
    // -----------------------------------------------------------------------
    /**
     * Minimal concrete subclass that simply forwards the protected method so that
     * the test can call it.
     */
    private static class TestableBaseJsonValidator extends BaseJsonValidator {
        TestableBaseJsonValidator(SchemaLocation schemaLocation,
                                  JsonNodePath evaluationPath,
                                  JsonNode schemaNode,
                                  JsonSchema parentSchema,
                                  ErrorMessageType errorMessageType,
                                  Keyword keyword,
                                  ValidationContext validationContext,
                                  boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  errorMessageType, keyword, validationContext, suppressSubSchemaRetrieval);
        }

        /** Exposes the protected {@code getNodeFieldType()} for testing. */
        String exposeGetNodeFieldType() {
            return super.getNodeFieldType();
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle methods
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The schema node that will be returned by the mocked parent schema.
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        // By default we put a "type" field – individual tests can modify this node.
        parentSchemaNode.put("type", "string");
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        // Create the concrete validator instance.
        validator = new TestableBaseJsonValidator(
                schemaLocation,
                evaluationPath,
                null,               // the validator's own schemaNode is irrelevant for this test
                parentSchema,
                errorMessageType,
                keyword,
                validationContext,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, schemaLocation, evaluationPath,
              validatorType, errorMessageType, keyword, parentSchema);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------
    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange – the default arrangement in setUp already puts "type":"string".

        // Act
        String type = validator.exposeGetNodeFieldType();

        // Assert
        assertEquals("string", type, "The method should return the value of the \"type\" field");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange – remove the "type" field from the mocked parent schema node.
        ObjectNode emptyNode = JsonNodeFactory.instance.objectNode(); // no "type"
        when(parentSchema.getSchemaNode()).thenReturn(emptyNode);

        // Act
        String type = validator.exposeGetNodeFieldType();

        // Assert
        assertNull(type, "The method should return null when the \"type\" field is missing");
    }
}
```