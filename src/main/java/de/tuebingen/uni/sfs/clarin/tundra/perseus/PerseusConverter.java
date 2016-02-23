package de.tuebingen.uni.sfs.clarin.tundra.perseus;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by megalex on 19/02/16.
 */
public class PerseusConverter {
    public void convert(String inputString) {
        System.out.println(inputString);
    }

    /**
     * @param sourceFolder the name of the folder to browse
     * @return list of files in this folder
     */
    public static List<String> getFilesList(String sourceFolder) {
        File folder = new File(sourceFolder);
        File[] listOfFiles = folder.listFiles();
        List<String> folderList = new ArrayList<String>();;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                //System.out.println("File " + listOfFiles[i].getName());
                folderList.add(listOfFiles[i].getName());
            }
        }
        return folderList;
    }

    public static List<Element> findAllChildren(List<Element> allElements, String elementId) {
        List<Element> foundChildren = new ArrayList<Element>();
        for (int ai = 0; ai < allElements.size(); ai++) {
            Element curTokenElement = allElements.get(ai);



            if (curTokenElement.getAttribute("head").equals(elementId)) {
                if (elementHasChildren(allElements, curTokenElement.getAttribute("id"))) {
                    List<Element> curFoundChildren = new ArrayList<Element>();
                    curFoundChildren = findAllChildren(allElements, curTokenElement.getAttribute("id"));
                    for (int fi = 0; fi < curFoundChildren.size(); fi++) {
                        curTokenElement.appendChild(curFoundChildren.get(fi));
                    }
                    //childrenCount += curFoundChildren.size();

                }


                foundChildren.add(curTokenElement);
                //childrenCount = 0;
            }



            Integer childrenNum = countChildren(allElements, curTokenElement.getAttribute("id"), 0);
            curTokenElement.setAttribute("children", String.valueOf(childrenNum));

        }
        return foundChildren;
    }

    public static Integer countChildren(List<Element> allElements, String elementId, Integer childrenNumber) {
        //Integer childrenNumber = 0;
        for (int ai = 0; ai < allElements.size(); ai++) {
            Element curTokenElement = allElements.get(ai);

            if (curTokenElement.getAttribute("head").equals(elementId)) {

                if (elementHasChildren(allElements, curTokenElement.getAttribute("id"))) {
                    //childrenNumber += 1;

                    childrenNumber = childrenNumber + countChildren(allElements,curTokenElement.getAttribute("id"), childrenNumber);

                }

                else {
                    childrenNumber += 1;
                }



            }
        }
        return childrenNumber;
    }

    public static boolean elementHasChildren(List<Element> allElements, String elementId) {
        Boolean itHasChildren = false;
        for (int ch = 0; ch < allElements.size(); ch++) {
            Element curElement = allElements.get(ch);
            if (curElement.getAttribute("head").equals(elementId)) {
                itHasChildren = true;
                break;
            }
        }
        return itHasChildren;
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        if (args == null) {
            System.err.println("Missing argument(s)!\n");
            return;
        }
        if (args.length > 2) {
            System.err.println("Too many arguments!\n");
            return;
        }
        if (args.length == 2) {
            String inputFolder = args[0];
            String outputFile = args[1];
            System.out.println("Opening the folder containing XML files: " + inputFolder);
            System.out.println("**********************************************************");



            // First we need to create a new XML file for the output
            DocumentBuilderFactory dbOutputFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dOutputBuilder = dbOutputFactory.newDocumentBuilder();

            // root elements
            Document docOutput = dOutputBuilder.newDocument();
            Element rootElement = docOutput.createElement("treebank");
            docOutput.appendChild(rootElement);
            Integer sentenceCounter = 0;
            Integer tokenCounter = 0;

            // Reading files from a given folder (we are going to merge them together in one treebank file)
            List<String> xmlFiles = getFilesList(inputFolder);
            for (int xi = 0; xi < xmlFiles.size(); xi++) {
                System.out.println("Reading the source file: " + xmlFiles.get(xi));
                // Reading XML
                DocumentBuilderFactory dbInputFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dInputBuilder = dbInputFactory.newDocumentBuilder();
                Document docInput = dInputBuilder.parse(new File(inputFolder+"/"+ xmlFiles.get(xi)));
                /* Getting the root node of the current xml file, i.e. <treebank> node.
                Now we need to get its children, i.e. <sentence> nodes, and afterwards
                <word> nodes with all their attributes
                */
                Element root = docInput.getDocumentElement();
                NodeList nList = root.getChildNodes();





                for (int si = 0; si < nList.getLength(); si++) {
                    Node nNode = nList.item(si); // sentence nodes
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                        Map<String, String> nodeXMLattributes = new HashMap<String, String>();
                        Element eElement = (Element) nNode;
                        NamedNodeMap curAttributes = eElement.getAttributes();

                        if (curAttributes.getLength()>0) {
                            Element sentElement = docOutput.createElement("sent");
                            Element consElement = docOutput.createElement("cons");
                            consElement.setAttribute("start", String.valueOf(tokenCounter+1));
                            consElement.setAttribute("num", String.valueOf(tokenCounter+1));
                            consElement.setAttribute("cat", "ROOT");
                            consElement.setAttribute("_root", "true");
                            sentenceCounter += 1;
                            // Start adding attributes to the sentence node
                            sentElement.setAttribute("id", "st" + sentenceCounter);

                            // Copy all sentence attributes from the source node to the new one
                            for (int attr = 0; attr < curAttributes.getLength(); attr++) {
                                if (curAttributes.item(attr).getNodeName().equals("id") == false) {
                                    sentElement.setAttribute(curAttributes.item(attr).getNodeName(), curAttributes.item(attr).getNodeValue());
                                }
                            }

                            // Now we create a nested structure for the words
                            NodeList nChildList = nNode.getChildNodes(); // getting children of a sentence
                            List<Element> elList = new ArrayList<Element>(); // store all elements in a list


                            for (int wi = 0; wi < nChildList.getLength(); wi++) {
                                Node nChildNode = nChildList.item(wi); // words nodes
                                if (nChildNode.getNodeType() == Node.ELEMENT_NODE) {

                                    Element eChildElement = (Element) nChildNode;
                                    NamedNodeMap curChildAttributes = eChildElement.getAttributes();
                                    Element tokenElement = docOutput.createElement("token");
                                    tokenCounter += 1;
                                    for (int attr = 0; attr < curChildAttributes.getLength(); attr++) {
                                        String curNodeName = curChildAttributes.item(attr).getNodeName();
                                        String curNodeValue = curChildAttributes.item(attr).getNodeValue();
                                        // Remapping attribute names
                                        if (curNodeName.equals("form")) {
                                            curNodeName = "token";
                                        }
                                        if (curNodeName.equals("postag")) {
                                            curNodeName = "pos";
                                            if (curNodeValue.equals("u--------")) {
                                                tokenElement.setAttribute("_punct", "true");
                                            }
                                        }
                                        if (curNodeName.equals("relation")) {
                                            curNodeName = "edge";
                                        }

                                        //tokenElement.setAttribute(curChildAttributes.item(attr).getNodeName(), curChildAttributes.item(attr).getNodeValue());
                                        tokenElement.setAttribute(curNodeName, curNodeValue);
                                        if (curNodeName.equals("head")) {
                                            if (curNodeValue.equals("")) {
                                                curChildAttributes.item(attr).setNodeValue("0");
                                            }
                                        }
                                    }
                                    tokenElement.setAttribute("start", String.valueOf(tokenCounter-1));
                                    tokenElement.setAttribute("num", String.valueOf(tokenCounter));
                                    tokenElement.setAttribute("finish", "");
                                    tokenElement.setAttribute("order", String.valueOf(tokenCounter-1));
                                    elList.add(tokenElement);
                                }
                            }
                            consElement.setAttribute("finish", String.valueOf(tokenCounter-1));



                            for (int ri = 0; ri < elList.size(); ri++) {
                                Element attachedToRoot = elList.get(ri);
                                if (attachedToRoot.getAttribute("head").equals("0")) {
                                    List<Element> closeToRootChildren = new ArrayList<Element>();
                                    for (int ui = 0; ui < elList.size(); ui++) {
                                        Element attachedChild = elList.get(ui);
                                        if (attachedChild.getAttribute("head").equals(attachedToRoot.getAttribute("id"))) {
                                            closeToRootChildren.add(attachedChild);
                                        }
                                    }
                                    Element closeToRootElement = attachedToRoot;
                                    List<Element> itsChildren = findAllChildren(elList,closeToRootElement.getAttribute("id"));
                                    for (int chidInd = 0; chidInd < itsChildren.size(); chidInd++) {
                                        closeToRootElement.appendChild(itsChildren.get(chidInd));
                                    }
                                    consElement.appendChild(closeToRootElement);
                                    sentElement.appendChild(consElement);
                                }
                            }

                            rootElement.appendChild(sentElement);
                        }

                    }


                }


                if (xi>0) {
                    break;
                }
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(docOutput);
                StreamResult result = new StreamResult(new File(outputFile));
                transformer.transform(source, result);
                System.out.println("Writing data into the file: " + outputFile);


            }
        }




    }

}
