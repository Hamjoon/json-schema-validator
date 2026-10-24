"""Six-group reporting, with derived survival and measured coverage for Dev (scope)."""
import argparse
import run_commit
import aggregate_survival
import aggregate_coverage
import final_check
from run_dev_scope import denominator_for_group
from report_adjusted2 import wide_tables

GROUPS=['dev','dev-scope','ktester','ouedraogo','ouedraogo-adj','ouedraogo-adj2']

def main():
    parser=argparse.ArgumentParser();parser.add_argument('stage',choices=['survival','coverage','verify']);args=parser.parse_args()
    run_commit.GROUPS[:]=GROUPS
    final_check.denominator=denominator_for_group
    if args.stage=='survival':aggregate_survival.main()
    elif args.stage=='coverage':aggregate_survival.main();aggregate_coverage.main();wide_tables()
    else:final_check.main()

if __name__=='__main__':main()
