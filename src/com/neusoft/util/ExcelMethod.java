package com.neusoft.util;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.write.Alignment;
import jxl.write.Border;
import jxl.write.BorderLineStyle;
import jxl.write.Colour;
import jxl.write.Label;
import jxl.write.VerticalAlignment;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;


public class ExcelMethod {
	
	public void toExcelTest(List<Object[]> list, HttpServletResponse response){
		//创建工作流	
		OutputStream os = null;		
		//初始化工作表	
		WritableWorkbook workbook = null;	
		Date d = new Date();
		 Random random = new Random();
		 SimpleDateFormat s = new SimpleDateFormat("yyyy.MM.dd.mm."+random.nextInt(100));
		
		try {			
			//设置弹出对话框		
			response.setContentType("application/DOWLOAD");	
			//设置工作表的标题		
			response.setHeader("Content-Disposition", "attachment; filename="+s.format(d)+".xls");	
			os = response.getOutputStream();			
			//创建工作表	
			workbook = Workbook.createWorkbook(os);	
			//定义工作表 sheet 标题		
			WritableSheet ws = workbook.createSheet("统计", 0);	
			ws.getSettings().setShowGridLines(true);		
			ws.getSettings().setProtected(false);		
			//控制列的宽度,如果你要不给一样的宽度,就单独写,i代表的是列的下标,从0开始 ,从左到右	
			for(int i=0;i<10;i++){	
				ws.setColumnView(i, 20);	
				}	
			Label titleLabel = null;
		    if(list!=null && list.size()>0){
				// 創建标题列名称
				for(int i=0;i<list.size();i++){
					Object[] obj = (Object[]) list.get(i);
					titleLabel = new Label(0, 0, "时间", getHeadFormat());		
					ws.addCell(titleLabel);		
	 				titleLabel = new Label(i+1, 0, obj[0].toString(), getHeadFormat());		
					ws.addCell(titleLabel);	
					titleLabel = new Label(0, 1, "通信次数", getHeadFormat());		
					ws.addCell(titleLabel);	
					titleLabel = new Label(i+1, 1, obj[1].toString(), getHeadFormat());		
					ws.addCell(titleLabel);		
				}
			}
					
			workbook.write();	
			workbook.close();		
			os.close();	
			} catch (Exception e) {	
				System.out.println(e.getCause());	
				System.out.println(e.getMessage());	
				}	
		}		
	/**	 * 设置单元格样式	 * @return	 * @throws Exception	 */	
	public static WritableCellFormat getHeadFormat() throws Exception {		
		//设置字体
	WritableFont wf = new WritableFont(WritableFont.ARIAL, 8, WritableFont.BOLD);	
	//创建单元格FORMAT		
	WritableCellFormat wcf = new WritableCellFormat(wf);	
	wcf.setAlignment(Alignment.CENTRE);       
	wcf.setVerticalAlignment(VerticalAlignment.CENTRE);     
	wcf.setLocked(true);	
	wcf.setBorder(Border.ALL, BorderLineStyle.THIN, Colour.BLACK);	
	wcf.setBackground(Colour.GREY_25_PERCENT);	
	return wcf;
}
	

}
