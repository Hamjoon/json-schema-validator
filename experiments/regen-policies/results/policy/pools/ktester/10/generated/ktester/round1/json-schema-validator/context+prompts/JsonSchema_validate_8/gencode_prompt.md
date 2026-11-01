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
Here is the code of focal method, `validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, with the focal class `JsonSchema`:
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
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
        return validate(createExecutionContext(), rootNode, format, executionCustomizer);
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
    @Override
public String toString();
    public Set<ValidationMessage> validate(JsonNode rootNode);
    public Set<ValidationMessage> validate(JsonNode rootNode, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(JsonNode rootNode, Consumer<ExecutionContext> executionCustomizer);
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation);
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

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

api document of method validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer): 

     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param rootNode            the root node
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     

return type: 
T

dependent classes: 
class `com.networknt.schema.ExecutionContextCustomizer`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult walk(JsonNode, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Walk the JSON node.
     * 
     * @param node     the input
     * @param validate true to validate the input against the schema
     * @return the validation result
     ;
		method `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param rootNode            the root node
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param rootNode            the root node
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public ValidationResult validateAndCollect(JsonNode)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `public T validate(ExecutionContext, JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validates to a format.
     * 
     * @param <T>              the result type
     * @param executionContext the execution context
     * @param node             the node
     * @param format           the format
     * @return the result
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.