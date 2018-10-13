#include <Arduino.h>
#include <Servo.h>
#include "robot.h"

void parseData(String data);

Robot robot;
char input;

void setup() {
  Serial.begin(9600);
  robot.init();
  input = '\n';
}

void loop() {
    if (Serial.available()) {
      input = Serial.readString().charAt(0) ;
      parseData(input);
    }else{
      parseData(input);
    }
}

void parseData(char data) {
  switch (data- '0') {

    case 1: // Up
      Serial.println("Up");
      robot.walk(1, 1000);
      break;

    case 2: // Down
      Serial.println("Stop");
      break;

    case 3: // Left
      Serial.println("Left");
      robot.turnL(1, 550);
      
      break;

    case 4: // Right
      Serial.println("Right");
      robot.turnR(1, 550);
      break;

    case 5: // STOP
      robot.home();
      Serial.println("Stop");
      break;

    default:
      break;
  }
}
