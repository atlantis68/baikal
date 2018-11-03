package com.dmall.monitor.spiders;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dmall.monitor.sdk.Monitor;
import com.dmall.monitor.sdk.UserInfo;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class BaikalWorker extends TimerTask {

	private static final Logger logger = LoggerFactory.getLogger(BaikalWorker.class);
	
	private String key;
	
	private String cookie;
	
	private int threshold;
	
	private float rate;
	
	private float poolHashrate;
	
	private float networkHashrate;
	
	public BaikalWorker(String key, String cookie, int threshold, float rate, int interval) {
		this.key = key;
		this.cookie = cookie;
		this.threshold = threshold;
		this.rate = rate;
		this.poolHashrate = 0;
		this.networkHashrate = 0;
		new Timer().schedule(this, 0, interval * 1000);
	}
	
	public void run() {
		try {
			boolean isAlert = false;
			StringBuffer sb = new StringBuffer();
			Request request = new Request.Builder()
			    .url("http://eu3.blakecoin.com/index.php?page=api&action=getdashboarddata&id=3815&api_key=" + key)
			    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
			    .addHeader("Accept-Encoding", "deflate, sdch")
			    .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
			    .addHeader("Cache-Control", "max-age=0")
			    .addHeader("Connection", "keep-alive")
			    .addHeader("Host", "eu3.blakecoin.com")
			    .addHeader("Cookie", cookie)
			    .build();
			Response response = HttpService.sendHttp(request);
			if(response != null && response.isSuccessful()) {
				JSONObject result = JSON.parseObject(response.body().string());
				String dashboard = result.getString("getdashboarddata");
				if(dashboard != null) {
					result = JSON.parseObject(dashboard);
					String data = result.getString("data");
					if(data != null) {
						result = JSON.parseObject(data);
						String raw = result.getString("raw");
						if(raw != null) {
							result = JSON.parseObject(raw);
							String personal = result.getString("personal");
							if(personal != null) {
								result = JSON.parseObject(personal);
								String tempHashrate = result.getString("hashrate");
								if(StringUtils.isNotEmpty(tempHashrate)) {
									Float hashrate = Float.parseFloat(tempHashrate);
									sb.append("personal hashrate : ").append(formatValue(hashrate, 1000000)).append("G, ");
									if(hashrate < threshold) {
										isAlert = true;
									}
								}
							}
							result = JSON.parseObject(raw);
							String pool = result.getString("pool");
							if(pool != null) {
								result = JSON.parseObject(pool);
								String tempHashrate = result.getString("hashrate");
								if(StringUtils.isNotEmpty(tempHashrate)) {
									Float hashrate = Float.parseFloat(tempHashrate);
									sb.append("pool hashrate : ").append(formatValue(poolHashrate, 1000000000))
										.append("T -> ").append(formatValue(hashrate, 1000000000)).append("T, ");
									if(poolHashrate == 0 || hashrate == 0 || Math.abs(hashrate - poolHashrate) / poolHashrate > rate) {
										isAlert = true;
									}
									poolHashrate = hashrate;
								}
							}
							result = JSON.parseObject(raw);
							String network = result.getString("network");
							if(network != null) {
								result = JSON.parseObject(network);
								String tempHashrate = result.getString("hashrate");
								if(StringUtils.isNotEmpty(tempHashrate)) {
									Float hashrate = Float.parseFloat(tempHashrate);
									sb.append("network hashrate : ").append(formatValue(networkHashrate, 1000000000))
										.append("T -> ").append(formatValue(hashrate, 1000000000)).append("T, ");
									if(networkHashrate == 0 || hashrate == 0 || Math.abs(hashrate - networkHashrate) / networkHashrate > rate) {
										isAlert = true;
									}
									networkHashrate = hashrate;
								}
							}
							logger.info("{}", sb.toString());
						}
					}
				}
			}
			if(isAlert) {
				List<UserInfo> userInfos = new ArrayList<UserInfo>();
				UserInfo duheng = new UserInfo();
				duheng.setPhone("18980868096");
				userInfos.add(duheng);
				UserInfo chenzhen = new UserInfo();
				chenzhen.setPhone("13308239343");
				userInfos.add(chenzhen);
				Monitor.alarm("601", "lightning baikal提醒" + sb.toString() + "请及时关注", userInfos);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private String formatValue(float value, int rate) {
		if(value > 0) {
			return String.format("%.2f", value/rate);		
		} 
		return String.format("%.2f", value);	
	}

}
