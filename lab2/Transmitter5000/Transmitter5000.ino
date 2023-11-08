#include <Arduino.h>
#include <Vector.h>
#include "SAMDUETimerInterrupt.h"

#define PERIOD1 270   // Microsecond
#define PERIOD2 200   // Microsecond
#define PERIOD3 150   // Microsecond
#define PERIOD4 110
   // Microsecond


#define NOP __asm__ __volatile__ ("nop\n\t") // Inline assebly instruction to delay one clock cycle
float periods[4] = {200, PERIOD3, PERIOD2, PERIOD1};

int times = 0;
int period = 0;

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

  // Data transmission
  if(tx_ready){
      times++;
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
    bin.push_back(1);
    bin.push_back(0);
    bin.push_back(1);
    bin.push_back(0);
    bin.push_back(1);
    bin.push_back(0);
    bin.push_back(1);
    bin.push_back(0);
    start = !start;
    attachDueInterrupt(periods[period], TimerHandler, "ITimer0");
  }
  // if(dueTimerInterrupt.getPeriod() > periods[period] - 5 && dueTimerInterrupt.getPeriod() < periods[period] + 5){
    
  //   int one = 1 / (periods[period] / 1000000);
    
  //   if(period != 4 - 1){
  //     if(times >= one){
  //       Serial.println(one);
  //       dueTimerInterrupt.stopTimer();
  //       dueTimerInterrupt.restartTimer();
  //       period++;
  //       attachDueInterrupt(periods[period], TimerHandler, "ITimer0");
  //       times = 0;
  //     }
  //   }else{
      
  //     if(times >= one){
  //       Serial.println(one);
  //       dueTimerInterrupt.stopTimer();
  //       analogWrite(ledR, 255);
  //       times = 0;
  //     }
  //   }
  // }  
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
