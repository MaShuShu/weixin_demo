package com.lx.weixin.util;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lx.weixin.bean.AccessToken;
import com.lx.weixin.httpClient.HttpClientUtil;

/**
 * 微信access_token获取工具类
 * 
 * @author lixin
 *
 */
public class AccessTokenUtil {
	
	private static Logger logger = LoggerFactory.getLogger(AccessTokenUtil.class);
	
	private static final String GRANT_TYPE = "client_credential";
	private static final String APPID = "wx70b0d2dbde434838";
	private static final String APPSECRET = "42c8fec5da7afa27bd973488f218342c";
	
	public static void main(String[] args) {
		AccessToken accessToken = getAccessToken();
		System.out.println(accessToken);
	}
	
	/**
	 * 获取access_token
	 * @return
	 */
	public static AccessToken getAccessToken() {
		
		AccessToken accessToken = null;
		String url = WeixinURLUtil.ACCESS_TOKEN_URL.substring(0, WeixinURLUtil.ACCESS_TOKEN_URL.indexOf("?"));
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put("grant_type", GRANT_TYPE);
		parameter.put("appid", APPID);
		parameter.put("secret", APPSECRET);
		try {
			HttpClient httpClient = HttpClientUtil.initHttpClient();
			GetMethod method = HttpClientUtil.getMethod(url, parameter);
			int state = httpClient.executeMethod(method);
			if(state == 200) {
				String respBodyStr = method.getResponseBodyAsString();
				JSONObject jsonObject = JsonUtil.strToJson(respBodyStr);
				String token = jsonObject.getString("access_token");
				String expiresInStr = jsonObject.getString("expires_in");
				accessToken = new AccessToken(token, NumberUtil.strToInteger(expiresInStr));
			}
			
		} catch (Exception e) {
			logger.error("获取access_token失败，原因："+e);
			e.printStackTrace();
		}
		System.out.println("\n\naccessToken="+accessToken.getToken()+"\n");
		return accessToken;
	}
}
