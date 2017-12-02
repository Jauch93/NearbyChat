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

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    private static final long CONNECTION_TIME_OUT = 10000L;
    private GoogleApiClient mGoogleApiClient;
    private String adress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                0);

        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("Pending");
    }
    //-----------------------------------------------------------------Zugriffsrechte einstellen
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
                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                    mStatusText.setText("Verbindung zum GoogleApiClient wird hergestellt.");
                    mGoogleApiClient.connect();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                    mStatusText.setText("Verbindung zum GoogleApiClient fehlgeschlagen.");
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
        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("Mit GoogleApiClient verbunden.");
        // GoogleApiClient is now connected, we can start advertising
    }

    //-------------------------------------------------------------------------------Establish connection
    public void startAdvertising(View view)      //Wird in onConnected() aufgerufen
    {
        String name = "Nearby Advertising";
        String serviceId = "SERVICE_ID";

        Nearby.Connections.startAdvertising(mGoogleApiClient,       //Ab hier wird advertising wirklich ausgeführt!
                name,
                serviceId,
                mConnectionLifecycleCallback,       //Die Funtion wird aufgerufen, wenn ein discoverer eine Verbindung anfordert.
                new AdvertisingOptions((P2P_CLUSTER)))
                .setResultCallback( new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult( Connections.StartAdvertisingResult result ) {
                        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                        if( result.getStatus().isSuccess() ) {
                            mStatusText.setText("Advertising...");
                        }
                        else
                        {
                            mStatusText.setText("Advertising konnte nicht gestartet werden...");
                        }
                    }
                });
    }

    public void startDiscovery(View view)
    {
        String serviceId = "SERVICE_ID";

        Nearby.Connections.startDiscovery(mGoogleApiClient,
                serviceId,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(P2P_CLUSTER))
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                        if (status.isSuccess()) {
                            mStatusText.setText( "Discovering" );
                        } else {
                            mStatusText.setText("Unable to initiate Discovery");
                        }
                    }
                });
    }

    public void disconnectAll(View view)
    {
        Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("Verbindung abgebrochen");
    }

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                    mStatusText.setText("An Advertiser has been found!");

                    String nickname = "Jauch";
                    Nearby.Connections.requestConnection(mGoogleApiClient,
                            nickname,
                            endpointId,
                            mConnectionLifecycleCallback)
                            .setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                                    if (status.isSuccess()) {
                                        mStatusText.setText( "Connection requested" );
                                    } else {
                                        mStatusText.setText("Unable to request a connection.");
                                    }
                                }
                            });

                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {


                @Override       //Wird aufgerufen, wenn ein discoverer eine Verbindung anfordert.
                public void onConnectionInitiated(String endpointId , ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.Connections.acceptConnection(mGoogleApiClient, endpointId, mPayloadCallback);

                    adress = endpointId;

                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                    mStatusText.setText("Verbindung wird hergestellt");
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            mStatusText.setText("Verbindung erfolgreich hergestellt");
                            Nearby.Connections.stopAdvertising(mGoogleApiClient);                   //Advertising und Discovery wieder deaktivieren.
                            Nearby.Connections.stopDiscovery(mGoogleApiClient);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
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
                    TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
                    mStatusText.setText("Verbindung abgebrochen");
                }
            };

    @Override
    public void onConnectionSuspended(int i)
    {
        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("FEHLERCODE #2");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        TextView mStatusText = (TextView) findViewById(R.id.mStatusText);
        mStatusText.setText("FEHLERCODE #1");
        mGoogleApiClient.connect();
    }
//----------------------------------------------------------------------------------Sending shit


    public void sendMessage(View view)
    {
        TextView mTextInput = (TextView) findViewById(R.id.messageInput);
        CharSequence text = mTextInput.getText();
        String s = text.toString();
        mTextInput.clearComposingText();
        byte[] k;
        k = s.getBytes(Charset.forName("UTF-8"));

        Payload bytePayload;
        bytePayload = Payload.fromBytes(k);

        Nearby.Connections.sendPayload(mGoogleApiClient, adress, bytePayload);        //Steht der String als Adresse für das empfangende Gerät??

        TextView messageOutput = (TextView) findViewById(R.id.messageOutput);
        messageOutput.setText("Sending Message: " + s);
    }


    public PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload)
        {
            TextView mTextOutput = (TextView) findViewById(R.id.messageOutput);
            mTextOutput.setText(s);
            byte[] received = payload.asBytes();

            //String message = received.toString();
            String message = new String(received, Charset.forName("UTF-8"));

            mTextOutput.setText(s + ": " + message);
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };
}
