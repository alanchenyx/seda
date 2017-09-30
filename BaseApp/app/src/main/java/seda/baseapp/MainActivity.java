package seda.baseapp;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import seda.baseapp.adapter.NavigationItemAdapter;
import seda.baseapp.fragment.GeneralContentFragment;
import seda.baseapp.fragment.ToDoFragment;
import seda.baseapp.todo.ToDoActivity;

public class MainActivity extends AppCompatActivity {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        Log.i("SEDA Base App", "Initialized...");
//
//        Button sedaBtn = findViewById(R.id.sedaBtn);
//        sedaBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, ToDoActivity.class);
//                Log.i("SEDA Base App", "Go to ToDo");
//                startActivity(intent);
//            }
//        });
//    }

    private String[] navigationItemsNames = null;
    private DrawerLayout drawerLayout = null;
    private ListView drawerList = null;
    private String curFragmentName = "";

    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice bluetoothDevice;
    private HashMap<String, BluetoothDevice> bluetoothDeviceHashMap = new HashMap<String, BluetoothDevice>();


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                Log.wtf("bingfengappservice", " BluetoothDevice.ACTION_FOUND.equals(action) -> " + BluetoothDevice.ACTION_FOUND.equals(action));
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.wtf("bingfengappservice", " bluetoothDevice " + bluetoothDevice);

                final String deviceName = bluetoothDevice.getName();
                Log.wtf("bingfengappservice", " deviceName " + deviceName);

                bluetoothDeviceHashMap.put(deviceName, bluetoothDevice);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Found -> " + deviceName, Toast.LENGTH_LONG).show();
                    }
                });

                String deviceHardwareAddress = bluetoothDevice.getAddress(); // MAC address
            }
        }
    };


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navigationItemsNames = getResources().getStringArray(R.array.navigation_bar_item_names);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //could change it to other list view use it here first
        drawerList = (ListView) findViewById(R.id.left_drawer);
        //
        NavigationItemAdapter navigationItemAdapter = new NavigationItemAdapter(this,R.layout.navigation_list_item);


        // Set the adapter for the list view
        drawerList.setAdapter(navigationItemAdapter);

        for (String oneNaviItem:navigationItemsNames)
        {
            navigationItemAdapter.add(oneNaviItem);
        }


        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        GeneralContentFragment generalContentFragment = new GeneralContentFragment();
        generalContentFragment.setLayoutId(R.layout.about_us);

        curFragmentName = getString(R.string.about_us);

        fragmentTransaction.add(R.id.content_frame, generalContentFragment, getString(R.string.about_us));
        fragmentTransaction.commit();

//        need to request permission in code aswell for bluetooth?

////        request code
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);



//        blue tooth https://www.tutorialspoint.com/android/android_bluetooth.htm

        BA = BluetoothAdapter.getDefaultAdapter();

//        these two line ask request for blue tooth
        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//
//        // 0-> request code
        startActivityForResult(turnOn, 0);


//         make this device discoverable in 300s
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);

        startActivityForResult(discoverableIntent, 1);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);


    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //position is a index value, which refers to the element in mPlanetTitles
            selectItem(position, (String)view.getTag());
            Log.d("bingappservice", "navigation item clicked, position: " + position + ", tag: " +view.getTag());
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position, String itemTagName) {

        FragmentManager fragmentManager = getFragmentManager();

        //fragment replace will destroy the fragment
        //use hide instead
        //change fragment in activity_main to the right layout
        //https://stackoverflow.com/questions/22713128/how-can-i-switch-between-two-fragments-without-recreating-the-fragments-each-ti
        if(itemTagName.equals(getString(R.string.to_do_item)) && !itemTagName.equals(curFragmentName))
        {
            ToDoFragment fragment = new ToDoFragment();

            toggleFragment(fragmentManager, fragment, itemTagName, getString(R.string.to_do_item), position);

        }
        else if (itemTagName.equals(getString(R.string.about_us)) && !itemTagName.equals(curFragmentName))
        {
            GeneralContentFragment fragment = new GeneralContentFragment();

            //set layout id
            fragment.setLayoutId(R.layout.about_us);
            toggleFragment(fragmentManager, fragment, itemTagName, getString(R.string.to_do_item), position);

        }

        drawerList.setItemChecked(position, true);
//        setTitle(navigationItemsNames[position]);
        drawerLayout.closeDrawer(drawerList);

    }

    public void toggleFragment(FragmentManager fragmentManager, Fragment fragment, String curFragmentTagName, String startingFragmentName, int currentFragmentTagNameIndex)
    {
        //start new fragment
        if (fragmentManager.findFragmentByTag(curFragmentTagName) != null)
        {
            fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag(curFragmentTagName)).commit();
        }
        else
        {
            fragmentManager.beginTransaction()
                    .add(R.id.content_frame, fragment, getString(R.string.to_do_item))
                    .commit();
        }

        //hide old fragment
        if (fragmentManager.findFragmentByTag(curFragmentName) != null)
        {
            fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(curFragmentName)).commit();
        }

        curFragmentName = navigationItemsNames[currentFragmentTagNameIndex];

    }


//    listen for registered message
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // means request bluetooth successfully
        if (requestCode == 0)
        {
            if (resultCode == RESULT_OK)
            {
                Log.wtf("bingfengappservice", "request bluetooth successfully");
            }
            else
            {
                Log.wtf("bingfengappservice", "request bluetooth failed");
            }
        }

        if (requestCode == 1)
        {

            //if dicoverable failed it will return cancel
            //seemed that you need to make sure bluetooth is opened then start discoverying other devices
            //otherwise, the dicovery process wont start.
            if (resultCode != RESULT_CANCELED)
            {
                if (BA.isDiscovering()) {
                    BA.cancelDiscovery();
                }

                BA.startDiscovery();

                Log.wtf("bingfengappservice", "dic request bluetooth successfully -> result code -> " + resultCode);
            }
            else
            {
                Log.wtf("bingfengappservice", "dic request bluetooth failed -> result code -> " + requestCode);
            }

        }

    }

    public void startServerThread(TextView displayBluetoothTextView)
    {
        new AcceptThread(displayBluetoothTextView).start();
    }

    public void startClientThread(String name, TextView displayBluetoothTextView)
    {
        new ConnectThread(bluetoothDeviceHashMap.get(name), displayBluetoothTextView).start();
    }

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mmServerSocket;
        private final TextView displayBluetoothTextView;

        public AcceptThread(TextView displayBluetoothTextView) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            //https://stackoverflow.com/questions/29059530/is-there-any-way-to-generate-the-same-uuid-based-on-the-seed-string-in-jdk-or-so
            this.displayBluetoothTextView = displayBluetoothTextView;
            BluetoothServerSocket tmp = null;
            String name = "server1";
            String aString="OUR_SECRET";
            UUID resultUUID = UUID.nameUUIDFromBytes(aString.getBytes());

            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = BA.listenUsingRfcommWithServiceRecord(name, resultUUID);
            } catch (IOException e) {
                Log.wtf("bingfengappserver", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BufferedWriter out;
            BufferedReader in;
            BluetoothSocket clientSocket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    clientSocket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.wtf("bingfengappserver", "Socket's accept() method failed", e);
                    break;
                }

                if (clientSocket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
//                    manageMyConnectedSocket(socket);
                    try
                    {
//                        echo server
                        out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String inputLine = null;
                        while (true)
                        {
                            out.write("" + System.currentTimeMillis() + "\n");
                            out.flush();
                            inputLine = in.readLine();
                            Log.wtf("bingfengappservice", "From Client -> " + inputLine);
//                            displayBluetoothTextView.setText(inputLine);

//                            this code is ugly will refactor it later. just want to show msg on the UI
                            final String finalInputLine = inputLine;
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Toast.makeText(getApplicationContext(), "From Client -> " + finalInputLine, Toast.LENGTH_LONG).show();
                                }
                            });

                            Thread.sleep(1000);

                        }
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        try
                        {
                            mmServerSocket.close();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.wtf("bingfengappservice", "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final TextView displayBluetoothTextView;
        private BufferedWriter out;
        private BufferedReader in;

        public ConnectThread(BluetoothDevice device, TextView displayBluetoothTextView) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            this.displayBluetoothTextView = displayBluetoothTextView;

            String name = "server1";
            String aString="OUR_SECRET";
            UUID resultUUID = UUID.nameUUIDFromBytes(aString.getBytes());

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(resultUUID);
            } catch (IOException e) {
                Log.wtf("bingfengappservice", "Socket's create() method failed", e);
            }
            //client connecting to server sockt
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BA.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                out = new BufferedWriter(new OutputStreamWriter(mmSocket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
                String inputLine = null;
                while (true)
                {
                    Log.wtf("bingfengappservice", "start reading");
                    inputLine = in.readLine();
                    Log.wtf("bingfengappservice", "from server -> " + inputLine);

//                    displayBluetoothTextView.setText(inputLine);

                    //this code is ugly will refactor it later. just want to show msg on the UI
                    final String finalInputLine = inputLine;
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(getApplicationContext(), "From server -> " + finalInputLine, Toast.LENGTH_LONG).show();
                        }
                    });

//                    Toast.makeText(getApplicationContext(), "From server -> " + inputLine,Toast.LENGTH_LONG).show();
                    out.write("" + inputLine + "\n");
                    //have to flush it otherwise does not work.
                    out.flush();

                    Thread.sleep(1000);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                // Unable to connect; close the socket and return.
            }
            finally
            {
                try
                {
                    mmSocket.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
//            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.wtf("bingfengappservice", "Could not close the client socket", e);
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }









}