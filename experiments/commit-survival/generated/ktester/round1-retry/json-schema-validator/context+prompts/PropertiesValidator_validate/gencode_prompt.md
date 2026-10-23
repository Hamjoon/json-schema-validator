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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `PropertiesValidator`:
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
	
class `com.networknt.schema.PropertiesValidator`:
	dependent fields:
		java.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;
		org.slf4j.Logger com.networknt.schema.PropertiesValidator.logger;
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
		method `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.ValidatorState`:
	dependent fields:
		java.lang.String com.networknt.schema.ValidatorState.VALIDATOR_STATE_KEY;
	
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;
	
class `com.networknt.schema.OneOfValidator`:
	related functions:
		method `private static void resetValidatorState(CollectorContext)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.PatternPropertiesValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.AdditionalPropertiesValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.AnyOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.