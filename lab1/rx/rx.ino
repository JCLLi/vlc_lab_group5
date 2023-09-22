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

// const int PD = A0;
int threshold = 300;
int count = 0;
int messageLength = 0;
const unsigned long period = 5000;
int upper_threshould = 550;
int data = -1;
// const int preamble = 0xAAAAAA;
int preamble[] = {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0};

int times = 0;
int light = 0;
int dark = 0;
int last = 0;

const int ELEMENT_COUNT_MAX = 5 + 255 + 2;
int receivedData_a[ELEMENT_COUNT_MAX * 8 * 2];
int resultVector_a[ELEMENT_COUNT_MAX * 8];
uint8_t byteData_a[ELEMENT_COUNT_MAX];
char decodeResult_a[ELEMENT_COUNT_MAX];
int a = 0;
bool started = false;
Vector<int> receivedData(receivedData_a);
Vector<int> resultVector(resultVector_a);
Vector<char> decodeResult(decodeResult_a);
int currentIndex = 0;
unsigned long time3 = 0;
unsigned long time4 = 0;

/*
 * Some configurations
 */
void setup() {
    Serial.begin(115200);
}

/*
 * The Main function
 */
void loop() {
  unsigned long time1 = micros();
  while(light != 5 || dark != 5){
    uint32_t value = analogRead(PD);
    delay(10);
    if (value > upper_threshould){
      if (last < 200) {
        light++;
        Serial.print("light ");
        Serial.println(light);
      }
    }
    if (value < 200){
      if(last > upper_threshould) {
        dark++;
        Serial.print("dark ");
        Serial.println(dark);
      }
    }
    last = value;
  }

  a++;
  // read light intensity
  int lightIntensity = analogRead(PD);

  if (lightIntensity > threshold) 
    data = 1; 
  else 
    data = 0;
  // Serial.print(data);
  
  // if (a == 8) {
  //   Serial.println();
  //   a = 0;
  // }
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
  unsigned long time2 = micros();
  // Serial.print("time: ");Serial.println(time2 - time1);
  unsigned long delay = period - (time2 - time1) % period;
  delayMicroseconds(delay); 
}


// message length转换为十进制
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

int readCRC() {
  uint16_t CRC = 0;
  for (int i = 0; i < 16; i++) {
    CRC = (CRC << 1) | resultVector[resultVector.size() - 2 * 8 + i]; //changed
  }
  return CRC;
}

// calculate CRC and store byte data into vector byteData
int calculateCRC() {
  int size = 5 + readMessageLength();
  crc.reset();
  for (int i = 0; i < size; i++) {
    uint8_t byte = readDataForCRC(i);
    crc.add(byte);
  } 
  uint16_t crcValue = crc.calc();
  return crcValue;
}

// CRC前的数转换为8位为一组的二进制数
int readDataForCRC(int n) {
  uint8_t oneByte = 0;
  for (int i = 0; i < 8; i++) {
    oneByte = (oneByte << 1) | resultVector[n * 8 + i];
  }
  return oneByte;
}

int msgDecoding(){
  char a = 0;
  for(int i = 0; i < messageLength; i++){
    a = 0;
    for(int j = 0; j < 8; j++){
      a = (a << 1) | resultVector[40 + i * 8 + j];
    }
    Serial.print(a);
  }
  Serial.println();
}