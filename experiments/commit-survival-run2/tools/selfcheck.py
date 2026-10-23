"""Check scientific measurement edge cases without running model calls."""
from pathlib import Path
import tempfile
from start import EXP
from prepare_coverage import changed_lines
from run_commit import method_state,parse_reports


def main():
    before,after=changed_lines('--- a/src/main/java/A.java\n+++ b/src/main/java/A.java\n@@ -2,0 +3,2 @@\n+x\n+y\n@@ -9,1 +11,0 @@\n-z\n')
    assert before=={'src/main/java/A.java':[9]}
    assert after=={'src/main/java/A.java':[3,4]}
    before,after=changed_lines('--- /dev/null\n+++ b/src/main/java/New.java\n@@ -0,0 +1,3 @@\n+a\n+b\n+c\n')
    assert before=={} and after=={'src/main/java/New.java':[1,2,3]}
    work=EXP/'work/selfcheck';work.mkdir(parents=True,exist_ok=True)
    (work/'TEST-example.xml').write_text('''<testsuite><testcase classname="A" name="test(int)[1]"/><testcase classname="A" name="test(int)[2]"><failure type="org.opentest4j.AssertionFailedError" message="expected"/></testcase><testcase classname="A" name="skipped"><skipped/></testcase><testcase classname="B" name="works"/></testsuite>''')
    obs=parse_reports(work)
    assert method_state({'class':'A','method':'test'},obs,{'timeout':False})[0]=='fail'
    assert method_state({'class':'A','method':'skipped'},obs,{'timeout':False})[0]=='absent'
    assert method_state({'class':'B','method':'works'},obs,{'timeout':False})[0]=='pass'
    assert method_state({'class':'A','method':'missing'},obs,{'timeout':False})[0]=='absent'
    assert method_state({'class':'B','method':'works'},obs,{'timeout':True})[0]=='timeout'
    print('Passed: insertion/deletion coordinates, parameterized case aggregation, assertion failures, skipped/absent methods, and timeouts.')


if __name__=='__main__':main()
