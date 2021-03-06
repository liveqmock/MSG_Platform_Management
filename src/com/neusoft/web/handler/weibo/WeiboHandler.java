package com.neusoft.web.handler.weibo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.neusoft.core.EapDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.core.channel.DataMessage;
import com.neusoft.core.channel.WeiXin;
import com.neusoft.util.persistence.PersistenceFactory;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;
import com.neusoft.web.model.FavMessage;
import com.neusoft.web.model.LogicDatabase;
import com.neusoft.web.model.SNSAccount;
import com.neusoft.web.model.TypeCategory;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/weibo")
public class WeiboHandler extends Handler{
	@RequestMapping(value = "/index")
    public ModelAndView index(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/index"  ) ; 
		return request(responseData, orgi ,data) ;
    }
	
	@RequestMapping(value = "/report")
    public ModelAndView report(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/report"  ) ; 
		responseData.setDataList(APIContext.getMessageListBySubType(EapDataContext.ChannelTypeEnum.WEIBO.toString(), "post", orgi, data.getPs(),data.getP())) ;
		
		ModelAndView view = request(responseData, orgi , data) ;
		view.addObject("snsaccount", super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", EapDataContext.ChannelTypeEnum.WEIBO.toString()))))) ;
		return view ;
    }
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/report/send")
    public ModelAndView sendreport(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/report.html"  ) ; 
		List<SNSAccount> snsAccountList = super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", EapDataContext.ChannelTypeEnum.WEIBO.toString())))) ;
		WeiXin channel = (WeiXin) EapDataContext.getSNSUserBean(data.getChannel(), EapDataContext.SNSBeanType.MESSAGE.toString()).newInstance() ;
		channel.setChannel(EapDataContext.ChannelTypeEnum.WEIBO.toString()) ;
		((WeiXin)channel).setSubtype("post");
		channel.setReplytype(EapDataContext.ReplyType.MANUALLY.toString()) ;
		channel.setUsername(super.getUser(request).getUsername()) ;
		channel.setText(data.getContent()) ;
		
		if(channel.getText().trim().length()>0){
			channel.setMessagetype(EapDataContext.MessageType.TEXT.toString()) ;
			channel.setOrgi(orgi) ;
			channel.setContextid(request.getSession().getId()) ;
			channel.setTouser(data.getUsername()) ;
			channel.setUserid(super.getUser(request).getId()) ;
			channel.setCreatetime(new Date()) ;
			DataMessage dataMessage = new DataMessage(channel.getChannel() , channel , channel.getOrgi() , channel.getUserid()) ;
			/**
			 * SOURCE , 使用 的微博账号 
			 */
			channel.setSource(request.getParameter("source"));
//			APIContext.sendMessageToUser(dataMessage) ;	//发布微博
			
			for(String source : request.getParameterValues("source")){
				for(SNSAccount snsAcount : snsAccountList){
					if(source.trim().equals(snsAcount.getId())){
						//保存到微博发布状态表
						//保存代码
						TypeCategory category = new TypeCategory();
						category.setParentid(source) ;
						category.setDescription(channel.getText()) ;
						category.setCtype(channel.getId()) ;
						category.setIconstr(super.getUser(request).getUsername()) ;
						category.setOrgi(orgi) ;
						category.setCatetype(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())) ;
						category.setTitle(snsAcount.getAccount()) ;
						if(snsAcount.getName()!=null && snsAcount.getName().equals("1")){
							category.setName("1") ;//需要发布审核
							category.setCode("0") ;//未发布
						}else{
							//无需审核，直接发布
							category.setName("0") ;	//无需审核
							category.setCode("1") ;//已发布
							channel.setSource(source) ;
							if(snsAcount.getAccount()!=null){
								if(channel.getStatus()!=null){
									channel.setStatus(channel.getStatus()+","+ snsAcount.getAccount()) ;
								}else{
									channel.setStatus(snsAcount.getAccount()) ;
								}
							}
							APIContext.sendMessageToUser(dataMessage) ;	//发布微博
						}
						super.getService().saveIObject(category) ;
						break ;
					}
				}
			}
			APIContext.saveMessage(dataMessage) ;	//保存数据
		}
		return request(responseData, orgi , data) ;
    }
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/monitor")
    public ModelAndView monitor(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/monitor"  ) ; 
		List<SNSAccount> snsAccountList = super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", "weibokeyword")))) ;
		responseData.setDataList(snsAccountList) ;
		responseData.setData(snsAccountList.size()>0 ? snsAccountList.get(0) : null) ;
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/rm/{id}")
    public ModelAndView rm(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/report.html"  ) ; 
		WeiXin channel = (WeiXin) PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi) ;
		if(channel.getStatus()!=null){
			responseData.setError("微博已经发布，无法删除") ;
		}else{
			PersistenceFactory.getInstance().rmMessage(channel) ;
			List<TypeCategory> typeCategoriesList = super.getService().findAllByCriteria(DetachedCriteria.forClass(TypeCategory.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("ctype", id)))) ;
			for(TypeCategory tc : typeCategoriesList){
				super.getService().deleteIObject(tc) ;
			}
		}
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/check")
    public ModelAndView check(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/check"  ) ; 
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(TypeCategory.class).add(Restrictions.eq("code", "0")).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("name", "1"))))) ;
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/check/{id}/{weixinid}")
    public ModelAndView checkmsg(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id, @PathVariable String weixinid, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/checkweibo" , "/pages/include/iframeindex" ) ; 
		responseData.setData(super.getService().getIObjectByPK(TypeCategory.class, id)) ;
		ModelAndView view = request(responseData, orgi , data) ;
		view.addObject("msg", PersistenceFactory.getInstance().getMessage(weixinid, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return view;
    }
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/checkdo/{id}/{weixinid}")
    public ModelAndView checkdo(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id, @PathVariable String weixinid, @ModelAttribute("data") LogicDatabase data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/public/success"  ) ; 
		TypeCategory type = (TypeCategory) super.getService().getIObjectByPK(TypeCategory.class, id) ;
		if(data!=null && "1".equals(data.getMappingtype())){
			type.setCode("1") ;
			WeiXin channel = (WeiXin) PersistenceFactory.getInstance().getMessage(weixinid, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi) ;
			DataMessage dataMessage = new DataMessage(channel.getChannel() , channel , channel.getOrgi() , channel.getUserid()) ;
			if(channel.getStatus()!=null && channel.getStatus().length()>0){
				channel.setStatus(channel.getStatus()+","+type.getTitle()) ;
			}else{
				channel.setStatus(type.getTitle()) ;
			}
			APIContext.sendMessageToUser(dataMessage) ;	//发布微博
			super.getService().updateIObject(type) ;
			PersistenceFactory.getInstance().updateMessage(channel) ;
		}else{
			
		}
		return request(responseData, orgi , new RequestData()) ;
    }
	
	@RequestMapping(value = "/at")
    public ModelAndView at(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/at"  ) ; 
		responseData.setDataList(APIContext.getMessageListBySubType(EapDataContext.ChannelTypeEnum.WEIBO.toString(), "at", orgi, data.getPs(),data.getP())) ;
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/primsg")
    public ModelAndView primsg(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/primsg"  ) ; 
		responseData.setDataList(APIContext.getMessageListBySubType(EapDataContext.ChannelTypeEnum.WEIBOPRIMSG.toString(), null , orgi, data.getPs(),data.getP())) ;
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/atuser/{id}")
    public ModelAndView atuser(HttpServletRequest request ,@PathVariable String orgi ,@PathVariable String id , @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/atuser" , "/pages/include/iframeindex"  ) ; 
		responseData.setData(PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/process/{id}")
    public ModelAndView process(HttpServletRequest request ,@PathVariable String orgi ,@PathVariable String id , @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/processweibo" , "/pages/include/iframeindex"  ) ; 
		responseData.setData(PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/processdo")
    public ModelAndView processdo(HttpServletRequest request ,@PathVariable String orgi ,@PathVariable String id , @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/processweibo" , "/pages/include/iframeindex"  ) ; 
		responseData.setData(PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/repost/{id}")
    public ModelAndView repost(HttpServletRequest request ,@PathVariable String orgi ,@PathVariable String id , @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/repost" , "/pages/include/iframeindex"  ) ; //转发
		responseData.setData(PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/comment/{id}")
    public ModelAndView comment(HttpServletRequest request ,@PathVariable String orgi ,@PathVariable String id , @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/comment" , "/pages/include/iframeindex"  ) ; //评论
		responseData.setData(PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/tag/{id}")
    public ModelAndView tag(HttpServletRequest request ,@PathVariable String orgi ,@PathVariable String id , @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/tag" , "/pages/include/iframeindex"  ) ; //评论
		responseData.setData(PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi)) ;
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/account")
    public ModelAndView account(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/account"  ) ; 
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/chance")
    public ModelAndView chance(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/chance"  ) ; 
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/topic")
    public ModelAndView topic(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/topic"  ) ; 
		return request(responseData, orgi , data) ;
    }
	@RequestMapping(value = "/keyword/add")
    public ModelAndView topicadd(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/topicadd"  , "/pages/include/iframeindex"  ) ; 
		List<SNSAccount> accountList = super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", EapDataContext.ChannelTypeEnum.WEIBO.toString())))) ; 
		ModelAndView view  = request(responseData, orgi , data)  ;
		view.addObject("account", accountList) ;
		return view;
    }
	
	@RequestMapping(value = "/keyword/addo")
    public ModelAndView keywordaddo(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") SNSAccount data , @ModelAttribute("rqdata") RequestData rqdata) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/monitor.html"  ) ; 
		data.setCreatetime(new Date());
		data.setOrgi(orgi);
		List<SNSAccount> accountList = super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.eq("username", data.getUsername())).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", data.getSnstype()))));
		if(accountList.size()==0){
			super.getService().saveIObject(data);
			List<SNSAccount> snsAccountList = new ArrayList<SNSAccount>();
			snsAccountList.add(data) ;
			APIContext.sendSNSAccountToGW(snsAccountList) ;
		}else{
			responseData.setError(" 分组名重复，请重新输入") ;
		}
		return request(responseData, orgi , rqdata) ; 
    }
	@RequestMapping(value = "/keyword/export")
    public ModelAndView topicexport(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/topicexport"  ) ; 
		return request(responseData, orgi , data) ;
    }
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/keyword/edit/{id}")
    public ModelAndView topicedit(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id,@ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/topicedit"  , "/pages/include/iframeindex"   ) ; 
		responseData.setData(super.getService().getIObjectByPK(SNSAccount.class, id)) ;
		List<SNSAccount> accountList = super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", EapDataContext.ChannelTypeEnum.WEIBO.toString())))) ; 
		ModelAndView view  = request(responseData, orgi , data)  ;
		view.addObject("account", accountList) ;
		return view ;
    }
	
	@RequestMapping(value = "/keyword/editdo")
    public ModelAndView keywordeditdo(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") SNSAccount data , @ModelAttribute("rqdata") RequestData rqdata) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/monitor.html"  ) ; 
		data.setCreatetime(new Date());
		data.setOrgi(orgi);
		List<SNSAccount> accountList = super.getService().findPageByCriteria(DetachedCriteria.forClass(SNSAccount.class).add(Restrictions.eq("username", data.getUsername())).add(Restrictions.not(Restrictions.eq("id", data.getId()))).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("snstype", data.getSnstype()))));
		if(accountList.size()==0){
			super.getService().updateIObject(data);
			List<SNSAccount> snsAccountList = new ArrayList<SNSAccount>();
			snsAccountList.add(data) ;
			APIContext.sendSNSAccountToGW(snsAccountList) ;
		}else{
			responseData.setError(" 分组名重复，请重新输入") ;
		}
		return request(responseData, orgi , rqdata) ; 
    }
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/keyword/rm/{id}")
    public ModelAndView keywordrm(HttpServletRequest request ,@PathVariable String orgi, @PathVariable String id,@ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/monitor.html"  ) ;
		SNSAccount snsAccount = new SNSAccount();
		snsAccount.setId(id) ;
		super.getService().deleteIObject(snsAccount) ;
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/usermonitor")
    public ModelAndView usermonitor(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/usermonitor"  ) ; 
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/fav")
    public ModelAndView fav(HttpServletRequest request ,@PathVariable String orgi, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/fav"  ) ; 
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(FavMessage.class).add(Restrictions.eq("orgi", orgi)).add(Restrictions.eq("favuser", super.getUser(request).getId())) , data.getPs() , data.getP())) ;
		return request(responseData, orgi , data) ;
    }
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/fav/{id}")
    public ModelAndView favdo(HttpServletRequest request ,@PathVariable String orgi,@PathVariable String id, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/at.html"  ) ;
		WeiXin weixin  = (WeiXin) PersistenceFactory.getInstance().getMessage(id, EapDataContext.ChannelTypeEnum.WEIBO.toString(), orgi) ;
		
		List<FavMessage> favMsgList = super.getService().findPageByCriteria(DetachedCriteria.forClass(FavMessage.class).add(Restrictions.eq("orgi", orgi)).add(Restrictions.eq("weixinid", weixin.getId()))) ;
		if(favMsgList.size()==0){
			FavMessage favMsg = new FavMessage() ;
			BeanUtils.copyProperties(favMsg, weixin) ;
			favMsg.setFavuser(super.getUser(request).getId()) ;
			favMsg.setWeixinid(weixin.getId()) ;
			super.getService().saveIObject(favMsg) ;
		}else{
			responseData.setError("已收藏") ;
		}
		return request(responseData, orgi , data) ;
    }
	
	@RequestMapping(value = "/favcheck/{id}")
    public ModelAndView favcheck(HttpServletRequest request ,@PathVariable String orgi,@PathVariable String id, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("/pages/weibo/favcheck"  ) ; 
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(FavMessage.class).add(Restrictions.eq("orgi", orgi)).add(Restrictions.eq("weixinid", id))) );
		ModelAndView view = request(responseData, orgi , data) ; 
		view.addObject("id", id) ;
		return view ;
    }
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/fav/rm/{id}")
    public ModelAndView favrm(HttpServletRequest request ,@PathVariable String orgi,@PathVariable String id, @ModelAttribute("data") RequestData data) throws Exception {
		ResponseData responseData = new ResponseData("redirect:/{orgi}/weibo/fav.html"  ) ;
		FavMessage favMsg = new FavMessage();
		favMsg.setId(id) ;
		super.getService().deleteIObject(favMsg);
 		return request(responseData, orgi , data) ; 
    }
	
}