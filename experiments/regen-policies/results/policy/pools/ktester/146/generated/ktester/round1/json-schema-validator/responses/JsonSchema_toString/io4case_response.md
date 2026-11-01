{
    "groups": [
        {
            "group": "Additional standard behavior",
            "cases": [
                {
                    "input": [],
                    "expected": "\"/array[10]/item\" : 42",
                    "description": "When the evaluation path points to a deep array element and the schema node is a numeric JsonNode (42), toString() should embed the numeric value without extra quotes."
                },
                {
                    "input": [],
                    "expected": "\"/unicode/☃\" : true",
                    "description": "When the evaluation path contains a Unicode character and the schema node is a Boolean JsonNode (true), the method should correctly render the path and the boolean literal."
                },
                {
                    "input": [],
                    "expected": "\"\" : {}",
                    "description": "Root evaluation path (empty string) with an empty ObjectNode should produce an empty JSON object after the colon."
                },
                {
                    "input": [],
                    "expected": "\"/emptyArray\" : []",
                    "description": "A non‑root path with an empty ArrayNode should be rendered as an empty JSON array."
                },
                {
                    "input": [],
                    "expected": "\"/nested/object\" : {\"inner\":{\"type\":\"integer\"}}",
                    "description": "A nested object path with a schema node that itself contains a nested object should preserve the full JSON structure in the output."
                },
                {
                    "input": [],
                    "expected": "\"/quote\\\"test\" : \"value\"",
                    "description": "Path containing a double‑quote character and a TextNode schema node. The method must keep the outer quoting while correctly escaping the inner quote."
                },
                {
                    "input": [],
                    "expected": "\"/space test\" : null",
                    "description": "Path with a space character and a NullNode schema node. The output should display the literal JSON null."
                },
                {
                    "input": [],
                    "expected": "\"/largePath/segment1/segment2/segment3/segment4\" : {\"type\":\"object\"}",
                    "description": "Very long evaluation path combined with a simple object schema node, verifying that concatenation works for long strings."
                }
            ]
        },
        {
            "group": "Additional edge cases",
            "cases": [
                {
                    "input": [],
                    "expected": "\"null\" : null",
                    "description": "Both getEvaluationPath() and getSchemaNode() return null. The method concatenates the string \"null\" for the path and then calls toString() on a null node, which results in a NullPointerException. This case captures the double‑null scenario and expects the same NPE as when only the schema node is null."
                },
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "When getEvaluationPath() returns a non‑null value but getSchemaNode() returns null, invoking toString() on the null node throws NullPointerException. This mirrors the existing edge case but isolates the null schema node."
                },
                {
                    "input": [],
                    "expected": "\"/weird/\\n\\t\" : {\"type\":\"string\"}",
                    "description": "Evaluation path contains escaped characters (newline and tab). The method should include the raw characters inside the quoted path, producing a string with visible escape sequences."
                }
            ]
        }
    ]
}