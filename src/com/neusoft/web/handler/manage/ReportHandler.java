package com.neusoft.web.handler.manage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.neusoft.core.channel.WeiXin;
import com.neusoft.util.ExcelMethod;
import com.neusoft.util.persistence.DBPersistence;
import com.neusoft.web.handler.Handler;
import com.neusoft.web.handler.RequestData;
import com.neusoft.web.handler.ResponseData;
import com.neusoft.web.model.DataDic;

@Controller
@SessionAttributes
@RequestMapping({"/{orgi}/report"})
public class ReportHandler extends Handler
{
  Connection connection = null;
  PreparedStatement ps = null;
  ResultSet rs = null;
  String[] colors = { "AFD8F8", "F6BD0F", "FF8E46", "8BBA00", "008E8E", "D64646", "8E468E", "588526", "B3AA00", "008ED6", "9D080D", "A186BE" };

  @RequestMapping({"/tablelist/{channel}/{type}/{beginTime}/{endTime}"})
  public ModelAndView reportlist(HttpServletRequest request, @PathVariable String orgi, @PathVariable String channel, @PathVariable String type, @PathVariable String beginTime, @PathVariable String endTime, @ModelAttribute("data") RequestData data)
  {
    ResponseData responseData = new ResponseData("/pages/manage/report/tablelist");
    String result = null;
    ReportHandler rh = new ReportHandler();
    result = rh.getReport(channel, type, beginTime, endTime);
    responseData.setMessage(result);
    return request(responseData, orgi, data);
  }

  @RequestMapping({"/tablelist"})
  public ModelAndView reportType(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("data") RequestData data)
  {
    ResponseData responseData = new ResponseData("/pages/manage/report/tablelist");
    String result = null;
    ReportHandler rh = new ReportHandler();
    String type = request.getParameter("type");
    String beginTime = request.getParameter("beginTime");
    String endTime = request.getParameter("endTime");
    if (type.equals("YEAR")) {
      beginTime = beginTime + "-01-01 00:00:00";
      endTime = endTime + "-12-31 23:59:59";
    } else if (type.equals("MONTH")) {
      beginTime = beginTime + "-01 00:00:00";
      endTime = endTime + "-31 23:59:59";
    } else if (type.equals("DAY")) {
      beginTime = beginTime + " 00:00:00";
      endTime = endTime + " 23:59:59";
    }
    result = rh.getReport("weixin", type, beginTime, endTime);
    responseData.setMessage(result);
    return request(responseData, orgi, data);
  }

  public String getReport(String channel, String type, String beginTime, String endTime)
  {
    StringBuffer sb = new StringBuffer();
    try
    {
      this.connection = DBPersistence.getconnection();

      if (type.equals("nullType")) {
        sb.append("<graph caption='通信次数' xAxisName='省份' yAxisName='总数' showNames='1' decimalPrecision='0' formatNumberScale='0'>");
      }
      else {
        sb.append("<graph caption='通信次数' xAxisName='日期' yAxisName='总数' showNames='1' decimalPrecision='0' formatNumberScale='0'>");
      }

      if (this.connection != null) {
        try {
          String sql = null;
          if (!type.equals("nullType")) {
            String creatstart = beginTime.length() > 12 ? "and createtime>='" + beginTime + "'" : "";
            String creatend = endTime.length() > 12 ? "and createtime<='" + endTime + "'" : "";
            if (type.equals("DAY")) {
              sql = "select left(createtime,4),subString(createtime,6,2)," + type + "(createtime),count(content) from rivu_weixinmessage where channel=? " + creatstart + creatend + " group by " + type + "(createtime) order by " + type + "(createtime);";
              this.ps = this.connection.prepareStatement(sql);
              this.ps.setString(1, channel);
              this.rs = this.ps.executeQuery();
              while (this.rs.next())
                sb.append("<set name='").append(this.rs.getString("left(createtime,4)") + "年" + this.rs.getString("subString(createtime,6,2)") + "月" + this.rs.getString(new StringBuilder(String.valueOf(type)).append("(createtime)").toString()) + "日").append("' value='").append(this.rs.getString("count(content)")).append("' color='").append(this.colors[new java.util.Random().nextInt(12)]).append("' />");
            }
            else if (type.equals("MONTH")) {
              sql = "select left(createtime,4)," + type + "(createtime),count(content) from rivu_weixinmessage where channel=? " + creatstart + creatend + " group by " + type + "(createtime) order by " + type + "(createtime);";
              this.ps = this.connection.prepareStatement(sql);
              this.ps.setString(1, channel);
              this.rs = this.ps.executeQuery();
              while (this.rs.next())
                sb.append("<set name='").append(this.rs.getString("left(createtime,4)") + "年" + this.rs.getString(new StringBuilder(String.valueOf(type)).append("(createtime)").toString()) + "月").append("' value='").append(this.rs.getString("count(content)")).append("' color='").append(this.colors[new java.util.Random().nextInt(12)]).append("' />");
            }
            else if (type.endsWith("YEAR")) {
              sql = "select " + type + "(createtime),count(content) from rivu_weixinmessage where channel=? " + creatstart + creatend + " group by " + type + "(createtime) order by " + type + "(createtime);";
              this.ps = this.connection.prepareStatement(sql);
              this.ps.setString(1, channel);
              this.rs = this.ps.executeQuery();
              while (this.rs.next()) {
                sb.append("<set name='").append(this.rs.getString(new StringBuilder(String.valueOf(type)).append("(createtime)").toString()) + "年").append("' value='").append(this.rs.getString("count(content)")).append("' color='").append(this.colors[new java.util.Random().nextInt(12)]).append("' />");
              }
            }
            sb.append("</graph>");
          } else {
            sql = "select rivu_weixinuser.province,count(rivu_weixinmessage.contextid) from rivu_weixinmessage left join rivu_weixinuser on rivu_weixinmessage.userid=rivu_weixinuser.userid  where  rivu_weixinmessage.channel=? and rivu_weixinuser.province !='' group by rivu_weixinuser.province order by count(rivu_weixinmessage.contextid )  limit 10;";
            this.ps = this.connection.prepareStatement(sql);
            this.ps.setString(1, channel);
            this.rs = this.ps.executeQuery();
            while (this.rs.next()) {
              sb.append("<set name='").append(this.rs.getString("rivu_weixinuser.province")).append("' value='").append(this.rs.getString("count(rivu_weixinmessage.contextid)")).append("' color='").append(this.colors[new java.util.Random().nextInt(12)]).append("' />");
            }
            sb.append("</graph>");
          }
        } catch (Exception e) {
          e.printStackTrace();
          try
          {
            this.rs.close();
            this.ps.close();
            this.connection.close();
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        }
        finally
        {
          try
          {
            this.rs.close();
            this.ps.close();
            this.connection.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        try
        {
          this.rs.close();
          this.ps.close();
          this.connection.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    catch (Exception localException1) {
    }
    return sb.toString();
  }
  @RequestMapping({"/sinosigreport"})
  public ModelAndView sinosigreport(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("rqdata") RequestData rqdata) {
    ResponseData responseData = new ResponseData("/pages/manage/report/sinosigstatslist");

    String[] colors = { "AFD8F8", "F6BD0F", "FF8E46", "8BBA00", "008E8E", "D64646", "8E468E", "588526", "B3AA00", "008ED6", "9D080D", "A186BE" };
    String beginTime = request.getParameter("beginTime") == null ? "" : request.getParameter("beginTime");
    String endTime = request.getParameter("endTime") == null ? "" : request.getParameter("endTime");

    String bsql = "";
    if (!beginTime.equals("")) {
      bsql = "and msg.title >='" + beginTime + "'";
    }
    String esql = "";
    if (!endTime.equals("")) {
      esql = "and msg.title <='" + endTime + "'";
    }
    StringBuffer sb = new StringBuffer();
    String sql = "select msg.name,count(*) from DataDic msg where msg.orgi='" + orgi + "'  " + bsql + "  " + esql + "  and msg.name is not null and msg.name!='关注提示' group by msg.name order by count(*) desc";
    sb.append("<graph caption='菜单点击统计' xAxisName='菜单' yAxisName='总数' showNames='1' decimalPrecision='0' formatNumberScale='0'>");
    List list = super.getService().hqlList(sql, DataDic.class, 100, 1);
    for (Iterator localIterator = list.iterator(); localIterator.hasNext(); ) { Object object = localIterator.next();
      Object[] account = (Object[])object;
      if ((account != null) && (account.length == 2)) {
        sb.append("<set name='").append(account[0]).append("' value='").append(account[1]).append("' color='").append(colors[new java.util.Random().nextInt(12)]).append("' />");
      }
    }
    if ((list == null) || ((list != null) && (list.size() == 0))) {
      sb.append("<set name='无数据' value='0' ").append(" color='").append(colors[new java.util.Random().nextInt(12)]).append("' />");
    }
    sb.append("</graph>");
    responseData.setMessage(sb.toString());
    ModelAndView view = request(responseData, orgi, rqdata);
    view.addObject("beginTime", beginTime);
    view.addObject("endTime", endTime);
    return view;
  }
  @RequestMapping({"/agentreport"})
  public ModelAndView agentreport(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("rqdata") RequestData rqdata) {
    ResponseData responseData = new ResponseData("pages/manage/report/tablelist");
    String beginTime = request.getParameter("beginTime");
    String endTime = request.getParameter("endTime");
    String[] colors = { "AFD8F8", "F6BD0F", "FF8E46", "8BBA00", "008E8E", "D64646", "8E468E", "588526", "B3AA00", "008ED6", "9D080D", "A186BE" };
    List agentlist = super.getService().hqlList("select to_char(agt.logindate,'yyyy-mm-dd'),count(*) from AgentUser agt where orgi='" + orgi + "' " + ((beginTime == null) || ("".equals(beginTime)) ? "" : new StringBuilder("and logindate >= to_date('").append(beginTime).append("','yyyy-MM-dd')").toString()) + ((endTime == null) || ("".equals(endTime)) ? "" : new StringBuilder("and logindate <= to_date('").append(endTime).append("','yyyy-MM-dd')").toString()) + " group by to_char(logindate,'yyyy-mm-dd') order by to_char(logindate,'yyyy-mm-dd') desc", WeiXin.class, 10, 1);
    StringBuffer sb = new StringBuffer();
    sb.append("<graph caption='通信次数' xAxisName='日期' yAxisName='总数' showNames='1' decimalPrecision='0' formatNumberScale='0'>");
    for (Iterator localIterator = agentlist.iterator(); localIterator.hasNext(); ) { Object object = localIterator.next();
      Object[] agent = (Object[])object;
      if ((agent != null) && (agent.length == 2)) {
        sb.append("<set name='").append(agent[0]).append("' value='").append(agent[1]).append("' color='").append(colors[new java.util.Random().nextInt(12)]).append("' />");
      }
    }
    if ((agentlist == null) || ((agentlist != null) && (agentlist.size() == 0))) {
      sb.append("<set name='无数据' value='0' ").append(" color='").append(colors[new java.util.Random().nextInt(12)]).append("' />");
    }
    sb.append("</graph>");
    responseData.setMessage(sb.toString());
    ModelAndView view = request(responseData, orgi, rqdata);
    view.addObject("beginTime", beginTime);
    view.addObject("endTime", endTime);
    return view;
  }

  @RequestMapping({"/agenttotal"})
  public ModelAndView agenttotal(HttpServletRequest request, @PathVariable String orgi, @ModelAttribute("rqdata") RequestData rqdata) {
    ResponseData responseData = new ResponseData("pages/manage/report/agenttotal");
    String beginTime = request.getParameter("beginTime");
    String endTime = request.getParameter("endTime");
    String subtype = request.getParameter("subtype");
    String[] colors = { "AFD8F8", "F6BD0F", "FF8E46", "8BBA00", "008E8E", "D64646", "8E468E", "588526", "B3AA00", "008ED6", "9D080D", "A186BE" };

    StringBuffer sb = new StringBuffer("select to_char(wx.createtime,'yyyy-mm-dd'),count(*) from WeiXin wx where orgi='" + orgi + "' and messagetype='text' " + ((beginTime == null) || ("".equals(beginTime)) ? "" : new StringBuilder("and createtime >= to_date('").append(beginTime).append("','yyyy-MM-dd')").toString()) + ((endTime == null) || ("".equals(endTime)) ? "" : new StringBuilder("and createtime <= to_date('").append(endTime).append("','yyyy-MM-dd')").toString()));
    if ((subtype != null) && (!"".equals(subtype))) {
      sb.append(" and replytype='").append(subtype).append("' ");
    }
    sb.append(" group by to_char(createtime,'yyyy-mm-dd') order by to_char(createtime,'yyyy-mm-dd') desc");
    List agentlist = super.getService().hqlList(sb.toString(), WeiXin.class, 10, 1);
    sb.append("<graph caption='下行消息总量' xAxisName='日期' yAxisName='总数' showNames='1' decimalPrecision='0' formatNumberScale='0'>");
    for (Iterator localIterator = agentlist.iterator(); localIterator.hasNext(); ) { Object object = localIterator.next();
      Object[] agent = (Object[])object;
      if ((agent != null) && (agent.length == 2)) {
        sb.append("<set name='").append(agent[0]).append("' value='").append(agent[1]).append("' color='").append(colors[new java.util.Random().nextInt(12)]).append("' />");
      }
    }
    if ((agentlist == null) || ((agentlist != null) && (agentlist.size() == 0))) {
      sb.append("<set name='无数据' value='0' ").append(" color='").append(colors[new java.util.Random().nextInt(12)]).append("' />");
    }
    sb.append("</graph>");
    responseData.setMessage(sb.toString());
    ModelAndView view = request(responseData, orgi, rqdata);
    view.addObject("beginTime", beginTime);
    view.addObject("endTime", endTime);
    view.addObject("subtype", subtype);
    return view;
  }

  @RequestMapping({"/export"})
  public ModelAndView export(HttpServletRequest request, HttpServletResponse response, @PathVariable String orgi)
    throws IOException
  {
    String beginTime = request.getParameter("beginTime");
    String endTime = request.getParameter("endTime");
    List agentlist = super.getService().hqlList("select to_char(agt.logindate,'yyyy-mm-dd'),count(*) from AgentUser agt where orgi='" + orgi + "' " + ((beginTime == null) || ("".equals(beginTime)) ? "" : new StringBuilder("and logindate >= to_date('").append(beginTime).append("','yyyy-MM-dd')").toString()) + ((endTime == null) || ("".equals(endTime)) ? "" : new StringBuilder("and logindate <= to_date('").append(endTime).append("','yyyy-MM-dd')").toString()) + " group by to_char(logindate,'yyyy-mm-dd') order by to_char(logindate,'yyyy-mm-dd') desc", WeiXin.class, 10, 1);
    String path = request.getRealPath("/");
    ExcelMethod excel = new ExcelMethod();
    excel.toExcelTest(agentlist, response);
    return null;
  }

  @RequestMapping({"/exportagenttotal"})
  public ModelAndView exportagenttotal(HttpServletRequest request, HttpServletResponse response, @PathVariable String orgi, @ModelAttribute("data") RequestData data)
    throws IOException
  {
    String beginTime = request.getParameter("beginTime");
    String endTime = request.getParameter("endTime");
    String subtype = request.getParameter("subtype");
    String[] colors = { "AFD8F8", "F6BD0F", "FF8E46", "8BBA00", "008E8E", "D64646", "8E468E", "588526", "B3AA00", "008ED6", "9D080D", "A186BE" };

    StringBuffer sb = new StringBuffer("select to_char(wx.createtime,'yyyy-mm-dd'),count(*) from WeiXin wx where orgi='" + orgi + "' and messagetype='text' " + ((beginTime == null) || ("".equals(beginTime)) ? "" : new StringBuilder("and createtime >= to_date('").append(beginTime).append("','yyyy-MM-dd')").toString()) + ((endTime == null) || ("".equals(endTime)) ? "" : new StringBuilder("and createtime <= to_date('").append(endTime).append("','yyyy-MM-dd')").toString()));
    if ((subtype != null) && (!"".equals(subtype))) {
      sb.append(" and replytype='").append(subtype).append("' ");
    }
    sb.append(" group by to_char(createtime,'yyyy-mm-dd') order by to_char(createtime,'yyyy-mm-dd') desc");
    List agentlist = super.getService().hqlList(sb.toString(), WeiXin.class, 10, 1);
    ExcelMethod excel = new ExcelMethod();
    excel.toExcelTest(agentlist, response);
    return null;
  }

  @RequestMapping({"/exportsinosig"})
  public ModelAndView exportsinosig(HttpServletRequest request, HttpServletResponse response, @PathVariable String orgi, @ModelAttribute("data") RequestData data)
    throws IOException
  {
    String beginTime = request.getParameter("beginTime") == null ? "" : request.getParameter("beginTime");
    String endTime = request.getParameter("endTime") == null ? "" : request.getParameter("endTime");

    String bsql = "";
    if (!beginTime.equals("")) {
      bsql = "and msg.title >='" + beginTime + "'";
    }
    String esql = "";
    if (!endTime.equals("")) {
      esql = "and msg.title <='" + endTime + "'";
    }
    StringBuffer sb = new StringBuffer();
    String sql = "select msg.name,count(*) from DataDic msg where msg.orgi='" + orgi + "'  " + bsql + "  " + esql + "  and msg.name is not null and msg.name!='关注提示' group by msg.name order by count(*) desc";
    sb.append("<graph caption='菜单点击统计' xAxisName='日期' yAxisName='总数' showNames='1' decimalPrecision='0' formatNumberScale='0'>");
    List list = super.getService().hqlList(sql, DataDic.class, 100, 1);
    List name = new ArrayList();
    List totals = new ArrayList();
    for (Iterator localIterator = list.iterator(); localIterator.hasNext(); ) { Object object = localIterator.next();
      Object[] account = (Object[])object;
      if ((account != null) && (account.length == 2)) {
        name.add(account[0].toString());
        totals.add(account[1].toString());
      }
    }

    ExcelMethod ex = new ExcelMethod();
    Object os = null;

    WritableWorkbook workbook = null;
    Date d = new Date();
    SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd-mm");
    try
    {
      response.setContentType("application/DOWLOAD");

      response.setHeader("Content-Disposition", "attachment; filename=" + s.format(d) + ".xls");
      os = response.getOutputStream();

      workbook = Workbook.createWorkbook((OutputStream)os);

      WritableSheet ws = workbook.createSheet("统计", 0);
      ws.getSettings().setShowGridLines(true);
      ws.getSettings().setProtected(false);

      for (int i = 0; i < 10; i++) {
        ws.setColumnView(i, 20);
      }
      Label titleLabel = null;

      if ((name != null) && (name.size() > 0))
      {
        for (int i = 0; i < name.size(); i++) {
          String str = (String)name.get(i);
          titleLabel = new Label(0, 0, "菜单名称", ExcelMethod.getHeadFormat());
          ws.addCell(titleLabel);
          titleLabel = new Label(i + 1, 0, str.toString(), ExcelMethod.getHeadFormat());
          ws.addCell(titleLabel);
        }
      }
      if ((totals != null) && (totals.size() > 0))
      {
        for (int i = 0; i < totals.size(); i++) {
          String str = (String)totals.get(i);
          titleLabel = new Label(0, 1, "点击次数", ExcelMethod.getHeadFormat());
          ws.addCell(titleLabel);
          titleLabel = new Label(i + 1, 1, str.toString(), ExcelMethod.getHeadFormat());
          ws.addCell(titleLabel);
        }
      }

      workbook.write();
      workbook.close();
      ((OutputStream)os).close();
    } catch (Exception e) {
      e.getCause();
      e.getMessage();
    }

    return null;
  }

  private static void exprot(List<Object[]> list, String path)
    throws IOException
  {
    HSSFWorkbook wb = new HSSFWorkbook();
    HSSFSheet sheet = null;
    sheet = wb.createSheet("统计");

    HSSFCellStyle style = wb.createCellStyle();
    style.setAlignment((short)2);
    HSSFRow row = sheet.createRow(0);

    HSSFCell cell = row.createCell((short)0);
    cell.setCellValue("日期");
    HSSFCell cell1 = row.createCell((short)1);
    cell1.setCellValue("次数");
    if ((list.size() > 0) && (list != null))
    {
      for (int i = 0; i < list.size(); i++) {
        HSSFRow r = sheet.createRow(i + 1);
        HSSFCell c = r.createCell((short)0);
        Object[] obj = (Object[])list.get(i);
        c.setCellValue(obj[0].toString());
        HSSFCell c1 = r.createCell((short)1);
        c1.setCellValue(obj[1].toString());

        cell.setCellStyle(style);
        cell1.setCellStyle(style);
      }
    }

    FileOutputStream fout = null;
    Date d = new Date();
    SimpleDateFormat s = new SimpleDateFormat("yyyy.MM.dd.mm");
    try {
      File f = new File(path + "/statistics");
      f.mkdir();
      fout = new FileOutputStream(path + "/statistics/" + s.format(d) + ".xls");
      wb.write(fout);
      fout.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
}