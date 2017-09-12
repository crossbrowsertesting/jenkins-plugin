package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.Screenshots;
import com.google.inject.Inject;
import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;

import javax.annotation.Nonnull;

public class CBTScreenshotsStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    @StepContextParameter
    private transient Run<?,?> run;
    @Inject(optional=true)
    private transient CBTScreenshotsStep step;
    private BodyExecution body;
    Screenshots screenshotsApi;

    private String username, authkey = "";

    public CBTScreenshotsStepExecution(StepContext sc) {
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
        }
        return null;
    }
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        if (body!=null) {
            body.cancel(cause);
        }
    }
}