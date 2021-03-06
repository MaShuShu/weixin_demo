package com.lx.weixin.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lx.weixin.service.IUserLoginRegsiterService;
import com.lx.weixin.spring.bean.UserInfo;
import com.lx.weixin.spring.util.ApplicationContextHelper;
import com.lx.weixin.util.JsonUtil;
import com.lx.weixin.util.ResultHandle;
import com.lx.weixin.util.UUIDUtil;
import com.lx.weixin.util.WeixinConst;

/**
 * 用户注册、登录Servlet
 */
public class LoginRegsiterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String CURRENT_LOGIN_USER = "session_user";
	
	private IUserLoginRegsiterService userLoginRegsiterService;
	
	@Override
	public void init() throws ServletException {
		
		userLoginRegsiterService = (IUserLoginRegsiterService) ApplicationContextHelper.getBean("userLoginRegsiterService");
		if(userLoginRegsiterService == null) {
			logger.error("初始化 IUserLoginRegsiterService bean 失败");
		}
	}
    
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		dispatchMethod(request, response);
	}
	
	/**
	 * 分发请求
	 * @param request
	 * @param response
	 */
	public void dispatchMethod(HttpServletRequest request, HttpServletResponse response) {
		String methodName = request.getParameter("method");
		if(StringUtils.isEmpty(methodName)) {	
			String requestURI = request.getRequestURI();
			StringBuffer requestURL = request.getRequestURL();
			System.out.println("requestURI="+requestURI+"\nrequestURL="+requestURL);
			int paramsIndex = requestURL.indexOf("?");
			String urlTemp = "";
			if(paramsIndex > -1) {
				urlTemp = requestURL.substring(0, paramsIndex);
				String urlTempPart = urlTemp.substring(urlTemp.lastIndexOf("/"));
				if(urlTempPart.lastIndexOf("_") > -1) {
					methodName = urlTempPart.substring(urlTempPart.lastIndexOf("_"));
				}
			}
		}

		/*try {
			Method method = getClass().getDeclaredMethod(methodName, request.getClass(), response.getClass());
			method.setAccessible(true);	//释放私有方法调用访问权限
			method.invoke(getClass(), request, response);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}*/
		if("register".equals(methodName)) {	//注册
			register(request, response);
		} else if("login".equals(methodName)) {	//登录
			login(request, response);
		} else if("checkUserNameIsExist".equals(methodName)) {	//检测用户唯一性
			checkUserNameIsExist(request, response);
		} else if("loginOut".equals(methodName)) {	//检测用户唯一性
			loginOut(request, response);
		}
	}
	
	/**
	 * 检测注册的用户名是否已存在
	 * @param request
	 * @param response
	 */
	public void checkUserNameIsExist(HttpServletRequest request, HttpServletResponse response) {
		boolean success = false;	//false：已存在，不可用；true：不存在，可用；默认用户名已存在
		String errorMsg = "";
		
		String userName = request.getParameter("userName");
		try {
			
			UserInfo userInfo = userLoginRegsiterService.getUserInfo(userName);
			if(userInfo == null) {
				success = true;
			} else {
				errorMsg = "用户名已被注册";
			}
		} catch (Exception e) {
			logger.error("用户"+userName+"检测唯一性失败，原因："+e);
			e.printStackTrace();
		}
		Map<String, Object> resultMap = ResultHandle.getResultMap(success, errorMsg);
		JsonUtil.writeJsonStr(response, JsonUtil.objToStr(resultMap));
	}
	
	/**
	 * 用户注册
	 */
	public void register(HttpServletRequest request, HttpServletResponse response) {
		
		boolean success = false;
		String errorMsg = "";
		
		String id = UUIDUtil.id();
//		String unionId = request.getParameter("unionId");	//微信账号唯一标识，注册时将当前注册的账号与微信用户唯一标示UnionId进行绑定，便于自动登录
		Object unionIdObj = request.getSession().getAttribute(WeixinConst.SESSION_UNIONID);
		String unionId = unionIdObj != null ? unionIdObj.toString() : null;
		String userName = request.getParameter("userName");
		String password = request.getParameter("password");
		String trueName = request.getParameter("trueName");
		String nickName = request.getParameter("nickName");
		String sex = request.getParameter("sex");
		String email = request.getParameter("email");
		String phone = request.getParameter("phone");
		String validFlag = request.getParameter("validFlag");
		Date createDate = new Date();
		UserInfo user = new UserInfo(id, 
				userName, password, trueName, 
				unionId, nickName, sex, email, 
				phone, validFlag, createDate);
		try {
			
			int flag = userLoginRegsiterService.saveUser(user);
			if(flag > -1) {
				success = true;
			} else {
				errorMsg = "注册失败！";
			}
		} catch (Exception e) {
			errorMsg = "注册失败！";
			logger.error("用户"+userName+"注册失败，原因："+e);
			e.printStackTrace();
		}
		Map<String, String> data = new HashMap<String, String>();
		data.put("unionId", unionId);
		Map<String, Object> resultMap = ResultHandle.getResultMap(success, data, errorMsg);
		JsonUtil.writeJsonStr(response, JsonUtil.objToStr(resultMap));
	}
	
	/**
	 * 登录
	 */
	public void login(HttpServletRequest request, HttpServletResponse response) {
		
		logger.info(">>>>>>>>>>>>>>进入登录了");
		boolean success = false;
		String errorMsg = "";
		
		//微信账号唯一标识，注册时将当前注册的账号与微信用户唯一标示UnionId进行绑定，便于自动登录
		String unionId = request.getParameter("unionId");
		String userName = request.getParameter("userName");
		String password = request.getParameter("password");
		
		UserInfo user = null;
		if(StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
			user = userLoginRegsiterService.getUserInfo(userName);
			if(user != null && user.getPassword().equals(password)) {
				//已注册的账号且未绑定微信号唯一标识，则在此自动绑定微信唯一标识
				if(StringUtils.isNotBlank(unionId)) {
					String unionIdTemp = user.getUnionId();
					if(StringUtils.isEmpty(unionIdTemp)) {
						try {
							userLoginRegsiterService.updateUserUnionId(userName, unionId);
						} catch (Exception e) {
							logger.error("绑定mooc平台账号与微信账号唯一标识unionId失败，原因："+e);
							errorMsg = "用户登录号绑定微信账号失败";
						}
					} else if(unionIdTemp.equals(unionId)) {
						user = null;
						errorMsg = "该用户已绑定其它微信账号";
					}
				}
			}else if(user == null) {
				errorMsg = "用户不存在";
			} else if(!user.getPassword().equals(password)) {
				errorMsg = "密码错误";
			}
		} else if(StringUtils.isNotBlank(unionId)){
    		user = userLoginRegsiterService.getUserInfoByUnionId(unionId);	// 通过微信账号唯一标识unionId登录
		} else {
			errorMsg = "用户名和密码不能为空";
		}
		
		if(user != null) {	//登录成功，将用户存入session
			success = true;
			request.getSession().setAttribute(CURRENT_LOGIN_USER, user);
		}
		
		Map<String, Object> resultMap = ResultHandle.getResultMap(success, errorMsg);
		JsonUtil.writeJsonStr(response, JsonUtil.objToStr(resultMap));
	}

	/**
	 * 注销登录（若微站有手动注销登录，则注销时最好是清除该用户与微信账号unionId的绑定关系，下次再登录该微站时使用手动输入密码登录并重新与该微信账号UnionId绑定关系；是否需要这样具体还得根据项目需求来定）
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public void loginOut(HttpServletRequest request, HttpServletResponse response) {
		
		UserInfo user = (UserInfo) request.getSession().getAttribute(CURRENT_LOGIN_USER);
		
		//手动注销，清除用户绑定的unionId
		userLoginRegsiterService.updateUserUnionId(user.getUserName(), null);

		//移除session
		request.getSession().removeAttribute(CURRENT_LOGIN_USER);
		
		Map<String, Object> resultMap = ResultHandle.getResultMap(true, "");
		JsonUtil.writeJsonStr(response, JsonUtil.objToStr(resultMap));
	}
}
