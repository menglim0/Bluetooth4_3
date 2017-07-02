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
      //������main�����ϵĲ��֣�������ͼ���������������
        LinearLayout layout = (LinearLayout)findViewById(R.id.lineLayout1);
        
        //������������������ϵ����е㣬��һ����ļ��ϣ�������Щ�㻭������
       series = new XYSeries(title);
        
        //����һ�����ݼ���ʵ����������ݼ�������������ͼ��
        mDataset = new XYMultipleSeriesDataset();
        
        //���㼯��ӵ�������ݼ���
        mDataset.addSeries(series);
        
        //���¶������ߵ���ʽ�����Եȵȵ����ã�renderer�൱��һ��������ͼ������Ⱦ�ľ��
        int color = Color.GREEN;
        PointStyle style = PointStyle.CIRCLE;
        renderer = buildRenderer(color, style, true);
        
       //���ú�ͼ�����ʽ
        setChartSettings(renderer, "X", "Y", 0, 100, 0, 90, Color.WHITE, Color.WHITE);
        
        //����ͼ��
        chart = ChartFactory.getLineChartView(context, mDataset, renderer);
        
        //��ͼ����ӵ�������ȥ
        layout.addView(chart, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        //�����Handlerʵ������������Timerʵ������ɶ�ʱ����ͼ��Ĺ���
               
        Drawhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) 
        {
         //ˢ��ͼ��
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
       	
       	/*������Ļ����*/
       	powerManager = (PowerManager)this.getSystemService(this.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
        /*end of ������Ļ����*/
       
        getActionBar().setTitle("��ȡ����");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        /*��Ӧ��������--BluetoothLeService*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        wakeLock.acquire();// ������Ļ���� 
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
           
            Log.d(TAG, "C onnect request result=" + result);
        }
     // ��ʼ��y������
     
     
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        wakeLock.release();
    }

    @Override
    protected void onDestroy() {
    	 //����������ʱ�ص�Timer
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
            
            /*ѡ��Oil Temp��Ҫ�ķ���ffe0 �µ�ffe4*/
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
            /*ѡ��Oil Temp��Ҫ�ķ���ffe0 �µ�ffe4*/
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
     * ����ָ������
     */
    protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        
        //����ͼ�������߱������ʽ��������ɫ����Ĵ�С�Լ��ߵĴ�ϸ��
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
        //�йض�ͼ�����Ⱦ�ɲο�api�ĵ�
        renderer.setChartTitle(title);
        renderer.setAxisTitleTextSize(25); // ��������������С�� 16 
        renderer.setLabelsTextSize(25); // ���ǩ�����С�� 15
        renderer.setMargins( new int [] {30, 60, 25, 40}); // ͼ�� 4 �߾�(�ϣ����ң���)
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
       
       //���ú���һ����Ҫ���ӵĽڵ�
       addX = 0;
       //addY = (int)(Math.random() * 90);
       addY = temperature;
       
       
       //�жϵ�ǰ�㼯�е����ж��ٵ㣬��Ϊ��Ļ�ܹ�ֻ������100�������Ե���������100ʱ��������Զ��100
       int length = series.getItemCount();
       if (length > 100) {
        length = 100;
       }
       gCount++;
      

       if (gCount < 100)
       {//100�������ڵģ�ֱ����ӽ�ȥ����
           series.add(length+1, addY);
          
       }
       else
       {

   		 //���ɵĵ㼯��x��y����ֵȡ��������backup�У����ҽ�x��ֵ��1�������������ƽ�Ƶ�Ч��
   		 for (int i = 0; i < length-1; i++) {
   			
   		 xv[i] = (int) series.getX(i+1) -1;
   		// xv[i] =  new Date((long)series.getX(i));
   		 yv[i] = (int) series.getY(i+1);
   		 }
   		 //�㼯����գ�Ϊ�������µĵ㼯��׼��
   		 series.clear();

   		 //���²����ĵ����ȼ��뵽�㼯�У�Ȼ����ѭ�����н�����任���һϵ�е㶼���¼��뵽�㼯��
   		 //�����������һ�°�˳��ߵ�������ʲôЧ������������ѭ���壬������²����ĵ�
	 
   		 for (int k = 0; k < length-1; k++) {
   		     series.add(xv[k], yv[k]);
   		    }
   	
   	     series.add(length, addY);//��100�㴦����µĵ�
       }
       
       mDataset.removeSeries(series);
    //�����ݼ�������µĵ㼯
    mDataset.addSeries(series);
    
    //��ͼ���£�û����һ�������߲�����ֶ�̬
    //����ڷ�UI���߳��У���Ҫ����postInvalidate()������ο�api
    chart.invalidate();
        }

}
