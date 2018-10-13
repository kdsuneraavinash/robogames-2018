#include <Arduino.h>
#include <Servo.h>
#include "robot.h"

void parseData(String data);

Robot robot;
char input;

void setup() {
  Serial.begin(9600);
  robot.init();
  input = 5;
}

void loop() {
    while (Serial.available()) {
      char inp = Serial.read();
      inp -= '0';
      if (inp <1 || inp>9){
        continue;
      }else{
        input = inp;
      }
    }
      parseData(input);
}

void parseData(char data) {
  switch (data) {

    case 1: // Up
      Serial.println("Up");
      robot.walk(1, 500);
      break;

    case 2: // Down
      Serial.println("Stop");
      delay(200);
      break;

    case 3: // Left
      Serial.println("Left");
      robot.turnL(1, 550);
      input = 5;
      break;

    case 4: // Right
      Serial.println("Right");
      robot.turnR(1, 550);
      input = 5;
      break;

    case 5: // STOP
      robot.home();
      Serial.println("Stop");
      delay(200);
      break;
    case 6://push up
      Serial.println("push up");
      robot.pushUp(1,550);

    default:
      break;
  }
}
