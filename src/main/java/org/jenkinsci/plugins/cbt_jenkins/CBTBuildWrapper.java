package org.jenkinsci.plugins.cbt_jenkins;

import com.crossbrowsertesting.api.Account;
import com.crossbrowsertesting.api.LocalTunnel;
import com.crossbrowsertesting.configurations.Browser;
import com.crossbrowsertesting.configurations.OperatingSystem;
import com.crossbrowsertesting.configurations.Resolution;
import com.crossbrowsertesting.plugin.Constants;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import net.sf.json.JSONObject;
import org.json.JSONArray;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class CBTBuildWrapper extends BuildWrapper implements Serializable {

	private LocalTunnel tunnel;
	private boolean useLocalTunnel;
	private boolean useTestResults;
	private boolean useNewSeleniumCaps;

	private List <JSONObject> seleniumTests,
    						  screenshotsTests;
    private String localTunnelPath,
    			   tunnelName,
    			   credentialsId,
    			   username,
    			   authkey = "";

    private final static Logger log = Logger.getLogger(CBTBuildWrapper.class.getName());

    @DataBoundConstructor
    public CBTBuildWrapper(List<JSONObject> screenshotsTests, List<JSONObject> seleniumTests, boolean useLocalTunnel, boolean useTestResults, boolean useNewSeleniumCaps, String credentialsId, String tunnelName, String localTunnelPath) {
    	/*
    	 * Instantiated when the configuration is saved
    	 * Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    	 */
    	log.entering(this.getClass().getName(), "constructor");
		setScreenshotsTests(screenshotsTests);
		setSeleniumTests(seleniumTests);
		setUseLocalTunnel(useLocalTunnel);
		setUseTestResults(useTestResults);

    	// reset the username and authkey for screenshots
		setCredentials(credentialsId);
    	//advanced options
		setTunnelName(tunnelName);
    	setLocalTunnelPath(localTunnelPath);
    	setUseNewSeleniumCaps(useNewSeleniumCaps);
		log.exiting(this.getClass().getName(), "constructor");
	}

    public List<JSONObject> getSeleniumTests() {
    	return this.seleniumTests;
    }
    public List<JSONObject> getScreenshotsTests() {
    	return this.screenshotsTests;
    }
    public boolean getUseLocalTunnel() {
    	return this.useLocalTunnel;
    }
    public boolean getUseTestResults() {
    	return this.useTestResults;
    }
    public String getLocalTunnelPath() {
    	return this.localTunnelPath;
    }
    public String getCredentialsId() {
    	return this.credentialsId;
    }
    public String getTunnelName() {
    	return this.tunnelName;
    }
    public boolean getUseNewSeleniumCaps() {
    	return this.useNewSeleniumCaps;
	}

    private void setSeleniumTests(List<JSONObject> seleniumTests) {
		if (seleniumTests == null) { // prevent null pointer
			log.finest("seleniumTests is null");
			this.seleniumTests = new LinkedList<JSONObject>();
		} else {
			log.finest("seleniumTests: "+seleniumTests.toString());
			this.seleniumTests = seleniumTests;
		}
	}
	private void setScreenshotsTests(List<JSONObject> screenshotsTests) {
		if (screenshotsTests == null) { // prevent null pointer
			log.finest("screenshotsTests is null");
			this.screenshotsTests = new LinkedList<JSONObject>();
		} else {
			this.screenshotsTests = screenshotsTests;
			log.finest("screenshotsTest: "+screenshotsTests.toString());
		}
	}
	private void setLocalTunnelPath(String localTunnelPath) {
		if (localTunnelPath == null) {
			this.localTunnelPath = "";
		} else {
			this.localTunnelPath = localTunnelPath;
		}
		log.finer("localTunnelPath: "+localTunnelPath);
	}
	private void setTunnelName(String tunnelName) {
		if (tunnelName == null) {
			this.tunnelName = "";
		} else {
			this.tunnelName = tunnelName;
		}
		log.finer("tunnelName: "+tunnelName);
	}
	private void setUseLocalTunnel(boolean useLocalTunnel) {
		try { // prevent null pointer
			if (useLocalTunnel != true) {
				this.useLocalTunnel = false;
			} else {
				this.useLocalTunnel = useLocalTunnel;
			}
		} catch (NullPointerException npe) {
			this.useLocalTunnel = false;
		}
	}
	private void setUseTestResults(boolean useTestResults) {
		try { // prevent null pointer
			if (useTestResults != true) {
				this.useTestResults = false;
			} else {
				this.useTestResults = useTestResults;
			}
		} catch (NullPointerException npe) {
			this.useTestResults = false;
		}
	}
	private void setUseNewSeleniumCaps(boolean useNewSeleniumCaps) {
		try { // prevent null pointer
			if (useNewSeleniumCaps != true) {
				this.useNewSeleniumCaps = false;
			} else {
				this.useNewSeleniumCaps = useNewSeleniumCaps;
			}
		} catch (NullPointerException npe) {
			this.useNewSeleniumCaps = false;
		}
	}
	private void setCredentials(String credentialsId) {
		this.credentialsId = credentialsId;
		log.fine("about to get credentials");
		final CBTCredentials credentials = CBTCredentials.getCredentials(null, credentialsId);
		log.fine("got credentials");
		this.username = credentials.getUsername();
		this.authkey = credentials.getAuthkey();
		log.fine("setting credentials");
		getDescriptor().setBuildCredentials(username, authkey);
	}

	private void startLocalTunnel(final BuildListener listener) throws InterruptedException {
		// tunnel stuff
		if (!tunnelName.isEmpty() && useLocalTunnel) {
			listener.getLogger().println(Constants.TUNNEL_USING_TUNNELNAME(tunnelName));
			tunnel = new LocalTunnel(username, authkey, tunnelName);
		}else if(tunnelName.isEmpty() && useLocalTunnel){
			listener.getLogger().println(Constants.TUNNEL_USING_DEFAULT);
			tunnel = new LocalTunnel(username, authkey);
		}
		if (useLocalTunnel) {
			tunnel.queryTunnel();
			getDescriptor().checkProxySettingsAndReloadRequest(tunnel);
			if (!tunnel.isTunnelRunning) {
				listener.getLogger().println(Constants.TUNNEL_NEED_TO_START);
				try {
					if (localTunnelPath != null && localTunnelPath.equals("")) {
						log.fine("using embedded local tunnel");
						tunnel.start(true);
					} else {
						log.fine("using specified local tunnel");
						tunnel.start(localTunnelPath);
					}
					listener.getLogger().println(Constants.TUNNEL_WAITING);
					for (int i=1 ; i<15 && !tunnel.isTunnelRunning ; i++) {
						//will check every 2 seconds for upto 30 to see if the tunnel connected
						TimeUnit.SECONDS.sleep(4);
						tunnel.queryTunnel();
					}
					if (tunnel.isTunnelRunning) {
						listener.getLogger().println(Constants.TUNNEL_CONNECTED);
					}else {
						throw new Error(Constants.TUNNEL_START_FAIL);
					}
				}catch (URISyntaxException | IOException e) {
					log.finer("err: "+e);
					throw new Error(Constants.TUNNEL_START_FAIL);
				}
			}else {
				listener.getLogger().println(Constants.TUNNEL_NO_NEED_TO_START);
			}
		}
	}
	private void startScreenshotsTest(final AbstractBuild build, final BuildListener listener) throws InterruptedException {
		// screenshots stuff
		if (screenshotsTests != null && !screenshotsTests.isEmpty()) {
			Iterator<JSONObject> screenshotsIterator = screenshotsTests.iterator();
			while(screenshotsIterator.hasNext()) {
				JSONObject ssTest = screenshotsIterator.next();
				String screenshotsBrowserList = ssTest.getString("browserList");
				if (screenshotsBrowserList.equals("**SELECT A BROWSERLIST*") || screenshotsBrowserList.isEmpty()) {
					screenshotsBrowserList = "";
				}
				String screenshotsLoginProfile = ssTest.getString("loginProfile");
				boolean useLoginProfile = true;
				if (screenshotsLoginProfile.equals("**SELECT A LOGIN PROFILE / SELENIUM SCRIPT**") || screenshotsLoginProfile.isEmpty()) {
					useLoginProfile = false;
					screenshotsLoginProfile = "";
				}

				String screenshotsUrl = ssTest.getString("url");
				HashMap<String, String> screenshotTestResultsInfo = new HashMap<String, String>();
				boolean screenshotsTestStarted = false;
				for (int i=1; i<=12 && !screenshotsTestStarted;i++) { // in windows it takes 4 -5 attempts before the screenshots test begins
					if (useLoginProfile) {
						screenshotTestResultsInfo = getDescriptor().screenshotApi.runScreenshotTest(screenshotsBrowserList, screenshotsUrl, screenshotsLoginProfile);
					}else {
						screenshotTestResultsInfo = getDescriptor().screenshotApi.runScreenshotTest(screenshotsBrowserList, screenshotsUrl);
					}
					if (screenshotTestResultsInfo.containsKey("screenshot_test_id") && screenshotTestResultsInfo.get("screenshot_test_id") != null) {
						log.info("screenshot test started: "+ screenshotTestResultsInfo.get("screenshot_test_id"));
						screenshotsTestStarted = true;
					} else {
						log.info("screenshot test did not start... going to try again: "+ i);
						TimeUnit.SECONDS.sleep(10);
					}
				}
				if (screenshotTestResultsInfo.containsKey("error")) {
					listener.getLogger().println("[ERROR] 500 error returned for Screenshot Test");
				} else {
					screenshotTestResultsInfo.put("browser_list", screenshotsBrowserList);
					screenshotTestResultsInfo.put("url", screenshotsUrl);
					ScreenshotsBuildAction ssBuildAction = new ScreenshotsBuildAction(useTestResults, screenshotsBrowserList, screenshotsUrl);
					ssBuildAction.setBuild(build);
					ssBuildAction.setTestinfo(screenshotTestResultsInfo);
					ssBuildAction.setLoginProfile(screenshotsLoginProfile);
					build.addAction(ssBuildAction);
					if (!screenshotTestResultsInfo.isEmpty()) {
						listener.getLogger().println("\n-----------------------");
						listener.getLogger().println("SCREENSHOT TEST RESULTS");
						listener.getLogger().println("-----------------------");
					}
					for (Map.Entry<String, String> screenshotResultsEntry : screenshotTestResultsInfo.entrySet()) {
						listener.getLogger().println(screenshotResultsEntry.getKey() + ": "+ screenshotResultsEntry.getValue());
					}
				}
			}
		}
	}

    @SuppressWarnings("rawtypes")
	@Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
    	/*
    	 * called when you the build runs
    	 */
		log.entering(this.getClass().getName(), "setup");
		log.fine("about to get the credentials using the credentialId");
        final CBTCredentials credentials = CBTCredentials.getCredentials(build.getProject(), credentialsId);
        this.username = credentials.getUsername();
        this.authkey = credentials.getAuthkey();
        //track install
        Account account = new Account(username, authkey);
        account.init();
        log.fine("testing the credentials");
        if (account.connectionSuccessful) {
        	log.fine("sending a mixpanel event");
    		account.sendMixpanelEvent("Jenkins Plugin Downloaded");
        }
        
        getDescriptor().seleniumApi.setRequest(username, authkey); // add credentials to requests

		startLocalTunnel(listener);
		startScreenshotsTest(build, listener);

    	return new CBTEnvironment(build);
    }

    @Override
    public CBTDescriptor getDescriptor() {
    	return (CBTDescriptor) super.getDescriptor();
    }


	@SuppressWarnings("rawtypes")
    private class CBTEnvironment extends BuildWrapper.Environment {
		private AbstractBuild build;
    	private CBTEnvironment(final AbstractBuild build) {
    		this.build = build;
    	}
    	private void makeSeleniumBuildActionFromJSONObject(JSONObject config) {
    		log.entering(this.getClass().getName(), "makeSeleniumBuildActionFromJSONObject");
    		log.finest("JSONObject: "+config.toString());
    		String buildname = build.getFullDisplayName().substring(0, build.getFullDisplayName().length()-(String.valueOf(build.getNumber()).length()+1));
			String buildnumber = String.valueOf(build.getNumber());
			String operatingSystemApiName = config.getString("operating_system");
			String browserApiName = config.getString("browser");
			String resolution = config.getString("resolution");
	    	SeleniumBuildAction seBuildAction = new SeleniumBuildAction(useTestResults, operatingSystemApiName, browserApiName, resolution);
	    	log.fine("created selenium build action for: "+seBuildAction.displayName);
	    	seBuildAction.setBuild(build);
	    	seBuildAction.setBuildName(buildname);
	    	seBuildAction.setBuildNumber(buildnumber);
	    	log.fine("adding build action");
	    	build.addAction(seBuildAction);
			log.exiting(this.getClass().getName(), "makeSeleniumBuildActionFromJSONObject");

		}
    	private JSONObject addBrowserNameToJSONObject(JSONObject config) {
			String operatingSystemApiName = config.getString("operating_system");
			String browserApiName = config.getString("browser");
			// Javascript Selenium Tests have an extra capability "browserName"
			String browserIconClass = getDescriptor().seleniumApi.operatingSystems2.get(operatingSystemApiName).browsers2.get(browserApiName).getIconClass();
			String browserName = "";
			if (browserIconClass.equals("ie")) {
				browserName = "internet explorer";
			} else if (browserIconClass.equals("safari-mobile")) {
				browserName = "safari";
			} else {
				browserName = browserIconClass;
			}
			config.put("browserName", browserName);
			return config;
    	}
    	@Override
    	public void buildEnvVars(Map<String, String> env) {
    		/*
    		 * runs for every build step
    		 */
    		log.entering(this.getClass().getName(), "buildEnvVars");
    		// selenium environment variables
    		String buildname = build.getFullDisplayName().substring(0, build.getFullDisplayName().length()-(String.valueOf(build.getNumber()).length()+1));
			String buildnumber = String.valueOf(build.getNumber());
    		JSONArray browsers = new JSONArray();
    		log.finest("seleniumTests.size(): "+seleniumTests.size());
    		for (JSONObject config : seleniumTests) {
    			System.out.println(config);
    			if (useNewSeleniumCaps) {
					String operatingSystemApiName = config.getString("operating_system");
					String browserApiName = config.getString("browser");
					String resolution = config.getString("resolution");
					OperatingSystem os = getDescriptor().seleniumApi.operatingSystems2.get(operatingSystemApiName);
					System.out.println(os.getName());
					System.out.println(os.getPlatform());
					System.out.println(os.getPlatformName());
					System.out.println(os.getPlatformVersion());

					Browser browser = os.browsers2.get(browserApiName);
					Resolution res = os.resolutions2.get(resolution);
					JSONObject j_browser = new JSONObject();
					j_browser.put("browserName", getDescriptor().seleniumApi.operatingSystems2.get(operatingSystemApiName).browsers2.get(browserApiName).getBrowserName());
					System.out.println("os.isMobile()" + getDescriptor().seleniumApi.operatingSystems2.get(operatingSystemApiName).isMobile());
					j_browser.put("isMobile", os.isMobile());
					if (os.isMobile()) {
						j_browser.put("deviceName", os.getDeviceName());
						j_browser.put("platformVersion", os.getPlatformVersion());
						j_browser.put("platformName", os.getPlatformName());
						j_browser.put("deviceOrientation", res.getDeviceOrientation());
					} else {
						j_browser.put("version", browser.getVersion());
						j_browser.put("platform", os.getPlatform());
						j_browser.put("screenResolution", res.getScreenResolution());
					}
					browsers.put(j_browser);

				} else {
					config = addBrowserNameToJSONObject(config);
					//makeSeleniumBuildActionFromJSONObject(config);
					browsers.put(config);
				}
    		}

        	if ( seleniumTests.size() == 1 ){
        		JSONObject seTest = seleniumTests.get(0);
				String operatingSystemApiName = seTest.getString("operating_system");
				String browserApiName = seTest.getString("browser");
				String resolution = seTest.getString("resolution");
        		if (useNewSeleniumCaps) {
        			OperatingSystem os = getDescriptor().seleniumApi.operatingSystems2.get(operatingSystemApiName);
					Browser browser = os.browsers2.get(browserApiName);
					Resolution res = os.resolutions2.get(resolution);
					env.put(Constants.BROWSERNAME, browser.getBrowserName());
					env.put("CBT_ISMOBILE", String.valueOf(os.isMobile()));
					if (os.isMobile()) {
						env.put("CBT_DEVICENAME", os.getDeviceName());
						env.put("CBT_PLATFORMVERSION", os.getPlatformVersion());
						env.put("CBT_PLATFORMNAME", os.getPlatformName());
						env.put("CBT_DEVICEORIENTATION", res.getDeviceOrientation());
					} else {
						env.put("CBT_VERSION", browser.getVersion());
						env.put("CBT_PLATFORM", os.getPlatform());
						env.put("CBT_SCREENRESOLUTION", res.getScreenResolution());
					}
				} else {
					String browserName = seTest.getString("browserName");
					env.put(Constants.OPERATINGSYSTEM, operatingSystemApiName);
					env.put(Constants.BROWSER, browserApiName);
					env.put(Constants.RESOLUTION, resolution);
					env.put(Constants.BROWSERNAME, browserName);
				}
        	}
    		env.put(Constants.BROWSERS, browsers.toString());
    		env.put(Constants.USERNAME, username);
    		env.put(Constants.APIKEY, authkey); // for legacy
    		env.put(Constants.AUTHKEY, authkey);
    		env.put(Constants.BUILDNAME, buildname);
    		env.put(Constants.BUILDNUMBER, buildnumber);
    		super.buildEnvVars(env);
			log.exiting(this.getClass().getName(), "buildEnvVars");
		}
    	@Override
    	public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
    		/*
    		 * Runs after the build
    		 */
    		log.entering(this.getClass().getName(), "teardown" );
			for (JSONObject config : seleniumTests) {
				makeSeleniumBuildActionFromJSONObject(config);
			}

			HashMap<String, Queue<Map<String, String>>> seleniumEnvironments = new HashMap<String, Queue<Map<String, String>>>();
			for (SeleniumBuildAction se : build.getActions(SeleniumBuildAction.class)) {
				log.fine("found a selenium build action");
				// this is to catch a user that puts the same configuration more than once
				// instead of making the call to "/selenium" multiple times, it only calls it once and reuses the results
				String key = se.getBrowser()+se.getOperatingSystem()+se.getResolution();

				String buildName = se.getBuildName();
				log.fine("buildName: "+buildName);
				String buildNumber = se.getBuildNumber();
				log.fine("buildNumber: "+buildNumber);
				String browserApiName = se.getBrowser();
				log.fine("browserApiName: "+browserApiName);
				String osApiName = se.getOperatingSystem();
				log.fine("osApiName: "+osApiName);
				String resolution = se.getResolution();
				log.fine("resolution: "+resolution);

				if (!seleniumEnvironments.containsKey(key)) {
					log.fine("going to get the selenium test info");
					Queue<Map<String, String>> tests = getDescriptor().seleniumApi.getSeleniumTestInfo2(buildName, buildNumber, browserApiName, osApiName, resolution);
					seleniumEnvironments.put(key, tests);
				}
				Map<String, String> testInfo = seleniumEnvironments.get(key).poll();
				String seleniumTestId = "";
				String publicUrl = "";
				try {
					seleniumTestId = testInfo.get("selenium_test_id");
					log.fine("seleniumTestId: "+ seleniumTestId);
					publicUrl = testInfo.get("show_result_public_url");
					log.fine("publicUrl: "+publicUrl);
				}catch (NullPointerException npe) {
					log.fine("got null pointer from seleniumTestId or publicUrl");
				}

				se.setTestId(seleniumTestId);
				se.setTestPublicUrl(publicUrl);
				se.setTestUrl(seleniumTestId);
				if(seleniumTestId == null || seleniumTestId.isEmpty()) {
					// lets get a phony test id for the contributor if we cant find one for some reason
					seleniumTestId = getDescriptor().seleniumApi.getSeleniumTestId(buildName, buildNumber, browserApiName, osApiName, resolution);
				}
				String jenkinsVersion = build.getHudsonVersion();
				String pluginVersion = getDescriptor().getVersion();
				getDescriptor().seleniumApi.updateContributer(seleniumTestId, Constants.JENKINS_CONTRIBUTER, jenkinsVersion, pluginVersion);
			}
			// we need to wait for the screenshots tests to finish (definitely before closing the tunnel)
			boolean isAtLeastOneScreenshotTestActive;
			int count = 1;
			do {
				isAtLeastOneScreenshotTestActive = false;
				for (ScreenshotsBuildAction ss : build.getActions(ScreenshotsBuildAction.class)) {
					log.fine("found a screenshot test");
					// checks each screenshot_test_id to see if the test is finished
					if(getDescriptor().screenshotApi.testIsRunning(ss.getTestId())) {
						if (count == 1) { // no need to spam the logs with this print statement
							listener.getLogger().println("waiting for screenshots tests to finish...");
						}
						isAtLeastOneScreenshotTestActive = true;
					}
					Thread.sleep(30000);
				}
				count++;
				// if any of the tests say they are still running. try again
			}while(isAtLeastOneScreenshotTestActive);
			if (tunnel != null && tunnel.pluginStartedTheTunnel) {
    			for (int i=1 ; i<20 && tunnel.isTunnelRunning; i++) {
					try {
						log.info("about to kill the tunnel using the api");
						tunnel.stop();
						log.info("done killing the tunnel using the api");
					}catch(IOException ioe) {
						// most likely got a bad gateway but we're going to delete the cbt_tunnel binary on exit anyway so the tunnel will still be killed
						log.warning("got IOException while killing the tunnel");
					}
    				//will check every 15 seconds for up to 5 minutes to see if the tunnel disconnected
					log.fine("waiting for the tunnel to die");
    				Thread.sleep(15000);
    				boolean tunnelIsRunning = tunnel.queryTunnel();
    				log.fine("tunnelIsRunning: "+tunnelIsRunning);
    				log.fine("tunnel.isTunnelRunning: "+tunnel.isTunnelRunning);
    			}
    			if (!tunnel.isTunnelRunning) {
    				listener.getLogger().println(Constants.TUNNEL_STOP);
    			} else {
    				listener.getLogger().println(Constants.TUNNEL_STOP_FAIL);
    			}
			}
			return true;
    	}
    }
}
