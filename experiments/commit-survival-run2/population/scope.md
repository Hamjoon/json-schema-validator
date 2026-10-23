# Run 2 selected scope

Eligible: m >= 3, including abstract classes; no dedicated-test requirement or minimum class count. Exclude classes with more than half accessor methods. Accessors use the addendum name-prefix and single-return/single-field-assignment syntax, parsed by the JDK AST. Full method evidence: accessor-methods.tsv; all-class counts: accessor-audit.csv. Sort by c descending, m descending, then fully qualified class name. Stop upon cumulative m >= 50, before an addition would exceed 80, or at 10 classes.

Selected 5 classes; cumulative m = 64.

| Rank before exclusions | Class | c | m | Accessors | Excluded (>half) | Selected | Selected cumulative m |
|---:|---|---:|---:|---:|---|---|---:|
| 1 | com.networknt.schema.JsonSchema | 26 | 23 | 7 | no | yes | 23 |
| 2 | com.networknt.schema.SchemaValidatorsConfig | 17 | 57 | 37 | yes | no | 23 |
| 3 | com.networknt.schema.BaseJsonValidator | 17 | 15 | 4 | no | yes | 38 |
| 4 | com.networknt.schema.AnyOfValidator | 17 | 3 | 0 | no | yes | 41 |
| 5 | com.networknt.schema.OneOfValidator | 17 | 3 | 0 | no | yes | 44 |
| 6 | com.networknt.schema.JsonSchemaFactory | 15 | 20 | 9 | no | yes | 64 |
| 7 | com.networknt.schema.JsonMetaSchema | 14 | 15 | 8 | yes | no | 64 |
| 8 | com.networknt.schema.AllOfValidator | 14 | 3 | 0 | no | no | 64 |
| 9 | com.networknt.schema.ItemsValidator | 13 | 5 | 2 | no | no | 64 |
| 10 | com.networknt.schema.PrefixItemsValidator | 12 | 4 | 1 | no | no | 64 |
| 11 | com.networknt.schema.PropertiesValidator | 12 | 4 | 1 | no | no | 64 |
| 12 | com.networknt.schema.RefValidator | 11 | 5 | 1 | no | no | 64 |
| 13 | com.networknt.schema.ItemsValidator202012 | 11 | 4 | 1 | no | no | 64 |
| 14 | com.networknt.schema.AdditionalPropertiesValidator | 11 | 3 | 0 | no | no | 64 |
| 15 | com.networknt.schema.ValidationMessageHandler | 10 | 7 | 3 | no | no | 64 |
| 16 | com.networknt.schema.walk.WalkEvent | 9 | 12 | 10 | yes | no | 64 |
| 17 | com.networknt.schema.NotValidator | 9 | 3 | 0 | no | no | 64 |
| 18 | com.networknt.schema.TypeValidator | 9 | 3 | 1 | no | no | 64 |
| 19 | com.networknt.schema.ValidationMessage | 8 | 20 | 14 | yes | no | 64 |
| 20 | com.networknt.schema.ValidationContext | 8 | 15 | 6 | no | no | 64 |
| 21 | com.networknt.schema.IfValidator | 8 | 3 | 0 | no | no | 64 |
| 22 | com.networknt.schema.utils.JsonNodeUtil | 7 | 5 | 0 | no | no | 64 |
| 23 | com.networknt.schema.DependentSchemas | 7 | 3 | 0 | no | no | 64 |
| 24 | com.networknt.schema.CollectorContext | 6 | 13 | 3 | no | no | 64 |
| 25 | com.networknt.schema.walk.AbstractWalkListenerRunner | 5 | 3 | 0 | no | no | 64 |
| 26 | com.networknt.schema.walk.DefaultKeywordWalkListenerRunner | 5 | 3 | 1 | no | no | 64 |
| 27 | com.networknt.schema.SpecVersionDetector | 4 | 5 | 0 | no | no | 64 |
| 28 | com.networknt.schema.FormatKeyword | 4 | 4 | 2 | no | no | 64 |
| 29 | com.networknt.schema.JsonSchemaRef | 3 | 3 | 1 | no | no | 64 |
| 30 | com.networknt.schema.ValidationResult | 3 | 3 | 3 | yes | no | 64 |
| 31 | com.networknt.org.apache.commons.validator.routines.DomainValidator | 2 | 15 | 1 | no | no | 64 |
| 32 | com.networknt.schema.ValidatorState | 2 | 8 | 8 | yes | no | 64 |
| 33 | com.networknt.org.apache.commons.validator.routines.EmailValidator | 2 | 6 | 2 | no | no | 64 |
| 34 | com.networknt.org.apache.commons.validator.routines.InetAddressValidator | 2 | 4 | 2 | no | no | 64 |
| 35 | com.networknt.schema.utils.StringChecker | 2 | 3 | 1 | no | no | 64 |
| 36 | com.networknt.schema.ApplyDefaultsStrategy | 1 | 3 | 0 | no | no | 64 |
| 37 | com.networknt.schema.uri.ClasspathURLFactory | 1 | 3 | 0 | no | no | 64 |
| 38 | com.networknt.schema.uri.URISchemeFactory | 1 | 3 | 1 | no | no | 64 |
| 39 | com.networknt.schema.utils.UnicodeDatabase | 0 | 10 | 5 | no | no | 64 |
| 40 | com.networknt.org.apache.commons.validator.routines.RegexValidator | 0 | 5 | 1 | no | no | 64 |

Verification of the illustrative expected result: the frozen survey has AnyOfValidator and OneOfValidator at c=17, m=3; neither is accessor-dominated, so both rank before JsonSchemaFactory. The >=50 threshold is already reached when JsonSchemaFactory is added. JsonMetaSchema has c=14 in the frozen survey (not 13). The algorithm, rather than the illustrative expected list, determines this scope.
