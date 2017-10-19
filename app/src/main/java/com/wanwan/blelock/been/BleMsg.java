package com.wanwan.blelock.been;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 文 件 名: QrScanActivity
 * 创 建 人: 万万
 * 创建日期: 17-9-20 20:36
 * 描    述: 二维码扫描页面
 * 修 改 人:
 * 修改时间：
 * 修改备注：
 */
public class BleMsg {
    private byte[] realmsg; // 实际数据
    private String dismsg; // 显示数据
    private BluetoothGattCharacteristic charact; // 对应特征值
    private DIR dir = DIR.SEND;

    public BleMsg() {
    }

    public BleMsg(BluetoothGattCharacteristic charact, DIR dir, byte[] realmsg, String dismsg) {
        this.charact = charact;
        this.dir = dir;
        this.realmsg = realmsg;
        this.dismsg = dismsg;
    }

    public byte[] getRealmsg() {
        return realmsg;
    }

    public void setRealmsg(byte[] realmsg) {
        this.realmsg = realmsg;
    }

    public String getDismsg() {
        return dismsg;
    }

    public void setDismsg(String dismsg) {
        this.dismsg = dismsg;
    }

    public BluetoothGattCharacteristic getCharact() {
        return charact;
    }

    public void setCharact(BluetoothGattCharacteristic charact) {
        this.charact = charact;
    }

    public DIR getDir() {
        return dir;
    }

    public void setDir(DIR dir) {
        this.dir = dir;
    }

    // 发送方向
    public enum DIR {
        SEND, RECIVE
    }
}
