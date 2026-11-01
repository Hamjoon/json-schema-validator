{
  "testCases": [
    {
      "group": "walk – validation enabled (true) – valid instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": \"John Doe\" }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat<Set<ValidationMessage>>"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "An empty Set – no validation messages because the instance conforms to the schema.",
          "description": "The schema expects a required string property \"name\". The supplied node satisfies the schema, so walk should invoke the underlying validator and return an empty collection when validation is turned on."
        }
      ]
    },
    {
      "group": "walk – validation enabled (true) – invalid instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": 123 }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat<Set<ValidationMessage>>"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "A Set containing at least one ValidationMessage indicating a type error for property \"name\".",
          "description": "The schema requires \"name\" to be a string, but the node provides a numeric value. With validation enabled, walk must run the type validator and return the corresponding validation message(s)."
        }
      ]
    },
    {
      "group": "walk – validation disabled (false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": 123 }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat<Set<ValidationMessage>>"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "An empty Set – no validation messages because validation was skipped.",
          "description": "When the validate flag is false, walk must bypass all validators and return an empty result regardless of the node content."
        }
      ]
    },
    {
      "group": "walk – null JsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat<Set<ValidationMessage>>"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "NullPointerException",
          "description": "The implementation expects a non‑null JsonNode. Supplying null should cause a NullPointerException before any validation logic is executed."
        }
      ]
    },
    {
      "group": "walk – null OutputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": \"John\" }"
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
          "expected": "IllegalArgumentException (or NullPointerException) indicating that outputFormat must not be null",
          "description": "The method should guard against a null OutputFormat because it is required to produce a result. The expected behaviour is to throw an exception immediately."
        }
      ]
    },
    {
      "group": "walk – node is an array (schema expects object)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[ { \"name\": \"Alice\" } ]"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat<Set<ValidationMessage>>"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "A Set containing a ValidationMessage about the instance type mismatch (expected OBJECT, found ARRAY).",
          "description": "The schema is defined for an object with a required property \"name\". Providing an array should trigger the type validator and produce an appropriate error message."
        }
      ]
    },
    {
      "group": "walk – schema with recursive anchor (isRecursiveAnchor = true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"self\": { \"$ref\": \"#\" } }"
            },
            {
              "parameter": "outputFormat",
              "value": "CollectingOutputFormat<Set<ValidationMessage>>"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "A Set without infinite recursion – validation completes successfully (or contains only expected messages for the concrete instance).",
          "description": "When the schema contains a recursive anchor, walk must detect and handle recursion correctly, avoiding a StackOverflowError. The test verifies that the method returns a finite result."
        }
      ]
    }
  ]
}