package com.wanwan.blelock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wanwan.blelock.been.BleBeen;

import java.util.ArrayList;
import java.util.List;

/**
 * 文 件 名: QrScanActivity
 * 创 建 人: 万万
 * 创建日期: 17-9-20 20:36
 * 描    述: 蓝牙列表页面
 * 修 改 人:
 * 修改时间：
 * 修改备注：
 */
public class BleListActivity extends AppCompatActivity implements View.OnClickListener {

    //布局相关
    private LinearLayout lin_back;
    private TextView tv_title;
    private ListView list_ble;
    private List<BleBeen> bleList = new ArrayList<>();
    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return bleList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(BleListActivity.this).inflate(R.layout.item_ble, null);
            TextView tv_macaddr = (TextView) convertView.findViewById(R.id.tv_macaddr);
            TextView tv_ble_type = (TextView) convertView.findViewById(R.id.tv_ble_type);
            TextView tv_ble_psw = (TextView) convertView.findViewById(R.id.tv_ble_psw);
            TextView tv_edit_psw = (TextView) convertView.findViewById(R.id.tv_edit_psw);
            TextView tv_connect = (TextView) convertView.findViewById(R.id.tv_connect);
            tv_macaddr.setText(bleList.get(position).getMacAddr());
            tv_ble_type.setText(bleList.get(position).getType());
            tv_ble_psw.setText(bleList.get(position).getPsw());
            tv_edit_psw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String macAddr = bleList.get(position).getMacAddr();
                    String type = bleList.get(position).getType();
                    String psw = bleList.get(position).getPsw();
                    startActivityForResult(new Intent(BleListActivity.this, SetPswActivity.class)
                            .putExtra("macAddr", macAddr)
                            .putExtra("type", type)
                            .putExtra("psw", psw), 1001);
                }
            });
            tv_connect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String macAddr = bleList.get(position).getMacAddr();
                    String type = bleList.get(position).getType();
                    String psw = bleList.get(position).getPsw();
                    setResult(RESULT_OK, new Intent(BleListActivity.this, MainActivity.class)
                            .putExtra("macAddr", macAddr)
                            .putExtra("type", type)
                            .putExtra("psw", psw));
                    finish();
                }
            });
            return convertView;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        //初始化布局
        initView();

        getBleList();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        lin_back = (LinearLayout) findViewById(R.id.lin_back);
        tv_title = (TextView) findViewById(R.id.tv_title);
        list_ble = (ListView) findViewById(R.id.list_ble);

        lin_back.setOnClickListener(this);
        list_ble.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lin_back://历史记录
                finish();
                break;
        }
    }

    private void getBleList() {
        bleList.clear();
        SharedPreferences sp = getSharedPreferences("BleLock", Context.MODE_PRIVATE);
        String bleStr = sp.getString("ble", "");
        if (TextUtils.isEmpty(bleStr) || bleStr.split("#").length == 0) {
            return;
        } else {
            for (int i = 0; i < bleStr.split("#").length; i++) {
                String ble = bleStr.split("#")[i];
                if (ble.split("&").length == 3) {
                    BleBeen bleBeen = new BleBeen(ble.split("&")[0], ble.split("&")[1], ble.split("&")[2]);
                    bleList.add(bleBeen);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            getBleList();
        }
    }
}
