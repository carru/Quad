#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include "commands.h"
#include "rc.h"
#include "Wire.h"
#include "EggBus.h"
#include "DHT.h"

///////////////////////// ADK CONFIG /////////////////////////////
#define BUFFER_SIZE_FOR_IO 256
byte bufferI[BUFFER_SIZE_FOR_IO];
byte bufferO[BUFFER_SIZE_FOR_IO];

//("Manufacturer", "Model", "Description","Version", "URI", "Serial");
AndroidAccessory acc("UPC", "ArduinoADK", "Description","1.0", "URI", "Serial");
//////////////////////////////////////////////////////////////////

///////////////////////// RC CONFIG //////////////////////////////
#define chanel_number 8  // Number of chanels
#define PPM_FrLen 27000  // PPM frame length in microseconds
#define PPM_PulseLen 400 // Pulse length in microseconds
#define onState 0        // Polarity of the pulses: 1 is positive, 0 is negative

#define ppmOutPin 49 // PPM signal output pin on the arduino
#define ppmInPin  18 // PPM signal input pin on the arduino
#define interrupt  5 // Interrupt associated with PPM_in_pin (http://arduino.cc/en/Reference/AttachInterrupt)

//#define PULSEIN_TIMEOUT 50000 // Timeout to read RC frames in microseconds

int ppm[chanel_number];    // PPM generator reads these values
int ppm_in[chanel_number]; // RC PPM values are stored here

boolean ledFramesToToggle = 5; // Toggle LED when we've received this many RC frames
int ledPort = 13;
int ledCount = 0;
boolean ledStatus = true;

boolean mode;
#define AUTO true
#define MANUAL false

// For the interrupt
volatile int16_t timeNow = 0;
volatile int16_t timeBefore = 0;
volatile int delta = 0;
volatile int currentChannel = 0;

#define THROTTLE_MIN 1150 // Throttle has a different minimum value
#define CH_MIN       1000
#define CH_NEUTRAL   1500
#define CH_MAX       2000
//////////////////////////////////////////////////////////////////

///////////////////////// EGGSHIELD //////////////////////////////
#define DHTPIN A3
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);
EggBus eggBus;
//////////////////////////////////////////////////////////////////

void intHandler() {
  timeNow = micros();

  // How long since last interrupt?
  delta = timeNow - timeBefore;

  if (delta > 3000) {
    currentChannel = 0; // Channel 1 will be in the next interrupt
  }
  else {
    currentChannel++;
    if (currentChannel > 0 && currentChannel <= chanel_number) {
      ppm_in[currentChannel - 1] = delta;
    }
  }

  // Save timeNOW for the next interrupt
  timeBefore = timeNow;
}

void setup() {
  Serial.begin(115200);
  Serial.println("Start");

  // Eggshield
  dht.begin();

  // Blinking LED (RC)
  pinMode(ledPort, OUTPUT);

  // ADK
  acc.powerOn();


  // PPM
  pinMode(ppmInPin, INPUT);
  attachInterrupt(interrupt, intHandler, RISING);
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
  setFlightMode(MODE_LOITTER); // Motors can't arm in Loitter mode
  setSlidersNeutralNoThrottle();

  // Start with auto mode (then it checks the switch)
  mode = AUTO;
}

void sendSensorData(byte sensor, float value) {
  bufferO[0] = sensor;

  // float to bytes (4)
  byte *bytePointer = (byte*) &value;
  bufferO[4] = *(bytePointer    );
  bufferO[3] = *(bytePointer + 1);
  bufferO[2] = *(bytePointer + 2);
  bufferO[1] = *(bytePointer + 3);

  acc.write(bufferO, 5);
}

void setPPMChannel(int channel, int value) {
  if (mode == MANUAL) { 
    return; 
  }

  // Reject out of range values
  if (value < CH_MIN || value > CH_MAX) { 
    return; 
  }
  if (channel == CH_THROTTLE && value < THROTTLE_MIN) {
    value = THROTTLE_MIN;
  }

  ppm[channel-1] = value;
}

void attendCommand(byte command, int value) {
  float sensorData;
  int ch;

  switch(command) {
  case READ_SENSOR_TEMPERATURE:
    sensorData = dht.readTemperature();
    Serial.print("Temperature [ºC]: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_TEMPERATURE, sensorData);
    break;

  case READ_SENSOR_HUMIDITY:
    sensorData = dht.readHumidity();
    Serial.print("Humidity [%]: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_HUMIDITY, sensorData);
    break;

  case READ_SENSOR_NO2:
    eggBus.init();
    eggBus.next();
    sensorData = eggBus.getSensorValue(0);
    Serial.print("NO2 [ppb]: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_NO2, sensorData);
    break;

  case READ_SENSOR_CO:
    eggBus.init();
    eggBus.next();
    sensorData = eggBus.getSensorValue(1);
    Serial.print("CO [ppb]: ");
    Serial.println(sensorData);

    sendSensorData(DATA_SENSOR_CO, sensorData);
    break;

  case SET_MODE_ALTHOLD:
    setFlightMode(MODE_ALTHOLD);
    break;

  case SET_MODE_LOITTER:
    setFlightMode(MODE_LOITTER);
    break;

  case SET_MODE_AUTO:
    setFlightMode(MODE_AUTO);
    break;

  case SET_MODE_RTL:
    setFlightMode(MODE_RTL);
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
    Serial.print("Unknown command");
    break;
  }
}

void setSlidersNeutral() {
  setPPMChannel(CH_ROLL, CH_NEUTRAL);
  setPPMChannel(CH_PITCH, CH_NEUTRAL);
  setPPMChannel(CH_THROTTLE, CH_NEUTRAL);
  setPPMChannel(CH_YAW, CH_NEUTRAL);

  setPPMChannel(CH_SWITCH, CH_MIN); // No simple mode

  // Unused channels
  setPPMChannel(6, CH_MIN);
  setPPMChannel(8, CH_MIN);
}

void setSlidersNeutralNoThrottle() {
  setPPMChannel(CH_ROLL, CH_NEUTRAL);
  setPPMChannel(CH_PITCH, CH_NEUTRAL);
  setPPMChannel(CH_THROTTLE, THROTTLE_MIN);
  setPPMChannel(CH_YAW, CH_NEUTRAL);

  setPPMChannel(CH_SWITCH, CH_MIN); // No simple mode

  // Unused channels
  setPPMChannel(6, CH_MIN);
  setPPMChannel(8, CH_MIN);
}

void setFlightMode(int mode) {
  setPPMChannel(CH_MODE, mode);
}

void loop() {
  byte receivedCommand;

  // DEBUG print pwm values from the receiver
  /*for (int i = 0; i <= chanel_number-1; i++) {
    Serial.print(i+1);
    Serial.print(": ");
    Serial.print(ppm_in[i]);
    Serial.print("  ");
  }
  Serial.println();*/

  // Check RC mode
  if (ppm_in[CH_SWITCH-1] > CH_NEUTRAL) { // Switch on high position (auto)
    if (mode == MANUAL) {
      // Toggled from manual to auto
      mode = AUTO;
      // Set channels to neutral position (loitter).
      setFlightMode(MODE_LOITTER);
      setSlidersNeutral();
    } 
    else {
      // Was already in auto mode
      // Do nothing
    }
  }
  else { // Switch on low position (manual)
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
      // Command (1 byte)
      receivedCommand = bufferI[0];

      attendCommand(receivedCommand, 0);
    }
    else if (len == 3) {
      // Command with 2 bytes of data
      receivedCommand = bufferI[0];
      int value = bufferI[1];
      value = value << 8; // HSB read
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
      calc_rest = calc_rest + PPM_PulseLen;
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


