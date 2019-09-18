package com.dmall.monitor.spiders;

import java.io.IOException;
import java.util.Properties;

import com.alibaba.druid.pool.DruidDataSource;
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
			DruidDataSource druidDataSource = new DruidDataSource();
			druidDataSource.setConnectProperties(properties);
	    	String key = properties.get("key").toString().trim();
	    	String cookie = properties.get("cookie").toString().trim();
	    	int threshold = Integer.parseInt(properties.get("threshold").toString().trim());
	    	float rate = Float.parseFloat(properties.get("rate").toString().trim());
	    	int baikalInterval = Integer.parseInt(properties.get("baikal.interval").toString().trim());
			new MonitorConfig(properties.getProperty("dmc.projectCode"), properties.getProperty("dmc.appCode"), 
					Boolean.parseBoolean(properties.getProperty("dmc.startupMonitor"))).monitorInit();
//	    	new BaikalWorker(key, cookie, threshold, rate, baikalInterval);
	    	int eosToFoThreshold = Integer.parseInt(properties.get("eosToFoThreshold").toString().trim());
	    	int foToEosThreshold = Integer.parseInt(properties.get("foToEosThreshold").toString().trim());
	    	int afToFoThreshold = Integer.parseInt(properties.get("afToFoThreshold").toString().trim());
	    	int foToAfThreshold = Integer.parseInt(properties.get("foToAfThreshold").toString().trim());
	    	int foInterval = Integer.parseInt(properties.get("fo.interval").toString().trim());
	    	String types = properties.get("types").toString().trim();
//	    	new EosAndFoWorker(eosToFoThreshold, foToEosThreshold, afToFoThreshold, foToAfThreshold, druidDataSource, types, foInterval);
	    	int bicoinInterval = Integer.parseInt(properties.get("bicoin.interval").toString().trim());
	    	float bicoinRate = Float.parseFloat(properties.get("bicoin.rate").toString().trim());
	    	new BiCoinlWorker(key, bicoinRate, bicoinInterval);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }
}
