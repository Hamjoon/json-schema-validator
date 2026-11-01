{
  "groups": [
    {
      "group": "Intermediate sub‑schema created because $id is present",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /components/schemas/User/properties/id"
            }
          ],
          "expected": "A JsonSchema instance is returned. After processing the first segment \"components\" the method resolves a non‑null $id in that node, creates a sub‑schema and updates schemaLocation/evaluationPath with \"components\". The second segment \"schemas\" is then processed on the new parent, again updating the locations. Finally the third segment \"User\" (with $id still non‑null) creates another sub‑schema, and the last segment \"properties\"/\"id\" are appended, resulting in a schema whose schemaLocation ends with \"/components/schemas/User/properties/id\" and evaluationPath mirrors it. No exception is thrown.",
          "description": "The fragment has several segments. The node for the first segment contains a $id, so the condition `(id != null) || (x == nameCount‑1)` is true before the last segment, forcing early sub‑schema creation. The test verifies that early creation does not prevent later segments from being processed and that locations are correctly accumulated."
        }
      ]
    },
    {
      "group": "Numeric segment on a non‑array parent is treated as a string",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /properties/0"
            }
          ],
          "expected": "A JsonSchema instance is returned. Although the segment is a Number (0), the current parent node is an OBJECT, so the method follows the else‑branch, appending the string \"0\" to schemaLocation and evaluationPath. No array‑specific index handling occurs.",
          "description": "Ensures that numeric‑looking segments are only interpreted as array indexes when the parent node is actually an array. Here the parent is an object (the \"properties\" node), so the segment is handled like a normal property name."
        }
      ]
    },
    {
      "group": "Empty fragment (no segments) returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing an empty path"
            }
          ],
          "expected": "null is returned (no sub‑schema created and no exception thrown).",
          "description": "When the supplied JsonNodePath has `getNameCount() == 0`, the for‑loop is never entered. The method therefore returns the initially‑null `subSchema`. The test confirms that an empty fragment does not cause a NullPointerException or an exception."
        }
      ]
    },
    {
      "group": "Delegated schema resolves fragment but later fails – exception propagates",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /external/invalid"
            }
          ],
          "expected": "InvalidSchemaRefException is thrown. The initial document cannot resolve the fragment, `fetchSubSchemaNode` returns a non‑null JsonSchema (`found`), but `found.getSubSchema(fragment)` throws because the delegated schema also cannot resolve the fragment.",
          "description": "Covers the path where the first document delegates to another schema resource, but that resource ultimately cannot resolve the fragment, leading to an exception bubbling up from the recursive call."
        }
      ]
    },
    {
      "group": "Multiple segments with mixed numeric and string handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /items/0/properties/name"
            }
          ],
          "expected": "A JsonSchema instance is returned. The first segment \"items\" updates locations as a string. The second segment (numeric 0) is processed on an ARRAY node, causing numeric index handling (schemaLocation/evaluationPath append 0). The third segment \"properties\" and fourth \"name\" are appended as strings. The final schemaLocation ends with \"/items/0/properties/name\" and evaluationPath mirrors it.",
          "description": "Validates that the method correctly switches between string and numeric handling as it traverses a path that includes an array index followed by object properties."
        }
      ]
    }
  ]
}