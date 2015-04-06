package com.xmlcalabash.testsuite;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.drivers.RunTestReport;
import com.xmlcalabash.util.DefaultTestReporter;
import com.xmlcalabash.util.SilentTestReporter;

/**
 * Created by ndw on 8/19/14.
 */
public class SuiteRunner {
    XProcConfiguration config = new XProcConfiguration("he", true);
    XProcRuntime runtime = new XProcRuntime(config);

    public void close() {
        runtime.close();
    }

    protected RunTestReport reporter = new RunTestReport(runtime, new DefaultTestReporter(runtime));

    public void runTest(String test) {
        RunTestReport.TestSuiteResults results = reporter.run(test);
        int pass = 0;
        int fail = 0;
        for (RunTestReport.TestSuiteResult result : results.getResults()) {
            if (result.passed()) {
                pass++;
            } else {
                fail++;
                result.report();
            }
        }

        if (pass == 1 && fail == 0) {
            System.err.println("PASS " + test);
            return;
        }

        if (fail > 0) {
            if (pass == 0 && fail == 1) {
                System.err.println("FAIL " + test);
                throw new RuntimeException("Test failed");
            } else {
                System.err.println("FAIL " + fail + "/" + (pass+fail) + " " + test);
                throw new RuntimeException("Some tests failed");
            }
        }

    }
}
