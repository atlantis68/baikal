package com.dmall.monitor.baikal;

import java.io.IOException;
import java.util.Properties;

import com.dmall.monitor.sdk.MonitorConfig;

/**
 * Hello world!
 *
 */
public class LightningBaikal 
{
    public static void main( String[] args ) {
		try {
			Properties properties = new Properties();
			properties.load(LightningBaikal.class.getResourceAsStream("/sys.properties"));
	    	String key = properties.get("key").toString().trim();
	    	String cookie = properties.get("cookie").toString().trim();
	    	int threshold = Integer.parseInt(properties.get("threshold").toString().trim());
	    	float rate = Float.parseFloat(properties.get("rate").toString().trim());
	    	int interval = Integer.parseInt(properties.get("interval").toString().trim());
			new MonitorConfig(properties.getProperty("dmc.projectCode"), properties.getProperty("dmc.appCode"), 
					Boolean.parseBoolean(properties.getProperty("dmc.startupMonitor"))).monitorInit();
	    	new BaikalWorker(key, cookie, threshold, rate, interval);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }
}
