>在C:\Windows\system32 文件夹里放入我们要使用的D:\rough\QyChat\ddl\*.dll文件

> 配置类：
> EnterpriseParame
 
> 整体流程：https://developer.work.weixin.qq.com/document/path/91774#%E6%95%B4%E4%BD%93%E6%B5%81%E7%A8%8B

> 拉取企微消息：initQychatData
> 通过拉取的不同消息类型获取企微保存的媒体文件：downLodaFile
 
> 错误码：
> 10000	请求参数错误	检查Init接口corpid、secret参数；检查GetChatData接口limit参数是否未填或大于1000；检查GetMediaData接口sdkfileid是否为空，indexbuf是否正常
> 10001	网络请求错误	检查是否网络有异常、波动；检查使用代理的情况下代理参数是否设置正确的用户名与密码
> 10002	数据解析失败	建议重试请求。若仍失败，可以反馈给企业微信进行查询，请提供sdk接口参数与调用时间点等信息
> 10003	系统调用失败	GetMediaData调用失败，建议重试请求。若仍失败，可以反馈给企业微信进行查询，请提供sdk接口参数与调用时间点等信息
> 10004	已废弃	目前不会返回此错误码
> 10005	fileid错误	检查在GetMediaData接口传入的sdkfileid是否正确
> 10006	解密失败	请检查是否先进行base64decode再进行rsa私钥解密，再进行DecryptMsg调用
> 10007	已废弃	目前不会返回此错误码
> 10008	DecryptMsg错误	建议重试请求。若仍失败，可以反馈给企业微信进行查询，请提供sdk接口参数与调用时间点等信息
> 10009	ip非法	请检查sdk访问外网的ip是否与管理端设置的可信ip匹配，若不匹配会返回此错误码
> 10010	请求的数据过期	用户欲拉取的数据已过期，仅支持近5天内的数据拉取
> 10011	ssl证书错误	使用openssl版本sdk，校验ssl证书失败

> 常见问题解答：https://developer.work.weixin.qq.com/document/path/91552

> 公钥私钥生成工具及方法 http://web.chacuo.net/netrsakeypair
> 公钥保存在企微后台，私钥保存在应用中用于解密消息的encrypt_random_key，作为encrypt_key参数传入sdk来解密encrypt_chat_msg获取会话存档明文。
# QyChat

###### 最近做企业微信 会话内容存档 由于官方网站没有完整的JAVA代码 踩了很多坑   最后自己整理出来一个springboot + vue 的项目放在这里 以供大家参考

企业微信会话内容存档 前端页面展示<br>
##### 1登录页面
![image](images/login.png)

##### 2首页及开启会话内容存档权限的用户列表
![image](images/home.png)

##### 3企业微信用户消息记录表
![image](images/msgcontent.png)

##### 4消息列表中文本消息展示
![image](images/text.png)

##### 5消息列表图片消息展示
![image](images/image.png)
