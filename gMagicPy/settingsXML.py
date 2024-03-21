import os
import xml.etree.ElementTree as ET
from settings import *
from logger import *

XMLFileName = "SOFIPRGPY.XML"

def getXMLFile():
    return os.path.join(os.path.expanduser("."), XMLFileName)

class SoFiProgramXML:
    def __init__(self, settings):
        Settings = settings
        Logger.log("XMLPRG new: SoFiProgramXML, checking for " + getXMLFile())
        try:
            Logger.log("XMLPRG new: check/load/create " + XMLFileName)
            os.makedirs(os.path.dirname(getXMLFile()), exist_ok=True)
            
            tree = ET.parse(getXMLFile())
            root = tree.getroot()

            #print ("ET **** XML PARSE ****")
            #for child in root:
            #    print(child.tag, child.attrib)
            
            Logger.log("XMLPRG: Root element: " + root.tag)
            Logger.log("XMLPRG: reading XML")
            nListCheck = root.findall("VALID_SOFI_PROGRAM")
            print (nListCheck)
            if len(nListCheck) > 0:
                Logger.log("XMLPRG: valid SOFI XML Program Mark found, parsing...")
                node_loglevel = nListCheck[0]
                level = node_loglevel.find("LOGGING_LEVEL").text
                Settings.setVerboseLevel(int(level))
                
                nListTC = root.findall("ECLIPSE_REF_CONTACT_TIMES")
                if len(nListTC) > 0:
                    node = nListTC[0]
                    time_c1 = node.find("C1").text
                    time_c2 = node.find("C2").text
                    time_c3 = node.find("C3").text
                    time_c4 = node.find("C4").text
                    Settings.set_contact_times(time_c1, time_c2, time_c3, time_c4)
                
                nListPH = root.findall("PHASE")
                for i in range(min(len(nListPH), len(Settings.magic_program))):
                    node = nListPH[i]
                    phase = node.find("NAME").text
                    Settings.magic_program[i].name = phase
                    Logger.log("XMLPRG: reading magic phase settings for: " + phase)
                    Settings.magic_program[i].ref_contact_start = int(node.find("REF_CONTACT_START").text)
                    Settings.magic_program[i].start_time = int(node.find("START").text)
                    Settings.magic_program[i].ref_contact_end = int(node.find("REF_CONTACT_END").text)
                    Settings.magic_program[i].end_time = int(node.find("END").text)
                    Settings.magic_program[i].number_shots = int(node.find("NUMBER_SHOTS").text)
                    Settings.magic_program[i].set_ISOs_list(node.find("ISO_LIST").text)
                    Settings.magic_program[i].set_CFs_list(node.find("CAMERA_FLAG_LIST").text)
                    Settings.magic_program[i].set_BurstDurations_list(node.find("BURST_DURATION_LIST").text)
                    Settings.magic_program[i].set_F_list(node.find("F_LIST").text)
                    Settings.magic_program[i].set_SHUTTER_SPEEDS_list(node.find("SHUTTER_SPEED_LIST").text)
                    Logger.log("XMLPRG: reading exposure lists completed for phase " + phase)
                    if Settings.magic_program[i].number_shots == 0:
                        break
            else: 
                Logger.log("XMLPRG: no valid SOFI XML Program Mark found, creating defaults...")
                self.SoFiProgramStoreXML()
        except Exception as e:
            Logger.error("XMLPRG: File/XML parse exception/non existing for "+XMLFileName)
            self.SoFiProgramStoreXML()
            Logger.error(e)
    
    def SoFiProgramStoreXML(self):
        Logger.log("XMLPRG: SoFiProgramStoreXML")
        try:
            Logger.log("XMLPRG: store/creating new " + XMLFileName)
            os.makedirs(os.path.dirname(getXMLFile()), exist_ok=True)
            
            root = ET.Element("com.github.pyzahl", "sofimagic")
            doc = ET.ElementTree(root)
            elementV = ET.SubElement(root, "VALID_SOFI_PROGRAM")
            appendElement(elementV, "STATUS", "VALID")
            appendElement(elementV, "LOGGING_LEVEL", str(Logger.get_verbose_level()))
            appendElement(elementV, "COMMENT", "Eclipse Program: may edit this file or use App.\n" +
                    "NAME: Name of Shooting Phase. There may be more or any names used than default. PHASES must be listed in actual shooting order i.e. with ascending START times.\n" +
                    "PHASE Start/End time is relative to C0, C1..C4 as defined by REF_CONTACT_START/END plus a offset START and END in seconds.\n" +
                    "Number shots > 0: Automated Interval Shooting for PHASE.\n" +
                    "For each interval the Exposure Lists are executed once.\n" +
                    "CAMERA_FLAG_LIST: Drive Modes are S: Single Photo, C: Continuous High, M: Continuous Medium, L: Continuous Low, B: Bracketing\n" +
                    "BURST_DURATION_LIST: Defines Burst Duration if in C,M or L Drive Mode.\n" +
                    "               NOTE: For Bracketing Drive Mode B this is the number of Brackets. Only 3 and 5 are valid, else defaulting to 3.\n" +
                    "ISO_LIST: ISO to be set. Must be a valid ISO number. ISO=0 MUST terminate the list (internally)\n" +
                    "F_LIST: Aperture setting, 0: ignored. MUST be a valid Aperture.\n" +
                    "SHUTTER_SPEED_LIST: Shutter Speed to be set. MUST be a valid shutter speed.")
            elementTC = ET.SubElement(root, "ECLIPSE_REF_CONTACT_TIMES")
            appendElement(elementTC, "C1", getHMSfromS(Settings.tc1))
            appendElement(elementTC, "C2", getHMSfromS(Settings.tc2))
            appendElement(elementTC, "C3", getHMSfromS(Settings.tc3))
            appendElement(elementTC, "C4", getHMSfromS(Settings.tc4))
            appendElement(elementTC, "COMMENT1", "C0 is MAX = (TC2+TC3)/2="+getHMSfromS((Settings.tc2+Settings.tc3)/2))
            appendElement(elementTC, "COMMENT2", "REF_CONTACT = 0 is MAX computed from (TC2+TC3)/2. PHASE START, END times in sec relative to REF_CONTACT_START,_END index=1,2,0,3,4")
            for i in range(min(len(Settings.magic_program), len(Settings.magic_program)-1)):
                phase = Settings.magic_program[i].name
                Logger.log("XMLPRG: creating magic phase settings for: " + phase)
                elementPH = ET.SubElement(root, "PHASE")
                appendElement(elementPH, "NAME", phase)
                appendElement(elementPH, "REF_CONTACT_START", str(Settings.magic_program[i].ref_contact_start))
                appendElement(elementPH, "START", str(Settings.magic_program[i].start_time))
                appendElement(elementPH, "REF_CONTACT_END", str(Settings.magic_program[i].ref_contact_end))
                appendElement(elementPH, "END", str(Settings.magic_program[i].end_time))
                appendElement(elementPH, "NUMBER_SHOTS", str(Settings.magic_program[i].number_shots))
                ISO_list = Settings.magic_program[i].get_ISOs_list()
                appendElement(elementPH, "ISO_LIST", ISO_list)
                cf_list = Settings.magic_program[i].get_CFs_list()
                appendElement(elementPH, "CAMERA_FLAG_LIST", cf_list)
                cn_list = Settings.magic_program[i].get_BurstDurations_list()
                appendElement(elementPH, "BURST_DURATION_LIST", cn_list)
                F_list = Settings.magic_program[i].get_Fs_list()
                appendElement(elementPH, "F_LIST", F_list)
                SHUTTER_list = Settings.magic_program[i].get_ShutterSpeeds_list()
                appendElement(elementPH, "SHUTTER_SPEED_LIST", SHUTTER_list)
            tree = ET.ElementTree(root)
            tree.write(getXMLFile(), encoding="UTF-8", xml_declaration=True)
            Logger.log("XMLPRG: store XML completed")
        except Exception as e:
            Logger.error("XMLPRG: File/XML store/create exception for "+XMLFileName)
            Logger.error(e)
    
    def getValue(self, tag, element):
        nodeList = element.findall(tag)[0].text
        node = nodeList[0]
        Logger.log("XMLPRG: reading <" + tag + "> = " + node)
        return node
    
    def appendElement(self, parent, name, text):
        child = ET.SubElement(parent, name)
        if text is not None:
            child.text = text
        return child
    
    def getHMSfromS(self, s):
        tmp = abs(s)
        HH = tmp // 3600
        tmp -= HH * 3600
        MM = tmp // 60
        tmp -= MM * 60
        SS = tmp
        if s >= 0:
            return "{:02d}:{:02d}:{:02d}".format(HH, MM, SS)
        else:
            return "-{:02d}:{:02d}:{:02d}".format(HH, MM, SS)


