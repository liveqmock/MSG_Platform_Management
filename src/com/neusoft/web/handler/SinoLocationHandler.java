package com.neusoft.web.handler;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.neusoft.core.EapDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.util.rpc.message.Message;
import com.neusoft.util.rpc.message.SystemMessage;
import com.neusoft.web.model.SinoLocation;
@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/sinolocal")
@SuppressWarnings("unchecked")
public class SinoLocationHandler extends Handler {
	private static String SINOSIG_LOCATION="SINOSIG_LOCATION";
	@RequestMapping(value = "/add/{channel}/{province}")
	public ModelAndView add(HttpServletRequest request, @PathVariable String orgi,@PathVariable String channel,@PathVariable String province,@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/sinosig/location/add", "/pages/include/iframeindex");
		ModelAndView view = request(responseData, orgi, data);
		view.addObject("type", channel);
		view.addObject("province", province);
		return view;
	}

	@RequestMapping(value = "/adddo")
	public ModelAndView adddo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SinoLocation data) {
		String str=UUID.randomUUID().toString();
        String id=str.substring(0,8)+str.substring(9,13)+str.substring(14,18)+str.substring(19,23)+str.substring(24);
		//System.out.println("id================="+id);
		data.setId(id);
		data.setOrgi(orgi);
		data.setUpdatedate(new Date());
		data.setStatus("1");//新增
		super.getService().saveIObject(data);
		
		//发消息给GW，在GW更新百度信息
		APIContext.getRpcServer().sendMessageToServer(
				new Message(EapDataContext.HANDLER, JSON.toJSONString(new SystemMessage(SINOSIG_LOCATION,data), SerializerFeature.WriteClassName)));
		
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}
	@RequestMapping(value = "/edit/{id}")
	public ModelAndView edit(HttpServletRequest request, @PathVariable String orgi, @PathVariable String id,@ModelAttribute("data") SinoLocation data) {
		SinoLocation ptype=(SinoLocation) super.getService().getIObjectByPK(SinoLocation.class, id);
		ResponseData responseData = new ResponseData("/pages/manage/sinosig/location/edit", "/pages/include/iframeindex");
		responseData.setData(ptype);
		return request(responseData, orgi, null);
	}

	@RequestMapping(value = "/editdo")
	public ModelAndView editdo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SinoLocation data) {
		data.setOrgi(orgi);
		data.setStatus("2");//修改
		super.getService().updateIObject(data);
		//发消息给GW，在GW更新百度信息
		APIContext.getRpcServer().sendMessageToServer(
				new Message(EapDataContext.HANDLER, JSON.toJSONString(new SystemMessage(SINOSIG_LOCATION,data), SerializerFeature.WriteClassName)));
		ResponseData responseData = new ResponseData("/pages/public/success");
		return request(responseData, orgi, null);
	}
	@SuppressWarnings("unused")
	@RequestMapping(value = "/tablelist/{type}/{pro}")
	public ModelAndView list(HttpServletRequest request, @PathVariable String orgi, @PathVariable String type,@PathVariable String pro, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/sinosig/location/list");
		responseData.setRqdata(data);
		String search=request.getParameter("search");
		String key="%"+search+"%";
		if(search==null){
			if("0".equals(pro) || "".equals(pro)){
				//responseData.setValueList(super.getService().findAllByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("type", type))),null,Projections.distinct(Projections.property("province"))));
				responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("type", type))),data.getPs(),data.getP()));
			}else{
				responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("province", pro))).add(Restrictions.eq("type", type)),data.getPs(),data.getP()));
			}
		}else{
			if("0".equals(pro) || "".equals(pro)){
				//responseData.setValueList(super.getService().findAllByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("type", type))),null,Projections.distinct(Projections.property("province"))));
				responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("type", type))).add(Restrictions.or(Restrictions.or(Restrictions.like("province", key),Restrictions.like("city", key)), Restrictions.or(Restrictions.like("address", key), Restrictions.like("dept", key)))),data.getPs(),data.getP()));
			}else{
				responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("province", pro))).add(Restrictions.eq("type", type)).add(Restrictions.or(Restrictions.or(Restrictions.like("province", key),Restrictions.like("city", key)), Restrictions.or(Restrictions.like("address", key), Restrictions.like("dept", key)))),data.getPs(),data.getP()));
			}
		}
		/*responseData.setValueList(Arrays.asList(provincesmap.values()));
		responseData.setDataList(Arrays.asList(citysmap.values()));*/
		ModelAndView view=request(responseData , orgi , data);
		return  view;
	}
	@RequestMapping(value = "/tablelist/provs/{type}")
	public ModelAndView provincelist(HttpServletRequest request, @PathVariable String orgi,@PathVariable String type, @ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/sinosig/location/provincelist");
		responseData.setRqdata(data);
		List<SinoLocation> lists=super.getService().findAllByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.eq("orgi", orgi)).add(Restrictions.eq("type", type)));
		Map<String,SinoLocation> provincesmap=new HashMap<String, SinoLocation>();
		List<SinoLocation> list=new ArrayList<SinoLocation>();
		String indexpro=null;
		for (SinoLocation local : lists) {
			if(!provincesmap.containsKey(local.getProvince())){
				if(indexpro==null){
					indexpro=local.getProvince();
				}
				provincesmap.put(local.getProvince(), local);
			}
		}
		if(lists!=null && lists.size()>0){
			if(provincesmap != null){
				Iterator<SinoLocation> iterator=provincesmap.values().iterator();
				while(iterator.hasNext()){
					list.add(iterator.next());
				}
			}
		}
		responseData.setValueList(list);
		/*responseData.setValueList(Arrays.asList(provincesmap.values()));
		responseData.setDataList(Arrays.asList(citysmap.values()));*/
		ModelAndView view=request(responseData , orgi , data);
		return  view;
	}
	@RequestMapping(value = "/rm/{id}")
	public ModelAndView rm(HttpServletRequest request, @PathVariable String orgi,@PathVariable String id, @ModelAttribute("data") SinoLocation data) {
		data.setId(id);
		data.setStatus("3");//删除
		super.getService().deleteIObject(data);
		//发消息给GW，在GW更新百度信息
		APIContext.getRpcServer().sendMessageToServer(
				new Message(EapDataContext.HANDLER, JSON.toJSONString(new SystemMessage(SINOSIG_LOCATION,data), SerializerFeature.WriteClassName)));
		ResponseData responseData = new ResponseData("/pages/manage/sinosig/location/list");
		responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.and(Restrictions.eq("type", data.getType()), Restrictions.eq("province", data.getProvince()))))));
		return request(responseData, orgi, null);
	}
	@RequestMapping(value = "/search")
	public ModelAndView search(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") SinoLocation data) {
		String key = "%" + data.getProvince() + "%";
		ResponseData responseData = new ResponseData("/pages/manage/sinosig/location/list");
		String pro=request.getParameter("result");
		if(pro==null || "".equals(pro) || "0".equals(pro)){
			responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("type", data.getType()))).add(Restrictions.or(Restrictions.or(Restrictions.like("province", key),Restrictions.like("city", key)), Restrictions.or(Restrictions.like("address", key), Restrictions.like("dept", key))))));
		}else{
			responseData.setDataList(super.getService().findPageByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("province", pro)) ,Restrictions.eq("type", data.getType()))).add(Restrictions.or(Restrictions.or(Restrictions.like("province", key),Restrictions.like("city", key)), Restrictions.or(Restrictions.like("address", key), Restrictions.like("dept", key))))));
		}
		return request(responseData, orgi, null);
	}
}
