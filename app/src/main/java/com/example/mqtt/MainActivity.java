package com.example.mqtt;


import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
  private HistoryAdapter mAdapter;

  MqttAndroidClient mqttAndroidClient;

  //final String serverUri = "tcp://iot.eclipse.org:1883";
  final String serverUri =  "ws://broker.hivemq.com:8000";

  String clientId = "HumaniqChatTest";
  final String subscriptionTopic = "humaniqTestSubscription";
  final String publishTopic = "humaniqTestPublish";
  final String publishMessage = "Hello Humaniq!";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scrolling);
    //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    //setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        publishMessage();
      }
    });


    RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
    mRecyclerView.setLayoutManager(mLayoutManager);

    mAdapter = new HistoryAdapter(new ArrayList<String>());
    mRecyclerView.setAdapter(mAdapter);

    clientId = clientId + System.currentTimeMillis();
    System.setProperty("http.keepAlive", "false");
    mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
    mqttAndroidClient.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean reconnect, String serverURI) {

        if (reconnect) {
          addToHistory("Reconnected to : " + serverURI);
          // Because Clean Session is true, we need to re-subscribe
          subscribeToTopic();
        } else {
          addToHistory("Connected to: " + serverURI);
        }
      }

      @Override
      public void connectionLost(Throwable cause) {
        addToHistory("The Connection was lost.");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        addToHistory("Incoming message: " + new String(message.getPayload()));
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {

      }
    });

    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setAutomaticReconnect(true);
    mqttConnectOptions.setCleanSession(false);



    try {
      //addToHistory("Connecting to " + serverUri);
      mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
          DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
          disconnectedBufferOptions.setBufferEnabled(true);
          disconnectedBufferOptions.setBufferSize(100);
          disconnectedBufferOptions.setPersistBuffer(false);
          disconnectedBufferOptions.setDeleteOldestMessages(false);
          mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
          subscribeToTopic();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
          addToHistory("Failed to connect to: " + serverUri);
        }
      });


    } catch (MqttException ex){
      ex.printStackTrace();
    }

  }

  private void addToHistory(String mainText){
    System.out.println("LOG: " + mainText);
    mAdapter.add(mainText);
    Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
        .setAction("Action", null).show();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement

    return super.onOptionsItemSelected(item);
  }

  public void subscribeToTopic(){
    try {
      mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
          addToHistory("Subscribed!");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
          addToHistory("Failed to subscribe");
        }
      });

      // THIS DOES NOT WORK!
      mqttAndroidClient.subscribe(publishTopic, 0, new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, final MqttMessage message) throws Exception {
          // message Arrived!
          runOnUiThread(new Runnable() {
            @Override public void run() {
              addToHistory(message.toString());
            }
          });

          System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
        }
      });

    } catch (MqttException ex){
      System.err.println("Exception whilst subscribing");
      ex.printStackTrace();
    }
  }

  public void publishMessage(){

    try {
      MqttMessage message = new MqttMessage();
      message.setPayload(publishMessage.getBytes());
      mqttAndroidClient.publish(publishTopic, message);
      if(!mqttAndroidClient.isConnected()){
        addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
      }
    } catch (MqttException e) {
      System.err.println("Error Publishing: " + e.getMessage());
      e.printStackTrace();
    }
  }

}