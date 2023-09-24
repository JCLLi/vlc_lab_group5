#define PD A0 // PD: Photodiode
#include <Vector.h>
#include <Arduino.h>
#include "CRC16.h"
#include "SAMDUETimerInterrupt.h"

#define PERIOD 976 //microsecond
CRC16 crc;

const int threshold = 500;
const int upper_threshould = 950;
const int lower_threshould = 50;

const int ELEMENT_COUNT_MAX = 5 + 255 + 2;

int preamble[] = {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0};

int count = 0;
int currentIndex = 0;
int messageLength = 0;

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
int start = 0;
unsigned long time1 = 0;

void TimerHandler(){
  // start = !start;
  // Serial.println(start);
  nums++;
  int lightIntensity = analogRead(PD);
  int data = 0;
  if (lightIntensity > threshold) 
    data = 1; 
  // Serial.print(data);
  // if(nums == 8) {
  //   Serial.println();
  //   nums = 0;
  // }
  delayMicroseconds(1);
  if(count == 0){
    if(data == 1){
      receivedData.push_back(data);
      count++;
    }
  }else{
    receivedData.push_back(data);
    count++;
  }

  if (count % 2 == 0 && count != 0) {
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
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
}

void loop() {
  // put your main code here, to run repeatedly:
  while(light != 10 || dark != 10){ 
    uint32_t value = analogRead(PD);
    delay(1);
    if (value > upper_threshould){
      if (last < lower_threshould) {
        light++;
        // Serial.print(1);
      }
    }
    if (value < lower_threshould){
      if(last > upper_threshould) {
        dark++;
        // Serial.print(0);
        // time1 = micros();
      }
    }
    last = value;
    if(dark == 10){
      attachDueInterrupt(PERIOD, TimerHandler, "ITimer0");
      // unsigned long time2 = micros();
      // Serial.println(time2 - time1);
    }      
      
  }
  //Manchester decoding: 10 -> 1, 01 -> 0
  
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

}

uint16_t attachDueInterrupt(double microseconds, timerCallback callback, const char* TimerName)
{
  DueTimerInterrupt dueTimerInterrupt = DueTimer.getAvailable();
  
  dueTimerInterrupt.attachInterruptInterval(microseconds, callback);

  uint16_t timerNumber = dueTimerInterrupt.getTimerNumber();
  
  Serial.print(TimerName); Serial.print(F(" attached to Timer(")); Serial.print(timerNumber); Serial.println(F(")"));

  return timerNumber;
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
