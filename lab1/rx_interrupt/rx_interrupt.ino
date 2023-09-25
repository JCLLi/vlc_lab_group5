#define PD A0 // PD: Photodiode
#include <Vector.h>
#include <Arduino.h>
#include "CRC16.h"
#include "SAMDUETimerInterrupt.h"

#define PERIOD 488 // Microsecond, 190 min
CRC16 crc;

// Threshold for decoding and alignment
int threshold = 0;
int upper_threshold = 0;
int lower_threshold = 0;
int threshold_bias = 3;

// Max payload size
const int ELEMENT_COUNT_MAX = 5 + 255 + 2;

int preamble[] = {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0};

// Values used for decoding
int count = 0;
int currentIndex = 0;
int messageLength = 0;

// Vector used to store received data
int receivedData_a[ELEMENT_COUNT_MAX * 8 * 2];
int resultVector_a[ELEMENT_COUNT_MAX * 8];
uint8_t byteData_a[ELEMENT_COUNT_MAX];
char decodeResult_a[ELEMENT_COUNT_MAX];
Vector<int> receivedData(receivedData_a);
Vector<int> resultVector(resultVector_a);
Vector<char> decodeResult(decodeResult_a);

// Flags used for alignment
int light = 0;
int dark = 0;
int last = 0;

// functional flags
int nums = 0;
int start = 0;
int times = 0;
bool wait = false;
bool cali = true;

// Timer
DueTimerInterrupt dueTimerInterrupt = DueTimer.getAvailable();

// Interrupt handler
void TimerHandler(){
  // start = !start;
  // Serial.println(start);
  
 // Light detection and convert in bin bits
  int lightIntensity = analogRead(PD);
  int data = 0;
  if (lightIntensity > threshold) 
    data = 1; 

  // nums++;
  // Serial.print(data);
  // if(nums == 8) {
  //   Serial.println();
  //   nums = 0;
  // }
  delayMicroseconds(1);

 // Store data
  if(count == 0){
    if(data == 1){
      receivedData.push_back(data);
      count++;
    }
  }else{
    receivedData.push_back(data);
    count++;
  }

 // Check preamble
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
      }
    }
  }
}


void setup() {
  Serial.begin(115200);
}

void loop() {
// Function for restarting or calibration
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

// Calibration or alignment
  if(cali){
    calibration();
  }
  else{
    // Alignment: receive repeteaed 1 and 0 10 times
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
      last = value;
      if(dark == 10){
        // Alignment ends, timer starts
        attachDueInterrupt(PERIOD, TimerHandler, "ITimer0");
      }      
    }
  }

  // Calculate payload length
  if (resultVector.size() == 5 * 8){
    messageLength = readMessageLength();
  }

  // Check CRC and decode data to readble message
  if (resultVector.size() == (messageLength + 5 + 2) * 8) {
    // Calculate CRC and campare with the recieved CRC
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

// Timer interrupt setting
uint16_t attachDueInterrupt(double microseconds, timerCallback callback, const char* TimerName)
{
  dueTimerInterrupt.restartTimer();
  
  dueTimerInterrupt.attachInterruptInterval(microseconds, callback);

  uint16_t timerNumber = dueTimerInterrupt.getTimerNumber();
  
  Serial.print(TimerName); Serial.print(F(" attached to Timer(")); Serial.print(timerNumber); Serial.println(F(")"));

  return timerNumber;
}



/*********************************** sub functions ***********************************/
// Message length to DEC
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

// Read CRC from received data
uint16_t readCRC() {
  uint16_t CRC = 0;
  for (int i = 0; i < 16; i++) {
    CRC = (CRC << 1) | resultVector[resultVector.size() - 2 * 8 + i]; //changed
  }
  return CRC;
}

// Calculate CRC and store byte data into vector byteData
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

// Read msg bits to bytes
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
