// Required Libraries
#include <ETH.h>      
#include <WiFi.h>
#include <WiFiUdp.h>
#include <BluetoothSerial.h>

BluetoothSerial SerialBT;  // Create Bluetooth Serial object

// Wi-Fi credentials
const char* ssid = "Your_SSID";          
const char* password = "Your_Password";  

// Ethernet configuration
WiFiServer ethServer(80);  // Listen on port 80 for incoming connections

// UDP configuration
WiFiUDP wifiUdp;
const int UDP_BUFFER_SIZE = 512; 

void setup() {
  Serial.begin(115200);
  
  // Initialize Bluetooth
  SerialBT.begin("ESP32ETH2WiFi");

  // Connect to Wi-Fi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to Wi-Fi...");
  }
  Serial.println("Wi-Fi connected!");
  Serial.print("Wi-Fi IP: ");
  Serial.println(WiFi.localIP());

  // Initialize Ethernet without custom MAC
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
}

void forwardTCP() {
  WiFiClient ethClient = ethServer.available();
  if (ethClient) {
    Serial.println("New Ethernet client connected!");

    // Use the gateway IP from the Wi-Fi connection
    IPAddress wifiGateway = WiFi.gatewayIP();

    // Connect to the Wi-Fi server using the gateway IP
    WiFiClient wifiClient;
    if (!wifiClient.connect(wifiGateway, 80)) {
      Serial.println("Connection to Wi-Fi server failed");
      ethClient.stop();
      return;
    }

    // Forward data bidirectionally
    while (ethClient.connected() && wifiClient.connected()) {
      if (ethClient.available()) {
        String data = ethClient.readStringUntil('\n'); 
        wifiClient.println(data);  
        Serial.println("Forwarded to Wi-Fi: " + data);
      }

      if (wifiClient.available()) {
        String response = wifiClient.readStringUntil('\n'); 
        ethClient.println(response);  
        Serial.println("Forwarded to Ethernet: " + response);
      }
    }

    // Close connections
    ethClient.stop();
    wifiClient.stop();
    Serial.println("Client disconnected.");
  }
}

void forwardUDP() {
  // Check for available UDP data on Ethernet side
  int packetSize = wifiUdp.parsePacket();
  if (packetSize) {
    // Read the packet into buffer
    char buffer[UDP_BUFFER_SIZE];
    int len = wifiUdp.read(buffer, UDP_BUFFER_SIZE);
    buffer[len] = 0; 

    // Forward to Wi-Fi using the gateway IP
    IPAddress wifiGateway = WiFi.gatewayIP(); 
    wifiUdp.beginPacket(wifiGateway, 80); 
    wifiUdp.write((uint8_t*)buffer, len); 
    wifiUdp.endPacket();

    Serial.print("Forwarded UDP packet to Wi-Fi: ");
    Serial.println(buffer);
  }
}
