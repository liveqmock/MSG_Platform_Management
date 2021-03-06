package com.neusoft.web.handler.api;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neusoft.core.EapDataContext;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.model.SinosigUser;

@Controller
@SessionAttributes
@RequestMapping(value = "/{orgi}/api/user")
@SuppressWarnings("unchecked")
public class UserLogoutHandler extends Handler {
	private static final Logger log = Logger.getLogger(UserLogoutHandler.class);
	
	@RequestMapping(value = "/userlogout")
	public ModelAndView userlogout(HttpServletRequest request, @PathVariable String orgi) throws IOException {
		ModelAndView view = null;
		Document doc = null;  
        SAXReader reader = new SAXReader();  
        InputStream in = request.getInputStream();
        String message=null;
        String custid=null;
        String status="1"; //0标识成功；1失败
        try {
        	if(in!=null && "POST".equals(request.getMethod())){
        		doc = reader.read(in);  
            	log.info("\r\n请求的报文为："+doc.asXML());
    			Element root = doc.getRootElement();
    			//返回数据的类型可以为xml和json，默认为json
    			String dataType = root.element("ResponseType").getTextTrim().toLowerCase();
    			if(dataType!=null&&"xml".equals(dataType)){
    				view=new ModelAndView("/pages/manage/sinosig/api/user/sendmsg_xml");
    			}else{
    				view=new ModelAndView("/pages/manage/sinosig/api/user/sendmsg_json");
    			}
    			//获取到推送消息的内容
    			custid = root.element("AgentId").getTextTrim();
    			//获取到微信用户的标识，多个时以,分隔
    			String fromSystem = root.element("FromSystem").getTextTrim();
    			List<SinosigUser> users=EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(SinosigUser.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.and(Restrictions.eq("custid", custid), Restrictions.eq("userstatus", "1")))));
    			if(custid==null){
    				message="agentid为空";
    			}else if(fromSystem==null){
    				message="FromSystem为空";
    			}else if(users!=null && users.size()>0){
    				SinosigUser suser=users.get(0);
    				suser.setUpdatetime(new Date());
    				suser.setUserstatus("0");
    				EapDataContext.getService().updateIObject(suser);
    				message="用户注销成功";
    				status="0";
    			}else{
    				status="0";
    				message="agentid不存在";
    			}
        	}
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			message="请求报文错误";
			e.printStackTrace();
		} finally{
        	in.close();  
        }
        if(view==null){
			view=new ModelAndView("/pages/public/success");
		}
		view.addObject("message", message);
		view.addObject("agentid", custid);
		view.addObject("status", status);
		return view;
	}
	public String convertStreamToString(InputStream is) {   
		   BufferedReader reader = new BufferedReader(new InputStreamReader(is));   
		        StringBuilder sb = new StringBuilder();   
		        String line = null;   
		        try {   
		            while ((line = reader.readLine()) != null) {   
		                sb.append(line + "/n");   
		            }   
		        } catch (IOException e) {   
		            e.printStackTrace();   
		        } finally {   
		            try {   
		                is.close();   
		            } catch (IOException e) {   
		                e.printStackTrace();   

		            }   
		        }   
		        return sb.toString();   
	}
	@RequestMapping(value = "/test")
	public ModelAndView test(HttpServletRequest request, @PathVariable String orgi) throws IOException {
		BufferedReader in = new BufferedReader(new  InputStreamReader(request   
		         .getInputStream(), "UTF-8"));   
		  
		 // Read the request   
		 CharArrayWriter data = new CharArrayWriter();   
		 char[] buf = new char[8192];   
		 int ret;   
		 while ((ret = in.read(buf, 0, 8192)) != -1) {   
		     data.write(buf, 0, ret);   
		 }   
		System.out.println("请求的参数为："+data.toString());
		
        in.close();  
		ModelAndView view = new ModelAndView("/pages/manage/sinosig/api/user/sendmsg_json");
		JSONObject jo=JSON.parseObject(data.toString());
		if(jo!=null){
			String key1=(String)jo.get("name");
			if(key1!=null&&"test".equals(key1)){
				view.addObject("message", "成功了");
				view.addObject("status", "0");
			}
		}
		else{
			view.addObject("message", "失败了");
			view.addObject("status", "1");
		}
		return view;
	}
	public static void main(String[] args) {
		for (int i = 10; i < 100; i++) {
			String tem="insert into rivu_sinosig_user(id,orgi,userstatus,custid,apiusername,isbind,islogin) values('123"+i+"','sinosig','1','10"+i+"','api"+i+"',0,0);";
			System.out.println(tem);
		}
	}
}
