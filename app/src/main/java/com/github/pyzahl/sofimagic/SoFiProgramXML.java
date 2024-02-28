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
        Logger.log("XMLPRG: SoFiProgramXML, checking for " + XMLFileName);
        try {
            Logger.log("XMLPRG: check/create " + XMLFileName);
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
                for (int i = 0; i < nListPH.getLength(); i++) {
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
                        Settings.magic_program[i].CameraFlags = getValue("CAMERA_FLAGS", phase);
                        String[] iso_list = getValue("ISO_LIST", phase).split(",");
                        int k;
                        for (k=0; k<iso_list.length; k++)
                            Settings.magic_program[i].ISOs[k]=Integer.parseInt(iso_list[k]);
                        for (; k < Settings.magic_program[i].ISOs.length; k++)
                            Settings.magic_program[i].ISOs[k]=0; // this will terminate no need to clear the other lists.
                        String[] f_list = getValue("F_LIST", phase).split(",");
                        for (k=0; k<f_list.length; k++)
                            Settings.magic_program[i].Fs[k]=Integer.parseInt(f_list[k]);
                        String[] ss_list = getValue("SHUTTER_SPEED_LIST", phase).split(",");
                        for (k=0; k<ss_list.length; k++) {
                            String[] ss = ss_list[k].split("/");
                            Settings.magic_program[i].ShutterSpeeds[k][0]=Integer.parseInt(ss[0]);
                            Settings.magic_program[i].ShutterSpeeds[k][1]=Integer.parseInt(ss[1]);
                        }
                    }
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
            Logger.log("XMLPRG: check/creating new " + XMLFileName);
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
            root.appendChild(elementV);

            Element elementTC = doc.createElement("ECLIPSE_REF_CONTACT_TIMES");
            appendElement(elementTC, "C1", getHMSfromS(Settings.tc1));
            appendElement(elementTC, "C2", getHMSfromS(Settings.tc2));
            appendElement(elementTC, "C3", getHMSfromS(Settings.tc3));
            appendElement(elementTC, "C4", getHMSfromS(Settings.tc4));
            appendElement(elementTC, "COMMENT1", "C0 is MAX = (TC2+TC3)/2="+getHMSfromS((Settings.tc2+Settings.tc3)/2));
            appendElement(elementTC, "COMMENT2", "REF_CONTACT = 0 is MAX computed from (TC2+TC3)/2. PHASE START, END times in sec relative to REF_CONTACT_START,_END index=1,2,0,3,4");
            root.appendChild(elementTC);

            for (int i = 0; Settings.magic_program[i].number_shots != 0; i++) {
                Logger.log("XMLPRG: creating magic phase settings for: " + Settings.magic_program[i].name);
                Element elementPH = doc.createElement("PHASE");
                appendElement(elementPH, "NAME", Settings.magic_program[i].name);
                appendElement(elementPH, "REF_CONTACT_START", Integer.toString(Settings.magic_program[i].ref_contact_start));
                appendElement(elementPH, "START", Integer.toString(Settings.magic_program[i].start_time));
                appendElement(elementPH, "REF_CONTACT_END", Integer.toString(Settings.magic_program[i].ref_contact_end));
                appendElement(elementPH, "END", Integer.toString(Settings.magic_program[i].end_time));
                appendElement(elementPH, "NUMBER_SHOTS", Integer.toString(Settings.magic_program[i].number_shots));
                appendElement(elementPH, "CAMERA_FLAGS", Settings.magic_program[i].CameraFlags);
                String ISO_list="";
                String F_list="";
                String SHUTTER_list="";
                for (int k=0; k<Settings.magic_program.length; ++k){
                    if (Settings.magic_program[i].ISOs[k] > 0) {
                        ISO_list = ISO_list + Integer.toString(Settings.magic_program[i].ISOs[k]) + ",";
                        F_list = F_list + Integer.toString(Settings.magic_program[i].Fs[k]) + ",";
                        SHUTTER_list = SHUTTER_list + Integer.toString(Settings.magic_program[i].ShutterSpeeds[k][0]) + "/" + Integer.toString(Settings.magic_program[i].ShutterSpeeds[k][1]) + ",";
                    }
                }
                appendElement(elementPH, "ISO_LIST", ISO_list);
                appendElement(elementPH, "F_LIST", F_list);
                appendElement(elementPH, "SHUTTER_SPEED_LIST", SHUTTER_list);
                root.appendChild(elementPH);
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
            Logger.log("XMLPRG: building XML DONE");
        } catch (Exception e) {
            Logger.error("XMLPRG: File/XML create exception for "+XMLFileName);
            e.printStackTrace();
        }
    }

   private static String getValue(String tag, Element element) {
      NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
      Node node = nodeList.item(0);
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
