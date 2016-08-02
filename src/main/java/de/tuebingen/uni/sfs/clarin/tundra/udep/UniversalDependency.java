package de.tuebingen.uni.sfs.clarin.tundra.udep;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexandr Chernov on 01/08/16.
 * This is a converter for the universal dependency treebanks taken from
 * http://universaldependencies.org/
 */
public class UniversalDependency {
    public static Integer startNum = 0;
    /**
     * Provides a list of files in a certain folder
     * @param sourceFolder the name of the folder to browse
     * @return list of files in the given folder
     */
    public static List<String> getUDFileList(String sourceFolder) {
        File folder = new File(sourceFolder);
        File[] listOfFiles = folder.listFiles();
        List<String> folderList = new ArrayList<String>();;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String curFileName = listOfFiles[i].getName();
                if (curFileName.endsWith("conllu") == true) {
                    folderList.add(listOfFiles[i].getName());
                }
            }
        }
        return folderList;
    }

    public static List<String[]> getRelationsByTarget(List<List<String[]>> inputSentenceList, String targetRelation) {
        List<String[]> resultLineArray = new ArrayList<String[]>();
        //for (int i = 0; i < inputSentenceList.size(); i++) {
            for (int k = 0; k < inputSentenceList.size(); k++) {
                List<String[]> inputLineList = inputSentenceList.get(k);
                for (int m = 0; m < inputLineList.size(); m++) {
                    String[] inputLine = inputLineList.get(m);
                    //System.out.println(inputLine[6]);
                    //System.out.println(inputLine[6].length());
                    inputLine[6] = inputLine[6].trim();
                    if (inputLine[6].equals(targetRelation)) {

                        resultLineArray.add(inputLine);
                    }
                }
            }
        //}
        return resultLineArray;
    }

    public static List<Element> getTokenList(List<List<String[]>> inputSentenceList, Document docOutput) throws ParserConfigurationException {
        List<Element> elList = new ArrayList<Element>();
        /*DocumentBuilderFactory isOutputFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder isOutputBuilder = isOutputFactory.newDocumentBuilder();
        Document isOutput = isOutputBuilder.newDocument();*/

        for (int sl = 0; sl < inputSentenceList.size(); sl++) {
            List<String[]> sentenceTokens = inputSentenceList.get(sl);
            for (int rt = 0; rt < sentenceTokens.size(); rt++) {
                String[] sentenceTokenArray = sentenceTokens.get(rt);


                String elementOrder = "";
                String elementToken = "";
                String elementLemma = "";
                String elementPos1 = "";
                String elementPos2 = "";
                String elementCategories = "";
                String elementDepTarget = "";
                String elementEdge = "";
                String elementDeps = "";
                String elementSpaceAfter = "";

                if (sentenceTokenArray.length > 0) {
                    elementOrder = sentenceTokenArray[0].trim();
                }
                if (sentenceTokenArray.length > 1) {
                    elementToken = sentenceTokenArray[1].trim();
                }
                if (sentenceTokenArray.length > 2) {
                    elementLemma = sentenceTokenArray[2].trim();
                }
                if (sentenceTokenArray.length > 3) {
                    elementPos1 = sentenceTokenArray[3].trim();
                }
                if (sentenceTokenArray.length > 4) {
                    elementPos2 = sentenceTokenArray[4].trim();
                }
                if (sentenceTokenArray.length > 5) {
                    elementCategories = sentenceTokenArray[5].trim();
                }
                if (sentenceTokenArray.length > 6) {
                    elementDepTarget = sentenceTokenArray[6].trim();
                }
                if (sentenceTokenArray.length > 7) {
                    elementEdge = sentenceTokenArray[7].trim();
                }
                if (sentenceTokenArray.length > 8) {
                    elementDeps = sentenceTokenArray[8].trim();
                }
                if (sentenceTokenArray.length > 9) {
                    elementSpaceAfter = sentenceTokenArray[9].trim();
                }

                Element tokenElement = docOutput.createElement("token");

                tokenElement.setAttribute("order", elementOrder);
                tokenElement.setAttribute("text", elementToken);
                tokenElement.setAttribute("lemma", elementLemma);
                tokenElement.setAttribute("pos", elementPos1);
                tokenElement.setAttribute("pos2", elementPos2);
                tokenElement.setAttribute("categories", elementCategories); // need to be separated
                tokenElement.setAttribute("head", elementDepTarget);
                tokenElement.setAttribute("edge", elementEdge);
                tokenElement.setAttribute("deps", elementDeps);
                tokenElement.setAttribute("spaceafter", elementSpaceAfter);

                if (elementPos1.toLowerCase().equals("punct")) {
                    tokenElement.setAttribute("_punct", "true");
                }

                elList.add(tokenElement);
            }
        }
        return elList;
    }

    public static List<Integer> getChildIndex(List<Element> tokenList, String headValue) {
        List<Integer> indexList = new ArrayList<Integer>();
        for (int tl = 0; tl < tokenList.size(); tl++) {
            Element curElement = tokenList.get(tl);
            if (curElement.getAttribute("head").equals(headValue)) {
                if (tokenHasChildren(tokenList, curElement.getAttribute("order")) == false) {
                    indexList.add(tl);
                }
            }
        }
        return indexList;
    }

    public static boolean tokenHasChildren(List<Element> tokenList, String headValue) {
        boolean hasChildTokens = false;
        for (int tl = 0; tl < tokenList.size(); tl++) {
            Element curElement = tokenList.get(tl);
            if (curElement.getAttribute("head").equals(headValue)) {
                hasChildTokens = true;
                break;
            }
        }
        return hasChildTokens;
    }

    public static boolean hasOnlyRootTokens(List<Element> tokenList) {
        boolean onlyRootTokens = false;
        for (int tl = 0; tl < tokenList.size(); tl++) {
            Element curElement = tokenList.get(tl);
            if (curElement.getAttribute("head").equals("0")) {
                onlyRootTokens = true;
            }
            else {
                onlyRootTokens = false;
            }
        }
        return onlyRootTokens;
    }

    public static List<Element> removeEmptyTokens(List<Element> tokenList) {
        boolean repeatAgain = false;
        for (int tl = 0; tl < tokenList.size(); tl++) {
            Element curElement = tokenList.get(tl);
            if (curElement.getAttribute("head").equals("-1")) {
                tokenList.remove(tl);
                repeatAgain = true;
                break;
            }
        }
        if (repeatAgain == true) {
            tokenList = removeEmptyTokens(tokenList);
        }
        return tokenList;
    }

    public static List<Element> buildDepTree(List<Element> allTokens) throws ParserConfigurationException {
        //List<Element> resultList = new ArrayList<Element>();

        /*DocumentBuilderFactory isOutputFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder isOutputBuilder = isOutputFactory.newDocumentBuilder();
        Document isOutput = isOutputBuilder.newDocument();*/

        //if (hasOnlyRootTokens(allTokens) == false)
        while (hasOnlyRootTokens(allTokens) == false) {
            for (int at = 0; at < allTokens.size(); at++) {
                Element curToken = allTokens.get(at);

                List<Integer> childIndex = getChildIndex(allTokens, curToken.getAttribute("order"));
                if (childIndex.size() > 0) {
                    Element updatedToken = allTokens.get(at);
                    for (int ci = 0; ci < childIndex.size(); ci++) {
                        Element childToken = allTokens.get(childIndex.get(ci));
                        updatedToken.appendChild(childToken);
                        //childToken.setAttribute("head", "-1");


                    }
                    allTokens.set(at, updatedToken);

                    List<Element> resultList = new ArrayList<Element>();
                    for (int rl = 0; rl < allTokens.size(); rl++) {
                        if (childIndex.contains(rl) == false) {
                            resultList.add(allTokens.get(rl));
                        }
                    }
                    allTokens.clear();
                    allTokens = resultList;
                    //allTokens = removeEmptyTokens(allTokens);
                    //allTokens = buildDepTree(allTokens);
                    //System.out.println(allTokens.size());
                    //System.out.println(at + ") - " + childIndex.size());
                    break;
                }
            }
        }
        return allTokens;
    }

    public static Element addNumAttributes(Element inputNode) {
        inputNode.removeAttribute("head");
        startNum++;
        inputNode.setAttribute("num", String.valueOf(startNum));
        if (inputNode.hasAttribute("text")) {
            System.out.println(inputNode.getAttribute("text"));
        }

        if (inputNode.hasChildNodes()) {
            NodeList treeElList = inputNode.getChildNodes();
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    //startNum++;
                    //if (eElement.hasChildNodes()) {
                        addNumAttributes(eElement);
                    //}
                    //startNum++;
                }
            }
        }
        return inputNode;
    }
    /**
     * Main method of the class
     * @param args command line arguments. It takes exactly two parameters: source folder with the treebank files, and the name of the final treebank
     */
    public static void main(String[] args) throws ParserConfigurationException, IOException, TransformerException {
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
            System.out.println("Opening the folder containing CONLLU files: " + inputFolder);
            System.out.println("**********************************************************");

            // First we need to create a new XML file for the output
            DocumentBuilderFactory dbOutputFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dOutputBuilder = dbOutputFactory.newDocumentBuilder();

            // root elements
            Document docOutput = dOutputBuilder.newDocument();
            Element rootElement = docOutput.createElement("treebank");
            docOutput.appendChild(rootElement);

            Integer sentenceCounter = 0;
            Integer prevCounterVal = 0;
            Integer tokenCounter = 0;

            // Reading files from a given folder (we are going to merge them together in one treebank file)
            List<String> conlluFiles = getUDFileList(inputFolder);
            for (int ci = 0; ci < conlluFiles.size(); ci++) {
                System.out.println("Reading the source file: " + conlluFiles.get(ci));

                //BufferedReader br = new BufferedReader(new FileReader(inputFolder+"/"+conlluFiles.get(ci)));
                List<List<String[]>> sentenceList = new ArrayList<List<String[]>>();
                // root elements

                //try {
                //    String line = br.readLine();
                //    while (line != null) {
                try (BufferedReader br = new BufferedReader(new FileReader(inputFolder+"/"+conlluFiles.get(ci)))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        // process the line.

                        line = line.trim();
                        List<String[]> lineList = new ArrayList<String[]>();
                        if ((line.length() > 0) && (line.startsWith("#") == false)) { // not a comment or empty line
                            String[] lineArray = line.split("\t"); // split by TAB characters
                            lineArray[0] = lineArray[0].trim();
                            if (lineArray[0].equals("1")) {
                                sentenceCounter++;
                                //System.out.println(sentenceCounter + ") ");

                            }
                            lineList.add(lineArray);
                            sentenceList.add(lineList);

                        }

                        //System.out.println(line);
                        if ((line.length() == 0)) { // Empty line or end of file means a sentence boundary

                            Element sentElement = docOutput.createElement("sent"); // Adding a sentence node
                            sentElement.setAttribute("id", "st" + sentenceCounter);
                            Element consElement = docOutput.createElement("cons"); // Adding the main constituent node
                            // Setting attributes for the main constituent
                            consElement.setAttribute("start", "1");
                            consElement.setAttribute("_root", "true");
                            consElement.setAttribute("num", "1");
                            consElement.setAttribute("cat", "ROOT");

                            List<Element> depTreeList = buildDepTree(getTokenList(sentenceList, docOutput));
                            for (int dt = 0; dt < depTreeList.size(); dt++) {
                                consElement.appendChild(depTreeList.get(dt));
                            }
                            //consElement = addNumAttributes(consElement, 0);
                            //startNum = 0;
                            addNumAttributes(consElement);
                            sentElement.appendChild(consElement);
                            rootElement.appendChild(sentElement);

                            sentenceList.clear();
                        }

                        if (sentenceCounter>2) {
                            break;
                        }
                    }
                }

                if (sentenceCounter>2) {
                    break;
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
