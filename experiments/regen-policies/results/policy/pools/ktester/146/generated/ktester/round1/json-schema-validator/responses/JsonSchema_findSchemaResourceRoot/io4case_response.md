{
  "group": "findSchemaResourceRoot_additional",
  "cases": [
    {
      "input": [],
      "expected": "the topmost JsonSchema instance (level 5) because it is the first ancestor for which isSchemaResourceRoot() returns true",
      "description": "A hierarchy of five schemas is created (level1 → level2 → level3 → level4 → level5). Only level5 returns true for isSchemaResourceRoot(). The method should traverse up through four false ancestors and return level5."
    },
    {
      "input": [],
      "expected": "the immediate parent JsonSchema instance because it is the first ancestor that returns true for isSchemaResourceRoot()",
      "description": "A two‑level hierarchy is built: child (isSchemaResourceRoot() == false) → parent (isSchemaResourceRoot() == true). The method should stop after one iteration and return the parent."
    },
    {
      "input": [],
      "expected": "the middle JsonSchema instance (level3) because it is the first ancestor that returns true for isSchemaResourceRoot()",
      "description": "A hierarchy of four schemas is created: leaf (false) → level2 (false) → level3 (true) → top (true). The method must return level3, demonstrating that it stops at the first true result even when higher ancestors also return true."
    },
    {
      "input": [],
      "expected": "java.lang.StackOverflowError",
      "description": "A cyclic schema chain is constructed where a schema's getParentSchema() returns itself, creating an infinite loop. The method will keep traversing until the JVM stack overflows, confirming that cyclic parent references are not safely handled."
    }
  ]
}