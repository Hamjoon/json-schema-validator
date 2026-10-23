{
  "testCases": [
    {
      "group": "Valid JSON Schema Content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"string\"}"
            }
          ],
          "expected": "non-null JsonSchema instance",
          "description": "A simple, valid JSON schema supplied as a raw string should be parsed successfully and return a JsonSchema object."
        }
      ]
    },
    {
      "group": "Valid YAML Schema Content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: string"
            }
          ],
          "expected": "non-null JsonSchema instance",
          "description": "A valid YAML schema supplied as a raw string must be recognized, parsed by the internal YAMLMapper and result in a JsonSchema object."
        }
      ]
    },
    {
      "group": "Schema with External $ref (JSON)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"$ref\": \"classpath:ref-schema.json\" } } }"
            }
          ],
          "expected": "non-null JsonSchema instance with resolved reference",
          "description": "The main schema contains a $ref to a second schema located on the classpath; the factory must resolve the reference without throwing an exception."
        }
      ]
    },
    {
      "group": "Schema with External $ref (YAML)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  address:\n    $ref: classpath:address-schema.yaml"
            }
          ],
          "expected": "non-null JsonSchema instance with resolved reference",
          "description": "A YAML schema containing a $ref to another YAML schema on the classpath should be loaded and the reference resolved."
        }
      ]
    },
    {
      "group": "Invalid JSON Content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ invalid json }"
            }
          ],
          "expected": "JsonParseException",
          "description": "Malformed JSON should cause the underlying Jackson parser to throw a JsonParseException which propagates as an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid YAML Content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type string: 123"
            }
          ],
          "expected": "JsonParseException",
          "description": "Malformed YAML should trigger a parsing exception (JsonParseException) from the YAMLMapper."
        }
      ]
    },
    {
      "group": "Unsupported File Extension",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "classpath:unsupported-schema.txt"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A schema location ending with an unsupported extension (e.g., .txt) must be rejected with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Malformed URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "http://[invalid"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A syntactically invalid URI should cause the factory to wrap the URISyntaxException in an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Non‑existent Classpath Resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "classpath:does-not-exist.json"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the supplied classpath location cannot be found, the method must throw an IllegalArgumentException indicating the resource is missing."
        }
      ]
    },
    {
      "group": "Valid JSON Schema from Classpath (JSON file)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "classpath:valid-schema.json"
            }
          ],
          "expected": "non-null JsonSchema instance",
          "description": "A correctly formatted JSON schema placed on the classpath should be located, parsed and returned as a JsonSchema object."
        }
      ]
    },
    {
      "group": "Valid JSON Schema from Classpath (YAML file)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "classpath:valid-schema.yaml"
            }
          ],
          "expected": "non-null JsonSchema instance",
          "description": "A correctly formatted YAML schema on the classpath should be located, parsed by YAMLMapper and returned as a JsonSchema object."
        }
      ]
    },
    {
      "group": "Caching Behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "classpath:valid-schema.json"
            }
          ],
          "expected": "same JsonSchema instance on subsequent calls",
          "description": "Calling getSchema with the same classpath location multiple times should return the cached JsonSchema instance (identity equality)."
        }
      ]
    }
  ]
}