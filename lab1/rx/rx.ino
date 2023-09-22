/*
  test_rx.ino: testing the VLC receiver
  Course: CESE4110 Visible Light Communication & Sensing
*/


/*
 * The VLC receiver is equipped with an OPT101 photodiode. 
 * Pin 5 of the OPT101 is connected to A0 of the Arduino Due
 * Pin 1 of the OPT101 is connected to 5V of the Arduino Due
 * Pin 8 of the OPT101 is connected to GND of the Arduino Due
 */
#define PD A0 // PD: Photodiode
#include <Vector.h>
#include <Arduino.h>
#include "CRC16.h"

CRC16 crc;
unsigned long period = 100000;

const int threshold = 500;
const int upper_threshould = 950;

const int ELEMENT_COUNT_MAX = 5 + 255 + 2;

int preamble[] = {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0};

int data = -1;
int count = 0;
int currentIndex = 0;
int messageLength = 0;


unsigned long time_start = 0;
unsigned long time_stop = 0;
unsigned long delay_time = 0;
unsigned long loop_end = 0;

int receivedData_a[ELEMENT_COUNT_MAX * 8 * 2];
int resultVector_a[ELEMENT_COUNT_MAX * 8];
uint8_t byteData_a[ELEMENT_COUNT_MAX];
char decodeResult_a[ELEMENT_COUNT_MAX];

Vector<int> receivedData(receivedData_a);
Vector<int> resultVector(resultVector_a);
Vector<char> decodeResult(decodeResult_a);

int light = 0;
int dark = 0;
int last = 0;

int nums = 0;
int cycle = 0;


void setup() {
    Serial.begin(115200);
}

void loop() {
  time_start = micros();
  cycle = !cycle;
 
  while(light != 5 || dark != 5){ 
    uint32_t value = analogRead(PD);
    delay(10);
    if (value > upper_threshould){
      if (last < 200) {
        light++;
        // Serial.print(1);
      }
    }
    if (value < 200){
      if(last > upper_threshould) {
        dark++;
        // Serial.print(0);
      }
    }
    last = value;
  }
  // read light intensity
  int lightIntensity = analogRead(PD);

  if (lightIntensity > threshold) 
    data = 1; 
  else 
    data = 0;

  count += 1;

  // store data into vector receivedData
  receivedData.push_back(data);
  
  //Manchester decoding: 10 -> 1, 01 -> 0
  if (currentIndex + 1 < receivedData.size() || count % 2 == 0) {
    int firstValue = receivedData[currentIndex];
    int secondValue = receivedData[currentIndex + 1];
    int bin_bit = 0;

    //Decoding
    if (firstValue == 1 && secondValue == 0) {
      bin_bit = 1;
    } else if (firstValue == 0 && secondValue == 1) {
      bin_bit = 0;
    }

    resultVector.push_back(bin_bit); 
    currentIndex += 2;
    //Check preamble
    if(resultVector.size() < 24){
      if (bin_bit != preamble[resultVector.size() - 1]) {
        receivedData.clear();
        resultVector.clear();
        count = 0;
        currentIndex = 0;
        // Serial.println("Preamble is Wrong!");
      }
    }
  }

  if (resultVector.size() == 5 * 8){
    messageLength = readMessageLength();
  }

  if (resultVector.size() == (messageLength + 5 + 2) * 8) {
    // calculate CRC and campare with the recieved CRC
    uint16_t receivedCRC = readCRC();
    uint16_t calculatedCRC = calculateCRC();
  
    if (receivedCRC == calculatedCRC) {
      Serial.println("Message CRC OK");
      msgDecoding();
    } else {
      Serial.println("Message CRC Error");
    }
    receivedData.clear();
    resultVector.clear();
    count = 0;
    currentIndex = 0;
  } 
  time_stop = micros();
  // Serial.print("time: ");Serial.println(time2 - time1);
  delay_time = period - (time_stop - time_start) % period - 2 - 128;
  delayMicroseconds(delay_time); 
  // loop_end = micros();
  // Serial.println(loop_end - time_start);
  Serial.println(!cycle);

}





/*********************************** sub functions ***********************************/
// message length to DEC
int readMessageLength() {
  int messageLength = 0;
  for (int i = 0; i < 2 * 8; i++) {
    // Serial.print(i+24);
    if (resultVector[i + 3 * 8] == 1) {
        messageLength += pow(2, 15 - i);//Changed
    }
  }
  return messageLength;
}

uint16_t readCRC() {
  uint16_t CRC = 0;
  for (int i = 0; i < 16; i++) {
    CRC = (CRC << 1) | resultVector[resultVector.size() - 2 * 8 + i]; //changed
  }
  return CRC;
}

// calculate CRC and store byte data into vector byteData
uint16_t calculateCRC() {
  int size = 5 + readMessageLength();
  crc.reset();
  for (int i = 0; i < size; i++) {
    uint8_t byte = readDataForCRC(i);
    crc.add(byte);
  } 
  uint16_t crcValue = crc.calc();
  return crcValue;
}

// read msg bits to bytes
uint8_t readDataForCRC(int n) {
  uint8_t oneByte = 0;
  for (int i = 0; i < 8; i++) {
    oneByte = (oneByte << 1) | resultVector[n * 8 + i];
  }
  return oneByte;
}

int msgDecoding(){
  char cbyte = 0;
  for(int i = 0; i < messageLength; i++){
    cbyte = 0;
    for(int j = 0; j < 8; j++){
      cbyte = (cbyte << 1) | resultVector[40 + i * 8 + j];
    }
    Serial.print(cbyte);
  }
  Serial.println();
}