# SoFiMagic -- Sony App and optional platform/camera independend python script to fully automated shoot a Eclipse or similar task

![App Icon](app/src/main/res/drawable/icon.png)

Mainly for SONY -- but wait, there is now also a generic platform independent (requires only python3+gphoto2) alternative gphoto based python command line tool / script what does make use of the exacte same XML shooting program as used for the Sony app! I needed this to get my old Nikon going -- via armbian radxa or raspberrypi or such....
See Sony unrelated gMagicPy folder. Almost excatly same: please remove the xmlns=... url part from the global tag <sofimagic xmlns="com.github.pyzahl"> so it does only start with the plain <sofimagic> tag. See example xml fiel in this folder!

SoFiMagic (Automated Solar Eclipe Shooting) app for Sony Alpha cameras using the [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework).
Based in TimeLapse app, based on Focus app.

I have only a Sony Alpha7Rii (ILCE-7RM2) to test it, if you have another camera I would be happy to receive bug reports.

## Disclaimer ##
Install and use on own risk. Test everything before yourself.

## Installation ##
Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) 

See here for supported cameras, "App-Support" is required:
https://openmemories.readthedocs.io/devices.html

The build app apk package is included here ready to install.

Thanks to [ma1co](https://github.com/ma1co) for creating this amazing framework and [obs1dium](https://github.com/obs1dium), I used FocusBracket as a code base.
And final thanks to [jonasjuffinger](https://github.com/jonasjuffinger/TimeLapse) for creating TimeLapse I used as template to start SoFiMagic. 

## Building ##
Load project into Android Studio...

## Usage ##
Start App, configure. Review XML file on CF card in SOFIMAGI folder created by App itself at first start. Edit XML file... see below for example.
Global options: 

- Use Silent Shutter (SS) mode -- will automatically disabled for Burts/Cont Hi/Low shooting and enabled again!
- Manual Focus (MF) -- very recommened for most cases! Focus before. Also set Aperture if any! (F-Settings do not yet work correctly -- need help here!! Try various...)
- Display Off (DOFF) -- when running, can be turned back on/off while running (Fn, Play Buttons), see below.
- Start via Start button on display or Camera Play Button

Start App again.
Finally click the start button and wait.

You can stop by clicking the MENU button on the camera.

IMPORTANT NOTE: PLEASE DO NOT START THIS APP WITH CAMERA BEEN IN DRIVE MODE USING ANY TIMER SETTING (2/10s delay, or Bracket delayed...) -- THIS WILL FAIL PROGRAMMED SHOOTING!
SET TO SINGLE DRIVE MODE, NO TIMER.

The settings screens. Start button at the bottom, scroll down.

## Updates/new: ##
Settings:

- "Play" button now also starts the job from settings menu.
- "Fn" Button takes a exact Camera time reading when pressed and that time is shown/updated with ms precision on top of the settings screen.
- Camera/Program-ID or Nick Name is displayed on top of settings screen. Can only be set via XML file.

Bracketing CameraFlags are experimental, may not work -- TDB. Stick with S (Single Shot) and program the series required for now!

While running:
- "Play" Button will turn on the display, and activates timeout to display off (5s)
- "Fn" Button turn on display for infinite time. To turn off auto again press "Play".
- "Menu"/"Trash" Button stop / interrupt the progam, back to settings plage. Can start over at any time!
  
![Screenshot from 2024-03-25 12-03-01](https://github.com/pyzahl/SoFiMagic/assets/22110415/c4343c0a-c5c5-4bfb-adc3-a63160e4f5ff)

IMPORTANT: Bracketing and Aperture settings are still experimental and may casue issues/not work. DO NOT USE WITHOUT EXTENSIVE TESTING. MAY FAIL IN CERTAIN SITUATIONS.

## Settings: ##

![Screenshot from 2024-03-09 23-00-18](https://github.com/pyzahl/SoFiMagic/assets/22110415/eceec0ad-1528-4fc9-a48c-fd51befa8e62)
See at the bottom for all configuration screens.


Navigate with the navigation buttons/dial. Select field (green highlighted) to adjust using all three dials:

## Eclipse Setup Basics ##
a) setup contact times  CT1 .. CT4 to the second for your location. Make sure the Camera time is precisely set on the second!
b) setup exposure lists and intervals
c) do a test run!!!

## Adjusting times HH:MM:SS ##
use the front/back dials to adjust HH and SS, use the navigation combo dial toadjust H-1h and H+20min (custom mode).
use the front dial to adjust indicent, numbers and other settings. The 2nd and 3rd dial is applying larger increments!

Setup CT1..CT4.

Select Eclipse phase via the slider (Fron dial) and configure shooting details.

For each Eclipse Phase "NAME" a exposure program is to be configured.

	    NAME: Name of Shooting Phase:
              Partial1, Contact2, TotalityA, MaxTotaliy, TotalityB, Contact3, Partial2, END
            There may be more or any names used than default -- experimental, can be created via XML file.

	    REF_CONTACT = 0 is MAX computed from (TC2+TC3)/2. 
            PHASE START, END times are in seconds relative to REF_CONTACT_START,_END index=1,2,0,3,4

            Example 1st phase:
	      Partial1:   CT1 - 30s  to  CT2 - 10s   #Shots to be distributed (Interval shooting) 100

	    PHASE Start/End time is relative to C0, C1..C4 as defined by REF_CONTACT_START/END plus a offset START and END in seconds.
            Number shots (= Number of Exposure series to be run to be precise in interval mode) > 1: Automated Interval Shooting for this PHASE.
	    Number Shots set to -1: Repeat Exposure List as often as it fits into the time window. No delays.
            Number Shots set to 0 mean END of Lists!
	    
            For each interval the Exposure Lists are executed once.
            CF:  CAMERA_FLAG_LIST: Drive Modes are S: Single Photo, C: Continuous High, M: Continuous Medium, L: Continuous Low, B: Bracketing (experimental, may not work right)
            BT:  BURST_DURATION_LIST: Defines Burst Duration if in C,M or L Drive Mode. In Bracketing Mode (CF=B), see list of BT values below.
            ISO: ISO_LIST: ISO to be set. Must be a valid ISO number. ISO=0 MUST terminate the list (internally)
            F:   F_LIST: Aperture setting, 0: ignored. MUST be a valid Aperture. [expeimental, may not work right yet]
            t:   SHUTTER_SPEED_LIST: Shutter Speed to be set. MUST be a valid shutter speed.

        Bracketing Options:
        BT value
        3:       Bracket 0.3EV, 3 Pictures 
        4:       Bracket 0.3EV, 5 Pictures
        5:       Bracket 0.5EV, 3 Pictures
        6:       Bracket 0.5EV, 5 Pictures
        7:       Bracket 0.7EV, 3 Pictures
        8:       Bracket 0.7EV, 5 Pictures
        9:       Bracket 0.7EV, 9 Pictures ** no supported by all cameras
        10:      Bracket 1EV, 3 Pictures
        11:      Bracket 1EV, 5 Pictures
        12:      Bracket 1EV, 9 Picturesc ** no supported by all cameras
        20:      Bracket 2EV, 3 Pictures
        21:      Bracket 2EV, 5 Pictures
        22:      Bracket 2EV, 9 Picturse ** no supported by all cameras
        30:      Bracket 3EV, 3 Pictures
        31:      Bracket 3EV, 5 Pictures
        32:      Bracket 3EV, 9 Pictures ** no supported by all cameras

     

Note: if you wish you can edit the created XML file on SD card. The app stores (auto created if not existing or wrong) a XML file if the SOFIMAGI folder. And uses it if found.

## SS (Silent Shutter) ##
The silent shutter option is functionless on cameras without silent shutter mode.
WARNING: Burst/Continuous Shooting may not be available or work depending on Camera model. TEST IT! If in doubt, do not use.
Now camera is set to shutter use for busting automatically.

## MF (Manual Focus) ##
This sets focus mode to manual. Be sure to have focused before starting the app!

## DOFF (Display Off) ##
EXPERIMENTAL/TETSING -- DO NOT SELECT.
Turn the display off between each shot. This doesn't change the battery consumption but it can be healthy for the display when taking very long time lapses.


## Running it ##
After checking and adjusting the settings, activate the Start button.
The following screen will show a live view and shooting stats with time to next shot/phase and current time.

Pause/abort shooting via Menu button.
WARNING: Try to avoid pushign the button whil shooting / saving images is in progress -- may cause issues. Known bug.

## Restaring it while the Eclipse is in progress ##
YES, you can restart it any time and it will catch up to the correct point in time to continue! 
Still be cautious here, not extensive tested at this time.

## AUTO CREATED CONFIGURATION FILE and APP + SHOOTING LOG
The XML example below is auto created on first app start on SD Card as SOFIMAGI/SOFIPRG.XML.
A app log file is also created (and always appended to) in SOFIMAGI/LOG.TXT.

## CF-CARD-ROOT:/SOFIMAGI/SOFIPRG.XML

    <?xml version="1.0" encoding="UTF-8"?><sofimagic xmlns="com.github.pyzahl">
      <VALID_SOFI_PROGRAM>
        <STATUS>VALID</STATUS>
        <VERBOSE_LEVEL>VERBOSE</VERBOSE_LEVEL>
        <COMMENT>Eclipse Program: may edit this file or use App.
            NAME: Name of Shooting Phase. There may be more or any names used than default.
            PHASE Start/End time is relative to C0, C1..C4 as defined by REF_CONTACT_START/END plus a offset START and END in seconds.
            Number shots &gt; 0: Automated Interval Shooting for PHASE.
            For each interval the Exposure Lists are executed once.
            CAMERA_FLAG_LIST: Drive Modes are S: Single Photo, C: Continuous High, M: Continuous Medium, L: Continuous Low, B: Bracketing
            BURST_DURATION_LIST: Defines Burst Duration if in C,M or L Drive Mode.
            ISO_LIST: ISO to be set. Must be a valid ISO number. ISO=0 MUST terminate the list (internally)
            F_LIST: Aperture setting, 0: ignored. MUST be a valid Aperture.
            SHUTTER_SPEED_LIST: Shutter Speed to be set. MUST be a valid shutter speed.
        </COMMENT>
      </VALID_SOFI_PROGRAM>
      <ECLIPSE_REF_CONTACT_TIMES>
        <C1>11:00:00</C1>
        <C2>12:10:00</C2>
        <C3>12:14:00</C3>
        <C4>13:24:00</C4>
        <COMMENT1>C0 is MAX = (TC2+TC3)/2=12:12:00</COMMENT1>
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

Installed Apppication View:

![Screenshot from 2024-03-09 22-40-33](https://github.com/pyzahl/SoFiMagic/assets/22110415/d7d190c4-32fc-43ca-b878-b182de4ddc72)

App in action:

![Screenshot from 2024-03-09 22-39-26](https://github.com/pyzahl/SoFiMagic/assets/22110415/4c5bd69d-41d2-4ae3-ad78-8d81017de222)

All settings screenshots:

![Screenshot from 2024-03-09 23-00-18](https://github.com/pyzahl/SoFiMagic/assets/22110415/eceec0ad-1528-4fc9-a48c-fd51befa8e62)
![Screenshot from 2024-03-09 23-00-38](https://github.com/pyzahl/SoFiMagic/assets/22110415/d3d4a522-a061-4ba8-abf1-987f1aa36df7)
![Screenshot from 2024-03-09 23-00-47](https://github.com/pyzahl/SoFiMagic/assets/22110415/7059a81f-3f24-4258-8f76-ea9fbe115a34)
![Screenshot from 2024-03-09 23-01-00](https://github.com/pyzahl/SoFiMagic/assets/22110415/13a98789-d35e-461c-baf2-ed1450a5cad8)
![Screenshot from 2024-03-09 23-01-10](https://github.com/pyzahl/SoFiMagic/assets/22110415/cf6c4417-4291-467a-8a0a-c834e2440131)
![Screenshot from 2024-03-09 23-01-19](https://github.com/pyzahl/SoFiMagic/assets/22110415/7481e86b-ad25-44c4-8c74-47c74098a75b)
![Screenshot from 2024-03-09 23-01-28](https://github.com/pyzahl/SoFiMagic/assets/22110415/e6a0a5c4-d426-4f60-b792-300c64a2f7e5)

![Screenshot from 2024-05-01 10-18-37](https://github.com/pyzahl/SoFiMagic/assets/22110415/c536942b-8c5a-4da4-b846-f6887ddcf0da)

From the 2024 Eclipse:

https://youtube.com/shorts/pBB1ybe05FY?si=PzH9lR0DDI_xz8Ey


