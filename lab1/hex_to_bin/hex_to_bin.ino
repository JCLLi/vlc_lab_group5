#include <Arduino.h>
#include <Vector.h>
#include <CRC32.h>
void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200); // Set the Baud rate to 115200 bits/s
  while (Serial.available() == 0) {}  
}

int preamble = 0xAAAAAA;
int nn = 0b1100001;

void loop() {
  // put your main code here, to run repeatedly:
  while (Serial.available() == 0) {}     //wait for data available
  String input =  Serial.readString();
  Serial.println(input);
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
    for(int i = 7; i >= 0; i--){
      bin.push_back(bitRead(onechar, i));//add bin bits into the vector
    }
  }

  uint8_t bytes_for_crc[3 + 2 + length];
  for(int i = 0; i < 5 + length; i++){
    uint8_t onebyte = 0;
    for(int j = 0; j < 8; j++){
      onebyte |= bin[i * 8 + j];
      if(j != 7) onebyte = onebyte << 1;
    }
    Serial.println();
    bytes_for_crc[i] = onebyte; 
  }
  for(uint8_t byte: bytes_for_crc){
    Serial.println(byte, HEX);
  }
}
