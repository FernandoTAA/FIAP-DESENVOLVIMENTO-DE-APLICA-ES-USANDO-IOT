package br.com.fiap.a30scj.interfoneremoto

import a30scj.fiap.com.br.interfoneremoto.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer


@SuppressLint("NewApi")
class MainActivity : AppCompatActivity() {

    var runningTime: LocalDateTime = LocalDateTime.MIN
    val timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notRunning()
        checkAlarm("D")
        val runningTask = object : TimerTask() {
            override fun run() {
                val sequenceWithoutUpdate = ChronoUnit.SECONDS.between(runningTime, LocalDateTime.now())
                if (sequenceWithoutUpdate > 60) {
                    notRunning()
                }
            }
        }
        timer.schedule(runningTask, 0, 60_000)

        val mqttHelper = MQTTHelper()

        val listSubscribeMethod = ArrayList<Consumer<MqttAndroidClient>>()
        listSubscribeMethod.add(Consumer { mqttHelper.subscribe(it, mqttHelper.outTopicRunning, 0)})
        listSubscribeMethod.add(Consumer { mqttHelper.subscribe(it, mqttHelper.outTopic, 0)})

        val mapActionMethodByTopic = HashMap<String, Consumer<String>>()
        mapActionMethodByTopic.put(mqttHelper.outTopicRunning, Consumer { this.checkRunning(it) })
        mapActionMethodByTopic.put(mqttHelper.outTopic, Consumer { this.checkAlarm(it) })
        val client = mqttHelper.connect(applicationContext, listSubscribeMethod, mapActionMethodByTopic)

        btAbrirPortao.setOnClickListener {
            mqttHelper.publishMessage(client, mqttHelper.inTopic, "L", 0)
        }

        btVerOk.setOnClickListener {
            mqttHelper.publishMessage(client, mqttHelper.outTopic, "D", 0)
        }

    }

    private fun checkAlarm(payload: String) {
        if (payload == "L") {
            ivGreenLightCampainha.visibility = View.VISIBLE
            ivRedLightCampainha.visibility = View.GONE
        } else {
            ivGreenLightCampainha.visibility = View.GONE
            ivRedLightCampainha.visibility = View.VISIBLE
        }
    }

    private fun checkRunning(payload: String) {
        runningTime = LocalDateTime.now()

        ivGreenLightRodando.visibility = View.VISIBLE
        ivRedLightRodando.visibility = View.GONE
    }

    private fun notRunning() {
        ivGreenLightRodando.visibility = View.GONE
        ivRedLightRodando.visibility = View.VISIBLE
    }
}
