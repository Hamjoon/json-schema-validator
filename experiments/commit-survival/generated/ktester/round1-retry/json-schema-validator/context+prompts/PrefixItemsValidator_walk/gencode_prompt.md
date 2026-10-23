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
Here is the code of focal method, `walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, with the focal class `PrefixItemsValidator`:
```Java
package com.networknt.schema;
import java.util.Set;
import com.networknt.schema.walk.DefaultItemWalkListenerRunner;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.walk.WalkListenerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
public class PrefixItemsValidator extends BaseJsonValidator {
    private final List<JsonSchema> tupleSchema;
    @Override
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) {
        Set<ValidationMessage> validationMessages = new LinkedHashSet<>();
        if (this.applyDefaultsStrategy.shouldApplyArrayDefaults() && node.isArray()) {
            ArrayNode array = ((ArrayNode) (node));
            for (int i = 0; i < Math.min(node.size(), this.tupleSchema.size()); ++i) {
                JsonNode n = node.get(i);
                JsonNode defaultNode = this.tupleSchema.get(i).getSchemaNode().get("default");
                if (n.isNull() && (defaultNode != null)) {
                    array.set(i, defaultNode);
                    n = defaultNode;
                }
                doWalk(executionContext, validationMessages, i, n, rootNode, at, shouldValidateSchema);
            }
        }
        return validationMessages;
    }
private void doWalk(ExecutionContext executionContext, Set<ValidationMessage> validationMessages, int i, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
    public List<JsonSchema> getTupleSchema();
    @Override
public void preloadJsonSchema();
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
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
		com.networknt.schema.ApplyDefaultsStrategy com.networknt.schema.BaseJsonValidator.applyDefaultsStrategy;
	
class `com.networknt.schema.PrefixItemsValidator`:
	dependent fields:
		java.util.List<com.networknt.schema.JsonSchema> com.networknt.schema.PrefixItemsValidator.tupleSchema;
	related functions:
		method `public List<JsonSchema> getTupleSchema()` returns `java.util.List<com.networknt.schema.JsonSchema>`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
		method `private void doWalk(ExecutionContext, Set<ValidationMessage>, int, JsonNode, JsonNode, String, boolean)` returns `void`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.ItemsValidator202012`:
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.ItemsValidator`:
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `private static boolean isEnumObjectSchema(JsonSchema)` returns `boolean`, related with `com.networknt.schema.PrefixItemsValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.