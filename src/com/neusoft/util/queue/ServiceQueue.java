package com.neusoft.util.queue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import com.neusoft.core.EapDataContext;
import com.neusoft.core.EapSmcDataContext;
import com.neusoft.core.api.APIContext;
import com.neusoft.core.channel.DataMessage;
import com.neusoft.core.channel.SNSUser;
import com.neusoft.core.channel.WeiXin;
import com.neusoft.core.plugin.InstructPluginInterface;
import com.neusoft.core.plugin.SinosigZlpcSaveImagePlugin;
import com.neusoft.core.plugin.SinosigZlpcValidCardPlugin;
import com.neusoft.core.plugin.TipCurrentInstructPlugin;
import com.neusoft.core.plugin.TipNotFoundInstructPlugin;
import com.neusoft.core.plugin.TipSubInstructPlugin;
import com.neusoft.core.plugin.TipUserBindMessagePlugin;
import com.neusoft.core.plugin.TransferAgentSkillInstructPlugin;
import com.neusoft.util.comet.demo.talker.Constant;
import com.neusoft.util.persistence.PersistenceFactory;
import com.neusoft.web.model.DataDic;
import com.neusoft.web.model.ExtensionPoints;
import com.neusoft.web.model.Instruction;
import com.neusoft.web.model.SearchSetting;
import com.neusoft.web.model.SinosigZLBC;
import com.neusoft.web.model.User;

/**
 * 服务排队处理
 * @author admin
 *
 */
public class ServiceQueue {
	private static Map<String, AgentStatus> agentQueue = EapSmcDataContext.getInstance().getMap("agentQueue");	//已登陆，当前状态为  在线的坐席
	private static List<AgentStatus> agentUserQueue =  EapSmcDataContext.getInstance().getList("linkedagent");//new HashMap<String , LinkedList<AgentStatus>>(); 			//已登陆，当前状态为  在线的坐席，排队使用FIFO策略，出队列发生在坐席状态改变的时候，例如，用户服务结束
	private static Queue<AgentUser> queueUserList =  EapSmcDataContext.getInstance().getQueue("aueueuser");//new HashMap<String , Queue<AgentUser>>(); 				//排队用户，排队使用FIFO策略，出队列发生在坐席状态改变的时候，例如，用户服务结束
	private static Map<String , Queue<AgentUser>> vipQueueUserList = /*SmcRivuDataContext.getInstance().getMap("vipuser");*/new HashMap<String , Queue<AgentUser>>(); 				//VIP排队用户，排队使用FIFO策略，出队列发生在坐席状态改变的时候，例如，用户服务结束
	private static Map<String, AgentUser> userQueue = EapSmcDataContext.getInstance().getMap("userQueue");			//正在服务中的用户
	private static Map<String, AgentUser> queueUserMap = EapSmcDataContext.getInstance().getMap("queueUserMap");	//排队中的用户
	private static String DIS_LOCK = "dis_lock" ;
	public final static int maxUserNum = 5 ;			//坐席排队用户数
	public final static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	private static final Logger log = Logger.getLogger(ServiceQueue.class);
	public static Map<String, AgentUser> getUserQueue() {
		return userQueue;
	}
	
	public static Map<String, AgentStatus> getAgentQueue() {
		return agentQueue;
	}
	/**
	 * 处理用户长时间未联系和 自动断开
	 * @param agentUser
	 * @param setting
	 * @param orgi
	 * @param userid
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static synchronized AgentUser processUserList(AgentUser agentUser , SearchSetting setting , String orgi , String userid) throws InstantiationException, IllegalAccessException{
		if(agentUser!=null && !agentUser.isFromhis()){
			//从map获取时间参数
			long times = System.currentTimeMillis() - agentUser.getLastmessage().getTime() ; 
			if(setting.isContractip() && times>1000*60*setting.getContractipimin() && times<1000 * 60 * setting.getDislinkmin() && !agentUser.isTip() && agentUser.isAgent()){
				agentUser.setTip(true) ; //注释了，可以连续提示
				agentUser.setAgent(true) ; 
				DataMessage dm=createChannelMessage(agentUser.getOrgi(), agentUser.getChannel() , agentUser.getUserid() ,  setting.getContractipimsg() , null , agentUser.getSnsuser(),agentUser.getContextid());
				APIContext.sendMessageToUser(dm);
				agentUser.getLastmessage().setTime(new Date().getTime());
				dm.setType(Constant.TALK);
				APIContext.sendToAgent(Constant.APP_CHANNEL, agentUser.getAgentno() , dm , orgi);
				userQueue.put(userid, agentUser);
			}else if(times>=1000 * 60 * setting.getDislinkmin() && setting.isDislinktip() && agentUser.isAgent()){
				agentUser.setDisconnect(true);
				DataMessage dm=createChannelMessage(agentUser.getOrgi(), agentUser.getChannel() , agentUser.getUserid() , setting.getDislinkmsg()!=null ? setting.getDislinkmsg() : "" , null , agentUser.getSnsuser(),agentUser.getContextid());
				APIContext.sendMessageToUser(dm);

				/**
				 * 结束服务，从服务队列中移除 
				 */
				agentUser.setEndtime(new Date()) ;
				agentUser.setSessiontimes(agentUser.getEndtime().getTime() - agentUser.getLogindate().getTime()) ;
				EapDataContext.getService().updateIObject(agentUser) ;
				dm.setType(Constant.DOWN);
				APIContext.sendToAgent(Constant.APP_CHANNEL, agentUser.getAgentno() , dm , orgi);
				/**
				 * 先从 agentstatus 的 userlist 队列里删除
				 */
				userQueue.remove(userid) ;
				APIContext.userDisLink(agentUser, orgi , new WeiXin()) ;
			}
		}
		return agentUser ;
	}

	static{
		Thread idleUserClear = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try{
						Thread.sleep(3000) ;
						/**
						 * 统计信息
						 */ 
						{
							Iterator<String> userIterator = userQueue.keySet().iterator() ;
							while(userIterator.hasNext()){
								String userid = userIterator.next() ;
								AgentUser agentUser = userQueue.get(userid) ;
								if(agentUser!=null && !agentUser.isFromhis()){
									//从map获取时间参数
									SearchSetting setting = EapSmcDataContext.getSearchSetting(agentUser.getOrgi());
									long times = System.currentTimeMillis() - agentUser.getLastmessage().getTime() ; 
									if(setting.isContractip() && times>1000*60*setting.getContractipimin() && times<1000 * 60 * setting.getDislinkmin() && !agentUser.isTip() && agentUser.isAgent()){
										processUserList(agentUser, setting, agentUser.getOrgi(), userid) ;
									}else if(times>=1000 * 60 * setting.getDislinkmin() && setting.isDislinktip() && !agentUser.isDisconnect() && agentUser.isAgent()){
										processUserList(agentUser, setting, agentUser.getOrgi(), userid) ;
									}
								}
							}
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		});
		idleUserClear.start();
		/**
		 * 每分钟统计一次
		 */
		Thread staticThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try {
						staticAgent();
						Thread.sleep(1000*10) ;
						/**
						 * 每分钟统计一次
						 */
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		staticThread.start() ;
		
		/**
		 * 每分钟统计一次
		 */
		Thread cacheCheckThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try {
						Thread.sleep(1000*60) ;
						/**
						 * 缓存检查 ， 如果  agentQueue 和 agentUserQueue的数量对不上，就 复制
						 */
						if(agentUserQueue.size() != agentQueue.size()){
							agentUserQueue.clear();
							agentUserQueue.addAll(agentQueue.values());
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		cacheCheckThread.start() ;
	}
	/**
	 * 排队信息统计
	 */
	private static void staticAgent(){
		int agentnum = 0 , inservice = 0 , lineup = 0 , viplineup = 0 , usernum ;
		if(agentQueue!=null){
			agentnum = agentnum + agentQueue.size() ;
		}
		if(userQueue!=null){
			inservice = inservice + userQueue.size() ;
		}
		if(queueUserList!=null){
			lineup = lineup + queueUserList.size() ;
		}
		viplineup = viplineup + vipQueueUserList.size() ;
		// 客户总数：排队用户+排队vip+服务中用户
		usernum = viplineup + lineup + inservice ;
		//登陆坐席数
		EapDataContext.getRuntimeData().getReport().setAgentnum(agentnum) ;
		EapDataContext.getRuntimeData().getReport().setInserviceuser(inservice) ;
		EapDataContext.getRuntimeData().getReport().setLineupuser(lineup) ;
		EapDataContext.getRuntimeData().getReport().setViplineupuser(viplineup) ;
		EapDataContext.getRuntimeData().getReport().setUsernum(usernum) ;
		EapDataContext.getRuntimeData().setRpcServer(APIContext.getRpcServers()) ;
	}
	/**
	 * 坐席登陆
	 * @param orgi
	 * @param user
	 * @param status
	 * @return
	 * @throws Exception
	 */
	public static AgentStatus login(String orgi ,User user) throws Exception{
		AgentStatus status = new AgentStatus(user.getId() , user.getAgentno() , user.getAgentSkill(), new Date() , AgentStatus.AgentStatusEnum.READY.toString() , user.getOrgi());
		status.setUser(user);
		/**
		 * 以下非线程安全，需要在系统初始化的时候 初始载入所有租户
		 */
		if(agentQueue!=null && agentQueue.get(status.getAgentno())!=null){
			if(user.getType()!=null && user.getType().equals("1")){
				(status = agentQueue.get(status.getAgentno())).setStatus(AgentStatus.AgentStatusEnum.READY.toString()) ;
				user.setAgentstatus(status) ;
			}else{
				(status = agentQueue.get(status.getAgentno())).setStatus(AgentStatus.AgentStatusEnum.READY.toString()) ;
				user.setAgentstatus(status) ;
				
//				throw new IOException("该账号已登陆，请使用其他账号");
			}
		}
		agentUserQueue.add(status) ;
		agentQueue.put(status.getAgentno(), status);
		user.setAgentstatus(status) ;
		/**
		 * 设置坐席服务ID
		 */
		if(status.getAgentserviceid()==null || status.getAgentserviceid().length()==0){
			status.setAgentserviceid(String.valueOf(System.currentTimeMillis())) ;
		}
		return agentQueue.get(status.getAgentno()) ;
	}
	/**
	 * 坐席退出登陆
	 * @param orgi
	 * @param agentno
	 * @throws Exception
	 */
	public static void logout(String orgi ,String agentno) throws Exception{
		if(agentQueue.get(agentno)!=null){
			synchronized (agentQueue) {
				if(agentQueue.get(agentno)!=null ){
					if(agentQueue.get(agentno).getUserList().size()>0){
						/**
						 * 退出登陆前需要检查坐席的服务用户列表是否为空，如果不为空，则将未结束的用户转移到其他坐席
						 */
						AgentStatus agentStatus = agentQueue.remove(agentno) ;
						for(AgentUser agentUser : agentStatus.getUserList()){
							userQueue.remove(agentUser.getUserid()) ;
							/**
							 * 加入到排队队列
							 */
							queueUserList.add(agentUser);

							userServiceRequest(orgi , agentUser.getSnsuser(),false , agentUser.getUnreplaymessage().size()>0 ? agentUser.getUnreplaymessage().get(0) : null , maxUserNum , 0) ;
						}
					}
					removeAgentUserQueue(orgi, agentno);
				}
			}
		}else{
			throw new Exception("登出错误，坐席未登陆") ;
		}
	}
	/**
	 * 
	 * @param seatno
	 * @param status
	 */
	public static User statusChange(String orgi ,String agentno , String status , User user) throws Exception{
		//获取系统配置信息
		SearchSetting setting=EapSmcDataContext.getSearchSetting(orgi);
		if(user!=null && user.getAgentstatus()!=null){
			user.getAgentstatus().setStatus(status);
		}
		AgentStatus chageAs=null;
		if(agentQueue.containsKey(agentno)){
			if(status!=null){
				chageAs=agentQueue.get(agentno);
				chageAs.setStatus(status) ;
				agentQueue.put(user.getAgentno(), chageAs);
				
				//先移除，后添加，以便能更新状态
				removeAgentUserQueue(orgi, agentno);
				
				agentUserQueue.add(user.getAgentstatus()) ;
			}
			//TODO:如果多个坐席在线，则一起接入坐席,暂时没有考虑到VIP
			/**
			 * 增加分布式锁，在集群环境下保证 坐席分配的可靠
			 */
			if(agentQueue.get(agentno)!=null && agentQueue.get(agentno).getUserList()!=null && agentQueue.get(agentno).getUserList().size()< maxUserNum){
				processAllotUser(agentno, setting, orgi, user);
			}
			
			user.setAgentstatus(agentQueue.get(agentno));
		}else{
			throw new Exception("登出错误，坐席未登陆") ;
		}
		return user;
	}
	
	/**
	 * 
	 * @param seatno
	 * @param status
	 */
	public static User completion(String orgi ,String agentno  , User user , String userid) throws Exception{
		//获取系统配置信息
		SearchSetting setting=EapSmcDataContext.getSearchSetting(orgi);
		
		AgentUser agu = userQueue.get(userid) ;
		if(userQueue.get(userid)!=null){
			userQueue.remove(userid) ;
		}
		
		if(user.getAgentstatus()!=null && agu!=null){
			
			//移除前，更新记录,保存通话的结束时间
			agu.setEndtime(new Date());
			agu.setSessiontimes(agu.getEndtime().getTime() -  agu.getLogindate().getTime()) ;
			EapDataContext.getService().updateIObject(agu) ;
			//刷新在线绿人
			DataMessage dataMessage = new DataMessage(agu.getUserid(),Constant.NEWUSER,  new WeiXin() , user.getOrgi(), agu.getUserid()) ;
			APIContext.sendToAgent(Constant.APP_CHANNEL, user.getAgentno() , dataMessage , orgi) ;
			AgentStatus agentStatus = agentQueue.get(agentno) ;
			agentStatus.setLastMessage(agentQueue.get(agentno).getLastMessage()) ;
			agentQueue.put(agentno , agentStatus) ;
			processAllotUser(agentno, setting, orgi, user);
		}
		
		return user;
	}
	/**
	 * 
	 * @param agentStatus
	 * @param setting
	 * @param orgi
	 * @param user
	 */
	private static void processAllotUser(String agentno , SearchSetting setting , String orgi , User user){
		AgentStatus agentStatus = agentQueue.get(agentno) ;
		
		if(AgentStatus.AgentStatusEnum.SERVICES.toString().equals(agentStatus.getStatus()) && agentStatus.getUserList().size()<maxUserNum){
			AgentUser agentUser = null ;
			/**
			 * 坐席状态切换，登入
			 */
			boolean belongagent=false;
			if(vipQueueUserList.size()>0){
				//暂无VIP，如果添加则与else相同asumap.get(agentno).getUserList().add(agentUser = vipQueueUserList.get(orgi).poll()) ;
			}else if(queueUserList.size()>0){
				//去除排队中的用户，判断是否与切换状态坐席的技能组相同；相同，则接入坐席；并修改最后的通信时间为当前时间
				agentUser = queueUserList.peek();
				agentUser.setLastmessage(new Date());
				if((setting!=null&&!setting.isSkill())||(setting!=null&&setting.isSkill()&&user.getAgentSkill()!=null&&user.getAgentSkill().getCode().equals(agentUser.getAgentskill()))){
					queueUserList.poll();
					//注释，统一在allotAgent更新坐席的服务客户数asumap.get(agentno).getUserList().add(agentUser) ;
					belongagent=true;
					
					queueUserMap.remove(agentUser.getUserid()) ;
				}
				
			}
			/**
			 * 坐席状态切换的时候，如果有在排队的用户，将排队用户分配给坐席
			 */
			if(agentUser!=null&&belongagent){
				
				boolean res = allotAgentUser(agentno, orgi, agentUser, agentUser.getSnsuser())  ;
				/**
				 * 
				 */
				if(res){
					AgentQueueMessage agentQueueMessage = null ;
					DataMessage dataMessage = null ;
					if(agentUser!=null && agentUser.getUnreplaymessage()!=null){
						dataMessage = agentUser.getUnreplaymessage().size()>0?agentUser.getUnreplaymessage().get(0):null;
						/*if(dataMessage!=null){
							dataMessage.setUserid(agentUser.getUserid());
						}*/
						APIContext.retMessageToUser(agentQueueMessage = retMessageToUser(agentUser , agentStatus) , dataMessage);
					}
					/**
					 * 如果坐席不为空，将消息转发给坐席 ， 否则向用户返回一条消息，提示进入排队
					 */
					if(agentQueueMessage!=null && agentQueueMessage.getAgentUser()!=null){
	//					dataMessage.setUserid(agentQueueMessage.getAgentUser().getUserid());
						dataMessage.getChannel().setText("请求接入客服...");
						dataMessage.setType(Constant.TALK) ;
						APIContext.sendMessageToAgent(agentQueueMessage, dataMessage) ;
						AgentStatus ags = agentQueue.get(agentStatus.getAgentno()) ;
						ags.setLastMessage(agentQueue.get(agentStatus.getAgentno()).getLastMessage());
						
						agentQueue.put(ags.getAgentno(), ags);
						
					}
				}else{
					queueUserList.add(agentUser) ;
					queueUserMap.put(agentUser.getUserid(), agentUser) ;
				}
			}
		}
		
	}
	/**
	 * 将用户加入到排队队列
	 * @param serviceagent
	 * @param orgi
	 * @param dataMessage
	 * @param snsuser
	 * @param vip
	 * @param readyagent
	 * @return
	 */
	private static AgentQueueMessage pushUserQuene(AgentStatus serviceagent, String orgi , DataMessage dataMessage , AgentUser user  , boolean vip , AgentStatus readyagent ){
		AgentQueueMessage agentQueueMessage = null;
		if(dataMessage.getChannel().getText()!=null && dataMessage.getChannel().getText().length()<32){
			user.setAgentskill(dataMessage.getChannel().getText());
		}
		if(queueUserMap.get(user.getUserid())==null){
			/**
			 * 如果当前无登陆坐席，则将消息作为留言保存下来，不做任何处理
			 */
			if(agentQueue.size()==0 || readyagent==null){
				agentQueueMessage = new AgentQueueMessage(user , serviceagent ,EapDataContext.AgentQueueMessageType.NOAGENT.toString() , "您好，目前客服不在线，客服服务时间是8:30-20:30。您目前可直接使用菜单各功能，或者拨打阳光保险客服热线95510与我们联系，谢谢！");
			}else{
				/**
				 * 消息加入到未回复队列
				 */
				if(dataMessage!=null){
					user.getUnreplaymessage().add(0, dataMessage) ;
				}
				if(vip){
					if(vipQueueUserList.get(orgi)==null){
						vipQueueUserList.put(orgi, new LinkedList<AgentUser>()) ;
					}
					vipQueueUserList.get(orgi).add(user) ;
				}else{
					queueUserList.add(user) ;
				}
				agentQueueMessage = new AgentQueueMessage(user , serviceagent ,EapDataContext.AgentQueueMessageType.LINEUP.toString() , "您好，目前所有客服都在努力服务中。排在您前面的客户共有 "+ServiceQueue.getQueueNum(orgi)+"位，请稍微等待，客服将尽快为您服务！");
				queueUserMap.put(user.getUserid(), user) ;
			}
			
			/**
			 * 将用户放入到 等待队列中，方便查找
			 */
		}else{
			System.out.println("等待中userid==" + user.getUserid());
			user =  queueUserMap.get(user.getUserid()) ;
			if(dataMessage!=null){
				while(user.getUnreplaymessage().size()>5){
					user.getUnreplaymessage().remove(5);
				}
				user.getUnreplaymessage().add(0, dataMessage) ;
			}
		}
		return agentQueueMessage; 
	}
	/**
	 * 
	 * @param serviceagent
	 * @param orgi
	 * @param agentuser
	 * @param snsuser
	 * @return
	 */
	private synchronized static boolean allotAgentUser(String agentno , String orgi , AgentUser agentuser , SNSUser snsuser){
		boolean res = false ;
		Lock disLock = EapSmcDataContext.getInstance().getLock(DIS_LOCK) ;
		try{
			disLock.lock() ;
			if(AgentStatus.AgentStatusEnum.SERVICES.toString().equals(agentQueue.get(agentno).getStatus())){
				synchronized (agentQueue) {
					if(agentQueue.get(agentno).getUserList().size() < maxUserNum){
						allotAgent(agentuser , orgi , agentQueue.get(agentno)  , snsuser , agentuser.getContextid());
						res = true ;
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			disLock.unlock();
		}
		return res ;
	}
	/**
	 * 返回坐席消息
	 * @param agentuser
	 * @param serviceagent
	 * @param dataMessage
	 * @return
	 */
	private static AgentQueueMessage retMessageToUser(AgentUser agentuser , AgentStatus serviceagent){
		String tipmsg="您好，现在是"+serviceagent.getAgentno()+"为您服务，请问有什么可以帮您？";
		//分配坐席的消息，显示到坐席的界面
		tipAllotoAgentMsg(agentuser.getOrgi(), agentuser, tipmsg);
		return new AgentQueueMessage(agentuser , serviceagent ,EapDataContext.AgentQueueMessageType.ALLOTAGENT.toString() , tipmsg);
	}
	public static int getQueueNum(String orgi){
		return vipQueueUserList.size()+ queueUserList.size();
	}
	/**
	 * 获取用户排队信息
	 */
	static int abd=1;
	public static AgentQueueMessage userServiceRequest(String orgi , SNSUser snsuser , boolean vip ,DataMessage dataMessage, int maxUserNum , int maxQueueNum){
		AgentQueueMessage agentQueueMessage = null ;
		AgentUser agentuser = null ;
		Instruction lastInstruct = null;
		AgentStatus serviceagent = null ;
		AgentStatus readyagent = null ;
		//分配坐席提示信息
		String tipmsg=null;
		/********************************/
		//最后一次IMR
		List<Instruction> insList =null;
		if(dataMessage != null && dataMessage.isEvent()){
			/**
			 * 保存用户消息
			 */
			APIContext.saveMessage(dataMessage);
			/**
			 * 保存自动回复消息
			 */
			//事件处理
			agentQueueMessage = getEventMessage(dataMessage);
		}else{
			/**
			 * 判断是否是用户主动发起的关闭链接请求
			 */
			// 添加是否启用用户主动断开连接的判断，禁用的话，输入内容作为普通信息发送给坐席
			SearchSetting setting=EapSmcDataContext.getSearchSetting(orgi);
//			Instruction userDisLinkInstruction=null;
//			if(setting!=null&&setting.isUserdislink()){
//				String id=SmcRivuDataContext.getSearchSetting(orgi).getUserdislinkins();
//				userDisLinkInstruction = RivuDataContext.getInstructPlugin(orgi, id==null?"":id) ;
//			}
			/**
			 * 机器人处理， 1：指令处理
			 * 				1、从当前缓存的内存队列中查询用户的随路数据，如果找到，则使用随路数据服务，如果未找到，则从持久化接口中查询
			 * 				2、如果都未找到，则分别提示业务指令和系统指令。默认的，用户关注后的第一条消息为系统导航菜单提示
			 * 		   2：业务机器人处理
			 * 		   3：第三方排队机处理
			 */
				/**
				 * 从数据库存储中找到最后一次系统指令提示
				 */
			String lastinstruct =null /*考虑性能的问题，注释了PersistenceFactory.getInstance().getLastInstruct(snsuser.getUserid(), dataMessage)*/ ;
			ExtensionPoints plugin = null ;
			//判断用户是在服务中的，且插件为断开连接的插件
			if(userQueue.get(snsuser.getUserid())!=null && dataMessage.getChannel().getText()!=null){
				insList = EapDataContext.getInstruct(orgi).get(dataMessage.getChannel().getText().toLowerCase()) ;
				if(insList!=null && insList.size()>0){
					lastInstruct = insList.get(0) ;
				}
				if(lastInstruct!=null && lastInstruct.getPlugin()!=null){
					plugin = EapDataContext.getPlugin(lastInstruct.getPlugin()) ;
				}
			}
			if(EapDataContext.MessageType.EVENT.toString().equals(dataMessage.getChannel().getMessagetype()) || userQueue.get(snsuser.getUserid())==null || (plugin!=null && plugin.getClazz()!=null && "com.rivues.core.plugin.UserDisLinkInstructPlugin".equals(plugin.getClazz().trim()))){
				try {
					agentQueueMessage = getMessageInstruct(orgi , dataMessage.getChannel().getText() , lastinstruct , dataMessage , userQueue.get(snsuser.getUserid())) ;
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			boolean isKfEventQueneClick=false;
			AgentUser au = null;
			if(queueUserMap.containsKey(snsuser.getUserid())){   //是否在排队中
				au = queueUserMap.get(snsuser.getUserid());
				if(au!=null){
					isKfEventQueneClick = true;
					au.getUnreplaymessage().add(dataMessage);
					queueUserMap.remove(snsuser.getUserid());
					queueUserMap.put(snsuser.getUserid(), au);
					//保存排队时用户发送的消息
					for (AgentUser au2 : queueUserList) {
						if(au2.getUserid().equals(snsuser.getUserid())){
							queueUserList.remove(au2);
						}
					}
					queueUserList.add(au);
				}
			}
			if(userQueue.containsKey(snsuser.getUserid()) && !EapDataContext.MessageType.EVENT.toString().equals(dataMessage.getChannel().getMessagetype())){
				//考虑处理指令的时候，有用户的移除，所以重新获取
				AgentUser agentUser = userQueue.get(snsuser.getUserid()) ;
				
				agentUser.setLastmessage(new Date());
				agentUser.setTip(false) ;
				agentUser.setAgent(false) ;
				userQueue.put(agentUser.getUserid(), agentUser) ;
			}
			boolean isKfEventagingClick=false;
			//修复了客户已接入客服：然后点击企业菜单接入客服导致客服收到企业菜单的code；判断条件为：用户已接入客服&消息类型为企业菜单点击&插件为人工客服
			if(userQueue.containsKey(snsuser.getUserid()) && EapDataContext.MessageType.EVENT.toString().equals(dataMessage.getChannel().getMessagetype()) && plugin!=null && "com.rivues.core.plugin.TransferAgentInstructPlugin".equals(plugin.getClazz())){
				isKfEventagingClick=true;
			}
			/**机器人处理结束*/
			if(agentQueueMessage==null && !"unsubscribe".equals(dataMessage.getChannel().getText()) && !isKfEventagingClick && !isKfEventQueneClick){
				agentuser = userQueue.get(snsuser.getUserid()) ;
				if(agentuser == null){
					/**
					 * 排队算法，规则：
					 * 			0、如果缓存里有该用户的 坐席分配记录，则直接命中，如果没有，则到 R3 rivuES中获取一次用户服务记录，如果获取有，则优先匹配盖坐席，否则直接进行下一步
					 * 			1、根据质检评分，获取评分最高的坐席（或空闲时间最长的坐席优先分配 ， 坐席 空闲时长 按照 分钟计算 ， 分钟数 相同，则返回 质检评分最高的），
					 * 			2、如果第一步获取的坐席有空闲，则返回改坐席
					 * 			3、如果第一步获取坐席不空闲，则重新获取质检评分稍低的坐席
					 * 			4、重复判定第二步和第三步，直到找到坐席
					 * 			5、如果直到便利完所有坐席都未找到最合适的坐席，此时，再次从第一步开始查找，查找依据为 排队人数最少
					 */
					if(agentQueue!=null && isInServiceTime(setting, new Date())){
						synchronized (agentQueue) {
							Map<String, Integer> map = new TreeMap<String, Integer>();
							for (AgentStatus agentstatuss : agentUserQueue) {
								AgentStatus agentstatus = agentQueue.get(agentstatuss.getAgentno()) ;
								if(agentstatus!=null && AgentStatus.AgentStatusEnum.SERVICES.toString().equals(agentstatus.getStatus())){
									if(lastInstruct!=null && lastInstruct.isVir()){
										if(agentstatus.getAgentSkill()!=null && dataMessage.getChannel().getText().toLowerCase().equals(agentstatus.getAgentSkill().getCode())){
											if((serviceagent == null && agentstatus.getUserList().size()<maxUserNum) || serviceagent.getUserList().size()>agentstatus.getUserList().size() && agentstatus.getAgentSkill()!=null && agentstatus.getAgentSkill().getCode()!=null && agentstatus.getAgentSkill().getCode().toLowerCase().equals(dataMessage.getChannel().getText().toLowerCase())){
												serviceagent = agentstatus ;
												break;
											}
										}
									}else{
//										map.put(agentstatus.getAgentno(), agentstatus.getUserList().size());
										if((serviceagent == null && agentQueue.get(agentstatuss.getAgentno()).getUserList().size() < maxUserNum)){
											serviceagent = agentstatus ;
											break;
										}else if(agentQueue.get(agentstatuss.getAgentno()).getUserList().size()>=maxUserNum){
											readyagent=agentstatus;
											continue;
										}
									}
								}else if(readyagent==null && AgentStatus.AgentStatusEnum.READY.toString().equals(agentstatus.getStatus())){   //排队
									//开启技能组
									if(lastInstruct!=null && lastInstruct.isVir() && setting!=null && setting.isSkill() && agentstatus.getAgentSkill()!=null ){
										if(dataMessage.getChannel().getText().toLowerCase().equals(agentstatus.getAgentSkill().getCode())){
											readyagent=agentstatus;
										}
									}else{
										readyagent=agentstatus;
									}
								}
							}
							//未找到上次服务坐席,使用平均分配法则
							if(serviceagent==null && map!=null && map.size()>0){
								List<Entry<String,Integer>> list =new ArrayList<Entry<String,Integer>>(map.entrySet());
								Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
									public int compare(Map.Entry<String, Integer> o1,
											Map.Entry<String, Integer> o2) {
										return (o1.getValue() - o2.getValue());
									}
								});
								String agentno = list.get(0).getKey();
								AgentStatus agentstatus = agentQueue.get(agentno) ;
								if((serviceagent == null && agentQueue.get(agentno).getUserList().size() < maxUserNum)){
									serviceagent = agentstatus ;
								}else{
									readyagent=agentstatus;
								}
							}
							//System.out.println("排队前后首位的是：=============="+agentUserQueue..peekFirst().getAgentno());
						}
					}
					if(serviceagent==null){		//未获得坐席信息，ACD策略，需要进入排队队列 ， 等待坐席状态更新
						agentuser = new AgentUser(null , snsuser.getUserid() , snsuser.getChannel() , snsuser , dataMessage.getChannel().getSource()) ;
						agentQueueMessage = pushUserQuene(serviceagent, orgi, dataMessage, agentuser, vip, readyagent) ;
					}else{
						agentuser = new AgentUser(serviceagent.getAgentno() , snsuser.getUserid() , snsuser.getChannel()  , snsuser , dataMessage.getChannel().getSource()) ;
						if(dataMessage!=null){
							agentuser.getUnreplaymessage().add(dataMessage) ;
						}
						/**
						 * 坐席分配
						 */
						boolean res = allotAgentUser(serviceagent.getAgentno(), orgi, agentuser, snsuser) ;
						if(res){
							tipmsg="您好，现在是"+serviceagent.getAgentno()+"为您服务，请问有什么可以帮您？";
							agentQueueMessage = new AgentQueueMessage(agentuser , serviceagent ,EapDataContext.AgentQueueMessageType.ALLOTAGENT.toString() , tipmsg);
							agentuser.setLastmessage(new Date());
							agentuser.setTip(false) ;
							agentuser.setAgent(true) ;
							userQueue.put(agentuser.getUserid(), agentuser) ;
							if(agentQueueMessage!=null){
//												dataMessage.setUserid(agentuser.getUserid());
								dataMessage.getChannel().setText("请求接入客服");
								APIContext.sendToAgent(Constant.APP_CHANNEL, agentQueueMessage.getAgentUser().getAgentno() , dataMessage , dataMessage.getOrgi()) ;
							}
							
						}else{
							agentuser = new AgentUser(null , snsuser.getUserid() , snsuser.getChannel() , snsuser , dataMessage.getChannel().getSource()) ;
							agentQueueMessage = pushUserQuene(serviceagent, orgi, dataMessage, agentuser, vip, serviceagent) ;	//这个地方不一样，从分布式缓存里获取的 作息当前服务用户数量超过5个以后重新将该用户返回给排队队列
						}
					}		
				}else{
					agentQueueMessage = new AgentQueueMessage(agentuser , serviceagent ,EapDataContext.AgentQueueMessageType.INSERVICE.toString() , "");
					if(dataMessage!=null){
						while(agentuser.getUnreplaymessage().size()>5){
							agentuser.getUnreplaymessage().remove(5);
						}
						agentuser.getUnreplaymessage().add(0,dataMessage) ;
					}
				}
				if(agentQueueMessage!=null && agentQueueMessage.getAgentUser()!=null){
					/**
					 * 保存用户消息
					 */
					APIContext.saveMessage(dataMessage , agentQueueMessage);
					if(tipmsg!=null){
						//分配坐席的消息，显示到坐席的界面
						tipAllotoAgentMsg(orgi, agentuser, tipmsg);
					}
				}else{
					APIContext.saveMessage(dataMessage);
				}
				/**
				 * 如果坐席不为空，将消息转发给坐席 ， 否则向用户返回一条消息，提示进入排队
				 */
				if(agentQueueMessage!=null && agentQueueMessage.getAgentUser()!=null && userQueue.get(agentQueueMessage.getAgentUser().getUserid())!=null/*&& RivuDataContext.AgentQueueMessageType.INSERVICE.toString().equals(agentQueueMessage.getType())*/){
					APIContext.sendMessageToAgent(agentQueueMessage, dataMessage) ;
				}
			}else{	//保存系统自动服务消息
				if(isKfEventagingClick){
					//不做处理，也不保存数据
				}else if(isKfEventQueneClick){//排队时点击kf
					agentQueueMessage = new AgentQueueMessage(au , serviceagent ,EapDataContext.AgentQueueMessageType.NOAGENT.toString() , "您目前正在排队中，请稍后，谢谢！");
				}else{
					/**
					 * 保存用户消息
					 */
					APIContext.saveMessage(dataMessage , agentQueueMessage);
					/**
					 * 保存自动回复消息
					 */
					try {
						if(agentQueueMessage!=null && agentQueueMessage.getMessage()!=null){
							createChannelMessage( dataMessage.getOrgi(), dataMessage.getChannel().getChannel() , dataMessage.getUserid() , agentQueueMessage.getMessage() ,agentQueueMessage.getInstruct()!=null? agentQueueMessage.getInstruct().getId() : null , dataMessage.getChannel().getSnsuser()) ;
						}
					}catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			/**
			 * 如果用户不存在，则保存用户信息到存储
			 */
			if (dataMessage.getChannel().getSnsuser().getId()==null) { // 如果 用户信息不在数据库里，则保存到数据库
				APIContext.saveUser(dataMessage.getChannel().getSnsuser());
			}
		}
		return agentQueueMessage ;
	}

	private static void tipAllotoAgentMsg(String orgi, AgentUser agentuser,
			String tipmsg) {
		//接入客服的提示消息也在坐席界面显示
		try {
			DataMessage dm=createChannelMessage(agentuser.getOrgi(), agentuser.getChannel() , agentuser.getUserid() ,  tipmsg , null , agentuser.getSnsuser(),agentuser.getContextid());
			APIContext.sendToAgent(Constant.APP_CHANNEL, agentuser.getAgentno() , dm , orgi);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param orgi
	 * @param cl
	 * @param userid
	 * @param message
	 * @param instruct
	 * @param user
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static DataMessage createChannelMessage(String orgi , String cl , String userid,String message , String instruct , SNSUser user,String contextid) throws InstantiationException, IllegalAccessException{
		DataMessage dm = null ;
		WeiXin channel = (WeiXin) EapDataContext.getSNSUserBean(cl, EapDataContext.SNSBeanType.MESSAGE.toString()).newInstance();
		channel.setTouser(userid) ;
		channel.setUserid(userid) ;
		channel.setChannel(cl) ;
		channel.setReplytype(EapDataContext.ReplyType.AUTOMATIC.toString()) ;
		channel.setChannel(cl) ;
		//保存数据，坐席可以看到提示数据:算做人工回复，以便能显示到左边坐席发送的消息
		if(contextid!=null && contextid.length()>0){
			channel.setContextid(contextid);
			channel.setReplytype(EapDataContext.ReplyType.MANUALLY.toString()) ;
		}
		if(message.trim().length()>0){
			if(message.startsWith(EapDataContext.MessageType.NEWS.toString())){
				//发送多媒体消息
				channel.setMessagetype(EapDataContext.MessageType.NEWS.toString()) ;
				if(message.length()>5){
					channel.setText(message.substring(5)) ;
				}else{
					channel.setText("");
				}
			}else{
				channel.setMessagetype(EapDataContext.MessageType.TEXT.toString()) ;
				channel.setText(message) ;
			}
			
			channel.setOrgi(orgi) ;
			if(instruct!=null){
				channel.setInstruct(instruct);
			}
			dm = new DataMessage(channel.getChannel() , channel , channel.getOrgi() , channel.getUserid()) ;
			channel.setSnsuser(user) ;
			APIContext.saveMessage(dm) ;
		}
		return dm ;
	}
	/**
	 * 
	 * 新增会话id的参数，作为保存提示消息
	 */
	public static DataMessage createChannelMessage(String orgi , String cl , String userid,String message , String instruct , SNSUser user) throws InstantiationException, IllegalAccessException{
		return createChannelMessage(orgi, cl, userid, message, instruct, user,null);
	}
	public static void staticEventMenuCount(String orgi ,String textcode){
		List<Instruction> list =EapDataContext.getInstruct(orgi).get(textcode);
		 Date date = new Date();
		 SimpleDateFormat  s = new SimpleDateFormat("yyyy-MM-dd");
		 if(list!=null && list.size()==1){
			 //创建统计信息
			 DataDic dic=new DataDic();
			 dic.setName(list.get(0).getName()==null?"":list.get(0).getName());//菜单的名称
			 dic.setCode(textcode);// 菜单的code
			 dic.setOrgi(orgi);//租户
			 dic.setTitle(s.format(date).toString());//创建时间
			 EapDataContext.getService().saveIObject(dic);	 
		 }
	}
	/**
	 * 
	 * @param orgi
	 * @param code
	 * @param text
	 * @return
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private static AgentQueueMessage getMessageInstruct(String orgi , String text  , String parentinsid , DataMessage dataMessage , AgentUser agentUser) throws InstantiationException, IllegalAccessException{
		//系统配置
		SearchSetting setting=EapSmcDataContext.getSearchSetting(orgi);
		AgentQueueMessage agentQueueMessage = null ;
		Map<String , List<Instruction>> insMap = EapDataContext.getInstruct(orgi) ;
		InstructPluginInterface instractPlugin = null ;
		
		if(insMap!=null){			
			List<Instruction> insList = text!=null && insMap.get(text.toLowerCase())!=null?insMap.get(text.toLowerCase()):dataMessage.getChannel()!=null?insMap.get(dataMessage.getChannel().getMessagetype()):null ;
			if(insList!=null && insList.size()>0){
				Instruction msgIns = null ; 
				if(insList!=null && insList.get(0)!=null && EapDataContext.InstructionType.EVENTMENU.toString().equals(insList.get(0).getType())){
					//保存企业菜单的点击
					staticEventMenuCount(orgi,text.toLowerCase());
				}
				for(Instruction ins : insList){
					if(ins.isUserbind() && !dataMessage.getChannel().getSnsuser().isUserbind()){
						instractPlugin = new TipUserBindMessagePlugin();
						break ;
					}
					if(ins.isVir()&&setting.isSkill()){
						msgIns = ins ;
						instractPlugin = new TransferAgentSkillInstructPlugin();
						break ;
					}
					if(insMap.get(text.toLowerCase())!=null){
						if(dataMessage.getChannel().getSnsuser()!=null){
							if("zlbc".equals(ins.getCode()) || "chexianlpzlbc".equals(ins.getCode().toLowerCase())){
								SinosigZLBC szlbc=new SinosigZLBC();
								szlbc.setCaseid("0");
								EapSmcDataContext.getZlbcMap().put(dataMessage.getChannel().getSnsuser().getApiusername(),szlbc);
							}
						}
						if("0".equals(ins.getParent())){
	 					    msgIns = ins ;			//找到系统指令
							List<Instruction> subInstructList = EapDataContext.getInstructList(orgi, ins.getId()) ;
							if(subInstructList.size()>0){
								//提示下级指令
								instractPlugin = new TipSubInstructPlugin();
							}else{
								if(msgIns.getPlugin()!=null && msgIns.getPlugin().length()>0){
									ExtensionPoints plugin = EapDataContext.getPlugin(msgIns.getPlugin()) ;
									if(plugin!=null){
										try {
											instractPlugin = (InstructPluginInterface) Class.forName(plugin.getClazz()).newInstance() ;
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
									if(parentinsid !=null &&parentinsid.equals(ins.getParent())){
										break ;
									}
								}else{
									instractPlugin = new TipCurrentInstructPlugin();
								}
							}
						}else if(ins.getParent()!=null&&!ins.isVir()){
							//parentinsid!=null && parentinsid.equals(ins.getParent())
							msgIns = ins ;			//找到系统指令
							List<Instruction> subInstructList = EapDataContext.getInstructList(orgi, ins.getId()) ;
							if(subInstructList.size()>0){
								instractPlugin = new TipSubInstructPlugin();
							}else{	//找到最后一级，执行最近的一级上级目录的插件
								Instruction instruct = EapDataContext.getInstructPlugin(orgi, ins.getId());
								
								if(instruct.getPlugin()!=null && instruct.getPlugin().length()>0){
									ExtensionPoints plugin = EapDataContext.getPlugin(instruct.getPlugin()) ;
									if(plugin!=null){
										try {
											instractPlugin = (InstructPluginInterface) Class.forName(plugin.getClazz()).newInstance() ;
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
									if(parentinsid !=null &&parentinsid.equals(ins.getParent())){
										break ;
									}
								}else{
									instractPlugin = new TipCurrentInstructPlugin();
								}
							}
						}else{
							continue ;
						}
						break ;
					}else{
						if((parentinsid!=null && parentinsid.equals(ins.getParent()))||(dataMessage.getChannel().getSnsuser()!=null && EapSmcDataContext.getZlbcMap().containsKey(dataMessage.getChannel().getSnsuser().getApiusername()))){
							if(dataMessage.getChannel().getSnsuser()!=null  && EapSmcDataContext.getZlbcMap().containsKey(dataMessage.getChannel().getSnsuser().getApiusername())){
								//案件对象
								SinosigZLBC zlbc=EapSmcDataContext.getZlbcFromMap(orgi,dataMessage.getChannel().getSnsuser().getApiusername());
								msgIns=new Instruction();
								if(zlbc!=null && zlbc.getCaseid().length()>1 && EapDataContext.MessageType.IMAGE.toString().equals(dataMessage.getChannel().getMessagetype())){
									instractPlugin = new SinosigZlpcSaveImagePlugin();
									break ;
								}else if(zlbc!=null && zlbc.getCaseid().length()==1){
									instractPlugin = new SinosigZlpcValidCardPlugin();
									break ;
								}
							}else{
								msgIns = ins ;			//找到系统指令
								List<Instruction> subInstructList = EapDataContext.getInstructList(orgi, ins.getId()) ;
								if(subInstructList.size()>0){
									instractPlugin = new TipSubInstructPlugin();
								}else{	//找到最后一级，执行最近的一级上级目录的插件
									Instruction instruct = EapDataContext.getInstructPlugin(orgi, ins.getId());
									if(instruct.getPlugin()!=null && instruct.getPlugin().length()>0){
										ExtensionPoints plugin = EapDataContext.getPlugin(instruct.getPlugin()) ;
										if(plugin!=null){
											try {
												instractPlugin = (InstructPluginInterface) Class.forName(plugin.getClazz()).newInstance() ;
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									}else{
										instractPlugin = new TipCurrentInstructPlugin();
									}
								}
							}
						}
					}
				}
				if(instractPlugin!=null){
					String message = instractPlugin.getMessage(msgIns , agentUser , orgi , dataMessage.getChannel()) ;
					if(message!=null){
						agentQueueMessage = new AgentQueueMessage(null , null ,EapDataContext.AgentQueueMessageType.AUTOMATICREPLY.toString() , message);
						agentQueueMessage.setInstruct(msgIns) ;
					}
				}
			}
			//判断插件为空，且不在服务中的状态
			if(instractPlugin==null && ((userQueue.get(dataMessage.getUserid())==null))){	//未命中消息，无指令
				//提示下级指令
				instractPlugin = new TipNotFoundInstructPlugin();
				String message = instractPlugin.getMessage(null , null , orgi , dataMessage.getChannel()) ;
				if(message!=null){
					agentQueueMessage = new AgentQueueMessage(null , null ,EapDataContext.AgentQueueMessageType.AUTOMATICREPLY.toString() , message);
				}
			}
		}
		return agentQueueMessage ;
	}
	
	private static AgentQueueMessage getEventMessage(DataMessage dataMessage){
		if(dataMessage!=null && dataMessage.isEvent()){
			/**
			 * 消息处理，从后台获取数据处理插件，根据code获取消息
			 */
		}
		return null ;
	}
	/**
	 * 为对话用户分配坐席
	 * @param agentuser
	 * @param orgi
	 * @param serviceagent
	 * @param snsuser
	 */
	private synchronized static void allotAgent(AgentUser agentuser , String orgi , AgentStatus serviceagent , SNSUser snsuser , String contextid){
		agentuser.setAgentno(serviceagent.getAgentno()) ;
		agentuser.setOrgi(orgi) ;
		agentuser.setLogindate(new Date()) ;

		if(snsuser.getUserid()==null){
			snsuser = PersistenceFactory.getInstance().getSnsUserInfo(snsuser.getApiusername(), snsuser.getChannel(), snsuser.getOrgi()) ;
			agentuser.setSnsuser(snsuser) ;
		}
		/**
		 * 用户首次登入，需要为用户分配 会话ID，坐席的服务ID为当前日期 , 将 agentuser保存到 数据库
		 */
		agentuser.setContextid(contextid!=null ? contextid : String.valueOf(System.currentTimeMillis())) ;
		agentuser.setAgentserviceid(serviceagent.getAgentserviceid()) ;
		/**
		 * 首先判断用户是否存在，如果不存在，则保存用户到存储
		 */
		EapDataContext.getService().saveIObject(agentuser) ;

		//保存用户排队时发送的消息
		if(agentuser!=null && agentuser.getUnreplaymessage()!=null && agentuser.getUnreplaymessage().size()>1){
			for(int i=1;i<agentuser.getUnreplaymessage().size();i++){
				//执行更新操作
				DataMessage dms=agentuser.getUnreplaymessage().get(i);
				List<WeiXin> userList = EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(WeiXin.class).add(Restrictions.eq("msgid", ((WeiXin)dms.getChannel()).getMsgid())).add(Restrictions.eq("orgi", orgi)))  ;
				if(userList!=null && userList.size()>0){
					WeiXin wx=userList.get(0);
					wx.setContextid(agentuser.getContextid()) ;
					wx.setUserid(agentuser.getSnsuser().getUserid()) ;
					wx.setUsername(agentuser.getSnsuser().getUsername()) ;
					EapDataContext.getService().saveOrUpdateIObject(wx);
				}
			}
		}
		agentuser.setLastmessage(new Date());
		boolean iscz = true;
		for (Iterator iter = serviceagent.getUserList().iterator(); iter.hasNext();) {
			AgentUser element = (AgentUser)iter.next();
			if (agentuser.equals(element))
				iscz = false;
		}
		if(iscz){
//			serviceagent.getUserList().add(agentuser) ;
//			
//			
//			agentQueue.put(serviceagent.getAgentno(), serviceagent);
			
			//考虑新增用户的时候，有用户的添加，所以重新获取
			if(userQueue.get(agentuser.getUserid())==null){
				userQueue.put(agentuser.getUserid(), agentuser) ;
			}
		}
		//刷新在线绿人
		DataMessage dataMessage = new DataMessage(agentuser.getUserid(),Constant.NEWUSER,  new WeiXin() , agentuser.getOrgi(), agentuser.getUserid()) ;
		APIContext.sendToAgent(Constant.APP_CHANNEL, serviceagent.getAgentno() , dataMessage , orgi) ;
		//更新队列里的AgentStatus，避免排队的时候不更新数据
		removeToLastItem(serviceagent,orgi);
		
	}
	/**
	 * 
	 * @param orgi
	 * @return
	 */
	public static List<AgentStatus> getAgentList(String current , String orgi){
		List<AgentStatus> agentList = new ArrayList<AgentStatus>();
		Iterator<String> iterator = agentQueue.keySet().iterator() ;
		while(iterator.hasNext()){
			String agentno = iterator.next() ;
			if(!current.equals(agentno) && agentQueue.get(agentno).getStatus().equals(AgentStatus.AgentStatusEnum.SERVICES.toString())){
				agentList.add(agentQueue.get(agentno)) ;
			}
		}
		return agentList ;
	}
	
	/**
	 * 
	 * @param orgi
	 * @return
	 */
	public static List<AgentStatus> getAgentListBySkill(String current , String orgi , String skill){
		List<AgentStatus> agentList = new ArrayList<AgentStatus>();
		Iterator<String> iterator = agentQueue.keySet().iterator() ;
		while(iterator.hasNext()){
			String agentno = iterator.next() ;
			if(!current.equals(agentno) && 
					agentQueue.get(agentno).getStatus().equals(AgentStatus.AgentStatusEnum.SERVICES.toString()) && 
					agentQueue.get(agentno).getAgentSkill()!=null &&
					agentQueue.get(agentno).getAgentSkill().getCode().equals(skill)){
				agentList.add(agentQueue.get(agentno)) ;
			}
		}
		return agentList ;
	}
	
	/**
	 * 
	 * @param orgi
	 * @return
	 */
	public static AgentStatus getAgent(String agentno , String orgi){
		return agentQueue.get(agentno);
	}
	
	public static AgentUser getAgentUser(String orgi , String userid){
		return userQueue.get(userid);
	}
	
	public static void addUserToQueue(String orgi , AgentUser agentUser){
		if(userQueue.get(agentUser.getUserid())==null){
			userQueue.put(agentUser.getUserid(), agentUser) ;
		}else{
//			agentUser.setAgentUser(queueUserMap.remove(agentUser.getUserid()));
//			queueUserMap.put(agentUser.getUserid(), agentUser);
		}
	}
	public static void removeUser(String userid , String orgi) {
		userQueue.remove(userid) ;
	}
	/**
	 * 轮询：移除队列中的坐席
	 * @param orgi
	 * @param agentno
	 */
	public static void removeAgentUserQueue(String orgi,String agentno){
		for(int i=0 ; i< agentUserQueue.size() ; ){
			AgentStatus status = agentUserQueue.get(i) ;
			if(agentno.equals(status.getAgentno())){
				agentUserQueue.remove(i) ;
				continue ;
			}
			i++;
		}
	}
	/**
	 * 如果分配了客户，则移动到队尾
	 * @param agentstatus
	 * @param orgi
	 */
	public static void removeToLastItem(AgentStatus agentstatus,String orgi){
		for(int i=0 ; i< agentUserQueue.size() ; i++){
			AgentStatus status = agentUserQueue.get(i) ;
			if(agentstatus.getAgentno().equals(status.getAgentno())){
				agentUserQueue.remove(i) ;
				
				agentUserQueue.add(agentstatus) ;
				break;
			}
		}
	}
	/**
	 * 如果分配了客户，则移动到队尾 , UserList 已经修改为从 userQuene中实时获取
	 * @param agentstatus
	 * @param orgi
	 */
	public static void removeAgentQueueZUserList(String orgi,String userid,String agentno){
//		AgentStatus agentStatus = agentQueue.get(agentno) ;
//		if(agentStatus!=null){
////			for(int i=0;i<agentStatus.getUserList().size();i++){
////				AgentUser agu=agentStatus.getUserList().get(i);
////				if(agu!=null && agu.getUserid().equals(userid)){
////					//刷新在线绿人
////					
////					agentStatus.getUserList().remove(i) ;
////					break ;
////				}
////			}
//			agentQueue.put(agentno,agentStatus);
//		}
	}
	private static boolean isInServiceTime(SearchSetting setting,Date now){
		if(setting.getWork() && setting.getWorktime()!=null && setting.getNowork()!=null){
			log.info("***********setting  true ********");
			try {
				Date startD = sdf.parse(setting.getWorktime());
				Date endD = sdf.parse(setting.getNowork());
				boolean isAfterStart = now.getHours()>startD.getHours() || (now.getHours()==startD.getHours() && now.getMinutes()>=startD.getMinutes());
				boolean isBeforeEnd = now.getHours()<endD.getHours() || (now.getHours()==endD.getHours() && now.getMinutes()<=endD.getMinutes());
				return isAfterStart && isBeforeEnd;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}else{
			return true;
		}
	}
}
