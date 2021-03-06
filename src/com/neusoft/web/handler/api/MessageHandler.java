package com.neusoft.web.handler.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.neusoft.core.EapDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.core.channel.DataMessage;
import com.neusoft.core.channel.SNSUser;
import com.neusoft.core.channel.WeiXinUser;
import com.neusoft.util.comet.demo.talker.Constant;
import com.neusoft.util.persistence.PersistenceFactory;
import com.neusoft.util.process.ProcessResult;
import com.neusoft.util.process.ReqeustProcessUtil;
import com.neusoft.util.queue.AgentQueueMessage;
import com.neusoft.util.queue.ServiceQueue;
import com.neusoft.util.rpc.message.Message;
import com.neusoft.util.rpc.message.SystemMessage;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.model.IfaceInfo;
import com.neusoft.web.model.PageTemplate;
import com.neusoft.web.model.SNSAccount;
import com.neusoft.web.model.UserGroup;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/api")
public class MessageHandler extends Handler {
	@RequestMapping(value = "/message")
	public void page(HttpServletRequest request ,HttpServletResponse response , @PathVariable String orgi) throws IOException {
		PrintWriter out=null;
		/*//测试DEMO=====start
		WeiXin message = new WeiXin();
		String toUserName="toUserName";
		String fromUserName="oLc37joa4E05PcQ_FVXFGH1MzPbs8";
    	message.setFromUserName(fromUserName) ;
    	message.setToUserName(toUserName) ;
    	message.setMessagetype("text") ;
    	message.setChannel(RivuDataContext.ChannelTypeEnum.WEIXIN.toString()) ;
    	message.setOrgi(orgi) ;
    	message.setText(request.getParameter("text"));
    	message.setMsgid(UUID.randomUUID().toString().substring(0,10));
    	message.setCreatedate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date())) ;
    	message.setSource(RivuDataContext.ChannelTypeEnum.WEIXIN.toString()) ;
    	
    	WeiXinUser weiXinUser = new WeiXinUser() ;
    	weiXinUser.setUserid(fromUserName) ;
    	weiXinUser.setUsername(fromUserName) ;
    	weiXinUser.setNickName("KerwinDemo2") ;
    	weiXinUser.setApiusername(fromUserName) ;
    	weiXinUser.setFakeId(fromUserName) ;
    	weiXinUser.setOrgi(orgi) ;
    	weiXinUser.setMemo("微信");
    	
    	weiXinUser.setCreatedate(new java.text.SimpleDateFormat(RivuDataContext.DEFAULT_DATE_FORMAT).format(new Date())) ;
    	message.setSnsuser(weiXinUser);
    	message.setUsername(weiXinUser.getNickName());
    	DataMessage dataMessage=new DataMessage(message.getChannel() , message , message.getOrgi() , fromUserName );
    	Message msg=new Message(RivuDataContext.HANDLER , JSON.toJSONString(new SystemMessage(RivuDataContext.SystemRPComman.MESSAGE.toString(),dataMessage) , SerializerFeature.WriteClassName));
    	com.rivues.util.rpc.client.MessageHandler messageDistribute = new com.rivues.util.rpc.client.MessageHandler();
		messageDistribute.process(msg) ;
		out=response.getWriter();
		out.print("ko");
    	//测试DEMO===end
    	*/
		//正常接收Gateway的消息
		try {
			InputStream in = request.getInputStream();
			StringBuffer strb = new StringBuffer() ;
			//方式：转换成BufferedReader
			BufferedReader input = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			String tempString ="";
			 // 一次读入一行，直到读入null为文件结束
			while ((tempString = input.readLine()) != null) 
			{
				strb.append(tempString).append("\n");
			}
			
			Message message = new Message(EapDataContext.HANDLER,strb.toString()) ;
			response.setCharacterEncoding("utf-8");         
			response.setContentType("text/html; charset=utf-8");
			out=response.getWriter();
			//获取处理后的结果
			SystemMessage systemsg=process(message);
			if(systemsg!=null){
				out.print(new Message(EapDataContext.HANDLER,JSON.toJSONString(systemsg, SerializerFeature.WriteClassName)).getMessage());
			}else{
				out.print("");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			out.close();
		}
	}
	private SystemMessage process(Message msg) {
		SystemMessage systemMessage = JSON.parseObject(msg.getMessage(), SystemMessage.class);
		if(EapDataContext.SystemRPComman.GWREQUEST.toString().equals(systemMessage.getType())){
			systemMessage.setType(EapDataContext.SystemRPComman.SMCRESPONSE.toString());
			//获取GW传递过来的网页信息、接口列表以及map参数
			PageTemplate message=(PageTemplate) systemMessage.getMessage();
			String ifaces []= message.getIfaces()!=null && message.getIfaces().length() >0? (message.getIfaces() .split(",")) :null;
			Map<String,Object> resultVal=new HashMap<String,Object>();
			int type = 0 ;
			SNSUser snsUser = null ;
			if(ifaces!=null && ifaces.length>0){
				for (String str : ifaces) {
					IfaceInfo ifaceinfo=(IfaceInfo) EapDataContext.getService().getIObjectByPK(IfaceInfo.class, str);
					if(ifaceinfo!=null){
						//key为接口的别名
						try {
							ProcessResult result = ReqeustProcessUtil.getResponseBody(ifaceinfo,  message.getSnsUser(), message.getOrgi() ,message.getParams()) ;
							if(result!=null && result.getResponseBody()!=null){
								resultVal.put(ifaceinfo.getCode(), result.getResponseBody());
							}
							if(result!=null && result.getResultVal()!=null){
								resultVal.putAll(result.getResultVal()) ;
							}
							if(result.getSnsUser()!= null){
								snsUser = result.getSnsUser() ;
							}
							if(type < result.getType()){
								type = result.getType();
							}
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			ProcessResult result = new ProcessResult(type, resultVal) ; 
			if(snsUser!=null){
				result.setSnsUser(snsUser) ;
			}
			//给GW返回数据
			systemMessage.setMessage(result);
//			APIContext.responseMessageToGW(systemMessage);
			return systemMessage;
		}
		if(EapDataContext.SystemRPComman.UPDATESNSACCOUNT.toString().equals(systemMessage.getType())){
			SNSAccount snsAccount = (SNSAccount)systemMessage.getMessage() ;
			EapDataContext.getService().updateIObject(snsAccount) ;
			return null;
		}
		if(EapDataContext.SystemRPComman.GETUSER.toString().equals(systemMessage.getType())){
			SNSUser snsUser = (SNSUser)systemMessage.getMessage() ;
			snsUser = PersistenceFactory.getInstance().getSnsUserInfo(snsUser.getUserid(), snsUser.getChannel(), snsUser.getOrgi()) ;
			systemMessage.setMessage(snsUser);
//			APIContext.responseMessageToGW(systemMessage);
			return systemMessage;
		}
		if(EapDataContext.SystemRPComman.GWRESPONSEUSERS.toString().equals(systemMessage.getType())){
			List<WeiXinUser> users = (List<WeiXinUser>)systemMessage.getMessage() ;
			//key is the group's code、value is group
			Map<String,UserGroup> groupMap=new HashMap<String, UserGroup>();
			List<UserGroup> groups=null;
			//获取到系统所有分组情况：1.如果有系统不存在的分组，则添加2.如果当前数据与最新不一致，则更新为最新
			String orgi="";
			if(users!=null && users.size()>0){
				orgi=users.get(0).getOrgi();
				groups=EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(UserGroup.class).add(Restrictions.eq("orgi",orgi)));
			}
			for (UserGroup userGroup : groups) {
				if(!groupMap.containsKey(userGroup.getCode())){
					groupMap.put(userGroup.getCode(), userGroup);
				}
			}
			
			List<UserGroup> saveUgList = new ArrayList<UserGroup>();
			List<WeiXinUser> saveUsList = new ArrayList<WeiXinUser>();
			List<WeiXinUser> wx=EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(WeiXinUser.class).add(Restrictions.eq("orgi",orgi)));
			Map<String, String> fMap = new HashMap<String, String>();
			for(int i=0;i<wx.size();i++){
				fMap.put(wx.get(i).getFakeId(), wx.get(i).getFakeId());
			}
			wx = null;
			for (WeiXinUser user : users) {
				//TODO:如何区分用户,是否要考虑多租户的情况;如果数据库有fakeid,则不处理
				if(groupMap.containsKey(user.getGroupID()) && user.getMemo()!=null && !user.getMemo().equals(groupMap.get(user.getGroupID()).getGroupName())){
					//update group
					UserGroup ug=groupMap.get(user.getGroupID());
					ug.setGroupName(user.getMemo());
					EapDataContext.getService().updateIObject(ug);
				}
				if(!groupMap.containsKey(user.getGroupID())){
					//save group
					UserGroup ug=new UserGroup();
					ug.setGroupName(user.getMemo());
					ug.setCode(user.getGroupID());
					ug.setGroupType("1");
					ug.setOrgi(orgi);
					groupMap.put(user.getGroupID(), ug);
					//modify by huqi 2015/1/6  使用批处理
//					RivuDataContext.getService().saveIObject(ug);
					saveUgList.add(ug);
				}
				
				if(!fMap.containsKey(user.getFakeId())){
					//保存，考虑id
					user.setId(UUID.randomUUID().toString());
					//modify by huqi 2015/1/6  使用批处理
					saveUsList.add(user);
				}
			}
			//modify by huqi 2015/1/6  使用批处理
			if(saveUgList.size()>0)EapDataContext.getService().saveBat(saveUgList);
			if(saveUsList.size()>0)EapDataContext.getService().saveBat(saveUsList);
			return null;
		}
		DataMessage dataMessage = (DataMessage) systemMessage.getMessage() ;
		/**
		 * 统计接收到的消息
		 */
		EapDataContext.staticReciveRuntimeData(dataMessage.getLength()) ;
		/**
		 * 接入排队
		 */
		if (dataMessage.getChannel().getSnsuser() == null) {
			/**
			 * 查询用户ID，如果找到，则转发给排队坐席，如果未找到，则请求用户信息
			 */
			SNSUser user = APIContext.getUserInfo(dataMessage, dataMessage.getChannel().getChannel(), dataMessage.getUserid());
			/**
			 * 如果没有用户信息
			 */
			if (user == null) {
				try {
					user = (SNSUser) EapDataContext.getSNSUserBean(dataMessage.getChannel().getChannel() , EapDataContext.SNSBeanType.USER.toString()).newInstance();
					/**
					 * 获取用户信息
					 */
					this.process(new Message(EapDataContext.SystemRPComman.GETUSER.toString(), JSON.toJSONString(user, SerializerFeature.WriteClassName)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			dataMessage.getChannel().setSnsuser(user) ;
		}
		/**
		 * 消息处理
		 */
		//APIContext.processMessage(dataMessage);
		dataMessage.setType(Constant.TALK) ;
		AgentQueueMessage agentQueueMessage = ServiceQueue.userServiceRequest(dataMessage.getOrgi(), dataMessage.getChannel().getSnsuser(), false , dataMessage , 5 , 0) ; ;
		//没有返回消息或者已经在服务中了
		if(agentQueueMessage==null || "".equals(agentQueueMessage.getMessage())){
			return null;
		}
		dataMessage.getChannel().setText(agentQueueMessage.getMessage()) ;
		dataMessage.getChannel().setTouser(dataMessage.getUserid()) ;		//设置touser
		
		/**
		 * 统计发送的消息量
		 */
		EapDataContext.staticSendRuntimeData(dataMessage.getLength()) ;
		if(EapDataContext.ReplyType.AUTOMATIC.toString().equals(dataMessage.getChannel().getReplytype())){
			EapDataContext.staticAutoMessageRuntimeData(dataMessage.getLength()) ;
		}
		return new SystemMessage(EapDataContext.SystemRPComman.MESSAGE.toString() , dataMessage);
	}
}
