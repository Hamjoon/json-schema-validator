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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `PropertiesValidator_validate_Test`:
```Java
package com.networknt.schema;
import java.util.*;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.walk.DefaultPropertyWalkListenerRunner;
import com.networknt.schema.walk.WalkListenerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class PropertiesValidator extends BaseJsonValidator {
    public static final String PROPERTY = "properties";
    private static final Logger logger = LoggerFactory.getLogger(PropertiesValidator.class);
    private final Map<String, JsonSchema> schemas = new LinkedHashMap<>();
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);
        CollectorContext collectorContext = executionContext.getCollectorContext();
        WalkListenerRunner propertyWalkListenerRunner = new DefaultPropertyWalkListenerRunner(this.validationContext.getConfig().getPropertyWalkListeners());
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        // get the Validator state object storing validation data
        ValidatorState state = ((ValidatorState) (collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)));
        for (Map.Entry<String, JsonSchema> entry : this.schemas.entrySet()) {
            JsonSchema propertySchema = entry.getValue();
            JsonNode propertyNode = node.get(entry.getKey());
            if (propertyNode != null) {
                collectorContext.getEvaluatedProperties().add(atPath(at, entry.getKey()));// TODO: This should happen after validation
    
                // check whether this is a complex validator. save the state
                boolean isComplex = state.isComplexValidator();
                // if this is a complex validator, the node has matched, and all it's child elements, if available, are to be validated
                if (state.isComplexValidator()) {
                    state.setMatchedNode(true);
                }
                // reset the complex validator for child element validation, and reset it after the return from the recursive call
                state.setComplexValidator(false);
                if (!state.isWalkEnabled()) {
                    // validate the child element(s)
                    errors.addAll(propertySchema.validate(executionContext, propertyNode, rootNode, atPath(at, entry.getKey())));
                } else {
                    // check if walker is enabled. If it is enabled it is upto the walker implementation to decide about the validation.
                    walkSchema(executionContext, entry, node, rootNode, at, state.isValidationEnabled(), errors, propertyWalkListenerRunner);
                }
                // reset the complex flag to the original value before the recursive call
                state.setComplexValidator(isComplex);
                // if this was a complex validator, the node has matched and has been validated
                if (state.isComplexValidator()) {
                    state.setMatchedNode(true);
                }
            } else // check whether the node which has not matched was mandatory or not
            if (getParentSchema().hasRequiredValidator()) {
                Set<ValidationMessage> requiredErrors = getParentSchema().getRequiredValidator().validate(executionContext, node, rootNode, at);
                if (!requiredErrors.isEmpty()) {
                    // the node was mandatory, decide which behavior to employ when validator has not matched
                    if (state.isComplexValidator()) {
                        // this was a complex validator (ex oneOf) and the node has not been matched
                        state.setMatchedNode(false);
                        return Collections.emptySet();
                    }
                    errors.addAll(requiredErrors);
                }
            }
        }
        return Collections.unmodifiableSet(errors);
    }
public Map<String, JsonSchema> getSchemas();
    @Override
public void preloadJsonSchema();
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
    private void walkSchema(ExecutionContext executionContext, Map.Entry<String, JsonSchema> entry, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema, Set<ValidationMessage> validationMessages, WalkListenerRunner propertyWalkListenerRunner);
}
```


@input{context information}

constructors for class `com.networknt.schema.PropertiesValidator`: 
params: com.networknt.schema.JsonSchema parentSchema 
com.networknt.schema.ValidationContext validationContext 
body:
```java
    public PropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.PROPERTIES, validationContext);
        this.validationContext = validationContext;
        for (Iterator<String> it = schemaNode.fieldNames(); it.hasNext(); ) {
            String pname = it.next();
            this.schemas.put(pname, validationContext.newSchema(schemaPath + "/" + pname, schemaNode.get(pname), parentSchema));
        }
    }

```

parameters in constructors and focal method: 
class `com.networknt.schema.JsonSchema`:
api document : 
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 
constructor:
```java
    private JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode,
                       JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parent, null, validationContext, suppressSubSchemaRetrieval);
        this.validationContext = validationContext;
        this.metaSchema = validationContext.getMetaSchema();
        this.currentUri = combineCurrentUriWithIds(currentUri, schemaNode);
        if (uriRefersToSubschema(currentUri, schemaPath)) {
            updateThisAsSubschema(currentUri);
        }
        if (validationContext.getConfig() != null) {
            this.keywordWalkListenerRunner = new DefaultKeywordWalkListenerRunner(this.validationContext.getConfig().getKeywordWalkListenersMap());
            if (validationContext.getConfig().isOpenAPI3StyleDiscriminators()) {
                ObjectNode discriminator = (ObjectNode) schemaNode.get("discriminator");
                if (null != discriminator && null != validationContext.getCurrentDiscriminatorContext()) {
                    validationContext.getCurrentDiscriminatorContext().registerDiscriminator(schemaPath, discriminator);
                }
            }
        }
    }

```
class `com.networknt.schema.ValidationContext`:
constructor:
```java
    public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema,
                             JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config) {
        if (uriFactory == null) {
            throw new IllegalArgumentException("URIFactory must not be null");
        }
        if (metaSchema == null) {
            throw new IllegalArgumentException("JsonMetaSchema must not be null");
        }
        if (jsonSchemaFactory == null) {
            throw new IllegalArgumentException("JsonSchemaFactory must not be null");
        }
        this.uriFactory = uriFactory;
        this.urnFactory = urnFactory;
        this.metaSchema = metaSchema;
        this.jsonSchemaFactory = jsonSchemaFactory;
        this.config = config;
    }

```
class `com.networknt.schema.ExecutionContext`:
api document : 
 * Stores the execution context for the validation run.
 
constructor:
```java
    public ExecutionContext() {
        this(new CollectorContext());
    }

    public ExecutionContext(CollectorContext collectorContext) {
        this.collectorContext = collectorContext;
    }

```

existing test class: 
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by josejulio on 25/04/22.
 */
public class PropertiesValidatorTest extends BaseJsonSchemaValidatorTest {

    @Test
    public void testDoesNotThrowWhenApplyingDefaultPropertiesToNonObjects() throws Exception {
        Assertions.assertDoesNotThrow(() -> {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

            SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
            schemaValidatorsConfig.setApplyDefaultsStrategy(new ApplyDefaultsStrategy(
                    true,
                    true,
                    true
            ));

            JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"foo\":{\"type\":\"object\", \"properties\": {} },\"i-have-default\":{\"type\":\"string\",\"default\":\"foo\"}}}", schemaValidatorsConfig);
            JsonNode node = getJsonNodeFromStringContent("{\"foo\": \"bar\"}");
            ValidationResult result = schema.walk(node, true);
            Assertions.assertEquals(result.getValidationMessages().size(), 1);
        });
    }
}

```

invoke examples of focal method: 
example 1:
```java
    @Override
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) {
        HashSet<ValidationMessage> validationMessages = new LinkedHashSet<>();
        if (this.applyDefaultsStrategy.shouldApplyPropertyDefaults() && null != node && node.getNodeType() == JsonNodeType.OBJECT) {
            applyPropertyDefaults((ObjectNode) node);
        }
        if (shouldValidateSchema) {
            validationMessages.addAll(validate(executionContext, node, rootNode, at));
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
class PropertiesValidator_validate_Test {
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