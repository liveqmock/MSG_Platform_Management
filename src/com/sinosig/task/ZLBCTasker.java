package com.sinosig.task;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.neusoft.core.EapDataContext;
import com.neusoft.core.plugin.SinosigZlpcPostZipPlugin;
import com.neusoft.web.model.SinosigZLBC;
import com.sinosig.task.interfaces.ISinosigTasker;
import com.sinosig.task.interfaces.IZLBCVariables;

@Component
public class ZLBCTasker implements ISinosigTasker ,IZLBCVariables{

	private static Logger log =Logger.getLogger(ZLBCTasker.class);
	
	@SuppressWarnings("unchecked")
	@Scheduled(cron="0 0/30 8-20 * * ?") 
    @Override  
	public void executor() {
		try
		{
			
			List<SinosigZLBC> zlbcList = EapDataContext.getService().findAllByCriteria(DetachedCriteria.forClass(SinosigZLBC.class).add(Restrictions.and(Restrictions.eq("orgi", "sinosig") ,Restrictions.eq("status", 2))));
			
			log.info(java.util.Calendar.getInstance().getTime()+"执行，共有 < "+zlbcList.size()+" > 条需要处理！");
			
			for(int i=0;i<zlbcList.size();i++)
			{
				SinosigZLBC zlbc = zlbcList.get(i);
				
				SinosigZlpcPostZipPlugin.postZip(zlbc);
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
