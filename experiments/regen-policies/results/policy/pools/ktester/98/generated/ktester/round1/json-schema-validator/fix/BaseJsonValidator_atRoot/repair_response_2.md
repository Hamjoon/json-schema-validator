```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.PathType;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.VersionFlag;
import com.networknt.schema.BaseJsonValidator;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonNodePathJsonPointer;
import com.networknt.schema.JsonNodePathLegacy;
import com.networknt.schema.JsonNodePathJsonPath;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link BaseJsonValidator#atRoot()}.
 */
@ExtendWith(org.junit.jupiter.api.extension.ExtensionContext.class) // dummy extension to keep the annotation
class BaseJsonValidator_atRoot_Test {

    private ConcreteValidator validator;

    /**
     * Simple stub for {@link ValidationContext}.
     */
    private static class ValidationContextStub extends ValidationContext {
        private final SchemaValidatorsConfig cfg;

        ValidationContextStub(SchemaValidatorsConfig cfg) {
            super(JsonMetaSchema.getV7(),
                  JsonSchemaFactory.getInstance(VersionFlag.V7),
                  cfg);
            this.cfg = cfg;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return this.cfg;
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
            // Use the constructor that takes the essential arguments.
            super(null,               // schemaLocation
                  null,               // evaluationPath
                  null,               // schemaNode
                  null,               // parentSchema
                  null,               // validatorType (can be null for our test)
                  vc,                 // validationContext
                  false);             // suppressSubSchemaRetrieval
        }

        /**
         * Expose the protected {@code atRoot()} for testing.
         */
        JsonNodePath invokeAtRoot() {
            return atRoot();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath at) {
            // No validation needed for these tests.
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        ConfigStub cfg = new ConfigStub(PathType.JSON_POINTER);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    cfg);
        validator = new ConcreteValidator(vc);
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPointer when PathType is JSON_POINTER")
    void atRootReturnsJsonPointer() {
        ConfigStub cfg = new ConfigStub(PathType.JSON_POINTER);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath result = validator.invokeAtRoot();
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPointer.getInstance(), result,
                "Expected the singleton JsonNodePathJsonPointer instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathLegacy when PathType is LEGACY")
    void atRootReturnsLegacy() {
        ConfigStub cfg = new ConfigStub(PathType.LEGACY);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath result = validator.invokeAtRoot();
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathLegacy.getInstance(), result,
                "Expected the singleton JsonNodePathLegacy instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPath when PathType is JSON_PATH")
    void atRootReturnsJsonPath() {
        ConfigStub cfg = new ConfigStub(PathType.JSON_PATH);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath result = validator.invokeAtRoot();
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPath.getInstance(), result,
                "Expected the singleton JsonNodePathJsonPath instance");
    }

    @Test
    @DisplayName("atRoot creates a new JsonNodePath for a custom PathType")
    void atRootCreatesNewInstanceForCustomPathType() {
        // Assume the library defines a CUSTOM constant.
        PathType custom = PathType.CUSTOM;
        ConfigStub cfg = new ConfigStub(custom);
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    cfg);
        validator = new ConcreteValidator(vc);
        JsonNodePath first = validator.invokeAtRoot();
        JsonNodePath second = validator.invokeAtRoot();
        assertNotNull(first, "First result must not be null");
        assertNotNull(second, "Second result must not be null");
        assertNotSame(first, second,
                "Two successive calls with a custom PathType should return different instances");
        assertEquals(custom, first.getPathType(),
                "The JsonNodePath should carry the custom PathType supplied by the config");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when validator's validationContext is null")
    void atRootThrowsWhenValidationContextIsNull() {
        ConcreteValidator nullContextValidator = new ConcreteValidator(null);
        assertThrows(NullPointerException.class,
                nullContextValidator::invokeAtRoot,
                "Calling atRoot() with a null ValidationContext should throw NPE");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when ValidationContext#getConfig() returns null")
    void atRootThrowsWhenConfigIsNull() {
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    new ConfigStub(PathType.JSON_POINTER)) {
            @Override
            public SchemaValidatorsConfig getConfig() {
                return null;
            }
        };
        ConcreteValidator v = new ConcreteValidator(vc);
        assertThrows(NullPointerException.class,
                v::invokeAtRoot,
                "Calling atRoot() when config is null should throw NPE");
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
        ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                    JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                    cfg);
        ConcreteValidator v = new ConcreteValidator(vc);
        assertThrows(NullPointerException.class,
                v::invokeAtRoot,
                "Calling atRoot() when PathType is null should throw NPE");
    }

    @Test
    @DisplayName("atRoot returns a JsonNodePath whose PathType matches the configuration")
    void atRootRespectsConfiguredPathType() {
        for (PathType pt : new PathType[] { PathType.JSON_POINTER, PathType.LEGACY, PathType.JSON_PATH }) {
            ConfigStub cfg = new ConfigStub(pt);
            ValidationContext vc = new ValidationContext(JsonMetaSchema.getV7(),
                                                        JsonSchemaFactory.getInstance(VersionFlag.V7),
                                                        cfg);
            validator = new ConcreteValidator(vc);
            JsonNodePath result = validator.invokeAtRoot();
            assertNotNull(result, "Result must not be null");
            assertEquals(pt, result.getPathType(),
                    "JsonNodePath#getPathType() should return the configured PathType");
        }
    }
}
```