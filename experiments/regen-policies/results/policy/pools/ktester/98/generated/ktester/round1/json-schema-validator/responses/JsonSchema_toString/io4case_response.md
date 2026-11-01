{
    "group": "JsonSchema.toString() - diverse data type handling",
    "cases": [
        {
            "input": [],
            "expected": "\"/description\" : \"A \\\"quoted\\\" description\"",
            "description": "Path contains regular characters while the schema node is a TextNode whose value includes escaped double‑quote characters. The toString() method should embed the raw text (including the quotes) inside the JSON string representation without additional escaping."
        },
        {
            "input": [],
            "expected": "\"/price\" : 19.99",
            "description": "Schema node is a numeric (DecimalNode) value. Verify that the numeric literal is appended correctly after the colon and space."
        },
        {
            "input": [],
            "expected": "\"/nullNode\" : null",
            "description": "Schema node is a NullNode. The method should render the literal `null` after the colon."
        },
        {
            "input": [],
            "expected": "\"/unicode/π\" : {\"type\":\"number\"}",
            "description": "Evaluation path contains a Unicode character (Greek pi). The path should be quoted verbatim and the ObjectNode representing `{ \"type\": \"number\" }` should be rendered in compact form."
        },
        {
            "input": [],
            "expected": "\"/space path/with spaces\" : true",
            "description": "Path includes spaces. Ensure that spaces are preserved inside the quoted path and that a BooleanNode is rendered as `true`."
        },
        {
            "input": [],
            "expected": "\"null\" : 42",
            "description": "The underlying evaluation path is `null`. In Java string concatenation, `null` becomes the literal string \"null\", so the result should be `\"null\" : 42` when the schema node is a numeric node with value 42."
        },
        {
            "input": [],
            "expected": "\"/escaped\\\\backslash\" : false",
            "description": "Path contains a backslash character which must be preserved in the output (resulting in `\\\\` in the JSON string). The schema node is a BooleanNode `false`."
        }
    ]
}