package org.jenkinsci.plugins.cbt_jenkins.test;

import org.jenkinsci.plugins.cbt_jenkins.CBTBuildWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.jvnet.hudson.test.JenkinsRule;

public class CBTBuildWrapperTest {
    @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

    CBTBuildWrapper cbtBW;

    @After public void teardown() {
        if (cbtBW != null) {
            cbtBW = null;
        }
    }
}
