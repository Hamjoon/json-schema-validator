{
  "group": "detectOptionalVersion",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": true }"
        }
      ],
      "expected": "JsonSchemaException: \"'true' is unrecognizable schema\"",
      "description": "The $schema value is a boolean true. asText() converts it to \"true\", which is not a known schema URI, causing a JsonSchemaException."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": false }"
        }
      ],
      "expected": "JsonSchemaException: \"'false' is unrecognizable schema\"",
      "description": "The $schema value is a boolean false. asText() converts it to \"false\", leading to an unrecognizable schema exception."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": [ \"http://json-schema.org/draft-07/schema#\" ] }"
        }
      ],
      "expected": "JsonSchemaException: \"'[\\\"http://json-schema.org/draft-07/schema#\\\"]' is unrecognizable schema\"",
      "description": "The $schema value is an array. asText() yields the JSON array representation, which does not match any known schema URI, resulting in a JsonSchemaException."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"   \" }"
        }
      ],
      "expected": "JsonSchemaException: \"'   ' is unrecognizable schema\"",
      "description": "The $schema value consists only of whitespace. After normalization the identifier remains blank, causing an exception."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "[ { \"$schema\": \"http://json-schema.org/draft-07/schema#\" } ]"
        }
      ],
      "expected": "Optional.empty",
      "description": "The root node is an array; get(\"$schema\") returns null, so the method should return an empty Optional."
    }
  ]
}