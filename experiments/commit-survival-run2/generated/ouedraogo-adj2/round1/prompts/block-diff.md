# Corrected dependency-block diff

Compared with the frozen addendum 2 blocks. Only declaration headers and varargs notation change; dependency order, membership, omissions, API order, constraints, and caps are unchanged.

## JsonSchema

```diff
--- ouedraogo-adj/JsonSchema
+++ ouedraogo-adj2/JsonSchema
@@ -1,5 +1,5 @@
 // public API of com.networknt.schema.BaseJsonValidator
-class BaseJsonValidator<> extends ValidationMessageHandler implements JsonValidator {
+public abstract class BaseJsonValidator extends ValidationMessageHandler implements JsonValidator {
   public BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext);
   public BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval);
   protected static boolean equals(double n1, double n2);
@@ -20,10 +20,10 @@
 }
 
 // public API of com.networknt.schema.ValidationMessageHandler
-class ValidationMessageHandler<> {
+public abstract class ValidationMessageHandler {
   protected ValidationMessageHandler(boolean failFast, ErrorMessageType errorMessageType, String customMessage, ResourceBundle resourceBundle, ValidatorTypeCode validatorType, JsonSchema parentSchema, String schemaPath);
-  protected ValidationMessage buildValidationMessage(String at, String[] arguments);
-  protected ValidationMessage constructValidationMessage(String messageKey, String at, String[] arguments);
+  protected ValidationMessage buildValidationMessage(String at, String... arguments);
+  protected ValidationMessage constructValidationMessage(String messageKey, String at, String... arguments);
   protected ValidatorTypeCode getValidatorType();
   protected ErrorMessageType getErrorMessageType();
   protected boolean isPartOfOneOfMultipleType();
@@ -32,7 +32,7 @@
 }
 
 // public API of com.networknt.schema.JsonValidator
-interface JsonValidator<> extends JsonSchemaWalker {
+public interface JsonValidator extends JsonSchemaWalker {
   default Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode rootNode);
    Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
   default void preloadJsonSchema() throws JsonSchemaException;
@@ -40,12 +40,12 @@
 }
 
 // public API of com.networknt.schema.walk.JsonSchemaWalker
-interface JsonSchemaWalker<> {
+public interface JsonSchemaWalker {
    Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
 }
 
 // public API of com.networknt.schema.ValidationContext
-class ValidationContext<> {
+public class ValidationContext {
   public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema, JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config);
   public JsonSchema newSchema(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
   public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode, JsonSchema parentSchema, String customMessage);
@@ -65,7 +65,7 @@
 }
 
 // public API of com.networknt.schema.JsonMetaSchema
-class JsonMetaSchema<> {
+public class JsonMetaSchema {
   public static final List<Format> COMMON_BUILTIN_FORMATS;
   public static JsonMetaSchema getV4();
   public static JsonMetaSchema getV6();
@@ -83,7 +83,7 @@
 }
 
 // public API of com.networknt.schema.TypeValidator
-class TypeValidator<> extends BaseJsonValidator {
+public class TypeValidator extends BaseJsonValidator {
   public TypeValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext);
   public JsonType getSchemaType();
   public boolean equalsToSchemaType(JsonNode node);
@@ -91,13 +91,13 @@
 }
 
 // public API of com.networknt.schema.walk.WalkListenerRunner
-interface WalkListenerRunner<> {
+public interface WalkListenerRunner {
   public boolean runPreWalkListeners(ExecutionContext executionContext, String keyWordPath, JsonNode node, JsonNode rootNode, String at, String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext, JsonSchemaFactory jsonSchemaFactory);
   public void runPostWalkListeners(ExecutionContext executionContext, String keyWordPath, JsonNode node, JsonNode rootNode, String at, String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext, JsonSchemaFactory jsonSchemaFactory, Set<ValidationMessage> validationMessages);
 }
 
 // public API of com.networknt.schema.ExecutionContext
-class ExecutionContext<> {
+public class ExecutionContext {
   public ExecutionContext();
   public ExecutionContext(CollectorContext collectorContext);
   public CollectorContext getCollectorContext();
@@ -105,7 +105,7 @@
 }
 
 // public API of com.networknt.schema.ValidationMessage
-class ValidationMessage<> {
+public class ValidationMessage {
   public String getCode();
   public String getPath();
   public String getSchemaPath();
@@ -118,13 +118,13 @@
   public int hashCode();
   public String getType();
   public void setType(String type);
-  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String[] arguments);
-  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String[] arguments);
+  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String... arguments);
+  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String... arguments);
   public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, Map<String, Object> details);
 }
 
 // public API of com.networknt.schema.ValidationResult
-class ValidationResult<> {
+public class ValidationResult {
   public ValidationResult(Set<ValidationMessage> validationMessages, ExecutionContext executionContext);
   public Set<ValidationMessage> getValidationMessages();
   public ExecutionContext getExecutionContext();
@@ -132,7 +132,7 @@
 }
 
 // public API of com.networknt.schema.CollectorContext.Scope
-class Scope<> {
+public static class Scope {
   public boolean isTop();
   public JsonSchema getContainingSchema();
   public Collection<String> getEvaluatedItems();
@@ -142,7 +142,7 @@
 }
 
 // public API of com.networknt.schema.SpecVersion.VersionFlag
-enum VersionFlag<> {
+public enum VersionFlag {
   public static final VersionFlag V4;
   public static final VersionFlag V6;
   public static final VersionFlag V7;
@@ -154,7 +154,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext.DiscriminatorContext
-class DiscriminatorContext<> {
+public static class DiscriminatorContext {
   public void registerDiscriminator(String schemaPath, ObjectNode discriminator);
   public ObjectNode getDiscriminatorForPath(String schemaPath);
   public void markMatch();
@@ -163,7 +163,7 @@
 }
 
 // public API of com.networknt.schema.utils.StringUtils
-class StringUtils<> {
+public final class StringUtils {
   public static boolean isBlank(String s);
   public static boolean isNotBlank(String s);
 }
```

## BaseJsonValidator

```diff
--- ouedraogo-adj/BaseJsonValidator
+++ ouedraogo-adj2/BaseJsonValidator
@@ -1,8 +1,8 @@
 // public API of com.networknt.schema.ValidationMessageHandler
-class ValidationMessageHandler<> {
+public abstract class ValidationMessageHandler {
   protected ValidationMessageHandler(boolean failFast, ErrorMessageType errorMessageType, String customMessage, ResourceBundle resourceBundle, ValidatorTypeCode validatorType, JsonSchema parentSchema, String schemaPath);
-  protected ValidationMessage buildValidationMessage(String at, String[] arguments);
-  protected ValidationMessage constructValidationMessage(String messageKey, String at, String[] arguments);
+  protected ValidationMessage buildValidationMessage(String at, String... arguments);
+  protected ValidationMessage constructValidationMessage(String messageKey, String at, String... arguments);
   protected ValidatorTypeCode getValidatorType();
   protected ErrorMessageType getErrorMessageType();
   protected boolean isPartOfOneOfMultipleType();
@@ -11,7 +11,7 @@
 }
 
 // public API of com.networknt.schema.JsonValidator
-interface JsonValidator<> extends JsonSchemaWalker {
+public interface JsonValidator extends JsonSchemaWalker {
   default Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode rootNode);
    Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
   default void preloadJsonSchema() throws JsonSchemaException;
@@ -19,12 +19,12 @@
 }
 
 // public API of com.networknt.schema.walk.JsonSchemaWalker
-interface JsonSchemaWalker<> {
+public interface JsonSchemaWalker {
    Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
 }
 
 // public API of com.networknt.schema.JsonSchema
-class JsonSchema<> extends BaseJsonValidator {
+public class JsonSchema extends BaseJsonValidator {
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode);
   public JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent);
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode, boolean suppressSubSchemaRetrieval);
@@ -51,7 +51,7 @@
 }
 
 // public API of com.networknt.schema.ValidatorTypeCode
-enum ValidatorTypeCode<> implements Keyword,ErrorMessageType {
+public enum ValidatorTypeCode implements Keyword,ErrorMessageType {
   public static final ValidatorTypeCode ADDITIONAL_PROPERTIES;
   public static final ValidatorTypeCode ALL_OF;
   public static final ValidatorTypeCode ANY_OF;
@@ -117,7 +117,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext
-class ValidationContext<> {
+public class ValidationContext {
   public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema, JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config);
   public JsonSchema newSchema(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
   public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode, JsonSchema parentSchema, String customMessage);
@@ -137,7 +137,7 @@
 }
 
 // public API of com.networknt.schema.ApplyDefaultsStrategy
-class ApplyDefaultsStrategy<> {
+public class ApplyDefaultsStrategy {
   public ApplyDefaultsStrategy(boolean applyPropertyDefaults, boolean applyPropertyDefaultsIfNull, boolean applyArrayDefaults);
   public boolean shouldApplyPropertyDefaults();
   public boolean shouldApplyPropertyDefaultsIfNull();
@@ -145,7 +145,7 @@
 }
 
 // public API of com.networknt.schema.PathType
-enum PathType<> {
+public enum PathType {
   public static final PathType LEGACY;
   public static final PathType JSON_PATH;
   public static final PathType JSON_POINTER;
@@ -157,7 +157,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext.DiscriminatorContext
-class DiscriminatorContext<> {
+public static class DiscriminatorContext {
   public void registerDiscriminator(String schemaPath, ObjectNode discriminator);
   public ObjectNode getDiscriminatorForPath(String schemaPath);
   public void markMatch();
@@ -166,7 +166,7 @@
 }
 
 // public API of com.networknt.schema.ExecutionContext
-class ExecutionContext<> {
+public class ExecutionContext {
   public ExecutionContext();
   public ExecutionContext(CollectorContext collectorContext);
   public CollectorContext getCollectorContext();
@@ -174,7 +174,7 @@
 }
 
 // public API of com.networknt.schema.ValidationMessage
-class ValidationMessage<> {
+public class ValidationMessage {
   public String getCode();
   public String getPath();
   public String getSchemaPath();
@@ -187,8 +187,8 @@
   public int hashCode();
   public String getType();
   public void setType(String type);
-  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String[] arguments);
-  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String[] arguments);
+  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String... arguments);
+  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String... arguments);
   public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, Map<String, Object> details);
 }
 
```

## AnyOfValidator

```diff
--- ouedraogo-adj/AnyOfValidator
+++ ouedraogo-adj2/AnyOfValidator
@@ -1,5 +1,5 @@
 // public API of com.networknt.schema.BaseJsonValidator
-class BaseJsonValidator<> extends ValidationMessageHandler implements JsonValidator {
+public abstract class BaseJsonValidator extends ValidationMessageHandler implements JsonValidator {
   public BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext);
   public BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval);
   protected static boolean equals(double n1, double n2);
@@ -20,10 +20,10 @@
 }
 
 // public API of com.networknt.schema.ValidationMessageHandler
-class ValidationMessageHandler<> {
+public abstract class ValidationMessageHandler {
   protected ValidationMessageHandler(boolean failFast, ErrorMessageType errorMessageType, String customMessage, ResourceBundle resourceBundle, ValidatorTypeCode validatorType, JsonSchema parentSchema, String schemaPath);
-  protected ValidationMessage buildValidationMessage(String at, String[] arguments);
-  protected ValidationMessage constructValidationMessage(String messageKey, String at, String[] arguments);
+  protected ValidationMessage buildValidationMessage(String at, String... arguments);
+  protected ValidationMessage constructValidationMessage(String messageKey, String at, String... arguments);
   protected ValidatorTypeCode getValidatorType();
   protected ErrorMessageType getErrorMessageType();
   protected boolean isPartOfOneOfMultipleType();
@@ -32,7 +32,7 @@
 }
 
 // public API of com.networknt.schema.JsonValidator
-interface JsonValidator<> extends JsonSchemaWalker {
+public interface JsonValidator extends JsonSchemaWalker {
   default Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode rootNode);
    Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
   default void preloadJsonSchema() throws JsonSchemaException;
@@ -40,12 +40,12 @@
 }
 
 // public API of com.networknt.schema.walk.JsonSchemaWalker
-interface JsonSchemaWalker<> {
+public interface JsonSchemaWalker {
    Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
 }
 
 // public API of com.networknt.schema.JsonSchema
-class JsonSchema<> extends BaseJsonValidator {
+public class JsonSchema extends BaseJsonValidator {
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode);
   public JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent);
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode, boolean suppressSubSchemaRetrieval);
@@ -72,7 +72,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext
-class ValidationContext<> {
+public class ValidationContext {
   public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema, JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config);
   public JsonSchema newSchema(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
   public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode, JsonSchema parentSchema, String customMessage);
@@ -92,7 +92,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext.DiscriminatorContext
-class DiscriminatorContext<> {
+public static class DiscriminatorContext {
   public void registerDiscriminator(String schemaPath, ObjectNode discriminator);
   public ObjectNode getDiscriminatorForPath(String schemaPath);
   public void markMatch();
@@ -101,7 +101,7 @@
 }
 
 // public API of com.networknt.schema.ExecutionContext
-class ExecutionContext<> {
+public class ExecutionContext {
   public ExecutionContext();
   public ExecutionContext(CollectorContext collectorContext);
   public CollectorContext getCollectorContext();
@@ -109,7 +109,7 @@
 }
 
 // public API of com.networknt.schema.ValidationMessage
-class ValidationMessage<> {
+public class ValidationMessage {
   public String getCode();
   public String getPath();
   public String getSchemaPath();
@@ -122,13 +122,13 @@
   public int hashCode();
   public String getType();
   public void setType(String type);
-  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String[] arguments);
-  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String[] arguments);
+  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String... arguments);
+  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String... arguments);
   public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, Map<String, Object> details);
 }
 
 // public API of com.networknt.schema.CollectorContext.Scope
-class Scope<> {
+public static class Scope {
   public boolean isTop();
   public JsonSchema getContainingSchema();
   public Collection<String> getEvaluatedItems();
```

## OneOfValidator

```diff
--- ouedraogo-adj/OneOfValidator
+++ ouedraogo-adj2/OneOfValidator
@@ -1,5 +1,5 @@
 // public API of com.networknt.schema.BaseJsonValidator
-class BaseJsonValidator<> extends ValidationMessageHandler implements JsonValidator {
+public abstract class BaseJsonValidator extends ValidationMessageHandler implements JsonValidator {
   public BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext);
   public BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval);
   protected static boolean equals(double n1, double n2);
@@ -20,10 +20,10 @@
 }
 
 // public API of com.networknt.schema.ValidationMessageHandler
-class ValidationMessageHandler<> {
+public abstract class ValidationMessageHandler {
   protected ValidationMessageHandler(boolean failFast, ErrorMessageType errorMessageType, String customMessage, ResourceBundle resourceBundle, ValidatorTypeCode validatorType, JsonSchema parentSchema, String schemaPath);
-  protected ValidationMessage buildValidationMessage(String at, String[] arguments);
-  protected ValidationMessage constructValidationMessage(String messageKey, String at, String[] arguments);
+  protected ValidationMessage buildValidationMessage(String at, String... arguments);
+  protected ValidationMessage constructValidationMessage(String messageKey, String at, String... arguments);
   protected ValidatorTypeCode getValidatorType();
   protected ErrorMessageType getErrorMessageType();
   protected boolean isPartOfOneOfMultipleType();
@@ -32,7 +32,7 @@
 }
 
 // public API of com.networknt.schema.JsonValidator
-interface JsonValidator<> extends JsonSchemaWalker {
+public interface JsonValidator extends JsonSchemaWalker {
   default Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode rootNode);
    Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
   default void preloadJsonSchema() throws JsonSchemaException;
@@ -40,12 +40,12 @@
 }
 
 // public API of com.networknt.schema.walk.JsonSchemaWalker
-interface JsonSchemaWalker<> {
+public interface JsonSchemaWalker {
    Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
 }
 
 // public API of com.networknt.schema.JsonSchema
-class JsonSchema<> extends BaseJsonValidator {
+public class JsonSchema extends BaseJsonValidator {
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode);
   public JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent);
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode, boolean suppressSubSchemaRetrieval);
@@ -72,7 +72,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext
-class ValidationContext<> {
+public class ValidationContext {
   public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema, JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config);
   public JsonSchema newSchema(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
   public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode, JsonSchema parentSchema, String customMessage);
@@ -92,7 +92,7 @@
 }
 
 // public API of com.networknt.schema.ExecutionContext
-class ExecutionContext<> {
+public class ExecutionContext {
   public ExecutionContext();
   public ExecutionContext(CollectorContext collectorContext);
   public CollectorContext getCollectorContext();
@@ -100,7 +100,7 @@
 }
 
 // public API of com.networknt.schema.ValidationMessage
-class ValidationMessage<> {
+public class ValidationMessage {
   public String getCode();
   public String getPath();
   public String getSchemaPath();
@@ -113,13 +113,13 @@
   public int hashCode();
   public String getType();
   public void setType(String type);
-  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String[] arguments);
-  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String[] arguments);
+  public static ValidationMessage ofWithCustom(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String customMessage, String at, String schemaPath, String... arguments);
+  public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, String... arguments);
   public static ValidationMessage of(String type, ErrorMessageType errorMessageType, MessageFormat messageFormat, String at, String schemaPath, Map<String, Object> details);
 }
 
 // public API of com.networknt.schema.CollectorContext
-class CollectorContext<> {
+public class CollectorContext {
   public CollectorContext();
   public CollectorContext(boolean disableUnevaluatedItems, boolean disableUnevaluatedProperties);
   public Scope enterDynamicScope();
@@ -137,7 +137,7 @@
 }
 
 // public API of com.networknt.schema.CollectorContext.Scope
-class Scope<> {
+public static class Scope {
   public boolean isTop();
   public JsonSchema getContainingSchema();
   public Collection<String> getEvaluatedItems();
```

## JsonSchemaFactory

```diff
--- ouedraogo-adj/JsonSchemaFactory
+++ ouedraogo-adj2/JsonSchemaFactory
@@ -1,5 +1,5 @@
 // public API of com.networknt.schema.uri.URISchemeFactory
-class URISchemeFactory<> implements URIFactory {
+public class URISchemeFactory implements URIFactory {
   public URISchemeFactory(Map<String, URIFactory> uriFactories);
   public Map<String, URIFactory> getURIFactories();
   public URI create(String uri);
@@ -7,19 +7,19 @@
 }
 
 // public API of com.networknt.schema.uri.URISchemeFetcher
-class URISchemeFetcher<> implements URIFetcher {
+public class URISchemeFetcher implements URIFetcher {
   public URISchemeFetcher(Map<String, URIFetcher> uriFetchers);
   public Map<String, URIFetcher> getURIFetchers();
   public InputStream fetch(URI uri) throws IOException;
 }
 
 // public API of com.networknt.schema.urn.URNFactory
-interface URNFactory<> {
+public interface URNFactory {
    URI create(String urn);
 }
 
 // public API of com.networknt.schema.JsonMetaSchema
-class JsonMetaSchema<> {
+public class JsonMetaSchema {
   public static final List<Format> COMMON_BUILTIN_FORMATS;
   public static JsonMetaSchema getV4();
   public static JsonMetaSchema getV6();
@@ -37,16 +37,16 @@
 }
 
 // public API of com.networknt.schema.uri.URITranslator.CompositeURITranslator
-class CompositeURITranslator<> extends ArrayList<URITranslator> implements URITranslator {
+public static class CompositeURITranslator extends ArrayList<URITranslator> implements URITranslator {
   public CompositeURITranslator();
-  public CompositeURITranslator(URITranslator[] translators);
+  public CompositeURITranslator(URITranslator... translators);
   public CompositeURITranslator(Collection<? extends URITranslator> c);
   public URI translate(URI original);
   public CompositeURITranslator with(URITranslator translator);
 }
 
 // public API of com.networknt.schema.JsonSchema
-class JsonSchema<> extends BaseJsonValidator {
+public class JsonSchema extends BaseJsonValidator {
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode);
   public JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent);
   public JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode, boolean suppressSubSchemaRetrieval);
@@ -73,14 +73,14 @@
 }
 
 // public API of com.networknt.schema.JsonSchemaFactory.Builder
-class Builder<> {
+public static class Builder {
   public Builder();
   public Builder objectMapper(ObjectMapper objectMapper);
   public Builder yamlMapper(YAMLMapper yamlMapper);
   public Builder defaultMetaSchemaURI(String defaultMetaSchemaURI);
-  public Builder uriFactory(URIFactory uriFactory, String[] schemes);
+  public Builder uriFactory(URIFactory uriFactory, String... schemes);
   public Builder uriFactory(URIFactory uriFactory, Iterable<String> schemes);
-  public Builder uriFetcher(URIFetcher uriFetcher, String[] schemes);
+  public Builder uriFetcher(URIFetcher uriFetcher, String... schemes);
   public Builder uriFetcher(URIFetcher uriFetcher, Iterable<String> schemes);
   public Builder addMetaSchema(JsonMetaSchema jsonMetaSchema);
   public Builder addMetaSchemas(Collection<? extends JsonMetaSchema> jsonMetaSchemas);
@@ -94,7 +94,7 @@
 }
 
 // public API of com.networknt.schema.SpecVersion.VersionFlag
-enum VersionFlag<> {
+public enum VersionFlag {
   public static final VersionFlag V4;
   public static final VersionFlag V6;
   public static final VersionFlag V7;
@@ -106,13 +106,13 @@
 }
 
 // public API of com.networknt.schema.JsonSchemaVersion
-class JsonSchemaVersion<> {
+public abstract class JsonSchemaVersion {
   public static final List<Format> BUILTIN_FORMATS;
   public abstract JsonMetaSchema getInstance();
 }
 
 // public API of com.networknt.schema.SchemaValidatorsConfig
-class SchemaValidatorsConfig<> {
+public class SchemaValidatorsConfig {
   public static final String ALL_KEYWORD_WALK_LISTENER_KEY;
   public SchemaValidatorsConfig disableUnevaluatedAnalysis();
   public SchemaValidatorsConfig disableUnevaluatedItems();
@@ -179,7 +179,7 @@
 }
 
 // public API of com.networknt.schema.ValidationContext
-class ValidationContext<> {
+public class ValidationContext {
   public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema, JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config);
   public JsonSchema newSchema(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
   public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode, JsonSchema parentSchema, String customMessage);
@@ -199,30 +199,30 @@
 }
 
 // public API of com.networknt.schema.uri.URIFactory
-interface URIFactory<> {
+public interface URIFactory {
    URI create(String uri);
    URI create(URI baseURI, String segment);
 }
 
 // public API of com.networknt.schema.uri.URITranslator
-interface URITranslator<> {
+public interface URITranslator {
   static final URITranslator NOOP;
    URI translate(URI original);
   static URITranslator map(String source, String target);
   static URITranslator map(URI source, URI target);
   static URITranslator map(Map<String, String> uriMappings);
-  static CompositeURITranslator combine(URITranslator[] uriTranslators);
+  static CompositeURITranslator combine(URITranslator... uriTranslators);
   static URITranslator prefix(String source, String target);
   static CompositeURITranslator combine(Collection<? extends URITranslator> uriTranslators);
 }
 
 // public API of com.networknt.schema.uri.URIFetcher
-interface URIFetcher<> {
+public interface URIFetcher {
    InputStream fetch(URI uri) throws IOException;
 }
 
 // public API of com.networknt.schema.uri.URLFactory
-class URLFactory<> implements URIFactory {
+public final class URLFactory implements URIFactory {
   public static final Set<String> SUPPORTED_SCHEMES;
   public URI create(String uri);
   public URI create(URI baseURI, String segment);
```

