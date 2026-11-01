{
  "group": "isSchemaResourceRoot",
  "cases": [
    {
      "input": [],
      "expected": true,
      "description": "When the schema has a non‑null id, the method should return true without checking parent schema or locations."
    },
    {
      "input": [],
      "expected": true,
      "description": "When the id is null and the schema has no parent (root schema), the method should return true."
    },
    {
      "input": [],
      "expected": false,
      "description": "When the id is null, a parent schema exists, and both the schema and its parent have the same absolute IRI, the method should return false (the schema is not a resource root)."
    },
    {
      "input": [],
      "expected": true,
      "description": "When the id is null, a parent schema exists, and the absolute IRIs differ, the method should return true (the schema is a resource root)."
    }
  ]
}