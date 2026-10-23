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
Here is the code of focal method, `walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, with the focal class `PropertiesValidator`:
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
    private final Map<String, JsonSchema> schemas = new LinkedHashMap<>();
    @Override
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) {
        HashSet<ValidationMessage> validationMessages = new LinkedHashSet<>();
        if ((this.applyDefaultsStrategy.shouldApplyPropertyDefaults() && (null != node)) && (node.getNodeType() == JsonNodeType.OBJECT)) {
            applyPropertyDefaults(((ObjectNode) (node)));
        }
        if (shouldValidateSchema) {
            validationMessages.addAll(validate(executionContext, node, rootNode, at));
        } else {
            WalkListenerRunner propertyWalkListenerRunner = new DefaultPropertyWalkListenerRunner(this.validationContext.getConfig().getPropertyWalkListeners());
            for (Map.Entry<String, JsonSchema> entry : this.schemas.entrySet()) {
                walkSchema(executionContext, entry, node, rootNode, at, shouldValidateSchema, validationMessages, propertyWalkListenerRunner);
            }
        }
        return validationMessages;
    }
private void applyPropertyDefaults(ObjectNode node);
    public Map<String, JsonSchema> getSchemas();
    @Override
public void preloadJsonSchema();
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
    private void walkSchema(ExecutionContext executionContext, Map.Entry<String, JsonSchema> entry, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema, Set<ValidationMessage> validationMessages, WalkListenerRunner propertyWalkListenerRunner);
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

dependent classes: 
class `com.networknt.schema.ExecutionContext`:
	
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;
		com.networknt.schema.ApplyDefaultsStrategy com.networknt.schema.BaseJsonValidator.applyDefaultsStrategy;
	
class `com.networknt.schema.PropertiesValidator`:
	dependent fields:
		java.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
		method `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.ItemsValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, api document: 
     * This method both validates and collects the data in a CollectorContext.
     * Unlike others this methods cleans and removes everything from collector
     * context before returning.
     * @param executionContext ExecutionContext
     * @param jsonNode JsonNode
     * @param rootNode JsonNode
     * @param at       String path
     *
     * @return ValidationResult
     ;
		method `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.format.DateTimeValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.