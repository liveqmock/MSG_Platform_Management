package com.neusoft.web.handler.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.neusoft.core.EapDataContext;
import com.neusoft.core.EapSmcDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.core.channel.Channel;
import com.neusoft.core.channel.DataMessage;
import com.neusoft.core.channel.SNSUser;
import com.neusoft.core.channel.WeiXin;
import com.neusoft.util.comet.demo.talker.Constant;
import com.neusoft.util.persistence.PersistenceFactory;
import com.neusoft.util.queue.AgentStatus;
import com.neusoft.util.queue.AgentUser;
import com.neusoft.util.queue.ServiceQueue;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;
import com.neusoft.web.model.AgentServiceStatus;
import com.neusoft.web.model.AgentSkill;
import com.neusoft.web.model.FAQModel;
import com.neusoft.web.model.SearchSetting;
import com.neusoft.web.model.User;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/agent")
public class AgentHandler extends Handler{
	@RequestMapping(value = "/status/{statusval}")
    public ModelAndView index(HttpServletRequest request ,@PathVariable String orgi,@PathVariable String statusval, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/agent/status"  ) ; 
		User user = super.getUser(request);
		AgentServiceStatus as = new AgentServiceStatus();
		as.setAgentno(user.getAgentstatus().getAgentno());
		as.setAgentname(user.getAgentstatus().getUser().getUsername());
		as.setCreatetime(new Date());
		as.setOperatetime(new Date());
		as.setOrgi(orgi);
		if("ready".equals(statusval)){ 
			as.setOperatetype("1");
			user=ServiceQueue.statusChange(user.getOrgi(), user.getAgentstatus().getAgentno() ,  AgentStatus.AgentStatusEnum.SERVICES.toString() , user) ;
		}else if("leave".equals(statusval)){
			as.setOperatetype("0");
			user=ServiceQueue.statusChange(user.getOrgi(), user.getAgentstatus().getAgentno() ,  AgentStatus.AgentStatusEnum.LEAVE.toString(), user) ;
		}
		super.getService().saveIObject(as);
    	request.getSession().removeAttribute(EapDataContext.USER_SESSION_NAME) ;
    	request.getSession().setAttribute(EapDataContext.USER_SESSION_NAME, user) ;
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/status/refresh")
    public ModelAndView refresh(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		return request(new ResponseData("/pages/agent/status"  ), orgi, data) ;
    }
	
	@RequestMapping(value = "/inservice")
    public ModelAndView inservice(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ModelAndView view = request(new ResponseData("/pages/agent/inservice"  ), orgi, data);
		User user = super.getUser(request) ;
		if(user!=null){
			AgentStatus agentStatus = ServiceQueue.getAgent(user.getAgentno(), orgi);
			view.addObject("agentstatus", agentStatus) ;
			view.addObject("userlist", agentStatus!=null ? agentStatus.getUserList() : null) ;
			view.addObject("inseruser", agentStatus.getUserList().size()) ;
			view.addObject("queueuser", ServiceQueue.getQueueNum(orgi));
		}
		return view;
    }
	
	@RequestMapping(value = "/index")
    public ModelAndView agent(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/agent"  ) ; 
		ModelAndView view = request(responseData, orgi, data) ;
		view.addObject("setting", EapSmcDataContext.getSearchSetting(orgi)) ;
		User user = super.getUser(request) ;
		if(user!=null){
			AgentStatus agentStatus = ServiceQueue.getAgent(user.getAgentno(), orgi);
			view.addObject("agentstatus", agentStatus) ;
			view.addObject("userlist", agentStatus!=null ? agentStatus.getUserList() : null) ;
			AgentUser agentUser = agentStatus!=null && agentStatus.getUserList()!=null && agentStatus.getUserList().size()>0? agentStatus.getUserList().get(0) : null ;
			
			
			if(agentStatus!=null && agentStatus.getUserList().size()>0){
				view.addObject("contract", PersistenceFactory.getInstance().getSnsUserInfo(agentUser.getUserid(), agentUser.getChannel(), orgi)) ;
				fileValue(view , agentUser , agentUser.getUserid() , orgi , data) ;
				//添加FAQ
				view.addObject("faqlist", super.getFaqByUserid(orgi, agentStatus));
				view.addObject("commonlang", super.getCommonLanguage(orgi, agentStatus)) ;
				view.addObject("contractlist", super.getService().findPageByCriteria(DetachedCriteria.forClass(AgentUser.class).add(Restrictions.and(Restrictions.eq("userid", agentUser.getUserid()), Restrictions.eq("agentno", agentUser.getAgentno()))).add(Restrictions.eq("orgi", orgi)).add(Restrictions.not(Restrictions.eq("id", agentUser.getId()))).addOrder(Order.desc("logindate")), super.PAGE_SIZE_TEN , data.getP())) ;
				view.addObject("msglist", PersistenceFactory.getInstance().getMessagetList(agentUser, 0, 1000)) ;
			}else{
				view = request( new ResponseData("redirect:/{orgi}/index/dashboard.html"), orgi, data);
			}
		}
		
		return view ;
    }
	
	@RequestMapping(value = "/chat/{id}")
    public ModelAndView agent(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/agent/userblock"  ) ; 
		ModelAndView view = request(responseData, orgi, data) ;
		User user = super.getUser(request) ;
		if(user!=null){
			AgentStatus agentStatus = user.getAgentstatus() ;
			AgentUser agentUser = null ;
			for(AgentUser tmp : agentStatus.getUserList()){
				if(tmp.getId().equals(id) || tmp.getUserid().equals(id)){
					agentUser = tmp ;
					break ;
				}
			}
			if(agentUser==null){
				if(id.length()==32){
					agentUser=(AgentUser) super.getService().getIObjectByPK(AgentUser.class, id);
				}else{
					List<AgentUser> agents=super.getService().findPageByCriteria(DetachedCriteria.forClass(AgentUser.class).add(Restrictions.and(Restrictions.eq("orgi", orgi),Restrictions.eq("userid", id))).addOrder(Order.desc("logindate")),1,1);
					if(agents!=null && agents.size()>0){
						agentUser=agents.get(0);
					}
				}
				if(agentUser!=null){
					agentUser.setSnsuser(PersistenceFactory.getInstance().getSnsUserInfo(agentUser.getUserid(), agentUser.getChannel() , agentUser.getOrgi())) ;
					List<AgentUser> userList = new ArrayList();
					userList.add(agentUser);
					agentconsole(view , data , orgi ,user , agentUser , userList) ;
				}
			}else{
				agentconsole(view , data , orgi ,user , agentUser , user.getAgentstatus().getUserList()) ;
			}
			
		}
		return view ;
    }
	
	@RequestMapping(value = "/chat/his/{id}")
    public ModelAndView agenthis(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData( "/pages/agent") ; 
		ModelAndView view = request(responseData, orgi, data) ;
		User user = super.getUser(request) ;
		if(user!=null){
			AgentUser agentUser = null ;
			if(agentUser==null){
				agentUser=(AgentUser) super.getService().getIObjectByPK(AgentUser.class, id);
			}
			agentUser.setFromhis(true) ;
//			user.getAgentstatus().getUserList().add(agentUser) ;
//			ServiceQueue.addUserToQueue(orgi, agentUser) ;
			
			/*WeiXinUser wx=new WeiXinUser();
			wx.setFakeId("1975198961");
			wx.setUserid("1975198961");
			wx.setChannel("weixin");
			wx.setOrgi(orgi);
			wx.setUsername("长虹");
			wx.setNickName("长虹");
			wx.setApiusername("ocP-SjrnCbTimltWm7rJTmA82SmM");
			RivuDataContext.getService().saveIObject(wx);*/
			agentUser.setSnsuser(PersistenceFactory.getInstance().getSnsUserInfo(agentUser.getUserid(), agentUser.getChannel() , agentUser.getOrgi())) ;
			List<AgentUser> userList = new ArrayList();
			userList.add(agentUser);
			agentconsole(view , data , orgi ,user , agentUser ,userList) ;
		} 
		return view ;
    }
	
    public ModelAndView agentconsole(ModelAndView view , RequestData data , String orgi , User user , AgentUser agentUser,List userlist) {
		if(agentUser!=null){
			AgentStatus agentStatus = user.getAgentstatus() ;
			view.addObject("agentstatus", agentStatus) ;
			view.addObject("userlist", userlist) ;
			fileValue(view , agentUser , agentUser.getUserid() , orgi , data) ;
			//添加FAQ
			view.addObject("faqlist", super.getFaqByUserid(orgi, agentStatus));
			view.addObject("commonlang", super.getCommonLanguage(orgi, agentStatus)) ;
			view.addObject("contractlist", super.getService().findPageByCriteria(DetachedCriteria.forClass(AgentUser.class).add(Restrictions.and(Restrictions.eq("userid", agentUser.getUserid()), Restrictions.eq("agentno", agentUser.getAgentno()))).add(Restrictions.eq("orgi", orgi)).add(Restrictions.not(Restrictions.eq("id", agentUser.getId()))).addOrder(Order.desc("logindate")), super.PAGE_SIZE_TEN , data.getP())) ;
			view.addObject("msglist", PersistenceFactory.getInstance().getMessagetList(agentUser, 0, 1000)) ;
		}
		return view ;
    }
	
	private void fileValue(ModelAndView view , AgentUser agentUser , String userid ,String orgi , RequestData data){
		view.addObject("contract", PersistenceFactory.getInstance().getSnsUserInfo(agentUser.getUserid(), agentUser.getChannel(), orgi)) ;
		view.addObject("agentuser", agentUser) ;
		//来访次数的统计
		view.addObject("usertimes",super.getService().getCountByCriteria(DetachedCriteria.forClass(AgentUser.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("userid", userid)))));
	}
	
	@RequestMapping(value = "/msg/{msgid}/{channel}")
    public ModelAndView msgid(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String msgid, @PathVariable String channel, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/agent/userchat"  ) ; 
		ModelAndView view = request(responseData, orgi, data) ;
		Channel message = PersistenceFactory.getInstance().getMessage(msgid, channel , orgi) ;
		view.addObject("msg", message) ;
		view.addObject("newmsg", true) ;
		view.addObject("contract", ServiceQueue.getAgentUser(orgi, message.getUserid()));
		return view ;
    }
	
	@RequestMapping(value = "/img/{id}/{channel}")
    public void getimg(HttpServletRequest request , HttpServletResponse response , @PathVariable String orgi, @PathVariable String id,@PathVariable String channel,@ModelAttribute("data") RequestData data) {
		Channel message = PersistenceFactory.getInstance().getMessage(id, channel , orgi) ;
		try {
			response.setContentType("image/png") ;
			response.getOutputStream().write(message.getBytedata()) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ;
    }
	@RequestMapping(value = "/media/{id}/{channel}")
    public void getmedia(HttpServletRequest request , HttpServletResponse response , @PathVariable String orgi, @PathVariable String id,@PathVariable String channel,@ModelAttribute("data") RequestData data) throws IOException {
//		Channel message = PersistenceFactory.getInstance().getMessage(id, channel , orgi) ;
		Channel message = (Channel) super.getService().getIObjectByPK(WeiXin.class, id) ;
		try {
			if(message.getMessagetype().equals(EapDataContext.MessageType.VOICE.toString())){
				response.setContentType("audio/mp3") ;
			}else if(message.getMessagetype().equals(EapDataContext.MessageType.VIDEO.toString())){
				response.setContentType("video/mp4") ;
			}
			response.getOutputStream().write(message.getBytedata()) ;
			response.getOutputStream().flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	@RequestMapping(value = "/exchange/{id}")
    public ModelAndView exchange(HttpServletRequest request , HttpServletResponse response , @PathVariable String orgi, @PathVariable String id,@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/agent/exchange" , "/pages/include/iframeindex") ; 
		ModelAndView view = request(responseData, orgi, data) ;
		User user = super.getUser(request) ;
		view.addObject("orgi", orgi) ;
		view.addObject("userid", id) ;
		view.addObject("setting", EapSmcDataContext.getSearchSetting(orgi)) ;
		view.addObject("skill", super.getService().findPageByCriteria(DetachedCriteria.forClass(AgentSkill.class).add(Restrictions.eq("orgi", orgi)))) ;
		if(user!=null){
			AgentStatus agentStatus = user.getAgentstatus() ;
			view.addObject("agentlist", ServiceQueue.getAgentList( agentStatus.getAgentno() , orgi)) ;
		}
		return view;
    }
	
	@RequestMapping(value = "/exchange/skill/{id}/{code}")
    public ModelAndView exchangeskill(HttpServletRequest request , HttpServletResponse response , @PathVariable String orgi,@PathVariable String id, @PathVariable String code,@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/agent/skillagent") ; 
		ModelAndView view = request(responseData, orgi, data) ;
		User user = super.getUser(request) ;
		view.addObject("orgi", orgi) ;
		view.addObject("userid", id) ;
		view.addObject("setting", EapSmcDataContext.getSearchSetting(orgi)) ;
		if(user!=null){
			AgentStatus agentStatus = user.getAgentstatus() ;
			view.addObject("agentlist", ServiceQueue.getAgentListBySkill( agentStatus.getAgentno() , orgi , code)) ;
		}
		return view;
    }
	
	@RequestMapping(value = "/exchangedo/{id}")
    public ModelAndView exchangedo(HttpServletRequest request , HttpServletResponse response , @PathVariable String orgi, @PathVariable String id, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/public/success") ; 
		ModelAndView view = request(responseData, orgi, data) ;
		String agentno = request.getParameter("agentno") ;
		if(agentno==null||agentno.equals("")){
			return request(new ResponseData("redirect://pages/agent/skillagent.html" , "要转入坐席不能为空，请重新选择！" , true , null), orgi, null) ;
		}
		User user = super.getUser(request) ;
		if(user!=null){
			//获取转接用户的AgentStatus
			{
				//获取当前用户的agentStatus
				AgentStatus agentStatus = ServiceQueue.getAgentQueue().get(user.getAgentno()) ;
				//目标AgentStatus
				AgentStatus descas = ServiceQueue.getAgentQueue().get(agentno) ;
				if(descas.getUserList().size()>=5){
					return request(new ResponseData("redirect://pages/agent/skillagent.html" , "要转入坐席服务客户已满，请重新选择！" , true , null), orgi, null) ;
				}
				for(int i =0; i<agentStatus.getUserList().size();i++){
					AgentUser agentuser=agentStatus.getUserList().get(i);
					if(agentuser.getUserid().equals(id)){
						//刷新发起方的在线绿人
						APIContext.sendToAgent(Constant.APP_CHANNEL, user.getAgentno() , new DataMessage(agentuser.getUserid(),Constant.NEWUSER,  new WeiXin() , orgi, agentuser.getUserid()) , orgi) ;
						//从当前客服中移除
						agentStatus.getUserList().remove(i);
						ServiceQueue.getAgentQueue().put(user.getAgentno(), agentStatus);
						if(agentno!=null){
							ServiceQueue.getUserQueue().remove(agentuser.getUserid()) ;
							//刷新接收方的在线绿人
							APIContext.sendToAgent(Constant.APP_CHANNEL, agentno, new DataMessage(agentuser.getUserid(),Constant.NEWUSER,  new WeiXin() , orgi,agentuser.getUserid()) , orgi) ;
							//坐席服务客户数改变了，同步到客服队列
							agentuser.setAgentno(agentno);
							agentuser.setLastmessage(new Date());
							agentuser.setFromhis(false);
							descas.getUserList().add(agentuser);
							ServiceQueue.getAgentQueue().put(agentno, descas);
							//更新队列里的AgentStatus，避免排队的时候不更新数据
							ServiceQueue.removeToLastItem(descas,orgi);
							ServiceQueue.getUserQueue().put(agentuser.getUserid(), agentuser) ;
						}
						break ;
					}
				}
			}
		}
		return view;
    }
	
	@RequestMapping(value = "/completion/{id}/{chl}")
    public ModelAndView completion(HttpServletRequest request , HttpServletResponse response , @PathVariable String orgi, @PathVariable String id,@PathVariable String chl,@ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/agent/index.html") ; 
		ModelAndView view = request(responseData, orgi, data) ;
		if(data!=null && data.getUserid()!=null && ServiceQueue.getUserQueue().get(data.getUserid())!=null){
			Channel channel = (Channel) EapDataContext.getSNSUserBean(chl, EapDataContext.SNSBeanType.MESSAGE.toString()).newInstance() ;
			channel.setContextid(data.getContextid()) ;
			channel.setTouser(data.getUserid()) ;
			channel.setChannel(chl) ;
			channel.setReplytype(EapDataContext.ReplyType.MANUALLY.toString()) ;
			channel.setUserid(data.getUserid()) ;
			channel.setUsername(super.getUser(request).getUsername()) ;
			channel.setText(EapSmcDataContext.getSearchSetting(orgi).getDislinkmsg()) ;
			channel.setMessagetype(EapDataContext.MessageType.TEXT.toString()) ;
			channel.setOrgi(orgi) ;
			DataMessage dataMessage = new DataMessage(channel.getChannel() , channel , channel.getOrgi() , channel.getUserid()) ;
			SNSUser agent = (SNSUser) EapDataContext.getSNSUserBean(chl, EapDataContext.SNSBeanType.USER.toString()).newInstance() ;
			agent.setUserid(super.getUser(request).getAgentstatus().getAgentno()) ;
			agent.setOrgi(orgi) ;
			channel.setSnsuser(agent) ;
			APIContext.saveMessage(dataMessage) ;
			APIContext.sendMsg(dataMessage, orgi) ;
		}
		ServiceQueue.completion(orgi, super.getUser(request).getAgentstatus().getAgentno(), super.getUser(request), id) ;
		User user = super.getUser(request) ;
		if(user!=null){
			AgentStatus agentStatus = ServiceQueue.getAgent(user.getAgentno(), orgi);
			view.addObject("agentstatus", agentStatus) ;
			view.addObject("userlist", agentStatus!=null ? agentStatus.getUserList() : null) ;
			view.addObject("inseruser", agentStatus.getUserList().size()) ;
			view.addObject("queueuser", ServiceQueue.getQueueNum(orgi));
		}
		return view;
    }
	@RequestMapping(value = "/share/weibo")
    public ModelAndView weiboshare(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ModelAndView view = request(new ResponseData("/pages/agent/weiboshare" ,"/pages/include/iframeindex"), orgi, data)  ;
		view.addObject("msg", PersistenceFactory.getInstance().getMessage(data.getId(), data.getChannel() , orgi)) ;
		return view;
    }
	
	@RequestMapping(value = "/share/mail")
    public ModelAndView mailshare(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ModelAndView view = request(new ResponseData("/pages/agent/mailshare" ,"/pages/include/iframeindex"), orgi, data)  ;
		view.addObject("msg", PersistenceFactory.getInstance().getMessage(data.getId(), data.getChannel(),orgi)) ;
		return view;
    }
	
	@RequestMapping(value = "/service/getmessage")
    public ModelAndView getmessage(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ModelAndView view = request(new ResponseData("/pages/agent/getmessage" ), orgi, data)  ;
		User user=super.getUser(request);
		if(user!=null){
			if(ServiceQueue.getAgentQueue()!=null){
				AgentStatus agentStatus = ServiceQueue.getAgentQueue().get(user.getAgentno()) ;
				if(agentStatus!=null && agentStatus.getLastMessage()!=null){
					ServiceQueue.statusChange(orgi, agentStatus.getAgentno(), null ,user );
					
					agentStatus = ServiceQueue.getAgentQueue().get(user.getAgentno()) ;
					if(agentStatus.getLastMessage().size()>0){
						Iterator<String> iterator = agentStatus.getLastMessage().keySet().iterator() ;
						List<List<DataMessage>> lastMsg = new ArrayList<List<DataMessage>>();
						while(iterator.hasNext()){
							lastMsg.add(agentStatus.getLastMessage().get(iterator.next())) ;
						}
						view.addObject("agent", lastMsg) ;
					}
					synchronized (ServiceQueue.getAgentQueue()) {
						agentStatus = ServiceQueue.getAgentQueue().get(user.getAgentno()) ;
						agentStatus.getLastMessage().clear(); 
						ServiceQueue.getAgentQueue().put(agentStatus.getAgentno(), agentStatus) ;
					}
					
//					System.out.println(agentStatus.getUserList().size());
					view.addObject("inseruser", agentStatus.getUserList().size()) ;
					view.addObject("queueuser", ServiceQueue.getQueueNum(orgi));
				}
			}
		}
		return view;
    }
	
	/**
	 * FAQ_Search
	 * @param request
	 * @param orgi
	 * @param data
	 * @param rqdata
	 * @return
	 */
	@RequestMapping(value = "/faq/search/{inputValue}")
    public ModelAndView faqSearch(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") FAQModel data,@ModelAttribute("rqdata") RequestData rqdata,@PathVariable String inputValue) {
		String title="%"+inputValue+"%";
		ResponseData responseData = new ResponseData("/pages/agent/faqCheck");
		SearchSetting setting = EapSmcDataContext.getSearchSetting(orgi);
		AgentSkill ask =(AgentSkill)super.getUser(request).getAgentSkill();
		if(setting.isSkill()==false){
			if(inputValue==""||inputValue.equals(null)){
				responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(FAQModel.class).add(Restrictions.eq("orgi", orgi))));
			}
			responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(FAQModel.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.like("title", title)))));
		}
		
		if(setting.isSkill()==true && ask!=null){
			if(inputValue==""||inputValue.equals(null)){
				responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(FAQModel.class).add(Restrictions.and(Restrictions.eq("orgi", orgi),Restrictions.eq("skillid", ask.getId())))));
			}
			responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(FAQModel.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.like("title", title))).add(Restrictions.eq("skillid", ask.getId()))));
		}
		if(setting.isSkill()==true && ask==null){
			if(inputValue==""||inputValue.equals(null)){
				responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(FAQModel.class).add(Restrictions.eq("orgi", orgi))));
			}
			responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(FAQModel.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.like("title", title)))));
		}
		return request(responseData, orgi , rqdata) ; 
    }
	
}
