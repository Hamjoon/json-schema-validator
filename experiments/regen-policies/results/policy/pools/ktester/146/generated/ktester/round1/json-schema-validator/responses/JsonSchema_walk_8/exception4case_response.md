{
  "test_cases": [
    {
      "group": "$ref resolution failure – missing external schema",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonSchemaException thrown indicating unresolved $ref",
          "description": "The JsonSchema instance is loaded with a schema that contains a $ref to an external schema file that does not exist. When walk is invoked, the $ref resolution fails and a JsonSchemaException is thrown."
        }
      ]
    },
    {
      "group": "Recursive schema detection – infinite recursion",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonSchemaException thrown indicating recursive reference",
          "description": "The JsonSchema instance contains a recursive $ref without a proper $recursiveAnchor, leading to infinite recursion detection. The walk method should detect this and throw a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Invalid schema definition – unknown keyword",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown indicating unknown schema keyword",
          "description": "The JsonSchema instance is constructed with a schema containing an unknown keyword (e.g., \"unknownKeyword\": true) while strict mode is enabled. The walk method should detect the invalid schema definition and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported schema version – unknown $schema value",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown indicating unsupported schema version",
          "description": "The JsonSchema instance is loaded with a schema that declares an unsupported $schema version (e.g., \"http://json-schema.org/draft-04/schema#\"). The walk method should fail fast with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Custom validator throws unchecked exception",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "RuntimeException thrown from custom validator",
          "description": "The JsonSchema instance includes a custom validator (registered via SchemaValidatorsConfig) that throws a RuntimeException during validation. When walk is called with validation enabled, the exception should propagate."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer introduces invalid state causing IllegalArgumentException",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "consumer that sets ExecutionContext's schema version to null" }
          ],
          "expected": "IllegalArgumentException thrown due to invalid ExecutionContext configuration",
          "description": "A Consumer is provided that modifies the ExecutionContext in an invalid way (e.g., setting the schema version to null). When walk proceeds, the invalid configuration leads to an IllegalArgumentException."
        }
      ]
    }
  ]
}