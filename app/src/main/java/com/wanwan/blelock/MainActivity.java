package com.wanwan.blelock;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mt.sdk.ble.MTBLEDevice;
import com.mt.sdk.ble.MTBLEManager;
import com.mt.sdk.ble.MTBLEManager.MTScanCallback;
import com.mt.sdk.ble.base.MTSeriaBase;
import com.mt.sdk.ble.model.BLEBaseAction.Option;
import com.mt.sdk.ble.model.ErroCode;
import com.mt.sdk.ble.model.WriteCharactAction;
import com.mt.sdk.tools.MTTools;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.wanwan.blelock.been.BleBeen;
import com.wanwan.blelock.been.BleMsg;
import com.wanwan.blelock.help.GlobalVariable;
import com.wanwan.blelock.utils.CheckPermissionUtils;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {
    //扫描跳转Activity RequestCode
    public static final int REQUEST_CODE = 111;
    /*设置密码*/
    public static final int REQUEST_SETPSW = 120;
    /*历史记录*/
    public static final int REQUEST_HISTORY = 121;
    /**
     * 请求CAMERA权限码
     */
    public static final int REQUEST_CAMERA_PERM = 101;
    private static boolean connecting = false;
    //布局相关
    private LinearLayout lin_history, lin_scan, lin_ble_state;
    private TextView tv_title, tv_ble_state, tv_ble_num, tv_ble_addr;
    private ImageView iv_ble_state, iv_lock, iv_unlock;
    //扫描相关
    private MTBLEManager manger;
    private GlobalVariable mGlobalVariable;
    //蓝牙链接
    private MTSeriaBase mBle;
    private String devNumStr = "已发现*个可用设备";
    private ProgressDialog mDialog;
    private static final int intervalTime = 300;
    /**
     * 蓝牙扫描回调
     */
    private MTScanCallback scanCallback = new MTScanCallback() {

        @Override
        public void onScanFail(int errocode, String erromsg) {

        }

        @Override
        public void onScan(MTBLEDevice device) {

            for (com.mt.sdk.ble.MTBLEDevice MTBLEDevice : mGlobalVariable.getDeviceList()) {
                if (MTBLEDevice.getDevice().getAddress().equals(device.getDevice().getAddress())) {
                    MTBLEDevice.reflashInf(device);
                    return;
                }
            }
            mGlobalVariable.getDeviceList().add(device);
            refreshDevNum();
        }
    };
    /**
     * 连接中，刷新UI
     */
    private Handler BleConnectingHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1 && connecting) {//连接进行中
                iv_ble_state.setSelected(msg.arg1 == 1 ? true : false);
                Message msg2 = new Message();
                msg2.what = 1;
                msg2.arg1 = msg.arg1 == 1 ? 0 : 1;
                BleConnectingHandle.sendMessageDelayed(msg2, 300);
            } else {//连接已结束

            }
        }
    };
    /**
     * 连接回调
     */
    private MTSeriaBase.CallBack callback = new MTSeriaBase.CallBack() {

        @Override
        public void onConnect(ErroCode errocode) {
            if ((mDialog != null) && (mDialog.isShowing())) {
                mDialog.dismiss();
            }

            if (errocode.getCode() == 0) {//连接成功
                refreshConnectState(1);
                return;
            } else {// 连接失败
                refreshConnectState(5);
            }
        }

        @Override
        public void reTryConnect(int lasttimes) {
            if ((mDialog != null) && (mDialog.isShowing())) {
                mDialog.setTitle("正在重连->" + lasttimes);
            }
        }

        @Override
        public void onDisConnect(ErroCode errocode) {
            Toast.makeText(getApplicationContext(), "断开连接", Toast.LENGTH_SHORT).show();
            refreshConnectState(3);
        }

        @Override
        public void onManualDisConnect(ErroCode errocode) {
            Toast.makeText(getApplicationContext(), "断开连接", Toast.LENGTH_SHORT).show();
            refreshConnectState(3);
        }

        @Override
        public void onDatasRecive(BluetoothGattCharacteristic rxd_charact, byte[] datas) {
            Toast.makeText(getApplicationContext(), "收到数据：" + datas.toString(), Toast.LENGTH_SHORT).show();
        }
    };
    // 发送的数据Handle
    private Handler sendMsgHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            BleMsg sendmsg = getSendMsg(msg.what);
            onSendMsg(sendmsg);
            Log.e("sendMsgHandle", sendmsg.getDismsg());
            if (msg.arg1 > 0) {
                Message msg2 = new Message();
                msg2.arg1 = msg.arg1 - 1;
                msg2.what = msg.what;
                sendMsgHandle.sendMessageDelayed(msg2, intervalTime);
            } else {

            }
        }
    };
    /**
     * 开始连接
     */
    private String curAddr = "";
    private Handler scanhandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化布局
        initView();
        //初始化扫描
        initScan();
        //初始化蓝牙连接
        initBleConnect();
        //初始化权限
        initPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        manger.startScan(scanCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manger.stopScan();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        lin_history = (LinearLayout) findViewById(R.id.lin_history);
        lin_scan = (LinearLayout) findViewById(R.id.lin_scan);
        lin_ble_state = (LinearLayout) findViewById(R.id.lin_ble_state);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_ble_state = (TextView) findViewById(R.id.tv_ble_state);
        tv_ble_num = (TextView) findViewById(R.id.tv_ble_num);
        tv_ble_addr = (TextView) findViewById(R.id.tv_ble_addr);
        iv_ble_state = (ImageView) findViewById(R.id.iv_ble_state);
        iv_lock = (ImageView) findViewById(R.id.iv_lock);
        iv_unlock = (ImageView) findViewById(R.id.iv_unlock);

        lin_history.setOnClickListener(this);
        lin_scan.setOnClickListener(this);
        iv_lock.setOnClickListener(this);
        iv_unlock.setOnClickListener(this);
        tv_ble_num.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.lin_history://历史记录
                startActivityForResult(new Intent(MainActivity.this, BleListActivity.class), REQUEST_HISTORY);
                break;
            case R.id.lin_scan://扫描
                intent = new Intent(MainActivity.this, QrScanActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.tv_ble_num://蓝牙扫描
                if (!manger.isScaning()) {
                    manger.startScan(scanCallback);
                    Toast.makeText(getApplicationContext(), "扫描中。。。", Toast.LENGTH_SHORT).show();
                } else {
                    manger.stopScan();
                    Toast.makeText(getApplicationContext(), "已停止扫描", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.iv_lock://上锁
                if (mBle.isConnected()) {
                    Message msg = new Message();
                    msg.what = 1;//数据类型
                    msg.arg1 = 2;//发送次数
                    sendMsgHandle.sendMessage(msg);
                } else {
                    Toast.makeText(getApplicationContext(), "未连接蓝牙设备", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.iv_unlock://解锁
                if (mBle.isConnected()) {
                    Message msg2 = new Message();
                    msg2.what = 2;//数据类型
                    msg2.arg1 = 2;//发送次数
                    sendMsgHandle.sendMessage(msg2);
                } else {
                    Toast.makeText(getApplicationContext(), "未连接蓝牙设备", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    /**
     * 初始化蓝牙扫描
     */
    private void initScan() {
        mGlobalVariable = GlobalVariable.getinstance();
        manger = mGlobalVariable.getBleManger();
        Log.e("initScan", mGlobalVariable.getDeviceList().size() + "");
        refreshDevNum();
        if (!this.manger.isEnable()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
        }
    }

    /**
     * 刷新蓝牙扫描
     */
    private void refreshDevNum() {
        if (mGlobalVariable != null) {
            String str = devNumStr.replace("*", mGlobalVariable.getDeviceList().size() + "");
            tv_ble_num.setText(str);
        }
    }

    /**
     * 初始化权限事件
     */
    private void initPermission() {
        //检查权限
        String[] permissions = CheckPermissionUtils.checkPermission(this);
        if (permissions.length == 0) {
            //权限都申请了
        } else {
            //申请权限
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * 处理二维码扫描结果
         */
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE) {
                //处理扫描结果（在界面上显示）
                if (null != data) {
                    Bundle bundle = data.getExtras();
                    if (bundle != null) {
                        if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                            String result = bundle.getString(CodeUtils.RESULT_STRING);
                            if (!TextUtils.isEmpty(result) && result.split("\"").length > 4) {
                                String address = result.split("\"")[3];
                                checkBle(address);
                                Toast.makeText(this, "macAddr:" + address, Toast.LENGTH_LONG).show();
                            }
                        } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                            Toast.makeText(MainActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else if (requestCode == REQUEST_CAMERA_PERM) {
                Toast.makeText(this, "从设置页面返回...", Toast.LENGTH_SHORT)
                        .show();
            } else if (requestCode == REQUEST_SETPSW) {
                String address = data.getStringExtra("macAddr");
                startConnect(address);
            }
//            else if (requestCode == REQUEST_SETPSW) {
//                String address = data.getStringExtra("macAddr");
//                startConnect(address);
//            }
        }
    }

    /**
     * 初始化蓝牙连接
     */
    private void initBleConnect() {
        mBle = new MTSeriaBase(getApplicationContext(), manger);
    }

    /**
     * 检查是否可以扫描到
     */
    private void checkBle(String addr) {
        String address = addr.toUpperCase();
        for (com.mt.sdk.ble.MTBLEDevice MTBLEDevice : mGlobalVariable.getDeviceList()) {
            if (MTBLEDevice.getDevice().getAddress().equals(addr) || MTBLEDevice.getDevice().getAddress().equals(address)) {
                String type = MTBLEDevice.getDevicetype().name();
//                startActivityForResult(new Intent(MainActivity.this, SetPswActivity.class).putExtra("macAddr", address).putExtra("type", type), REQUEST_SETPSW);
                if ("MTBeacon1".equals(type) || "MTBeacon2".equals(type) || "MTBeacon3".equals(type) || "MTBeacon4".equals(type)) {
                    startActivityForResult(new Intent(MainActivity.this, SetPswActivity.class).putExtra("macAddr", address).putExtra("type", type), REQUEST_SETPSW);
                } else {
                    saveBle(address, type, "888888");
                    startConnect(address);
                }
                return;
            }
        }
        Toast.makeText(getApplicationContext(), "当前设备不在附近，或未通电", Toast.LENGTH_LONG).show();
    }

    private void startConnect(String addr) {
        String address = addr.toUpperCase();
        tv_ble_addr.setText("当前蓝牙：" + address);
        mDialog = ProgressDialog.show(MainActivity.this, "正在连接", "正在连接", true, true, new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mBle.disConnect();
                refreshConnectState(0);
            }
        });
        manger = mGlobalVariable.getBleManger();
        mBle = new MTSeriaBase(getApplicationContext(), manger);
        ErroCode result = mBle.connect(address, 1, false, callback);
        if (result.getCode() != 0) { // 连接失败
            refreshConnectState(4);
            if ((mDialog != null) && (mDialog.isShowing())) {
                mDialog.dismiss();
            }
        } else {//正在连接
            refreshConnectState(2);
        }
    }

    // 获取要发送的数据
    private BleMsg getSendMsg(int type) {
        String sendMsg = type == 1 ? "AA0001010000BB" : "AA0001020000BB";
        BleMsg msg = new BleMsg();
        msg.setDir(BleMsg.DIR.SEND);
        byte[] realdatas = MTTools.convertStringToByteArray(sendMsg);
        msg.setRealmsg(realdatas);
        msg.setDismsg(sendMsg);
        return msg;
    }

    public void onSendMsg(BleMsg msg) {
        if (!mBle.isConnected()) {
            refreshConnectState(0);
            return;
        } else {
            refreshConnectState(1);
            mBle.addWriteDatasAction(new WriteCharactAction(null, msg.getRealmsg(), new Option(1000)));
        }

    }


    //以下为权限申请

    /**
     * 更新链接状态
     *
     * @param state 0:未连接，1：已连接，2：连接中，3：断开连接，4：设备不存在,5：连接失败
     */
    private void refreshConnectState(int state) {

        if (state == 0) {
            tv_ble_state.setText("未连接");
            iv_ble_state.setSelected(false);
//            iv_lock.setEnabled(false);
//            iv_unlock.setEnabled(false);
            tv_ble_addr.setVisibility(View.INVISIBLE);
            connecting = false;
        } else if (state == 1) {
            tv_ble_state.setText("已连接");
            iv_ble_state.setSelected(true);
//            iv_lock.setEnabled(true);
//            iv_unlock.setEnabled(true);
            tv_ble_addr.setVisibility(View.VISIBLE);
            connecting = false;
        } else if (state == 2) {
            tv_ble_state.setText("连接中");
//            iv_lock.setEnabled(false);
//            iv_unlock.setEnabled(false);
            tv_ble_addr.setVisibility(View.VISIBLE);
            connecting = true;
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = 1;
            BleConnectingHandle.sendMessage(msg);
        } else if (state == 3) {
            tv_ble_state.setText("断开连接");
            iv_ble_state.setSelected(false);
//            iv_lock.setEnabled(false);
//            iv_unlock.setEnabled(false);
            tv_ble_addr.setVisibility(View.VISIBLE);
            connecting = false;
        } else if (state == 4) {
            tv_ble_state.setText("设备不存在");
            iv_ble_state.setSelected(false);
//            iv_lock.setEnabled(false);
//            iv_unlock.setEnabled(false);
            tv_ble_addr.setVisibility(View.VISIBLE);
            connecting = false;
        }else if (state == 5) {
            tv_ble_state.setText("连接失败");
            iv_ble_state.setSelected(false);
//            iv_lock.setEnabled(false);
//            iv_unlock.setEnabled(false);
            tv_ble_addr.setVisibility(View.VISIBLE);
            connecting = false;
        }
    }

    /**
     * 保存蓝牙设备到本地
     */
    private void saveBle(String macAddr, String type, String psw) {
        SharedPreferences sp = getSharedPreferences("BleLock", Context.MODE_PRIVATE);
        String bleStr = sp.getString("ble", "");
        List<BleBeen> bleList = getBleList(bleStr, macAddr);
        BleBeen bleBeen = new BleBeen(macAddr, type, psw);
        bleList.add(bleBeen);

        String bleListStr = "";
        for (int i = 0; i < bleList.size(); i++) {
            bleListStr = bleList.get(i).getMacAddr() + "&" + bleList.get(i).getType() + "&" + bleList.get(i).getPsw() + "#" + bleListStr;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("ble", bleListStr);
        editor.commit();
    }

    /**
     * 获取本地保存的蓝牙列表
     */
    private List<BleBeen> getBleList(String bleStr, String addr) {
        List<BleBeen> bleList = new ArrayList<>();
        if (TextUtils.isEmpty(bleStr) || bleStr.split("#").length == 0) {
            return bleList;
        } else {
            for (int i = 0; i < bleStr.split("#").length; i++) {
                String ble = bleStr.split("#")[i];
                if (ble.split("&").length == 3 && !addr.equals(ble.split("&")[0])) {
                    BleBeen bleBeen = new BleBeen(ble.split("&")[0], ble.split("&")[1], ble.split("&")[2]);
                    bleList.add(bleBeen);
                }
            }
            return bleList;
        }
    }

    /**
     * 获取本地保存的指定蓝牙
     */
    private BleBeen getBle(String addr) {
        SharedPreferences sp = getSharedPreferences("BleLock", Context.MODE_PRIVATE);
        String bleStr = sp.getString("ble", "");
        if (TextUtils.isEmpty(bleStr) || bleStr.split("#").length == 0) {
            return null;
        } else {
            for (int i = 0; i < bleStr.split("#").length; i++) {
                String ble = bleStr.split("#")[i];
                if (ble.split("&").length == 3 && ble.split("&")[0].equals(addr)) {
                    BleBeen bleBeen = new BleBeen(ble.split("&")[0], ble.split("&")[1], ble.split("&")[2]);
                    return bleBeen;
                }
            }
            return null;
        }
    }

    /**
     * EsayPermissions接管权限处理逻辑
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Toast.makeText(this, "执行onPermissionsGranted()...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "执行onPermissionsDenied()...", Toast.LENGTH_SHORT).show();
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, "当前App需要申请camera权限,需要打开设置页面么?")
                    .setTitle("权限申请")
                    .setPositiveButton("确认")
                    .setNegativeButton("取消", null /* click listener */)
                    .setRequestCode(REQUEST_CAMERA_PERM)
                    .build()
                    .show();
        }
    }


}
