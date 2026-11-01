{
  "group": "findLexicalRoot additional scenarios",
  "cases": [
    {
      "input": [],
      "expected": "returns the same JsonSchema instance because getId() returns an empty string (non‑null), so the while loop is never entered",
      "description": "Schema's id is \"\" (empty string). The method treats empty string as a valid id, does not iterate, and returns the original instance."
    },
    {
      "input": [],
      "expected": "returns the same JsonSchema instance because getId() returns a whitespace string (non‑null), loop never entered",
      "description": "Schema's id is \"   \". Whitespace is still a non‑null value, therefore the method returns the original schema without climbing the parent chain."
    },
    {
      "input": [],
      "expected": "returns the direct parent JsonSchema instance even though both child and parent ids are null, because parent has no further parent",
      "description": "Child schema id = null, parent schema id = null, parent.getParentSchema() = null. The loop runs once, moves to the parent, then breaks on null parent and returns that parent instance."
    },
    {
      "input": [],
      "expected": "returns the immediate parent JsonSchema instance because it is the closest ancestor with a non‑null id, even though a higher ancestor also has an id",
      "description": "Child id = null, parent id = \"#parent\", grand‑parent id = \"#grandparent\". The loop stops after the first iteration when it reaches the parent whose id is non‑null, so the method returns the parent, not the grand‑parent."
    },
    {
      "input": [],
      "expected": "returns the top‑most ancestor (great‑grand‑parent) whose id is \"#root\", after climbing through three intermediate ancestors with null ids",
      "description": "Chain: child (id null) → level1 (id null) → level2 (id null) → level3 (id null) → great‑grand‑parent (id \"#root\"). The method iterates four times, moving up the chain until it finds the first non‑null id, and returns that great‑grand‑parent instance."
    }
  ]
}