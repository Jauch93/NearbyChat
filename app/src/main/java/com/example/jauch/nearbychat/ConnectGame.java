package com.example.jauch.nearbychat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
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

import java.nio.charset.Charset;

import static com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER;

/**
 * Created by jauch on 02.12.17.
 * Ziel dieser Datei ist es, die VerbindungsRoutine inklusive SendeMethoden vom GUI abzutrennen.
 */

public class ConnectGame extends Activity implements        //WTF --> wo isch das scheiss ManifestTeil mit de berechtigunge hichoo?
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    private GoogleApiClient mGoogleApiClient;
    private boolean isAdvertiser;
    private String state;
    private String name;
    private Activity act;
    private String adress;
    private String serviceId = "kkk";  //Noch nicht klar, was das hier bewirkt.

    ConnectGame(String name, boolean isAdvertiser, int partySize, Activity act)
    {
        this.name = name;
        this.isAdvertiser = isAdvertiser;
        this.act = act;


    }
    //-----------------------------------------------------------------Zugriffsrechte einstellen

    public void requestPermissions()
    {
        ActivityCompat.requestPermissions(act,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                0);

        state = "Requesting Permissions";
    }

    public String getStatus()
    {
        return state;
    }


    //Wird über Callback aufgerufen, wenn das POPUP zu Zugriffsrechten bestätigt wurde.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mGoogleApiClient = new GoogleApiClient.Builder( this )
                            .addConnectionCallbacks( this )
                            .addOnConnectionFailedListener( this )
                            .addApi( Nearby.CONNECTIONS_API )
                            .useDefaultAccount()
                            .build();
                    mGoogleApiClient.connect();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    //Sobald das GoogleApiClient objekt erstellt und verbunden ist,
    // wird diese Funktion mittels CallBackAufgerufen.
    {
        // GoogleApiClient is now connected, we can start advertising
        state = "Connected to GoogleApiClient";

        if(isAdvertiser)
            startAdvertising();
        else
            startDiscovery();
    }

    //-------------------------------------------------------------------------------Establish connection
    public void startAdvertising()      //Wird in onConnected() aufgerufen
    {
         Nearby.Connections.startAdvertising(mGoogleApiClient,       //Ab hier wird advertising wirklich ausgeführt!
                name,
                serviceId,
                mConnectionLifecycleCallback,       //Die Funtion wird aufgerufen, wenn ein discoverer eine Verbindung anfordert.
                new AdvertisingOptions((P2P_CLUSTER)))
                .setResultCallback( new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult( Connections.StartAdvertisingResult result ) {
                        if( result.getStatus().isSuccess() ) {
                            state = "Advertising...";
                        }
                        else
                        {
                            state = "Error: Unable to start Advertising.";
                        }
                    }
                });
    }

    public void startDiscovery()
    {
        Nearby.Connections.startDiscovery(mGoogleApiClient,
                serviceId,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(P2P_CLUSTER))
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            state = "Discovering...";
                        } else {
                            state = "Unable to start Discovering.";
                        }
                    }
                });
    }

    public void disconnectAll()
    {
        Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("Verbindung abgebrochen");
    }

    public final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    state = "Advertiser Found!";

                    String endpointName = discoveredEndpointInfo.getEndpointName();
                    state = ("AdvertiserName " + endpointName);

                    String nickname = "Jauch";          //Name des Discoverers, der beim Advertiser angezeigt wird.
                    Nearby.Connections.requestConnection(mGoogleApiClient,
                            nickname,
                            endpointId,
                            mConnectionLifecycleCallback)
                            .setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    if (status.isSuccess()) {
                                        state = "Connection Requested";
                                    } else {
                                        state = "Unable to request a connection.";
                                    }
                                }
                            });

                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    public final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {


                @Override       //Wird aufgerufen, wenn ein discoverer eine Verbindung anfordert.
                public void onConnectionInitiated(String endpointId , ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mPayloadCallback);

                    adress = endpointId;
                    String endPointName = connectionInfo.getEndpointName();
                    state = ("DiscovererName = " + endPointName);

                                                               //Hier werden allen Spielern ihre EndPointId zugewiesen.
                    state = "Verbindung wird hergestellt";
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            state = "Verbindung erfolgreich hergestellt";
                            Nearby.Connections.stopAdvertising(mGoogleApiClient);                   //Advertising und Discovery wieder deaktivieren.
                            Nearby.Connections.stopDiscovery(mGoogleApiClient);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            state = "Connection rejected";
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    state = "Verbindung abgebrochen";
                }
            };

    @Override
    public void onConnectionSuspended(int i)
    {
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        mGoogleApiClient.connect();
    }
//----------------------------------------------------------------------------------Sending shit


    public void sendMessage(String message)
    {
        byte[] k;
        k = message.getBytes(Charset.forName("UTF-8"));

        Payload bytePayload;
        bytePayload = Payload.fromBytes(k);

        Nearby.Connections.sendPayload(mGoogleApiClient, adress, bytePayload);        //Steht der String als Adresse für das empfangende Gerät??

        TextView messageOutput = (TextView) findViewById(R.id.messageOutput);
        messageOutput.setText("Sending Message: " + message);
    }


    public PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload)
        {
            //TextView mTextOutput = (TextView) findViewById(R.id.messageOutput);
            //mTextOutput.setText(s);
            byte[] received = payload.asBytes();

            String message = new String(received, Charset.forName("UTF-8"));

            //mTextOutput.setText(s + ": " + message);
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
