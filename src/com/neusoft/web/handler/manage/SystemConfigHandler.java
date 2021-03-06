package com.neusoft.web.handler.manage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.hazelcast.core.Hazelcast;
import com.neusoft.core.EapDataContext;
import com.neusoft.core.EapSmcDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.util.queue.AgentStatus;
import com.neusoft.util.queue.ServiceQueue;
import com.neusoft.util.rpc.message.Message;
import com.neusoft.util.rpc.message.SystemMessage;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;
import com.neusoft.web.model.PageTemplate;
import com.neusoft.web.model.SearchSetting;
import com.neusoft.web.model.SystemConfig;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/sysconf")
@SuppressWarnings("unchecked")
public class SystemConfigHandler extends Handler {
	@RequestMapping(value = "/edit/{sysconfid}")
	public ModelAndView edit(HttpServletRequest request, @PathVariable String orgi,@PathVariable String sysconfid,@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/systemconfig/sysconfedit", "/pages/include/iframeindex");
		responseData.setData(super.getService().getIObjectByPK(SystemConfig.class, sysconfid));
		return request(responseData, orgi, data);
	}
	@RequestMapping(value = "/look/{sysconfid}")
	public ModelAndView look(HttpServletRequest request, @PathVariable String orgi,@PathVariable String sysconfid,@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/systemconfig/sysconflook", "/pages/include/iframeindex");
		responseData.setData(super.getService().getIObjectByPK(SystemConfig.class, sysconfid));
		return request(responseData, orgi, data);
	}
	@RequestMapping(value="/clean")
	public ModelAndView clean(HttpServletRequest request, @PathVariable String orgi){
		ResponseData responseData=new ResponseData("redirect:/{orgi}/index/systemconf/");
		responseData.setError("静态接口更新成功") ;
		return request(responseData, orgi, null);
	}
	@RequestMapping(value = "/editdo")
	public ModelAndView editdo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SearchSetting data) {
		String hisskill=request.getParameter("hisskill");
		String nowskill=request.getParameter("skill");
		if(ServiceQueue.getUserQueue()!=null&&ServiceQueue.getUserQueue().size()>0){
			request(new ResponseData("redirect://index/systemconf.html" , "当前有在服务中的客户，不可修改" , true , null), orgi, null) ;
		}
		if(hisskill!=null && nowskill !=null && !nowskill.equals(hisskill)){
			//登录用户列表
			Iterator<String> userIterator = ServiceQueue.getAgentQueue().keySet().iterator() ;
			boolean isanyone=false;
			while(userIterator.hasNext()){
				String userid = userIterator.next() ;
				AgentStatus as = ServiceQueue.getAgentQueue().get(userid) ;
				if(as!=null && as.getId()!=null && super.getUser(request).getId()!=null && !as.getId().equals(super.getUser(request).getId())){
					isanyone=true;
					break;
				}
			}
			if(isanyone){
				return request(new ResponseData("redirect://index/systemconf.html" , "当前有其他坐席已经登录，不可修改" , true , null), orgi, null) ;
			}
		}
		data.setOrgi(orgi);
		if(data.getId()==null || data.getId().length()==0){
			data.setId(null) ;
			super.getService().saveIObject(data);
		}else{
			super.getService().updateIObject(data);
		}
		//是否启用技能组，重启系统后生效
//		boolean tem=SmcRivuDataContext.getSearchSetting().get(orgi).isSkill();
//		data.setSkill(tem);
		//TODO 需要更新到map里，实时生效
		EapSmcDataContext.getSearchSetting().put(orgi , data);
		//推送到GW去
		List<SearchSetting> list=new ArrayList<SearchSetting>();
		list.add(data);
		/*//设置代理IP和端口
		if(data.getLoginurl()!=null && data.getLoginurl().length()>0 && data.getRegurl()!=null && data.getRegurl().length()>0){
			System.setProperty("proxySet", "true");
			System.setProperty("proxyHost", data.getLoginurl()) ;
			System.setProperty("proxyPort", data.getRegurl());
		}else{
			System.clearProperty("proxySet");
			System.clearProperty("proxyHost") ;
			System.clearProperty("proxyPort");
		}*/
		if(APIContext.getRpcServers().size()>0){
			APIContext.getRpcServer().sendMessageToServer(
				new Message(EapDataContext.HANDLER, JSON.toJSONString(new SystemMessage(EapDataContext.SystemRPComman.SETTINGPUBLISH.toString(), list), SerializerFeature.WriteClassName)));
		}
//		List<PageTemplate> list = super.getService().findAllByCriteria(DetachedCriteria.forClass(PageTemplate.class).add(Restrictions.eq("orgi", orgi)));
//		APIContext.getRpcServer().sendMessageToServer(
//				new Message(RivuDataContext.HANDLER, JSON.toJSONString(new SystemMessage(RivuDataContext.SystemRPComman.SETTINGPUBLISH.toString(), list), SerializerFeature.WriteClassName)));
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}
	@RequestMapping(value = "/tablelist/{conftype}")
	public ModelAndView agentinfolist(HttpServletRequest request, @PathVariable String orgi,@PathVariable String conftype, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/systemconfig/sysconfiglist");
		responseData.setResult(conftype);
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SystemConfig.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("conftype", conftype))),data.getPs(),data.getP()));
		return request(responseData, orgi, data);
	}
	@RequestMapping(value = "/add/{conftype}")
	public ModelAndView add(HttpServletRequest request, @PathVariable String orgi,@PathVariable String conftype) {
		ResponseData responseData = new ResponseData("/pages/manage/systemconfig/sysconfigadd", "/pages/include/iframeindex");
		ModelAndView view = request(responseData, orgi, null);
		view.addObject("conftype",conftype);
		return view;
	}
	@RequestMapping(value = "/adddo")
	public ModelAndView adddo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SystemConfig data) {
		data.setOrgi(orgi);
		super.getService().saveIObject(data);
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}
	@RequestMapping(value = "/rm/{conftype}/{sysconfid}")
	public ModelAndView rm(HttpServletRequest request, @PathVariable String orgi,@PathVariable String sysconfid,@PathVariable String conftype, @ModelAttribute("data") SystemConfig data) {
		data.setId(sysconfid);
		ResponseData responseData = new ResponseData("/pages/manage/systemconfig/sysconfiglist");
		super.getService().deleteIObject(data);
		responseData.setResult(sysconfid);
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SystemConfig.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("conftype", conftype)))));
		return request(responseData, orgi, null);
	}
	@RequestMapping(value = "/search")
	public ModelAndView search(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SystemConfig data) {
		String key = "%" + data.getName() + "%";
		ResponseData responseData = new ResponseData("/pages/manage/systemconfig/sysconfiglist");
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SystemConfig.class).add(Restrictions.and(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("conftype", data.getConftype())) ,Restrictions.like("name", key)))));
		responseData.setResult(data.getConftype());
		return request(responseData, orgi, null);
	}
}
