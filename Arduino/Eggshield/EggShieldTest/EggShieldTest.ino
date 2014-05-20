

#include "Wire.h"
#include "EggBus.h"
#include "DHT.h"

#define DHTPIN A3
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);
EggBus eggBus;

float getSensorValueByName(char * sensor_name);

void setup(void){
  Serial.begin(115200); 
  Serial.println("Egg Shield Test!");  
  dht.begin();
}

void loop(void){
  
  Serial.print("NO2(ppb): ");
  Serial.println(getSensorValueByName("NO2"), 3);
  
  Serial.print("CO(ppb): ");
  Serial.println(getSensorValueByName("CO"), 3);
  
  Serial.print("Temperature(Celsius): ");
  Serial.println(getSensorValueByName("Temperature"), 3); 
  
  Serial.print("Humidity(%): ");
  Serial.println(getSensorValueByName("Humidity"), 3); 
  
  delay(1000);
}

float getSensorValueByName(char * sensor_name){

  if(strcmp("Temperature", sensor_name) == 0){
      return dht.readTemperature();
  }
  
  if(strcmp("Humidity", sensor_name) == 0){
    return dht.readHumidity();
  }
  
  eggBus.init();
  while(eggBus.next()){  
    uint8_t numSensors = eggBus.getNumSensors();
    for(uint8_t ii = 0; ii < numSensors; ii++){      
      if(strcmp(eggBus.getSensorType(ii), sensor_name) == 0){
         return eggBus.getSensorValue(ii);
      }       
    }    
  }

  return 0.0f;
}
