{
  "testCases": [
    {
      "group": "Recursive anchor set via $recursiveAnchor keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"$recursiveAnchor\": true}"
            }
          ],
          "expected": true,
          "description": "When a schema JSON contains the \"$recursiveAnchor\": true keyword, the JsonSchema instance created from it must have recursiveAnchor set to true, and isRecursiveAnchor() should return true."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"$recursiveAnchor\": false}"
            }
          ],
          "expected": false,
          "description": "When the \"$recursiveAnchor\" keyword is explicitly false, the field must stay false; isRecursiveAnchor() should return false."
        }
      ]
    },
    {
      "group": "Recursive anchor absent in schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"type\":\"object\",\"properties\":{}}"
            }
          ],
          "expected": false,
          "description": "A schema that does not contain the \"$recursiveAnchor\" keyword must leave the field at its default false value."
        }
      ]
    },
    {
      "group": "Recursive anchor after schema inheritance (fromRef)",
      "cases": [
        {
          "input": [
            {
              "parameter": "parentSchemaJson",
              "value": "{\"$recursiveAnchor\": true}"
            },
            {
              "parameter": "childRef",
              "value": "#"
            }
          ],
          "expected": true,
          "description": "A child schema obtained via fromRef() that points to a parent schema with $recursiveAnchor:true must inherit the true flag; isRecursiveAnchor() on the child should return true."
        },
        {
          "input": [
            {
              "parameter": "parentSchemaJson",
              "value": "{\"$recursiveAnchor\": false}"
            },
            {
              "parameter": "childRef",
              "value": "#"
            }
          ],
          "expected": false,
          "description": "If the parent schema has $recursiveAnchor:false, the child schema obtained via fromRef() must also report false."
        }
      ]
    },
    {
      "group": "Recursive anchor unaffected by validator initialization",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"$recursiveAnchor\": true}"
            },
            {
              "parameter": "callInitializeValidators",
              "value": true
            }
          ],
          "expected": true,
          "description": "Calling initializeValidators() on a schema that already has recursiveAnchor=true must not change the flag; isRecursiveAnchor() should still return true."
        },
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"type\":\"string\"}"
            },
            {
              "parameter": "callInitializeValidators",
              "value": true
            }
          ],
          "expected": false,
          "description": "Calling initializeValidators() on a schema without the keyword must leave recursiveAnchor false."
        }
      ]
    },
    {
      "group": "Recursive anchor after explicit reflection reset",
      "cases": [
        {
          "input": [
            {
              "parameter": "setRecursiveAnchorToTrue",
              "value": true
            },
            {
              "parameter": "setRecursiveAnchorToFalse",
              "value": false
            }
          ],
          "expected": false,
          "description": "After setting the private field to true via reflection and then resetting it to false, isRecursiveAnchor() must reflect the latest state (false)."
        }
      ]
    },
    {
      "group": "Recursive anchor with malformed keyword value",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{\"$recursiveAnchor\": \"notABoolean\"}"
            }
          ],
          "expected": false,
          "description": "If the $recursiveAnchor keyword is present but not a boolean, JsonSchema should ignore it and keep the default false value."
        }
      ]
    },
    {
      "group": "Recursive anchor on schema created from InputFormat (String input)",
      "cases": [
        {
          "input": [
            {
              "parameter": "inputString",
              "value": "{ \"$recursiveAnchor\": true }"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": true,
          "description": "When a schema is built from a JSON string containing $recursiveAnchor:true via the validate(String, InputFormat) overload, the resulting JsonSchema must have recursiveAnchor true."
        },
        {
          "input": [
            {
              "parameter": "inputString",
              "value": "{ \"type\": \"array\" }"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": false,
          "description": "When a schema is built from a JSON string without the $recursiveAnchor keyword, the flag stays false."
        }
      ]
    }
  ]
}