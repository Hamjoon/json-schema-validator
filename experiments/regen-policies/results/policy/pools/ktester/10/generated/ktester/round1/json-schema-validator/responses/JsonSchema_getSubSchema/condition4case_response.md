{
  "testCases": [
    {
      "group": "Normal traversal - object property segment",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath with a single string segment ['title']"
            }
          ],
          "expected": "JsonSchema instance representing the sub‑schema located at '/title' of the root document",
          "description": "The fragment points to an existing property. The loop finds a non‑null subSchemaNode on the first iteration, creates a new JsonSchema via the validation context and returns it."
        }
      ]
    },
    {
      "group": "Normal traversal - multiple mixed segments (object + array index)",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath with segments ['properties', 'items', 0, 'type']"
            }
          ],
          "expected": "JsonSchema instance representing the deepest sub‑schema located at '/properties/items/0/type'",
          "description": "The fragment contains both string and numeric segments. Each iteration finds a non‑null node; when the segment is a Number the code appends an integer index to the SchemaLocation and EvaluationPath. The final sub‑schema is returned after the loop finishes."
        }
      ]
    },
    {
      "group": "Fragment not found – fetchSubSchemaNode returns null → exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath with a single string segment ['nonexistent']"
            }
          ],
          "expected": "JsonSchemaException with message containing \"Unable to find subschema\" and the fragment string",
          "description": "The fragment cannot be resolved in the current document (subSchemaNode is null). The method calls `document.findSchemaResourceRoot().fetchSubSchemaNode(...)` which returns null, causing the method to throw a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Fragment not found – fetchSubSchemaNode finds another document",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath with a single string segment ['external']"
            }
          ],
          "expected": "JsonSchema instance representing the sub‑schema located at '/external' in the fetched external schema document",
          "description": "The fragment is not present in the current document, so `fetchSubSchemaNode` returns a different JsonSchema (the external document). The method then recursively calls `found.getSubSchema(fragment)` which successfully resolves the fragment in that document and returns the resulting sub‑schema."
        }
      ]
    },
    {
      "group": "Segment is a Number (array index) – ensures numeric handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath with a single numeric segment [2]"
            }
          ],
          "expected": "JsonSchema instance representing the sub‑schema located at '/2' of the root array schema",
          "description": "The only segment is a Number. The loop treats it as an index, appends the integer to the SchemaLocation and EvaluationPath, creates a new JsonSchema via the validation context and returns it."
        }
      ]
    }
  ]
}