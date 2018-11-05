package com.dmall.monitor.spiders;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;
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
	
	private int foToEosThreshold;
	
	private int afToFoThreshold;
	
	private int foToAfThreshold;
	
	private String types;
	
	private DruidDataSource druidDataSource;
	
	private SimpleDateFormat simpleDateFormat;
	
	//全局最大高度
	private int golabBlockNum;
	
	public EosAndFoWorker(int eosToFoThreshold, int foToEosThreshold, int afToFoThreshold, int foToAfThreshold, 
			DruidDataSource druidDataSource, String types, int interval) {
		this.eosToFoThreshold = eosToFoThreshold;
		this.foToEosThreshold = foToEosThreshold;
		this.afToFoThreshold = afToFoThreshold;
		this.foToAfThreshold = foToAfThreshold;
		this.druidDataSource = druidDataSource;
		this.types = types;
		simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		golabBlockNum = 0;
		new Timer().schedule(this, 0, interval * 1000);
	}
	
	public void run() {
		try {
			boolean isAlert = false;
			StringBuffer sb = new StringBuffer();
			String newType = null;
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
						String fromContract = quantity.getString("contract");
						String toContract = tosym.getString("contract");
						String toContent = tosym.getString("sym");
						int fromOffset;
						int toOffset;
						if((fromOffset = fromContent.indexOf(" ")) > -1 && (toOffset = toContent.indexOf(",")) > -1) {
							float number = Float.parseFloat(fromContent.substring(0, fromOffset));
							String from = fromContent.substring(fromOffset + 1);
							String to = toContent.substring(toOffset + 1);
							Date date = simpleDateFormat.parse(content.getString("timestamp").replace("T", " "));
							String dateTime = simpleDateFormat.format(date.getTime() + 8 * 60 * 60 * 1000);
							if((from.toLowerCase().equals("eos") && to.toLowerCase().equals("fo") && number > eosToFoThreshold) ||
									(from.toLowerCase().equals("fo") && to.toLowerCase().equals("eos") && number > foToEosThreshold) ||
									(from.toLowerCase().equals("af") && to.toLowerCase().equals("fo") && number > afToFoThreshold) ||
									(from.toLowerCase().equals("fo") && to.toLowerCase().equals("af") && number > foToAfThreshold)) {
								sb.append("账户").append(data.getString("owner")).append("在").append(dateTime)
									.append("将").append(number).append("个").append(from).append("兑换到").append(to).append("\n");
								isAlert = true;
							}
							if(!(fromContract.equals("lixunlixunli") || toContract.equals("lixunlixunli"))) {
								if(types.indexOf("," + from.toLowerCase() + ",") == -1) {
									types += from.toLowerCase() + ",";			
									newType = from + ",";
								}
								if(types.indexOf("," + to.toLowerCase() + ",") == -1) {
									types += to.toLowerCase() + ",";	
									newType = to + ",";
								}
							}
							if((from.toLowerCase().equals("af") && to.toLowerCase().equals("fo")) ||
									(from.toLowerCase().equals("fo") && to.toLowerCase().equals("af"))) {
								Connection connection = null;
								Statement statement = null;
								try {
									connection = druidDataSource.getConnection();
									statement = connection.createStatement();
									ResultSet resultSet = statement.executeQuery("select * from fo_and_af where transaction_id = '" + content.getString("transaction_id") + "'");
									if(!resultSet.next()) {
										statement.execute("INSERT INTO fo_and_af (transaction_id, block_num, owner, from_type, to_type, from_contract, to_contract, "
												+ "quantity, timestamp) VALUES ('" + content.getString("transaction_id") + "', " + content.getIntValue("block_num") 
												+ ", '" + data.getString("owner")  + "', '" + from + "', '" + to + "', '" + fromContract 
												+ "', '" + toContract + "', " + number + ", '" + dateTime + "' )");
									}
								} catch(Exception e) {
									logger.error("插入数据库异常:", e);
								} finally {
									if(statement != null) {
										statement.close();
									}
									if(connection != null) {
										connection.close();
									}
								}
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
			if(StringUtils.isNotEmpty(newType)) {
				logger.info("new type : {}", newType);
				List<UserInfo> userInfos = new ArrayList<UserInfo>();
				UserInfo duheng = new UserInfo();
				duheng.setPhone("18980868096");
				userInfos.add(duheng);
				UserInfo chenzhen = new UserInfo();
				chenzhen.setPhone("13308239343");
				userInfos.add(chenzhen);
				Monitor.alarm("603", "lightning type提醒新增类型" + newType + "请及时关注", userInfos);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
