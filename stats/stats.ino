#include <Arduino.h>
#include <Servo.h>
#include "minikame.h"

void parseData(String data);

MiniKame robot;
bool running=0;
String input;


void setup() {
    Serial.begin(9600);
    delay(1000);
    robot.init();
}

void loop() {
    if (running){
        Serial.println("running");
        if (Serial.available()) {
            while(Serial.available()) input = Serial.readStringUntil('+');
            parseData(input);
        } else {
            Serial.println("Keep Moving");
            parseData(input);
        }
    }
    else{
        Serial.println("Normal mode");
        if (Serial.available()) {
            while(Serial.available()) input = Serial.readStringUntil('+');
            parseData(input);
        }
        else robot.home();
    }
}

void parseData(String data){

    switch (data.toInt()){

        case 1: // Up
            robot.walk(1,550);
            running = 1;
            break;

        case 2: // Down
            break;

        case 3: // Left
            robot.turnL(1,550);
            running = 1;
            break;

        case 4: // Right
            robot.turnR(1,550);
            running = 1;
            break;

        case 5: // STOP
            running = 0;
            break;

        default:
            robot.home();
            break;
    }
}
