package com.dmall.monitor.spiders;

import java.io.IOException;
import java.util.Properties;

import com.dmall.monitor.sdk.MonitorConfig;

/**
 * Hello world!
 *
 */
public class LightningSpiders 
{
    public static void main( String[] args ) {
		try {
			Properties properties = new Properties();
			properties.load(LightningSpiders.class.getResourceAsStream("/sys.properties"));
	    	String key = properties.get("key").toString().trim();
	    	String cookie = properties.get("cookie").toString().trim();
	    	int threshold = Integer.parseInt(properties.get("threshold").toString().trim());
	    	float rate = Float.parseFloat(properties.get("rate").toString().trim());
	    	int baikalInterval = Integer.parseInt(properties.get("baikal.interval").toString().trim());
			new MonitorConfig(properties.getProperty("dmc.projectCode"), properties.getProperty("dmc.appCode"), 
					Boolean.parseBoolean(properties.getProperty("dmc.startupMonitor"))).monitorInit();
	    	new BaikalWorker(key, cookie, threshold, rate, baikalInterval);
	    	int eosToFoThreshold = Integer.parseInt(properties.get("eosToFoThreshold").toString().trim());
	    	int foToEosThreshold = Integer.parseInt(properties.get("foToEosThreshold").toString().trim());
	    	int foInterval = Integer.parseInt(properties.get("fo.interval").toString().trim());
	    	new EosAndFoWorker(eosToFoThreshold, foToEosThreshold, foInterval);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }
}
