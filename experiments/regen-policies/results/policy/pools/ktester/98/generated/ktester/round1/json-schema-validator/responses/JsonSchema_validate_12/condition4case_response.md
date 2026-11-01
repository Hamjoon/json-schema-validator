{
  "testCases": [
    {
      "group": "Successful validation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            }
          ],
          "expected": "empty list []",
          "description": "A well‑formed JSON document that conforms to the schema should be deserialized successfully and the validator should return an empty list of ValidationMessage objects."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"id\":1,\"tags\":[\"a\",\"b\"]}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> messages) { return messages.isEmpty() ? \"OK\" : \"FAIL\"; } }"
            }
          ],
          "expected": "\"OK\"",
          "description": "When the JSON input is valid the OutputFormat converts the empty ValidationMessage set to the string \"OK\"."
        }
      ]
    },
    {
      "group": "Invalid JSON / deserialization errors",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{invalidJson: true" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            }
          ],
          "expected": "com.fasterxml.jackson.core.JsonParseException",
          "description": "A malformed JSON string should cause the internal deserialize method to throw a JsonParseException, which propagates out of validate."
        },
        {
          "input": [
            { "parameter": "input", "value": "name=John&age=30" },
            { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            }
          ],
          "expected": "java.io.UnsupportedEncodingException",
          "description": "If the InputFormat is URL_ENCODED and the internal URLDecoder cannot decode the string, an UnsupportedEncodingException should be thrown."
        }
      ]
    },
    {
      "group": "Null argument handling",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "null" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "Passing a null input string should result in a NullPointerException from deserialize."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"valid\":true}" },
            { "parameter": "inputFormat", "value": "null" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "A null InputFormat argument should cause a NullPointerException before deserialization."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"valid\":true}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "null" }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "A null OutputFormat argument should cause a NullPointerException when the overloaded validate method is invoked."
        }
      ]
    },
    {
      "group": "Custom OutputFormat behavior",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Integer>() { public Integer apply(Set<ValidationMessage> messages) { return messages.size(); } }"
            }
          ],
          "expected": "0",
          "description": "When the document is valid the custom OutputFormat returns the count of validation messages, which should be zero."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"unexpected\":123}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Integer>() { public Integer apply(Set<ValidationMessage> messages) { return messages.size(); } }"
            }
          ],
          "expected": "greater than 0",
          "description": "For an input that violates the schema, the custom OutputFormat should return a positive integer representing the number of validation errors."
        }
      ]
    }
  ]
}