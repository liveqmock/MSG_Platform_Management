package com.neusoft.web.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "rivu_sinosig_opt_count")
@org.hibernate.annotations.Proxy(lazy = false)
public class OptCount  implements java.io.Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private	String	id	;
	private	String	apiusername	;
	private	String	opttype	;
	private	String	optname	;
	private	String	busitype	;
	private	Date	optdate	;
	/**创建时间起*/
	private Date optdateBegin;
	/**创建时间止*/
	private Date optdateEnd;
	private String  orgi ;
	private	int	    isvalid	;
	private	String	remark1	;
	private	String	remark2	;
	private	String	remark3	;
	private	String	remark4	;
	private	String	remark5	;
	private	String	remark6	;
	private	String	remark7	;
	private	String	remark8	;
	
	private String busito ;
	
	private String busitypes ;
	
	@Id
	@Column(length = 32)
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getApiusername() {
		return apiusername;
	}
	public void setApiusername(String apiusername) {
		this.apiusername = apiusername;
	}
	public String getOpttype() {
		return opttype;
	}
	public void setOpttype(String opttype) {
		this.opttype = opttype;
	}
	public String getOptname() {
		return optname;
	}
	public void setOptname(String optname) {
		this.optname = optname;
	}
	public String getBusitype() {
		return busitype;
	}
	public void setBusitype(String busitype) {
		this.busitype = busitype;
	}
	public Date getOptdate() {
		return optdate;
	}
	public void setOptdate(Date optdate) {
		this.optdate = optdate;
	}
	
	@Transient
	public Date getOptdateBegin() {
		return optdateBegin;
	}
	public void setOptdateBegin(Date optdateBegin) {
		this.optdateBegin = optdateBegin;
	}
	
	@Transient
	public Date getOptdateEnd() {
		return optdateEnd;
	}
	public void setOptdateEnd(Date optdateEnd) {
		this.optdateEnd = optdateEnd;
	}
	public String getOrgi() {
		return orgi;
	}
	public void setOrgi(String orgi) {
		this.orgi = orgi;
	}
	public int getIsvalid() {
		return isvalid;
	}
	public void setIsvalid(int isvalid) {
		this.isvalid = isvalid;
	}
	public String getRemark1() {
		return remark1;
	}
	public void setRemark1(String remark1) {
		this.remark1 = remark1;
	}
	public String getRemark2() {
		return remark2;
	}
	public void setRemark2(String remark2) {
		this.remark2 = remark2;
	}
	public String getRemark3() {
		return remark3;
	}
	public void setRemark3(String remark3) {
		this.remark3 = remark3;
	}
	public String getRemark4() {
		return remark4;
	}
	public void setRemark4(String remark4) {
		this.remark4 = remark4;
	}
	public String getRemark5() {
		return remark5;
	}
	public void setRemark5(String remark5) {
		this.remark5 = remark5;
	}
	public String getRemark6() {
		return remark6;
	}
	public void setRemark6(String remark6) {
		this.remark6 = remark6;
	}
	public String getRemark7() {
		return remark7;
	}
	public void setRemark7(String remark7) {
		this.remark7 = remark7;
	}
	public String getRemark8() {
		return remark8;
	}
	public void setRemark8(String remark8) {
		this.remark8 = remark8;
	}
	@Transient
	public String getBusito() {
		return busito;
	}
	public void setBusito(String busito) {
		this.busito = busito;
	}
	@Transient
	public String getBusitypes() {
		return busitypes;
	}
	public void setBusitypes(String busitypes) {
		this.busitypes = busitypes;
	}
	
	

}
