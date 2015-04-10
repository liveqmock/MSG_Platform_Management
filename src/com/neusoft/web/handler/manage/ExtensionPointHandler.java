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
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;
import com.neusoft.web.model.ExtensionPoints;
import com.neusoft.web.model.Instruction;

import edu.emory.mathcs.backport.java.util.Arrays;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/extensionpoint")
@SuppressWarnings("unchecked")
public class ExtensionPointHandler extends Handler {

	@RequestMapping(value = "/tablelist/{etpid}")
	public ModelAndView tablelist(HttpServletRequest request, @PathVariable String orgi, @PathVariable String etpid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/extensionpoint/tablelist", super.getService().findPageByCriteria(
				DetachedCriteria.forClass(ExtensionPoints.class).add(Restrictions.eq("extensiontype", EapDataContext.PluginType.INSTRUCTION.toString())), data.getPs(), data.getP()), data);
		responseData.setResult(etpid);
		return request(responseData, orgi, data);
	}

	@RequestMapping(value = "/changetype/{etpid}")
	public ModelAndView changetype(HttpServletRequest request, @PathVariable String orgi, @PathVariable String etpid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/extensionpoint/tablelist", super.getService().findPageByCriteria(
				DetachedCriteria.forClass(ExtensionPoints.class).add(
						Restrictions.eq("extensiontype", etpid)), data.getPs(), data.getP()), data);
		responseData.setResult(etpid);
		return request(responseData, orgi, data);
	}

	@RequestMapping(value = "/add/{etpid}")
	public ModelAndView add(HttpServletRequest request, @PathVariable String orgi, @PathVariable String etpid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/extensionpoint/add", "/pages/include/iframeindex");
		responseData.setDataList(Arrays.asList(EapDataContext.PluginType.class.getEnumConstants()));
		ModelAndView view = request(responseData, orgi, data);
		view.addObject("etpid", etpid);
		return view;
	}

	@RequestMapping(value = "/adddo")
	public ModelAndView adddo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") ExtensionPoints data) {
		try {
			Class<?> c=Class.forName(data.getClazz());
			if(c!=null&& c instanceof Class){
				super.getService().saveIObject(data);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			return request(new ResponseData("redirect://tablelist/"+data.getExtensiontype()+".html" , "类接口不存在，请重新输入" , true , null), orgi, null) ;
		}
		ResponseData responseData = new ResponseData("/pages/public/success");
		// 更新到插件列表中
		EapDataContext.initPlugin();
//		RivuDataContext.getPluginList().get(RivuDataContext.PluginType.INSTRUCTION.toString()).add(data);
		return request(responseData, orgi, null);
	}

	@RequestMapping(value = "/rm/{pluginid}")
	public ModelAndView rm(HttpServletRequest request, @PathVariable String orgi, @PathVariable String pluginid,
			@ModelAttribute("data") ExtensionPoints data) {
		List<Instruction> plugins = super.getService().findAllByCriteria(DetachedCriteria.forClass(Instruction.class).add(Restrictions.eq("orgi", orgi)).add(Restrictions.eq("plugin", pluginid))) ;
		if(plugins!=null&&plugins.size()>0){
			ResponseData responseData=new ResponseData("redirect:/{orgi}/extensionpoint/changetype/instruction");
			responseData.setError("在IMR有引用，请先IMR中对该插件的引用") ;
			return request(responseData, orgi, null);
		}else{
			data.setId(pluginid);
			/*// 更新到插件列表中
			int pindex = -1;
			// 查找出要删除插件在列表中的索引
			List<ExtensionPoints> plist = RivuDataContext.getPluginList().get(RivuDataContext.PluginType.INSTRUCTION.toString());
			for (int i = 0; i < plist.size(); i++) {
				ExtensionPoints etp = plist.get(i);
				if (etp != null && etp.getId()!=null && etp.getId().equals(pluginid)) {
					pindex = i;
					break;
				}
			}
			if(pindex!=-1)
			RivuDataContext.getPluginList().get(RivuDataContext.PluginType.INSTRUCTION.toString()).remove(pindex);
			*/
			// 执行数据库删除操作
			super.getService().deleteIObject(data);
			EapDataContext.initPlugin();
		}
		return request(new ResponseData("redirect:/{orgi}/extensionpoint/changetype/instruction.html"), orgi, null);
	}

	@RequestMapping(value = "/edit/{pluginid}")
	public ModelAndView eidt(HttpServletRequest request, @PathVariable String orgi, @PathVariable String pluginid,
			@ModelAttribute("data") RequestData data) {
		ResponseData responseData = new ResponseData("/pages/manage/extensionpoint/edit", "/pages/include/iframeindex");
		responseData.setData(super.getService().getIObjectByPK(ExtensionPoints.class, pluginid));
		ModelAndView view = request(responseData, orgi, data);
		return view;
	}

	@RequestMapping(value = "/editdo")
	public ModelAndView editdo(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") ExtensionPoints data) {
		try {
			Class<?> c=Class.forName(data.getClazz());
			if(c!=null&& c instanceof Class){
				super.getService().updateIObject(data);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			return request(new ResponseData("redirect://tablelist/"+data.getExtensiontype()+".html" , "类接口不存在，请重新输入" , true , null), orgi, null) ;
		}
		/*// 更新到插件列表中
		int pindex = -1;
		// 查找出要删除插件在列表中的索引
		List<ExtensionPoints> plist = RivuDataContext.getPluginList().get(RivuDataContext.PluginType.INSTRUCTION.toString());
		for (int i = 0; i < plist.size(); i++) {
			ExtensionPoints etp = plist.get(i);
			if (etp != null && etp.getId().equals(data.getId())) {
				pindex = i;
				break;
			}
		}
		//
		RivuDataContext.getPluginList().get(RivuDataContext.PluginType.INSTRUCTION.toString()).remove(pindex);
		RivuDataContext
				.getPluginList()
				.get(RivuDataContext.PluginType.INSTRUCTION.toString())
				.add((ExtensionPoints) super.getService()
						.findAllByCriteria(DetachedCriteria.forClass(ExtensionPoints.class).add(Restrictions.eq("id", data.getId()))).get(0));
*/
		EapDataContext.initPlugin();
		ResponseData responseData = new ResponseData("/pages/public/success");
		//多余代码，数据由回调函数加载responseData.setDataList(super.getService().findAllByCriteria(DetachedCriteria.forClass(ExtensionPoints.class)));
		return request(responseData, orgi, null);
	}

	@RequestMapping(value = "/search")
	public ModelAndView search(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") ExtensionPoints data) {
		String key = "%" + data.getName() + "%";
		ResponseData responseData = new ResponseData("/pages/manage/extensionpoint/tablelist");
		responseData.setDataList(super.getService().findPageByCriteria(
				DetachedCriteria.forClass(ExtensionPoints.class).add(Restrictions.like("name", key)).add(Restrictions.eq("extensiontype", EapDataContext.PluginType.INSTRUCTION.toString()))));
		return request(responseData, orgi, null);
	}
}
