
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:5: error: package com.networknt.schema.error does not exist
import com.networknt.schema.error.ErrorMessageType;
                                 ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:6: error: package com.networknt.schema.json does not exist
import com.networknt.schema.json.NodeType;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:7: error: package com.networknt.schema.json does not exist
import com.networknt.schema.json.SchemaMetaData;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:8: error: package com.networknt.schema.json.SchemaMetaData does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilder;
                                               ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:9: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilder does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilder.SchemaMetaDataBuilderStep;
                                                                     ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:10: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:11: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:12: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:13: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:14: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:15: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:16: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:17: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:18: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:19: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:20: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:21: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:22: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:23: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:24: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:25: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:26: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:27: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:28: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:29: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:30: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:31: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:32: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:33: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:34: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:35: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:36: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:37: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:38: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:39: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:40: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:41: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:42: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:43: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:44: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:45: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:46: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:47: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:48: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:49: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:50: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:51: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:52: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:53: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:54: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:55: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:56: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:57: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:58: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:59: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:60: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:61: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:62: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:63: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:64: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:65: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:66: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:67: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:68: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:69: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:70: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:71: error: package com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep does not exist
import com.networknt.schema.json.SchemaMetaData.SchemaMetaDataBuilderStep.SchemaMetaDataBuilderStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStepStep;
                                                                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:72: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:75: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:80: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:86: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_atRoot_Test
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:92: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_atRoot_Test
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:104: error: no suitable constructor found for ValidationContext(no arguments)
        ValidationContextStub(SchemaValidatorsConfig config) {
                                                             ^
    constructor ValidationContext.ValidationContext(JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig) is not applicable
      (actual and formal argument lists differ in length)
    constructor ValidationContext.ValidationContext(JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig,ConcurrentMap<String,JsonSchema>,ConcurrentMap<String,JsonSchema>,ConcurrentMap<String,JsonSchema>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:134: error: ConcreteValidator is not abstract and does not override abstract method validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath) in JsonValidator
    private static class ConcreteValidator extends BaseJsonValidator {
                   ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:159: error: validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath) in ConcreteValidator cannot implement validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath) in JsonValidator
        public void validate(ExecutionContext executionContext, com.fasterxml.jackson.databind.JsonNode node, com.fasterxml.jackson.databind.JsonNode rootNode, JsonNodePath at) {
                    ^
  return type void is not compatible with Set<ValidationMessage>
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:138: error: no suitable constructor found for BaseJsonValidator(<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,ValidationContext,boolean)
            super(// schemaLocation
            ^
    constructor BaseJsonValidator.BaseJsonValidator(SchemaLocation,JsonNodePath,JsonNode,JsonSchema,ValidatorTypeCode,ValidationContext) is not applicable
      (actual and formal argument lists differ in length)
    constructor BaseJsonValidator.BaseJsonValidator(SchemaLocation,JsonNodePath,JsonNode,JsonSchema,ErrorMessageType,Keyword,ValidationContext,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor BaseJsonValidator.BaseJsonValidator(boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:158: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:168: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:176: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:180: error: cannot find symbol
        assertSame(JsonNodePathJsonPointer.getInstance(), result, "Expected the singleton JsonNodePathJsonPointer instance");
                   ^
  symbol:   variable JsonNodePathJsonPointer
  location: class BaseJsonValidator_atRoot_Test
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:187: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:191: error: cannot find symbol
        assertSame(JsonNodePathLegacy.getInstance(), result, "Expected the singleton JsonNodePathLegacy instance");
                   ^
  symbol:   variable JsonNodePathLegacy
  location: class BaseJsonValidator_atRoot_Test
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:198: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:202: error: cannot find symbol
        assertSame(JsonNodePathJsonPath.getInstance(), result, "Expected the singleton JsonNodePathJsonPath instance");
                   ^
  symbol:   variable JsonNodePathJsonPath
  location: class BaseJsonValidator_atRoot_Test
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:210: error: cannot find symbol
        PathType custom = PathType.CUSTOM;
                                  ^
  symbol:   variable CUSTOM
  location: class PathType
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:212: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:232: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), new ConfigStub(PathType.JSON_POINTER)) {
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:253: error: no suitable method found for getInstance(no arguments)
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                              ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java:263: error: no suitable method found for getInstance(no arguments)
            ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(), cfg);
                                                                                                  ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
Note: src/test/java/com/networknt/schema/BaseJsonValidator_atRoot_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
89 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method atRoot()': '\n     * Get the root path.\n     *\n     * @return The path.\n     ', 'return type': 'com.networknt.schema.JsonNodePath', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.PathType`:\n\tdependent fields:\n\t\tcom.networknt.schema.PathType com.networknt.schema.PathType.JSON_POINTER;\n\t\tcom.networknt.schema.PathType com.networknt.schema.PathType.LEGACY;\n\t\tcom.networknt.schema.PathType com.networknt.schema.PathType.JSON_PATH;\n\trelated functions:\n\t\tmethod `public String convertToJsonPointer(String)` returns `java.lang.String`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`;\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`;\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `protected JsonSchema newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`, api document: \n     * Creates a json schema from initial input.\n     * \n     * @param schemaUri the schema location\n     * @param schemaNode the schema data node\n     * @param config the config to use\n     * @return the schema\n     ;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, String)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.BaseJsonValidator.atRoot()`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


