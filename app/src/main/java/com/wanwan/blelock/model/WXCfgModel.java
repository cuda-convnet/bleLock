package com.wanwan.blelock.model;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "wxcfgmodel")
public class WXCfgModel {
	@Column(name = "id", isId = true)
	private int id;
	
	@Column(name = "deviceid")
	private String deviceid;
	
	@Column(name = "mac")
	private String mac;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDeviceid() {
		return deviceid;
	}

	public void setDeviceid(String deviceid) {
		this.deviceid = deviceid;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}
	
}
