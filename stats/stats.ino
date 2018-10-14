#include <Arduino.h>
#include <Servo.h>
#include "robot.h"

void parseData(String data);

Robot robot;
char input;
int period;
int incr;
int dir;
int angle = 0;
//int angle = 0;
void setup() {
  Serial.begin(9600);
  Serial.setTimeout(50);
  robot.init();
  input = 9;
  period = 1200;
  incr = 0;

}

bool isValidNumber(int n) {
  return n >= 0 && n < 10;
}

void loop() {
  while (Serial.available()) {
    char inp = Serial.read();
    inp -= '0';
    if (!isValidNumber(inp)) {
      continue;
    } else if (inp == 6) {
      angle = 0;
      delay(10);
      dir = Serial.read() - '0';
      delay(10);
      while (Serial.available()) {
        int nextV = Serial.read() - '0';
        if (!isValidNumber(nextV)) {
          break;
        }
        angle = angle * 10 + nextV;
      }
      //Serial.readString();
      if (angle < 45) {
        angle = 0;
      } else if (angle > 45) {
        angle = 90;
      }
      //Serial.print("Angle : ");
      //Serial.println(angle);
      //Serial.print("Dir : ");
      //Serial.println(dir);
      input = inp;
      break;
    } else {
      input = inp;
      //Serial.println(input);
      delay(10);
    }
  }
  //Serial.println('a');
  if (input == 9) {
    return;
  }
  else if (input == 6) {
    //Serial.println("B");
    if (dir == 0) {
      parseData(10, angle);
    } else {
      parseData(11, angle);
    }
  } else {
    //Serial.println("A");
    parseData(input, 0);
  }
}

void parseData(char data, int angle) {
  switch (data) {

    case 1: // Up
      //Serial.println("Up");
      robot.walk(1, 800);
      input = 9;
      break;

    case 3: // Left
      //Serial.println("Left");
      robot.turnL(1, 1000);
      //robot.walk(1, 1000);
      //robot.zero();
      input = 9;
      break;

    case 4: // Right
      //Serial.println("Right");
      robot.turnR(1, 1000);
      //robot.walk(2, 1000);
      //robot.zero();
      input = 9;
      break;

    case 5: // STOP
      robot.zero();
      //Serial.println("Stop");
      input = 9;
      break;

    case 8://push up
      //Serial.println("push up");
      //robot.pushUp(2, period);
      robot.walk(6, 1500);
      //Serial.println(angle);
      robot.turnR(angle, 1000);
      //Serial.println("finish");
      input = 9;
      break;
    case 10:
      Serial.println("clockwise");
      robot.walk(5, 1200);
      robot.turnR(7, 800);
      robot.walk(2, 800);
      sendDone();
      input = 9;
      break;
    case 11:
      Serial.println("antiClock");
      robot.walk(5, 1200);
      robot.turnL(7, 800);
      robot.walk(2, 800);
      sendDone();
      input = 9;
      break;
    default:
      break;
  }
}

void sendDone(){
  for(int i = 0; i<1; i++){
    Serial.print("z");
  }
}
