{
    "group": "Unsupported InputFormat handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "{\"valid\":true}"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.UNKNOWN"
                },
                {
                    "parameter": "format",
                    "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
                }
            ],
            "expected": "java.lang.IllegalArgumentException",
            "description": "Passing an InputFormat value that the validator does not recognise should cause an IllegalArgumentException before any deserialization occurs."
        },
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "{\"valid\":true}"
                },
                {
                    "parameter": "inputFormat",
                    "value": "null"
                },
                {
                    "parameter": "format",
                    "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
                }
            ],
            "expected": "java.lang.NullPointerException",
            "description": "A null InputFormat (different from the previously tested null reference) that is explicitly passed as the string \"null\" should be interpreted as a null enum value, triggering a NullPointerException."
        }
    ]
}