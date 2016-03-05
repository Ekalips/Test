package com.example.djqrj.test;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{
    WifiManager mWifiManager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    Context contex;
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        contex = this;

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Это приложение нуждается в разрешении определения геолокации");
                builder.setMessage("Пожалуйста дайте приложинию доступ, что бы оно могло определять точки доступа.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
        Toast.makeText(this,"Получение списка доступных сетей",Toast.LENGTH_SHORT).show();


        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!mWifiManager.isWifiEnabled()) mWifiManager.setWifiEnabled(true);
        registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();




    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mSwipeRefreshLayout.setRefreshing(false);
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                Log.d("wifiScanRes",mScanResults.toString());
                ArrayList<Item> items = new ArrayList<>();
                for (ScanResult a:mScanResults) {
                    items.add(new Item(a));
                }
                ListAdapter adapter = new ListAdapter(items);
                recyclerView.setAdapter(adapter);

                recyclerView.addOnItemTouchListener( new RecyclerItemClickListener(contex, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        ConnectToNetwork(((ListAdapter)recyclerView.getAdapter()).getItem(position).result);
                    }
                }));

                for (ScanResult a:mScanResults) {
                    if (!mWifiManager.isWifiEnabled()) mWifiManager.setWifiEnabled(true);
                    if (a.SSID.contains(getResources().getString(R.string.WiFi))) ConnectToNetwork(a);
                }
            }
        }
    };

    public void ConnectToNetwork(final ScanResult res)
    {
        if (res.capabilities.contains("WEP")|| res.capabilities.contains("WPA")) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.user_input, null);
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setView(view);
                final EditText text = (EditText) view.findViewById(R.id.editText);
                alertBuilder.setCancelable(true).setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (res.capabilities.contains("WEP"))
                        {
                            ConnectToWep(res.SSID,text.getText().toString());
                            Toast.makeText(contex,"Trying to connect",Toast.LENGTH_LONG).show();
                        }
                        else{
                            ConnectToWpa(res.SSID,text.getText().toString());
                            Toast.makeText(contex,"Trying to connect",Toast.LENGTH_LONG).show();

                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                Dialog dialog = alertBuilder.create();
                dialog.show();
        }
        else
        {
            ConnectToNoPass(res.SSID);
            Toast.makeText(contex,"Trying to connect",Toast.LENGTH_LONG).show();
        }
    }
    public void ConnectToWep(String networkSSID,String password)
    {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.wepKeys[0] = "\"" + password + "\"";
        conf.wepTxKeyIndex = 0;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        Log.d("mywifi",String.valueOf(Connect(networkSSID, conf)));
    }
    public void ConnectToWpa(String networkSSID,String password)
    {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.preSharedKey = "\""+ password +"\"";
        Log.d("mywifi",String.valueOf(Connect(networkSSID, conf)));
    }
    public void ConnectToNoPass(String networkSSID)
    {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        Log.d("mywifi",String.valueOf(Connect(networkSSID, conf)));
    }

    private boolean Connect(String networkSSID, WifiConfiguration conf) {
        if (!mWifiManager.isWifiEnabled()) mWifiManager.setWifiEnabled(true);
        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(conf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        DisconnectFromAll();
                    }
                };
                new Timer().schedule(task,(long) 10000);
                return wifiManager.reconnect();
            }
        }
        return false;
    }
    private void DisconnectFromAll()
    {
        Log.d("mywifi","disconecting");
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("log", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    public void onRefresh() {
        mWifiManager.startScan();
    }

}
