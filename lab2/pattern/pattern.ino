#include <Arduino.h>
#include <Vector.h>
#include "CRC16.h"
#include "SAMDUETimerInterrupt.h"

#define PERIOD 300   // Microsecond
#define NOP __asm__ __volatile__ ("nop\n\t") // Inline assebly instruction to delay one clock cycle
CRC16 crc;

// Pin setting for LED
const int ledR= 38; // GPIO for controlling R channel
const int ledG= 42; // GPIO for controlling G channel
const int ledB= 34; // GPIO for controlling B channel

// Setting for LED brightness
int britnessR = 255; // Default: lowest brightness
int britnessG = 255; // Default: lowest brightness
int britnessB = 255; // Default: lowest brightness


// Functional flags
int bin_index = 0;
bool start = false;
bool tx_ready = false;

// Timer
DueTimerInterrupt dueTimerInterrupt = DueTimer.getAvailable();

// Vector used to store data needs to be sent
int bin_array[(3 + 2 + 256 + 2) * 8];
Vector<int> bin(bin_array);//create vector for bin data

// Interrupt handler
void TimerHandler(){
  // start = !start;
  // Serial.println(start);

  // Data transmission
  if(tx_ready){
      analogWrite(ledR, !bin[bin_index] * 255);
      bin_index++;

    // Finish transmission
    if(bin_index == bin.size()){
      bin_index = 0;
    }
  }

}


void setup() {
  Serial.begin(115200); // Set the Baud rate to 115200 bits/s
  while (Serial.available() == 0) {}  

  pinMode(ledR, OUTPUT);
  pinMode(ledG, OUTPUT);
  pinMode(ledB, OUTPUT);

  analogWrite(ledG, britnessG); // Turn OFF the G channel
  analogWrite(ledB, britnessB); // Turn OFF the B channel
  analogWrite(ledR, britnessB); // Turn OFF the R channel
}

void loop() {
  if(Serial.available() != 0){
    String a = Serial.readString();
    tx_ready = true;
  }
  if(!start){ // Wait for data available
    int a = 1;
    int index = 0;
    bin.push_back(1);
    bin.push_back(0);
    bin.push_back(1);
    bin.push_back(0);
    bin.push_back(1);
    bin.push_back(0);
    bin.push_back(1);
    bin.push_back(0);
    start = !start;
    attachDueInterrupt(PERIOD, TimerHandler, "ITimer0");
  }

  
}

// Timer interrupt setup
uint16_t attachDueInterrupt(double microseconds, timerCallback callback, const char* TimerName)
{
  
  dueTimerInterrupt.restartTimer();
  
  dueTimerInterrupt.attachInterruptInterval(microseconds, callback);

  uint16_t timerNumber = dueTimerInterrupt.getTimerNumber();
  
  Serial.print(TimerName); Serial.print(F(" attached to Timer(")); Serial.print(timerNumber); Serial.println(F(")"));

  return timerNumber;
}
