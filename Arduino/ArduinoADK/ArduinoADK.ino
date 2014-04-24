#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include "commands.h"

#define BUFFER_SIZE_FOR_IO 256
byte bufferI[BUFFER_SIZE_FOR_IO];
byte bufferO[BUFFER_SIZE_FOR_IO];

//AndroidAccessory acc("Manufacturer", "Model", "Description","Version", "URI", "Serial");
AndroidAccessory acc("UPC", "ArduinoADK", "Description","1.0", "URI", "Serial");

void setup() {
  Serial.begin(115200);
  acc.powerOn();
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

void attendCommand(byte command) {
  long sensorData;

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

  default:
    break;
  }
}

void loop() {
  byte receivedCommand;

  if (acc.isConnected()) {
    // Read command
    int len = acc.read(bufferI, sizeof(bufferI), 1);

    if (len == 1) {
      // Is a command (1 byte)
      receivedCommand = bufferI[0];

      attendCommand(receivedCommand);
    }
    else {
      // Not a command, ignore
    }
  }
}

