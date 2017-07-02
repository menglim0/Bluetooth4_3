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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.R.layout;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import java.util.Timer;
import java.util.TimerTask;
import org.achartengine.ChartFactory; 
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle; 
import org.achartengine.model.XYMultipleSeriesDataset; 
import org.achartengine.model.XYSeries; 
import org.achartengine.renderer.XYMultipleSeriesRenderer; 
import org.achartengine.renderer.XYSeriesRenderer;
import android.app.Activity; 
import android.content.Context;
import android.graphics.Color; 
import android.graphics.Paint.Align;
import android.os.Bundle; 
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
//import com.mt.truthblue2_1.TalkActivity;
//import com.example.circleview.MainActivity.MyTimerTask;
//import com.example.circleview.MainActivity.MyTimerTask;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private Button str_button1;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    
    private BluetoothGattCharacteristic mGattCharacteristics_Serial;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    PowerManager powerManager = null;
    WakeLock wakeLock = null;
    
    /* Draw*/
    private Timer drawTimer = new Timer();
    private TimerTask drawTask;
    private Handler Drawhandler;
    private String title = "Oil Temperature Read";
    private XYSeries series;
    private XYMultipleSeriesDataset mDataset;
    private GraphicalView chart;
    private XYMultipleSeriesRenderer renderer;
    private Context context;
    private int addX = -1, addY,gCount;
    
    int[] xv = new int[100];
    int[] yv = new int[100];
    
    int temperature=1;
    /* End of Draw*/
    
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                temperature = Integer.parseInt(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    /*
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                           int childPosition, long id) {
                              
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                            
                       
                        }
                        return true;
                    }
                    return false;
                }
    };
    */
/*
    private final OnClickListener SerialTransClickListner = new OnClickListener(){
    	public void onClick(View v){
    		switch (v.getId()) {
    		case R.id.str_button1:
    			mBluetoothLeService.setCharacteristicNotification(mGattCharacteristics_Serial,true);	
    			break;
    			
    			default:
    				break;
    		};
    		
    		
    	};
    	
    };
    */
    
    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        
        context = getApplicationContext();
      //这里获得main界面上的布局，下面会把图表画在这个布局里面
        LinearLayout layout = (LinearLayout)findViewById(R.id.lineLayout1);
        
        //这个类用来放置曲线上的所有点，是一个点的集合，根据这些点画出曲线
       series = new XYSeries(title);
        
        //创建一个数据集的实例，这个数据集将被用来创建图表
        mDataset = new XYMultipleSeriesDataset();
        
        //将点集添加到这个数据集中
        mDataset.addSeries(series);
        
        //以下都是曲线的样式和属性等等的设置，renderer相当于一个用来给图表做渲染的句柄
        int color = Color.GREEN;
        PointStyle style = PointStyle.CIRCLE;
        renderer = buildRenderer(color, style, true);
        
       //设置好图表的样式
        setChartSettings(renderer, "X", "Y", 0, 100, 0, 90, Color.WHITE, Color.WHITE);
        
        //生成图表
        chart = ChartFactory.getLineChartView(context, mDataset, renderer);
        
        //将图表添加到布局中去
        layout.addView(chart, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        //这里的Handler实例将配合下面的Timer实例，完成定时更新图表的功能
               
        Drawhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) 
        {
         //刷新图表
         updateChart();
         super.handleMessage(msg);
        }
        };
        
        drawTask = new TimerTask() {
        @Override
        public void run() {
        Message message = new Message();
            message.what = 1;
            Drawhandler.sendMessage(message);
        }
        };
        
        drawTimer.schedule(drawTask, 500, 500);
        
        
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
       ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
       // mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
       // mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
       	mDataField = (TextView) findViewById(R.id.data_value);
       	
       	/*保持屏幕常亮*/
       	powerManager = (PowerManager)this.getSystemService(this.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
        /*end of 保持屏幕常亮*/
       
        getActionBar().setTitle("读取油温");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        /*响应蓝牙服务--BluetoothLeService*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        wakeLock.acquire();// 开启屏幕常亮 
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
           
            Log.d(TAG, "C onnect request result=" + result);
        }
     // 初始化y轴数据
     
     
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        wakeLock.release();
    }

    @Override
    protected void onDestroy() {
    	 //当结束程序时关掉Timer
    	drawTimer.cancel();
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
    	
        if (data != null) {
        	//tmp=Integer.toString(data);
        	String str1 = " ";
        	String str2 = data;
        	String str3 = " ";
        	String str4 = " DegC";
        	StringBuilder builder = new StringBuilder();
        	builder.append(str1);
        	builder.append(str2);
        	builder.append(str3);
        	builder.append(str4);
            mDataField.setText(builder);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        
        
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
      
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            
            /*选择Oil Temp需要的服务ffe0 下的ffe4*/
            if (uuid.contains("0000ffe0-0000-1000-8000-00805f9b34fb")) {
            	
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            	
           // BluetoothGattCharacteristic gattCharacteristic =   
            				//	gattService.getCharacteristic(UUID.fromString(uuid)); 
            
     
       	 
           gattServiceData.add(currentServiceData);

           ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
          List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                  new ArrayList<BluetoothGattCharacteristic>();
                  

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
               charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);  
                
             // When find the UUID, connect directly 
                //mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                mGattCharacteristics_Serial = gattCharacteristic;
                mBluetoothLeService.setCharacteristicNotification(mGattCharacteristics_Serial,true);

            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
            }
            /*选择Oil Temp需要的服务ffe0 下的ffe4*/
        }


/*
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
        */
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    /**
     * 绘制指定区域
     */
    protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        
        //设置图表中曲线本身的样式，包括颜色、点的大小以及线的粗细等
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(color);
        r.setPointStyle(style);
        r.setFillPoints(fill);
        r.setLineWidth(3);
        renderer.addSeriesRenderer(r);
        
        return renderer;
       }
       
       protected void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle, String yTitle,
       double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor) {
        //有关对图表的渲染可参看api文档
        renderer.setChartTitle(title);
        renderer.setAxisTitleTextSize(25); // 坐标轴标题字体大小： 16 
        renderer.setLabelsTextSize(25); // 轴标签字体大小： 15
        renderer.setMargins( new int [] {30, 60, 25, 40}); // 图形 4 边距(上，左，右，下)
        renderer.setXTitle(xTitle);
        renderer.setYTitle(yTitle);
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
        renderer.setShowGrid(true);
        renderer.setGridColor(Color.GRAY);
        renderer.setXLabels(20);
        renderer.setYLabels(10);
        renderer.setXTitle("Time");
        renderer.setYTitle("Temperature");
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setPointSize((float) 2);
        renderer.setShowLegend(false);
       }
       
       private void updateChart() {
       
       //设置好下一个需要增加的节点
       addX = 0;
       //addY = (int)(Math.random() * 90);
       addY = temperature;
       
       
       //判断当前点集中到底有多少点，因为屏幕总共只能容纳100个，所以当点数超过100时，长度永远是100
       int length = series.getItemCount();
       if (length > 100) {
        length = 100;
       }
       gCount++;
      

       if (gCount < 100)
       {//100个点以内的，直接添加进去就行
           series.add(length+1, addY);
          
       }
       else
       {

   		 //将旧的点集中x和y的数值取出来放入backup中，并且将x的值加1，造成曲线向右平移的效果
   		 for (int i = 0; i < length-1; i++) {
   			
   		 xv[i] = (int) series.getX(i+1) -1;
   		// xv[i] =  new Date((long)series.getX(i));
   		 yv[i] = (int) series.getY(i+1);
   		 }
   		 //点集先清空，为了做成新的点集而准备
   		 series.clear();

   		 //将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
   		 //这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点
	 
   		 for (int k = 0; k < length-1; k++) {
   		     series.add(xv[k], yv[k]);
   		    }
   	
   	     series.add(length, addY);//在100点处添加新的点
       }
       
       mDataset.removeSeries(series);
    //在数据集中添加新的点集
    mDataset.addSeries(series);
    
    //视图更新，没有这一步，曲线不会呈现动态
    //如果在非UI主线程中，需要调用postInvalidate()，具体参考api
    chart.invalidate();
        }

}
