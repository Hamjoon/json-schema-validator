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
Here is the code of focal method, `add(String, Collector<E>)`, with the focal class `CollectorContext`:
```Java
package com.networknt.schema;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Deque;
import java.util.Map;
import java.util.Collections;
public class CollectorContext {
    private Map<String, Object> collectorMap = new HashMap<>();
    public <E> void add(String name, Collector<E> collector) {
        this.collectorMap.put(name, collector);
    }
public <E> void add(String name, Object object);
    public void combineWithCollector(String name, Object data);
    public Scope enterDynamicScope();
    public Scope enterDynamicScope(JsonSchema containingSchema);
    public Scope exitDynamicScope();
    public Object get(String name);
    public Map<String, Object> getAll();
    public Scope getDynamicScope();
    public Collection<String> getEvaluatedItems();
    public Collection<String> getEvaluatedProperties();
    public JsonSchema getOutermostSchema();
    void loadCollectors();
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

api document of class com.networknt.schema.CollectorContext: 

 * Context for holding the output returned by the {@link Collector}
 * implementations.
 

api document of method add(String, Collector<E>): 

     * Adds a collector with give name. Preserving this method for backward
     * compatibility.
     *
     * @param <E>       element
     * @param name      String
     * @param collector Collector
     

dependent classes: 
class `com.networknt.schema.CollectorContext`:
	dependent fields:
		java.util.Map<java.lang.String, java.lang.Object> com.networknt.schema.CollectorContext.collectorMap;
	related functions:
		method `public void combineWithCollector(String, Object)` returns `void`, related with `com.networknt.schema.CollectorContext.add(String, Collector<E>)`, api document: 
     * Combines data with Collector identified by the given name.
     *
     * @param name String
     * @param data Object
     ;
		method ` void loadCollectors()` returns `void`, related with `com.networknt.schema.CollectorContext.add(String, Collector<E>)`, api document: 
     * Loads data from all collectors.
     ;
class `com.networknt.schema.JsonSchemaFactory.Builder`:
	related functions:
		method `public Builder uriFactory(URIFactory, Iterable<String>)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.CollectorContext.add(String, Collector<E>)`;
class `com.networknt.schema.SchemaValidatorsConfig`:
	related functions:
		method `public void setStrict(String, boolean)` returns `void`, related with `com.networknt.schema.CollectorContext.add(String, Collector<E>)`, api document: 
     * Alters the strictness of validations for a specific keyword. When set to
     * {@literal true}, instructs the keyword's validators to perform strict
     * validation. Otherwise, a validator may perform a more permissive check.
     * 
     * @param keyword The keyword to adjust (not null)
     * @param strict Whether to perform strict validations
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.