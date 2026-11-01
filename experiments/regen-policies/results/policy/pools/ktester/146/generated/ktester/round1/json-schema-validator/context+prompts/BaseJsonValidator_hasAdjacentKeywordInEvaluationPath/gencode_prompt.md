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
Here is the code of focal method, `hasAdjacentKeywordInEvaluationPath(String)`, with the focal class `BaseJsonValidator`:
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
    protected boolean hasAdjacentKeywordInEvaluationPath(String keyword) {
        JsonSchema schema = getEvaluationParentSchema();
        while (schema != null) {
            for (JsonValidator validator : schema.getValidators()) {
                if (keyword.equals(validator.getKeyword())) {
                    return true;
                }
            }
            Object element = schema.getEvaluationPath().getElement(-1);
            if ("properties".equals(element) || "items".equals(element)) {
                // If there is a change in instance location then return false
                return false;
            }
            schema = schema.getEvaluationParentSchema();
        } 
        return false;
    }
protected JsonNodePath atRoot();
    protected static void checkDiscriminatorMatch(final DiscriminatorContext currentDiscriminatorContext, final ObjectNode discriminator, final String discriminatorPropertyValue, final JsonSchema jsonSchema);
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
    @Override
protected MessageSourceValidationMessage.Builder message();
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
 

api document of method hasAdjacentKeywordInEvaluationPath(String): 

     * Determines if the keyword exists adjacent in the evaluation path.
     * <p>
     * This does not check if the keyword exists in the current meta schema as this
     * can be a cross-draft case where the properties keyword is in a Draft 7 schema
     * and the unevaluatedProperties keyword is in an outer Draft 2020-12 schema.
     * <p>
     * The fact that the validator exists in the evaluation path implies that the
     * keyword was valid in whatever meta schema for that schema it was created for.
     * 
     * @param keyword the keyword to check
     * @return true if found
     

return type: 
boolean

dependent classes: 
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected boolean hasAdjacentKeywordInEvaluationPath(String)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`, api document: 
     * Determines if the keyword exists adjacent in the evaluation path.
     * <p>
     * This does not check if the keyword exists in the current meta schema as this
     * can be a cross-draft case where the properties keyword is in a Draft 7 schema
     * and the unevaluatedProperties keyword is in an outer Draft 2020-12 schema.
     * <p>
     * The fact that the validator exists in the evaluation path implies that the
     * keyword was valid in whatever meta schema for that schema it was created for.
     * 
     * @param keyword the keyword to check
     * @return true if found
     ;
class `com.networknt.schema.OneOfValidator`:
	related functions:
		method `protected boolean canShortCircuit()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `private static boolean isEnumObjectSchema(JsonSchema)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;
class `com.networknt.schema.utils.StringChecker`:
	related functions:
		method `public static boolean isBoolean(String)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;
class `com.networknt.org.apache.commons.validator.routines.DomainValidator.IDNBUGHOLDER`:
	related functions:
		method `private static boolean keepsTrailingDot()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.