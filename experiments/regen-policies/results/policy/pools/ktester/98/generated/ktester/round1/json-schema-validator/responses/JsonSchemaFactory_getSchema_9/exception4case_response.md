{
  "test_cases": [
    {
      "group": "Malformed schemaUri",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://[invalid\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"object\"}')"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schemaUri string is syntactically invalid and cannot be parsed into a URI. The factory should detect this early and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid $id in jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$id\":\"http://[invalid\",\"type\":\"object\"}')"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The $id keyword contains a malformed URI. During schema creation the factory validates $id and should throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "$ref with unsupported scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$ref\":\"ftp://example.com/other.json\"}')"
            }
          ],
          "expected": "SchemaLoadingException",
          "description": "The $ref uses the FTP scheme which the factory does not support. Resolving the reference should raise a SchemaLoadingException."
        }
      ]
    },
    {
      "group": "$ref with malformed URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$ref\":\"http://[invalid\"}')"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The $ref contains a syntactically invalid URI. The factory should fail fast with IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Circular $ref reference",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$ref\":\"#\"}')"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "The schema references itself via a circular $ref. The factory should detect the cycle and throw JsonSchemaException."
        }
      ]
    },
    {
      "group": "Null root JsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"null\")"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "A JSON null node is not a valid schema root (must be object or boolean). The factory should reject it with JsonSchemaException."
        }
      ]
    }
  ]
}