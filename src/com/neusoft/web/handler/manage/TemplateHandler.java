package com.neusoft.web.handler.manage;

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

import com.neusoft.core.EapDataContext;
import com.neusoft.core.EapSmcDataContext;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;
import com.neusoft.web.model.ExtensionPoints;
import com.neusoft.web.model.SearchResultTemplet;

import edu.emory.mathcs.backport.java.util.Arrays;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/template")
@SuppressWarnings("unchecked")
public class TemplateHandler extends Handler {

	@RequestMapping(value = "/tablelist/{channel}")
	public ModelAndView tablelist(HttpServletRequest request, @PathVariable String orgi, @PathVariable String channel,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData(null,"/pages/manage/template/tablelist",super.getService().findPageByCriteria(DetachedCriteria.forClass(SearchResultTemplet.class).add(Restrictions.and(Restrictions.eq("orgi", orgi),Restrictions.eq("channel", channel))),  data.getPs(),data.getP()));
		responseData.setResult(channel);
		return request(responseData, orgi, data);
	}

	@RequestMapping(value = "/changetype/{etpid}")
	public ModelAndView changetype(HttpServletRequest request, @PathVariable String orgi, @PathVariable String etpid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/extensionpoint/tablelist", super.getService().findPageByCriteria(
				DetachedCriteria.forClass(ExtensionPoints.class).add(Restrictions.and(Restrictions.eq("orgi", orgi),
						Restrictions.eq("extensiontype", EapDataContext.PluginType.INSTRUCTION.toString()))), data.getPs(), data.getP()), data);
		responseData.setResult(etpid);
		return request(responseData, orgi, data);
	}

	@RequestMapping(value = "/add/{templatetype}/{channel}")
	public ModelAndView add(HttpServletRequest request, @PathVariable String orgi,@PathVariable String templatetype, @PathVariable String channel,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/template/add", "/pages/include/iframeindex");
		responseData.setDataList(Arrays.asList(EapDataContext.ChannelTypeEnum.class.getEnumConstants()));
		ModelAndView view = request(responseData, orgi, data);
		view.addObject("templatetype", templatetype);
		view.addObject("channel", channel);
		return view;
	}

	@RequestMapping(value = "/adddo")
	public ModelAndView adddo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SearchResultTemplet data) {
		List list=super.getService().findAllByCriteria(DetachedCriteria.forClass(SearchResultTemplet.class).add(Restrictions.eq("channel", data.getChannel())).add(Restrictions.and(Restrictions.eq("orgi", orgi),Restrictions.eq("code", data.getCode()))));
		if(list!=null&&list.size()<1){
			data.setOrgi(orgi);
			super.getService().saveIObject(data);
			// 更新到缓存的模板中去
			EapSmcDataContext.getSearchResultTempletList(data.getChannel()).add(data);
		}else{
			return request(new ResponseData("redirect://tablelist/"+data.getChannel()+".html" , "代码 "+data.getCode()+" 已存在，请重新输入" , true , null), orgi, null) ;
		}
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}

	@RequestMapping(value = "/rm/{templateid}")
	public ModelAndView rm(HttpServletRequest request, @PathVariable String orgi, @PathVariable String templateid,
			@ModelAttribute("data") SearchResultTemplet data) {
		List<ExtensionPoints> plugins = super.getService().findAllByCriteria(DetachedCriteria.forClass(ExtensionPoints.class).add(Restrictions.eq("iconimagepath", data.getCode()))) ;
		if(plugins!=null&&plugins.size()>0){
			ResponseData responseData=new ResponseData("redirect:/{orgi}/template/changetype/"+data.getChannel());
			responseData.setError("在插件中有引用，请先删除插件中对该模板的引用") ;
			return request(responseData, orgi, null);
		}else{
			data.setId(templateid);
			// 执行数据库删除操作
			super.getService().deleteIObject(data);
			// 更新到缓存的模板中去
			for(SearchResultTemplet srt : EapSmcDataContext.getSearchResultTempletList(data.getChannel())){
				if(srt.getId().equals(data.getId())){
					EapSmcDataContext.getSearchResultTempletList(data.getChannel()).remove(srt) ;
					break ;
				}
			}
		}
		return request(new ResponseData("redirect:/{orgi}/template/tablelist/"+data.getChannel()+".html"), orgi, null);
	}

	@RequestMapping(value = "/edit/{templateid}")
	public ModelAndView eidt(HttpServletRequest request, @PathVariable String orgi, @PathVariable String templateid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/template/edit", "/pages/include/iframeindex");
		responseData.setData(super.getService().getIObjectByPK(SearchResultTemplet.class, templateid));
		return request(responseData, orgi, data);
	}
	
	@RequestMapping(value = "/codeedit/{templateid}")
	public ModelAndView codeedit(HttpServletRequest request, @PathVariable String orgi, @PathVariable String templateid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/template/codeedit", "/pages/include/iframeindex");
		responseData.setData(super.getService().getIObjectByPK(SearchResultTemplet.class, templateid));
		return request(responseData, orgi, data);
	}
	
	@RequestMapping(value = "/codeeditdo")
	public ModelAndView codeeditdo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SearchResultTemplet data) {
		SearchResultTemplet searchTempletResult = (SearchResultTemplet) super.getService().getIObjectByPK(SearchResultTemplet.class, data.getId()) ;
		searchTempletResult.setTemplettext(data.getTemplettext()) ;
		data = searchTempletResult ;
		super.getService().updateIObject(searchTempletResult);
		// 更新到缓存的模板中去
		for(int i=0 ; i< EapSmcDataContext.getSearchResultTempletList(data.getChannel()).size() ; i++){
			SearchResultTemplet srt = EapSmcDataContext.getSearchResultTempletList(data.getChannel()).get(i) ;
			if(srt.getId().equals(data.getId())){
				EapSmcDataContext.getSearchResultTempletList(data.getChannel()).set(i, data) ;
				break ;
			}
		}
		
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}
	
	@RequestMapping(value = "/editdo")
	public ModelAndView editdo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SearchResultTemplet data) {
		List<SearchResultTemplet> list=super.getService().findAllByCriteria(DetachedCriteria.forClass(SearchResultTemplet.class).add(Restrictions.eq("channel", data.getChannel())).add(Restrictions.and(Restrictions.eq("orgi", orgi),Restrictions.eq("code", data.getCode()))));
		if((list.size()==0)||(list!=null&&list.size()>0&&list.get(0).getId().equals(data.getId()))){
			super.getService().updateIObject(data);
			// 更新到缓存的模板中去
			for(int i=0 ;i<EapSmcDataContext.getSearchResultTempletList(data.getChannel()).size() ;){
				SearchResultTemplet srt = EapSmcDataContext.getSearchResultTempletList(data.getChannel()).get(i);
				if(srt.getId().equals(data.getId())){
					EapSmcDataContext.getSearchResultTempletList(data.getChannel()).remove(i) ;
				}else{
					i++ ;
				}
			}
			EapSmcDataContext.getSearchResultTempletList(data.getChannel()).add(data);
		}else{
			return request(new ResponseData("redirect://tablelist/"+data.getChannel()+".html" , "代码 "+data.getCode()+" 已存在，请重新输入" , true , null), orgi, null) ;
		}
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}
	
	@RequestMapping(value = "/search")
	public ModelAndView search(HttpServletRequest request, @PathVariable String orgi,@ModelAttribute("data") SearchResultTemplet data) {
		String key = "%" + data.getName() + "%";
		ResponseData responseData = new ResponseData("/pages/manage/template/tablelist");
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SearchResultTemplet.class).add(Restrictions.and(Restrictions.and(Restrictions.eq("orgi", orgi),Restrictions.eq("channel", data.getChannel())) ,Restrictions.like("name", key)))));
		return request(responseData, orgi, null);
	}
	
}
