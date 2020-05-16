package com.dmall.monitor.spiders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class MinerWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(MinerWorker.class);

	private int interval;
	
	private String url;
	
	public MinerWorker(int interval, String url) {
		this.interval = interval;
		this.url = url;
	}
	
	public void run() {
		while(true) {
			try {
				StringBuffer sb = new StringBuffer();
				Request request = new Request.Builder()
				    .url("http://127.0.0.1:8800")
				    .build();
				Response response = HttpService.sendHttp(request);
				if(response != null && response.isSuccessful()) {
//					logger.info("{}", response.body().string());
					String body = response.body().string();
					request = new Request.Builder()
					    .url(url)
					    .post(RequestBody.create(MediaType.parse("text/html; charset=utf-8"), body))
					    .build();
					response = HttpService.sendHttp(request);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(interval * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
