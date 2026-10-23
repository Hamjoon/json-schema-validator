"""Adapt copied Codec pre/post-image changed-line enumeration to all JSV files."""
import collections
import csv
import re
from start import RESULTS, git, write_json
from select_scope import csvout


def changed_lines(diff):
    pre,post=collections.defaultdict(set),collections.defaultdict(set)
    old,new=None,None
    for line in diff.splitlines():
        if line.startswith('--- '):old=line[4:].removeprefix('a/')
        elif line.startswith('+++ '):new=line[4:].removeprefix('b/')
        elif line.startswith('@@ '):
            match=re.match(r'@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@',line);assert match,line
            a,ac,b,bc=match.groups()
            if old!='/dev/null':pre[old].update(range(int(a),int(a)+int(ac or 1)))
            if new!='/dev/null':post[new].update(range(int(b),int(b)+int(bc or 1)))
    return {k:sorted(v) for k,v in pre.items()},{k:sorted(v) for k,v in post.items()}


def main():
    commits=list(csv.DictReader((RESULTS/'commits.csv').open()));indices={0}
    for row in commits:
        if row['changes-src-main']!='yes':continue
        i=int(row['index']);indices.update([i,i-1]);sha=row['sha'];parent=git('rev-parse',sha+'^1')
        # Preserve rename detection, as in the copied Codec coverage tool.
        diff=git('diff','--no-ext-diff','--find-renames','-U0',parent,sha,'--','src/main/java')
        pre,post=changed_lines(diff)
        write_json(RESULTS/f'changes/{i}.json',dict(index=i,sha=sha,parent=parent,pre_image=pre,post_image=post,diff=diff))
    csvout(RESULTS/'coverage-commits.csv',[dict(index=i) for i in sorted(indices)],['index'])
    print(dict(coverage_indices=len(indices),production_changes=sum(r['changes-src-main']=='yes' for r in commits)))


if __name__=='__main__':main()
