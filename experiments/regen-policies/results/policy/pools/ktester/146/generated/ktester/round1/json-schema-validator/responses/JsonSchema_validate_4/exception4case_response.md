{
  "testCases": [
    {
      "group": "$ref resolution errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"$ref\":\"http://example.com/invalid%ZZschema.json\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "UnsupportedEncodingException (or IllegalArgumentException) indicating that the $ref URI contains an invalid percent‑encoding",
          "description": "The schema contains a $ref with a malformed percent‑encoded fragment. When validate() tries to decode the reference it should throw an exception rather than silently ignore the error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"$ref\":\"nonexistent-schema.json\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or IOException) stating that the referenced schema could not be found or loaded",
          "description": "A $ref that points to a non‑existent schema resource should cause the validation process to abort with a clear exception, because the schema cannot be resolved."
        }
      ]
    },
    {
      "group": "Invalid configuration via customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that sets maxDepth to -1 (e -> e.getConfig().setMaxDepth(-1))"
            }
          ],
          "expected": "IllegalArgumentException indicating that maxDepth must be non‑negative",
          "description": "The customizer mutates the ExecutionContext configuration to an illegal value (negative maxDepth). Validation should detect this and throw an IllegalArgumentException before processing the JSON."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that disables all validators (e -> e.getConfig().setValidateAll(false))"
            }
          ],
          "expected": "IllegalStateException (or IllegalArgumentException) because disabling all validation makes the schema unusable",
          "description": "If the customizer disables every validator, the schema cannot perform any checks. The implementation should recognise this mis‑configuration and raise an exception."
        }
      ]
    },
    {
      "group": "MessageSource null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":\"thirty\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that sets MessageSource to null (e -> e.setMessageSource(null))"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) when attempting to format validation messages",
          "description": "A null MessageSource prevents localisation of messages. When the validator later tries to retrieve a message, it should throw a clear NPE rather than returning a default or corrupt string."
        }
      ]
    },
    {
      "group": "UnsupportedEncodingException from URLDecoder",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"$ref\":\"#/definitions/invalid%2\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "UnsupportedEncodingException (or IllegalArgumentException) caused by an incomplete percent‑escape in the fragment identifier",
          "description": "The $ref fragment contains an incomplete percent‑encoding sequence. The internal call to URLDecoder.decode should raise an exception, which must propagate out of validate()."
        }
      ]
    }
  ]
}