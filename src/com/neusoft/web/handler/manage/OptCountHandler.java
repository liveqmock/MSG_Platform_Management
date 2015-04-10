package com.neusoft.web.handler.manage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
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
import com.neusoft.web.model.OptCount;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/optcount")
public class OptCountHandler extends Handler {


	@RequestMapping(value = "/tablelist")
	public ModelAndView tablelist(HttpServletRequest request ,@PathVariable String orgi,@ModelAttribute("data") RequestData data) throws ParseException {
		OptCount optCount = new OptCount();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//初始化日期为null
		Date begindate,endDate=null;
		if(request.getParameter("begintime")!=null && !"".equals(request.getParameter("begintime"))){
		     begindate = format.parse(request.getParameter("begintime") + " 00:00:00 ");
		     optCount.setOptdateBegin(begindate);
		}
		if(request.getParameter("endtime")!=null && !"".equals(request.getParameter("endtime"))){
			 endDate = format.parse(request.getParameter("endtime") + " 23:59:59 ");
			 optCount.setOptdateEnd(endDate);
		}
		
		if(request.getParameter("apiusername")!=null && !"".equals(request.getParameter("apiusername").trim())){
			optCount.setApiusername(request.getParameter("apiusername"));
		}
		
		if(request.getParameter("opttype")!=null && !"".equals(request.getParameter("opttype").trim())){
			optCount.setOpttype(request.getParameter("opttype"));
		}
		
		if(request.getParameter("optname")!=null && !"".equals(request.getParameter("optname").trim())){
			optCount.setOptname(request.getParameter("optname"));
		}
		
		if(request.getParameter("busito")!=null && !"".equals(request.getParameter("busito").trim())&&request.getParameter("busitypes")!=null && !"".equals(request.getParameter("busitypes").trim())){
			String busito=request.getParameter("busito");
			String busitypes=request.getParameter("busitypes");
			String busitype=busito+busitypes;
			optCount.setBusitype(busitype);
			optCount.setBusito(busito);
			optCount.setBusitypes(busitypes);
			//System.out.println("111111111111111111111="+busitype);
		}
		
		optCount.setOrgi(orgi);
		ResponseData responseData=new ResponseData("/pages/manage/optcount/optcountList");
		responseData.setDataList(getoptCount(optCount, data.getP(), data.getPs()));
		ModelAndView view = request(responseData, orgi , data);
		view.addObject("condtion", optCount);
		view.addObject("begintime",request.getParameter("begintime"));
		view.addObject("endtime",request.getParameter("endtime"));
		return view ; 
	
	}
	
	//查询条件
	public List<?> getoptCount(OptCount optCount, int p, int ps) {
		DetachedCriteria dr=DetachedCriteria.forClass(OptCount.class);
		Criterion temp=Restrictions.eq("orgi",optCount.getOrgi());
		dr.add(Restrictions.eq("isvalid",new Integer(1)));
		Order order=Order.desc("optdate");
		SimpleExpression apiusername=Restrictions.like("apiusername", "%"+optCount.getApiusername()+"%");
		if(optCount.getApiusername()!=null&&!"".equals(optCount.getApiusername()))
		{
			temp=Restrictions.and(apiusername,temp);
		}
		SimpleExpression opttype=Restrictions.eq("opttype",optCount.getOpttype());
		if(optCount.getOpttype()!=null&&!"".equals(optCount.getOpttype()))
		{
			temp=Restrictions.and(opttype,temp);
		}
		SimpleExpression optname=Restrictions.like("optname", "%"+optCount.getOptname()+"%");
		if(optCount.getOptname()!=null&&!"".equals(optCount.getOptname()))
		{
			temp=Restrictions.and(optname,temp);
		}
		SimpleExpression busitype=Restrictions.eq("busitype",optCount.getBusitype());
		if(optCount.getBusitype()!=null&&!"".equals(optCount.getBusitype()))
		{
			temp=Restrictions.and(busitype,temp);
		}
		
		SimpleExpression begintime=Restrictions.ge("optdate", optCount.getOptdateBegin());
		SimpleExpression endtime=Restrictions.le("optdate", optCount.getOptdateEnd());
		Criterion bothtime=Restrictions.between("optdate", optCount.getOptdateBegin(), optCount.getOptdateEnd());
		
		if(optCount.getOptdateBegin()!=null&&optCount.getOptdateEnd()==null){
			temp=Restrictions.and(temp, begintime);
		}else if(optCount.getOptdateBegin()==null&&optCount.getOptdateEnd()!=null){
			temp=Restrictions.and(temp, endtime);
		}else if(optCount.getOptdateBegin()!=null&&optCount.getOptdateEnd()!=null){
			temp=Restrictions.and(temp, bothtime);
		}
		
		
		dr.add(temp);
		dr.addOrder(order);
		return EapDataContext.getService().findPageByCriteria(dr,ps,p);
	}
	

}
