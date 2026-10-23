{
  "testCases": [
    {
      "group": "$schema keyword empty string",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": \"\" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because the `$schema` value is an empty string and cannot be resolved to a meta‑schema.",
          "description": "An empty string for `$schema` provides no URI; the factory should reject it and raise an exception."
        }
      ]
    },
    {
      "group": "$schema keyword whitespace‑only",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": \"   \" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because after trimming the `$schema` value becomes empty and cannot be resolved.",
          "description": "A `$schema` that contains only whitespace should be treated as missing/invalid and cause an exception."
        }
      ]
    },
    {
      "group": "$schema keyword malformed URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": \"http://[invalid]\" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown due to URISyntaxException when parsing the malformed `$schema` URI.",
          "description": "The `$schema` value is not a syntactically valid URI; the factory must fail during meta‑schema lookup."
        }
      ]
    },
    {
      "group": "$schema keyword unsupported scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": \"ftp://json-schema.org/draft-07/schema#\" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because the scheme `ftp` is not supported for meta‑schema identification.",
          "description": "Only HTTP/HTTPS (and possibly URN) schemes are recognised; an unsupported scheme should trigger an error."
        }
      ]
    },
    {
      "group": "$schema keyword explicit null value",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": null }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because `$schema` is explicitly null, which is not a valid string URI.",
          "description": "A `$schema` keyword with a JSON null value should be rejected."
        }
      ]
    },
    {
      "group": "Schema node not an object (array root)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "[ { \"type\": \"string\" } ]"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because the root schema node is not a JSON object; meta‑schema resolution expects an object node.",
          "description": "The factory should validate that the provided schema node is an object; passing an array should cause an exception."
        }
      ]
    }
  ]
}