package com.vocust.qywx.demo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tencent.wework.Finance;
import com.vocust.qywx.demo.dao.entity.ChatDatas;
import com.vocust.qywx.demo.dao.entity.MsgContent;
import com.vocust.qywx.demo.dao.entity.QueryParam;
import com.vocust.qywx.demo.dao.entity.Qychat;
import com.vocust.qywx.demo.dao.mapper.MsgContentMapper;
import com.vocust.qywx.demo.dao.mapper.QychatMapper;
import com.vocust.qywx.demo.service.MsgContentService;
import com.vocust.qywx.demo.service.QychatService;
import com.vocust.qywx.demo.service.UserService;
import com.vocust.qywx.demo.utils.EnterpriseParame;
import com.vocust.qywx.demo.utils.RSAUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Service
@EnableScheduling
public class QychatServiceImpl implements QychatService {
	@Autowired
	private QychatMapper qychatMapper;

	@Autowired
	private MsgContentMapper msgContentMapper;
	
	@Autowired
	private MsgContentService msgContentService;
	
	@Autowired
	private UserService userService;
	
	@Value("${filepath}")
	private String filepath;
	
	private static final Gson gson =new Gson();;

	@Override
	public List<Qychat> queryAllInfos() {
		log.info("/queryAllUsers start...");
		return qychatMapper.queryAllInfos();
	}

	/**
	 * 每五分钟执行一次定时任务
	 */
	@Override
	@Scheduled(cron = "0 */5 * * * ?")
	public void initQychatData() {
		QueryParam param = new QueryParam();
		// 从第几条开始拉取 表示该企业存档消息序号，该序号单调递增，拉取序号建议设置为上次拉取返回结果中最大序号。首次拉取时seq传0，sdk会返回有效期内最早的消息。
		Integer seq = qychatMapper.getSeq() == null ? 0 : qychatMapper.getSeq();
		param.setLimit(100);
		param.setTimeout(5);
		int ret = 0;

		//使用sdk前需要初始化，初始化成功后的sdk可以一直使用。
		//如需并发调用sdk，建议每个线程持有一个sdk实例。
		//初始化时请填入自己企业的corpid与secrectkey。
		long sdk = Finance.NewSdk();
		ret = Finance.Init(sdk, EnterpriseParame.CORPID, EnterpriseParame.SECRET);
		if(ret != 0){
			Finance.DestroySdk(sdk);
			System.out.println("init sdk err ret " + ret);
			return;
		}
		int limit = param.getLimit();
		// 每次使用GetChatData拉取存档前需要调用NewSlice获取一个slice，在使用完slice中数据后，还需要调用FreeSlice释放。
		long slice = Finance.NewSlice();
		// proxy与passwd为代理参数，如果运行sdk的环境不能直接访问外网，需要配置代理参数。sdk访问的域名是"https://qyapi.weixin.qq.com"。
		ret = Finance.GetChatData(sdk, seq, limit, param.getProxy(), param.getPassword(), param.getTimeout(), slice);
		if (ret != 0) {
			log.error("get chatdata ret " + ret);
			Finance.FreeSlice(slice);
			return;
		}
		// 获取消息
		String data = Finance.GetContentFromSlice(slice);
		Finance.FreeSlice(slice);
		JSONObject jsonObject = JSONObject.parseObject(data);
		ChatDatas cdata = JSON.toJavaObject(jsonObject, ChatDatas.class);
		List<Qychat> list = cdata.getChatdata();
		for (Qychat qychat : list) {
			//解密会话存档内容
			//sdk不会要求用户传入rsa私钥，保证用户会话存档数据只有自己能够解密。
			//此处需要用户先用rsa私钥解密encrypt_random_key后，作为encrypt_key参数传入sdk来解密encrypt_chat_msg获取会话存档明文。
			String encrypt_chat_msg = qychat.getEncrypt_chat_msg();
			String encrypt_key = null;
			try {
				encrypt_key = RSAUtils.getPrivateKey(qychat.getEncrypt_random_key());
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 每次使用DecryptData解密会话存档前需要调用NewSlice获取一个slice，在使用完slice中数据后，还需要调用FreeSlice释放。
			long msg = Finance.NewSlice();
			ret = Finance.DecryptData(sdk, encrypt_key, encrypt_chat_msg, msg);
			if (ret != 0) {
				System.out.println("get DecryptData ret " + ret);
				Finance.FreeSlice(msg);
				continue;
			}
			// 最后得到明文消息内容
			String decrypt_msg = Finance.GetContentFromSlice(msg);
			Finance.FreeSlice(msg);
			qychatMapper.insertQychat(qychat);
			JSONObject content = JSONObject.parseObject(decrypt_msg);
			MsgContent msgcontent = new MsgContent();
			if(content.getString("action").equals("send"))
			{
				msgcontent.setMsgid(content.getString("msgid"));
				msgcontent.setAction(content.getString("action"));
				msgcontent.setFrom(content.getString("from"));
				msgcontent.setFromView(userService.getUsernameByUserid(content.getString("from")));
				msgcontent.setTolist(content.getString("tolist"));
				msgcontent.setTolistView(getTolistByUserId(content.getString("tolist")));
				msgcontent.setRoomid(content.getString("roomid"));
				msgcontent.setRoomidView(getGroupchatName(content.getString("roomid")));
				msgcontent.setMsgtime(content.getString("msgtime"));
				msgcontent.setMsgtype(content.getString("msgtype"));
				msgcontent.setText(isEmpty(content.getString("text")));
				msgcontent.setImage(isEmpty(content.getString("image")));
				msgcontent.setWeapp(isEmpty(content.getString("weapp")));
				msgcontent.setRedpacket(isEmpty(content.getString("redpacket")));
				msgcontent.setFile(isEmpty(content.getString("file")));
				msgcontent.setVideo(isEmpty(content.getString("video")));
				msgcontent.setVoice(isEmpty(content.getString("voice")));
				msgcontent.setChatrecord(isEmpty(content.getString("chatrecord")));
				msgcontent.setFilename(getFileNameAndDownloadData(msgcontent));	
			}else if(content.getString("action").equals("switch"))
			{
				log.info("switch 消息"+content);
				msgcontent.setMsgid(content.getString("msgid"));
				msgcontent.setAction(content.getString("action"));
				msgcontent.setFrom(content.getString("user"));
				msgcontent.setMsgtime(content.getString("time"));
				msgcontent.setMsgtype("switch");
				msgcontent.setFromView(userService.getUsernameByUserid(content.getString("user")));
			}else 
			{
				msgcontent.setMsgid(content.getString("msgid"));
				msgcontent.setAction(content.getString("action"));
				msgcontent.setMsgtime(content.getString("time"));
				msgcontent.setMsgtype("revoke");
			}
			// 解析消息 并插入到数据库
			msgContentMapper.insertMsgContent(msgcontent);
		}
//		Finance.FreeSlice(slice);
		log.info("----------------------scheduled tasks qywx data success-----------------------");

	}

	
	private String getGroupchatName(String roomid) {
		if(StringUtils.isEmpty(roomid))
			return null;
		String data  = msgContentService.getGroupchatInfoByRoomid(roomid);
		JsonObject result = gson.fromJson(data, JsonObject.class);
		String name=null;
		if(result.get("errcode").getAsInt()!=0)
		{
			name = "该群不是客户群";
		}
		else
		{
			name  = result.get("group_chat").getAsJsonObject().get("name").getAsString();
		}
	
		return name;
	}

	private String getTolistByUserId(String tolist) {
		if(StringUtils.isEmpty(tolist))
			return null;
		List<String> list =new ArrayList<String>();
		JsonArray result = gson.fromJson(tolist, JsonArray.class);
		for (JsonElement jsonElement : result) {
			list.add(userService.getUsernameByUserid(jsonElement.getAsString()));
		}
		return list.toString();
	}



	private String content = null;

	private String isEmpty(String data) {
		if (StringUtils.isEmpty(data)) {
			return null;
		} else {
			content = data;
			return data;

		}
	}

	private String getFileNameAndDownloadData(MsgContent msgcontent) {
		String fileName = null;
		try {
			String fileType = msgcontent.getMsgtype();
			JSONObject jsonObject = JSONObject.parseObject(content);
			String sdkfileid = jsonObject.getString("sdkfileid");
			fileName =getFileName(jsonObject,fileType);
			if (!StringUtils.isEmpty(sdkfileid) && null != fileName) {
				downLodaFile(fileName, sdkfileid);
			}
		} catch (Exception e) {
			log.info("下载文件出错" + e);
		}
		return fileName;

	}

	private String getFileName(JSONObject jsonObject, String fileType) {
		String fileName = null;
		String md5sum =jsonObject.getString("md5sum");
		switch (fileType) {
		case "image":
			fileName = md5sum+ ".jpg";
			break;
		case "voice":
			fileName = md5sum+ ".mp3";
			break;
		case "video":
			fileName = md5sum+ ".mp4";
			break;
		case "file":
			fileName = jsonObject.getString("filename");
			break;
		default:
			fileName = "default.jpg";
			break;
		}
		return fileName;
	}

	@Async
	public void downLodaFile(String fileName, String sdkFileid) {
		int ret = 0;
		long sdk = Finance.NewSdk();
		// 初始化
		Finance.Init(sdk, EnterpriseParame.CORPID, EnterpriseParame.SECRET);

		//媒体文件每次拉取的最大size为512k，因此超过512k的文件需要分片拉取。若该文件未拉取完整，sdk的IsMediaDataFinish接口会返回0，同时通过GetOutIndexBuf接口返回下次拉取需要传入GetMediaData的indexbuf。
		//indexbuf一般格式如右侧所示，”Range:bytes=524288-1048575“，表示这次拉取的是从524288到1048575的分片。单个文件首次拉取填写的indexbuf为空字符串，拉取后续分片时直接填入上次返回的indexbuf即可。
		String indexbuf = "";
		while (true) {
			// 每次使用GetMediaData拉取存档前需要调用NewMediaData获取一个media_data，在使用完media_data中数据后，还需要调用FreeMediaData释放。
			long media_data = Finance.NewMediaData();
			// sdkFileid 解密企微消息的消息体内容中的sdkfileid信息。
			ret = Finance.GetMediaData(sdk, indexbuf, sdkFileid, null, null, 3, media_data);
			if (ret != 0) {
				return;
			}
			System.out.printf("getmediadata outindex len:%d, data_len:%d, is_finis:%d\n",
					Finance.GetIndexLen(media_data), Finance.GetDataLen(media_data),
					Finance.IsMediaDataFinish(media_data));
			try {
				//大于512k的文件会分片拉取，此处需要使用追加写，避免后面的分片覆盖之前的数据。
				FileOutputStream outputStream = new FileOutputStream(new File(filepath + fileName), true);
				outputStream.write(Finance.GetData(media_data));
				outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (Finance.IsMediaDataFinish(media_data) == 1) {
				//已经拉取完成最后一个分片
				Finance.FreeMediaData(media_data);
				break;
			} else {
				//获取下次拉取需要使用的indexbuf
				indexbuf = Finance.GetOutIndexBuf(media_data);
				Finance.FreeMediaData(media_data);
			}
		}
		log.info("下载完毕");
		Finance.DestroySdk(sdk);
	}

}