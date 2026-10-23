{
  "testCases": [
    {
      "group": "hasTypeValidator - typeValidator is non-null after schema with type",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "Create a JsonSchema from a JSON schema node that contains a \"type\" keyword (e.g., {\"type\":\"string\"}). After calling initializeValidators() the private field `typeValidator` is instantiated, therefore hasTypeValidator() should return true."
        }
      ]
    },
    {
      "group": "hasTypeValidator - typeValidator becomes null after reflection reset",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "Start with a JsonSchema that has a non‑null `typeValidator` (schema with a \"type\" keyword). Then, using reflection, set the private field `typeValidator` to null. The subsequent call to hasTypeValidator() must return false, confirming the method reflects the current field state."
        }
      ]
    },
    {
      "group": "hasTypeValidator - typeValidator remains null for schema without type",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "Create a JsonSchema from a schema node that does NOT contain a \"type\" keyword (e.g., an empty object or only \"properties\"). After initializeValidators(), `typeValidator` stays null, so hasTypeValidator() returns false."
        }
      ]
    },
    {
      "group": "hasTypeValidator - typeValidator true after child schema inherits type",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "Create a parent JsonSchema with a \"type\" keyword, then call createChildSchema() to obtain a child schema that inherits the same type definition. The child's `typeValidator` is populated, so hasTypeValidator() on the child returns true."
        }
      ]
    }
  ]
}