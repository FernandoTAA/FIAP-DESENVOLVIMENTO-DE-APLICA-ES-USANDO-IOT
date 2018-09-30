#include <ESP8266WiFi.h>
#include <PubSubClient.h>

const char* ssid = "2.4G_NETVIRTUA_752";
const char* password = "3903605320";
const char* mqtt_server = "iot.eclipse.org";

const char* mqttClient = "HomeAlarmClient";
const char* inTopic = "inTopic9ce06661f8d92aa194aca8ceeb0f3d01";
const char* outTopic = "outTopic9ce06661f8d92aa194aca8ceeb0f3d01";
const char* outTopicRunning = "outTopicRunning9ce06661f8d92aa194aca8ceeb0f3d01";

const int rele = 4;
const int led = 5;
const int button = 16;

WiFiClient espClient;
PubSubClient client(espClient);
char msg[50];
long qtdOfLoops = 0;

void setup() {
  pinMode(rele, OUTPUT);
  pinMode(led, OUTPUT);
  pinMode(button, INPUT);
  digitalWrite(rele, HIGH);
  Serial.begin(9600);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void loop() {
  Serial.println("loop");
  if (!client.connected()) {
    reconnect();
  }
  
  client.loop();
  
  int buttonPushed = digitalRead(button);
  if (buttonPushed == HIGH) {
    client.publish(outTopic, "L", true);
    digitalWrite(led, HIGH);
    delay(1000);
  } else {
    digitalWrite(led, LOW);
  }

  if (qtdOfLoops == 0 || qtdOfLoops > 100) {
    snprintf(msg, 75, "%ld", millis());
    client.publish(outTopicRunning, msg);
    qtdOfLoops = 1;
  } else {
    qtdOfLoops++;
  }
  delay(200);
}

void setup_wifi() {
  delay(10);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void callback(char* topic, byte* payload, unsigned int length) {
  if (((char)payload[0]) == 'L') {
    digitalWrite(rele, LOW);
    delay(3000);
    digitalWrite(rele, HIGH);
    client.publish(inTopic, "D");
  }
}

void reconnect() {
  while (!client.connected()) {
    if (client.connect(mqttClient)) {
      client.subscribe(inTopic);
    } else {
      delay(5000);
    }
  }
}
