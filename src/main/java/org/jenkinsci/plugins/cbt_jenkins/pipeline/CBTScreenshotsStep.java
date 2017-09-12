package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.ApiFactory;
import com.crossbrowsertesting.api.Screenshots;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

public class CBTScreenshotsStep extends AbstractStepImpl {
    private static String username, authkey = "";
    private static Screenshots screenshotApi = null;
    private static CBTCredentials credentials;

    private String credentialsId = "";
    public String browserList, url = "";

    @DataBoundConstructor
    public CBTScreenshotsStep(String browserList, String url) {
        this.browserList = browserList;
        this.url = url;

    }
    public void setCredentialsId(String credentialsId) {
        System.out.println("screenshotsstep credentials = "+credentialsId);
        if (credentialsId != null && !credentialsId.isEmpty()) {
            this.credentialsId = credentialsId;
        }
    }
    public String getCredentialsId() {
        return this.credentialsId;
    }

    public static class CBTScreenshotsStepExecution extends AbstractStepExecutionImpl {
        @StepContextParameter private transient Run<?,?> run;
        @Inject(optional=true) private transient CBTScreenshotsStep step;
        private BodyExecution body;
        private transient Screenshots screenshotsApi;
        private transient String username, authkey = "";

        @Override
        public boolean start() throws Exception {
            Job<?, ?> job = run.getParent();
            CBTCredentials credentials = getContext().get(CBTCredentials.class);
            if (credentials == null) {
                    credentials = CBTCredentials.getCredentialsById(job, step.getCredentialsId());                if (credentials == null) {
                    throw new Exception("no credentials provided");
                } else {
                    step.setCredentialsId(credentials.getId());
                    username = credentials.getUsername();
                    authkey = credentials.getAuthkey();
                }
            } else {
                step.setCredentialsId(credentials.getId());
                username = credentials.getUsername();
                authkey = credentials.getAuthkey();
            }
            return false;
        }
        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            if (body!=null) {
                body.cancel(cause);
            }
        }
    }

    @Extension
    public static final class CBTScreenshotsStepDescriptor extends AbstractStepDescriptorImpl {
        public CBTScreenshotsStepDescriptor() {
            super(CBTScreenshotsStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cbtScreenshotsTest";
        }
        @Override
        public String getDisplayName() {
            return "Run a CrossbrowserTesting.com Screenshots Test";
        }
        public ListBoxModel doFillnullItems() {
            return new ListBoxModel();
        }
        public void checkProxySettingsAndReloadRequest(ApiFactory af) {
            // gets the proxy settings and reloads the Api Requests with them
            Jenkins jenkins = Jenkins.getInstance();
            try {
                String hostname = jenkins.proxy.name;
                int port = jenkins.proxy.port; // why is this throwing a null pointer if not set???
                try { // we'll do these too, just in case it throws a NPE too
                    String proxyUsername = jenkins.proxy.getUserName();
                    String proxyPassword = jenkins.proxy.getPassword();
                    if (proxyUsername != null && proxyPassword != null && !proxyUsername.isEmpty() && !proxyPassword.isEmpty()) {
                        af.getRequest().setProxyCredentials(proxyUsername, proxyPassword);
                    }
                } catch(NullPointerException npe) {
                    //System.out.println("no proxy credentials were set");
                } // no proxy credentials were set
                af.getRequest().setProxy(hostname, port);
                af.init();
            } catch(NullPointerException npe) {
                //System.out.println("dont need to use a proxy");
            } // dont need to use a proxy
        }
        public ListBoxModel doFillBrowserListItems(@QueryParameter("credentialsId") final String credentialsId) {
            CBTCredentials local_credentials = CBTCredentials.getCredentialsById(null, credentialsId);
            if (local_credentials != null) {
                credentials = local_credentials;
                username = local_credentials.getUsername();
                authkey = local_credentials.getAuthkey();
            }
            screenshotApi = new Screenshots(username, authkey);
            checkProxySettingsAndReloadRequest(screenshotApi);
            ListBoxModel items = new ListBoxModel();
            try {
                for (int i=0 ; i<screenshotApi.browserLists.size() ; i++) {
                    String browserList = screenshotApi.browserLists.get(i);
                    items.add(browserList);
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
            return CBTCredentials.fillCredentialsIdItems(context);
        }
        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
            return CBTCredentials.testCredentials(username, authkey);
        }
        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }
    }
}
