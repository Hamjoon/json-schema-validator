{
  "group": "isSchemaResourceRoot_edge_cases",
  "cases": [
    {
      "input": [],
      "expected": true,
      "description": "When the schema id is an empty string (non‑null), the method should return true, treating it as a defined id."
    },
    {
      "input": [],
      "expected": false,
      "description": "When id is null, a parent schema exists, and both the schema and its parent have a null SchemaLocation, Objects.equals(null, null) is true, so the method returns false."
    },
    {
      "input": [],
      "expected": true,
      "description": "When id is null, a parent schema exists, the schema's SchemaLocation is null while the parent's SchemaLocation has a non‑null absolute IRI, Objects.equals(null, nonNull) is false, thus the method returns true."
    },
    {
      "input": [],
      "expected": true,
      "description": "When id is null, a parent schema exists, the schema's SchemaLocation has a non‑null absolute IRI but the parent's SchemaLocation is null, Objects.equals(nonNull, null) is false, so the method returns true."
    }
  ]
}