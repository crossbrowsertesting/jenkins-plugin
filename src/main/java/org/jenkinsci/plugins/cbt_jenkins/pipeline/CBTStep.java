package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.LocalTunnel;
import com.crossbrowsertesting.plugin.Constants;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

public class CBTStep extends AbstractStepImpl {
    public boolean useLocalTunnel = false;
    public String credentialsId,
            localTunnelPath,
            tunnelName = "";

    @DataBoundConstructor
    public CBTStep(boolean useLocalTunnel, String credentialsId) {
    //public CBTStep(boolean useLocalTunnel, String credentialsId, String localTunnelPath, String tunnelName) {
        this.useLocalTunnel = useLocalTunnel;
        this.credentialsId = credentialsId;
        //this.localTunnelPath = localTunnelPath;
        //this.tunnelName = tunnelName;
    }
    public static class CBTStepExecution extends AbstractStepExecutionImpl {
        @StepContextParameter private transient Run<?,?> run;
        @Inject(optional=true) private transient CBTStep step;

        private BodyExecution body;

        private transient LocalTunnel tunnel = null;
        private String username, authkey = "";

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
                    for (int i=1 ; i<15 && !tunnel.isTunnelRunning ; i++) {
                        //will check every 2 seconds for up to 30 to see if the tunnel connected
                        Thread.sleep(4000);
                        tunnel.queryTunnel();
                    }
                    if (tunnel.isTunnelRunning) {
                        System.out.println(Constants.TUNNEL_CONNECTED);
                    }else {
                        throw new Error(Constants.TUNNEL_START_FAIL);
                    }
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
                    System.out.println(Constants.TUNNEL_STOP);
                    tunnel.stop();
                }
            }
        }
    }
    @Extension
    public static final class CBTStepDescriptor extends AbstractStepDescriptorImpl {
        public CBTStepDescriptor() {
            super(CBTStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cbt";
        }
        @Override
        public String getDisplayName() {
            return "CrossBrowserTesting.com";
        }
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
            return CBTCredentials.fillCredentialsIdItems(context);
        }
        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
            return CBTCredentials.testCredentials(username, authkey);
        }
    }
}
