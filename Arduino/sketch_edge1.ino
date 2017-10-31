#include <Wire.h>
/* DHT11 - temperature sensor. At this project Adafruit library(https://learn.adafruit.com/dht) was used */
#include "DHT.h"

/* Global settings */ 
#define SLAVE_ADDRESS 4
#define MAX_RESPONSE_SIZE 16
#define STATUS_FAIL 0
#define STATUS_SUCCEESS 1
#define TOTAL_DEVICES 7

/* Devices addrs */ 
#define DEVICE_BUZZER 0
#define DEVICE_LED1 1
#define DEVICE_LED2 2
#define DEVICE_LED3 3
#define DEVICE_LED4 4
#define DEVICE_SMOKE_DETECTOR 5
#define DEVICE_SERVO 6

/* Enabled devices */
#define DEVICE_BUZZER_CONNECTED 1
#define DEVICE_LED1_CONNECTED 1
#define DEVICE_LED2_CONNECTED 0
#define DEVICE_LED3_CONNECTED 0
#define DEVICE_LED4_CONNECTED 0
#define DEVICE_SMOKE_DETECTOR_CONNECTED 1
#define DEVICE_SERVO_CONNECTED 0

/* Devices pins */ 
#define DEVICE_BUZZER_PIN 9
#define DEVICE_LED1_PIN 8
#define DEVICE_LED2_PIN 2
#define DEVICE_LED3_PIN 3
#define DEVICE_LED4_PIN 4
#define DEVICE_SMOKE_DETECTOR_PIN A0
#define DEVICE_SERVO_PIN 6


/*
	Commands description
	
	Send number of devices:
			Master(Raspberry)	|			|	Slave(Arduino)
			opCode=0 			|	---->	|		
								|	<----	|		device_numbers
								
	Show value from device to master:
			Master(Raspberry)	|			|	Slave(Arduino)
			opCode=1 			|	---->	|		
			device number		|	---->	|		
								|	<----	|		device_value / STATUS_FAIL
								
	Put value to device from master:
			Master(Raspberry)	|			|	Slave(Arduino)
			opCode=2 			|	---->	|		
			device number		|	---->	|		
			device value		|	---->	|		
								|	<----	|		STATUS_FAIL / STATUS_SUCCEESS
*/


/*
	Possible opCodes: 
		0 - sendNumberOfDevices
		1 - getValue
		2 - setValue
 */
byte opCode = -1;
byte deviceNumber = -1;  
byte deviceNewValue = -1;
byte number = 0;
boolean flag_sendErrorStatus = false;
boolean flag_readyToSend = false;
boolean flag_waitingOpCode = true;
boolean flag_waitingDeviceNum = true;
boolean flag_waitingValue = true;

byte devicesCount = DEVICE_BUZZER_CONNECTED + DEVICE_LED1_CONNECTED + DEVICE_LED2_CONNECTED +
					DEVICE_LED3_CONNECTED + DEVICE_LED4_CONNECTED + DEVICE_SMOKE_DETECTOR_CONNECTED + 
					DEVICE_SERVO_CONNECTED;

//DEVICE_BUZZER | DEVICE_LED1 | DEVICE_LED2 | DEVICE_LED3 | DEVICE_LED4 | DEVICE_SMOKE_DETECTOR | DEVICE_SERVO
byte devicesValues[] = { 0, 0, 0, 0, 0, 0, 0};

DHT sensor(DEVICE_SMOKE_DETECTOR_PIN, DHT11);

void setup()
{
	
  Wire.begin(SLAVE_ADDRESS);
  Wire.onReceive(receiveData); // register event
  Wire.onRequest(sendData);
  Serial.begin(9600); // start serial for output
  
  sensor.begin();
  
  /* Init devices pins */  
  
  pinMode(DEVICE_BUZZER_PIN, OUTPUT);    
  pinMode(DEVICE_LED1_PIN, OUTPUT);   
  pinMode(DEVICE_LED2_PIN, OUTPUT);   
  pinMode(DEVICE_LED3_PIN, OUTPUT);   
  pinMode(DEVICE_LED4_PIN, OUTPUT);    
}

void loop() {
	
	if (DEVICE_BUZZER_CONNECTED)
	{
		if (devicesValues[DEVICE_BUZZER] == 0)
			digitalWrite(DEVICE_BUZZER_PIN, LOW);
		else
			digitalWrite(DEVICE_BUZZER_PIN, HIGH);			
	}
	
	if (DEVICE_LED1_CONNECTED)
	{
		if (devicesValues[DEVICE_LED1] == 0)
			digitalWrite(DEVICE_LED1_PIN, LOW);
		else
			digitalWrite(DEVICE_LED1_PIN, HIGH);			
	}
	
	if (DEVICE_SMOKE_DETECTOR_CONNECTED)
	{
		float t = sensor.readTemperature();
		if (isnan(t)) {
			Serial.println("Error..");
			return;
		}
		else
		{
			devicesValues[DEVICE_SMOKE_DETECTOR] = (byte)t;
		}
		//Serial.print("Temperature: ");
		//Serial.println(t);
	}
	delay(10);
}

void receiveData(int byteCount)
{
        
	while (Wire.available())
	{
		Serial.println(Wire.available());
		number = Wire.read();

		if (flag_waitingOpCode)
		{
			opCode = number;
			if (opCode>3)
			{
			  flag_sendErrorStatus = true;
			  continue;
			}
			if (opCode == 0)
				flag_readyToSend = true;
			else
			{
				flag_waitingOpCode = false;
			}
		}
		else if (flag_waitingDeviceNum)
		{        
			deviceNumber = number;
			flag_waitingDeviceNum = false;
			if (opCode == 1)
				flag_readyToSend = true;
		}
		else
		{
			deviceNewValue = number;
			flag_readyToSend = true;
		}
	}
}

void sendData()
{
	if (flag_sendErrorStatus)
	{
		Wire.write(STATUS_FAIL);
		flag_sendErrorStatus = false; 
	}
	if (flag_readyToSend)
	{
		switch (opCode)
		{
			case 0:
			{
                Serial.println("NumDevicesSended");
				Wire.write(devicesCount);				
				break;
			}
			case 1:
			{
                Serial.print("Value show: ");
                Serial.println(devicesValues[deviceNumber]);
				
				if (deviceNumber <= TOTAL_DEVICES)
					Wire.write(devicesValues[deviceNumber]);
				else
					Wire.write(STATUS_FAIL);
					
				break;
			}
			case 2:
			{
                Serial.println("Value set ");
				if (deviceNumber <= TOTAL_DEVICES)
				{
					devicesValues[deviceNumber] = deviceNewValue;
					Wire.write(STATUS_SUCCEESS);
				}
				else
				{
					Wire.write(STATUS_FAIL);
				}		
				break;
			}
		}
		flag_readyToSend = false;
		flag_waitingOpCode = true;
		flag_waitingDeviceNum = true;
		flag_waitingValue = true;
	}  
}
