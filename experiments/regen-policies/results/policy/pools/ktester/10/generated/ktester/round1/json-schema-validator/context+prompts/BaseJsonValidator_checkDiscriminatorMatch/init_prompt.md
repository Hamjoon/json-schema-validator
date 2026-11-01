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
Here is the code of focal method, `checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, with the focal class `BaseJsonValidator_checkDiscriminatorMatch_Test`:
```Java
package com.networknt.schema;
import java.util.Set;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Iterator;
import com.networknt.schema.i18n.DefaultMessageSource;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.slf4j.Logger;
public abstract class BaseJsonValidator extends ValidationMessageHandler implements JsonValidator {
    protected final boolean suppressSubSchemaRetrieval;
    protected final ApplyDefaultsStrategy applyDefaultsStrategy;
    protected final JsonNode schemaNode;
    protected ValidationContext validationContext;
    protected static void checkDiscriminatorMatch(final DiscriminatorContext currentDiscriminatorContext, final ObjectNode discriminator, final String discriminatorPropertyValue, final JsonSchema jsonSchema) {
        if (discriminatorPropertyValue == null) {
            currentDiscriminatorContext.markMatch();
            return;
        }
        final JsonNode discriminatorMapping = discriminator.get("mapping");
        if (null == discriminatorMapping) {
            checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, discriminatorPropertyValue, jsonSchema);
        } else {
            checkForExplicitDiscriminatorMappingMatch(currentDiscriminatorContext, discriminatorPropertyValue, discriminatorMapping, jsonSchema);
            if ((!currentDiscriminatorContext.isDiscriminatorMatchFound()) && noExplicitDiscriminatorKeyOverride(discriminatorMapping, jsonSchema)) {
                checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, discriminatorPropertyValue, jsonSchema);
            }
        }
    }
protected JsonNodePath atRoot();
    private static void checkForExplicitDiscriminatorMappingMatch(final DiscriminatorContext currentDiscriminatorContext, final String discriminatorPropertyValue, final JsonNode discriminatorMapping, final JsonSchema schema);
    private static void checkForImplicitDiscriminatorMappingMatch(final DiscriminatorContext currentDiscriminatorContext, final String discriminatorPropertyValue, final JsonSchema schema);
    protected static void debug(Logger logger, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation);
    protected static boolean equals(double n1, double n2);
    protected JsonSchema fetchSubSchemaNode(ValidationContext validationContext);
    public JsonSchema getEvaluationParentSchema();
    @Override
public JsonNodePath getEvaluationPath();
    @Override
public String getKeyword();
    protected String getNodeFieldType();
    public JsonSchema getParentSchema();
    @Override
public SchemaLocation getSchemaLocation();
    public JsonNode getSchemaNode();
    private static boolean noExplicitDiscriminatorKeyOverride(final JsonNode discriminatorMapping, final JsonSchema parentSchema);
    protected void preloadJsonSchemas(final Collection<JsonSchema> schemas);
    protected static void registerAndMergeDiscriminator(final DiscriminatorContext currentDiscriminatorContext, final ObjectNode discriminator, final JsonSchema schema, final JsonNodePath instanceLocation);
    @Override
public String toString();
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node);
    public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format);
    public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
}
```


@input{context information}

constructors for class `com.networknt.schema.BaseJsonValidator`: 
params: com.networknt.schema.SchemaLocation schemaLocation 
com.networknt.schema.JsonNodePath evaluationPath 
com.networknt.schema.JsonSchema parentSchema 
com.networknt.schema.ValidatorTypeCode validatorType 
com.networknt.schema.ValidationContext validationContext 
body:
```java
    public BaseJsonValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
        this(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validatorType, validationContext,
                false);
    }

```
params: com.networknt.schema.SchemaLocation schemaLocation 
com.networknt.schema.JsonNodePath evaluationPath 
com.networknt.schema.JsonSchema parentSchema 
com.networknt.schema.ErrorMessageType errorMessageType 
com.networknt.schema.Keyword keyword 
com.networknt.schema.ValidationContext validationContext 
body:
```java
    public BaseJsonValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ErrorMessageType errorMessageType, Keyword keyword,
            ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
        super(validationContext != null
                && validationContext.getConfig() != null && validationContext.getConfig().isFailFast(),
                errorMessageType,
                (validationContext != null && validationContext.getConfig() != null)
                        ? validationContext.getConfig().isCustomMessageSupported()
                        : true,
                (validationContext != null && validationContext.getConfig() != null)
                        ? validationContext.getConfig().getMessageSource()
                        : DefaultMessageSource.getInstance(),
                keyword, parentSchema, schemaLocation, evaluationPath);
        this.validationContext = validationContext;
        this.schemaNode = schemaNode;
        this.suppressSubSchemaRetrieval = suppressSubSchemaRetrieval;
        this.applyDefaultsStrategy = (validationContext != null && validationContext.getConfig() != null
                && validationContext.getConfig().getApplyDefaultsStrategy() != null)
                        ? validationContext.getConfig().getApplyDefaultsStrategy()
                        : ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY;
        this.pathType = (validationContext != null && validationContext.getConfig() != null
                && validationContext.getConfig().getPathType() != null) ? validationContext.getConfig().getPathType()
                        : PathType.DEFAULT;
    }

```
params: com.networknt.schema.BaseJsonValidator copy 
body:
```java
    protected BaseJsonValidator(BaseJsonValidator copy) {
        super(copy);
        this.suppressSubSchemaRetrieval = copy.suppressSubSchemaRetrieval;
        this.applyDefaultsStrategy = copy.applyDefaultsStrategy;
        this.pathType = copy.pathType;
        this.schemaNode = copy.schemaNode;
        this.validationContext = copy.validationContext;
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
class `com.networknt.schema.ValidatorTypeCode`:
constructor:
```java
    private ValidatorTypeCode(String value, String errorCode, ValidatorFactory validatorFactory, VersionCode versionCode) {
        this.value = value;
        this.errorCode = errorCode;
        this.validatorFactory = validatorFactory;
        this.versionCode = versionCode;
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
        this.config = config == null ? new SchemaValidatorsConfig() : config;
        this.schemaReferences = schemaReferences;
        this.schemaResources = schemaResources;
        this.dynamicAnchors = dynamicAnchors;
    }

```
class `com.networknt.schema.ErrorMessageType`:
constructor:
```java
```
class `com.networknt.schema.Keyword`:
constructor:
```java
```
class `com.networknt.schema.BaseJsonValidator`:
constructor:
```java
    public BaseJsonValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
        this(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validatorType, validationContext,
                false);
    }

    public BaseJsonValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ErrorMessageType errorMessageType, Keyword keyword,
            ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
        super(validationContext != null
                && validationContext.getConfig() != null && validationContext.getConfig().isFailFast(),
                errorMessageType,
                (validationContext != null && validationContext.getConfig() != null)
                        ? validationContext.getConfig().isCustomMessageSupported()
                        : true,
                (validationContext != null && validationContext.getConfig() != null)
                        ? validationContext.getConfig().getMessageSource()
                        : DefaultMessageSource.getInstance(),
                keyword, parentSchema, schemaLocation, evaluationPath);
        this.validationContext = validationContext;
        this.schemaNode = schemaNode;
        this.suppressSubSchemaRetrieval = suppressSubSchemaRetrieval;
        this.applyDefaultsStrategy = (validationContext != null && validationContext.getConfig() != null
                && validationContext.getConfig().getApplyDefaultsStrategy() != null)
                        ? validationContext.getConfig().getApplyDefaultsStrategy()
                        : ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY;
        this.pathType = (validationContext != null && validationContext.getConfig() != null
                && validationContext.getConfig().getPathType() != null) ? validationContext.getConfig().getPathType()
                        : PathType.DEFAULT;
    }

    protected BaseJsonValidator(BaseJsonValidator copy) {
        super(copy);
        this.suppressSubSchemaRetrieval = copy.suppressSubSchemaRetrieval;
        this.applyDefaultsStrategy = copy.applyDefaultsStrategy;
        this.pathType = copy.pathType;
        this.schemaNode = copy.schemaNode;
        this.validationContext = copy.validationContext;
    }

```
class `com.networknt.schema.DiscriminatorContext`:
constructor:
```java
```

invoke examples of focal method: 
example 1:
```java
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
        debug(logger, node, rootNode, instanceLocation);
        CollectorContext collectorContext = executionContext.getCollectorContext();

        // get the Validator state object storing validation data
        ValidatorState state = executionContext.getValidatorState();

        Set<ValidationMessage> childSchemaErrors = new LinkedHashSet<>();

        for (JsonSchema schema : this.schemas) {
            Set<ValidationMessage> localErrors = new HashSet<>();

            Scope parentScope = collectorContext.enterDynamicScope();
            try {
                if (!state.isWalkEnabled()) {
                    localErrors = schema.validate(executionContext, node, rootNode, instanceLocation);
                } else {
                    localErrors = schema.walk(executionContext, node, rootNode, instanceLocation, true);
                }

                childSchemaErrors.addAll(localErrors);

                if (this.validationContext.getConfig().isOpenAPI3StyleDiscriminators()) {
                    final Iterator<JsonNode> arrayElements = this.schemaNode.elements();
                    while (arrayElements.hasNext()) {
                        final ObjectNode allOfEntry = (ObjectNode) arrayElements.next();
                        final JsonNode $ref = allOfEntry.get("$ref");
                        if (null != $ref) {
                            final DiscriminatorContext currentDiscriminatorContext = executionContext
                                    .getCurrentDiscriminatorContext();
                            if (null != currentDiscriminatorContext) {
                                final ObjectNode discriminator = currentDiscriminatorContext
                                        .getDiscriminatorForPath(allOfEntry.get("$ref").asText());
                                if (null != discriminator) {
                                    registerAndMergeDiscriminator(currentDiscriminatorContext, discriminator, this.parentSchema, instanceLocation);
                                    // now we have to check whether we have hit the right target
                                    final String discriminatorPropertyName = discriminator.get("propertyName").asText();
                                    final JsonNode discriminatorNode = node.get(discriminatorPropertyName);
                                    final String discriminatorPropertyValue = discriminatorNode == null
                                            ? null
                                            : discriminatorNode.textValue();

                                    final JsonSchema jsonSchema = this.parentSchema;
                                    checkDiscriminatorMatch(
    }
```
example 2:
```java
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation) {
        if (validationContext.getConfig().isOpenAPI3StyleDiscriminators()) {
            ObjectNode discriminator = (ObjectNode) schemaNode.get("discriminator");
            if (null != discriminator && null != executionContext.getCurrentDiscriminatorContext()) {
                executionContext.getCurrentDiscriminatorContext().registerDiscriminator(schemaLocation,
                        discriminator);
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
                if (results == null || results.isEmpty()) {
                    parentScope.mergeWith(scope);
                } else {
                    if (errors == null) {
                        errors = new LinkedHashSet<>();
                    }
                    errors.addAll(results);
                    if (v instanceof PrefixItemsValidator || v instanceof ItemsValidator
                            || v instanceof ItemsValidator202012 || v instanceof ContainsValidator) {
                        collectorContext.getEvaluatedItems().addAll(scope.getEvaluatedItems());
                    }
                    if (v instanceof PropertiesValidator || v instanceof AdditionalPropertiesValidator
                            || v instanceof PatternPropertiesValidator) {
                        collectorContext.getEvaluatedProperties().addAll(scope.getEvaluatedProperties());
                    }
                }

            }
        }

        if (config.isOpenAPI3StyleDiscriminators()) {
            ObjectNode discriminator = (ObjectNode) this.schemaNode.get("discriminator");
            if (null != discriminator) {
                final DiscriminatorContext discriminatorContext = executionContext
                        .getCurrentDiscriminatorContext();
                if (null != discriminatorContext) {
                    final ObjectNode discriminatorToUse;
                    final ObjectNode discriminatorFromContext = discriminatorContext
                            .getDiscriminatorForPath(this.schemaLocation);
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
                    final String discriminatorPropertyValue = discriminatorNode == null ? null
                            : discriminatorNode.asText();
                    checkDiscriminatorMatch(discriminatorContext, discriminatorToUse, discriminatorPropertyValue,
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
class BaseJsonValidator_checkDiscriminatorMatch_Test {
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