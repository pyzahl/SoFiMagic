package com.github.pyzahl.sofimagic;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Set;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

//import java.io.InputStream;
//import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class SoFiProgramXML {
    final static String XMLFileName = "SOFIMAGI/SOFIPRG.XML";
    public static File getXMLFile() {
        return new File(Environment.getExternalStorageDirectory(), XMLFileName);
    }
    SoFiProgramXML() {
        Logger.log("XMLPRG new: SoFiProgramXML, checking for " + XMLFileName);
        try {
            Logger.log("XMLPRG new: check/load/create " + XMLFileName);
            getXMLFile().getParentFile().mkdirs();

            // https://developer.android.com/reference/javax/xml/parsers/DocumentBuilder

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(getXMLFile());
            doc.getDocumentElement().normalize();

            Logger.log("XMLPRG: Root element: " + doc.getDocumentElement().getNodeName());
            Logger.log("XMLPRG: reading XML");

            NodeList nListCheck = doc.getElementsByTagName("VALID_SOFI_PROGRAM");
            if (nListCheck.getLength() > 0) {
                Logger.log("XMLPRG: valid SOFI XML Program Mark found, parsing...");

                Node node_loglevel = nListCheck.item(0);
                if (node_loglevel.getNodeType() == Node.ELEMENT_NODE){
                    Element level = (Element) node_loglevel;
                    Settings.setVerboseLevel (getValue("LOGGING_LEVEL", level));
                }

                NodeList nListTC = doc.getElementsByTagName("ECLIPSE_REF_CONTACT_TIMES");
                if (nListTC.getLength() > 0) {
                    Node node = nListTC.item(0);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element phase = (Element) node;
                        String time_c1 = getValue("C1", phase);
                        String time_c2 = getValue("C2", phase);
                        String time_c3 = getValue("C3", phase);
                        String time_c4 = getValue("C4", phase);
                        Settings.set_contact_times(time_c1, time_c2, time_c3, time_c4);
                    }
                }

                NodeList nListPH = doc.getElementsByTagName("PHASE");
                for (int i = 0; i < nListPH.getLength() && i < Settings.magic_program.length; i++) {
                    Node node = nListPH.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element phase = (Element) node;
                        Settings.magic_program[i].name = getValue("NAME", phase);
                        Logger.log("XMLPRG: reading magic phase settings for: " + Settings.magic_program[i].name);
                        Settings.magic_program[i].ref_contact_start = Integer.parseInt(getValue("REF_CONTACT_START", phase));
                        Settings.magic_program[i].start_time = Integer.parseInt(getValue("START", phase));
                        Settings.magic_program[i].ref_contact_end = Integer.parseInt(getValue("REF_CONTACT_END", phase));
                        Settings.magic_program[i].end_time = Integer.parseInt(getValue("END", phase));
                        Settings.magic_program[i].number_shots = Integer.parseInt(getValue("NUMBER_SHOTS", phase));
                        Settings.magic_program[i].set_CFs_list(getValue("CAMERA_FLAG_LIST", phase));
                        Settings.magic_program[i].set_BurstDurations_list(getValue("BURST_DURATION_LIST", phase));
                        Settings.magic_program[i].set_ISOs_list(getValue("ISO_LIST", phase));
                        Settings.magic_program[i].set_F_list(getValue("F_LIST", phase));
                        Settings.magic_program[i].set_SHUTTER_SPEEDS_list(getValue("SHUTTER_SPEED_LIST", phase));
                        Logger.log("XMLPRG: reading exposure lists completed for phase " + Settings.magic_program[i].name);
                    }
                    if (Settings.magic_program[i].number_shots == 0)
                        break;
                }
            } else { // create default template
                Logger.log("XMLPRG: no valid SOFI XML Program Mark found, creating defaults...");
                SoFiProgramStoreXML();
            }

        } catch (Exception e) {
            Logger.error("XMLPRG: File/XML parse exception/non existing for "+XMLFileName);
            SoFiProgramStoreXML();
            e.printStackTrace();
        }
    }
    public void SoFiProgramStoreXML() {
        Logger.log("XMLPRG: SoFiProgramStoreXML");
        try {
            Logger.log("XMLPRG: store/creating new " + XMLFileName);
            getXMLFile().getParentFile().mkdirs();

            // https://developer.android.com/reference/javax/xml/parsers/DocumentBuilder
            //https://developer.android.com/reference/org/w3c/dom/Document
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element root = doc.createElementNS("com.github.pyzahl", "sofimagic");
            doc.appendChild(root);

            Element elementV = doc.createElement("VALID_SOFI_PROGRAM");
            appendElement(elementV, "STATUS", "VALID");
            appendElement(elementV, "LOGGING_LEVEL", "VERBOSE");
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
                    "SHUTTER_SPEED_LIST: Shutter Speed to be set. MUST be a valid shutter speed.");
            root.appendChild(elementV);

            Element elementTC = doc.createElement("ECLIPSE_REF_CONTACT_TIMES");
            appendElement(elementTC, "C1", getHMSfromS(Settings.tc1));
            appendElement(elementTC, "C2", getHMSfromS(Settings.tc2));
            appendElement(elementTC, "C3", getHMSfromS(Settings.tc3));
            appendElement(elementTC, "C4", getHMSfromS(Settings.tc4));
            appendElement(elementTC, "COMMENT1", "C0 is MAX = (TC2+TC3)/2="+getHMSfromS((Settings.tc2+Settings.tc3)/2));
            appendElement(elementTC, "COMMENT2", "REF_CONTACT = 0 is MAX computed from (TC2+TC3)/2. PHASE START, END times in sec relative to REF_CONTACT_START,_END index=1,2,0,3,4");
            root.appendChild(elementTC);

            for (int i = 0; Settings.magic_program[i].number_shots != 0 && i < Settings.magic_program.length-1; i++) {
                Logger.log("XMLPRG: creating magic phase settings for: " + Settings.magic_program[i].name);
                Element elementPH = doc.createElement("PHASE");
                appendElement(elementPH, "NAME", Settings.magic_program[i].name);
                appendElement(elementPH, "REF_CONTACT_START", Integer.toString(Settings.magic_program[i].ref_contact_start));
                appendElement(elementPH, "START", Integer.toString(Settings.magic_program[i].start_time));
                appendElement(elementPH, "REF_CONTACT_END", Integer.toString(Settings.magic_program[i].ref_contact_end));
                appendElement(elementPH, "END", Integer.toString(Settings.magic_program[i].end_time));
                appendElement(elementPH, "NUMBER_SHOTS", Integer.toString(Settings.magic_program[i].number_shots));

                String cf_list=Settings.magic_program[i].get_CFs_list();
                appendElement(elementPH, "CAMERA_FLAG_LIST", cf_list);
                String cn_list=Settings.magic_program[i].get_BurstDurations_list();
                appendElement(elementPH, "BURST_DURATION_LIST", cn_list);
                String ISO_list=Settings.magic_program[i].get_ISOs_list();
                appendElement(elementPH, "ISO_LIST", ISO_list);
                String F_list=Settings.magic_program[i].get_Fs_list();
                appendElement(elementPH, "F_LIST", F_list);
                String SHUTTER_list=Settings.magic_program[i].get_ShutterSpeeds_list();
                appendElement(elementPH, "SHUTTER_SPEED_LIST", SHUTTER_list);
                root.appendChild(elementPH);

                if (Settings.magic_program[i].number_shots == 0)
                    break;
            }


            // DONE CREATING DOC

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transf = transformerFactory.newTransformer();

            transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transf.setOutputProperty(OutputKeys.INDENT, "yes");
            transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);

            StreamResult file = new StreamResult(getXMLFile());
            transf.transform(source, file);
            Logger.log("XMLPRG: store XML completed");
        } catch (Exception e) {
            Logger.error("XMLPRG: File/XML store/create exception for "+XMLFileName);
            e.printStackTrace();
        }
    }

   private static String getValue(String tag, Element element) {
      NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
      Node node = nodeList.item(0);
      Logger.log("XMLPRG: reading <" + tag + "> = " + node.getNodeValue());
      return node.getNodeValue();
      //node.setNodeValue();
   }
    private Element appendElement(Element parent, String name, String text) {
        Document document = parent.getOwnerDocument();
        Element child = document.createElement(name);
        parent.appendChild(child);
        if (text != null) {
            Text textNode = document.createTextNode(text);
            child.appendChild(textNode);
        }
        return child;
    }


    public String getHMSfromS(long s) {
        long tmp = Math.abs(s);
        long HH = tmp / 3600;
        tmp -= HH * 3600;
        long MM = tmp / 60;
        tmp -= MM * 60;
        long SS = tmp;
        if (s >= 0)
            return String.format("%02d:%02d:%02d", HH, MM, SS);
        else
            return String.format("-%02d:%02d:%02d", HH, MM, SS);
    }

}
