{
  "group": "findSchemaResourceRoot",
  "cases": [
    {
      "input": [],
      "expected": "the same JsonSchema instance because it is already a schema resource root",
      "description": "When the current JsonSchema instance reports isSchemaResourceRoot() == true, the method should return the instance itself without traversing to a parent."
    },
    {
      "input": [],
      "expected": "the ancestor JsonSchema instance that first returns true for isSchemaResourceRoot()",
      "description": "A hierarchy of three schemas is created: child → middle → root. Only the root returns true for isSchemaResourceRoot(). The method should walk up the chain and return the root instance."
    },
    {
      "input": [],
      "expected": "the middle JsonSchema instance because it is the first ancestor that returns true for isSchemaResourceRoot()",
      "description": "A hierarchy of three schemas is created: leaf → middle → top. Both middle and top return true for isSchemaResourceRoot(). The method must stop at the first true result (middle) and return it."
    },
    {
      "input": [],
      "expected": "java.lang.NullPointerException",
      "description": "A schema whose isSchemaResourceRoot() returns false and whose getParentSchema() returns null is used. The method will attempt to continue the loop and dereference null, resulting in a NullPointerException. This verifies the method’s behaviour when the resource‑root invariant is violated."
    }
  ]
}