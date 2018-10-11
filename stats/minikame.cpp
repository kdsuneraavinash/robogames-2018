#include "minikame.h"


void MiniKame::init(){
    // Map between servos and board pins
    board_pins[0] = 2; // Servo S0 - X
    board_pins[1] = 5; // Servo S1 - X
    board_pins[2] = 9; // Servo S2 - Z
    board_pins[3] = 7; // Servo S3 - Z
    board_pins[4] = 8; // Servo S4 - X
    board_pins[5] = 6; // Servo S5 - X
    board_pins[6] = 3; // Servo S6 - Z
    board_pins[7] = 4; // Servo S7 - Z

    // Trim values for zero position calibration.
    // Error value for every servo.
    // Trim Error = Real turning - Angle given
    trim[0] = -65;
    trim[1] = 45;
    trim[2] = -22;
    trim[3] = 5;
    trim[4] = 35;
    trim[5] = -60;
    trim[6] = 0;
    trim[7] = -30;

    // Set reverse movement to false -  forward setting
    for (int i=0; i<8; i++) reverse[i] = false;

    // Init an oscillator for each servo
    for(int i=0; i<8; i++){
        oscillator[i].start(); // start every oscillator
        servo[i].attach(board_pins[i]); // attach every servo
    }
    //Set everyservo to initial pos
    zero();
}

void MiniKame::turnR(float steps, int T=600){
    // Adjust for Right ----------------
    int x_amp = 15;
    int z_amp = 35;
    int ap = 15;
    int hi = 23;
    // ---------------------------------
    int period[] = {T, T, T, T, T, T, T, T};
    int amplitude[] = {x_amp,x_amp,z_amp,z_amp,x_amp,x_amp,z_amp,z_amp};
    int offset[] = {90+ap,90-ap,90-hi,90+hi,90-ap,90+ap,90+hi-10,90-hi};
    int phase[] = {0,180,90,90,180,0,90,90};

    execute(steps, period, amplitude, offset, phase);
}

void MiniKame::turnL(float steps, int T=600){
    // Adjust for Right ----------------
    int x_amp = 15;
    int z_amp = 15;
    int ap = 15;
    int hi = 23;
    // ---------------------------------
    int period[] = {T, T, T, T, T, T, T, T};
    int amplitude[] = {x_amp,x_amp,z_amp,z_amp,x_amp,x_amp,z_amp,z_amp};
    int offset[] = {90+ap,90-ap,90-hi,90+hi,90-ap,90+ap,90+hi,90-hi};
    int phase[] = {180,0,90,90,0,180,90,90};

    execute(steps, period, amplitude, offset, phase);
}

void MiniKame::dance(float steps, int T=600){
    // Pass
}

void MiniKame::frontBack(float steps, int T=600){
    // Pass
}

void MiniKame::run(float steps, int T=5000){
    // Pass
}

void MiniKame::omniWalk(float steps, int T, bool side, float turn_factor){
    // Pass
}

void MiniKame::moonwalkL(float steps, int T=5000){
    // Pass
}

void MiniKame::walk(float steps, int T=5000){
    // Adjust for Right ----------------
    int x_amp = 15;
    int z_amp = 20;
    int ap = 20;
    int hi = 30;
    int front_x = 12;
    // ---------------------------------

    int period[] = {T, T, T/2, T/2, T, T, T/2, T/2};
    int amplitude[] = {x_amp,x_amp,z_amp,z_amp,x_amp,x_amp,z_amp,z_amp};
    int offset[] = {   90+ap-front_x,
                                90-ap+front_x,
                                90-hi,
                                90+hi,
                                90-ap-front_x,
                                90+ap+front_x,
                                90+hi,
                                90-hi
                    };
    int  phase[] = {90, 90, 270, 90, 270, 270, 90, 270};

    for (int i=0; i<8; i++){
        oscillator[i].reset(); // Reset oscillator
        oscillator[i].setPeriod(period[i]); // Set period
        oscillator[i].setAmplitude(amplitude[i]);// Set Amplitude
        oscillator[i].setPhase(phase[i]); // Set Phase
        oscillator[i].setOffset(offset[i]); // Set offset
    }

    // Time after going indicated no of steps
    _final_time = millis() + period[0]*steps;
    // Starting time
    _init_time = millis();
    bool side;
    while (millis() < _final_time){ // While walking is supposed to end
        side = (int)((millis()-_init_time) / (period[0]/2)) % 2; // Switchmoving side
        setServo(0, oscillator[0].refresh());
        setServo(1, oscillator[1].refresh());
        setServo(4, oscillator[4].refresh());
        setServo(5, oscillator[5].refresh());

        // Two instances of walking
        if (side == 0){
            setServo(3, oscillator[3].refresh());
            setServo(6, oscillator[6].refresh());
        }
        else{
            setServo(2, oscillator[2].refresh());
            setServo(7, oscillator[7].refresh());
        }
        delay(1);
    }
}

void MiniKame::upDown(float steps, int T=5000){
    // Pass
}


void MiniKame::pushUp(float steps, int T=600){
    // Pass
}

void MiniKame::hello(){
    // Pass
}

void MiniKame::jump(){
    // Pass
}

void MiniKame::home(){
    int ap = 20;
    int hi = 35;
    int position[] = {90+ap,90-ap,90-hi,90+hi,90-ap,90+ap,90+hi,90-hi};
    for (int i=0; i<8; i++) setServo(i, position[i]);
}

// Initial position
void MiniKame::zero(){
    for (int i=0; i<8; i++) setServo(i, 90);
}

// Change value of reverse array
void MiniKame::reverseServo(int id){
    if (reverse[id])
        reverse[id] = 0;
    else
        reverse[id] = 1;
}


// Sets an angle to a Servo
void MiniKame::setServo(int id, float target){
    if (!reverse[id])
        // If servo is set to be turned in forward,
        // Go (angle+error)
        servo[id].write(target+trim[id]);
    else
        servo[id].write(180-(target+trim[id]));
    _servo_position[id] = target;
}

float MiniKame::getServo(int id){
    return _servo_position[id];
}


void MiniKame::moveServos(int time, float target[8]) {
    if (time>10){
        for (int i = 0; i < 8; i++)	_increment[i] = (target[i] - _servo_position[i]) / (time / 10.0);
        _final_time =  millis() + time;

        while (millis() < _final_time){
            _partial_time = millis() + 10;
            for (int i = 0; i < 8; i++) setServo(i, _servo_position[i] + _increment[i]);
            while (millis() < _partial_time); //pause
        }
    }
    else{
        for (int i = 0; i < 8; i++) setServo(i, target[i]);
    }
    for (int i = 0; i < 8; i++) _servo_position[i] = target[i];
}

void MiniKame::execute(float steps, int period[8], int amplitude[8], int offset[8], int phase[8]){

    for (int i=0; i<8; i++){
        oscillator[i].setPeriod(period[i]);
        oscillator[i].setAmplitude(amplitude[i]);
        oscillator[i].setPhase(phase[i]);
        oscillator[i].setOffset(offset[i]);
    }

    unsigned long global_time = millis();

    for (int i=0; i<8; i++) oscillator[i].setTime(global_time);

    _final_time = millis() + period[0]*steps;
    while (millis() < _final_time){
        for (int i=0; i<8; i++){
            setServo(i, oscillator[i].refresh());
        }
        yield();
    }
}

int MiniKame::angToUsec(float value){
    return value/180 * (MAX_PULSE_WIDTH-MIN_PULSE_WIDTH) + MIN_PULSE_WIDTH;
}
