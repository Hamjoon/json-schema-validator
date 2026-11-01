{
  "testCases": [
    {
      "group": "Successful validation with validate=true",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat (captures ValidationResult as a List<String>)"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "OutputFormat returns a List containing no validation messages (empty list) because the instance node matches the schema and validation is performed.",
          "description": "A valid JSON object is supplied, validation is enabled and a non‑null OutputFormat is used. The method should walk the schema, perform validation and return an empty result set."
        }
      ]
    },
    {
      "group": "Validation disabled (validate=false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "OutputFormat returns a List containing no validation messages (empty list) because validation is skipped.",
          "description": "Even though the instance node could be invalid, the `validate` flag is false, so the walk method must skip validation and still return a result via the OutputFormat."
        }
      ]
    },
    {
      "group": "Invalid instance node – validation error produced",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"not-an-integer\""
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "OutputFormat returns a List containing at least one ValidationMessage indicating type mismatch (expected integer, found string).",
          "description": "The schema expects an integer, but a string is supplied. With validation enabled, the walk method must detect the type error and propagate it through the OutputFormat."
        }
      ]
    },
    {
      "group": "Null node argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown because the node argument is null.",
          "description": "The method should guard against a null JsonNode and throw an appropriate exception."
        }
      ]
    },
    {
      "group": "Null OutputFormat argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"object\" }"
            },
            {
              "parameter": "outputFormat",
              "value": null
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown because the OutputFormat argument is null.",
          "description": "The method must validate that the OutputFormat is provided; a null value should result in an exception."
        }
      ]
    },
    {
      "group": "Schema with recursive anchor – ensure walk handles recursion flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"#\" }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "OutputFormat returns a List with no validation messages (the recursive anchor is correctly resolved without infinite recursion).",
          "description": "When the schema contains a recursive `$ref` (self‑reference), walk should detect `isRecursiveAnchor()` and avoid stack overflow, still producing a valid result."
        }
      ]
    }
  ]
}