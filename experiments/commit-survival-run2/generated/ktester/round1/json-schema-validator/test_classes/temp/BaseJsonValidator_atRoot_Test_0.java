package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private SchemaValidatorsConfig mockConfig;

    @Mock
    private PathType mockPathType;

    @Mock
    private JsonNode mockSchemaNode;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     */
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the atRoot() tests.
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing for the "custom PathType" tests.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getPathType()).thenReturn(mockPathType);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks between tests to avoid cross‑test contamination.
        Mockito.reset(mockParentSchema, mockValidatorType, mockValidationContext, mockConfig, mockPathType, mockSchemaNode);
    }

    /**
     * --------------------------------------------------------------
     *  2️⃣  Normal behaviour with several PathType implementations
     *  --------------------------------------------------------------
     */
    private static Stream<RootProvider> normalRootProvider() {
        return Stream.of(new RootProvider("$", "PathType returning JSONPath root \"$\""), new RootProvider("", "PathType returning empty string (JSON Pointer)"), new RootProvider("/", "PathType returning \"/\" (JSON Pointer root)"));
    }

    @Test
    void atRoot_returnsDefaultRoot_whenValidationContextIsNull() {
        // Arrange – create the validator with a null ValidationContext so that
        // the constructor falls back to PathType.DEFAULT.
        DummyValidator validator = new DummyValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, // <-- null ValidationContext
        null, false);
        // Act
        String root = validator.atRoot();
        // Assert – PathType.DEFAULT's root is "$" in the library.
        assertEquals("$", root, "When ValidationContext is null the validator should use PathType.DEFAULT and return \"$\" as the root path.");
    }

    @Test
    void atRoot_returnsCustomRoot_whenPathTypeIsProvided() {
        // Arrange – stub the custom PathType to return a specific root string.
        when(mockPathType.getRoot()).thenReturn("customRoot");
        DummyValidator validator = new DummyValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, // <-- non‑null context
        mockValidationContext, false);
        // Act
        String root = validator.atRoot();
        // Assert
        assertEquals("customRoot", root, "The atRoot() method should delegate to the PathType supplied via ValidationContext.");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("normalRootProvider")
    void atRoot_returnsProvidedRoot_forVariousPathTypes(RootProvider provider) {
        // Stub the mocked PathType to return the desired root string.
        when(mockPathType.getRoot()).thenReturn(provider.expectedRoot);
        DummyValidator validator = new DummyValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, // non‑null context → uses mocked PathType
        mockValidationContext, false);
        String root = validator.atRoot();
        Assertions.assertEquals(provider.expectedRoot, root, "atRoot() should return the root supplied by PathType.getRoot()");
    }

    /**
     * --------------------------------------------------------------
     *  3️⃣  Custom PathType that returns null
     *  --------------------------------------------------------------
     */
    @Test
    void atRoot_propagatesNull_whenPathTypeReturnsNull() {
        when(mockPathType.getRoot()).thenReturn(null);
        DummyValidator validator = new DummyValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, false);
        Assertions.assertNull(validator.atRoot(), "If PathType.getRoot() returns null, atRoot() should return null without throwing.");
    }

    /**
     * --------------------------------------------------------------
     *  4️⃣  Null PathType → NullPointerException
     *  --------------------------------------------------------------
     */
    @Test
    void atRoot_throwsNullPointerException_whenPathTypeIsNull() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, false);
        // Force the private 'pathType' field to null via reflection.
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, null);
        Assertions.assertThrows(NullPointerException.class, validator::atRoot, "Calling atRoot() with a null PathType should result in NullPointerException.");
    }

    /**
     * --------------------------------------------------------------
     *  5️⃣  PathType#getRoot() throws an unchecked exception → propagation
     *  --------------------------------------------------------------
     */
    @Test
    void atRoot_propagatesRuntimeException_whenPathTypeThrows() {
        when(mockPathType.getRoot()).thenThrow(new IllegalStateException("boom"));
        DummyValidator validator = new DummyValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, false);
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, validator::atRoot, "The exception from PathType.getRoot() should be propagated unchanged.");
        Assertions.assertEquals("boom", ex.getMessage(), "Propagated exception should retain the original message.");
    }
}
