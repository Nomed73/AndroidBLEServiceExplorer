package packt.com.androidbleserviceexplorer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.AsyncTaskLoader;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BLEPackt";
    Button startScanningButton;
    Button stopScanningButton;
    ListView deviceListView;

    ArrayAdapter<String> listAdapter;
    ArrayList<BluetoothDevice> deviceList;

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = (ListView) findViewById(R.id.deviceListView);
        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setVisibility(View.INVISIBLE);

        listAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        deviceList = new ArrayList<>();
        deviceListView.setAdapter(listAdapter);

        startScanningButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                stopScanning();
            }
        });

        //Do we add this function call?
        initialiseBluetooth();

        //Not sure if this belongs here or after the isDuplicate method.
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                stopScanning();
                listAdapter.clear();
                BluetoothDevice device = deviceList.get(position);
                device.connectGatt(MainActivity.this, true,gattCallback);
            }
        });

        //Prompt user to turn on BT
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        /*
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
        */
    }

    private void initialiseBluetooth(){
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public void startScanning(){
        listAdapter.clear();
        deviceList.clear();
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
    }

    public void stopScanning(){
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run(){
                bluetoothLeScanner.stopScan(leScanCallBack); } });
    }

    //Device scan callback
    private ScanCallback leScanCallBack = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            if (result.getDevice() != null){
                if (!isDuplicate(result.getDevice())){
                    synchronized (result.getDevice()){
                        String itemDetail = result.getDevice().getName() == null ? result.getDevice().getAddress():
                                result.getDevice().getName();
                        listAdapter.add(itemDetail);
                        deviceList.add(result.getDevice());
                    }
                }
            }
        }
    };

    //Filter out duplicates
    private boolean isDuplicate(BluetoothDevice device) {
        for (int i = 0; i < listAdapter.getCount(); i++) {
            String addedDeviceDetail = listAdapter.getItem(i);
            if (addedDeviceDetail.equals(device.getAddress()) || addedDeviceDetail.equals(device.getName())) {
                return true;
            }
        }
        return false;
    }

    //May have to move the code in onCreate here

    //
    protected BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED)  {
                Log.i(TAG, "onConnectionStateChange() -   STATE_CONNECTED");
                boolean discoverServicesOk = gatt.discoverServices();
            } else if (newState ==   BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange() -   STATE_DISCONNECTED");
            }
        }
    };

    //@Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        final List < BluetoothGattService > services = gatt.getServices();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < services.size(); i++) {
                    BluetoothGattService service = services.get(i);
                    StringBuffer buffer = new
                            StringBuffer
                            (services.get(i).getUuid().toString());
                    List < BluetoothGattCharacteristic > characteristics =
                            service.getCharacteristics();
                    for (int j = 0; j < characteristics.size(); j++) {
                        buffer.append("\n");
                        buffer.append("Characteristic:" +    characteristics.get(j).getUuid().toString());
                    }
                    listAdapter.add(buffer.toString());}}});
    }

}