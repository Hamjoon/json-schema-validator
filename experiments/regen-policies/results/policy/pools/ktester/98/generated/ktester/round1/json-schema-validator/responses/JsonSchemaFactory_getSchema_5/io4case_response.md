Extra data: line 65 column 131 (char 3247)
{
  "testCases": [
    {
      "group": "JsonSchemaFactory.getSchema(String) - advanced valid schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" }, \"ref\": { \"$ref\": \"#/definitions/name\" } }, \"definitions\": { \"name\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. The schema resolves the internal $ref correctly, so validation of a JSON object containing a string property \"ref\" must succeed.",
          "description": "Pass a well‑formed schema that contains an internal $ref. The factory should parse the schema, resolve the reference, and produce a usable JsonSchema."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"array\", \"items\": { \"type\": \"number\" } }"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. The meta‑schema URI is recognized (draft‑07) and the resulting schema has root type ARRAY.",
          "description": "Supply a schema that explicitly declares a draft‑07 meta‑schema via the $schema keyword. The factory should load the appropriate meta‑schema and create the schema accordingly."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"boolean\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. The root schema is a boolean schema (true/false) and should be treated as ALWAYS valid.",
          "description": "Provide a boolean schema (the JSON Schema spec allows true/false as a schema). The factory must accept it and return a JsonSchema that always validates."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{}"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. An empty object is a valid schema that imposes no constraints.",
          "description": "Pass an empty JSON object as a schema. The factory should treat it as a permissive schema and not throw an exception."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"emoji\": { \"type\": \"string\", \"pattern\": \"\\uD83D\\uDE00\" } } }"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. Unicode characters in pattern strings are handled correctly.",
          "description": "Test handling of non‑ASCII / Unicode characters inside the schema (e.g., an emoji in a regex pattern)."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(String) - error handling for malformed JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } "   }   ]   }   ]   }   ]   }<|end|>