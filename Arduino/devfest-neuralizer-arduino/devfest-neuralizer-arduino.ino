/*
  Neuralizer - BLE Controlled Modulino LEDs

  Description:
    This sketch implements a BLE (Bluetooth Low Energy) peripheral named "devfest-neuralizer". 
    It allows an external smartphone app (BLE Central) to set RGB color and brightness. 
    When a button on the Modulino Buttons is pressed, the LEDs of Modulino Pixels light up
    according to the stored color and brightness settings. 
    Brightness levels control how many LEDs are turned on:
    0 = off, 1 = 2 LEDs, 2 = 5 LEDs, 3 = 8 LEDs.

  Hardware Required:
    - Arduino board with BLE support (e.g., Nano 33 BLE, Nano 33 BLE Sense)
    - Arduino Modulino Buttons
    - Arduino Modulino Pixels

  BLE Specifications:
    Device Name: "devfest-neuralizer"
    Service UUID: 12345678-1234-5678-1234-56789abcdef1
    Characteristics:
      - Color (3 byte, Read/Write): 12345678-1234-5678-1234-56789abcdef2
      - Brightness (1 byte, Read/Write): 12345678-1234-5678-1234-56789abcdef3
      - Active State (1 byte, Read/Notify): 12345678-1234-5678-1234-56789abcdef4

  Libraries:
    - Modulino by Arduino
    - ArduinoBLE by Arduino

  Authors:
    - Leonardo Cavagnis
*/

//TODO: Storing values in EEPROM

#include <ArduinoBLE.h>
#include <Modulino.h>

ModulinoButtons buttons;
ModulinoPixels pixels;

// --- BLE UUIDs ---
#define BASE_UUID        "12345678-1234-5678-1234-56789abcdef0"
#define SERVICE_UUID     "12345678-1234-5678-1234-56789abcdef1"
#define COLOR_UUID       "12345678-1234-5678-1234-56789abcdef2"
#define BRIGHTNESS_UUID  "12345678-1234-5678-1234-56789abcdef3"
#define ACTIVE_UUID      "12345678-1234-5678-1234-56789abcdef4"

// --- BLE Service & Characteristics ---
BLEService neuralizerService(SERVICE_UUID);
BLECharacteristic colorChar(COLOR_UUID, BLERead | BLEWrite, 3);      // RGB
BLEByteCharacteristic brightnessChar(BRIGHTNESS_UUID, BLERead | BLEWrite);
BLEByteCharacteristic activeChar(ACTIVE_UUID, BLERead | BLENotify);

uint8_t color[3] = {255, 255, 255};   // default white
uint8_t brightness = 1;               // 0=off, 1=low, 2=medium, 3=high

void setup() {
  Serial.begin(9600);

  // --- Modulino Setup ---
  Modulino.begin();
  buttons.begin();
  pixels.begin();

  for(int i = 0; i < 8; i++) {
    pixels.clear(i);
  }
  pixels.show();

  buttons.setLeds(false, false, false);

  // --- BLE Setup ---
  if (!BLE.begin()) {
    Serial.println("Failed to start BLE!");
    while (1);
  }

  BLE.setLocalName("devfest-neuralizer");
  BLE.setAdvertisedService(neuralizerService);

  // Add characteristics to service
  neuralizerService.addCharacteristic(colorChar);
  neuralizerService.addCharacteristic(brightnessChar);
  neuralizerService.addCharacteristic(activeChar);

  BLE.addService(neuralizerService);

  // Set initial values
  colorChar.writeValue(color, 3);
  brightnessChar.writeValue(brightness);
  activeChar.writeValue(0);

  BLE.advertise();
  Serial.println("BLE Neuralizer Peripheral started");
}

void loop() {
  // --- BLE: Check incoming connection ---
  BLEDevice central = BLE.central();

  // --- Buttons management ---
  bool anyPressed = false;

  if (buttons.update()) {
    if (buttons.isPressed('A') || buttons.isPressed('B') || buttons.isPressed('C')) {
      anyPressed = true;
      showPixels(color, brightness);
    } else {
      showPixels(color, 0); // Level 0 = LEDs OFF
    }

    // If BLE is connected, notify the button press
    if (central && central.connected()) {
      activeChar.writeValue(anyPressed ? 1 : 0);
    }
  }

  // --- BLE: Characteristics management if connected ---
  if (central && central.connected()) {
    // Check if color was written
    if (colorChar.written()) {
      colorChar.readValue(color, 3);
      Serial.print("Color updated: ");
      Serial.print(color[0]); Serial.print(",");
      Serial.print(color[1]); Serial.print(",");
      Serial.println(color[2]);
    }

    // Check if brightness was written
    if (brightnessChar.written()) {
      brightness = brightnessChar.value();
      Serial.print("Brightness updated: ");
      Serial.println(brightness);
    }
  }

  // Delay for stability
  delay(10);
}

// --- Function to update Pixels according to brightness ---
void showPixels(uint8_t rgb[3], uint8_t level) {
  int ledsOn = 0;
  switch(level) {
    case 0: ledsOn = 0; break;
    case 1: ledsOn = 2; break;
    case 2: ledsOn = 5; break;
    case 3: ledsOn = 8; break;
    default: ledsOn = 8; break;
  }

  // Aggiorna tutti i LED
  for(int i = 0; i < 8; i++) {
    if(i < ledsOn) {
      pixels.set(i, ModulinoColor(rgb[0], rgb[1], rgb[2]), 30); // LED ON 
    } else {
      pixels.set(i, ModulinoColor(0, 0, 0), 0);                 // LED OFF
    }
  }

  pixels.show();
}