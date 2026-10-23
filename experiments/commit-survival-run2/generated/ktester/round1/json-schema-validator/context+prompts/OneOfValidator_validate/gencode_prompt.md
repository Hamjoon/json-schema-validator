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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `OneOfValidator`:
```Java
package com.networknt.schema;
import java.util.*;
import com.networknt.schema.CollectorContext.Scope;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class OneOfValidator extends BaseJsonValidator {
    private static final Logger logger = LoggerFactory.getLogger(OneOfValidator.class);
    private final List<JsonSchema> schemas = new ArrayList<>();
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        CollectorContext collectorContext = executionContext.getCollectorContext();
        Scope grandParentScope = collectorContext.enterDynamicScope();
        try {
            debug(logger, node, rootNode, at);
            ValidatorState state = ((ValidatorState) (collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)));
            // this is a complex validator, we set the flag to true
            state.setComplexValidator(true);
            int numberOfValidSchema = 0;
            Set<ValidationMessage> childErrors = new LinkedHashSet<>();
            for (JsonSchema schema : this.schemas) {
                Set<ValidationMessage> schemaErrors = Collections.emptySet();
                Scope parentScope = collectorContext.enterDynamicScope();
                try {
                    // Reset state in case the previous validator did not match
                    state.setMatchedNode(true);
                    if (!state.isWalkEnabled()) {
                        schemaErrors = schema.validate(executionContext, node, rootNode, at);
                    } else {
                        schemaErrors = schema.walk(executionContext, node, rootNode, at, state.isValidationEnabled());
                    }
                    // check if any validation errors have occurred
                    if (schemaErrors.isEmpty()) {
                        // check whether there are no errors HOWEVER we have validated the exact validator
                        if (!state.hasMatchedNode())
                            continue;
    
                        numberOfValidSchema++;
                    }
                    if (numberOfValidSchema > 1) {
                        // short-circuit
                        break;
                    }
                    childErrors.addAll(schemaErrors);
                } finally {
                    Scope scope = collectorContext.exitDynamicScope();
                    if (schemaErrors.isEmpty()) {
                        parentScope.mergeWith(scope);
                    }
                }
            }
            // ensure there is always an "OneOf" error reported if number of valid schemas is not equal to 1.
            if (numberOfValidSchema != 1) {
                ValidationMessage message = buildValidationMessage(at, Integer.toString(numberOfValidSchema));
                if (this.failFast) {
                    throw new JsonSchemaException(message);
                }
                errors.add(message);
                errors.addAll(childErrors);
                collectorContext.getEvaluatedItems().clear();
                collectorContext.getEvaluatedProperties().clear();
            }
            // Make sure to signal parent handlers we matched
            if (errors.isEmpty())
                state.setMatchedNode(true);
    
            // reset the ValidatorState object
            resetValidatorState(collectorContext);
            return Collections.unmodifiableSet(errors);
        } finally {
            Scope scope = collectorContext.exitDynamicScope();
            if (errors.isEmpty()) {
                grandParentScope.mergeWith(scope);
            }
        }
    }
@Override
public void preloadJsonSchema();
    private static void resetValidatorState(CollectorContext collectorContext);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
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
	
class `com.networknt.schema.CollectorContext`:
	
class `com.networknt.schema.ValidatorState`:
	dependent fields:
		java.lang.String com.networknt.schema.ValidatorState.VALIDATOR_STATE_KEY;
	
class `com.networknt.schema.OneOfValidator`:
	dependent fields:
		org.slf4j.Logger com.networknt.schema.OneOfValidator.logger;
		java.util.List<com.networknt.schema.JsonSchema> com.networknt.schema.OneOfValidator.schemas;
	related functions:
		method `private static void resetValidatorState(CollectorContext)` returns `void`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.ValidationMessageHandler`:
	dependent fields:
		boolean com.networknt.schema.ValidationMessageHandler.failFast;
	
class `com.networknt.schema.AnyOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.AdditionalPropertiesValidator`:
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.IfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.NotValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.ItemsValidator202012`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.