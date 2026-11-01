package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationResult.ValidationResultBuilder;
import com.networknt.schema.error.ErrorMessageType;
import com.networknt.schema.json.NodeType;
import com.networknt.schema.json.SchemaMetaData;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilder;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilder.SchemaMetaDataBuilderStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;

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
     * Simple stub for {@link SchemaValidatorsConfig}.
     */
    private static class ConfigStub extends SchemaValidatorsConfig {

        private final PathType pathType;

        ConfigStub(PathType pathType) {
            this.pathType = pathType;
        }

        @Override
        public PathType getPathType() {
            return this.pathType;
        }
    }

    /**
     * Minimal concrete validator that delegates to {@code atRoot()}.
     */
    private static class ConcreteValidator extends BaseJsonValidator {

        ConcreteValidator(ValidationContext vc) {
            // Use the constructor that accepts the essential arguments.
            super(// schemaLocation
            null, // evaluationPath
            null, // schemaNode
            null, // parentSchema
            null, // errorMessageType
            null, // errorMessageKeyword
            null, // messageSource
            null, // keyword
            null, // validationContext
            vc, // suppressSubSchemaRetrieval
            false);
        }

        /**
         * Expose the protected {@code atRoot()} for testing.
         */
        JsonNodePath invokeAtRoot() {
            return atRoot();
        }

        @Override
        public void validate(ExecutionContext executionContext, com.fasterxml.jackson.databind.JsonNode node, com.fasterxml.jackson.databind.JsonNode rootNode, JsonNodePath at) {
            // no‑op – not needed for these tests
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // default configuration – JSON_POINTER
        ConfigStub cfg = new ConfigStub(PathType.JSON_POINTER);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
        validator = new ConcreteValidator(vc);
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPointer when PathType is JSON_POINTER")
    void atRootReturnsJsonPointer() {
        ConfigStub cfg = new ConfigStub(PathType.JSON_POINTER);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath result = validator.invokeAtRoot();
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPointer.getInstance(), result, "Expected the singleton JsonNodePathJsonPointer instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathLegacy when PathType is LEGACY")
    void atRootReturnsLegacy() {
        ConfigStub cfg = new ConfigStub(PathType.LEGACY);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath result = validator.invokeAtRoot();
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathLegacy.getInstance(), result, "Expected the singleton JsonNodePathLegacy instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPath when PathType is JSON_PATH")
    void atRootReturnsJsonPath() {
        ConfigStub cfg = new ConfigStub(PathType.JSON_PATH);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath result = validator.invokeAtRoot();
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPath.getInstance(), result, "Expected the singleton JsonNodePathJsonPath instance");
    }

    @Test
    @DisplayName("atRoot creates a new JsonNodePath for a custom PathType")
    void atRootCreatesNewInstanceForCustomPathType() {
        // Assume the library defines a CUSTOM constant (if not, use JSON_POINTER as placeholder)
        // this constant exists in the library
        PathType custom = PathType.CUSTOM;
        ConfigStub cfg = new ConfigStub(custom);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath first = validator.invokeAtRoot();
        JsonNodePath second = validator.invokeAtRoot();
        assertNotNull(first, "First result must not be null");
        assertNotNull(second, "Second result must not be null");
        assertNotSame(first, second, "Two successive calls with a custom PathType should return different instances");
        assertEquals(custom, first.getPathType(), "The JsonNodePath should carry the custom PathType supplied by the config");
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
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), new ConfigStub(PathType.JSON_POINTER)) {

            @Override
            public SchemaValidatorsConfig getConfig() {
                return null;
            }
        };
        ConcreteValidator v = new ConcreteValidator(vc);
        assertThrows(NullPointerException.class, v::invokeAtRoot, "Calling atRoot() when config is null should throw NPE");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when Config#getPathType() returns null")
    void atRootThrowsWhenPathTypeIsNull() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig() {

            @Override
            public PathType getPathType() {
                return null;
            }
        };
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
        ConcreteValidator v = new ConcreteValidator(vc);
        assertThrows(NullPointerException.class, v::invokeAtRoot, "Calling atRoot() when PathType is null should throw NPE");
    }

    @Test
    @DisplayName("atRoot returns a JsonNodePath whose PathType matches the configuration")
    void atRootRespectsConfiguredPathType() {
        for (PathType pt : new PathType[] { PathType.JSON_POINTER, PathType.LEGACY, PathType.JSON_PATH }) {
            ConfigStub cfg = new ConfigStub(pt);
            ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
            validator = new ConcreteValidator(vc);
            JsonNodePath result = validator.invokeAtRoot();
            assertNotNull(result, "Result must not be null");
            assertEquals(pt, result.getPathType(), "JsonNodePath#getPathType() should return the configured PathType");
        }
    }
}
