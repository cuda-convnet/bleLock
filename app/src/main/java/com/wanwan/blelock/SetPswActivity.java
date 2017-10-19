package com.wanwan.blelock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wanwan.blelock.been.BleBeen;

import java.util.ArrayList;
import java.util.List;
/**
 * 文 件 名: QrScanActivity
 * 创 建 人: 万万
 * 创建日期: 17-9-20 20:36
 * 描    述: 设置密码
 * 修 改 人:
 * 修改时间：
 * 修改备注：
 */
public class SetPswActivity extends AppCompatActivity implements View.OnClickListener {

    //布局相关
    private LinearLayout lin_back, lin_submit;
    private TextView tv_title, tv_macaddr;
    private EditText et_psw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mima);
        getIntendData();
        //初始化布局
        initView();
    }

    private String macAddr = "",type = "",psw="";
    private void getIntendData() {
        macAddr = getIntent().getStringExtra("macAddr");
        type = getIntent().getStringExtra("type");
        psw = getIntent().getStringExtra("psw");
        if(TextUtils.isEmpty(macAddr)){
            Toast.makeText(getApplicationContext(),"蓝牙Mac地址为空",Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    /**
     * 初始化布局
     */
    private void initView() {
        lin_back = (LinearLayout) findViewById(R.id.lin_back);
        lin_submit = (LinearLayout) findViewById(R.id.lin_submit);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_macaddr = (TextView) findViewById(R.id.tv_macaddr);
        et_psw = (EditText) findViewById(R.id.et_psw);

        lin_back.setOnClickListener(this);
        lin_submit.setOnClickListener(this);

        tv_macaddr.setText(macAddr);
        et_psw.setText(psw);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lin_back://历史记录
                finish();
                break;
            case R.id.lin_submit://扫描
                String psw = et_psw.getText().toString().trim();
                if(TextUtils.isEmpty(psw)){
                    Toast.makeText(getApplicationContext(),"请输入密码",Toast.LENGTH_SHORT).show();
                }else {
                    saveBle();
                    setResult(RESULT_OK,new Intent().putExtra("macAddr",macAddr).putExtra("type",type));
                    finish();
                }
                break;
        }
    }

    private void saveBle() {
        SharedPreferences sp=getSharedPreferences("BleLock", Context.MODE_PRIVATE);
        String bleStr = sp.getString("ble","");
        List<BleBeen> bleList = getBleList(bleStr,macAddr);
        BleBeen bleBeen = new BleBeen(macAddr,type,et_psw.getText().toString().trim());
        bleList.add(bleBeen);

        String bleListStr = "";
        for(int i=0;i<bleList.size();i++){
            bleListStr = bleList.get(i).getMacAddr()+"&" + bleList.get(i).getType()+"&"+bleList.get(i).getPsw()+"#" + bleListStr;
        }
        SharedPreferences.Editor editor=sp.edit();
        editor.putString("ble", bleListStr);
        editor.commit();
    }

    private List<BleBeen> getBleList(String bleStr, String addr) {
        List<BleBeen> bleList = new ArrayList<>();
        if(TextUtils.isEmpty(bleStr)||bleStr.split("#").length==0){
            return bleList;
        }else {
            for(int i=0;i<bleStr.split("#").length;i++){
                String ble = bleStr.split("#")[i];
                if(ble.split("&").length==3&&!addr.equals(ble.split("&")[0])){
                    BleBeen bleBeen = new BleBeen(ble.split("&")[0],ble.split("&")[1],ble.split("&")[2]);
                    bleList.add(bleBeen);
                }
            }
            return bleList;
        }
    }


}
