"""Run 2 scope and project-wide Dev population from frozen run 1 records."""
import csv
import json
from start import EXP, RESULTS, write_json
from select_scope import csvout


def main():
    survey = list(csv.DictReader((EXP/'population/class-survey.csv').open()))
    for row in survey:
        for key in ['m', 'd', 'c']:
            row[key] = int(row[key])
    ranked = sorted((r for r in survey if r['m'] >= 3), key=lambda r: (-r['c'], -r['m'], r['class']))
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
             'Eligible: m >= 3, including abstract classes; no dedicated-test requirement. Sort by c descending, m descending, then fully qualified class name. Stop upon cumulative m >= 50, before an addition would exceed 80, or at 10 classes.', '',
             f'Selected {len(chosen)} classes; cumulative m = {cumulative}.', '',
             '| Rank | Class | c | m | Selected | Selected cumulative m |',
             '|---:|---|---:|---:|---|---:|']
    total = 0
    for i, row in enumerate(ranked, 1):
        selected = row in chosen
        if selected:
            total += row['m']
        lines.append(f"| {i} | {row['class']} | {row['c']} | {row['m']} | {'yes' if selected else 'no'} | {total} |")
    stop = len(chosen) < 3
    if stop:
        lines += ['', 'The threshold is reached after the second class, exactly at the 80-method cap. A third eligible class would exceed that cap. The inherited run 1 stop condition (fewer than three selected classes) therefore applies. No class is skipped or added to evade either rule.', '']
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
                  eligible_classes=len(ranked), mandatory_stop=stop,
                  stop_reason='Inherited run 1: scope selects fewer than three classes' if stop else None,
                  model_calls=0)
    write_json(RESULTS/'scope-selection.json', result)
    print(json.dumps(result), flush=True)


if __name__ == '__main__':
    main()
