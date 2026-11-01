Invalid control character at: line 12 column 44 (char 373)
{
    "group": "Normal behavior – no customisation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "ExecutionContext with empty annotation map"
                },
                {
                    "parameter": "customizer",
                    "value": "builder -> {} // no‑op"
                }
            ],
            "expected": "Annotation map contains exactly one JsonNodeAnnotation whose evaluationPath equals validator.evaluationPath, schemaLocation equals validator.schemaLocation and keyword equals validator.getKeyword(). No extra fields are present.",
            "description": "Verifies that putAnnotation creates a builder populated with the validator's evaluationPath, schemaLocation and keyword, applies a no‑op customiser and stores the built annotation in the execution context."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "ExecutionContext with pre‑filled annotation map containing a different key"
                },
                {
                    "parameter": "customizer",
                    "value": "builder -> {} // no‑op"
                }
            ],
            "expected": "Previous entries remain unchanged and a new entry (key = built annotation) is added.",
            "description": "Ensures that putAnnotation does not clear existing annotations but merely adds the new one."
        }
    ]
}