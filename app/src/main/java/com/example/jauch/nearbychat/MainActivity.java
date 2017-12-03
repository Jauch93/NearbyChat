package com.example.jauch.nearbychat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER;

public class MainActivity extends AppCompatActivity
{

    private ConnectGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("Pending");
    }

    public void sendMessage(View view)
    {
        TextView mTextInput = (TextView) findViewById(R.id.messageInput);
        CharSequence text = mTextInput.getText();
        String s = text.toString();
        mTextInput.clearComposingText();

        game.sendMessage(s);
    }

    public void startAdvertising(View view)
    {
        game = new ConnectGame("Tisch 1", true, 1, this);
        game.requestPermissions();
    }

    public void startDiscovery(View view)
    {
        game = new ConnectGame("Spieler 1", false, 1, this);
        game.requestPermissions();
    }

    public void getStatus(View view)
    {
        String status = game.getStatus();
        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText(status);
    }

}
