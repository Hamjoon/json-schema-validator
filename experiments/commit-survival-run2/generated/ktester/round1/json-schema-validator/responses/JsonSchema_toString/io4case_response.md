{
  "testCases": [
    {
      "group": "Special Characters",
      "cases": [
        {
          "input": [
            { "parameter": "schemaPath", "value": "/unicode/π" },
            { "parameter": "schemaNode", "value": "{\"type\":\"number\"}" }
          ],
          "expected": "\"/unicode/π\" : {\"type\":\"number\"}",
          "description": "Path contains a Unicode character (π). The method should embed the character unchanged inside the surrounding quotes."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/backslash\\\\test" },
            { "parameter": "schemaNode", "value": "{\"type\":\"null\"}" }
          ],
          "expected": "\"/backslash\\\\test\" : {\"type\":\"null\"}",
          "description": "Path contains a backslash. The backslash is treated as a normal character and appears doubled in the Java source string but as a single backslash in the output."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "prop'Name" },
            { "parameter": "schemaNode", "value": "{\"type\":\"boolean\"}" }
          ],
          "expected": "\"prop'Name\" : {\"type\":\"boolean\"}",
          "description": "Path contains a single‑quote character. No escaping is performed, so the quote appears verbatim."
        }
      ]
    },
    {
      "group": "Whitespace and Control Characters",
      "cases": [
        {
          "input": [
            { "parameter": "schemaPath", "value": "   /whitespace   " },
            { "parameter": "schemaNode", "value": "{\"type\":\"string\"}" }
          ],
          "expected": "\"   /whitespace   \" : {\"type\":\"string\"}",
          "description": "Path has leading and trailing spaces. Spaces are preserved inside the surrounding double quotes."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/new\nline" },
            { "parameter": "schemaNode", "value": "{\"type\":\"boolean\"}" }
          ],
          "expected": "\"/new\nline\" : {\"type\":\"boolean\"}",
          "description": "Path contains an actual newline character. The newline is part of the string that gets wrapped in quotes."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/tab\tchar" },
            { "parameter": "schemaNode", "value": "{\"type\":\"integer\"}" }
          ],
          "expected": "\"/tab\tchar\" : {\"type\":\"integer\"}",
          "description": "Path contains a tab character. The method includes the tab unchanged inside the quoted path."
        }
      ]
    },
    {
      "group": "Empty Nodes",
      "cases": [
        {
          "input": [
            { "parameter": "schemaPath", "value": "/emptyObject" },
            { "parameter": "schemaNode", "value": "{}" }
          ],
          "expected": "\"/emptyObject\" : {}",
          "description": "Schema node is an empty JSON object. Its toString() yields '{}' which is concatenated correctly."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/emptyArray" },
            { "parameter": "schemaNode", "value": "[]" }
          ],
          "expected": "\"/emptyArray\" : []",
          "description": "Schema node is an empty JSON array. The method should output '[]' after the colon."
        }
      ]
    },
    {
      "group": "Primitive Nodes",
      "cases": [
        {
          "input": [
            { "parameter": "schemaPath", "value": "/textNode" },
            { "parameter": "schemaNode", "value": "\"simple text\"" }
          ],
          "expected": "\"/textNode\" : \"simple text\"",
          "description": "Schema node is a JSON string (TextNode). Its toString() includes surrounding quotes, which are kept in the final output."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/numericNode" },
            { "parameter": "schemaNode", "value": "123" }
          ],
          "expected": "\"/numericNode\" : 123",
          "description": "Schema node is a numeric literal. The numeric value appears without quotes after the colon."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/booleanNode" },
            { "parameter": "schemaNode", "value": "true" }
          ],
          "expected": "\"/booleanNode\" : true",
          "description": "Schema node is a boolean literal. The method should output the literal 'true' unchanged."
        },
        {
          "input": [
            { "parameter": "schemaPath", "value": "/nullNodeString" },
            { "parameter": "schemaNode", "value": "null" }
          ],
          "expected": "\"/nullNodeString\" : null",
          "description": "Schema node is the JSON null literal (Jackson's NullNode). Its toString() returns the string 'null'."
        }
      ]
    },
    {
      "group": "Long Path",
      "cases": [
        {
          "input": [
            { "parameter": "schemaPath", "value": "/veryLongPath/" },
            { "parameter": "schemaNode", "value": "{\"type\":\"string\"}" }
          ],
          "expected": "\"/veryLongPath/\" : {\"type\":\"string\"}",
          "description": "Path is a relatively long string (over 100 characters). The method should handle it without truncation."
        }
      ]
    },
    {
      "group": "Literal \"null\" Path",
      "cases": [
        {
          "input": [
            { "parameter": "schemaPath", "value": "null" },
            { "parameter": "schemaNode", "value": "{\"type\":\"string\"}" }
          ],
          "expected": "\"null\" : {\"type\":\"string\"}",
          "description": "Path is the literal word \"null\" (not a Java null). The method treats it as any other string and wraps it in quotes."
        }
      ]
    }
  ]
}