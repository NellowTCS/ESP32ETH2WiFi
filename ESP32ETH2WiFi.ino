// Required Libraries
#include <ETH.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <BluetoothSerial.h>

BluetoothSerial SerialBT; // Bluetooth Serial object

// Initial Wi-Fi credentials
String ssid = "Netgear25";          
String password = "windyvalley933";  

// Ethernet configuration
uint8_t mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED }; 
WiFiServer ethServer(80);  

// UDP configuration
WiFiUDP wifiUdp;
const int UDP_BUFFER_SIZE = 512;

void setup() {
  Serial.begin(115200);
  
  // Initialize Bluetooth
  SerialBT.begin("ESP32ETH2WiFi");

  // Connect to Wi-Fi
  connectToWiFi();

  // Initialize Ethernet
  ETH.begin();
  while (ETH.localIP() == IPAddress(0, 0, 0, 0)) {
    delay(1000);
    Serial.println("Connecting to Ethernet...");
  }
  Serial.print("Ethernet IP: ");
  Serial.println(ETH.localIP());

  // Start the Ethernet server
  ethServer.begin();
  Serial.println("Ethernet server started.");

  // Initialize UDP
  wifiUdp.begin(1234);
}

void loop() {
  forwardTCP();
  forwardUDP();

  // Check for Bluetooth commands
  if (SerialBT.available()) {
    String command = SerialBT.readStringUntil('\n');
    handleCommand(command);
  }
}

void connectToWiFi() {
  Serial.println("Connecting to Wi-Fi...");
  WiFi.disconnect();  // Disconnect if already connected
  WiFi.begin(ssid.c_str(), password.c_str());
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }
  Serial.println("\nWi-Fi connected!");
  Serial.print("Wi-Fi IP: ");
  Serial.println(WiFi.localIP());
}

void handleCommand(String command) {
  if (command.startsWith("SET_WIFI")) {
    int delimiterIndex = command.indexOf(',');
    if (delimiterIndex > 0) {
      ssid = command.substring(9, delimiterIndex);  // Get SSID from command
      password = command.substring(delimiterIndex + 1); // Get password from command
      connectToWiFi(); // Attempt to reconnect with new credentials
      SerialBT.println("Wi-Fi credentials updated. Reconnecting...");
    } else {
      SerialBT.println("Invalid command format. Use: SET_WIFI,SSID,PASSWORD");
    }
  }
}

void forwardTCP() {
  WiFiClient ethClient = ethServer.available();
  if (ethClient) {
    Serial.println("New Ethernet client connected!");

    IPAddress wifiGateway = WiFi.gatewayIP();
    WiFiClient wifiClient;
    if (!wifiClient.connect(wifiGateway, 80)) {
      Serial.println("Connection to Wi-Fi server failed");
      ethClient.stop();
      return;
    }

    while (ethClient.connected() && wifiClient.connected()) {
      if (ethClient.available()) {
        String data = ethClient.readStringUntil('\n'); 
        wifiClient.println(data);  
        SerialBT.println("Forwarded to Wi-Fi: " + data); // Send to Bluetooth
      }

      if (wifiClient.available()) {
        String response = wifiClient.readStringUntil('\n'); 
        ethClient.println(response);  
        SerialBT.println("Forwarded to Ethernet: " + response); // Send to Bluetooth
      }
    }

    ethClient.stop();
    wifiClient.stop();
    Serial.println("Client disconnected.");
  }
}

void forwardUDP() {
  int packetSize = wifiUdp.parsePacket();
  if (packetSize) {
    char buffer[UDP_BUFFER_SIZE];
    int len = wifiUdp.read(buffer, UDP_BUFFER_SIZE);
    buffer[len] = 0; 

    IPAddress wifiGateway = WiFi.gatewayIP();
    wifiUdp.beginPacket(wifiGateway, 80); 
    wifiUdp.write((uint8_t*)buffer, len);
    wifiUdp.endPacket();

    SerialBT.print("Forwarded UDP packet to Wi-Fi: ");
    SerialBT.println(buffer);
  }
}
