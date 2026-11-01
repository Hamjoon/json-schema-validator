{
  "group": "findLexicalRoot - basic scenarios",
  "cases": [
    {
      "input": [],
      "expected": "returns the same JsonSchema instance because it already has a non‑null id",
      "description": "The schema itself has an id (getId() != null). The while loop condition fails immediately, so the method should return the original instance."
    },
    {
      "input": [],
      "expected": "returns the original JsonSchema instance because it has no id and no parent (getParentSchema() returns null)",
      "description": "The schema has a null id and getParentSchema() returns null. The loop enters once, hits the break statement, and returns the original instance."
    },
    {
      "input": [],
      "expected": "returns the top‑most ancestor that possesses a non‑null id",
      "description": "A chain of three schemas is created: child → parent → root. Only the root schema has a non‑null id. The method should walk up the chain and return the root schema."
    },
    {
      "input": [],
      "expected": "returns the highest ancestor in the chain (the one whose parent is null) because none of the ancestors have an id",
      "description": "A chain of three schemas is created where every schema's getId() returns null. The loop will traverse until getParentSchema() is null, then break and return that highest ancestor."
    },
    {
      "input": [],
      "expected": "returns the first ancestor (not necessarily the top) that has a non‑null id",
      "description": "A chain of four schemas is created: child → level1 → level2 → root. level2 has a non‑null id, while root's id is null. The method should stop at level2 and return it."
    }
  ]
}