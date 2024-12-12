package com.vocust.qywx.demo.dao.entity;

import lombok.Data;

/**
 * @author hf
 * @version 1.0
 * @date 2020年5月15日 下午8:23:10
 * @desc
 */

@Data
public class QueryParam {

	private String searchType;
	/**
	 * 表示本次拉取的最大消息条数，取值范围为1~1000
	 */
	private int limit;

	/**
	 * proxy与passwd为代理参数，如果运行sdk的环境不能直接访问外网，需要配置代理参数。sdk访问的域名是"https://qyapi.weixin.qq.com"
	 */
	private String proxy;
	private String password;
	/**
	 * 为拉取会话存档的超时时间，单位为秒，建议超时时间设置为5s。
	 */
	private long timeout;
}
