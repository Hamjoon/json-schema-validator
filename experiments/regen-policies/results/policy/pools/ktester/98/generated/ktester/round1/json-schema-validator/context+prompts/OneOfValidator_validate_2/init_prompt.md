You are tasked with creating a framework for a test class in Java. Now analyze the following information carefully:

1. The method that needs to be tested and its parameters.
2. The constructor of the target class and its parameters.
3. Any dependencies or external resources used by the target class.

Based on this analysis, complete the test class template in the provided structure:

1. Initialize necessary fields and dependencies within setupBeforeAll() and setupBeforeEach().
2. Handle any required cleanup in teardownAfterEach() and teardownAfterAll().
3. Make sure not to invoke any private methods or fields of the target class and dependent class.
4. Refer to the "given" phase in the existing test class for guidance.
5. Add necessary import statements and ensure there are no syntax errors in the generated code.

@input{focal method and target class}
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`, with the focal class `OneOfValidator_validate_2_Test`:
```Java
package com.networknt.schema;
import java.util.Set;
import com.networknt.schema.utils.SetView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Collections;
public class OneOfValidator extends BaseJsonValidator {
    private static final Logger logger = LoggerFactory.getLogger(OneOfValidator.class);
    private final List<JsonSchema> schemas;
    protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean walk) {
        Set<ValidationMessage> errors = null;
        debug(logger, executionContext, node, rootNode, instanceLocation);
        int numberOfValidSchema = 0;
        int index = 0;
        SetView<ValidationMessage> childErrors = null;
        List<String> indexes = null;
        // Save flag as nested schema evaluation shouldn't trigger fail fast
        boolean failFast = executionContext.isFailFast();
        try {
            DiscriminatorValidator discriminator = null;
            if (this.validationContext.getConfig().isDiscriminatorKeywordEnabled()) {
                DiscriminatorContext discriminatorContext = new DiscriminatorContext();
                executionContext.enterDiscriminatorContext(discriminatorContext, instanceLocation);
                // check if discriminator present
                discriminator = ((DiscriminatorValidator) (this.getParentSchema().getValidators().stream().filter(v -> "discriminator".equals(v.getKeyword())).findFirst().orElse(null)));
                if (discriminator != null) {
                    // this is just to make the discriminator context active
                    discriminatorContext.registerDiscriminator(discriminator.getSchemaLocation(), ((ObjectNode) (discriminator.getSchemaNode())));
                }
            }
            executionContext.setFailFast(false);
            for (JsonSchema schema : this.schemas) {
                Set<ValidationMessage> schemaErrors = Collections.emptySet();
                if (!walk) {
                    schemaErrors = schema.validate(executionContext, node, rootNode, instanceLocation);
                } else {
                    schemaErrors = schema.walk(executionContext, node, rootNode, instanceLocation, true);
                }
                // check if any validation errors have occurred
                if (schemaErrors.isEmpty()) {
                    numberOfValidSchema++;
                    if (indexes == null) {
                        indexes = new ArrayList<>();
                    }
                    indexes.add(Integer.toString(index));
                }
                if ((numberOfValidSchema > 1) && canShortCircuit()) {
                    // short-circuit
                    // note that the short circuit means that only 2 valid schemas are reported even if could be more
                    break;
                }
                if (this.validationContext.getConfig().isDiscriminatorKeywordEnabled()) {
                    // The discriminator will cause all messages other than the one with the
                    // matching discriminator to be discarded. Note that the discriminator cannot
                    // affect the actual validation result.
                    if ((discriminator != null) && (!discriminator.getPropertyName().isEmpty())) {
                        JsonNode discriminatorPropertyNode = node.get(discriminator.getPropertyName());
                        if (discriminatorPropertyNode != null) {
                            String discriminatorPropertyValue = discriminatorPropertyNode.asText();
                            discriminatorPropertyValue = discriminator.getMapping().getOrDefault(discriminatorPropertyValue, discriminatorPropertyValue);
                            JsonNode refNode = schema.getSchemaNode().get("$ref");
                            if (refNode != null) {
                                String ref = refNode.asText();
                                if (ref.equals(discriminatorPropertyValue) || ref.endsWith("/" + discriminatorPropertyValue)) {
                                    executionContext.getCurrentDiscriminatorContext().markMatch();
                                }
                            }
                        } else {
                            // See issue 436 where the condition was relaxed to not cause an assertion
                            // due to missing discriminator property value
                            // Also see BaseJsonValidator#checkDiscriminatorMatch
                            executionContext.getCurrentDiscriminatorContext().markIgnore();
                        }
                    }
                    DiscriminatorContext currentDiscriminatorContext = executionContext.getCurrentDiscriminatorContext();
                    if (currentDiscriminatorContext.isDiscriminatorMatchFound() && (childErrors == null)) {
                        // Note that the match is set if found and not reset so checking if childErrors
                        // found is null triggers on the correct schema
                        childErrors = new SetView<>();
                        childErrors.union(schemaErrors);
                    } else if (currentDiscriminatorContext.isDiscriminatorIgnore() || (!currentDiscriminatorContext.isActive())) {
                        // This is the normal handling when discriminators aren't enabled
                        if (childErrors == null) {
                            childErrors = new SetView<>();
                        }
                        childErrors.union(schemaErrors);
                    }
                } else if ((!schemaErrors.isEmpty()) && reportChildErrors(executionContext)) {
                    // This is the normal handling when discriminators aren't enabled
                    if (childErrors == null) {
                        childErrors = new SetView<>();
                    }
                    childErrors.union(schemaErrors);
                }
                index++;
            }
            if (((this.validationContext.getConfig().isDiscriminatorKeywordEnabled() && ((discriminator != null) || executionContext.getCurrentDiscriminatorContext().isActive())) && (!executionContext.getCurrentDiscriminatorContext().isDiscriminatorMatchFound())) && (!executionContext.getCurrentDiscriminatorContext().isDiscriminatorIgnore())) {
                errors = Collections.singleton(message().instanceNode(node).instanceLocation(instanceLocation).locale(executionContext.getExecutionConfig().getLocale()).arguments("based on the provided discriminator. No alternative could be chosen based on the discriminator property").build());
            }
        } finally {
            // Restore flag
            executionContext.setFailFast(failFast);
            if (this.validationContext.getConfig().isDiscriminatorKeywordEnabled()) {
                executionContext.leaveDiscriminatorContextImmediately(instanceLocation);
            }
        }
        // ensure there is always an "OneOf" error reported if number of valid schemas
        // is not equal to 1.
        // errors will only not be null in the discriminator case where no match is found
        if ((numberOfValidSchema != 1) && (errors == null)) {
            ValidationMessage message = message().instanceNode(node).instanceLocation(instanceLocation).messageKey(numberOfValidSchema > 1 ? "oneOf.indexes" : "oneOf").locale(executionContext.getExecutionConfig().getLocale()).failFast(executionContext.isFailFast()).arguments(Integer.toString(numberOfValidSchema), numberOfValidSchema > 1 ? String.join(", ", indexes) : "").build();
            if (childErrors != null) {
                errors = new SetView<ValidationMessage>().union(Collections.singleton(message)).union(childErrors);
            } else {
                errors = Collections.singleton(message);
            }
        }
        return errors != null ? errors : Collections.emptySet();
    }
protected boolean canShortCircuit();
    @Override
public void preloadJsonSchema();
    protected boolean reportChildErrors(ExecutionContext executionContext);
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema);
}
```


@input{context information}

api document of class com.networknt.schema.OneOfValidator: 

 * {@link JsonValidator} for oneOf.
 

constructors for class `com.networknt.schema.OneOfValidator`: 
params: com.networknt.schema.SchemaLocation schemaLocation 
com.networknt.schema.JsonNodePath evaluationPath 
com.networknt.schema.JsonSchema parentSchema 
com.networknt.schema.ValidationContext validationContext 
body:
```java
    public OneOfValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaLocation, evaluationPath, schemaNode, parentSchema, ValidatorTypeCode.ONE_OF, validationContext);
        if (!schemaNode.isArray()) {
            JsonType nodeType = TypeFactory.getValueNodeType(schemaNode, this.validationContext.getConfig());
            throw new JsonSchemaException(message().instanceNode(schemaNode)
                    .instanceLocation(schemaLocation.getFragment())
                    .messageKey("type")
                    .arguments(nodeType.toString(), "array")
                    .build());
        }
        int size = schemaNode.size();
        this.schemas = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            JsonNode childNode = schemaNode.get(i);
            this.schemas.add(validationContext.newSchema( schemaLocation.append(i), evaluationPath.append(i), childNode, parentSchema));
        }
    }

```

parameters in constructors and focal method: 
class `com.networknt.schema.SchemaLocation`:
api document : 
 * The schema location is the canonical IRI of the schema object plus a JSON
 * Pointer fragment indicating the subschema that produced a result. In contrast
 * with the evaluation path, the schema location MUST NOT include by-reference
 * applicators such as $ref or $dynamicRef.
 
constructor:
```java
    public SchemaLocation(AbsoluteIri absoluteIri, JsonNodePath fragment) {
        this.absoluteIri = absoluteIri;
        this.fragment = fragment;
    }

    public SchemaLocation(AbsoluteIri absoluteIri) {
        this(absoluteIri, JSON_POINTER);
    }

```
class `com.networknt.schema.JsonNodePath`:
api document : 
 * Represents a path to a JSON node.
 
constructor:
```java
    public JsonNodePath(PathType type) {
        this.type = type;
        this.parent = null;
        this.pathSegment = null;
        this.pathSegmentIndex = -1;
    }

    private JsonNodePath(JsonNodePath parent, String pathSegment) {
        this.parent = parent;
        this.type = parent.type;
        this.pathSegment = pathSegment;
        this.pathSegmentIndex = -1;
    }

    private JsonNodePath(JsonNodePath parent, int pathSegmentIndex) {
        this.parent = parent;
        this.type = parent.type;
        this.pathSegment = null;
        this.pathSegmentIndex = pathSegmentIndex;
    }

```
class `com.networknt.schema.JsonSchema`:
api document : 
 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 
constructor:
```java
    private JsonSchema(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath,
            JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        super(resolve(schemaLocation, schemaNode, parent == null, validationContext), evaluationPath, schemaNode, parent,
                null, null, validationContext, suppressSubSchemaRetrieval);
        String id = this.validationContext.resolveSchemaId(this.schemaNode);
        if (id != null) {
            // In earlier drafts $id may contain an anchor fragment see draft4/idRef.json
            // Note that json pointer fragments in $id are not allowed
            SchemaLocation result = id.indexOf('#') != -1 ? schemaLocation.resolve(id) : this.schemaLocation;
            if (hasNoFragment(result)) {
                this.id = id;
            } else {
                // This is an anchor fragment and is not a document
                // This will be added to schema resources later
                this.id = null;
            }
            this.validationContext.getSchemaResources().putIfAbsent(result != null ? result.toString() : id, this);
        } else {
            if (hasNoFragment(schemaLocation)) {
                // No $id but there is no fragment and is thus a schema resource
                this.id = schemaLocation.getAbsoluteIri() != null ? schemaLocation.getAbsoluteIri().toString() : "";
                this.validationContext.getSchemaResources()
                        .putIfAbsent(schemaLocation != null ? schemaLocation.toString() : this.id, this);
            } else {
                this.id = null;
            }
        }
        String anchor = this.validationContext.getMetaSchema().readAnchor(this.schemaNode);
        if (anchor != null) {
            String absoluteIri = this.schemaLocation.getAbsoluteIri() != null
                    ? this.schemaLocation.getAbsoluteIri().toString()
                    : "";
            this.validationContext.getSchemaResources()
                    .putIfAbsent(absoluteIri + "#" + anchor, this);
        }
        String dynamicAnchor = this.validationContext.getMetaSchema().readDynamicAnchor(schemaNode);
        if (dynamicAnchor != null) {
            String absoluteIri = this.schemaLocation.getAbsoluteIri() != null
                    ? this.schemaLocation.getAbsoluteIri().toString()
                    : "";
            this.validationContext.getDynamicAnchors()
                    .putIfAbsent(absoluteIri + "#" + dynamicAnchor, this);
        }
        getValidators();
    }

    protected JsonSchema(
            /* Below from JsonSchema */
            List<JsonValidator> validators,
            boolean validatorsLoaded,
            boolean recursiveAnchor,
            TypeValidator typeValidator,
            String id,            
            /* Below from BaseJsonValidator */
            boolean suppressSubSchemaRetrieval,
            JsonNode schemaNode,
            ValidationContext validationContext,
            /* Below from ValidationMessageHandler */
            ErrorMessageType errorMessageType,
            String errorMessageKeyword,
            MessageSource messageSource,
            Keyword keyword,
            JsonSchema parentSchema,
            SchemaLocation schemaLocation,
            JsonNodePath evaluationPath,
            JsonSchema evaluationParentSchema,
            Map<String, String> errorMessage) {
        super(suppressSubSchemaRetrieval, schemaNode, validationContext, errorMessageType, errorMessageKeyword, messageSource, keyword,
                parentSchema, schemaLocation, evaluationPath, evaluationParentSchema, errorMessage);
        this.validators = validators;
        this.validatorsLoaded = validatorsLoaded;
        this.recursiveAnchor = recursiveAnchor;
        this.typeValidator = typeValidator;
        this.id = id;
    }

```
class `com.networknt.schema.ValidationContext`:
constructor:
```java
    public ValidationContext(JsonMetaSchema metaSchema,
                             JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config) {
        this(metaSchema, jsonSchemaFactory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }
    
    public ValidationContext(JsonMetaSchema metaSchema, JsonSchemaFactory jsonSchemaFactory,
            SchemaValidatorsConfig config, ConcurrentMap<String, JsonSchema> schemaReferences,
            ConcurrentMap<String, JsonSchema> schemaResources, ConcurrentMap<String, JsonSchema> dynamicAnchors) {
        if (metaSchema == null) {
            throw new IllegalArgumentException("JsonMetaSchema must not be null");
        }
        if (jsonSchemaFactory == null) {
            throw new IllegalArgumentException("JsonSchemaFactory must not be null");
        }
        this.metaSchema = metaSchema;
        this.jsonSchemaFactory = jsonSchemaFactory;
        // The deprecated SchemaValidatorsConfig constructor needs to remain until removed
        this.config = config == null ? new SchemaValidatorsConfig() : config;
        this.schemaReferences = schemaReferences;
        this.schemaResources = schemaResources;
        this.dynamicAnchors = dynamicAnchors;
    }

```
class `com.networknt.schema.ExecutionContext`:
api document : 
 * Stores the execution context for the validation run.
 
constructor:
```java
    public ExecutionContext() {
        this(new ExecutionConfig(), null);
    }

    public ExecutionContext(CollectorContext collectorContext) {
        this(new ExecutionConfig(), collectorContext);
    }

    public ExecutionContext(ExecutionConfig executionConfig) {
        this(executionConfig, null);
    }

    public ExecutionContext(ExecutionConfig executionConfig, CollectorContext collectorContext) {
        this.collectorContext = collectorContext;
        this.executionConfig = executionConfig;
    }

```

existing test class: 
```java
/*
 * Copyright (c) 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * OneOfValidatorTest.
 */
public class OneOfValidatorTest {
    @Test
    void oneOfMultiple() {
        String schemaData = "{\r\n"
                + "  \"oneOf\": [\r\n"
                + "    { \r\n"
                + "      \"type\" : \"object\" ,\r\n"
                + "      \"properties\" : {\r\n"
                + "        \"hello\" : { \"type\" : \"string\" }\r\n"
                + "      },\r\n"
                + "      \"additionalProperties\" : false\r\n"
                + "    },\r\n"
                + "    { \r\n"
                + "      \"type\" : \"object\" ,\r\n"
                + "      \"properties\" : {\r\n"
                + "        \"world\" : { \"type\" : \"string\" }\r\n"
                + "      },\r\n"
                + "      \"additionalProperties\" : { \"type\" : \"string\" }\r\n"
                + "    },\r\n"
                + "    { \r\n"
                + "      \"type\" : \"object\" ,\r\n"
                + "      \"properties\" : {\r\n"
                + "        \"fox\" : { \"type\" : \"string\" }\r\n"
                + "      },\r\n"
                + "      \"additionalProperties\" : { \"type\" : \"string\" }\r\n"
                + "    }\r\n"
                + "  ]\r\n"
                + "}";
        String inputData = "{\r\n"
                + "  \"fox\" : \"test\",\r\n"
                + "  \"world\" : \"test\"\r\n"
                + "}";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaData);
        Set<ValidationMessage> messages = schema.validate(inputData, InputFormat.JSON);
        assertEquals(3, messages.size()); // even if more than 1 matches the mismatch errors are still reported
        List<ValidationMessage> assertions = messages.stream().collect(Collectors.toList());
        assertEquals("oneOf", assertions.get(0).getType());
        assertEquals("$", assertions.get(0).getInstanceLocation().toString());
        assertEquals("$.oneOf", assertions.get(0).getEvaluationPath().toString());
        assertEquals("$: must be valid to one and only one schema, but 2 are valid with indexes '1, 2'",
                assertions.get(0).getMessage());
    }

    @Test
    void oneOfZero() {
        String schemaData = "{\r\n"
                + "  \"oneOf\": [\r\n"
                + "    { \r\n"
                + "      \"type\" : \"object\" ,\r\n"
                + "      \"properties\" : {\r\n"
                + "        \"hello\" : { \"type\" : \"string\" }\r\n"
                + "      },\r\n"
                + "      \"additionalProperties\" : false\r\n"
                + "    },\r\n"
                + "    { \r\n"
                + "      \"type\" : \"object\" ,\r\n"
                + "      \"properties\" : {\r\n"
                + "        \"world\" : { \"type\" : \"string\" }\r\n"
                + "      },\r\n"
                + "      \"additionalProperties\" : { \"type\" : \"string\" }\r\n"
                + "    },\r\n"
                + "    { \r\n"
                + "      \"type\" : \"object\" ,\r\n"
                + "      \"properties\" : {\r\n"
                + "        \"fox\" : { \"type\" : \"string\" }\r\n"
......
                + "            }\r\n"
                + "          }\r\n"
                + "        }\r\n"
                + "      ]\r\n"
                + "    },\r\n"
                + "    \"number\": {\r\n"
                + "      \"allOf\": [\r\n"
                + "        {\r\n"
                + "          \"$ref\": \"#/$defs/typed\"\r\n"
                + "        },\r\n"
                + "        {\r\n"
                + "          \"properties\": {\r\n"
                + "            \"type\": {\r\n"
                + "              \"type\": \"string\"\r\n"
                + "            },\r\n"
                + "            \"value\": {\r\n"
                + "              \"type\": \"number\"\r\n"
                + "            }\r\n"
                + "          }\r\n"
                + "        }\r\n"
                + "      ]\r\n"
                + "    }\r\n"
                + "  }\r\n"
                + "}";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaData,
                SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build());
        // Valid
        String inputData = "{\r\n"
                + "  \"type\": \"number\",\r\n"
                + "  \"value\": 1\r\n"
                + "}";
        Set<ValidationMessage> messages = schema.validate(inputData, InputFormat.JSON);
        assertEquals(0, messages.size());

        // Invalid only 1 message returned for number
        String inputData2 = "{\r\n"
                + "  \"type\": \"number\",\r\n"
                + "  \"value\": {}\r\n"
                + "}";
        Set<ValidationMessage> messages2 = schema.validate(inputData2, InputFormat.JSON);
        assertEquals(2, messages2.size());

        // Invalid both messages for string and object returned
        JsonSchema schema2 = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaData,
                SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build());
        Set<ValidationMessage> messages3 = schema2.validate(inputData2, InputFormat.JSON);
        assertEquals(3, messages3.size());
    }

}
```

invoke examples of focal method: 
example 1:
```java
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
            JsonNodePath instanceLocation) {
        return validate(executionContext, node, rootNode, instanceLocation, false);
    }
```
example 2:
```java
    @Override
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema) {
        HashSet<ValidationMessage> validationMessages = new LinkedHashSet<>();
        if (shouldValidateSchema) {
            validationMessages.addAll(validate(executionContext, node, rootNode, instanceLocation, true));
    }
```



@input{test class template}
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary

@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_2_Test {
    @BeforeAll
    static void setupBeforeAll() {
    }

    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
    }
}
```

@output{initial test class}: now consider the invoke examples output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.