{
  "testCases": [
    {
      "group": "Required validator remains null for schema without required keyword",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Create a JsonSchema from a simple schema that does not contain the `required` keyword (e.g., `{ \"type\": \"object\" }`). After invoking `initializeValidators()` the private field `requiredValidator` should stay `null`. Calling `getRequiredValidator()` must therefore return `null`."
        }
      ]
    },
    {
      "group": "Required validator created for schema with empty required array",
      "cases": [
        {
          "input": [],
          "expected": "instance of com.networknt.schema.RequiredValidator (non‑null)",
          "description": "Build a JsonSchema from a schema that contains an empty `required` array (`{ \"type\": \"object\", \"required\": [] }`). Even though the array is empty, `initializeValidators()` should instantiate a `RequiredValidator` and store it in `requiredValidator`. `getRequiredValidator()` must return a non‑null object whose class is `RequiredValidator`."
        }
      ]
    },
    {
      "group": "Idempotent getRequiredValidator call",
      "cases": [
        {
          "input": [],
          "expected": "same instance on successive calls",
          "description": "After creating a JsonSchema with a `required` keyword and calling `initializeValidators()`, invoke `getRequiredValidator()` twice. Both calls must return the exact same object reference, proving that the getter does not create a new instance on each call."
        }
      ]
    },
    {
      "group": "Child schema without its own required keyword does not inherit parent's validator",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Create a parent JsonSchema from a schema containing a `required` keyword, then call `initializeValidators()` on the parent. Use `createChildSchema()` to produce a child schema from a node that lacks a `required` keyword. After initializing validators on the child (if any), `child.getRequiredValidator()` must return `null`, confirming that the required validator is not automatically inherited from the parent."
        }
      ]
    },
    {
      "group": "Re‑initialising validators does not clear required validator",
      "cases": [
        {
          "input": [],
          "expected": "non‑null (still a RequiredValidator instance)",
          "description": "For a JsonSchema built from a schema with a `required` keyword, call `initializeValidators()` twice in succession. After the second call, `getRequiredValidator()` must still return a non‑null `RequiredValidator` (the same or a newly created instance, but never `null`). This verifies that repeated initialisation does not unintentionally reset the field."
        }
      ]
    }
  ]
}