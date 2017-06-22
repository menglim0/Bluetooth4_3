/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@SuppressLint("NewApi")

//在main active后，扫描的list activity 切换到这个蓝牙配对界面
public class DeviceScanActivity extends ListActivity {
	
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
   
    private static final long SCAN_PERIOD = 10000;  // 10秒后停止查找搜索.
    UUID u1 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
   // UUID u2 = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");
    //UUID u3 = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

    final UUID[] myUUID = { u1 };
	
	
			@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        //调用系统API去打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                
              //会以Dialog样式显示一个Activity ， 我们可以在onActivityResult()方法去处理返回值
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    //处理蓝牙打开结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	System.out.println("==position=="+position);
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        
        //intent传递参数Device adress和device name到DeviceActive界面；
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        //选择Device后，跳转到DeviceControlActivity 页面
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
    	
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
           // UUID[0]=ParcelUuid.fromString("00000000-0000-0000-0000-000000000000");
          
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
       /*
        if(mBluetoothAdapter == null) {
            return;
        }
    
        /*
        为什么不能再使用单例的BluetoothAdapter? 原因如下:
        bluetoothAdapter.startLeScan() //deprecated
        http://stackoverflow.com/questions/30223071/startlescan-replacement-to-current-api
        Remember that the method: public BluetoothLeScanner getBluetoothLeScanner () isn't static.
        If you do: BluetoothAdapter.getBluetoothLeScanner()
        you will get an error, since getDefaultAdapter() is a static method, but getBluetoothLeScanner() isn't.
        You need an instance of a BluetoothAdapter.
         
        final BluetoothLeScanner scanner = BluetoothAdapter.getBluetoothLeScanner();
        if(enable) {
            //scan分为2类,而在android L之前,搜索条件只有uuid
            //(1)直接搜索全部周围peripheral(外围的)设备,搜索结果将通过这个callback返回
            scanner.startScan(mLeScanCallback);
            //(2)根据过滤条件搜索设备
            final List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
            //uuid格式8-4-4-4-12(32位,128bit)
            //address格式(12位,48bit)
            scanFilters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00000000-0000-0000-0000-000000000000")).setDeviceAddress("00:00:00:00:00:00").build());
            ScanSettings scanSettings = new ScanSettings.Builder()
                    //require API 23
                    //.setCallbackType(0).setMatchMode(0).setNumOfMatches(0)
                    .setReportDelay(0).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE).build();
            scanner.startScan(scanFilters, scanSettings, scanCallback);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);
        } else {
            scanner.stopScan(scanCallback);
        }*/
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
        
//显示蓝牙设备名称和地址
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

    	/*扫描蓝牙设备，如果扫描指定的蓝牙设备，可以使用startLeScan()*/
    	
        
        @Override
           public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
      
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}