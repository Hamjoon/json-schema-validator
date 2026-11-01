{
  "group": "JsonSchema.toString() - basic concatenation scenarios",
  "cases": [
    {
      "input": [],
      "expected": "\"/properties/name\" : {\"type\":\"string\"}",
      "description": "Typical case where the evaluation path is a non‑empty JSON Pointer and the schema node is an ObjectNode representing `{ \"type\": \"string\" }`. The toString() method should wrap the path in quotes, add a colon and space, then append the node's JSON representation."
    },
    {
      "input": [],
      "expected": "\"\" : true",
      "description": "Edge case with an empty evaluation path (root schema) and a simple Boolean schema node (`BooleanNode.TRUE`). The method must still produce a pair of quotes for the empty path and correctly serialize the Boolean value."
    },
    {
      "input": [],
      "expected": "\"/items[0]/$ref\" : [1,2,3]",
      "description": "Path containing special characters (`[`, `]`, `$`) and an ArrayNode schema node. Verifies that the method does not escape the path content and that the array is rendered in compact JSON form."
    },
    {
      "input": [],
      "expected": "java.lang.NullPointerException",
      "description": "Defensive test: when the underlying schema node is `null` (which should not happen in normal usage), the call to `getSchemaNode().toString()` throws a NullPointerException. The test asserts that the exception is propagated."
    }
  ]
}