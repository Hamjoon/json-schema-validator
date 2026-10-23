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
Here is the code of focal method, `enterDynamicScope(JsonSchema)`, with the focal class `CollectorContext`:
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
    private final Deque<Scope> dynamicScopes = new LinkedList<>();
        public static class Scope {
        private final JsonSchema containingSchema;
    
        /**
         * Used to track which array items have been evaluated.
         */
        private final Collection<String> evaluatedItems;
    
        /**
         * Used to track which properties have been evaluated.
         */
        private final Collection<String> evaluatedProperties;
    
        private final boolean top;
    
        Scope(boolean disableUnevaluatedItems, boolean disableUnevaluatedProperties, JsonSchema containingSchema) {
            this(false, disableUnevaluatedItems, disableUnevaluatedProperties, containingSchema);
        }
    
        Scope(boolean top, boolean disableUnevaluatedItems, boolean disableUnevaluatedProperties, JsonSchema containingSchema) {
            this.top = top;
            this.containingSchema = containingSchema;
            this.evaluatedItems = newCollection(disableUnevaluatedItems);
            this.evaluatedProperties = newCollection(disableUnevaluatedProperties);
        }
    
        private static Collection<String> newCollection(boolean disabled) {
            return !disabled ? new ArrayList<>() : new AbstractCollection<String>() {
                @Override
                public boolean add(String e) {
                    return false;
                }
    
                @Override
                public Iterator<String> iterator() {
                    return Collections.emptyIterator();
                }
    
                @Override
                public boolean remove(Object o) {
                    return false;
                }
    
                @Override
                public int size() {
                    return 0;
                }
            };
        }
    
        public boolean isTop() {
            return this.top;
        }
    
        public JsonSchema getContainingSchema() {
            return this.containingSchema;
        }
    
        /**
         * Identifies which array items have been evaluated.
         *
         * @return the set of evaluated items (never null)
         */
        public Collection<String> getEvaluatedItems() {
            return this.evaluatedItems;
        }
    
        /**
         * Identifies which properties have been evaluated.
         *
         * @return the set of evaluated properties (never null)
         */
        public Collection<String> getEvaluatedProperties() {
            return this.evaluatedProperties;
        }
    
        /**
         * Merges the provided scope into this scope.
         *
         * @param scope
         * 		the scope to merge
         * @return this scope
         */
        public Scope mergeWith(Scope scope) {
            getEvaluatedItems().addAll(scope.getEvaluatedItems());
            getEvaluatedProperties().addAll(scope.getEvaluatedProperties());
            return this;
        }
    
        @Override
        public String toString() {
            return new StringBuilder("{ ").append("\"evaluatedItems\": ").append(this.evaluatedItems).append(", ").append("\"evaluatedProperties\": ").append(this.evaluatedProperties).append(" }").toString();
        }
    }
public Scope enterDynamicScope(JsonSchema containingSchema) {
        Scope parent = this.dynamicScopes.peek();
        this.dynamicScopes.push(newScope(null != containingSchema ? containingSchema : parent.getContainingSchema()));
        return parent;
    }
public <E> void add(String name, Collector<E> collector);
    public <E> void add(String name, Object object);
    public void combineWithCollector(String name, Object data);
    public Scope enterDynamicScope();
    public Scope exitDynamicScope();
    public Object get(String name);
    public Map<String, Object> getAll();
    public Scope getDynamicScope();
    public Collection<String> getEvaluatedItems();
    public Collection<String> getEvaluatedProperties();
    public JsonSchema getOutermostSchema();
    void loadCollectors();
    private Scope newScope(JsonSchema containingSchema);
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
 

api document of method enterDynamicScope(JsonSchema): 

     * Creates a new scope
     * 
     * @param containingSchema the containing schema
     * @return the previous, parent scope
     

return type: 
com.networknt.schema.CollectorContext.Scope

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	
class `com.networknt.schema.CollectorContext`:
	dependent fields:
		java.util.Deque<com.networknt.schema.CollectorContext.Scope> com.networknt.schema.CollectorContext.dynamicScopes;
	related functions:
		method `public JsonSchema getOutermostSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.CollectorContext.enterDynamicScope(JsonSchema)`;
		method `public Scope exitDynamicScope()` returns `com.networknt.schema.CollectorContext.Scope`, related with `com.networknt.schema.CollectorContext.enterDynamicScope(JsonSchema)`, api document: 
     * Restores the previous, parent scope
     * @return the exited scope
     ;
		method `public Scope getDynamicScope()` returns `com.networknt.schema.CollectorContext.Scope`, related with `com.networknt.schema.CollectorContext.enterDynamicScope(JsonSchema)`, api document: 
     * Provides the currently active scope
     * @return the active scope
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.CollectorContext.enterDynamicScope(JsonSchema)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.