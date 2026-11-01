{
    "group": "customizer sets null defaultMetaSchemaIri",
    "cases": [
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V4"
                },
                {
                    "parameter": "customizer",
                    "value": "builder -> builder.defaultMetaSchemaIri(null)"
                }
            ],
            "expected": "NullPointerException (or IllegalArgumentException) thrown during builder.build because the defaultMetaSchemaIri is null.",
            "description": "The customiser explicitly sets the default meta‑schema IRI to null. The builder should reject this configuration when build() is called, propagating a NPE/IAE."
        }
    ]
}