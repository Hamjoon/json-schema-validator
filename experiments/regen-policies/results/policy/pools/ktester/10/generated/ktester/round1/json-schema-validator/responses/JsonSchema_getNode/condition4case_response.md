{
  "testCases": [
    {
      "group": "Number Index",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": 0
            }
          ],
          "expected": "JsonNode representing the element at index 0 of the schema node array (e.g., a text node \"arrayValue0\")",
          "description": "When the argument is a Number, the method should treat it as an array index and return node.get(index). This covers the `instanceof Number` branch."
        }
      ]
    },
    {
      "group": "Simple String Property (no special chars)",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "simple"
            }
          ],
          "expected": "JsonNode representing the field \"simple\" of the schema node (e.g., a text node \"value1\")",
          "description": "String without '~' or '%' goes straight to node.get(unescaped). This covers the false path for both special‑character checks."
        }
      ]
    },
    {
      "group": "Escaped Tilde (~1 → /)",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "a~1b"
            }
          ],
          "expected": "JsonNode representing the field \"a/b\" of the schema node (e.g., a text node \"value2\")",
          "description": "String contains '~', triggering the replacement `~1` → `/`. The second replacement (`~0`) does not apply. This covers the `if (unescaped.contains(\"~\"))` branch."
        }
      ]
    },
    {
      "group": "Escaped Tilde Zero (~0 → ~)",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "a~0b"
            }
          ],
          "expected": "JsonNode representing the field \"a~b\" of the schema node (e.g., a text node \"value3\")",
          "description": "String contains '~', triggering the replacement `~0` → `~`. This also covers the `if (unescaped.contains(\"~\"))` branch."
        }
      ]
    },
    {
      "group": "Percent‑Encoded Property",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "a%2Fb"
            }
          ],
          "expected": "JsonNode representing the field \"a/b\" of the schema node (e.g., a text node \"value2\")",
          "description": "String contains '%', causing URL decoding. This covers the `if (unescaped.contains(\"%\"))` branch."
        }
      ]
    },
    {
      "group": "Combined Escaped Tilde and Percent Encoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "a~1b%2Fc"
            }
          ],
          "expected": "JsonNode representing the field \"a/b/c\" of the schema node (e.g., a text node \"value4\")",
          "description": "String contains both '~' and '%'. First the '~' replacement yields \"a/b%2Fc\", then URL decoding yields \"a/b/c\". This case exercises both special‑character branches sequentially."
        }
      ]
    },
    {
      "group": "Tilde Present but No Replacement Tokens",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "no~tilde"
            }
          ],
          "expected": "JsonNode representing the field \"no~tilde\" of the schema node (or null if not present)",
          "description": "String contains '~' but neither \"~1\" nor \"~0\". The replacement code runs but makes no changes, covering the true path of the '~' check without altering the key."
        }
      ]
    },
    {
      "group": "Null Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling the method with a null argument causes a NPE when `propertyOrIndex.toString()` is invoked. This verifies the method’s behaviour for invalid input."
        }
      ]
    }
  ]
}