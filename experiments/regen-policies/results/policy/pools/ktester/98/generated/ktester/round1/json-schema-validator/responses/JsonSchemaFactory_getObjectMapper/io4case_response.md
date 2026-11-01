{
  "tests": [
    {
      "group": "JsonSchemaFactory.getObjectMapper - overridden mapper delegation",
      "cases": [
        {
          "input": [
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "Instance returned by the overridden getJsonMapper() method (custom ObjectMapper) – the same object that the subclass supplies.",
          "description": "Create a subclass of JsonSchemaFactory that overrides getJsonMapper() to return a uniquely identifiable ObjectMapper (e.g., with a custom feature or a mock). Calling getObjectMapper(JSON) must return that exact instance, proving the method delegates to the overridden implementation."
        },
        {
          "input": [
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "Instance returned by the overridden getYamlMapper() method (custom ObjectMapper) – the same object that the subclass supplies.",
          "description": "Create a subclass of JsonSchemaFactory that overrides getYamlMapper() to return a uniquely identifiable ObjectMapper. Calling getObjectMapper(YAML) must return that exact instance, confirming proper delegation to the overridden method."
        },
        {
          "input": [
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "RuntimeException with message \"json mapper failure\" is propagated unchanged.",
          "description": "Subclass JsonSchemaFactory so that getJsonMapper() throws a RuntimeException. Verify that getObjectMapper(JSON) does not catch the exception and propagates it to the caller."
        },
        {
          "input": [
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "RuntimeException with message \"yaml mapper failure\" is propagated unchanged.",
          "description": "Subclass JsonSchemaFactory so that getYamlMapper() throws a RuntimeException. Verify that getObjectMapper(YAML) propagates the exception."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getObjectMapper - exact exception message verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException with exact message \"Unsupported input format null\"",
          "description": "Calling getObjectMapper(null) must throw IllegalArgumentException and the message must match exactly, not just contain the substring."
        },
        {
          "input": [
            {
              "parameter": "inputFormat",
              "value": "UNSUPPORTED"
            }
          ],
          "expected": "IllegalArgumentException with exact message \"Unsupported input format UNSUPPORTED\"",
          "description": "When a non‑enum value (simulated via a mock or a custom enum constant that is not JSON/YAML) is supplied, the method must throw IllegalArgumentException with the precise message."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getObjectMapper - distinct mapper instances",
      "cases": [
        {
          "input": [],
          "expected": "Two distinct ObjectMapper instances: one from getObjectMapper(JSON) and another from getObjectMapper(YAML) are not the same object.",
          "description": "Invoke getObjectMapper(JSON) and getObjectMapper(YAML) on a fresh JsonSchemaFactory instance and assert that the returned mappers are different objects, confirming that JSON and YAML mappings are independent."
        }
      ]
    }
  ]
}