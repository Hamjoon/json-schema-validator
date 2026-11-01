package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    /**
     * Mocked validation context used by the concrete validator.
     */
    @Mock
    ValidationContext validationContext;

    /**
     * Mocked configuration returned from the validation context.
     */
    @Mock
    SchemaValidatorsConfig config;

    private ConcreteValidator validator;

    /**
     * Simple stub for {@link SchemaValidatorsConfig}.
     */
    private static class ConfigStub implements SchemaValidatorsConfig {

        private PathType pathType;

        ConfigStub(PathType pathType) {
            this.pathType = pathType;
        }

        @Override
        public PathType getPathType() {
            return this.pathType;
        }

        // The real interface contains many other methods; for the purpose of
        // these tests we only need {@code getPathType()}.
        // All other methods return default values.
        @Override
        public boolean isFailFast() {
            return false;
        }

        @Override
        public boolean isOpenAPI30() {
            return false;
        }

        @Override
        public boolean isOpenAPI31() {
            return false;
        }

        @Override
        public boolean isEcma262Validator() {
            return false;
        }

        @Override
        public boolean isTypeLoose() {
            return false;
        }

        @Override
        public boolean isHandleNullableField() {
            return false;
        }

        @Override
        public boolean isFailFastOnFirstError() {
            return false;
        }

        @Override
        public boolean isNullableSupport() {
            return false;
        }

        @Override
        public boolean isCustomMessageSupported() {
            return false;
        }

        @Override
        public boolean isValidateSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaOnly() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJson() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessage() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCode() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocation() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValue() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocation() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocation() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocation() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocation() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocation() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchema() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPath() {
            return false;
        }

        @Override
        public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValueAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocationAndSchemaAndPathAndLocation() {
            return false;
        }

        @Override
        public Map<String, Object> getAdditionalProperties() {
            return Collections.emptyMap();
        }
        // ... other methods omitted for brevity
    }

    /**
     * Simple stub for {@link ValidationContext}.
     */
    private static class ValidationContextStub extends ValidationContext {

        private final SchemaValidatorsConfig config;

        ValidationContextStub(SchemaValidatorsConfig config) {
            this.config = config;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return this.config;
        }
    }

    /**
     * Minimal concrete validator that delegates to {@code atRoot()}.
     */
    private static class ConcreteValidator extends BaseJsonValidator {

        ConcreteValidator(ValidationContext vc) {
            // The super‑constructor of BaseJsonValidator has many parameters.
            // For the purpose of these tests only the first three are relevant.
            super(// suppressSubSchemaRetrieval
            false, // schemaNode
            null, // validationContext
            vc, // errorMessageType
            null, // errorMessageKeyword
            null, // messageSource
            null, // keyword
            null, // parentSchema
            null, // schemaLocation
            null, // evaluationPath
            null, // evaluationParentSchema
            null, // errorMessage map
            null, // errorMessagePath
            null);
        }

        /**
         * Expose the protected {@code atRoot()} for testing.
         */
        JsonNodePath invokeAtRoot() {
            return atRoot();
        }

        /**
         * The abstract {@code validate} method – not needed for these tests.
         */
        @Override
        protected void validate(ExecutionContext executionContext, com.fasterxml.jackson.databind.JsonNode node, com.fasterxml.jackson.databind.JsonNode rootNode, JsonNodePath at) {
            // no‑op
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Wire the mocks: ValidationContext#getConfig() returns the mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Create a fresh validator instance for every test
        validator = new ConcreteValidator(validationContext);
        // default to JSON_POINTER; individual tests will change the path type.
        config = new ConfigStub(PathType.JSON_POINTER);
        validationContext = new ValidationContextStub(config);
        validator = new ConcreteValidator(validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the interactions to avoid cross‑test contamination
        reset(validationContext, config);
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPointer when PathType is JSON_POINTER")
    void atRootReturnsJsonPointer() {
        // arrange
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        // act
        JsonNodePath result = validator.invokeAtRoot();
        // assert
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPointer.getInstance(), result, "Expected the singleton JsonNodePathJsonPointer instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathLegacy when PathType is LEGACY")
    void atRootReturnsLegacy() {
        // arrange
        when(config.getPathType()).thenReturn(PathType.LEGACY);
        // act
        JsonNodePath result = validator.invokeAtRoot();
        // assert
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathLegacy.getInstance(), result, "Expected the singleton JsonNodePathLegacy instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPath when PathType is JSON_PATH")
    void atRootReturnsJsonPath() {
        // arrange
        when(config.getPathType()).thenReturn(PathType.JSON_PATH);
        // act
        JsonNodePath result = validator.invokeAtRoot();
        // assert
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPath.getInstance(), result, "Expected the singleton JsonNodePathJsonPath instance");
    }

    @Test
    @DisplayName("atRoot creates a new JsonNodePath for a custom PathType")
    void atRootCreatesNewInstanceForCustomPathType() {
        // arrange – assume the library defines a CUSTOM constant
        when(config.getPathType()).thenReturn(PathType.CUSTOM);
        // act – first call
        JsonNodePath first = validator.invokeAtRoot();
        // act – second call
        JsonNodePath second = validator.invokeAtRoot();
        // assert – two distinct objects (no singleton reuse)
        assertNotNull(first, "First result must not be null");
        assertNotNull(second, "Second result must not be null");
        assertNotSame(first, second, "Two successive calls with a custom PathType should return different instances");
        // assert – the PathType stored inside the returned JsonNodePath matches the config
        assertEquals(PathType.CUSTOM, first.getPathType(), "The JsonNodePath should carry the custom PathType supplied by the config");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when validator's validationContext is null")
    void atRootThrowsWhenValidationContextIsNull() {
        ConcreteValidator nullContextValidator = new ConcreteValidator(null);
        assertThrows(NullPointerException.class, nullContextValidator::invokeAtRoot, "Calling atRoot() with a null ValidationContext should throw NPE");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when ValidationContext#getConfig() returns null")
    void atRootThrowsWhenConfigIsNull() {
        ValidationContextStub ctx = new ValidationContextStub(null);
        ConcreteValidator v = new ConcreteValidator(ctx);
        assertThrows(NullPointerException.class, v::invokeAtRoot, "Calling atRoot() when config is null should throw NPE");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when Config#getPathType() returns null")
    void atRootThrowsWhenPathTypeIsNull() {
        // Create a config stub that returns null for getPathType()
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig() {

            @Override
            public PathType getPathType() {
                return null;
            }

            // other methods can return defaults
            @Override
            public boolean isFailFast() {
                return false;
            }

            @Override
            public boolean isOpenAPI30() {
                return false;
            }

            @Override
            public boolean isOpenAPI31() {
                return false;
            }

            @Override
            public boolean isEcma262Validator() {
                return false;
            }

            @Override
            public boolean isTypeLoose() {
                return false;
            }

            @Override
            public boolean isHandleNullableField() {
                return false;
            }

            @Override
            public boolean isFailFastOnFirstError() {
                return false;
            }

            @Override
            public boolean isNullableSupport() {
                return false;
            }

            @Override
            public boolean isCustomMessageSupported() {
                return false;
            }

            @Override
            public boolean isValidateSchema() {
                return false;
            }

            @Override
            public boolean isValidateSchemaOnly() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJson() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJsonWithMessage() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJsonWithMessageAndCode() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocation() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchema() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPath() {
                return false;
            }

            @Override
            public boolean isValidateSchemaAsJsonWithMessageAndCodeAndLocationAndSchemaAndPathAndValue() {
                return false;
            }

            @Override
            public java.util.Map<String, Object> getAdditionalProperties() {
                return Collections.emptyMap();
            }
        };
        ValidationContextStub ctx = new ValidationContextStub(cfg);
        ConcreteValidator v = new ConcreteValidator(ctx);
        assertThrows(NullPointerException.class, v::invokeAtRoot, "Calling atRoot() when PathType is null should throw NPE");
    }

    @Test
    @DisplayName("atRoot returns a JsonNodePath whose PathType matches the configuration")
    void atRootRespectsConfiguredPathType() {
        for (PathType pt : new PathType[] { PathType.JSON_POINTER, PathType.LEGACY, PathType.JSON_PATH }) {
            config = new ConfigStub(pt);
            validationContext = new ValidationContextStub(config);
            validator = new ConcreteValidator(validationContext);
            JsonNodePath result = validator.invokeAtRoot();
            assertNotNull(result, "Result must not be null");
            assertEquals(pt, result.getPathType(), "JsonNodePath#getPathType() should return the configured PathType");
        }
    }
}
