// MultiStepper.pde
// -*- mode: C++ -*-
//
// Shows how to multiple simultaneous steppers
// Runs one stepper forwards and backwards, accelerating and decelerating
// at the limits. Runs other steppers at the same time
//
// Copyright (C) 2009 Mike McCauley
// $Id: MultiStepper.pde,v 1.1 2011/01/05 01:51:01 mikem Exp mikem $

#include <AccelStepper.h>
#include <SoftwareSerial.h> 
AccelStepper pan(AccelStepper::FULL2WIRE, 6, 7);
AccelStepper tilt(AccelStepper::FULL2WIRE, 4, 5);

SoftwareSerial B(2, 3); // RX | TX

int pan_diff = 0;
int tilt_diff = 0;
const int inputx = A0;
const int inputy = A1;
long setpointX = 0;
long setpointY = 0;
long currentX = 0;
long currentY = 0;
int startMillis = 0;
int startMillisBlue = 0;
int incomingByte = 0;
String oldString = "";
String msg = "";
bool PositionReached = true;
bool blocklocaljoy = false;

void setup()
{  
    Serial.begin(9600);
    B.begin(9600);
    pan.setMaxSpeed(20000.0);
    pan.setAcceleration(5000.0);
    
    tilt.setMaxSpeed(20000.0);
    tilt.setAcceleration(5000.0);
}

int processAnalog(int analogDiff){
  int diff = 0;
  if((analogDiff < 600)and(analogDiff > 500)){
    diff = 0;
  }
  if(analogDiff < 470){
    if(analogDiff < 80){
      diff = -1 * (450 -analogDiff);
      diff = diff * 2;
    }else{
      diff = -1 * (450 -analogDiff);
      diff = round(diff/2);
    }
    
  }
  if(analogDiff > 630){
    if(analogDiff > 980){
      diff = analogDiff - 500;
      diff = diff * 2;
    }else{
      diff = analogDiff - 500;
      diff = round(diff/2);
    }
  }
  return diff;
}



void loop()
{
         if (B.available() > 0) {
          oldString = "";
          while(oldString.length() < 21){
              incomingByte = B.read();
              //Serial.println(String(incomingByte));
              incomingByte = incomingByte - 48;
              oldString = oldString + String(incomingByte) ;
              //Serial.println("recieved: " +oldString);
          }
              if (oldString.length() == 21){
                Serial.print("I will process: ");
                Serial.println(oldString);
                int dir_pan, dir_tilt = 0;
                int pan_speed, tilt_speed = 0;
                dir_pan = oldString.substring(0,1).toInt();
                dir_tilt = oldString.substring(10,11).toInt();
                pan_speed = oldString.substring(6,10).toInt();
                tilt_speed = oldString.substring(16,20).toInt();
               
                if (dir_pan == 1){//direction inverse because of added pan gear
                    pan_diff = oldString.substring(1,6).toInt();
                }else{
                    if(dir_pan == 0){
                        pan_diff = -1 * oldString.substring(1,6).toInt();
                    }else{
                      oldString = "";
                      pan_diff = 0;
                      tilt_diff = 0;
                    }
                }
                //pan_diff = pan_diff * 9; //new gear ratio
                
                if (dir_tilt == 1){ 
                    tilt_diff = oldString.substring(11,16).toInt();
                }else{
                    if(dir_tilt == 0){
                      tilt_diff = -1 * oldString.substring(11,16).toInt();
                    }else{
                      oldString = "";
                      pan_diff = 0;
                      tilt_diff = 0;
                    }
                    
                }
                if (pan_diff == 0){
                   pan.setCurrentPosition(0);
                   setpointX = 0;
                }
                if (tilt_diff == 0){
                   tilt.setCurrentPosition(0);
                   setpointY = 0;
                }
                
                setpointX = setpointX + pan_diff;
                setpointY = setpointY + tilt_diff;
                pan.setMaxSpeed(pan_speed);
                tilt.setMaxSpeed(tilt_speed);
                pan.moveTo(setpointX);
                tilt.moveTo(setpointY);
                
                Serial.println("x moved to " + String(pan_diff) + " with speed: " + String(pan_speed)+ " y moved to " + String(tilt_diff)+" with speed: " + String(tilt_speed));
                msg = oldString.substring(20,21);
                Serial.println("message: " + msg);
                Serial.println(String(setpointX));
                if ((msg == "1")||(msg == "2")||(msg == "5")){
                  PositionReached = false; 
                }else{
                  blocklocaljoy = true;
                }
                 
          }
         }
         
        
        int currentMillis = millis();
        if ((currentMillis - startMillis >= 50)&&(blocklocaljoy == false)&&(PositionReached == true)){
          pan_diff = processAnalog(analogRead(inputy));
          tilt_diff = processAnalog(analogRead(inputx));
          pan.setMaxSpeed(20000.0);
          tilt.setMaxSpeed(20000.0);
          
          setpointX = setpointX + pan_diff;
          setpointY = setpointY + tilt_diff;
          
          if (pan_diff == 0){
            pan.setCurrentPosition(0);
            setpointX = 0;
          }else{
            pan.enableOutputs();
            pan.moveTo(setpointX);  
          }
          if (tilt_diff == 0){
            //tilt.disableOutputs();
            tilt.setCurrentPosition(0);
            setpointY = 0;
          }else{
             tilt.enableOutputs();
             tilt.moveTo(setpointY);
          }
//          Serial.println("X");
//          Serial.println(setpointX);
//          Serial.println("Y");
//          Serial.println(setpointY);
          startMillis = currentMillis;
       }

       if ((pan.currentPosition() == setpointX)&&(tilt.currentPosition() == setpointY)&&(PositionReached == false)){
          delay(1000);
          if (msg == "2"){
            B.write("zweiN");
            Serial.println("second done");
          }
           if (msg == "1"){
            B.write("einsN");
            Serial.println("first done");
          }
           if (msg == "5"){
            B.write("errorcheckN");
            Serial.println("errorcheck done");
          }
          blocklocaljoy = false;
          PositionReached = true;
          pan.setCurrentPosition(0);//sets current position to zero
          setpointX = 0;
          
          tilt.setCurrentPosition(0);//sets current position to zero
          setpointY = 0;
       
       }  
         
    pan.run();
    tilt.run();
    
}
