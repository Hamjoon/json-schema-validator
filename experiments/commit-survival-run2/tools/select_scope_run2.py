"""Run 2 scope and project-wide Dev population from frozen run 1 records."""
import csv
import json
import collections
from start import EXP, RESULTS, write_json
from select_scope import csvout


def main():
    survey = list(csv.DictReader((EXP/'population/class-survey.csv').open()))
    for row in survey:
        for key in ['m', 'd', 'c']:
            row[key] = int(row[key])
    audit_methods = list(csv.DictReader((EXP/'population/accessor-methods.tsv').open(), delimiter='\t'))
    counts = collections.Counter(r['class'] for r in audit_methods)
    accessors = collections.Counter(r['class'] for r in audit_methods if r['accessor'] == 'true')
    for row in survey:
        assert counts[row['class']] == row['m'], row
        row['accessors'] = accessors[row['class']]
        row['accessor_fraction'] = row['accessors'] / row['m']
        row['excluded_accessors'] = 'yes' if row['accessors'] * 2 > row['m'] else 'no'
    csvout(EXP/'population/accessor-audit.csv', survey, ['class','m','accessors','accessor_fraction','excluded_accessors'])
    full_ranked = sorted((r for r in survey if r['m'] >= 3), key=lambda r: (-r['c'], -r['m'], r['class']))
    ranked = [r for r in full_ranked if r['excluded_accessors'] == 'no']
    chosen = []
    cumulative = 0
    for row in ranked:
        if cumulative + row['m'] > 80:
            break
        chosen.append(row)
        cumulative += row['m']
        if cumulative >= 50 or len(chosen) == 10:
            break
    scope = dict(classes=chosen, m=cumulative, d=sum(r['d'] for r in chosen))
    write_json(EXP/'population/scope.json', scope)
    lines = ['# Run 2 selected scope', '',
             'Eligible: m >= 3, including abstract classes; no dedicated-test requirement or minimum class count. Exclude classes with more than half accessor methods. Accessors use the addendum name-prefix and single-return/single-field-assignment syntax, parsed by the JDK AST. Full method evidence: accessor-methods.tsv; all-class counts: accessor-audit.csv. Sort by c descending, m descending, then fully qualified class name. Stop upon cumulative m >= 50, before an addition would exceed 80, or at 10 classes.', '',
             f'Selected {len(chosen)} classes; cumulative m = {cumulative}.', '',
             '| Rank before exclusions | Class | c | m | Accessors | Excluded (>half) | Selected | Selected cumulative m |',
             '|---:|---|---:|---:|---:|---|---|---:|']
    total = 0
    for i, row in enumerate(full_ranked, 1):
        selected = row in chosen
        if selected:
            total += row['m']
        lines.append(f"| {i} | {row['class']} | {row['c']} | {row['m']} | {row['accessors']} | {row['excluded_accessors']} | {'yes' if selected else 'no'} | {total} |")
    lines += ['', 'Verification of the illustrative expected result: the frozen survey has AnyOfValidator and OneOfValidator at c=17, m=3; neither is accessor-dominated, so both rank before JsonSchemaFactory. The >=50 threshold is already reached when JsonSchemaFactory is added. JsonMetaSchema has c=14 in the frozen survey (not 13). The algorithm, rather than the illustrative expected list, determines this scope.', '']
    (EXP/'population/scope.md').write_text('\n'.join(lines))
    dev = [r for r in csv.DictReader((EXP/'dev/dev-methods-all.csv').open()) if r['stable'] == 'yes']
    assert len(dev) == 185, len(dev)
    assert len({(r['class'], r['method']) for r in dev}) == len(dev)
    csvout(EXP/'dev/dev-denominator.csv', dev, ['class', 'method', 'kind'])
    inventory = json.loads((RESULTS/'source-inventory.json').read_text())
    names = {r['class'] for r in chosen}
    methods = [r for r in inventory['main']['methods'] if r['eligible'] and r['class'] in names]
    assert len(methods) == cumulative
    csvout(EXP/'population/methods.csv', methods, ['class', 'method', 'signature', 'line'])
    result = dict(classes=len(chosen), m=cumulative, developer_denominator=len(dev),
                  eligible_classes=len(ranked), mandatory_stop=False,
                  stop_reason=None,
                  model_calls=0)
    write_json(RESULTS/'scope-selection.json', result)
    print(json.dumps(result), flush=True)


if __name__ == '__main__':
    main()
