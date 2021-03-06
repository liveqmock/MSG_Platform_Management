package com.neusoft.util.process;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import com.alibaba.fastjson.JSON;
import com.neusoft.core.EapDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.core.channel.Channel;
import com.neusoft.core.channel.DataMessage;
import com.neusoft.core.channel.SNSUser;
import com.neusoft.core.channel.WeiXin;
import com.neusoft.util.persistence.PersistenceFactory;
import com.neusoft.web.model.IfaceInfo;
import com.neusoft.web.model.SinoLocation;

import freemarker.template.TemplateException;

public class LocationProcess extends InnerProcess{

	@Override
	public ProcessResult getResponse(IfaceInfo info, SNSUser snsUser, String orgi, Map<String, Object> paraMap) throws TemplateException, Exception {
		// TODO Auto-generated method stub
		return super.getResponse(info, snsUser, orgi, paraMap);
	}

	@Override
	public ProcessResult getRequest(SNSUser snsUser, String orgi, Map<String, Object> paraMap) {
		return null;
	}

	@Override
	public ProcessResult getRequest(IfaceInfo info, SNSUser snsUser, String orgi, Map<String, Object> paraMap) {
		ProcessResult result = null;
		Map<String , Object> valueMap = new HashMap<String,Object>() ;
		String str="";
		if(snsUser==null){
			//snsUser = PersistenceFactory.getInstance().getSnsUserInfo((String)paraMap.get("apiusername"), RivuDataContext.ChannelTypeEnum.WEIXIN.toString() , orgi) ;
			snsUser = PersistenceFactory.getInstance().getSnsUserInfo((String)paraMap.get("apiusername"), EapDataContext.ChannelTypeEnum.WEIXIN.toString() , orgi) ;
		}
		String dept=(String) paraMap.get("dept"); 
		if(dept!=null){
			List<SinoLocation> locals=EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(SinoLocation.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("dept", dept))));
			if(locals!=null && locals.size()>0){
				SinoLocation local=locals.get(0);
				if("3".equals(local.getStatus())){
					str="审核";
				}else{
					//保存数据
					local.setContract((String) paraMap.get("contract"));
					local.setMobile((String) paraMap.get("mobile"));
					local.setDept(dept);
					local.setAddress((String) paraMap.get("address"));
					local.setTel((String) paraMap.get("tel"));
					local.setHistel((String) paraMap.get("histel"));
					local.setFackid(snsUser.getApiusername());
					local.setStatus("1");
					local.setUpdatedate(new Date());
					EapDataContext.getService().updateIObject(local);
					str="成功";
					Channel channel=new WeiXin();
					channel.setTouser(snsUser.getUserid()) ;
					channel.setUserid(snsUser.getUserid()) ;
					channel.setSnsuser(snsUser) ;
					channel.setText("信息已收到，请发送自己所在机构营业场所的位置");
					channel.setReplytype(EapDataContext.ReplyType.MANUALLY.toString()) ;
					channel.setMessagetype(EapDataContext.MessageType.TEXT.toString()) ;
					channel.setOrgi(snsUser.getOrgi()) ;
					DataMessage dataMessage = new DataMessage(snsUser.getChannel() , channel , orgi , snsUser.getUserid()) ;
					APIContext.saveMessage(dataMessage) ;
					APIContext.sendMessageToUser(dataMessage) ;
				}
					
			}else{
				str ="失败";
			}
		}
		valueMap.put(info.getCode(), str);
		return new ProcessResult(0,valueMap,null);
	}
}
