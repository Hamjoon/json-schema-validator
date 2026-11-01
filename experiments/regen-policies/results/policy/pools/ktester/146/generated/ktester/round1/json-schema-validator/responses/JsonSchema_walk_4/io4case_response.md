{
  "testCases": [
    {
      "group": "Missing required property – validation error",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"object\", \"required\": [\"id\"], \"properties\": { \"id\": { \"type\": \"integer\" } } }"
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
          "expected": "OutputFormat returns a List containing one ValidationMessage indicating that required property 'id' is missing.",
          "description": "The schema requires property 'id' of type integer, but the instance object is empty. With validation enabled, walk should report a missing‑required‑property error."
        }
      ]
    },
    {
      "group": "Enum constraint violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"enum\": [\"red\", \"green\", \"blue\"] }"
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
          "expected": "OutputFormat returns a List containing one ValidationMessage stating that the value is not one of the allowed enum values.",
          "description": "The schema restricts the string to a set of enum values. The instance value is not in that set, so a validation error must be produced."
        }
      ]
    },
    {
      "group": "Pattern constraint violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"pattern\": \"^[A-Z]{3}[0-9]{4}$\" }"
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
          "expected": "OutputFormat returns a List containing one ValidationMessage indicating that the string does not match the required regex pattern.",
          "description": "The schema defines a regex pattern for a product code. The supplied string fails to match, so walk should emit a pattern‑mismatch message."
        }
      ]
    },
    {
      "group": "Array items type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }"
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
          "expected": "OutputFormat returns a List containing at least one ValidationMessage for the array element that is not an integer.",
          "description": "The schema expects an array of integers. The instance array contains a non‑integer element (e.g., a string), which must be reported."
        }
      ]
    },
    {
      "group": "OneOf schema – matching subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }"
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
          "expected": "OutputFormat returns an empty List because the instance matches at least one subschema (string or integer).",
          "description": "The schema uses oneOf with two alternatives. The supplied instance is a valid string, so validation succeeds with no messages."
        }
      ]
    },
    {
      "group": "OneOf schema – no matching subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"oneOf\": [ { \"type\": \"boolean\" }, { \"type\": \"null\" } ] }"
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
          "expected": "OutputFormat returns a List containing at least one ValidationMessage stating that none of the oneOf subschemas matched.",
          "description": "The instance is a number (default JSON number node used by the test harness) which does not satisfy either boolean or null subschemas, so a oneOf validation error is expected."
        }
      ]
    },
    {
      "group": "OutputFormat throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"integer\" }"
            },
            {
              "parameter": "outputFormat",
              "value": "ExceptionThrowingOutputFormat (throws RuntimeException on any call)"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "RuntimeException propagated from the OutputFormat implementation.",
          "description": "A custom OutputFormat that deliberately throws an exception is supplied. walk should not swallow the exception; it must propagate it to the caller."
        }
      ]
    },
    {
      "group": "Validate flag false with invalid instance – no messages",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"integer\" }"
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
          "expected": "OutputFormat returns an empty List because validation is disabled, even though the instance node is not an integer.",
          "description": "The schema expects an integer, but the supplied instance is a JSON string. Since validate is false, walk must skip validation and return no messages."
        }
      ]
    },
    {
      "group": "Schema allowing null – node is JSON null",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": [\"null\", \"string\"] }"
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
          "expected": "OutputFormat returns an empty List because the instance is JSON null, which is permitted by the schema.",
          "description": "The schema permits either null or string. The instance supplied is the JSON literal null, so validation should succeed with no messages."
        }
      ]
    },
    {
      "group": "Custom OutputFormat returning non‑collection result",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"boolean\" }"
            },
            {
              "parameter": "outputFormat",
              "value": "BooleanResultOutputFormat (returns Boolean true if validation passes, false otherwise)"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "true (the Boolean value returned by the OutputFormat indicating successful validation).",
          "description": "A generic OutputFormat that returns a Boolean is used. The instance matches the schema, so walk should return true."
        }
      ]
    }
  ]
}