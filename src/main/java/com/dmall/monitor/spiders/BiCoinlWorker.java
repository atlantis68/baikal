package com.dmall.monitor.spiders;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class BiCoinlWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(BiCoinlWorker.class);
	
	private Map<String, User> users;
	
	private float bicoinRate;
	
	private int interval;
	
	private Random random;
	
	public BiCoinlWorker(String key, float bicoinRate, int interval) {
		this.bicoinRate = bicoinRate;
		this.interval = interval;
		users = new HashMap<String, User>();
		random = new Random();
	}
	
	public void run() {
		while(true) {
			try {
				boolean isAlert = false;
				StringBuffer sb = new StringBuffer();
				Request request = new Request.Builder()
				    .url("http://blz.bicoin.com.cn/firmOffer/positions")
				    .addHeader("Accept", "application/json,application/xml,application/xhtml+xml,text/html;q=0.9,image/webp,*/*;q=0.8")
				    .addHeader("Accept-Encoding", "gzip, deflate")
				    .addHeader("Accept-Language", "zh-CN,zh")
				    .addHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 8.0.0; zh-cn; MI 6 Build/OPR1.170623.027) AppleWebKit/533.1 (KHTML, like Gecko) Version/5.0 Mobile Safari/533.1")
				    .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
				    .addHeader("Logintime", "" + System.currentTimeMillis() + "000")
				    .addHeader("Mobilid", "Aj_ghTiagAAhowAMcRwZz2t1ECimSR2PbkJN_1om2SoQ")
					.addHeader("Mobilkey", "8D31D6930415DC0E520DAB9664FD888F")
					.addHeader("Redrisegreendown", "2")
				    .addHeader("Token", "571235a511c83122c46920f4c7a2f2b2")
				    .addHeader("Connection", "keep-alive")
				    .addHeader("Host", "blz.bicoin.com.cn")
				    .build();
				Response response = HttpService.sendHttp(request);
				if(response != null && response.isSuccessful()) {
					JSONObject result = JSON.parseObject(response.body().string());
					logger.info("{}", result);
					String datas = result.getString("data");
					if(datas != null) {
						JSONArray userInfos = JSON.parseArray(datas);
						if(userInfos != null && userInfos.size() > 0) {
							for(int i = 0; i < userInfos.size(); i++) {
								StringBuffer all = null;
								StringBuffer pos = null;
								JSONObject userInfo = JSON.parseObject(userInfos.get(i).toString());
								if(userInfo.get("userName") != null) {
									String userName = userInfo.getString("userName");
									User user = users.get(userName);
									String futureShortAmount = formatValue(userInfo.getFloat("futureShortAmount"));
									String futureLongAmount = formatValue(userInfo.getFloat("futureLongAmount"));
									if(user == null) {
										user = new User();
										users.put(userName, user);
									} else {
										String oldFutureLongAmount = user.getFutureLongAmount();
										if(diff(oldFutureLongAmount, futureLongAmount)) {
											all = initStringBuffer(all, userName);
											all.append("多倍:").append(oldFutureLongAmount).append("->").append(futureLongAmount).append(",");
										}
										String oldFutureShortAmount = user.getFutureShortAmount();
										if(diff(oldFutureShortAmount, futureShortAmount)) {
											all = initStringBuffer(all, userName);
											all.append("空倍:").append(oldFutureShortAmount).append("->").append(futureShortAmount).append(",");
										}
									}
									user.setFutureLongAmount(futureLongAmount);
									user.setFutureShortAmount(futureShortAmount);
									String futurePosition = userInfo.getString("futurePosition");
									if(futurePosition != null) {
										JSONArray positions = JSON.parseArray(futurePosition);
										if(positions != null && positions.size() > 0) {
											for(int j = 0; j < positions.size(); j++) {
												JSONObject position = JSON.parseObject(positions.get(j).toString());
												String row = position.getString("exChange");
												String column = position.getString("instrumentId");
												String availQty = formatValue(position.getFloatValue("availQty"));
												String amount = formatValue(position.getFloatValue("amount"));
												String avgCost = formatValue(position.getFloatValue("avgCost"));
												Position p = user.getPosition(row, column);
												if(p != null) {
													String oldAvailQty = p.getAvailQty();
													String oldAmount = p.getAmount();
													String oldAvgCost = p.getAvgCost();
													if(diff(oldAvailQty, availQty)) {
														pos = appendStringBuffer(pos, all != null ? null : userName, row + "|" + column);
														pos.append("张数:").append(oldAvailQty).append("->").append(availQty).append(",");
													}
													if(diff(oldAmount, amount)) {
														pos = appendStringBuffer(pos, all != null ? null : userName, row + "|" + column);
														pos.append("个数:").append(oldAmount).append("->").append(amount).append(",");
													}
													if(diff(oldAvgCost, avgCost)) {
														pos = appendStringBuffer(pos, all != null ? null : userName, row + "|" + column);
														pos.append("均价:").append(oldAvgCost).append("->").append(avgCost).append(",");
													}
												}
												p = new Position(availQty, amount, avgCost);
												user.setPosition(row, column, p);
											}
										}
									}
								}
								if(all != null) {
									sb.append(all.toString());
									logger.info("all = {}", all.toString());
								}
								if(pos != null) {
									sb.append(pos.toString());
									logger.info("pos = {}", pos.toString());
								}
								if(all != null || pos != null) {
									sb.append("\n");
									isAlert = true;
								}
							}
						}
					}
				}
//				System.out.println(sb);
//				if(isAlert) {
//					List<UserInfo> userInfos = new ArrayList<UserInfo>();
//					UserInfo duheng = new UserInfo();
//					duheng.setPhone("18980868096");
//					userInfos.add(duheng);
//					UserInfo chenzhen = new UserInfo();
//					chenzhen.setPhone("13308239343");
//					userInfos.add(chenzhen);
//					Monitor.alarm("other", "lightning spider提醒" + sb.toString() + "请及时关注", userInfos);
//				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {
				int randomInterval = random.nextInt(interval / 2);
				if(random.nextInt(interval) % 2 == 0) {
					randomInterval = interval + randomInterval;
				} else {
					randomInterval = interval - randomInterval;
				}
				System.out.println(randomInterval);
				Thread.sleep(randomInterval * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private String formatValue(float value) {
		return String.format("%.2f", value);	
	}
	
	private boolean diff(String left, String right) {
		float leftValue = Float.parseFloat(left);
		float rightValue = Float.parseFloat(right);
		float diffValue = Math.abs(leftValue - rightValue);
		if(diffValue / leftValue > bicoinRate) {
			return true;
		}
		return false;
	}
	
	private StringBuffer initStringBuffer(StringBuffer sb, String content) {
		if(sb == null) {
			sb = new StringBuffer();	
			if(content != null) {
				sb.append("{").append(content).append("}");				
			}
		}
		return sb;
	}
	
	private StringBuffer appendStringBuffer(StringBuffer sb, String userName, String content) {
		sb = initStringBuffer(sb, userName);
		sb.append("[").append(content).append("],");
		return sb;
	}
}

class User {

	private String futureLongAmount;
	
	private String futureShortAmount;
	
	//row=交易所,column=合约类型,value=详情
	Table<String, String, Position> futurePosition;
	
	public User() {
		futurePosition = HashBasedTable.create();
	}
	
	public String getFutureLongAmount() {
		return futureLongAmount;
	}
	
	public void setFutureLongAmount(String futureLongAmount) {
		this.futureLongAmount = futureLongAmount;
	}
	
	public String getFutureShortAmount() {
		return futureShortAmount;
	}
	
	public void setFutureShortAmount(String futureShortAmount) {
		this.futureShortAmount = futureShortAmount;
	}
	
	public Position getPosition(String row, String column) {
		return futurePosition.get(row, column);
	}
	
	public void setPosition(String row, String column, Position position) {
		futurePosition.put(row, column, position);
	}
}

class Position {
	
	private String availQty;
	
	private String amount;
	
	private String avgCost;
	
	public Position(String availQty, String amount, String avgCost) {
		super();
		this.availQty = availQty;
		this.amount = amount;
		this.avgCost = avgCost;
	}

	public String getAvailQty() {
		return availQty;
	}
	
	public void setAvailQty(String availQty) {
		this.availQty = availQty;
	}
	
	public String getAmount() {
		return amount;
	}
	
	public void setAmount(String amount) {
		this.amount = amount;
	}
	
	public String getAvgCost() {
		return avgCost;
	}
	
	public void setAvgCost(String avgCost) {
		this.avgCost = avgCost;
	}
}