package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.LocalTunnel;
import com.google.inject.Inject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;

public class CBTStepExecution extends AbstractStepExecutionImpl {
    @StepContextParameter private transient Run<?,?> run;
    @Inject(optional=true) private transient CBTStep step;
    private BodyExecution body;

    LocalTunnel tunnel = null;
    private String username, authkey = "";

    public CBTStepExecution(StepContext sc) {
        super(sc);
    }
    @Override
    public boolean start() throws Exception {
        Job<?,?> job = run.getParent();
        if (!(job instanceof TopLevelItem)) {
            throw new Exception(job + " must be a top-level job");
        }
        CBTCredentials credentials = CBTCredentials.getCredentialsById(job, step.credentialsId);

        if (credentials == null) {
            throw new Exception("no credentials provided");
        }else {
            username = credentials.getUsername();
            System.out.println("username = "+username);
            authkey = credentials.getAuthkey();
            System.out.println("authkey = "+authkey);
            if (step.useLocalTunnel) {
                tunnel = new LocalTunnel(username, authkey);
                tunnel.start(true);
            }
        }
        body = getContext().newBodyInvoker()
                .withContext(credentials)
                .withCallback(new CBTStepTailCall())
                .start();
        return false;
    }
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        if (body!=null) {
            body.cancel(cause);
        }
    }
    public class CBTStepTailCall extends BodyExecutionCallback.TailCall {
        @Override
        protected void finished(StepContext context) throws Exception {
            if (tunnel != null && tunnel.pluginStartedTheTunnel) {
                tunnel.stop();
            }
        }
    }
}