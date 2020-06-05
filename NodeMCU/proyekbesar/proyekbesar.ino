
#include <ESP8266WiFi.h>

const char* ssid = "xSpendiqL";
const char* password = "Bojonegoro1";

uint8_t Pwm1 = D1; //Nodemcu PWM pin 
uint8_t Pwm2 = D2; //Nodemcu PWM pin

//Seven segment pins attachecd with nodemcu pins  
int a0 = 15;  //Gpio-15 of nodemcu esp8266  
int a1 = 13;  //Gpio-13 of nodemcu esp8266    
int a2 = 12;  //Gpio-12 of nodemcu esp8266   
int a3 = 14;  //Gpio-14 of nodemcu esp8266   

// Create an instance of the server
// specify the port to listen on as an argument
WiFiServer server(80);
int ledPin = 13;

void setup() {
  pinMode(a0, OUTPUT);
  pinMode(a1, OUTPUT);
  pinMode(a2, OUTPUT);
  pinMode(a3, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
  delay(10);
  // Connect to WiFi network
  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");
  
  // Start the server
  server.begin();
  Serial.println("Server started");

  // Print the IP address
  Serial.println(WiFi.localIP());
}

void loop() {

  digitalWrite(LED_BUILTIN, LOW);
  // Check if a client has connected
  WiFiClient client = server.available();
  if (!client) {
    return;
  }
  
  // Wait until the client sends some data
  Serial.println("new client");
  while(!client.available()){
    delay(1);
  }
  
  // Read the first line of the request
  String req = client.readStringUntil('\r');
  Serial.println(req);
  client.flush();
  
  // Match the request
  int val;
  if (req.indexOf("/control/Backward") != -1)  // led=on
    val = 0;
  else if (req.indexOf("/control/Forward") != -1)
    val = 1;
  else if (req.indexOf("/control/Left") != -1)
    val = 2;
  else if (req.indexOf("/control/Right") != -1)
    val = 3;
  else {
    Serial.println("Stationary");
    return;
  }

  // Set GPIO2 according to the request
  digitalWrite(ledPin, val);
  
  client.flush();

   // Return the response
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text/html");
  client.println(""); //  do not forget this one
 
  if(val == 0) {
    analogWrite(Pwm1, 512);
    analogWrite(Pwm1, 512);
    Serial.println("Backward");
    digitalWrite(a0, LOW);
    digitalWrite(a1, HIGH);

    digitalWrite(a2, LOW);
    digitalWrite(a3, HIGH);
    }
  else if (val == 1) {
    analogWrite(Pwm1, 512);
    analogWrite(Pwm1, 512);
    Serial.println("Forward");
    digitalWrite(a0, HIGH);
    digitalWrite(a1, LOW);

    digitalWrite(a2, HIGH);
    digitalWrite(a3, LOW);
    }
  else if (val == 2) { 
    analogWrite(Pwm1, 512);
  analogWrite(Pwm1, 512);
    Serial.println("Left");
    digitalWrite(a0, LOW);
    digitalWrite(a1, LOW);

    digitalWrite(a2, HIGH);
    digitalWrite(a3, LOW);
  }
  else if( val == 3) {
    analogWrite(Pwm1, 512);
    analogWrite(Pwm1, 512);
    Serial.println("Right");
    digitalWrite(a0, HIGH);
    digitalWrite(a1, LOW);

    digitalWrite(a2, LOW);
    digitalWrite(a3, LOW);
  }
 
  delay(10);

  // The client will actually be disconnected 
  // when the function returns and 'client' object is detroyed
}
