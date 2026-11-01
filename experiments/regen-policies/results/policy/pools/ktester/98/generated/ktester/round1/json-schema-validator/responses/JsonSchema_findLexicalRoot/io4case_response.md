{
  "tests": [
    {
      "group": "findLexicalRoot - immediate parent has id",
      "cases": [
        {
          "input": [],
          "expected": "returns the immediate parent schema because the child has a null id and the parent’s id is non‑null",
          "description": "Create a child JsonSchema with getId() == null and set its parent to a schema whose getId() returns a non‑null value. The method should stop at the first ancestor with a non‑null id and return that parent."
        }
      ]
    },
    {
      "group": "findLexicalRoot - empty string id is treated as non‑null",
      "cases": [
        {
          "input": [],
          "expected": "returns the original JsonSchema instance because its id is an empty string (non‑null)",
          "description": "Instantiate a JsonSchema whose getId() returns \"\" (empty string). The while condition checks for null only, so the method should not enter the loop and should return the same instance."
        }
      ]
    },
    {
      "group": "findLexicalRoot - first encountered id in a deep chain",
      "cases": [
        {
          "input": [],
          "expected": "returns the first ancestor (level2) that has a non‑null id, even though higher ancestors also have ids",
          "description": "Build a chain of five schemas: child → level1 → level2 → level3 → root. level2.getId() returns \"id2\", level3.getId() returns \"id3\", root.getId() returns null, and the lower two have null ids. The method should walk up until it finds level2 and return it."
        }
      ]
    },
    {
      "group": "findLexicalRoot - self‑referential parent with non‑null id",
      "cases": [
        {
          "input": [],
          "expected": "returns the original instance because its id is non‑null, avoiding an infinite loop despite getParentSchema() returning itself",
          "description": "Create a JsonSchema whose getParentSchema() returns the same instance (self‑reference) and whose getId() returns a non‑null value. Since the loop condition fails immediately, the method returns the original instance safely."
        }
      ]
    },
    {
      "group": "findLexicalRoot - chain where only middle ancestor has id and others are null",
      "cases": [
        {
          "input": [],
          "expected": "returns the middle ancestor that possesses the non‑null id",
          "description": "Construct a three‑level chain: child (id null) → middle (id \"mid\") → top (id null). The method should stop at the middle schema because it is the first ancestor with a non‑null id."
        }
      ]
    }
  ]
}