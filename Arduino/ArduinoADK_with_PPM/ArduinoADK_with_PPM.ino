#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include "commands.h"

///////////////////////// ADK CONFIG /////////////////////////////
#define BUFFER_SIZE_FOR_IO 256
byte bufferI[BUFFER_SIZE_FOR_IO];
byte bufferO[BUFFER_SIZE_FOR_IO];

//("Manufacturer", "Model", "Description","Version", "URI", "Serial");
AndroidAccessory acc("UPC", "ArduinoADK", "Description","1.0", "URI", "Serial");
//////////////////////////////////////////////////////////////////

///////////////////////// RC CONFIG //////////////////////////////
#define chanel_number 8  // Number of chanels
#define PPM_FrLen 27000  // PPM frame length in microseconds (1ms = 1000µs)
#define PPM_PulseLen 400 // Pulse length
#define onState 0        // Polarity of the pulses: 1 is positive, 0 is negative
#define ppmOutPin 10     // PPM signal output pin on the arduino
#define ppmInPin 4       // PPM signal input pin on the arduino
#define switchChannel 7  // Channel with the switch to toggle auto and manual

int ppm[chanel_number];    // PPM generator reads these values
int ppm_in[chanel_number]; // RC PPM values are stored here

boolean mode;
#define AUTO true
#define MANUAL false

#define MODE_LOITTER 1325 // Values from the quad
#define MODE_ALTHOLD 1783
//////////////////////////////////////////////////////////////////

void setup() {
  Serial.begin(115200);
  Serial.println("Start");


  // ADK
  acc.powerOn();


  // PPM
  pinMode(ppmInPin, INPUT);
  pinMode(ppmOutPin, OUTPUT);
  digitalWrite(ppmOutPin, !onState);  //set the PPM signal pin to the default state (off)

  // Setup timer1 (ppm output)
  cli();
  TCCR1A = 0; // set entire TCCR1 register to 0
  TCCR1B = 0;

  OCR1A = 100;  // compare match register, change this
  TCCR1B |= (1 << WGM12);  // turn on CTC mode
  TCCR1B |= (1 << CS11);  // 8 prescaler: 0,5 microseconds at 16mhz
  TIMSK1 |= (1 << OCIE1A); // enable timer compare interrupt
  sei();
  
  
  // Initialize ppm values
  setLoitter();
}

long readSensor() {
  //return random(-2147483648, 2147483647); // whole long range
  return random(-1024000, 1024000);
}

void sendSensorData(byte sensor, long value) {
  bufferO[0] = sensor;

  // long to bytes (4)
  bufferO[4] = (byte) value;
  bufferO[3] = (byte) (value >> 8);
  bufferO[2] = (byte) (value >> 16);
  bufferO[1] = (byte) (value >> 24);

  acc.write(bufferO, 5);
}

void setPPMChannel(int channel, int value) {
  if (mode == MANUAL) { return; }
  
  // Reject out of range values
  if (value < 1000 || value > 2000) { return; }
  
  ppm[channel-1] = value;
}

void attendCommand(byte command, int value) {
  long sensorData;
  int ch;

  switch(command) {
  case READ_SENSOR_1:
    Serial.println("Reading sensor 1 ...");
    sensorData = readSensor();
    Serial.print("Sensor 1 value: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_1, sensorData);
    break;

  case READ_SENSOR_2:
    Serial.println("Reading sensor 2 ...");
    sensorData = readSensor();
    Serial.print("Sensor 2 value: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_2, sensorData);
    break;

  case READ_SENSOR_3:
    Serial.println("Reading sensor 3 ...");
    sensorData = readSensor();
    Serial.print("Sensor 3 value: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_3, sensorData);
    break;

  case SET_CH1:
  case SET_CH2:
  case SET_CH3:
  case SET_CH4:
  case SET_CH5:
  case SET_CH6:
  case SET_CH7:
  case SET_CH8:
    ch = command & 0x0F; // Get channel number (lower 4 bits)
    setPPMChannel(ch, value);
    break;

  default:
    break;
  }
}

void setLoitter() {
  setPPMChannel(1, 1500);
  setPPMChannel(2, 1500);
  setPPMChannel(3, 1500);
  setPPMChannel(4, 1500);
  setPPMChannel(5, MODE_LOITTER);
  setPPMChannel(6, 1500); // Not used
  setPPMChannel(8, 1500); // Not used
}

void loop() {
  byte receivedCommand;


  // Read PPM frame
  if (pulseIn(ppmInPin, HIGH) > 3000) // New PPM frame starts after this long pulse
  {
    for (int i = 0; i <= chanel_number-1; i++) { // Read channels
      ppm_in[i] = pulseIn(ppmInPin, HIGH) + PPM_PulseLen;
    }
    
    
    
//    for(int i = 0; i <= chanel_number-1; i++)
//  {
//    Serial.print("CH");
//    Serial.print(i+1);
//    Serial.print(": ");
//    if (i!=chanel_number-1) {
//      Serial.print(ppm_in[i]);
//      Serial.print("  ");
//    } 
//    else {
//      Serial.println(ppm_in[i]);
//    }
//  }
  }
  
  
  // Check RC mode
  if (ppm_in[switchChannel-1] > (900 + 1900)/2) {
    // Switch on high position (auto)
    if (mode == MANUAL) {
      // Toggled from manual to auto
      mode = AUTO;
      // Set channels to neutral position (loitter)
      setLoitter();
    } else {
      // Was already in auto mode
      // Do nothing
    }
  }
  else {
    // Switch on low position (manual)
    for (int i = 0; i <= chanel_number-1; i++) {
      ppm[i] = ppm_in[i];
    }
    mode = MANUAL;
  }

  // Manage ADK connection
  if (acc.isConnected()) {
    // Read command
    int len = acc.read(bufferI, sizeof(bufferI), 1);

    if (len == 1) {
      // Is a command (1 byte)
      receivedCommand = bufferI[0];

      attendCommand(receivedCommand, 0);
    }
    else if (len == 3) {
      // Command with 2 bytes of data
      int value = bufferI[1];
      value = value >> 8; // HSB read
      value = value + bufferI[2]; // LSB read
      
      attendCommand(receivedCommand, value);
    }
    else {
      // Not a command, ignore
    }
  }
}

// Do not modify (PPM signal generation)
ISR(TIMER1_COMPA_vect){
  static boolean state = true;

  TCNT1 = 0;

  if(state) {  // Start pulse
    digitalWrite(ppmOutPin, onState);
    OCR1A = PPM_PulseLen * 2;
    state = false;
  }
  else{  // End pulse and calculate when to start the next pulse
    static byte cur_chan_numb;
    static unsigned int calc_rest;

    digitalWrite(ppmOutPin, !onState);
    state = true;

    if(cur_chan_numb >= chanel_number){
      cur_chan_numb = 0;
      calc_rest = calc_rest + PPM_PulseLen;// 
      OCR1A = (PPM_FrLen - calc_rest) * 2;
      calc_rest = 0;
    }
    else{
      OCR1A = (ppm[cur_chan_numb] - PPM_PulseLen) * 2;
      calc_rest = calc_rest + ppm[cur_chan_numb];
      cur_chan_numb++;
    }     
  }
}

