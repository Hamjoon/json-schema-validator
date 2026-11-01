You are a professional Java software testing expert. Your task is to generate high-quality unit test code for a focal method, using **JUnit5** and **Mockito** libraries. 

You'll be provided with:
1. The focal method to test.
2. The target class's structure (fields and methods).
3. A pre-defined test class framework with `@Before`/`@After` methods.
4. Test cases in JSON format.
5. Any relevant context.

**Requirements:**

Code Quality:
- Include clear comments explaining each test scenario.
- Ensure code is clean, understandable, and maintainable.
- Do not directly access private methods or fields. **Use reflection if the focal method is private.**
- Include all necessary imports; code must be free of syntax errors.

**Test Structure:**
- Follow the given test class framework; do not modify `@Before`/`@After` methods.
- Create one test function per test case group, ensuring all cases are covered.
- Use parameterized tests where appropriate.
- Reinitialize variables inside test functions if `@Setup` values don't fit.
- Properly mock and set up dependencies.

@input{focal method and target class}
Here is the code of focal method, `checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, with the focal class `BaseJsonValidator`:
```Java
package com.networknt.schema;
import java.util.Set;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.i18n.MessageSource;
import java.util.Collection;
import java.util.Iterator;
import com.networknt.schema.i18n.DefaultMessageSource;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.slf4j.Logger;
import com.networknt.schema.annotation.JsonNodeAnnotation;
public abstract class BaseJsonValidator extends ValidationMessageHandler implements JsonValidator {
    protected final boolean suppressSubSchemaRetrieval;
    protected final JsonNode schemaNode;
    protected final ValidationContext validationContext;
    protected static void checkDiscriminatorMatch(final DiscriminatorContext currentDiscriminatorContext, final ObjectNode discriminator, final String discriminatorPropertyValue, final JsonSchema jsonSchema) {
        if (discriminatorPropertyValue == null) {
            currentDiscriminatorContext.markIgnore();
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
    protected boolean collectAnnotations(ExecutionContext executionContext);
    protected boolean collectAnnotations(ExecutionContext executionContext, String keyword);
    public static void debug(Logger logger, ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation);
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
    protected boolean hasAdjacentKeywordInEvaluationPath(String keyword);
    @Override
protected MessageSourceValidationMessage.Builder message();
    private static boolean noExplicitDiscriminatorKeyOverride(final JsonNode discriminatorMapping, final JsonSchema parentSchema);
    protected void preloadJsonSchemas(final Collection<JsonSchema> schemas);
    protected void putAnnotation(ExecutionContext executionContext, Consumer<JsonNodeAnnotation.Builder> customizer);
    protected static void registerAndMergeDiscriminator(final DiscriminatorContext currentDiscriminatorContext, final ObjectNode discriminator, final JsonSchema schema, final JsonNodePath instanceLocation);
    @Override
public String toString();
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node);
}
```

@input{initial test class framework}
```Java
<initial_class>
```

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```


@input{context information}

api document of class com.networknt.schema.BaseJsonValidator: 

 * Base {@link JsonValidator}. 
 

api document of method checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema): 

     * Checks based on the current {@link DiscriminatorContext} whether the provided {@link JsonSchema} a match against
     * the current discriminator.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param discriminatorPropertyValue  the value of the <code>discriminator/propertyName</code> field
     * @param jsonSchema                  the {@link JsonSchema} to check
     

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected static void checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, api document: 
     * Checks based on the current {@link DiscriminatorContext} whether the provided {@link JsonSchema} a match against
     * the current discriminator.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param discriminatorPropertyValue  the value of the <code>discriminator/propertyName</code> field
     * @param jsonSchema                  the {@link JsonSchema} to check
     ;
		method `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, api document: 
     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine
     * the <code>propertyName</code> or mappings.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param schema                      the value of the <code>discriminator/propertyName</code> field
     * @param instanceLocation                          the logging prefix
     ;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;
class `com.networknt.schema.OneOfValidator`:
	related functions:
		method `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;
class `com.networknt.schema.AnyOfValidator`:
	related functions:
		method `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.