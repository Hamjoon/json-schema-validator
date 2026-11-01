{
  "testCases": [
    {
      "group": "JSON Pointer returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath where getPathType() returns PathType.JSON_POINTER and getFragment() is irrelevant"
            }
          ],
          "expected": "Returns null because getSubSchema(fragment) is mocked to return null",
          "description": "When the fragment is a JSON Pointer but the underlying getSubSchema method returns null, getRefSchema should propagate the null result."
        }
      ]
    },
    {
      "group": "Anchor with non‑empty base IRI found in schemaResources",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath where getPathType() returns PathType.ANCHOR and toString() returns \"/defs/foo\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored under the key \"http://example.com/schema.json#/defs/foo\" in validationContext.schemaResources",
          "description": "The JsonSchema has a non‑null absolute IRI (\"http://example.com/schema.json\"). The computed anchor should be \"http://example.com/schema.json#/defs/foo\" and the method must retrieve the schema from schemaResources."
        }
      ]
    },
    {
      "group": "Anchor with non‑empty base IRI found in dynamicAnchors",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath where getPathType() returns PathType.ANCHOR and toString() returns \"/defs/bar\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored under the key \"http://example.com/schema.json#/defs/bar\" in validationContext.dynamicAnchors",
          "description": "The base IRI is present, but the anchor is not in schemaResources. The method should fall back to dynamicAnchors and return the associated schema."
        }
      ]
    },
    {
      "group": "Anchor precedence – schemaResources over dynamicAnchors",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath where getPathType() returns PathType.ANCHOR and toString() returns \"/common\""
            }
          ],
          "expected": "Returns the JsonSchema instance from validationContext.schemaResources even though the same anchor exists in dynamicAnchors",
          "description": "When the computed anchor is present in both maps, the method must return the one from schemaResources, demonstrating the correct precedence."
        }
      ]
    },
    {
      "group": "Anchor fallback when schemaResources entry is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath where getPathType() returns PathType.ANCHOR and toString() returns \"/fallback\""
            }
          ],
          "expected": "Returns the JsonSchema instance from validationContext.dynamicAnchors because the schemaResources entry for the anchor is null",
          "description": "If schemaResources contains the anchor key but maps to null, the method should continue searching dynamicAnchors and return the non‑null schema found there."
        }
      ]
    },
    {
      "group": "Anchor with empty fragment string",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath where getPathType() returns PathType.ANCHOR and toString() returns \"\" (empty string)"
            }
          ],
          "expected": "Returns the JsonSchema instance stored under the key \"#\" in validationContext.schemaResources (or dynamicAnchors if not present there)",
          "description": "When the fragment's string representation is empty, the computed anchor becomes just \"#\" (or \"base#\" if a base IRI exists). The method must correctly handle this edge case."
        }
      ]
    },
    {
      "group": "Null fragment argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException",
          "description": "Passing a null JsonNodePath should result in a NullPointerException because the method dereferences fragment without a null check."
        }
      ]
    }
  ]
}