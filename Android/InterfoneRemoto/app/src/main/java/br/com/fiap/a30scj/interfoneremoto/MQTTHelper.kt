package br.com.fiap.a30scj.interfoneremoto

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.UnsupportedEncodingException
import java.util.function.Consumer

@SuppressLint("NewApi")
class MQTTHelper {

    val serverUri = "tcp://iot.eclipse.org:1883"

    var clientId = "HomeAlarmClientApp"
    val outTopic = "outTopic9ce06661f8d92aa194aca8ceeb0f3d01"
    val outTopicRunning = "outTopicRunning9ce06661f8d92aa194aca8ceeb0f3d01"
    val inTopic = "inTopic9ce06661f8d92aa194aca8ceeb0f3d01"


    fun getMqttConnectionOption(): MqttConnectOptions {
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.setWill(outTopic, "1".toByteArray(), 1, true)
        return mqttConnectOptions
    }

    fun getMqttClient(context: Context, brokerUrl: String, clientId: String, listSubscribeMethod: List<Consumer<MqttAndroidClient>>): MqttAndroidClient {
        val mqttAndroidClient = MqttAndroidClient(context, brokerUrl, clientId)
        try {
            val token = mqttAndroidClient.connect(getMqttConnectionOption(), object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    mqttAndroidClient.setBufferOpts(getDisconnectedBufferOptions())
                    listSubscribeMethod.forEach { it.accept(mqttAndroidClient) }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

        return mqttAndroidClient
    }

    fun getDisconnectedBufferOptions(): DisconnectedBufferOptions {
        val disconnectedBufferOptions = DisconnectedBufferOptions()
        disconnectedBufferOptions.isBufferEnabled = true
        disconnectedBufferOptions.bufferSize = 100
        disconnectedBufferOptions.isPersistBuffer = true
        disconnectedBufferOptions.isDeleteOldestMessages = false
        return disconnectedBufferOptions
    }

    @Throws(MqttException::class, UnsupportedEncodingException::class)
    fun publishMessage(client: MqttAndroidClient, topic: String, msg: String, qos: Int) {
        var encodedPayload = msg.toByteArray(charset("UTF-8"))
        val message = MqttMessage(encodedPayload)
        message.id = 5866
        message.isRetained = true
        message.qos = qos
        client.publish(topic, message)
    }

    @Throws(MqttException::class)
    fun subscribe(client: MqttAndroidClient, topic: String, qos: Int) {
        val token = client.subscribe(topic, qos, null, object : IMqttActionListener {

            override fun onSuccess(iMqttToken: IMqttToken) {
                Log.d("TESTE", "Subscribe Successfully")
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
            }
        })
    }

    @Throws(MqttException::class)
    fun unSubscribe(client: MqttAndroidClient, topic: String) {

        val token = client.unsubscribe(topic)
        token.actionCallback = object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                Log.d("TESTE", "UnSubscribe Successfully $topic")
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                Log.e("TESTE", "UnSubscribe Failed $topic")
            }
        }
    }

    @Throws(MqttException::class)
    fun disconnect(client: MqttAndroidClient) {
        val mqttToken = client.disconnect()
        mqttToken.actionCallback = object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                Log.d("TESTE", "Successfully disconnected")
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                Log.d("TESTE", "Failed to disconnected " + throwable.toString())
            }
        }
    }

    fun connect(applicationContext: Context, listSubscribeMethod: List<Consumer<MqttAndroidClient>>, mapActionMethodByTopic: HashMap<String, Consumer<String>>): MqttAndroidClient {
        val mqttAndroidClient = getMqttClient(applicationContext, serverUri, clientId, listSubscribeMethod)

        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                Log.i("ERRO", "connectComplete")
            }

            override fun connectionLost(throwable: Throwable) {
                Log.i("ERRO", "connectionLost")
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                for (entry in mapActionMethodByTopic.entries) {
                    if (entry.key == topic) {
                        entry.value.accept(String(mqttMessage.payload))
                    }
                }
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.i("ERRO", "connectionLost")
            }
        })

        return mqttAndroidClient;
    }

}