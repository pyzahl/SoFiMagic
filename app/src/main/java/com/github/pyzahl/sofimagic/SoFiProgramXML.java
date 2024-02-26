package com.github.pyzahl.sofimagic;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SoFiProgramXML {
    public static File getFile() {
        return new File(Environment.getExternalStorageDirectory(), "SOFIMAGI/SOFI_PRG.TXT");
    }
    SoFiProgramXML() {
        try {
            getFile().getParentFile().mkdirs();
            //InputStream is = new FileInputStream(getFile());

            // https://developer.android.com/reference/javax/xml/parsers/DocumentBuilder

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            //Document doc = dBuilder.parse(is);
            Document doc = dBuilder.parse((getFile());

            Element element = doc.getDocumentElement();
            element.normalize();

            NodeList nList = doc.getElementsByTagName("PHASE");

            for (int i = 0; i < nList.getLength(); i++) {

                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element phase = (Element) node;
                    String name = getValue("name", phase);
                    String start = getValue("start", phase);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public SoFiProgramStorXML() {
        try {
            getFile().getParentFile().mkdirs();
            //OutputStream os = new FileOutputStream(getFile());

            // https://developer.android.com/reference/javax/xml/parsers/DocumentBuilder

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            //Document doc = dBuilder.parse(is);
            Document doc = dBuilder.parse((getFile());
            //https://developer.android.com/reference/org/w3c/dom/Document
            Element element = doc.createElement("PHASE");

            doc.appendChild(element);
            //Element element = doc.getDocumentElement();
            //element.normalize();

            NodeList nList = doc.getElementsByTagName("PHASE");

            for (int i = 0; i < nList.getLength(); i++) {

                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element phase = (Element) node;
                    String name = getValue("name", phase);
                    String start = getValue("start", phase);
                }
            }

        } catch (Exception e) {
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
}
