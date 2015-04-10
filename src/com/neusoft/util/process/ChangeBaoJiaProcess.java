package com.neusoft.util.process;

import java.util.Map;

import net.sf.json.xml.XMLSerializer;

import com.neusoft.core.channel.SNSUser;
import com.neusoft.web.model.IfaceInfo;

import freemarker.template.TemplateException;

public class ChangeBaoJiaProcess extends InnerProcess{

	@Override
	public ProcessResult getResponse(IfaceInfo info, SNSUser snsUser, String orgi, Map<String, Object> paraMap) throws TemplateException, Exception {
		// TODO Auto-generated method stub
		return super.getResponse(info, snsUser, orgi, paraMap);
	}

	@Override
	public ProcessResult getRequest(SNSUser snsUser, String orgi, Map<String, Object> paraMap) {
		return null;
	}
	/**
	 *
	 */
	@Override
	public ProcessResult getRequest(IfaceInfo info, SNSUser snsUser, String orgi, Map<String, Object> paraMap) {
		CheXianProcess cp=new CheXianProcess();
		ProcessResult result =cp.getRequest(info, snsUser, orgi, paraMap);
		System.out.println(result.getResultVal().get(info.getCode()));
		String tem=result.getResultVal().get(info.getCode()).toString();
		//tem=tem.substring(tem.indexOf("{\"Header\""),tem.indexOf(",\"Sign\""));
		paraMap.put(info.getCode(), tem);
		//走核保请求接口
		return new ProcessResult(0,paraMap,tem);
	}
}
