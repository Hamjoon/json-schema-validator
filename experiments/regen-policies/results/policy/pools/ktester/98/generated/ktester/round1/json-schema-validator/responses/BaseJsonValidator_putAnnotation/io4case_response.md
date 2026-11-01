{
    "group": "putAnnotation_Default",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "ExecutionContext instance with empty Annotations map"
                },
                {
                    "parameter": "customizer",
                    "value": "consumer that does nothing (builder) -> {}"
                }
            ],
            "expected": "Annotations map contains exactly one JsonNodeAnnotation whose evaluationPath equals validator.evaluationPath, schemaLocation equals validator.schemaLocation, keyword equals validator.getKeyword(), and all optional fields are null/default",
            "description": "Verifies that when the customizer does not modify the builder, the annotation stored contains only the automatically populated fields (evaluationPath, schemaLocation, keyword)."
        }
    ]
}