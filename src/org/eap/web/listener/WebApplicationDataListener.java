/**
 * Licensed to the Rivulet under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     webapps/LICENSE-Rivulet-1.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eap.web.listener;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.neusoft.core.EapDataContext;
import com.neusoft.core.EapSmcDataContext;
import com.neusoft.util.tools.IndexTools;

/**
 * @author jaddy0302 Rivulet WebApplicationContextListener.java 2010-3-3
 * 
 */
public class WebApplicationDataListener implements HttpSessionListener,
		ServletContextListener, ServletContextAttributeListener {

	public void contextInitialized(final ServletContextEvent sce) {
		EapDataContext.setWac(WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext()));
		EapDataContext.REAL_PATH = sce.getServletContext().getRealPath("/WEB-INF/");
		EapDataContext.SAVE_FILE_DIR = sce.getServletContext().getRealPath(EapDataContext.SAVE_FILE_DIR) ;
		EapDataContext.initPlugin();
		EapSmcDataContext.initData();
		/**
		 * 数据代理RPC服务
		 */
		try {
//			Thread checkThread = new Thread(new Runnable() {
//				@Override
//				public void run() {
//					String[] hosts = sce.getServletContext().getInitParameter("rpc.host").split(",") ;
//					while(true){
////						for(int i=0 ; i<APIContext.getRpcServers().size() ; ){
////							Client client = APIContext.getRpcServers().get(i) ;
////							if((System.currentTimeMillis()-client.getLastPingTime())> 10 * 1000 && client.isConnected()){
////								client.close() ;
////								APIContext.getRpcServers().remove(i) ;
////								continue ;
////							}
////							i++;
////						}
//						if((APIContext.getRpcServers().size() + APIContext.getWaitConnectionServers().size())==hosts.length){
//							try {
//								Thread.sleep(3000) ;
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}else{
//							for(String host : hosts){
//								boolean live = false ;
//								for(Client client:APIContext.getRpcServers()){
//									if(host.equals(client.getHost())){
//										live = true ;
//										break ;
//									}
//								}
//								for(Client client:APIContext.getWaitConnectionServers()){
//									if(host.equals(client.getHost())){
//										live = true ;
//										break ;
//									}
//								}
//								if(!live){
//									System.out.println("创建新Server");
//									String[] server = host.split(":");
//									if(server.length == 2){
//										new Client( server[0] , Integer.parseInt(server[1]),host ,  new MessageHandler());
//									}
//									
//								}
//							}
//						}
//					}
//				}
//			});
//			checkThread.start();
			
			EapDataContext.initJars(EapDataContext.REAL_PATH );
			EapDataContext.initInstruct();
			/**
			 * 初始化分词
			 */
			IndexTools.getInstance() ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sessionCreated(HttpSessionEvent arg0) {
		// TODO Auto-generated method stub
	}

	public void sessionDestroyed(HttpSessionEvent arg0) {
	}

	public void contextDestroyed(ServletContextEvent arg0) {
	}

	public void attributeAdded(ServletContextAttributeEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void attributeRemoved(ServletContextAttributeEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void attributeReplaced(ServletContextAttributeEvent arg0) {
		// TODO Auto-generated method stub

	}
}
