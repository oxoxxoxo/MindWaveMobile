package com.neurosky.mindwavemobile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import java.net.URI;
import java.lang.Object;

/**
 * Created by Chirag on 15-09-2017.
 */
public class RecordAttentionImg extends Activity{
    private static final String TG2 = RecordAttention2.class.getSimpleName();
    private TgStreamReader tgStreamReader;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private String address = null;

    private TextView tv_ps = null;
    private TextView tv_attention = null;
    //    private TextView tv_meditation = null;
    public TextView at_mn = null;

    private Button btn_start = null;
    private Button btn_stop = null;
    private Button btn_selectdevice = null;
    //private LinearLayout wave_layout;
    ArrayList<Integer> rawData = new ArrayList<>();
    ArrayList<Integer> atVal = new ArrayList<>();

    final String abc = null;
    String line = null;

    Double dmean = 0.0;

    private static int RESULT_LOAD_IMAGE = 1;

    private Context mContext;

    private int currentState = 0;
    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TG2, "connectionStates change to: " + connectionStates);
            currentState  = connectionStates;
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTED:
                    //sensor.start();
                    showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    //byte[] cmd = new byte[1];
                    //cmd[0] = 's';
                    //tgStreamReader.sendCommandtoDevice(cmd);
                    LinkDetectedHandler.sendEmptyMessageDelayed(1234, 5000);
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    //get data time out
                    break;
                case ConnectionStates.STATE_COMPLETE:
                    //read file complete
                    break;
                case ConnectionStates.STATE_STOPPED:
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    break;
                case ConnectionStates.STATE_ERROR:
                    Log.d(TG2,"Connect error, Please try again!");
                    break;
                case ConnectionStates.STATE_FAILED:
                    Log.d(TG2,"Connect failed, Please try again!");
                    break;
            }

            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onRecordFail(int a) {
            // TODO Auto-generated method stub
            Log.e(TG2,"onRecordFail: " +a);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // TODO Auto-generated method stub

            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // TODO Auto-generated method stub
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = datatype;
            msg.arg1 = data;
            msg.obj = obj;
            LinkDetectedHandler.sendMessage(msg);
            //Log.i(TAG,"onDataReceived");
        }

    };

    private boolean isPressing = false;
    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private static final int MSG_CONNECT = 1003;
    private boolean isReadFilter = false;

    int raw;

    private int badPacketCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.attention_record_img_view);

        initView();

        try {
            // TODO
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Toast.makeText(
                        this,
                        "Please enable your Bluetooth and again click this button!",
                        Toast.LENGTH_LONG).show();
                finish();
//				return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TG2, "error:" + e.getMessage());
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            ImageView imageView = (ImageView) findViewById(R.id.imgv);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
        }
    }

    // function to initialise view
    public void initView() {
        tv_ps = (TextView) findViewById(R.id.tv_ps);
        tv_attention = (TextView) findViewById(R.id.tv_attention);
//        tv_meditation = (TextView) findViewById(R.id.tv_meditation);

        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        //wave_layout = (LinearLayout) findViewById(R.id.wave_layout);

        Button imgsel = (Button) findViewById(R.id.imgsel);
        imgsel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        ImageView iv = (ImageView) findViewById(R.id.imgv);


        btn_start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                badPacketCount = 0;
                showToast("connecting ...", Toast.LENGTH_SHORT);
                start();
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (tgStreamReader != null) {
                    tgStreamReader.stop();

                    try {
                        SimpleDateFormat sDate = new SimpleDateFormat("ddMMyyyyhhmmss");
                        String date = sDate.format(new Date());

                        File atValFile = new File("/sdcard/AtVal " + date + ".txt");
                        atValFile.createNewFile();
                        FileOutputStream fOut2 = new FileOutputStream(atValFile);
                        OutputStreamWriter myOutWriter2 = new OutputStreamWriter(fOut2);
                        for (int i = 0; i < atVal.size(); i++) {
                            myOutWriter2.append(Integer.toString(atVal.get(i)) + " ");
                        }
                        myOutWriter2.close();
                        fOut2.close();

                        File compressedFile = new File("/sdcard/C_" + date);
                        compressedFile.createNewFile();
                        HuffmanCompression huffmanCompression = new HuffmanCompression();
                        huffmanCompression.encode(atValFile, compressedFile);


                        Toast.makeText(getBaseContext(), "Done Writing To File", Toast.LENGTH_SHORT).show();

                        setContentView(R.layout.attention_graph);

                        File myFile2 = new File("/sdcard/AtVal " + date + ".txt");
                        try {
                            FileReader fr = new FileReader(myFile2);
                            BufferedReader br = new BufferedReader(fr);

                            StringBuilder sbr = new StringBuilder();
                            try {
                                while ((line = br.readLine()) != null){
                                    sbr.append(line);
                                    line = sbr.toString();
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        setUpDrawWaveView2();


                        String s = "Mean Attention Value : " + dmean.toString();
                        at_mn.setText(s);

                    } catch (Exception e) {
                        Log.v("Write Byte ", e.getMessage());
                        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

        });

        btn_selectdevice =  (Button) findViewById(R.id.btn_selectdevice);

        btn_selectdevice.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                scanDevice();
            }

        });
    }

    private void start(){
        if(address != null){
            BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
            createStreamReader(bd);

            tgStreamReader.connectAndStart();
        }else{
            showToast("Please select device first!", Toast.LENGTH_SHORT);
        }
    }

    public void stop() { //update file here

        if(tgStreamReader != null){

            tgStreamReader.stop();
            tgStreamReader.close();//if there is not stop cmd, please call close() or the data will accumulate
            tgStreamReader = null;

        }
    }

    // function to initialise graph
    public void setUpDrawWaveView2() {

        GraphView graph = (GraphView) findViewById(R.id.graph);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxY(100);

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(generateData(line));
        series.setColor(Color.BLACK);
        graph.addSeries(series);
    }

    public DataPoint[] generateData(String xyz) {
        List<Integer> l1 = getRawData(xyz);
        int sz = l1.size();
        DataPoint[] values = new DataPoint[sz];
        for (int i=0; i<sz; i++) {
            double x = i;
            double y = l1.get(i);
            dmean = dmean + y;
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        dmean = dmean / sz;
        return values;

    }

    public List<Integer> getRawData(String str) {
        Scanner scanner = new Scanner(str);
        List<Integer> list = new ArrayList<Integer>();
        while (scanner.hasNextInt()) {
            list.add(scanner.nextInt());
        }
        return list;
    }

    // function to draw graph as per new data
//    public void updateWaveView(int data) {
//        if (waveView != null) {
//            waveView.updateData(data);
//        }
//    }

    public void setScreenBrightness(int brightnessValue){
        if(brightnessValue >= 0 && brightnessValue <= 255){
           Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
            );
        }
    }

    int ind = 0, mn, cnt=0;
    // this function is used to process data read from the device
    private Handler LinkDetectedHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1234:
                    tgStreamReader.MWM15_getFilterType();
                    isReadFilter = true;
                    Log.d(TG2,"MWM15_getFilterType ");

                    break;
                case 1235:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_60HZ);
                    Log.d(TG2,"MWM15_setFilter  60HZ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1236:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_50HZ);
                    Log.d(TG2,"MWM15_SetFilter 50HZ ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;

                case 1237:
                    tgStreamReader.MWM15_getFilterType();
                    Log.d(TG2,"MWM15_getFilterType ");
                    break;

                case MindDataType.CODE_FILTER_TYPE:
                    Log.d(TG2," CODE_FILTER_TYPE : " + msg.arg1 + " isReadFilter: " + isReadFilter);
                    if(isReadFilter){
                        isReadFilter = false;
                        if(msg.arg1 == MindDataType.FilterType.FILTER_50HZ.getValue()){
                            LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
                        }else if(msg.arg1 == MindDataType.FilterType.FILTER_60HZ.getValue()){
                            LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
                        }else{
                            Log.e(TG2,"Error filter type");
                        }
                    }
                    break;

                case MindDataType.CODE_RAW:
//                    updateWaveView(msg.arg1);
//                    rawData.add(msg.arg1);
//                        myOutWriter.append(Integer.toString(msg.arg1)+"\n");
                    break;
                case MindDataType.CODE_MEDITATION:
//                    Log.d(TG2, "HeadDataType.CODE_MEDITATION " + msg.arg1);
//                    tv_meditation.setText("" +msg.arg1 );
                    break;
                case MindDataType.CODE_ATTENTION:
                    Log.d(TG2, "CODE_ATTENTION " + msg.arg1);
                    tv_attention.setText("" + msg.arg1);
                    atVal.add(msg.arg1);
                    int br = (int)((msg.arg1)*2.5);
                    setScreenBrightness(br);
//                    atfunc();
                    break;
                case MindDataType.CODE_EEGPOWER:
//                    EEGPower power = (EEGPower)msg.obj;
                    break;
                case MindDataType.CODE_POOR_SIGNAL://
                    int poorSignal = msg.arg1;
                    Log.d(TG2, "poorSignal:" + poorSignal);
                    String abc;
                    abc = "" + msg.arg1;
                    tv_ps.setText(abc);
                    break;
                case MSG_UPDATE_BAD_PACKET:
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
//    public void atfunc() {
//        ind++;
//        TextView tv = (TextView) findViewById(R.id.paraText);
//        cnt=2;
//        if(ind>3) {
//            while(cnt>0) {
//                mn += atVal.get(ind-cnt);
//                cnt--;
//            }
//            mn = mn/2;
//            if(mn>80)
//                tv.setTextColor(getResources().getColor(R.color.firstcolor, null));
//            else if(mn>60)
//                tv.setTextColor(getResources().getColor(R.color.secondcolor, null));
//            else if(mn>40)
//                tv.setTextColor(getResources().getColor(R.color.thirdcolor, null));
//            else if(mn>0)
//                tv.setTextColor(getResources().getColor(R.color.fourthcolor, null));
//        }
//    }


    public void showToast(final String msg,final int timeStyle){
        RecordAttentionImg.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }

    //show device list while scanning
    private ListView list_select;
    private BTDeviceListAdapter deviceListApapter = null;
    private Dialog selectDialog;

    // Getting Bluetooth device dynamically
    public void scanDevice(){

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        setUpDeviceListView();
        //register the receiver for scanning
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        mBluetoothAdapter.startDiscovery();
    }

    private void setUpDeviceListView(){

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_select_device, null);
        list_select = (ListView) view.findViewById(R.id.list_select);
        selectDialog = new Dialog(this, R.style.dialog1);
        selectDialog.setContentView(view);
        //List device dialog

        deviceListApapter = new BTDeviceListAdapter(this);
        list_select.setAdapter(deviceListApapter);
        list_select.setOnItemClickListener(selectDeviceItemClickListener);

        selectDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){

            @Override
            public void onCancel(DialogInterface arg0) {
                // TODO Auto-generated method stub
                Log.e(TG2,"onCancel called!");
                RecordAttentionImg.this.unregisterReceiver(mReceiver);
            }

        });

        selectDialog.show();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device: pairedDevices){
            deviceListApapter.addDevice(device);
        }
        deviceListApapter.notifyDataSetChanged();
    }

    //Select device operation
    private AdapterView.OnItemClickListener selectDeviceItemClickListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1,int arg2, long arg3) {
            Log.d(TG2, "Rico ####  list_select onItemClick     ");
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            //unregister receiver
            RecordAttentionImg.this.unregisterReceiver(mReceiver);

            mBluetoothDevice =deviceListApapter.getDevice(arg2);
            selectDialog.dismiss();
            selectDialog = null;

            Log.d(TG2,"onItemClick name: "+mBluetoothDevice.getName() + " , address: " + mBluetoothDevice.getAddress() );
            address = mBluetoothDevice.getAddress().toString();

            //ger remote device
            BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress().toString());

            tgStreamReader = createStreamReader(remoteDevice);
            tgStreamReader.connectAndStart();

        }

    };

    // If the TgStreamReader is created, just change the bluetooth else create TgStreamReader, set data receiver, TgStreamHandler and parser and return TgStreamReader
    public TgStreamReader createStreamReader(BluetoothDevice bd){

        if(tgStreamReader == null){
            // Example of constructor public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
            tgStreamReader = new TgStreamReader(bd,callback);
            tgStreamReader.startLog();
        }else{
            //If bluetooth deivce changed changeBluetoothDevice
            tgStreamReader.changeBluetoothDevice(bd);
            tgStreamReader.setTgStreamHandler(callback);
        }
        return tgStreamReader;
    }

    // this function bind device to app
    public void bindToDevice(BluetoothDevice bd){
        int ispaired = 0;
        if(bd.getBondState() != BluetoothDevice.BOND_BONDED){
            //ispaired = remoteDevice.createBond();
            try {
                //Set pin
                if(Utils.autoBond(bd.getClass(), bd, "0000")){
                    ispaired += 1;
                }
                //bind to device
                if(Utils.createBond(bd.getClass(), bd)){
                    ispaired += 2;
                }
                Method createCancelMethod=BluetoothDevice.class.getMethod("cancelBondProcess");
                boolean bool=(Boolean)createCancelMethod.invoke(bd);
                Log.d(TG2,"bool="+bool);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TG2, " paire device Exception:    " + e.toString());
            }
        }
        Log.d(TG2, " ispaired:    " + ispaired);
    }

    //The BroadcastReceiver that listens for discovered devices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TG2, "mReceiver()");

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TG2,"mReceiver found device: " + device.getName());

                // update to UI
                deviceListApapter.addDevice(device);
                deviceListApapter.notifyDataSetChanged();

            }
        }
    };

    @Override
    protected void onDestroy() {
        if(tgStreamReader != null){
            tgStreamReader.close();
            tgStreamReader = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }
}
