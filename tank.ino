#include <Servo.h> //servo library
#define INPUT_SIZE 30

Servo myservo; // create servo object to control servo


int Echo = A4;  
int Trig = A5; 
int IN1 = 6;
int IN2 = 7;
int IN3 = 8;
int IN4 = 9;
int ENA = 5;
int ENB = 13;
int ABS_L = 0;
int ABS_R = 0;
int BUZZER = 10;

int middleDistance = 0;

long buzzTimeout = 0;

int servoAngle = 90;
int i = 0;

void setup() { 
  myservo.attach(3);// attach servo on pin 3 to servo object
  Serial.begin(9600);
  Serial.setTimeout(5);   
  pinMode(Echo, INPUT);    
  pinMode(Trig, OUTPUT);  
  pinMode(IN1,OUTPUT);
  pinMode(IN2,OUTPUT);
  pinMode(IN3,OUTPUT);
  pinMode(IN4,OUTPUT);
  pinMode(ENA,OUTPUT);
  pinMode(ENB,OUTPUT);

  pinMode(BUZZER,OUTPUT);
} 
/*
 * Приём команды по serial 
 * Применение
 * Отправка данных
 */
void loop() { 

  receiveInfo();
  
  apply();

  
  if (i % 4 == 0) {
    middleDistance = distanceTest();
    sendInfo();
  }

  i++;
}

/*
 * Парсим набор команд и применяем
 */
void receiveInfo() {
  // Get next command from Serial (add 1 for final 0)
  char input[INPUT_SIZE + 1];
  byte size = Serial.readBytes(input, INPUT_SIZE);
  // Add the final 0 to end the C string
  input[size] = 0;
  
  // Read each command pair 
  char* command = strtok(input, ",");
  while (command != 0) {
      // Split the command in two values
      char* separator = strchr(command, '=');
      if (separator != 0) {
          // Actually split the string in 2: replace ':' with 0
          *separator = 0;
          char* cName = command;
          ++separator;
          char* cVal = separator;
          applyCommand(cName, cVal);
      }
      // Find the next command in input string
      command = strtok(0, "&");
  }
}

// Применение конкретной команды
void applyCommand(char* cName, char* cVal) {
    if (strcmp(cName, "sa") == 0) { // servoAngle 
        servoAngle = atoi(cVal);
    }
    if (strcmp(cName, "abs_l") == 0) { // abs L
        ABS_L = atoi(cVal);
    } 
    if (strcmp(cName, "abs_r") == 0) { // abs R
        ABS_R = atoi(cVal);
    }
    
}

void apply() {
    myservo.write(servoAngle);
    
    moveCar();

    buzz();
}

void buzz() {
  int m = millis();
  if (buzzTimeout == 0 || buzzTimeout < m) {
    int buzzDelay = middleDistance < 150 ? max(middleDistance*middleDistance/4, 50)  : 0; //
    buzzTimeout = m + buzzDelay;
    if (buzzDelay != 0) {
      tone(BUZZER, 2000, 3);
    }
    return;
  }
}

void sendInfo() {
    Serial.print("ud=");
    Serial.print(middleDistance);
    Serial.print(",");
    Serial.print("sa=");
    Serial.print(servoAngle);
    Serial.print(",");
    Serial.print("abs_l=");
    Serial.print(ABS_L);
    Serial.print(",");
    Serial.print("abs_r=");
    Serial.print(ABS_R);
    Serial.println();
}

void moveCar()
{
  analogWrite(ENA, getEngineForce(ABS_L));
  analogWrite(ENB, getEngineForce(ABS_R));

  digitalWrite(IN1, ABS_L > 0 ? LOW : HIGH);
  digitalWrite(IN2, ABS_L > 0 ? HIGH : LOW);

  digitalWrite(IN3, ABS_R > 0 ? LOW : HIGH);
  digitalWrite(IN4, ABS_R > 0 ? HIGH : LOW);
}

int getEngineForce(int force) {
  if (force == 0) return 0;
  force = abs(force);
  force = max(force, 64);
  force = min(force, 1024);
  return force;
}

int smoothed = 0;
 /*Ultrasonic distance measurement Sub function*/
int distanceTest() {

  digitalWrite(Trig, LOW);   
  delayMicroseconds(2);
  digitalWrite(Trig, HIGH);  
  delayMicroseconds(20);
  digitalWrite(Trig, LOW);
  
  float duration = pulseIn(Echo, HIGH);  
  
  int roundedDistance = (int)  duration  / 29 / 2; // / 58.138
  
  if (roundedDistance > 1000)  {
    roundedDistance = 0;
  }

  if (roundedDistance < 0) {
    roundedDistance = 0;
  }
  
  int result = smooth(roundedDistance, 0.85, smoothed);
  smoothed = result;
  return result;
}  

// Simple low pass filter. filterValue determines smoothness. 0 = off; 0.9999 = max 
int smooth(int sensor_reading, float filterValue, float smoothedValue){

  // Checking validity of filterValue; if beyond range, set to max/min value if out of range.
  if (filterValue > 1){      
    filterValue = .99;
  }
  else if (filterValue <= 0){
    filterValue = 0;
  }

  smoothedValue = (sensor_reading * (1 - filterValue)) + (smoothedValue  *  filterValue);
  return (int)smoothedValue;
}

