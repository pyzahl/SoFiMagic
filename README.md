# SoFiMagic -- Sony App to fully automated shot a Eclipse or similar task

![App Icon](app/src/main/res/drawable/icon.png)

SoFiMagic (Automated Solar Eclipe Shooting) app for Sony Alpha cameras using the [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework).
Based in TimeLapse app, based on Focus app.

I have only a Sony Alpha7Rii (ILCE-7RM2) to test it, if you have another camera I would be happy to receive bug reports.

## Installation ##
Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) or install through [sony-pmca.appspot.com](https://sony-pmca.appspot.com/apps).

The build app apk package is included here ready to install.

Thanks to [ma1co](https://github.com/ma1co) for creating this amazing framework and [obs1dium](https://github.com/obs1dium), I used FocusBracket as a code base.
And final thanks to [jonasjuffinger](https://github.com/jonasjuffinger/TimeLapse) for creating TimeLapse I used as template to start SoFiMagic. 

## Building ##
Load project into Android Studio...

## Usage ##
Start App, configure. Review XML file on CF card in SOFIMAGI folder created by App itself at first start. Edit XML file... see below for example. (On Camera GUI is pending completion).
Start App again.
Finally click the start button and wait.

You can stop by clicking the MENU button on the camera.

## SS (Silent Shutter) ##
The silent shutter option is functionless on cameras without silent shutter mode.

## MF (Manual Focus) ##
This sets focus mode to manual. Be sure to have focused before starting the app!

## DOFF (Display Off) ##
EXPERIMENTAL/TETSING -- DO NOT SELECT.
Turn the display off between each shot. This doesn't change the battery consumption but it can be healthy for the display when taking very long time lapses.

## AUTO CREATED CONFIGURATION FILE and APP + SHOOTING LOG
The XML example below is auto created on first app start on SD Card as SOFIMAGI/SOFIPRG.XML.
A app log file is also created (and always appended to) in SOFIMAGI/LOG.TXT.

## CF-CARD-ROOT:/SOFIMAGI/SOFIPRG.XML

    <?xml version="1.0" encoding="UTF-8"?><sofimagic xmlns="com.github.pyzahl">
      <VALID_SOFI_PROGRAM>
        <STATUS>VALID</STATUS>
        <COMMENT>Eclipse Program: may edit this file or use App.
    NAME: Name of Shooting Phase. There may be more or any names used than default.
    PHASE Start/End time is relative to C0, C1..C4 as defined by REF_CONTACT_START/END plus a offset START and END in seconds.
    Number shots &gt; 0: Automated Interval Shooting for PHASE.
    For each interval the Exposure Lists are executed once.
    CAMERA_FLAG_LIST: Drive Modes are S: Single Photo, C: Continuous High, M: Continuous Medium, L: Continuous Low, B: Bracketing
    BURST_DURATION_LIST: Defines Burst Duration if in C,M or L Drive Mode.
    ISO_LIST: ISO to be set. Must be a valid ISO number. ISO=0 MUST terminate the list (internally)
    F_LIST: Aperture setting, 0: ignored. MUST be a valid Aperture.
SHUTTER_SPEED_LIST: Shutter Speed to be set. MUST be a valid shutter speed.</COMMENT>
  </VALID_SOFI_PROGRAM>
  <ECLIPSE_REF_CONTACT_TIMES>
    <C1>16:45:00</C1>
    <C2>18:10:00</C2>
    <C3>18:14:00</C3>
    <C4>19:24:00</C4>
    <COMMENT1>C0 is MAX = (TC2+TC3)/2=18:12:00</COMMENT1>
    <COMMENT2>REF_CONTACT = 0 is MAX computed from (TC2+TC3)/2. PHASE START, END times in sec relative to REF_CONTACT_START,_END index=1,2,0,3,4</COMMENT2>
  </ECLIPSE_REF_CONTACT_TIMES>
  <PHASE>
    <NAME>Partial1</NAME>
    <REF_CONTACT_START>1</REF_CONTACT_START>
    <START>-30</START>
    <REF_CONTACT_END>2</REF_CONTACT_END>
    <END>-10</END>
    <NUMBER_SHOTS>100</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>S,C,S,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>0,4,0,</BURST_DURATION_LIST>
    <ISO_LIST>400,400,320,</ISO_LIST>
    <F_LIST>3.2,0.0,0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/3200,1/2000,1/4000,</SHUTTER_SPEED_LIST>
  </PHASE>
  <PHASE>
    <NAME>Contact2</NAME>
    <REF_CONTACT_START>2</REF_CONTACT_START>
    <START>-6</START>
    <REF_CONTACT_END>2</REF_CONTACT_END>
    <END>6</END>
    <NUMBER_SHOTS>-1</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>C,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>12,</BURST_DURATION_LIST>
    <ISO_LIST>100,</ISO_LIST>
    <F_LIST>0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/4000,</SHUTTER_SPEED_LIST>
  </PHASE>
  <PHASE>
    <NAME>TotalityA</NAME>
    <REF_CONTACT_START>2</REF_CONTACT_START>
    <START>6</START>
    <REF_CONTACT_END>0</REF_CONTACT_END>
    <END>-30</END>
    <NUMBER_SHOTS>-1</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>S,S,S,S,S,S,S,S,S,S,S,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>0,0,0,0,0,0,0,0,0,0,0,</BURST_DURATION_LIST>
    <ISO_LIST>50,100,400,800,800,800,800,800,800,800,800,</ISO_LIST>
    <F_LIST>0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/4000,1/2000,1/1000,1/1000,1/500,1/250,1/100,1/50,1/20,1/4,1/1,</SHUTTER_SPEED_LIST>
  </PHASE>
  <PHASE>
    <NAME>MaxTotality</NAME>
    <REF_CONTACT_START>0</REF_CONTACT_START>
    <START>-30</START>
    <REF_CONTACT_END>0</REF_CONTACT_END>
    <END>30</END>
    <NUMBER_SHOTS>-1</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>S,S,S,S,S,S,S,S,S,S,S,S,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>0,0,0,0,0,0,0,0,0,0,0,0,</BURST_DURATION_LIST>
    <ISO_LIST>100,400,800,800,800,800,800,800,800,800,800,800,</ISO_LIST>
    <F_LIST>0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/1000,1/1000,1/1000,1/500,1/1000,1/500,1/1000,1/500,1/100,1/20,1/1,2/1,</SHUTTER_SPEED_LIST>
  </PHASE>
  <PHASE>
    <NAME>TotalityB</NAME>
    <REF_CONTACT_START>0</REF_CONTACT_START>
    <START>30</START>
    <REF_CONTACT_END>3</REF_CONTACT_END>
    <END>-6</END>
    <NUMBER_SHOTS>-1</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>S,S,S,S,S,S,S,S,S,S,S,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>0,0,0,0,0,0,0,0,0,0,0,</BURST_DURATION_LIST>
    <ISO_LIST>50,100,400,800,800,800,800,800,800,800,800,</ISO_LIST>
    <F_LIST>0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/4000,1/2000,1/1000,1/1000,1/500,1/250,1/100,1/50,1/20,1/4,1/1,</SHUTTER_SPEED_LIST>
  </PHASE>
  <PHASE>
    <NAME>Contact3</NAME>
    <REF_CONTACT_START>3</REF_CONTACT_START>
    <START>-6</START>
    <REF_CONTACT_END>3</REF_CONTACT_END>
    <END>6</END>
    <NUMBER_SHOTS>-1</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>C,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>12,</BURST_DURATION_LIST>
    <ISO_LIST>100,</ISO_LIST>
    <F_LIST>0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/4000,</SHUTTER_SPEED_LIST>
  </PHASE>
  <PHASE>
    <NAME>Partial2</NAME>
    <REF_CONTACT_START>3</REF_CONTACT_START>
    <START>10</START>
    <REF_CONTACT_END>4</REF_CONTACT_END>
    <END>30</END>
    <NUMBER_SHOTS>100</NUMBER_SHOTS>
    <CAMERA_FLAG_LIST>S,S,S,</CAMERA_FLAG_LIST>
    <BURST_DURATION_LIST>0,0,0,</BURST_DURATION_LIST>
    <ISO_LIST>400,400,320,</ISO_LIST>
    <F_LIST>0.0,0.0,0.0,</F_LIST>
    <SHUTTER_SPEED_LIST>1/3200,1/2000,1/3200,</SHUTTER_SPEED_LIST>
  </PHASE>
</sofimagic>
