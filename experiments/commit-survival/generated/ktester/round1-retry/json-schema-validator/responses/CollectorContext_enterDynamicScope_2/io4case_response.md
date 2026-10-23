{
  "tests": [
    {
      "group": "enterDynamicScope(JsonSchema) - without existing parent scope",
      "cases": [
        {
          "input": [
            {
              "parameter": "containingSchema",
              "value": "null"
            }
          ],
          "expected": {
            "exception": "java.lang.NullPointerException",
            "exceptionMessage": "parent is null and containingSchema is null"
          },
          "description": "When there is no previously pushed scope (dynamicScopes is empty) and the argument is null, the method attempts to read the containingSchema from the non‑existent parent, resulting in a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "containingSchema",
              "value": "mockSchema"
            }
          ],
          "expected": {
            "returnParent": "null",
            "newTopContainingSchema": "mockSchema (the schema passed as argument)",
            "dynamicScopeSizeIncrease": "from 0 to 1"
          },
          "description": "When there is no existing parent scope and a non‑null schema is supplied, the method should return null (no parent) and push a new scope whose containingSchema equals the supplied schema. The dynamicScopes deque size should increase from 0 to 1."
        }
      ]
    },
    {
      "group": "enterDynamicScope(JsonSchema) - after previous push verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "containingSchema",
              "value": "mockSchemaA"
            }
          ],
          "expected": {
            "returnParent": "null",
            "newTopContainingSchema": "mockSchemaA",
            "dynamicScopeSizeIncrease": "from 0 to 1",
            "peekedScopeContainingSchema": "mockSchemaA"
          },
          "description": "Push the first scope with a non‑null schema and verify that getDynamicScope() (or peek) returns a Scope whose containingSchema matches the argument."
        },
        {
          "input": [
            {
              "parameter": "containingSchema",
              "value": "null"
            }
          ],
          "expected": {
            "returnParent": "non‑null Scope instance (the previously pushed scope)",
            "newTopContainingSchema": "mockSchemaA (inherited from parent)",
            "dynamicScopeSizeIncrease": "by 1 (previous size + 1)",
            "peekedScopeContainingSchema": "mockSchemaA"
          },
          "description": "After a first scope exists, call enterDynamicScope with null. The method should return the existing parent scope, push a new scope that inherits the parent’s containingSchema, and the top of the stack should still reference the original schema."
        }
      ]
    }
  ]
}