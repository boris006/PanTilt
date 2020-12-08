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
int setpointX = 0;
int setpointY = 0;
int currentX = 0;
int currentY = 0;
int startMillis = 0;
int startMillisBlue = 0;
int incomingByte = 0;
String oldString = "";
String msg = "";
bool PositionReached = false;

void setup()
{  
    Serial.begin(9600);
    B.begin(9600);
    pan.setMaxSpeed(5000.0);
    pan.setAcceleration(3000.0);
    
    tilt.setMaxSpeed(1000.0);
    tilt.setAcceleration(3000.0);
    
    //pan.moveTo(setpointX);  
   // tilt.moveTo(setpointY);
}

int processAnalog(int analogDiff){
  int diff = 0;
  if((analogDiff < 600)and(analogDiff > 500)){
    diff = 0;
  }
  if(analogDiff < 500){
    diff = -1 * (500 -analogDiff);
    diff = round(diff/10);
  }
  if(analogDiff > 600){
    diff = analogDiff - 500;
    diff = round(diff/10);
  }
  return diff;
}



void loop()
{
  
//    while(Serial.available()>0){
//      pan_diff = Serial.parseInt();
//      tilt_diff = Serial.parseInt();
//      if(Serial.available() == "\n"){
//        setpointX = setpointX + pan_diff;
//        setpointY = setpointY + tilt_diff;
//        Serial.println("X: "+ String(setpointX) + "      Y: " + String(setpointY));
//      }
//    }
    
    //get the current "time" (actually the number of milliseconds since the program started)
         
         //int currentMillisBlue = millis();
         //if (currentMillisBlue - startMillisBlue >= 5){
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
                //Serial.println("8 Zeichen lang");
                //Serial.println(oldString.substring(0,4));
                //Serial.println(oldString.substring(4,8));
                int dir_pan, dir_tilt = 0;
                int pan_speed, tilt_speed = 0;
                dir_pan = oldString.substring(0,1).toInt();
                dir_tilt = oldString.substring(10,11).toInt();
                pan_speed = oldString.substring(6,10).toInt();
                tilt_speed = oldString.substring(16,20).toInt();
                
                if (dir_pan == 0){
                    pan_diff = oldString.substring(1,6).toInt();
                }else{
                    if(dir_pan == 1){
                        pan_diff = -1 * oldString.substring(1,6).toInt();
                    }else{
                      oldString = "";
                      pan_diff = 0;
                      tilt_diff = 0;
                    }
                    
                }

                
                if (dir_tilt == 0){
                    tilt_diff = oldString.substring(11,16).toInt();
                }else{
                    if(dir_tilt == 1){
                      tilt_diff = -1 * oldString.substring(11,16).toInt();
                    }else{
                      oldString = "";
                      pan_diff = 0;
                      tilt_diff = 0;
                    }
                    
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
                PositionReached = false;

              
          }
          
         }
         
        
        int currentMillis = millis();
        if (currentMillis - startMillis >= 50){
  
          pan_diff = processAnalog(analogRead(inputy));
          tilt_diff = processAnalog(analogRead(inputx));
          setpointX = setpointX + pan_diff;
          setpointY = setpointY + tilt_diff;
          
          if (pan_diff == 0){
            pan.disableOutputs();
          }else{
            pan.enableOutputs();
            pan.moveTo(setpointX);  
          }
          if (tilt_diff == 0){
            tilt.disableOutputs();
          }else{
             tilt.enableOutputs();
             tilt.moveTo(setpointY);
          }
          //PositionReached = false;
        
        
       
        
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
          PositionReached = true;
       }
            
         
    pan.run();
    tilt.run();
    
}
