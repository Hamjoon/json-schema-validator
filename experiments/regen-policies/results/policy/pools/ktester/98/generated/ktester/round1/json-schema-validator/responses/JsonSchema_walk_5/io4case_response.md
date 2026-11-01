{
  "newTestCases": [
    {
      "group": "Valid YAML input – validation enabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: 30" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "A well‑formed YAML document is supplied. The method should deserialize using the YAML branch, create an ExecutionContext and walk the schema with validation on, returning a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid YAML input – validation disabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: 30" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": false }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "Same YAML payload but validation flag is false; the walk should succeed without applying schema constraints."
        }
      ]
    },
    {
      "group": "Whitespace‑only input string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "   \n\t  " },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.failure with a parsing error message such as \"No content to map due to end-of-input\"",
          "description": "An input that contains only whitespace should be treated as empty by the deserializer, resulting in a parsing failure."
        }
      ]
    },
    {
      "group": "JSON array as root node",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "[{\"id\":1},{\"id\":2}]" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "A JSON array is a valid JSON document. Assuming the schema does not forbid arrays, the walk should succeed."
        }
      ]
    },
    {
      "group": "JSON primitive number as root node",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "42" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "A JSON numeric literal is syntactically valid. The method should deserialize it to a JsonNode and walk the schema without errors."
        }
      ]
    },
    {
      "group": "JSON primitive boolean as root node",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "true" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "A JSON boolean literal is valid. The walk should complete successfully."
        }
      ]
    },
    {
      "group": "Null InputFormat with validation disabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": null },
            { "parameter": "validate", "value": false }
          ],
          "expected": "ValidationResult.failure with a message indicating that InputFormat is null or unsupported",
          "description": "Even when validation is turned off, a null InputFormat should be detected early and reported as a failure."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat (XML)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "<person><name>John</name></person>" },
            { "parameter": "inputFormat", "value": "InputFormat.XML" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.failure with a message stating that InputFormat.XML is not supported",
          "description": "The library does not handle XML input; the method should fail fast with an appropriate error."
        }
      ]
    },
    {
      "group": "Large JSON payload",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"items\":[{\"id\":0},{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5},{\"id\":6},{\"id\":7},{\"id\":8},{\"id\":9},{\"id\":10},{\"id\":11},{\"id\":12},{\"id\":13},{\"id\":14},{\"id\":15},{\"id\":16},{\"id\":17},{\"id\":18},{\"id\":19},{\"id\":20},{\"id\":21},{\"id\":22},{\"id\":23},{\"id\":24},{\"id\":25},{\"id\":26},{\"id\":27},{\"id\":28},{\"id\":29},{\"id\":30},{\"id\":31},{\"id\":32},{\"id\":33},{\"id\":34},{\"id\":35},{\"id\":36},{\"id\":37},{\"id\":38},{\"id\":39},{\"id\":40},{\"id\":41},{\"id\":42},{\"id\":43},{\"id\":44},{\"id\":45},{\"id\":46},{\"id\":47},{\"id\":48},{\"id\":49},{\"id\":50}]}"
            },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "A relatively large JSON document is processed to ensure the method can handle bigger inputs without performance‑related failures."
        }
      ]
    },
    {
      "group": "Unicode characters in JSON string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"message\":\"Hello, 世界 🌍\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true }
          ],
          "expected": "ValidationResult.success (no errors, isValid == true)",
          "description": "The input contains multibyte Unicode characters and an emoji. The deserializer must correctly handle UTF‑8 encoded text and the walk should succeed."
        }
      ]
    }
  ]
}