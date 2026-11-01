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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)`, with the focal class `JsonSchema_validate_Test`:
```Java
package com.networknt.schema;
import com.networknt.schema.serialization.JsonMapperFactory;
import com.networknt.schema.serialization.YamlMapperFactory;
import java.util.*;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.CollectorContext.Scope;
import java.util.function.Consumer;
import java.net.URLDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
public class JsonSchema extends BaseJsonValidator {
    WalkListenerRunner keywordWalkListenerRunner = null;
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation) {
        if (validationContext.getConfig().isOpenAPI3StyleDiscriminators()) {
            ObjectNode discriminator = ((ObjectNode) (schemaNode.get("discriminator")));
            if ((null != discriminator) && (null != executionContext.getCurrentDiscriminatorContext())) {
                executionContext.getCurrentDiscriminatorContext().registerDiscriminator(schemaLocation, discriminator);
            }
        }
        SchemaValidatorsConfig config = this.validationContext.getConfig();
        Set<ValidationMessage> errors = null;
        // Get the collector context.
        CollectorContext collectorContext = executionContext.getCollectorContext();
        // Set the walkEnabled and isValidationEnabled flag in internal validator state.
        setValidatorState(executionContext, false, true);
        for (JsonValidator v : getValidators()) {
            Set<ValidationMessage> results = null;
            Scope parentScope = collectorContext.enterDynamicScope(this);
            try {
                results = v.validate(executionContext, jsonNode, rootNode, instanceLocation);
            } finally {
                Scope scope = collectorContext.exitDynamicScope();
                if ((results == null) || results.isEmpty()) {
                    parentScope.mergeWith(scope);
                } else {
                    if (errors == null) {
                        errors = new LinkedHashSet<>();
                    }
                    errors.addAll(results);
                    if ((((v instanceof PrefixItemsValidator) || (v instanceof ItemsValidator)) || (v instanceof ItemsValidator202012)) || (v instanceof ContainsValidator)) {
                        collectorContext.getEvaluatedItems().addAll(scope.getEvaluatedItems());
                    }
                    if (((v instanceof PropertiesValidator) || (v instanceof AdditionalPropertiesValidator)) || (v instanceof PatternPropertiesValidator)) {
                        collectorContext.getEvaluatedProperties().addAll(scope.getEvaluatedProperties());
                    }
                }
            }
        }
        if (config.isOpenAPI3StyleDiscriminators()) {
            ObjectNode discriminator = ((ObjectNode) (this.schemaNode.get("discriminator")));
            if (null != discriminator) {
                final DiscriminatorContext discriminatorContext = executionContext.getCurrentDiscriminatorContext();
                if (null != discriminatorContext) {
                    final ObjectNode discriminatorToUse;
                    final ObjectNode discriminatorFromContext = discriminatorContext.getDiscriminatorForPath(this.schemaLocation);
                    if (null == discriminatorFromContext) {
                        // register the current discriminator. This can only happen when the current context discriminator
                        // was not registered via allOf. In that case we have a $ref to the schema with discriminator that gets
                        // used for validation before allOf validation has kicked in
                        discriminatorContext.registerDiscriminator(this.schemaLocation, discriminator);
                        discriminatorToUse = discriminator;
                    } else {
                        discriminatorToUse = discriminatorFromContext;
                    }
                    final String discriminatorPropertyName = discriminatorToUse.get("propertyName").asText();
                    final JsonNode discriminatorNode = jsonNode.get(discriminatorPropertyName);
                    final String discriminatorPropertyValue = (discriminatorNode == null) ? null : discriminatorNode.asText();
                    checkDiscriminatorMatch(discriminatorContext, discriminatorToUse, discriminatorPropertyValue, this);
                }
            }
        }
        return errors == null ? Collections.emptySet() : errors;
    }
public ExecutionContext createExecutionContext();
    public JsonSchema findAncestor();
    public JsonSchema findLexicalRoot();
    public JsonSchema findSchemaResourceRoot();
    static JsonSchema from(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval);
    public JsonSchema fromRef(JsonSchema refEvaluationParentSchema, JsonNodePath refEvaluationPath);
    public String getId();
    protected JsonNode getNode(Object propertyOrIndex);
    public JsonSchema getRefSchema(JsonNodePath fragment);
    public JsonNode getRefSchemaNode(String ref);
    public JsonValidator getRequiredValidator();
    public JsonSchema getSubSchema(JsonNodePath fragment);
    public TypeValidator getTypeValidator();
    ValidationContext getValidationContext();
    public List<JsonValidator> getValidators();
    public boolean hasRequiredValidator();
    public boolean hasTypeValidator();
    public void initializeValidators();
    public boolean isRecursiveAnchor();
    public boolean isSchemaResourceRoot();
    private static void setValidatorState(ExecutionContext executionContext, boolean isWalkEnabled, boolean shouldValidateSchema);
    @Override
public String toString();
    public Set<ValidationMessage> validate(JsonNode rootNode);
    public Set<ValidationMessage> validate(JsonNode rootNode, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(JsonNode rootNode, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, Consumer<ExecutionContext> executionCustomizer);
    public ValidationResult validateAndCollect(JsonNode node);
    public ValidationResult validateAndCollect(ExecutionContext executionContext, JsonNode node);
    public ValidationResult walk(JsonNode node, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean validate);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema);
    public ValidationResult walk(ExecutionContext executionContext, String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walk(String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walkAtNode(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean validate);
    public JsonSchema withConfig(SchemaValidatorsConfig config);
}
```


@input{context information}

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

constructors for class `com.networknt.schema.JsonSchema`: 
params: com.networknt.schema.ValidationContext validationContext 
com.networknt.schema.SchemaLocation schemaLocation 
com.networknt.schema.JsonNodePath evaluationPath 
com.networknt.schema.JsonSchema parent 
body:
```java
    private JsonSchema(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath,
            JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        super(schemaLocation.resolve(validationContext.resolveSchemaId(schemaNode)), evaluationPath, schemaNode, parent,
                null, null, validationContext, suppressSubSchemaRetrieval);
        this.metaSchema = this.validationContext.getMetaSchema();
        initializeConfig();
        String id = this.validationContext.resolveSchemaId(this.schemaNode);
        if (id != null) {
            // In earlier drafts $id may contain an anchor fragment
            // Note that json pointer fragments in $id are not allowed
            if (hasNoFragment(schemaLocation)) {
                this.id = id;
            } else {
                this.id = id;
            }
            this.validationContext.getSchemaResources()
                    .putIfAbsent(this.schemaLocation != null ? this.schemaLocation.toString() : id, this);
        } else {
            if (hasNoFragment(schemaLocation)) {
                // No $id but there is no fragment and is thus a schema resource
                this.id = this.schemaLocation.getAbsoluteIri() != null ? this.schemaLocation.getAbsoluteIri().toString() : "";
                this.validationContext.getSchemaResources()
                        .putIfAbsent(this.schemaLocation != null ? this.schemaLocation.toString() : this.id, this);
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
    
```
params: com.networknt.schema.JsonSchema copy 
body:
```java
    protected JsonSchema(JsonSchema copy) {
        super(copy);
        this.validators = copy.validators;
        this.metaSchema = copy.metaSchema;
        this.validatorsLoaded = copy.validatorsLoaded;
        this.recursiveAnchor = copy.recursiveAnchor;
        this.requiredValidator = copy.requiredValidator;
        this.typeValidator = copy.typeValidator;
        this.keywordWalkListenerRunner = copy.keywordWalkListenerRunner;
        this.id = copy.id;
    }

```

parameters in constructors and focal method: 
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
        this.config = config == null ? new SchemaValidatorsConfig() : config;
        this.schemaReferences = schemaReferences;
        this.schemaResources = schemaResources;
        this.dynamicAnchors = dynamicAnchors;
    }

```
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
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 
constructor:
```java
    private JsonSchema(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath,
            JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        super(schemaLocation.resolve(validationContext.resolveSchemaId(schemaNode)), evaluationPath, schemaNode, parent,
                null, null, validationContext, suppressSubSchemaRetrieval);
        this.metaSchema = this.validationContext.getMetaSchema();
        initializeConfig();
        String id = this.validationContext.resolveSchemaId(this.schemaNode);
        if (id != null) {
            // In earlier drafts $id may contain an anchor fragment
            // Note that json pointer fragments in $id are not allowed
            if (hasNoFragment(schemaLocation)) {
                this.id = id;
            } else {
                this.id = id;
            }
            this.validationContext.getSchemaResources()
                    .putIfAbsent(this.schemaLocation != null ? this.schemaLocation.toString() : id, this);
        } else {
            if (hasNoFragment(schemaLocation)) {
                // No $id but there is no fragment and is thus a schema resource
                this.id = this.schemaLocation.getAbsoluteIri() != null ? this.schemaLocation.getAbsoluteIri().toString() : "";
                this.validationContext.getSchemaResources()
                        .putIfAbsent(this.schemaLocation != null ? this.schemaLocation.toString() : this.id, this);
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
    
    protected JsonSchema(JsonSchema copy) {
        super(copy);
        this.validators = copy.validators;
        this.metaSchema = copy.metaSchema;
        this.validatorsLoaded = copy.validatorsLoaded;
        this.recursiveAnchor = copy.recursiveAnchor;
        this.requiredValidator = copy.requiredValidator;
        this.typeValidator = copy.typeValidator;
        this.keywordWalkListenerRunner = copy.keywordWalkListenerRunner;
        this.id = copy.id;
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
        this(new ExecutionConfig(), collectorContext);
    }

    public ExecutionContext(ExecutionConfig executionConfig) {
        this(executionConfig, new CollectorContext());
    }

    public ExecutionContext(ExecutionConfig executionConfig, CollectorContext collectorContext) {
        this.collectorContext = collectorContext;
        this.executionConfig = executionConfig;
    }

```

invoke examples of focal method: 
example 1:
```java
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
        debug(logger, node, rootNode, instanceLocation);

        Set<ValidationMessage> errors = new LinkedHashSet<ValidationMessage>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            final String pname = it.next();
            final TextNode pnameText = TextNode.valueOf(pname);
            final Set<ValidationMessage> schemaErrors = innerSchema.validate(executionContext, pnameText, node, instanceLocation.append(pname));
    }
```
example 2:
```java
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
        CollectorContext collectorContext = executionContext.getCollectorContext();
        Set<ValidationMessage> errors = new HashSet<>();

        Scope parentScope = collectorContext.enterDynamicScope();
        try {
            debug(logger, node, rootNode, instanceLocation);
            errors = this.schema.validate(executionContext, node, rootNode, instanceLocation);
    }
```
example 3:
```java
    private ValidationResult validateAndCollect(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation) {
        // Get the config.
        SchemaValidatorsConfig config = this.validationContext.getConfig();
        // Get the collector context from the thread local.
        CollectorContext collectorContext = executionContext.getCollectorContext();
        // Set the walkEnabled and isValidationEnabled flag in internal validator state.
        setValidatorState(executionContext, false, true);
        // Validate.
        Set<ValidationMessage> errors = validate(executionContext, jsonNode, rootNode, instanceLocation);
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
class JsonSchema_validate_Test {
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