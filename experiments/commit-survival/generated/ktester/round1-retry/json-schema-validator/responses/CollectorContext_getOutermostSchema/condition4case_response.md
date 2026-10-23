{
  "group": "getOutermostSchema",
  "cases": [
    {
      "input": [],
      "expected": "IllegalStateException",
      "description": "When the current dynamic scope's containing schema is null, the method must throw IllegalStateException (\"Missing a root schema in the dynamic scope.\")."
    },
    {
      "input": [],
      "expected": "lexicalRootSchema",
      "description": "Lexical root is not a dynamic anchor. The method should return the lexical root schema returned by `context.findLexicalRoot()`."
    },
    {
      "input": [],
      "expected": "innerDynamicAnchorSchema",
      "description": "Lexical root is a dynamic anchor, but a deeper dynamic scope (the most recent one in the deque) contains a non‑null schema that is also a dynamic anchor. The method must return that deeper schema."
    },
    {
      "input": [],
      "expected": "lexicalRootSchema",
      "description": "Lexical root is a dynamic anchor, yet none of the dynamic scopes contain a non‑null schema that is a dynamic anchor (either all are null or `isDynamicAnchor()` returns false). The method should fall back to returning the lexical root schema."
    }
  ]
}