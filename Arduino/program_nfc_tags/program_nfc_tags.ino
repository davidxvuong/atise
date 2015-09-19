#if 0
#include <SPI.h>
#include <PN532_SPI.h>
#include <PN532.h>
#include <NfcAdapter.h>

PN532_SPI pn532spi(SPI, 10);
NfcAdapter nfc = NfcAdapter(pn532spi);
#else

#include <Wire.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <NfcAdapter.h>

PN532_I2C pn532_i2c(Wire);
NfcAdapter nfc = NfcAdapter(pn532_i2c);
#endif

String input;

//function declarations

void resetNFCTag(int type);
void readFromNFCTag();
void writeToNFCTag();

void setup() {
  Serial.begin(9600);
  nfc.begin();
}

void loop() {
  delay(1500);
  Serial.println("\nEnter one of the following commands:\n - format: Format NFC Tag\n - clean: Clean NFC Tag\n - erase: Erase NFC Tag\n - read: Read NFC Tag\n - write: Write NFC Tag\n");

  while (!(Serial.available() > 0));

  input = "";

  while (Serial.available() > 0) {
    input += char(Serial.read());
    delay(2);
  }

  Serial.print("Command: ");
  Serial.println(input);
  
  if (input == "format") {
    resetNFCTag(1);
  }
  else if (input == "clean") {
    resetNFCTag(2);
  }
  else if (input == "erase") {
    resetNFCTag(3);
  }
  else if (input == "read") {
    readFromNFCTag();
  }
  else if (input == "write") {
    writeToNFCTag();
  }
}

void resetNFCTag(int type) {
  Serial.println("Place NFC tag on the reader.");

  while (!nfc.tagPresent());

  bool success;

  if (type == 1) {
    success = nfc.format();
  }
  else if (type == 2) {
    success = nfc.clean();
  }
  else {
    success = nfc.erase();
  }

  if (success) {
    Serial.println("Operation completed");
  }
  else {
    Serial.println("Operation failed");
  }
}

void readFromNFCTag() {
  Serial.println("Place NFC tag on the reader.");

  while (!nfc.tagPresent());

  NfcTag tag = nfc.read();

  tag.print();
}

void writeToNFCTag() {
  String data = "";
  Serial.println("Enter the text you want to write to the NFC tag: ");
  while (!(Serial.available() > 0));

  while (Serial.available() > 0) {
    data += char(Serial.read());
    delay(2);
  }

  Serial.println("Place NFC tag on the reader.");

  while (!nfc.tagPresent());

  NdefMessage msg = NdefMessage();

  msg.addUriRecord(data);

  if (nfc.write(msg)) {
    Serial.println("Write completed");
  }
  else {
    Serial.println("Write failed");
  }
}

