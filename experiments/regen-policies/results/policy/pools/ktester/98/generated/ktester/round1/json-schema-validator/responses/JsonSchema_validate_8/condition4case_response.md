{
  "testCases": [
    {
      "group": "Successful validation – schema matches",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":30,\"name\":\"Alice\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat (returns List<ValidationMessage>)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "An empty List (no validation messages)",
          "description": "When the JSON instance conforms to the schema, the validator should produce no ValidationMessage objects. The custom OutputFormat collects the messages into a List, which is expected to be empty."
        }
      ]
    },
    {
      "group": "Validation failures – schema violations",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":\"thirty\",\"name\":null}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat (returns List<ValidationMessage>)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "A List containing at least two ValidationMessage objects (type mismatch for 'age' and null value for required 'name')",
          "description": "The JSON instance violates the schema (wrong type for 'age' and missing required 'name'). The validator must return the collected validation messages via the OutputFormat."
        }
      ]
    },
    {
      "group": "Null rootNode handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null JsonNode should cause the method to throw a NullPointerException before any validation is performed."
        }
      ]
    },
    {
      "group": "Null OutputFormat handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":25}"
            },
            {
              "parameter": "format",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the OutputFormat argument is null the method must fail fast with a NullPointerException."
        }
      ]
    },
    {
      "group": "Consumer throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":20}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "Consumer that throws IllegalStateException"
            }
          ],
          "expected": "IllegalStateException propagated to the caller",
          "description": "When the provided Consumer throws an unchecked exception, the validate method must not swallow it; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "Consumer is invoked (side‑effect verification)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":40}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "Consumer that sets a flag on the ExecutionContext"
            }
          ],
          "expected": "Flag on ExecutionContext is true after validation",
          "description": "The Consumer supplied to the method should be executed exactly once. The test uses a mutable flag (e.g., AtomicBoolean) that the Consumer sets; after validation the flag must be true, confirming the Consumer was called."
        }
      ]
    }
  ]
}