package com.wanwan.blelock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.mt.sdk.ble.MTBLEManager;
import com.wanwan.blelock.help.GlobalVariable;
import com.wanwan.blelock.model.MixedValues;
import com.wanwan.blelock.model.UuidAndName;
import com.wanwan.blelock.net.RequestWithToken;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

public class MTTBleToolsApplication extends Application {

	/*截止日期*/
	private long closingDate = 1508500833L*1000L;
	@Override
	public void onCreate() {
		super.onCreate();

		initRule();

		getDatas();

		initInternet();

		initBLE();


	}

	/**
	* 查看权限
	* */
	private void initRule() {
		SharedPreferences sp=getSharedPreferences("BleLock", Context.MODE_PRIVATE);
		long curTime = System.currentTimeMillis();
		if(curTime>closingDate){
			exit(0);
		}
	}

	// 初始化数据
	private GlobalVariable mGlobalVariable;

	private void getDatas() {
		mGlobalVariable = GlobalVariable.getinstance();

		x.Ext.init(this);
		DbManager db = x.getDb(new DbManager.DaoConfig().setDbName("mtcarp.db").setDbVersion(1)
				.setDbOpenListener(new DbManager.DbOpenListener() {
					@Override
					public void onDbOpened(DbManager db) {
						db.getDatabase().enableWriteAheadLogging();
					}
				}).setDbUpgradeListener(new DbManager.DbUpgradeListener() {
					@Override
					public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
					}
				}));
		mGlobalVariable.setDb(db);

		try {
			List<UuidAndName> uuids = mGlobalVariable.getDb().findAll(UuidAndName.class);
			
			if(uuids == null){
				uuids = new ArrayList<UuidAndName>();
				uuids.add(new UuidAndName("微信FD", "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"));
				uuids.add(new UuidAndName("微信AB", "AB8190D5-D11E-4941-ACC4-42F30510B408"));
				mGlobalVariable.getDb().save(uuids);
				
				uuids = mGlobalVariable.getDb().findAll(UuidAndName.class);
			}
			mGlobalVariable.setUuids(uuids);
			
			MixedValues mixedvalues = mGlobalVariable.getDb().findFirst(MixedValues.class);
			if(mixedvalues == null){
				System.out.println("mixedvalues == null");
				mixedvalues = new MixedValues();
				mGlobalVariable.getDb().save(mixedvalues);
				mixedvalues = mGlobalVariable.getDb().findFirst(MixedValues.class);
			}else{
				System.out.println("mixedvalues != null");
			}
			mGlobalVariable.setMixedvalues(mixedvalues);
			
		} catch (DbException e) {
			e.printStackTrace();
			System.out.println("DbException->"+e.toString());
		}
	}

	// 初始化网络
	private void initInternet() {
		mGlobalVariable.setmRequestWithToken(RequestWithToken.getInstance(getApplicationContext()));
	}

	// 初始化蓝牙
	private void initBLE() {
		mGlobalVariable.setBleManger(MTBLEManager.getInstance(getApplicationContext()));
	}

}
