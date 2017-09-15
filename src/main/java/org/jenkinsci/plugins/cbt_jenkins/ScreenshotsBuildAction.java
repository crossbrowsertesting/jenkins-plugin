package org.jenkinsci.plugins.cbt_jenkins;

import java.util.HashMap;
import java.util.logging.Logger;

public class ScreenshotsBuildAction  extends AbstractBuildAction {
	private final static Logger log = Logger.getLogger(ScreenshotsBuildAction.class.getName());

	/*
	 * Holds info about the Screenshots Test
	 */
	private String browserList,
				   url,
				   versionId,
				   downloadResultsZipPublicUrl,
			       loginProfile = "";
	
	private HashMap<String, String> testinfo;
	
	public ScreenshotsBuildAction(final boolean showLink, final String browserList, final String url) {
		super("Screenshots");
		log.entering(this.getClass().getName(), "contructor");
		this.showLink = showLink;
		log.finest("showLink: "+showLink);
    	this.browserList = browserList;
    	log.finest("browserList: "+browserList);
    	this.url = url;
    	log.finest("url: "+url);
		log.exiting(this.getClass().getName(), "contructor");
	}
	public String getBrowserList() {
		return browserList;
	}
	public String getUrl() {
		return url;
	}
	public String getVersionId() {
		return versionId;
	}
	public String getDownloadResultsZipPublicUrl() {
		return downloadResultsZipPublicUrl;
	}
	public void setLoginProfile(String loginProfile) {
		this.loginProfile = loginProfile;
	}
	public String getLoginProfile() {
		return loginProfile;
	}
	public HashMap<String, String> getTestinfo() {
		return testinfo;
	}
	public void setTestinfo(HashMap<String, String> info) {
		log.entering(this.getClass().getName(), "setTestinfo");
		this.testinfo = info;
		String testid = info.get("screenshot_test_id");
		setTestId(testid);
		log.finest("testid:" +getTestId());
		setTestPublicUrl(info.get("show_results_public_url"));
		log.finest("testPublicUrl: "+getTestPublicUrl());
    	if (this.showLink) {
    		log.finest("showLink: "+this.showLink);
    		if (info.containsKey("browser_list") && !info.get("browser_list").equals(browserList)) {
    			browserList = info.get("browser_list");
			}
			if (info.containsKey("url") && !info.get("url").equals(url)) {
				url = info.get("url");
			}
    		setDisplayName("CBT Screenshots Test (" + browserList + " " + url + ")");
    		log.finest("displayName: "+this.getDisplayName());
    		this.iconFileName = "/plugin/crossbrowsertesting/img/cbtlogo.png";
    		setTestUrl(testid); // using the test id as the results url because it is unique
    	}
		log.exiting(this.getClass().getName(), "setTestinfo");
	}
}
