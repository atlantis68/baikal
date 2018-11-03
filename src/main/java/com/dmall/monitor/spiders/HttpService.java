package com.dmall.monitor.spiders;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class HttpService {

	private static OkHttpClient okHttpClient = new OkHttpClient();

	public static Response sendHttp(Request request) throws Exception {
		return okHttpClient.newCall(request).execute();
	}
}
