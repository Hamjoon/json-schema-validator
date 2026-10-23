{
  "group": "enterDynamicScope(JsonSchema) - parent exists with null containingSchema",
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
        "exceptionMessage": "parent.getContainingSchema() is null"
      },
      "description": "A parent Scope is already on the stack but its containingSchema is null. Calling enterDynamicScope with a null argument forces the method to use parent.getContainingSchema(), which is null, leading to a NullPointerException."
    },
    {
      "input": [
        {
          "parameter": "containingSchema",
          "value": "mockSchemaB"
        }
      ],
      "expected": {
        "returnParent": "non‑null Scope instance (the previously pushed scope)",
        "newTopContainingSchema": "mockSchemaB (the schema passed as argument)",
        "dynamicScopeSizeIncrease": "by 1 (previous size + 1)",
        "parentContainingSchema": "null"
      },
      "description": "A parent Scope exists with a null containingSchema. Supplying a non‑null schema overrides the null parent schema, so the method should return the existing parent, push a new Scope using the supplied schema, and increase the stack size by one."
    }
  ]
}