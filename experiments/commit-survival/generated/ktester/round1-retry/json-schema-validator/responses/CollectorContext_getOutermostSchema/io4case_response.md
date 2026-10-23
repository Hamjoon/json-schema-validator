{
  "group": "getOutermostSchema_additional",
  "cases": [
    {
      "input": [],
      "expected": "lexicalRootSchema",
      "description": "Lexical root is **not** a dynamic anchor, while a deeper dynamic scope contains a non‑null schema that *is* a dynamic anchor. The method must ignore the dynamic scopes and return the lexical root schema obtained from `context.findLexicalRoot()`."
    },
    {
      "input": [],
      "expected": "mostRecentDynamicAnchorSchema",
      "description": "Lexical root is a dynamic anchor. The dynamic scope stack contains (from oldest to newest): a `null` containing schema, a non‑null schema that is **not** a dynamic anchor, and finally a non‑null schema that *is* a dynamic anchor. The method should iterate from the newest scope and return the first schema that is a dynamic anchor – i.e., the most recent one."
    },
    {
      "input": [],
      "expected": "lexicalRootSchema",
      "description": "Lexical root is a dynamic anchor, but the dynamic scope stack contains only non‑dynamic‑anchor schemas (or null schemas). The method must fall back to returning the lexical root schema."
    },
    {
      "input": [],
      "expected": "newestDynamicAnchorSchema",
      "description": "Lexical root is a dynamic anchor and the dynamic scope stack contains **multiple** non‑null schemas that are dynamic anchors. The method should return the *newest* (most recent) dynamic anchor schema, not an older one."
    }
  ]
}