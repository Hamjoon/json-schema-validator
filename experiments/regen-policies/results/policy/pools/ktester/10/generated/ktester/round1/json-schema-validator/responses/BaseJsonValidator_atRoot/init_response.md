```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.i18n.DefaultMessageSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private ValidationContext validationContext;
    @Mock
    private SchemaValidatorsConfig config;
    @Mock
    private SchemaLocation schemaLocation;
    @Mock
    private JsonNodePath evaluationPath;
    @Mock
    private JsonNode schemaNode;
    @Mock
    private JsonSchema parentSchema;
    @Mock
    private ValidatorTypeCode validatorType;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext to return the mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Default stubs for config – they can be overridden in individual tests
        when(config.isFailFast()).thenReturn(false);
        when(config.isCustomMessageSupported()).thenReturn(true);
        when(config.getMessageSource()).thenReturn(DefaultMessageSource.getInstance());
        // config.getPathType() returns null by default, which triggers the DEFAULT path type
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
     * Concrete subclass of {@link BaseJsonValidator} exposing the protected {@code atRoot()} method.
     */
    private static class TestValidator extends BaseJsonValidator {
        private final JsonNodePath evaluationPath;

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.evaluationPath = evaluationPath;
        }

        @Override
        public String getKeyword() {
            return "test";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return evaluationPath;
        }

        @Override
        protected String getNodeFieldType() {
            return null;
        }

        /** Exposes the protected {@code atRoot()} method for testing. */
        public JsonNodePath callAtRoot() {
            return super.atRoot();
        }
    }

    @Test
    void testAtRootUsesDefaultPathType() throws Exception {
        // No explicit stub for config.getPathType() – it returns null → DEFAULT is used
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath rootPath = validator.callAtRoot();
        assertNotNull(rootPath, "Root path should not be null");

        // Verify that the path is a root (no parent, no segment)
        Field parentField = JsonNodePath.class.getDeclaredField("parent");
        parentField.setAccessible(true);
        assertNull(parentField.get(rootPath), "Root path should have no parent");

        Field segmentField = JsonNodePath.class.getDeclaredField("pathSegment");
        segmentField.setAccessible(true);
        assertNull(segmentField.get(rootPath), "Root path should have no segment");

        // Verify the path type is DEFAULT
        assertEquals(PathType.DEFAULT, rootPath.getPathType(),
                "Path type should be DEFAULT when config does not specify a type");
    }

    @Test
    void testAtRootUsesCustomPathType() throws Exception {
        // Stub config to return a specific PathType
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath rootPath = validator.callAtRoot();
        assertNotNull(rootPath, "Root path should not be null");

        // Verify that the path is a root (no parent, no segment)
        Field parentField = JsonNodePath.class.getDeclaredField("parent");
        parentField.setAccessible(true);
        assertNull(parentField.get(rootPath), "Root path should have no parent");

        Field segmentField = JsonNodePath.class.getDeclaredField("pathSegment");
        segmentField.setAccessible(true);
        assertNull(segmentField.get(rootPath), "Root path should have no segment");

        // Verify the path type matches the stubbed value
        assertEquals(PathType.JSON_POINTER, rootPath.getPathType(),
                "Path type should reflect the configured value");
    }
}
```