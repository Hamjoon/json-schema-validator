{
  "testCases": [
    {
      "group": "Invalid $ref – unresolved reference",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            }
          ],
          "expected": "JsonSchemaException (or ValidationException) with a message indicating that the $ref \"#/nonexistent\" could not be resolved.",
          "description": "The schema contains a $ref that points to a definition that does not exist. When validate(JsonNode) is invoked, the resolver should throw an exception rather than silently ignore the missing reference."
        }
      ]
    },
    {
      "group": "Unsupported keyword – unknown keyword in schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"any\""
            }
          ],
          "expected": "IllegalArgumentException with a message mentioning the unsupported keyword \"unknownKeyword\".",
          "description": "A schema that includes a non‑standard keyword (e.g., \"unknownKeyword\": true) should cause the validator construction to fail, and validate(JsonNode) should propagate an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid format value – unknown format specifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"someValue\""
            }
          ],
          "expected": "IllegalArgumentException with a message indicating that the format \"unknown-format\" is not supported.",
          "description": "The schema declares a format that the library does not recognise. When validation runs, the library should raise an IllegalArgumentException rather than attempting to validate the value."
        }
      ]
    },
    {
      "group": "Circular $ref without recursive anchor – leads to StackOverflowError",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"any\""
            }
          ],
          "expected": "StackOverflowError (or IllegalStateException if the implementation detects the cycle and aborts).",
          "description": "Two schemas reference each other via $ref but neither declares \"$recursiveAnchor\": true. The validator will recurse indefinitely, eventually throwing a StackOverflowError (or a guarded IllegalStateException if cycle detection is implemented)."
        }
      ]
    },
    {
      "group": "Schema with malformed $id – URL decoding error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"test\""
            }
          ],
          "expected": "UnsupportedEncodingException (wrapped in a RuntimeException) when the validator attempts to URL‑decode the malformed $id value.",
          "description": "A schema that defines an \"$id\" containing characters that cannot be decoded with the default charset should cause URLDecoder.decode to throw UnsupportedEncodingException, which propagates during validation."
        }
      ]
    }
  ]
}