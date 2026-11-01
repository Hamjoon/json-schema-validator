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
Here is the code of focal method, `validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, with the focal class `JsonSchema`:
```Java
package com.networknt.schema;
import com.networknt.schema.utils.SetView;
import java.util.Set;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import com.networknt.schema.i18n.MessageSource;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import com.networknt.schema.utils.JsonNodes;
import java.util.Collections;
public class JsonSchema extends BaseJsonValidator {
    
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
        return validate(createExecutionContext(), deserialize(input, inputFormat), format, executionCustomizer);
    }
public ExecutionContext createExecutionContext();
    private JsonNode deserialize(String input, InputFormat inputFormat);
    public JsonSchema findAncestor();
    public JsonSchema findLexicalRoot();
    public JsonSchema findSchemaResourceRoot();
    static JsonSchema from(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval);
    public JsonSchema fromRef(JsonSchema refEvaluationParentSchema, JsonNodePath refEvaluationPath);
    public String getId();
    protected JsonNode getNode(JsonNode node, Object propertyOrIndex);
    protected JsonNode getNode(Object propertyOrIndex);
    public JsonSchema getRefSchema(JsonNodePath fragment);
    public JsonNode getRefSchemaNode(String ref);
    public JsonSchema getSubSchema(JsonNodePath fragment);
    public TypeValidator getTypeValidator();
    public ValidationContext getValidationContext();
    public List<JsonValidator> getValidators();
    public boolean hasTypeValidator();
    public void initializeValidators();
    public boolean isRecursiveAnchor();
    public boolean isSchemaResourceRoot();
    @Override
public String toString();
    public Set<ValidationMessage> validate(JsonNode rootNode);
    public Set<ValidationMessage> validate(JsonNode rootNode, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(JsonNode rootNode, Consumer<ExecutionContext> executionCustomizer);
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation);
    public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format);
    public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, Consumer<ExecutionContext> executionCustomizer);
    public ValidationResult walk(JsonNode node, boolean validate);
    public <T> T walk(JsonNode node, OutputFormat<T> outputFormat, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean validate, ExecutionContextCustomizer executionCustomizer);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean validate, Consumer<ExecutionContext> executionCustomizer);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema);
    public <T> T walk(ExecutionContext executionContext, JsonNode node, OutputFormat<T> outputFormat, boolean validate, ExecutionContextCustomizer executionCustomizer);
    public <T> T walk(ExecutionContext executionContext, JsonNode node, OutputFormat<T> outputFormat, boolean validate, Consumer<ExecutionContext> executionCustomizer);
    public ValidationResult walk(ExecutionContext executionContext, String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, String input, InputFormat inputFormat, boolean validate, ExecutionContextCustomizer executionCustomizer);
    public <T> T walk(ExecutionContext executionContext, String input, InputFormat inputFormat, OutputFormat<T> outputFormat, boolean validate);
    public <T> T walk(ExecutionContext executionContext, String input, InputFormat inputFormat, OutputFormat<T> outputFormat, boolean validate, ExecutionContextCustomizer executionCustomizer);
    public ValidationResult walk(String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walk(String input, InputFormat inputFormat, boolean validate, ExecutionContextCustomizer executionCustomizer);
    public ValidationResult walk(String input, InputFormat inputFormat, boolean validate, Consumer<ExecutionContext> executionCustomizer);
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

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

api document of method validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer): 

     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     

return type: 
T

dependent classes: 
class `com.networknt.schema.InputFormat`:
	
class `com.networknt.schema.ExecutionContextCustomizer`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
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
		method `public T validate(ExecutionContext, JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validates to a format.
     * 
     * @param <T>              the result type
     * @param executionContext the execution context
     * @param node             the node
     * @param format           the format
     * @return the result
     ;
		method `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validate the given input string using the input format, starting at the root
     * of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param input       the input
     * @param inputFormat the inputFormat
     * @return A list of ValidationMessage if there is any validation error, or an
     *         empty list if there is no error.
     ;
		method `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
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
		method `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.