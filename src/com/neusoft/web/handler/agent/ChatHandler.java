package com.neusoft.web.handler.agent;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ModelAndView;

import com.neusoft.core.EapDataContext;
import com.neusoft.core.EapSmcDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.core.channel.DataMessage;
import com.neusoft.core.channel.SNSUser;
import com.neusoft.core.channel.WebIM;
import com.neusoft.core.channel.WeiXin;
import com.neusoft.util.queue.AgentUser;
import com.neusoft.util.queue.ServiceQueue;
import com.neusoft.util.store.EapTools;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/chat")
public class ChatHandler  extends Handler{
	private static final Logger logger = Logger.getLogger(ChatHandler.class);
	@Autowired
    CommonsMultipartResolver multipartResolver;
	@RequestMapping(value = "/send")
    public ModelAndView index(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		MultipartFile file = getFile(request);
		long times = System.currentTimeMillis();
		ResponseData responseData = new ResponseData("/pages/agent/userchat"  ) ;
		ModelAndView view = request(responseData, orgi, data) ;
		WeiXin channel = (WeiXin) EapDataContext.getSNSUserBean(data.getChannel(), EapDataContext.SNSBeanType.MESSAGE.toString()).newInstance() ;
		channel.setChannel(data.getChannel()) ;
		channel.setContextid(data.getContextid()) ;
		channel.setTouser(data.getUsername()) ;
		channel.setUserid(data.getUserid()) ;
		((WeiXin)channel).setSource(request.getParameter("source"));
		((WeiXin)channel).setTitle(request.getParameter("title"));
		((WeiXin)channel).setFromUserName("");
		((WeiXin)channel).setNickName(request.getParameter("nickName"));
		((WeiXin)channel).setSubtype("replay");
		channel.setReplytype(EapDataContext.ReplyType.MANUALLY.toString()) ;
		channel.setUsername(super.getUser(request).getUsername()) ;
		WebIM webim = (WebIM) EapDataContext.getWebImUserBean(data.getChannel(), EapDataContext.SNSBeanType.MESSAGE.toString()).newInstance() ;
		//ocP-Sjt58rvfEqvStZQgtXJzyxhg//生产
		//ou7F6s6-gw0Cw6OOUWA-Y7NjX_t4//本地
		if("ocP-Sjt58rvfEqvStZQgtXJzyxhg".equals(channel.getUserid())){
			channel.setChannel("webim") ;//webim
			webim.setChannel(data.getChannel());
			webim.setContextid(data.getContextid());
			webim.setTouser(data.getUsername());
			webim.setUserid(data.getUserid()) ;
			webim.setReplytype(EapDataContext.ReplyType.MANUALLY.toString()) ;
			webim.setUsername(super.getUser(request).getUsername()) ;
			
		}
		
		if(channel.getChannel().equals(EapDataContext.ChannelTypeEnum.WEIXIN.toString()) || channel.getChannel().equals(EapDataContext.ChannelTypeEnum.YIXIN.toString()) || channel.getChannel().equals(EapDataContext.ChannelTypeEnum.WEBIM.toString())){
			channel.setText(EapTools.htmlProcess(data.getContent() , EapSmcDataContext.getSearchSetting(orgi).getGwhost() , "img","a" )) ;
			webim.setText(EapTools.htmlProcess(data.getContent() , EapSmcDataContext.getSearchSetting(orgi).getGwhost() , "img","a" )) ;
		}else{
			channel.setText(data.getContent()) ;
		}
		//webim
		if(webim.getText().trim().length()>0){
			webim.setMessagetype(EapDataContext.MessageType.TEXT.toString());
			webim.setOrgi(data.getOrgi());
			DataMessage dataMessage = new DataMessage(webim.getChannel() , webim , webim.getOrgi() , webim.getUserid()) ;
			SNSUser agent = (SNSUser) EapDataContext.getSNSUserBean(data.getChannel(), EapDataContext.SNSBeanType.USER.toString()).newInstance() ;
			agent.setUserid(super.getUser(request).getAgentstatus().getAgentno()) ;
			agent.setOrgi(orgi) ;
			channel.setSnsuser(agent) ;
			for(AgentUser agentUser : super.getUser(request).getAgentstatus().getUserList()){
				if(agentUser.getUserid().equals(data.getUserid())){
					agentUser.getUnreplaymessage().clear();
				}
			}
			APIContext.saveMessage(dataMessage) ;
		}
		if(channel.getText().trim().length()>0){
			channel.setMessagetype(EapDataContext.MessageType.TEXT.toString()) ;
			channel.setOrgi(data.getOrgi()) ;
			DataMessage dataMessage = new DataMessage(channel.getChannel() , channel , channel.getOrgi() , channel.getUserid()) ;
			SNSUser agent = (SNSUser) EapDataContext.getSNSUserBean(data.getChannel(), EapDataContext.SNSBeanType.USER.toString()).newInstance() ;
			agent.setUserid(super.getUser(request).getAgentstatus().getAgentno()) ;
			agent.setOrgi(orgi) ;
			channel.setSnsuser(agent) ;
			for(AgentUser agentUser : super.getUser(request).getAgentstatus().getUserList()){
				if(agentUser.getUserid().equals(data.getUserid())){
					agentUser.getUnreplaymessage().clear();
				}
			}
			view.addObject("msg", channel) ;
			view.addObject("contract", agent) ;
			
			APIContext.saveMessage(dataMessage) ;
			
			String fileName = null;
			String text = channel.getText() ;
			if(request.getParameter("ipt_file")!=null && request.getParameter("ipt_file").length()>0 && file!=null && file.getBytes()!=null){
				fileName = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
				channel.setBytedata(file.getBytes()) ;
				if(channel.getText().trim().length()==0){
					channel.setText("<file/>") ;
				}
				channel.setRemarkName(channel.getId()+fileName) ;
				channel.setText(channel.getText()+" <a href=\""+EapSmcDataContext.getSearchSetting(orgi).getGwhost()+"/assets/download/"+channel.getRemarkName()+"\">下载文件："+file.getOriginalFilename()+"</a>") ;
			}
			APIContext.sendMessageToUser(dataMessage) ;
			channel.setText(text) ;
			if(ServiceQueue.getUserQueue()!=null && ServiceQueue.getUserQueue().get(data.getUserid())!=null){
				AgentUser agentUser = ServiceQueue.getUserQueue().get(data.getUserid()) ;
				agentUser.setTip(false) ;
				agentUser.setAgent(true);
				agentUser.getLastmessage().setTime(new Date().getTime());
				ServiceQueue.getUserQueue().put(data.getUserid(), agentUser);
			}
		}else{
			responseData = new ResponseData("/pages/public/success"  ) ;
		}
		logger.info("将消息发送到gw，发送成功，用时为: " + EapDataContext.getDecimalFormat().format((System.currentTimeMillis() - times)/1000.0) + "s");
		return view ;
    }
	private MultipartFile getFile(HttpServletRequest request){
		MultipartFile mfile=null;
		if(request instanceof MultipartHttpServletRequest){
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;   
			if (multipartResolver.isMultipart(multipartRequest)){  //判断 request 是否有文件上传,即多部分请求...  
	             // srcfname 是指 文件上传标签的 name=值  
	             MultiValueMap<String, MultipartFile> multfiles = multipartRequest.getMultiFileMap();
	             for(String srcfname:multfiles.keySet()){
	            	 mfile=  multfiles.getFirst(srcfname);
	             }
			}
		}
		return mfile;
	}

}
