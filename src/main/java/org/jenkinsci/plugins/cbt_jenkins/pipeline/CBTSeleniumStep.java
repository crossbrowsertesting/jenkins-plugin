package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.ApiFactory;
import com.crossbrowsertesting.api.Selenium;
import com.crossbrowsertesting.configurations.Browser;
import com.crossbrowsertesting.configurations.OperatingSystem;
import com.crossbrowsertesting.configurations.Resolution;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class CBTSeleniumStep extends Step {
    private static String username, authkey = "";
    private static CBTCredentials credentials;

    private String credentialsId = "";
    public String operatingSystem, browser, resolution = "";

    @DataBoundConstructor
    public CBTSeleniumStep(String operatingSystem, String browser, String resolution) {
        this.operatingSystem = operatingSystem;
        this.browser = browser;
        this.resolution = resolution;
    }
    public void setCredentialsId(String credentialsId) {
        if (credentialsId != null && !credentialsId.isEmpty()) {
            this.credentialsId = credentialsId;
        }
    }
    public String getCredentialsId() {
        return this.credentialsId;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        try {
            setCredentialsId(credentials.getId());
            credentials = context.get(CBTCredentials.class);
            username = credentials.getUsername();
            authkey = credentials.getAuthkey();

        } catch (IOException | InterruptedException e) {}
        return new CBTSeleniumStepExecution(context);
    }
    @Extension
    public static final class CBTSeleniumStepDescriptor extends StepDescriptor {
        private Selenium seleniumApi = new Selenium();
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.<Class<?>>singleton(CBTCredentials.class);
        }
        @Override
        public String getFunctionName() {
            return "cbtSeleniumTest";
        }
        @Override
        public String getDisplayName() {
            return "Run a CrossbrowserTesting.com Selenium Test";
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
                } catch(NullPointerException npe) {} // no proxy credentials were set
                af.getRequest().setProxy(hostname, port);
                af.init();
            } catch(NullPointerException npe) {} // dont need to use a proxy
        }

        public ListBoxModel doFillOperatingSystemItems() {
            checkProxySettingsAndReloadRequest(seleniumApi);
            ListBoxModel items = new ListBoxModel();
            try {
                for (int i=0 ; i<seleniumApi.operatingSystems.size() ; i++) {
                    OperatingSystem config = seleniumApi.operatingSystems.get(i);
                    items.add(config.getName(), config.getApiName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillBrowserItems(@QueryParameter("operatingSystem") final String operating_system) {
            ListBoxModel items = new ListBoxModel();
            try {
                OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);
                for (int i=0 ; i<config.browsers.size() ; i++) {
                    Browser configBrowser = config.browsers.get(i);
                    items.add(configBrowser.getName(), configBrowser.getApiName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillResolutionItems(@QueryParameter("operatingSystem") final String operating_system) {
            ListBoxModel items = new ListBoxModel();
            try {
                OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);
                for (int i=0 ; i<config.resolutions.size() ; i++) {
                    Resolution configResolution = config.resolutions.get(i);
                    items.add(configResolution.getName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillnullItems() { //  catch for null values
            return new ListBoxModel();
        }
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
            return CBTCredentials.fillCredentialsIdItems(context);
        }
        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
            return CBTCredentials.testCredentials(username, authkey);
        }
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }
}
