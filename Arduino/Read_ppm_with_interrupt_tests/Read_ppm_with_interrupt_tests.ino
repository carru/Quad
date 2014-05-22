volatile int16_t timeNow = 0;
volatile int16_t timeBefore = 0;
volatile int delta = 0;
volatile int currentChannel = 0;

#define PPM_in_pin 18 //pin the interrupt is attached to
#define interrupt 5 // the interrupt associated with PPM_in_pin
//#define PPM_PulseLen 400 // Pulse length in microseconds
#define chanel_number 8  // Number of chanels
int ppm_in[chanel_number]; // RC PPM values are stored here

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

void setup(){
  pinMode(PPM_in_pin, INPUT);
  attachInterrupt(interrupt, intHandler, RISING);
  Serial.begin(115200);
}

void loop() {
  for (int i = 0; i <= chanel_number-1; i++) {
    Serial.print(i+1);
    Serial.print(": ");
    Serial.print(ppm_in[i]);
    Serial.print("  ");
  }
  Serial.println();
}

