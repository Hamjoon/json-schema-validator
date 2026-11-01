{
  "group": "findLexicalRoot",
  "cases": [
    {
      "input": [],
      "expected": "the same JsonSchema instance that was called (its getId() is non‑null)",
      "description": "When the schema itself already has an ID, the while‑loop condition fails immediately and the method should return the original instance."
    },
    {
      "input": [],
      "expected": "the same JsonSchema instance that was called (its getId() is null and it has no parent)",
      "description": "The schema has a null ID and no parent schema. The loop enters, detects that getParentSchema() returns null, breaks, and returns the original instance."
    },
    {
      "input": [],
      "expected": "the direct parent JsonSchema instance (parent has a non‑null ID)",
      "description": "The schema’s ID is null but it has a parent whose ID is non‑null. The loop iterates once, moves to the parent, sees a non‑null ID and exits, returning that parent."
    },
    {
      "input": [],
      "expected": "the grand‑parent JsonSchema instance (grand‑parent has a non‑null ID)",
      "description": "The schema’s ID is null, its parent’s ID is also null, but the grand‑parent’s ID is non‑null. The loop iterates twice, climbing the hierarchy until it finds the first ancestor with an ID, which should be returned."
    },
    {
      "input": [],
      "expected": "the top‑most JsonSchema instance in the chain (all IDs are null, top‑most has no parent)",
      "description": "All schemas in the chain have null IDs. The loop climbs until it reaches the root schema whose getParentSchema() returns null, then breaks and returns that root schema."
    }
  ]
}