package com.dmall.monitor.baikal;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
	
	public BaikalWorker(String key, String cookie, int threshold, int interval) {
		this.key = key;
		this.cookie = cookie;
		this.threshold = threshold;
		new Timer().schedule(this, 0, interval * 1000);
	}
	
	public void run() {
		try {
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
						String personal = result.getString("personal");
						if(personal != null) {
							result = JSON.parseObject(personal);
							String hashrate = result.getString("hashrate");
							logger.info("hashrate = {}", hashrate);
							if(Float.parseFloat(hashrate) < threshold) {
								List<UserInfo> userInfos = new ArrayList<UserInfo>();
								UserInfo duheng = new UserInfo();
								duheng.setPhone("18980868096");
								userInfos.add(duheng);
								UserInfo chenzhen = new UserInfo();
								chenzhen.setPhone("13308239343");
								userInfos.add(chenzhen);
								Monitor.alarm("601", "lightning baikal当前hashrate" + hashrate + "，小于" + threshold + "，请及时关注", userInfos);
							}
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
