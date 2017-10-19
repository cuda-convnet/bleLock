package com.wanwan.blelock.been;

/**
 * 文 件 名: BleBeen
 * 创 建 人: 万万
 * 创建日期: 17-9-20 20:36
 * 描    述: 蓝牙Been
 * 修 改 人:
 * 修改时间：
 * 修改备注：
 */
public class BleBeen {


    public BleBeen(String macAddr, String type, String psw) {
        this.macAddr = macAddr;
        this.type = type;
        this.psw = psw;
    }

    private String macAddr;
    private String type;
    private String psw;

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPsw() {
        return psw;
    }

    public void setPsw(String psw) {
        this.psw = psw;
    }
}
