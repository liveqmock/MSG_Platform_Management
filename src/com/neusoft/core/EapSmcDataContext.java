package com.neusoft.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.neusoft.core.plugin.InstructPluginInterface;
import com.neusoft.util.queue.AgentUser;
import com.neusoft.web.model.IfaceInfo;
import com.neusoft.web.model.Instruction;
import com.neusoft.web.model.Material;
import com.neusoft.web.model.SearchResultTemplet;
import com.neusoft.web.model.SearchSetting;
import com.neusoft.web.model.SinosigZLBC;
import com.neusoft.web.model.SystemConfig;
import com.neusoft.web.model.UserTemplet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EapSmcDataContext {
	private static HazelcastInstance hazelcastInstance=Hazelcast.newHazelcastInstance();
	private static Map<String, InstructPluginInterface> templateMap =getInstance().getMap("templateMap");/*new HashMap<String, InstructPluginInterface>();*/
	private static Map<String, List<SearchResultTemplet>> searchResultTempletTypeMap =new HashMap<String, List<SearchResultTemplet>>();
	private static Map<String, SearchSetting> searchSettingMap =getInstance().getMap("searchSetting")/*new HashMap<String, SearchSetting>()*/;
	private static Map<String,Map<String,IfaceInfo>> noChangeIfaceMap=new HashMap<String,Map<String,IfaceInfo>>(); //分租户缓存接口数据
	private static Map<String, SinosigZLBC> zlbcMap=getInstance().getMap("zlbcMap");/*new HashMap<String, SinosigZLBC>();*/
	
	public static HazelcastInstance getInstance(){
		return hazelcastInstance;
	}
	/**
	 * 根据apiusername获取，如果缓存没有，从数据库查询是否有：该客户且状态为正在上传的事件
	 * @param orgi
	 * @param apiusername
	 * @return
	 */
	public static SinosigZLBC getZlbcFromMap(String orgi,String apiusername){
		SinosigZLBC zlbc=zlbcMap.get(apiusername);
		if(zlbc==null){
			List<SinosigZLBC> zlbcs=EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(SinosigZLBC.class).add(Restrictions.and(Restrictions.eq("orgi", orgi), Restrictions.eq("apiusername", apiusername))).add(Restrictions.eq("status", 0)).addOrder(Order.desc("createtime")));
			if(zlbcs!=null && zlbcs.size()>0){
				zlbc=zlbcs.get(0);
				//添加到缓存
				zlbcMap.put(apiusername, zlbc);
			}
		}
		return zlbc;
	}
	public static Map<String, SinosigZLBC> getZlbcMap() {
		return zlbcMap;
	}

	public static void setZlbcMap(Map<String, SinosigZLBC> zlbcMap) {
		EapSmcDataContext.zlbcMap = zlbcMap;
	}

	public static Map<String, Map<String, IfaceInfo>> getNoChangeIfaceMap() {
		return noChangeIfaceMap;
	}

	private static Map<String, InstructPluginInterface> getTemplateMap() {
		return templateMap;
	}
	
	public static Map<String , SearchSetting> getSearchSetting(){
		return searchSettingMap ;
	}
	
	public static SearchSetting getSearchSetting(String orgi){
		return searchSettingMap.get(orgi)!=null ? searchSettingMap.get(orgi) : new SearchSetting() ;
	}
	public static void setTemplateMap(Map<String, InstructPluginInterface> templateMap) {
		EapSmcDataContext.templateMap = templateMap;
	}
	
	/**
	 * 
	 * @param channel
	 * @param orgi
	 * @return
	 */
	public static List<UserTemplet> getUserTempletList(String channel , String orgi){
		return EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(UserTemplet.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("channel", channel)))) ;
 	}
	
	/**
	 * 
	 * @param channel
	 * @param orgi
	 * @return
	 */
	public static UserTemplet getUserTemplet(String id , String orgi){
		return (UserTemplet) EapDataContext.getService().getIObjectByPK(UserTemplet.class, id) ;
 	}
	
	public static List<SearchResultTemplet> getSearchResultTempletList(String channel){
		return searchResultTempletTypeMap.get(channel) ;
 	}
	/**
	 * 获取素材
	 * @param orgi
	 * @param id
	 * @return
	 */
	public static Material getMaterial(String orgi , String id){
		return (Material) EapDataContext.getService().getIObjectByPK(Material.class, id) ;
	}
	
	public static UserTemplet getUserTempletByChannel(String channel ,String orgi,String code){
		List<UserTemplet> userTempletList = EapDataContext.getService().findPageByCriteria(DetachedCriteria.forClass(UserTemplet.class).add(Restrictions.and(Restrictions.eq("orgi", orgi) ,Restrictions.eq("channel", channel)))) ;
		UserTemplet userTemplet = null ;
		for(UserTemplet tp : userTempletList){
			if(tp.getCode().equals(code) || tp.getName().equals(code)){
				userTemplet = tp ;
				break ;
			}
		}
		return  userTemplet;
 	}

	private static Map<String, SystemConfig> systemconfigMap = new HashMap<String, SystemConfig>();

	public static Map<String, SystemConfig> getSystemconfigMap() {
		return systemconfigMap;
	}

	public static void initData() {
		// 初始化模版列表,已经放入到setting中了，注释2013年9月15日15:20:33
		/*templateMap.put(TemplateCodeEnum.FOURMINSTIP.toString(), new FourMinsTipPlugin());
		templateMap.put(TemplateCodeEnum.TIPNOTFOUNDINSTRUCT.toString(), new TipNotFoundInstructPlugin());
		templateMap.put(TemplateCodeEnum.TWOMINSTIP.toString(), new TwoMinsTipPlugin());*/
		
		// 初始化系统配置
		List<SearchResultTemplet>  searchResultTempletList = EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(SearchResultTemplet.class));
		List<SearchSetting>  searchSettingList = EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(SearchSetting.class));
		for(SearchResultTemplet tp: searchResultTempletList ){
			List<SearchResultTemplet> hzsrt=searchResultTempletTypeMap.get(tp.getChannel());
			if(hzsrt==null){
				hzsrt=new ArrayList<SearchResultTemplet>() ;
			}
			hzsrt.add(tp) ;
			searchResultTempletTypeMap.put(tp.getChannel(),hzsrt) ;
		}
		for(SearchSetting setting : searchSettingList){
			searchSettingMap.put(setting.getOrgi(), setting) ;
		}
		//把setting添加到Hazelcat中去 ;Authurl=smc1地址;Regurl=smc2地址;
//		initHazelcastClient(searchSettingList);
	}
	
	/**
	 * 系统缓存静态接口
	 */
	public static void initNoChangeIfaces(){
		List<IfaceInfo> ifaceList = EapDataContext.getService().findAllByIObjectCType(IfaceInfo.class) ;
		for(IfaceInfo iface : ifaceList){
			if(iface.getCode()==null || iface.getName()==null){
				continue ;
			}
			if(EapSmcDataContext.noChangeIfaceMap.get(iface.getOrgi())==null){
				EapSmcDataContext.noChangeIfaceMap.put(iface.getOrgi(), new HashMap()) ;
			}
			if(EapSmcDataContext.noChangeIfaceMap.get(iface.getOrgi())!=null && EapSmcDataContext.noChangeIfaceMap.get(iface.getOrgi()).get(iface.getCode().toLowerCase())!=null && !iface.isIschange()){
				EapSmcDataContext.noChangeIfaceMap.get(iface.getOrgi()).put(iface.getCode().toLowerCase(),iface) ;
			}
		}
	}

	/*
	 * static{ pluginList.put(PluginType.INSTRUCTION.toString(), new
	 * ArrayList<ExtensionPoints>()) ; ExtensionPoints point = new
	 * ExtensionPoints() ;
	 * point.setClazz("com.rivues.core.plugin.TipNavMenuPlugin") ;
	 * point.setName("系统菜单提示") ; point.setId("165") ;
	 * pluginList.get(PluginType.INSTRUCTION.toString()).add(point) ;
	 * ExtensionPoints point2 = new ExtensionPoints() ;
	 * point2.setClazz("com.rivues.plugin.Plugin2Test") ; point2.setName("切换接入")
	 * ; point2.setId("154") ;
	 * pluginList.get(PluginType.INSTRUCTION.toString()).add(point2) ;
	 * 
	 * ExtensionPoints point3 = new ExtensionPoints() ;
	 * point3.setClazz("com.rivues.core.plugin.TransferAgentInstructPlugin") ;
	 * point3.setName("转人工坐席") ; point3.setId("1254") ;
	 * pluginList.get(PluginType.INSTRUCTION.toString()).add(point3) ;
	 * 
	 * }
	 */
	/**
	 * 系统模版code枚举类
	 * 
	 * @author lzg
	 * 
	 */
	public enum TemplateCodeEnum {
		FOURMINSTIP, TIPNOTFOUNDINSTRUCT, TWOMINSTIP , NEWS , TEXT;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}
	public enum EventMenuTypeEnum {
		MENU, MESSAGE, ROBOT;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}
	/**
	 * 接口的调用类型：HTTP、SAOP、Webservice
	 * @author kerwin
	 *
	 */
	public enum IfaceRPCTypeEnum {
		HTTP, SAOP, WebService;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}
	public static String createPluginMessage(AgentUser user , Instruction ins , String orgi){
		return ins.getMemo();
	}
	/***
	 * 
	 * @param requestDateMessage 请求报文
	 * @param requestURLAddress 请求地址
	 * @param requestCharset 请求编码“GBK”、“UTF-8”等等
	 * @param responseCharset 响应编码“GBK”、“UTF-8”等等
	 * @return 请求结果数据报文
	 * @throws IOException
	 */
	public static String httpPostBodyRequestProcesser(
			String requestDateMessage, String requestURLAddress,
			String requestCharset, String responseCharset,String method) throws IOException {

		URL url = null;

		HttpURLConnection httpURLConnection = null;

		String responseDateMessage = null;
		BufferedWriter out =null;
		BufferedReader in =null;

		try {
			url = new URL(requestURLAddress);
			httpURLConnection = (HttpURLConnection) url.openConnection();

			httpURLConnection.setDoOutput(true);
			httpURLConnection.setDoInput(true);
			httpURLConnection.setRequestMethod(method);
			httpURLConnection.setRequestProperty("Content-Type","text/xml;charset=" + requestCharset);
			httpURLConnection.setRequestProperty("SOAPAction","http://WebXml.com.cn/getWeatherbyCityName");
			httpURLConnection.setRequestProperty("User-Agent","Jakarta Commons-HttpClient/3.1");
			httpURLConnection.setConnectTimeout(50000);
			httpURLConnection.setReadTimeout(50000);

			httpURLConnection.connect();

			out = new BufferedWriter(new OutputStreamWriter(httpURLConnection.getOutputStream(), requestCharset));

			out.write(requestDateMessage);
			out.flush();
			out.close();
			
			in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), responseCharset));
			String line = null;
			StringBuilder sb = new StringBuilder();

			while ((line = in.readLine()) != null) 
			{
				sb.append(line);
			}

			responseDateMessage = sb.toString();

		} 
		catch (IOException e) 
		{
			throw new IOException();
		} 
		finally 
		{
			if(httpURLConnection!=null){
				httpURLConnection.disconnect();
			}
			if(in!=null){
				in.close();
			}
		}
		
		return responseDateMessage;
	}
	public static String findDateFromXml(String xml,String target){
		String result=null;
		if(xml!=null && target!=null && xml.indexOf(target)>=0){
			int start_index=xml.indexOf("<"+target+">")+target.length()+2;
			int end_index=xml.indexOf("</"+target+">");
			if(start_index>0 && end_index>0){
				result=xml.substring(start_index,end_index);
			}
		}
		return result;
	}
	public static String sendZip(String zipFilePath, String urlString) throws Exception {
		URL url;
		HttpURLConnection uc = null;
		String msg=null;
		File zipFile = new File(zipFilePath);

		if (zipFile.exists()) {

			int size = (int) zipFile.length();

			url = new URL(urlString);
			uc = (HttpURLConnection) url.openConnection();
			uc.setDoOutput(true);
			uc.setDoInput(true);
			uc.setUseCaches(false);
			uc.setRequestMethod("POST");
			uc.setFixedLengthStreamingMode(size);
			uc.setRequestProperty("Content-type", "multipart/form-data");
			uc.setRequestProperty("Connection", "Keep-Alive");
			uc.setRequestProperty("Cache-Control", "no-cache");
			uc.setConnectTimeout(30000);

			FileInputStream fis = new FileInputStream(zipFile);
			OutputStream out = new DataOutputStream(uc.getOutputStream());
			uc.connect();
			int bytes = 0;
			byte[] bufferOut = new byte[1024];
			while ((bytes = fis.read(bufferOut)) != -1) {
				out.write(bufferOut, 0, bytes);
			}
			out.flush();
			out.close();
			fis.close();

			// 读取响应
			int code = uc.getResponseCode();
			if (code == 200) {
				BufferedInputStream bis = new BufferedInputStream(
						uc.getInputStream());

				try {
					Document response = new SAXReader().read(bis);
					if (response != null) {
						msg=response.asXML();
						System.out.println(response.asXML());
					}
				} catch (DocumentException e) {
					e.printStackTrace();
				} finally {
					if (bis != null) {
						bis.close();
						bis = null;
					}
				}
			}
			System.out.println("=========发送zip完成===========");
		}
		if(uc!=null){
			uc.disconnect();
		}
			
		return msg;
	}
	public static void main(String[] args) {
		try {
//			System.out.println(httpPostBodyRequestProcesser("{\"action_name\": \"QR_LIMIT_SCENE\", \"action_info\": {\"scene\": {\"scene_id\": 123}}}", "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=8MrP3EHQuHAPRnMCaXG0p7_jPEBRUOGxzNw82-TEO2bt6_5wBTQbW6t9GZ8z2kQv3sRf2oK2WFJGRMHWBpM9bU0ENdubs1ASKEGAnCeatGJIL8YmF62C5cEGlJ9-nLxd0rvDeiwLs9_siVnCD5ItzA", "UTF-8", "UTF-8", "POST"));
			System.out.println(httpPostBodyRequestProcesser("{\"action_name\": \"QR_LIMIT_SCENE\", \"action_info\": {\"scene\": {\"scene_id\": 1001}}}", "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=5ic8RoqZd7IBUtE0aGXUhefsptRIRL_BVv_z0OWh5Im1YkJFUGlVa5AnsPUiEM4tF57R0aAkLLyllMR_iVx43_PH-0JYLRivX-7aVsq0hFti-3QTBrht3EMqsjgB5O5R5_3kScN8zFle7ESLrMcF5w", "UTF-8", "UTF-8", "POST"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
