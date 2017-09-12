package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.Screenshots;
import com.crossbrowsertesting.api.Selenium;
import com.crossbrowsertesting.plugin.Constants;
import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;

public class CBTSeleniumStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    @StepContextParameter
    private transient Run<?,?> run;
    @Inject(optional=true)
    private transient CBTSeleniumStep step;
    private BodyExecution body;
    private Selenium seleniumApi = new Selenium();
    private String username, authkey = "";

    public CBTSeleniumStepExecution(StepContext sc) {
        super(sc);
    }
    @Override
    protected Void run() throws Exception {
        Job<?, ?> job = run.getParent();
        CBTCredentials credentials = CBTCredentials.getCredentialsById(job, step.getCredentialsId());
        if (credentials == null) {
            throw new Exception("no credentials provided");
        } else {
            username = credentials.getUsername();
            authkey = credentials.getAuthkey();
            seleniumApi.setRequest(username, authkey);
        }
        String browserIconClass = seleniumApi.operatingSystems2.get(step.operatingSystem).browsers2.get(step.browser).getIconClass();
        String browserName = "";
        if (browserIconClass.equals("ie")) {
            browserName = "internet explorer";
        } else if (browserIconClass.equals("safari-mobile")) {
            browserName = "safari";
        } else {
            browserName = browserIconClass;
        }
        // set environment variables
        EnvVars env = run.getEnvironment();
        JSONArray browsers = new JSONArray();
        JSONObject browser = new JSONObject();
        browsers.put(browser);
        browser.put("operating_system", step.operatingSystem);
        browser.put("browser", step.browser);
        browser.put("resolution", step.resolution);
        browser.put("browserName", browserName);
        env.put(Constants.OPERATINGSYSTEM, step.operatingSystem);
        env.put(Constants.BROWSER, step.browser);
        env.put(Constants.RESOLUTION, step.resolution);
        env.put(Constants.BROWSERNAME, browserName);
        env.put(Constants.BROWSERS, browsers.toString());
        env.put(Constants.USERNAME, username);
        env.put(Constants.APIKEY, authkey); // for legacy
        env.put(Constants.AUTHKEY, authkey);
        String buildname = run.getFullDisplayName().substring(0, run.getFullDisplayName().length()-(String.valueOf(run.getNumber()).length()+1));
        env.put(Constants.BUILDNAME, buildname);
        env.put(Constants.BUILDNUMBER, String.valueOf(run.getNumber()));
        return null;
    }
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        if (body!=null) {
            body.cancel(cause);
        }
    }
}
