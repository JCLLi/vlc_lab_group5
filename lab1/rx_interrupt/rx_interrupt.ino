#define PD A0 // PD: Photodiode
#include <Vector.h>
#include <Arduino.h>
#include "CRC16.h"
#include "SAMDUETimerInterrupt.h"

#define PERIOD 488 //microsecond 190 min
CRC16 crc;

// 14cm: 28, 48, 10
// 13cm: 36, 63, 25
// 12cm: 40, 67, 13
// 11cm: 46, 75, 17
// 10cm: 48, 86, 10
// 9cm : 52, 94, 10
// 8cm : 64, 108, 10
// 7cm : 81, 145, 15
// 6cm : 104, 191, 15
// 5cm : 148, 276, 15
// 4cm : 215, 415, 16
// 3cm : 285, 554, 17
// 2cm : 480, 945, 15
// 1cm : 512, 1018, 16
int threshold = 0;//423,//148 48
int upper_threshold = 0;
int lower_threshold = 0;
int threshold_bias = 3;

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
int times = 0;
bool wait = false;
bool cali = true;
DueTimerInterrupt dueTimerInterrupt = DueTimer.getAvailable();

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
  if(Serial.available() != 0){
    String input = Serial.readString();
    if(input.equals("cali")){
      dueTimerInterrupt.stopTimer();
      cali = true;
    }else if(input.equals("restart")){
      dueTimerInterrupt.stopTimer();
      upper_threshold -= threshold_bias;
      lower_threshold += threshold_bias;
      times = 0;
      light = 0;
      dark = 0;
      cali = false;
    }
  }
  // put your main code here, to run repeatedly:
  if(cali){
    calibration();
  }
  else{
    while(light != 10 || dark != 10){ 
      if(!wait){
        delay(100);
        wait = true;
      }
      uint32_t value = analogRead(PD);
      if(abs(value - lower_threshold) < 10)
        value = lower_threshold - 1;
      if(abs(value - upper_threshold) < 10)
        value = upper_threshold + 1;
      delay(10);
      if (value > upper_threshold){
        if (last < lower_threshold) {
          light++;
          Serial.print(1);
        }
      }
      if (value < lower_threshold){
        if(last > upper_threshold) {
          dark++;
          Serial.print(0);
        }
      }
      // Serial.print("light: ");Serial.print(light);Serial.print(" dark: ");Serial.println(dark);
      last = value;
      if(dark == 10){
        attachDueInterrupt(PERIOD, TimerHandler, "ITimer0");
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
    Serial.print("rcrc: ");Serial.println(receivedCRC);
    Serial.print("ccrc: ");Serial.println(calculatedCRC);
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
  dueTimerInterrupt.restartTimer();
  
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

int calibration(){
    int read_value = analogRead(PD);
    if(times < 250){
      times++;
      lower_threshold += read_value;
    }
    if(times == 250){
      lower_threshold = lower_threshold / 250;
      upper_threshold = read_value;
      times++;
    }
    if(times == 251 && read_value > upper_threshold)
      upper_threshold = read_value;
    threshold = (lower_threshold + threshold_bias) + ((upper_threshold - threshold_bias) - lower_threshold - threshold_bias) / 2;
    Serial.print("min: ");Serial.print(lower_threshold + threshold_bias);Serial.print(" max: ");Serial.print(upper_threshold - threshold_bias);Serial.print(" average = ");Serial.print(threshold);Serial.print(" ");Serial.println(read_value);
    delayMicroseconds(10000); // two times per second
}
