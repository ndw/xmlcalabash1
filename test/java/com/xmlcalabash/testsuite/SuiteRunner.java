package com.xmlcalabash.testsuite;

import com.xmlcalabash.drivers.RunTestReport;
import org.junit.Test;

/**
 * Created by ndw on 8/19/14.
 */
public class SuiteRunner {
    protected RunTestReport reporter = new RunTestReport();

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
            return;
        }

        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0) {
            if (pass == 0 && fail == 1) {
                throw new RuntimeException("Test failed");
            } else {
                throw new RuntimeException("Some tests failed");
            }
        }

    }
}
