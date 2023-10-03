#include <Arduino.h>
#include <Vector.h>
#include "CRC16.h"
#include "SAMDUETimerInterrupt.h"

#define PERIOD 488 // Microsecond
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

// Preset preamble
int preamble = 0xAAAAAA;

// Functional flags
int bin_index = 0;
int start = 0;
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
    if(bin_index % 2 == 0){
      analogWrite(ledR, !bin[bin_index / 2] * 255);
      // Serial.print(bin[bin_index / 2]);
    } else if(bin_index % 2 == 1){
      analogWrite(ledR, bin[bin_index / 2] * 255);
      // Serial.print(!bin[bin_index / 2]);
    }
    bin_index++;
  }

  // Finish transmission
  if(bin_index == bin.size() * 2 && bin_index != 0){
    bin.clear();
    Serial.println("done");
    analogWrite(ledR, 255);
    bin_index = 0;
    tx_ready = false;
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
  if(Serial.available() != 0){ // Wait for data available
    String input =  Serial.readString();// Get user input
    int length = input.length();
    for(int i = 23; i >= 0; i--){
      bin.push_back(bitRead(preamble, i)); // Convert preamble bytes to bin bits and add into the vector
    }
    for(int i = 15; i >= 0; i--){
      bin.push_back(bitRead(length, i)); // Convert length bytes to bin bits and add into the vector
    }
    char payload[length + 1];
    input.toCharArray(payload, length + 1); // Convert input string to chars
    int index = 0;
    for(char onechar: payload){
      if(index != length){
        for(int i = 7; i >= 0; i--){
            bin.push_back(bitRead(onechar, i)); // Add bin bits into the vector
        }
      }
      index++;
    }
    uint8_t bytes_for_crc[3 + 2 + length];
    for(int i = 0; i < 5 + length; i++){
      uint8_t onebyte = 0;
      for(int j = 0; j < 8; j++){
        onebyte |= bin[i * 8 + j];
        if(j != 7) onebyte = onebyte << 1;
      }
      bytes_for_crc[i] = onebyte; // Get bytes used for calculating CRC
    }
    crc.restart();
    for(uint8_t byte: bytes_for_crc) {
      crc.add(byte);
    }
    uint16_t res = crc.calc(); // Calculate CRC
    for(int i = 15; i >= 0; i--){
      bin.push_back(bitRead(res, i)); // Add CRC bits to bin bit vector.
    }

    tx_ready = true;
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
