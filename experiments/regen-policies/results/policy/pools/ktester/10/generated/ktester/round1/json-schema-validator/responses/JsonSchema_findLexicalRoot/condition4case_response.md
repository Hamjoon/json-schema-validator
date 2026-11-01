{
  "group": "findLexicalRoot scenarios",
  "cases": [
    {
      "input": [],
      "expected": "returns the same JsonSchema instance because getId() is non‑null, loop never entered",
      "description": "Current schema has an id (e.g., \"#root\"). The while condition fails immediately, so the method should return the original object."
    },
    {
      "input": [],
      "expected": "returns the same JsonSchema instance because getId() is null and getParentSchema() is null, loop breaks on first iteration",
      "description": "Schema with no id and no parent. The while loop starts, detects a null parent, breaks, and returns the original instance."
    },
    {
      "input": [],
      "expected": "returns the direct parent JsonSchema instance because parent has a non‑null id",
      "description": "Current schema id is null, parent schema exists and its getId() returns a value (e.g., \"#parent\"). The loop iterates once, sets ancestor to the parent, then exits because parent id is non‑null."
    },
    {
      "input": [],
      "expected": "returns the grand‑parent JsonSchema instance because both child and parent ids are null but grand‑parent id is non‑null",
      "description": "Chain: child (id null) → parent (id null) → grandparent (id \"#grand\"). The loop iterates twice, moving up the hierarchy until it reaches the grand‑parent whose id is non‑null, then returns it."
    },
    {
      "input": [],
      "expected": "returns the topmost ancestor (with null parent) because all ancestors have null ids and the chain ends with a null parent",
      "description": "A chain of three schemas where each getId() returns null and the topmost schema's getParentSchema() returns null. The loop walks up until the parent is null, breaks, and returns that topmost schema."
    }
  ]
}