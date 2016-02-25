package de.tuebingen.uni.sfs.clarin.tundra.perseus;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexandr Chernov on 19/02/16.
 * This is a converter for the Latin and Ancient Greek treebanks
 * taken from the Perseus project https://github.com/PerseusDL/treebank_data
 */
public class PerseusConverter {
    /**
     * Provides a list of files in a certain folder
     * @param sourceFolder the name of the folder to browse
     * @return list of files in the given folder
     */
    public static List<String> getFilesList(String sourceFolder) {
        File folder = new File(sourceFolder);
        File[] listOfFiles = folder.listFiles();
        List<String> folderList = new ArrayList<String>();;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                folderList.add(listOfFiles[i].getName());
            }
        }
        return folderList;
    }

    /**
     * Finds all child nodes of a given XML element
     * @param allElements list of all elements found in a sentence
     * @param elementId ID of a particular element
     * @return list of children for a given element
     */
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
                }
                foundChildren.add(curTokenElement);
            }
        }
        return foundChildren;
    }

    /**
     * Checks if a given XML element has children
     * @param allElements list of all elements found in a sentence
     * @param elementId ID of a particular element
     * @return true or false depending on the result
     */
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

    /**
     * Removes attributes that we consider unnecessary for our treebank, adds "start" and "finish" attributes
     * @param treeElement list of all elements found in a sentence
     * @param tokensInTotal number of child tokens for a given element
     */
    public static void removeElementAttributes(Element treeElement, Integer tokensInTotal) {
        treeElement.removeAttribute("id");
        treeElement.removeAttribute("head");
        if (treeElement.hasChildNodes()) {
            treeElement.setAttribute("start", String.valueOf(addStartAttributes(treeElement, tokensInTotal)));
            treeElement.setAttribute("finish", String.valueOf(addFinishAttributes(treeElement, 0)));
            NodeList treeElList = treeElement.getChildNodes();
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;

                    // we need to remove attributes that are not going to be used by our system
                    eElement.removeAttribute("id");
                    eElement.removeAttribute("head");

                    // we repeat this procedure for each child
                    if (eElement.hasChildNodes()) {
                        removeElementAttributes(eElement, tokensInTotal);
                    }
                    else {
                        eElement.setAttribute("start", eElement.getAttribute("order"));
                        eElement.setAttribute("finish", eElement.getAttribute("order"));
                    }
                }
            }
        }
        else {
            treeElement.setAttribute("start", treeElement.getAttribute("order"));
            treeElement.setAttribute("finish", treeElement.getAttribute("order"));
        }
    }

    /**
     * Adds "num" attributes
     * @param treeElement an XML element
     * @param curNumValue "num" attribute of the given element
     * @return "num" attribute value of the last child of the given element
     */
    public static Integer addNumAttributes(Element treeElement, Integer curNumValue) {
        curNumValue += 1;
        treeElement.setAttribute("num", String.valueOf(curNumValue));
        if (treeElement.hasChildNodes()) {
            NodeList treeElList = treeElement.getChildNodes();
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;

                    // we repeat this procedure for each child
                    if (eElement.hasChildNodes()) {
                        curNumValue = addNumAttributes(eElement, curNumValue);
                    }
                    else {
                        curNumValue += 1;
                        eElement.setAttribute("num", String.valueOf(curNumValue));
                    }
                }
            }
        }
        return curNumValue;
    }

    /**
     * Adds "start" attributes
     * @param treeElement an XML element
     * @param minValue minimal value of the "order" attribute (among all children of the given element)
     * @return "start" attribute of the given element
     */
    public static Integer addStartAttributes(Element treeElement, Integer minValue) {
        if (treeElement.hasChildNodes()) {
            NodeList treeElList = treeElement.getChildNodes();
            // finding the minimal "order" attribute among children from the current level
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    Integer curOrder = Integer.valueOf(eElement.getAttribute("order"));
                    if (curOrder < minValue) {
                        minValue = curOrder;
                    }
                }
            }

            // finding the minimal "order" attribute among all nested children (when they exist)
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    if (eElement.hasChildNodes()) {
                        Integer possibleNewMin = addStartAttributes(eElement, minValue);
                        if (possibleNewMin<minValue) {
                            minValue = possibleNewMin;
                        }
                    }
                }
            }
        }
        else {
            treeElement.setAttribute("start",treeElement.getAttribute("order"));
        }
        return minValue;
    }

    /**
     * Adds "finish" attributes
     * @param treeElement an XML element
     * @param maxValue maximal value of the "order" attribute (among all children of the given element)
     * @return "finish" attribute of the given element
     */
    public static Integer addFinishAttributes(Element treeElement, Integer maxValue) {
        if (treeElement.hasChildNodes()) {
            NodeList treeElList = treeElement.getChildNodes();
            // finding the maximal "order" attribute among children from the current level
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    Integer curOrder = Integer.valueOf(eElement.getAttribute("order"));
                    if (curOrder > maxValue) {
                        maxValue = curOrder;
                    }
                }
            }

            // finding the maximal "order" attribute among all nested children (when they exist)
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    if (eElement.hasChildNodes()) {
                        Integer possibleNewMax = addFinishAttributes(eElement, maxValue);
                        if (possibleNewMax > maxValue) {
                            maxValue = possibleNewMax;
                        }
                    }
                }
            }
        }
        else {
            treeElement.setAttribute("finish",treeElement.getAttribute("order"));
        }
        return maxValue;
    }

    /**
     * Main method of the class
     * @param args command line arguments. It takes exactly two parameters: source folder with the treebank files, and the name of the final treebank
     */
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
                NodeList testList = root.getChildNodes();

                for (int ri = 0; ri < testList.getLength(); ri++) {
                    Node immediateChildNode = testList.item(ri);
                    if (immediateChildNode.getNodeType() == Node.ELEMENT_NODE) {
                        if (immediateChildNode.getNodeName().equals("sentence")) {
                            break;
                        }
                        if (immediateChildNode.getNodeName().equals("body")) {
                            root = (Element) immediateChildNode;
                            break;
                        }
                    }
                }
                NodeList nList = root.getChildNodes();

                for (int si = 0; si < nList.getLength(); si++) {
                    Node nNode = nList.item(si); // sentence nodes
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        String eName = eElement.getNodeName();
                        // ignore this information
                        if ((eName.equals("annotator")) || (eName.equals("primary")) || (eName.equals("secondary"))) {
                            continue;
                        }
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
                                    String cName = eChildElement.getNodeName();
                                    // ignore this information
                                    if ((cName.equals("annotator")) || (cName.equals("primary")) || (cName.equals("secondary"))) {
                                        continue;
                                    }

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
                                            // this POS usually corresponds to punctuation marks
                                            if (curNodeValue.equals("u--------")) {
                                                tokenElement.setAttribute("_punct", "true");
                                            }
                                        }
                                        if (curNodeName.equals("relation")) {
                                            curNodeName = "edge";
                                        }

                                        tokenElement.setAttribute(curNodeName, curNodeValue);
                                        if (curNodeName.equals("head")) {
                                            if (curNodeValue.equals("")) {
                                                curChildAttributes.item(attr).setNodeValue("0");
                                            }
                                        }
                                    }

                                    // handling elliptic constructions: missing token is hidden, its value is placed to the "recovered" attribute
                                    if (tokenElement.hasAttribute("insertion_id")) {
                                        tokenElement.setAttribute("recovered", tokenElement.getAttribute("token"));
                                        tokenElement.setAttribute("token", "--");
                                        tokenElement.removeAttribute("insertion_id");
                                    }

                                    // adding "text" attribute
                                    if ((tokenElement.getAttribute("id").equals("1")) || (tokenElement.hasAttribute("punct"))) {
                                        tokenElement.setAttribute("text", tokenElement.getAttribute("token"));
                                    }
                                    else {
                                        tokenElement.setAttribute("text", " " + tokenElement.getAttribute("token"));
                                    }
                                    tokenElement.setAttribute("order", String.valueOf(tokenCounter));
                                    elList.add(tokenElement);
                                }
                            }
                            consElement.setAttribute("finish", String.valueOf(tokenCounter-1));
                            Integer closeToRootCount = 0;
                            Integer curChildNum = 0;
                            for (int ri = 0; ri < elList.size(); ri++) {
                                Element attachedToRoot = elList.get(ri);
                                if (attachedToRoot.getAttribute("head").equals("0")) {
                                    closeToRootCount += 1;
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
                                    removeElementAttributes(closeToRootElement, tokenCounter); // remove irrelevant for us attributes
                                    if (closeToRootCount > 1) {
                                        curChildNum = addNumAttributes(closeToRootElement, curChildNum);
                                    }
                                    else {
                                        curChildNum = addNumAttributes(closeToRootElement, Integer.valueOf(consElement.getAttribute("num")));
                                    }
                                    consElement.appendChild(closeToRootElement); // token elements that belong to the main cons element
                                    sentElement.appendChild(consElement); // adding the main cons element to the current sentence element
                                }
                            }
                            rootElement.appendChild(sentElement);
                        }
                    }
                }
            }
            // saving our results into XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT,"yes"); // setting line breaks
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(docOutput);
            StreamResult result = new StreamResult(new File(outputFile));
            transformer.transform(source, result);
            System.out.println("Writing data into the file: " + outputFile);
        }
    }
}
