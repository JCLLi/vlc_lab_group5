#include <Arduino.h>
#include <Vector.h>
#include "CRC16.h"

const int ledR= 38; // GPIO for controlling R channel
const int ledG= 42; // GPIO for controlling G channel
const int ledB= 34; // GPIO for controlling B channel

int britnessR = 255; // Default: lowest brightness
int britnessG = 255; // Default: lowest brightness
int britnessB = 255; // Default: lowest brightness

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200); // Set the Baud rate to 115200 bits/s
  while (Serial.available() == 0) {}  

  pinMode(ledR, OUTPUT);
  pinMode(ledG, OUTPUT);
  pinMode(ledB, OUTPUT);

  analogWrite(ledG, britnessG); // Turn OFF the G channel
  analogWrite(ledB, britnessB); // Turn OFF the B channel
  analogWrite(ledR, britnessB);
}

int preamble = 0xAAAAAA;
CRC16 crc;

void loop() {
  while (Serial.available() == 0) {}     //wait for data available

  String input =  Serial.readString();//get user input
  int length = input.length();

  int bin_array[(3 + 2 + 256 + 2) * 8];
  Vector<int> bin(bin_array);//create vector for bin data

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
  
  for(uint8_t byte: bytes_for_crc) {
    crc.add(byte);
  }
  uint16_t res = crc.calc();//calculate CRC

  for(int i = 15; i >= 0; i--){
    bin.push_back(bitRead(res, i));//add CRC bits to bin bit vector.
  }

  int a = 0;
  for(int bit: bin){
    a++;
    Serial.print(bit);
    if(a % 8 == 0) Serial.println();
  }
  data_transmission(bin);
  analogWrite(ledR, 256);
}

void data_transmission(Vector<int> bin){
  for(int bit: bin){
    analogWrite(ledR,bit * 255);
    Serial.print(!bit);
    delay(100); // TX frequency:  1s/400ms = 2.5 Hz
    analogWrite(ledR, !bit * 255);
    Serial.println(bit);
    delay(100);
  }
}