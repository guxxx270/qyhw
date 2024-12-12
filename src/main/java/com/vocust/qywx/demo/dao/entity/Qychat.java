package com.vocust.qywx.demo.dao.entity;

import java.io.Serializable;

import lombok.Data;

/**
*@author  hf
*@version 1.0
*@date  2020年5月15日 下午4:27:43
*@desc 
*/

@Data
public class Qychat implements Serializable{
	
	private static final long serialVersionUID = 1L;
	/**
	 * 表示该企业存档消息序号，该序号单调递增，拉取序号建议设置为上次拉取返回结果中最大序号。首次拉取时seq传0，sdk会返回有效期内最早的消息。
	 */
	private String seq;

	/**
	 * 消息id，消息的唯一标识，企业可以使用此字段进行消息去重。String类型。msgid以_external结尾的消息，表明该消息是一条外部消息。msgid以_updown_stream结尾的消息，表明该消息是一条上下游消息。
	 */
	private String msgid;

	/**
	 * 加密此条消息使用的公钥版本号。Uint32类型
	 */
	private String publickey_ver;

	/**
	 * 使用publickey_ver指定版本的公钥进行非对称加密后base64加密的内容，
	 * 需要业务方先base64 decode处理后，再使用指定版本的私钥进行解密，得出内容。String类型
	 */
	private String encrypt_random_key;

	/**
	 * 消息密文。需要业务方使用将encrypt_random_key解密得到的内容，与encrypt_chat_msg，传入sdk接口DecryptData,得到消息明文。String类型
	 */
	private String encrypt_chat_msg;
}
