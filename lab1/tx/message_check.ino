// int ibyte = 0;
//   char cbyte = 0;
//   for(int index = 0; index < (length + 5) * 8; index++){
//     if(index < 3 * 8){
//       if(index % 8 == 0 && index != 0){
//         Serial.print("pre is: ");Serial.println(ibyte, BIN);
//         ibyte = 0;
//       }
//       ibyte |= (bin[index] & 0b1);
//       if((index + 1) % 8 != 0) ibyte = ibyte << 1;
//     }

//     if(index >= 3 * 8 && index < 5 * 8){
//       if(index == 3 * 8) ibyte = 0;
//       ibyte |= (bin[index] & 0b1);
//       if(index != 39) ibyte = ibyte << 1;
//       else {Serial.print("len is: ");Serial.println(ibyte, DEC);}
//     }

//     if(index >= 5 * 8){
//        if(index % 8 == 0 && index != 40 || index == ((length + 5) * 8 - 1)){
//         Serial.print("msg is: ");
//         Serial.println(cbyte);
//         cbyte = 0;
//       }
//       cbyte |= (bin[index] & 0b1);
//       if((index + 1) % 8 != 0) cbyte = cbyte << 1;
//     }
//   }

  //  nums++;
  // Serial.print(data);
  
  // if (a == 8) {
  //   Serial.println();
  //   a = 0;
  // }