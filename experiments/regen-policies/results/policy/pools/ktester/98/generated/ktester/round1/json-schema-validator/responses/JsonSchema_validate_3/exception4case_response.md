{
  "testCases": [
    {
      "group": "Malformed $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "UnsupportedEncodingException",
          "description": "The schema contains a $ref with an invalid percent‑encoding (e.g., \"%ZZ\"). When validate() attempts to resolve the reference it calls URLDecoder.decode, which throws UnsupportedEncodingException. The method must propagate this exception."
        }
      ]
    },
    {
      "group": "Invalid schema definition",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema node defines an unknown type value (e.g., \"type\": \"unknownType\"). During validator initialization JsonSchema should detect the illegal type and throw IllegalArgumentException before any validation occurs."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "UnsupportedOperationException",
          "description": "The schema includes a custom keyword that the library does not recognise (e.g., \"fooValidator\": true). While building the validator list JsonSchema throws UnsupportedOperationException, which must be propagated by validate()."
        }
      ]
    },
    {
      "group": "Infinite recursion without recursive anchor",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "StackOverflowError",
          "description": "The schema contains a circular $ref chain that does not involve a recursive anchor. When validate() follows the references it recurses indefinitely, eventually causing a StackOverflowError. The method should let this error surface."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer throws checked exception wrapped in RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"a\":1}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> { try { throw new java.io.IOException(\"io fail\"); } catch (Exception e) { throw new RuntimeException(e); } }"
            }
          ],
          "expected": "RuntimeException",
          "description": "The customizer throws a checked exception (IOException) which is wrapped in a RuntimeException. validate() must propagate the RuntimeException unchanged."
        }
      ]
    }
  ]
}