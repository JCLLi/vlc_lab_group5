#include <Arduino.h>
#include <Vector.h>
#include "CRC16.h"

CRC16 crc;

const int ledR= 38; // GPIO for controlling R channel
const int ledG= 42; // GPIO for controlling G channel
const int ledB= 34; // GPIO for controlling B channel

int britnessR = 255; // Default: lowest brightness
int britnessG = 255; // Default: lowest brightness
int britnessB = 255; // Default: lowest brightness

int preamble = 0xAAAAAA;

unsigned long period = 100000; //microsecond

unsigned long time_start = 0;
unsigned long time_stop = 0;
unsigned long delay_time = 0;
unsigned long loop_end = 0;

int bin_index = 0;

int bin_array[(3 + 2 + 256 + 2) * 8];
Vector<int> bin(bin_array);//create vector for bin data


int times = 0;
int cycle = 0;

void setup() {
  Serial.begin(115200); // Set the Baud rate to 115200 bits/s
  while (Serial.available() == 0) {}  

  pinMode(ledR, OUTPUT);
  pinMode(ledG, OUTPUT);
  pinMode(ledB, OUTPUT);

  analogWrite(ledG, britnessG); // Turn OFF the G channel
  analogWrite(ledB, britnessB); // Turn OFF the B channel
  analogWrite(ledR, britnessB);
}

void loop() {
  time_start = micros();
  cycle = !cycle;
  if(Serial.available() != 0){ //wait for data available
    String input =  Serial.readString();//get user input
    if(input.equals("start")){
      int times = 0;
      while(times != 5){
        analogWrite(ledR, 0);
        delay(1000);
        analogWrite(ledR, 255);
        times++;
        if(times != 5)
          delay(1000);
      }
    }else{
      int length = input.length();
      for(int i = 23; i >= 0; i--){
        bin.push_back(bitRead(preamble, i));//convert preamble bytes to bin bits and add into the vector
      }
      for(int i = 15; i >= 0; i--){
        bin.push_back(bitRead(length, i));//convert length bytes to bin bits and add into the vector
      }
      char payload[length + 1];
      input.toCharArray(payload, length + 1);//convert input string to chars
      int index = 0;
      for(char onechar: payload){
        if(index != length){
          for(int i = 7; i >= 0; i--){
              bin.push_back(bitRead(onechar, i));//add bin bits into the vector
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
        bytes_for_crc[i] = onebyte; //get bytes used for calculating CRC
      }
      crc.restart();
      for(uint8_t byte: bytes_for_crc) {
        crc.add(byte);
      }
      uint16_t res = crc.calc();//calculate CRC
      for(int i = 15; i >= 0; i--){
        bin.push_back(bitRead(res, i));//add CRC bits to bin bit vector.
      }
    }
  }else if (bin.size() != 0){
    data_transmission();
  }
  time_stop = micros();
  delay_time = period - (time_stop - time_start) % period - 2 - 125;
  delayMicroseconds(delay_time);
  // loop_end = micros();
  // Serial.println(loop_end - time_start);
  Serial.println(!cycle);
}





/*********************************** sub functions ***********************************/
void data_transmission(){
  if(bin_index % 2 == 0){
    analogWrite(ledR, !bin[bin_index / 2] * 255);
    // Serial.print(bin[bin_index / 2]);
  } else if(bin_index % 2 == 1){
    analogWrite(ledR, bin[bin_index / 2] * 255);
    // Serial.print(!bin[bin_index / 2]);
  }
    
  bin_index++;
  // if(bin_index % 8 == 0)
  //   Serial.println();

  if(bin_index == 2 * bin.size()){
    bin.clear();
    Serial.println("done");
    analogWrite(ledR, 255);
    bin_index = 0;
  }
}