package com.dmall.monitor.spiders;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dmall.monitor.sdk.Monitor;
import com.dmall.monitor.sdk.UserInfo;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class EosAndFoWorker extends TimerTask {

	private static final Logger logger = LoggerFactory.getLogger(EosAndFoWorker.class);
	
	private int eosToFoThreshold;
	
	private float foToEosThreshold;
	
	private SimpleDateFormat simpleDateFormat;
	
	//全局最大高度
	private int golabBlockNum;
	
	public EosAndFoWorker(int eosToFoThreshold, int foToEosThreshold, int interval) {
		this.eosToFoThreshold = eosToFoThreshold;
		this.foToEosThreshold = foToEosThreshold;
		simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		golabBlockNum = 0;
		new Timer().schedule(this, 0, interval * 1000);
	}
	
	public void run() {
		try {
			boolean isAlert = false;
			StringBuffer sb = new StringBuffer();
			Request request = new Request.Builder()
			    .url("http://explorer.fibos.rocks/api/contractTraces?contract=eosio.token&action=exchange&page=0")
			    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
			    .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
			    .addHeader("Cache-Control", "max-age=0")
			    .addHeader("Connection", "keep-alive")
			    .addHeader("Host", "explorer.fibos.rocks")
			    .build();
			Response response = HttpService.sendHttp(request);
			if(response != null && response.isSuccessful()) {
				JSONArray results = JSON.parseArray(response.body().string());
				//本次获取获取到的最大高度
				int curBlockNum = 0;
				for(Object result : results) {
					JSONObject content = JSON.parseObject("" + result);
					if(content.getIntValue("block_num") > golabBlockNum) {
						curBlockNum = content.getIntValue("block_num") > curBlockNum ? content.getIntValue("block_num") : curBlockNum;
						JSONObject data = content.getJSONObject("data");
						JSONObject quantity = data.getJSONObject("quantity");
						JSONObject tosym = data.getJSONObject("tosym");
						String fromContent = quantity.getString("quantity");
						String toContent = tosym.getString("sym");
						int fromOffset;
						int toOffset;
						if((fromOffset = fromContent.indexOf(" ")) > -1 && (toOffset = toContent.indexOf(",")) > -1) {
							float number = Float.parseFloat(fromContent.substring(0, fromOffset));
							String from = fromContent.substring(fromOffset + 1);
							String to = toContent.substring(toOffset + 1);
							if((from.toLowerCase().equals("eos") && to.toLowerCase().equals("fo") && number > eosToFoThreshold) ||
									(from.toLowerCase().equals("fo") && to.toLowerCase().equals("eos") && number > foToEosThreshold)) {
								Date date = simpleDateFormat.parse(content.getString("timestamp").replace("T", " "));
								sb.append("账户").append(data.getString("owner")).append("在").append(simpleDateFormat.format(date.getTime() + 8 * 60 * 60 * 1000))
									.append("将").append(number).append("个").append(from).append("兑换到").append(to).append("\n");					
							}
						}
					}
				}
				logger.info("global height : {}, current height : {}", golabBlockNum, curBlockNum);
				golabBlockNum = curBlockNum > golabBlockNum ? curBlockNum : golabBlockNum;
			}
			if(isAlert) {
				List<UserInfo> userInfos = new ArrayList<UserInfo>();
				UserInfo duheng = new UserInfo();
				duheng.setPhone("18980868096");
				userInfos.add(duheng);
				UserInfo chenzhen = new UserInfo();
				chenzhen.setPhone("13308239343");
				userInfos.add(chenzhen);
				Monitor.alarm("602", "lightning exchange提醒" + sb.toString() + "请及时关注", userInfos);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
