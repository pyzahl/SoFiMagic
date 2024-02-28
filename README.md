# SoFiMagic -- WORK IN PROGRESS -- NOT YET READY --
SoFiMagic (Automated Solar Eclipe Shooting) app for Sony Alpha cameras using the [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework).
Based in TimeLapse app, based on Focus app.

I have only a Sony Alpha7R (ILCE-7RM2) to test it, if you have another camera I would be happy to receive bug reports.

## Installation ##
Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) or install through [sony-pmca.appspot.com](https://sony-pmca.appspot.com/apps).

Thanks to [ma1co](https://github.com/ma1co) for creating this amazing framework and [obs1dium](https://github.com/obs1dium), I used FocusBracket as a code base.
And final thanks to [jonasjuffinger](https://github.com/jonasjuffinger/TimeLapse) for creating TimeLapse I used as template to start SoFiMagic. 

## Building ##
Load project into Android Studio...

## Usage ##
Start App, configure. Review XML file on CF card in SOFIGAGI folder created by App itself at first start. Edit XML file... see below for example. (On Camera GUI is pending completion).
Start App again.
Finally click the start button and wait.

You can stop by clicking the MENU button on the camera.

## SS (Silent Shutter) ##
The silent shutter option is functionless on cameras without silent shutter mode.

## MF (Manual Focus) ##
This sets focus mode to manual. Be sure to have focused before starting the app!

## BRC3 ##
EXPERIMENTAL/TESTING -- DO NOT SELECT.
The app supports exposure bracketing. Set the mode to three-image exposure bracketing outside of the app and check BRC3 checkbox in the app. The app will always take three pictures. Keep in mind that the interval time must be large enough to take all three pictures.

## DOFF (Display Off) ##
EXPERIMENTAL/TETSING -- DO NOT SELECT.
Turn the display off between each shot. This doesn't change the battery consumption but it can be healthy for the display when taking very long time lapses.

## Burst mode ##
EXPERIMENTAL -- DO NOT SELECT.
When selecting the lowest interval the camera is in burst mode. In this mode it takes pictures as fast as it can write to the SD card for the duration set by the second slider.

## Known Issues ##

TESTING NEEDED

## AUTO CREATED CONFIGURATION FILE and APP + SHOOTING LOG
The XML example below is auto created on first app start on SD Card as SOFIMAGI/SOFIPRG.XML.
A app log file is also created (and always appended to) in SOFIMAGI/LOG.TXT.

## CF-CARD-ROOT:/SOFIMAGI/SOFIPRG.XML

    <?xml version="1.0" encoding="UTF-8"?><sofimagic xmlns="com.github.pyzahl">
      <VALID_SOFI_PROGRAM>
        <STATUS>VALID</STATUS>
      </VALID_SOFI_PROGRAM>
      <ECLIPSE_REF_CONTACT_TIMES>
        <C1>11:50:00</C1>
        <C2>12:53:10</C2>
        <C3>13:01:40</C3>
        <C4>14:55:20</C4>
        <COMMENT1>C0 is MAX = (TC2+TC3)/2=22:00:10</COMMENT1>
        <COMMENT2>REF_CONTACT = 0 is MAX computed from (TC2+TC3)/2. PHASE START, END times in sec relative to REF_CONTACT_START,_END index=1,2,0,3,4</COMMENT2>
      </ECLIPSE_REF_CONTACT_TIMES>
      <PHASE>
        <NAME>Partial1</NAME>
        <REF_CONTACT_START>1</REF_CONTACT_START>
        <START>30</START>
        <REF_CONTACT_END>2</REF_CONTACT_END>
        <END>-30</END>
        <NUMBER_SHOTS>32</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>200,200,</ISO_LIST>
        <F_LIST>0,0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/500,1/2000,</SHUTTER_SPEED_LIST>
      </PHASE>
      <PHASE>
        <NAME>Contact2</NAME>
        <REF_CONTACT_START>2</REF_CONTACT_START>
        <START>-5</START>
        <REF_CONTACT_END>2</REF_CONTACT_END>
        <END>5</END>
        <NUMBER_SHOTS>-1</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>100,</ISO_LIST>
        <F_LIST>0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/4000,</SHUTTER_SPEED_LIST>
      </PHASE>
      <PHASE>
        <NAME>TotalityA</NAME>
        <REF_CONTACT_START>2</REF_CONTACT_START>
        <START>5</START>
        <REF_CONTACT_END>0</REF_CONTACT_END>
        <END>-30</END>
        <NUMBER_SHOTS>-1</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>50,100,400,800,800,800,800,800,</ISO_LIST>
        <F_LIST>0,0,0,0,0,0,0,0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/4000,1/2000,1/1000,1/1000,1/500,1/250,1/100,1/50,</SHUTTER_SPEED_LIST>
      </PHASE>
      <PHASE>
        <NAME>MaxTotality</NAME>
        <REF_CONTACT_START>0</REF_CONTACT_START>
        <START>-30</START>
        <REF_CONTACT_END>0</REF_CONTACT_END>
        <END>30</END>
        <NUMBER_SHOTS>-1</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>100,400,800,800,800,800,800,800,</ISO_LIST>
        <F_LIST>0,0,0,0,0,0,0,0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/1000,1/1000,1/1000,1/500,1/1000,1/500,1/1000,1/500,</SHUTTER_SPEED_LIST>
      </PHASE>
      <PHASE>
        <NAME>TotalityB</NAME>
        <REF_CONTACT_START>0</REF_CONTACT_START>
        <START>30</START>
        <REF_CONTACT_END>3</REF_CONTACT_END>
        <END>-5</END>
        <NUMBER_SHOTS>-1</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>50,100,400,800,800,800,800,800,</ISO_LIST>
        <F_LIST>0,0,0,0,0,0,0,0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/4000,1/2000,1/1000,1/1000,1/500,1/250,1/100,1/50,</SHUTTER_SPEED_LIST>
      </PHASE>
      <PHASE>
        <NAME>Contact3</NAME>
        <REF_CONTACT_START>3</REF_CONTACT_START>
        <START>-5</START>
        <REF_CONTACT_END>3</REF_CONTACT_END>
        <END>5</END>
        <NUMBER_SHOTS>-1</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>100,</ISO_LIST>
        <F_LIST>0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/4000,</SHUTTER_SPEED_LIST>
      </PHASE>
      <PHASE>
        <NAME>Partial2</NAME>
        <REF_CONTACT_START>3</REF_CONTACT_START>
        <START>30</START>
        <REF_CONTACT_END>4</REF_CONTACT_END>
        <END>-30</END>
        <NUMBER_SHOTS>32</NUMBER_SHOTS>
        <CAMERA_FLAGS>S0,B0,C0</CAMERA_FLAGS>
        <ISO_LIST>400,400,</ISO_LIST>
        <F_LIST>0,0,</F_LIST>
        <SHUTTER_SPEED_LIST>1/1000,1/2000,</SHUTTER_SPEED_LIST>
      </PHASE>
    </sofimagic>

