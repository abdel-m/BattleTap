package com.julab.battletap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * This is the battle activity
 */
public class BluetoothBattle extends Activity
{
    // Debugging
    private static final String TAG = "BluetoothBattle";
    private static final boolean D = true;

    // Message types sent from the BluetoothBattleService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothBattleService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private Button btnPush;
    private Button btnConfirm;
    private Button btnConnection;

    // Name of the connected device
    private String connectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer outStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothBattleService battleService = null;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.board_game);

        // Get local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null)
        {
            // Changer le toast par un dialog
            Toast.makeText(this, R.string.bluetooth_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize the push button with a listener that for click events
        btnPush = (Button) findViewById(R.id.btnPush);
        btnPush.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                sendMessage("end");
            }
        });

        // Initialize the confirm button with a listener that for click events
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessage("end");
            }
        });

        btnConnection = (Button) findViewById(R.id.btnConnection);
        btnConnection.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                BluetoothBattle.this.showDeviceList();
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupGame() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.enable();
        }
        else
        {
            if (battleService == null) setupGame();
        }
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (battleService != null)
        {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (battleService.getState() == BluetoothBattleService.STATE_NONE)
            {
                // Start the Bluetooth services
                battleService.start();
            }
        }
    }

    private void setupGame()
    {
        Log.d(TAG, "setupGame()");

        // Initialize the BluetoothBattleService to perform bluetooth connections
        battleService = new BluetoothBattleService(this, mHandler);

        // Initialize the buffer for outgoing messages
        outStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (battleService != null) battleService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable()
    {
        if (D) Log.d(TAG, "ensure discoverable");
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message)
    {
        // Check that we're actually connected before trying anything
        if (battleService.getState() != BluetoothBattleService.STATE_CONNECTED)
        {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0)
        {
            // Get the message bytes and tell the BluetoothBattleService to write
            byte[] send = message.getBytes();
            battleService.write(send);
        }
    }

    // The Handler that gets information back from the BluetoothBattleService
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1)
                    {
                        case BluetoothBattleService.STATE_CONNECTED:
                            // here, you can display a message for view that is connected
                            break;
                        case BluetoothBattleService.STATE_CONNECTING:
                            // here, you can display a message for view that is connected
                            break;
                        case BluetoothBattleService.STATE_LISTEN:
                        case BluetoothBattleService.STATE_NONE:
                            // here, you can display a message for view that is not connected
                            break;
                    }
                    break;
                case MESSAGE_WRITE: // go here if it is this app which send a message
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    Toast.makeText(getApplicationContext(), "WRITE : " + writeMessage, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_READ: // go here if the remote app send a message
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(connectedDeviceName + ":  " + readMessage);
                    if(readMessage.equals("end"))
                    {
                        Toast.makeText(getApplicationContext(), "READ : " + readMessage, Toast.LENGTH_LONG).show();
                        finish();
                    }
                    break;
                case MESSAGE_DEVICE_NAME: // go here when a device is connected
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode)
        {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK)
                {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    battleService.connect(device);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    public void showDeviceList()
    {
        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.scan:
                showDeviceList();
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

}