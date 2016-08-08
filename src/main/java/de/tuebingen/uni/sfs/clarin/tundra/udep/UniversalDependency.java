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
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexandr Chernov on 01/08/16.
 * This is a converter for the universal dependency treebanks taken from
 * http://universaldependencies.org/
 */
public class UniversalDependency {
    public static Integer startNum = 0;
    public static Integer minOrder = 0;
    public static Integer maxOrder = 0;
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


    /**
     * Provides a list of folders in a certain folder
     * @param sourceFolder the name of the folder to browse
     * @return list of folders in the given folder
     */
    public static List<String> getUDFolderList(String sourceFolder) {
        File folder = new File(sourceFolder);
        File[] listOfFiles = folder.listFiles();
        List<String> folderList = new ArrayList<String>();;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isDirectory()) {
                String curFolderName = listOfFiles[i].getName();
                folderList.add(curFolderName);
            }
        }
        return folderList;
    }

    public static List<String> fastSplit(String inputString, String delimiter) {
        List<String> outputArray = new ArrayList<>();

        int start = 0;
        while (true) {
            int found = inputString.indexOf(delimiter, start);
            if (found != -1) {
                outputArray.add(inputString.substring(start, found));
                //System.out.println(inputString.substring(start, found));
            }
            else {
                break;
            }
            start = found + 1;  // move start up for next iteration
        }


        return outputArray;
    }

    /**
     * Returns a list of all tokens in a given sentence
     * @param inputSentenceList tokens as strings
     * @param docOutput document object to create tokens of the Element type
     * @param tokenCount number of tokens in the previous sentence (needed for setting attribute values)
     * @return a list of all tokens in a given sentence
     */
    //public static List<Element> getTokenList(List<List<String[]>> inputSentenceList, Document docOutput, Integer tokenCount) throws ParserConfigurationException {
    public static List<Element> getTokenList(List<List<List<String>>> inputSentenceList, Document docOutput, Integer tokenCount) throws ParserConfigurationException {
        List<Element> elList = new ArrayList<Element>();
        for (int sl = 0; sl < inputSentenceList.size(); sl++) {
            //List<String[]> sentenceTokens = inputSentenceList.get(sl);
            List<List<String>> sentenceTokens = inputSentenceList.get(sl);
            for (int rt = 0; rt < sentenceTokens.size(); rt++) {
                //String[] sentenceTokenArray = sentenceTokens.get(rt);
                List<String> sentenceTokenArray = sentenceTokens.get(rt);

                String elementOrder = "";
                String elementToken = "";
                String elementText = "";
                String elementLemma = "";
                String elementPos1 = "";
                String elementPos2 = "";
                String elementCategories = "";
                String elementDepTarget = "";
                String elementEdge = "";
                String elementDeps = "";
                String elementSpaceAfter = "";

                String elementNumber = "";

                //if (sentenceTokenArray.length > 0) {
                if (sentenceTokenArray.size() > 0) {
                    //elementOrder = sentenceTokenArray[0].trim();
                    elementOrder = sentenceTokenArray.get(0).trim();
                    if (elementOrder.contains("-")) {
                        continue;
                    }
                    elementNumber = elementOrder;
                    elementOrder = String.valueOf(Integer.valueOf(elementOrder) + tokenCount);
                }
                if (sentenceTokenArray.size() > 1) {
                    elementToken = sentenceTokenArray.get(1).trim();
                    elementText = elementToken;
                    if (sl>0) {
                        elementText = " " + elementText;
                    }
                }
                if (sentenceTokenArray.size() > 2) {
                    elementLemma = sentenceTokenArray.get(2).trim();
                }
                if (sentenceTokenArray.size() > 3) {
                    elementPos1 = sentenceTokenArray.get(3).trim();
                }
                if (sentenceTokenArray.size() > 4) {
                    elementPos2 = sentenceTokenArray.get(4).trim();
                }
                if (sentenceTokenArray.size() > 5) {
                    elementCategories = sentenceTokenArray.get(5).trim();
                }
                if (sentenceTokenArray.size() > 6) {
                    elementDepTarget = sentenceTokenArray.get(6).trim();
                }
                if (sentenceTokenArray.size() > 7) {
                    elementEdge = sentenceTokenArray.get(7).trim();
                }
                if (sentenceTokenArray.size() > 8) {
                    elementDeps = sentenceTokenArray.get(8).trim();
                }
                if (sentenceTokenArray.size() > 9) {
                    elementSpaceAfter = sentenceTokenArray.get(9).trim();
                }

                Element tokenElement = docOutput.createElement("token");

                tokenElement.setAttribute("order", elementOrder);
                tokenElement.setAttribute("text", elementText);
                tokenElement.setAttribute("token", elementToken);
                //System.out.println(elementToken);
                tokenElement.setAttribute("lemma", elementLemma);
                tokenElement.setAttribute("pos", elementPos1);
                tokenElement.setAttribute("xpos", elementPos2);

                // Splitting categories
                //String[] categoriesList = elementCategories.split("\\|");
                List<String> categoriesList = fastSplit(elementCategories, "|");

                //String[] categoriesList = elementCategories.split("\\|");

                //for (int cl = 0; cl < categoriesList.length; cl++) {
                for (int cl = 0; cl < categoriesList.size(); cl++) {
                    String curCategory = categoriesList.get(cl);
                    //System.out.println(curCategory);

                    int borderPos = curCategory.indexOf("=");
                    if (borderPos>-1) {



                        List<String> singleCategory = new ArrayList<>();
                        singleCategory.add(curCategory.substring(0,borderPos));
                        singleCategory.add(curCategory.substring(borderPos+1,curCategory.length()));

                        if (singleCategory.size() == 2) {
                            int frontPos = singleCategory.get(0).indexOf("[");
                            int backPos = singleCategory.get(0).indexOf("]");
                            if (frontPos>-1) {
                                String tmpItem = singleCategory.get(0);
                                tmpItem = tmpItem.replace("[","_");
                                singleCategory.set(0,tmpItem);
                            }
                            if (backPos>-1) {
                                String tmpItem = singleCategory.get(0);
                                tmpItem = tmpItem.replace("]","");
                                singleCategory.set(0,tmpItem);
                            }


                            //tokenElement.setAttribute(singleCategory[0].toLowerCase(), singleCategory[1]);
                            tokenElement.setAttribute(singleCategory.get(0), singleCategory.get(1));
                            //System.out.println(singleCategory.get(0).toLowerCase() + " --- " + singleCategory.get(1));
                            //tokenElement.setAttribute(singleCategory[0], singleCategory[1]);

                        }




                    }
                }
                //tokenElement.setAttribute("categories", elementCategories);


                tokenElement.setAttribute("head", elementDepTarget);
                tokenElement.setAttribute("edge", elementEdge);
                tokenElement.setAttribute("deps", elementDeps);
                tokenElement.setAttribute("spaceafter", elementSpaceAfter);
                tokenElement.setAttribute("number", elementNumber);

                if (elementPos1.toLowerCase().equals("punct")) {
                    tokenElement.setAttribute("_punct", "true");
                }

                elList.add(tokenElement);
            }
        }
        return elList;
    }

    /**
     * Finds children based on the head attribute value
     * @param tokenList all tokens
     * @param headValue head attribute value of a certain node
     * @return a list of token indices
     */
    public static List<Integer> getChildIndex(List<Element> tokenList, String headValue) {
        List<Integer> indexList = new ArrayList<Integer>();
        for (int tl = 0; tl < tokenList.size(); tl++) {
            Element curElement = tokenList.get(tl);
            if (curElement.getAttribute("head").equals(headValue)) {
                if (tokenHasChildren(tokenList, curElement.getAttribute("number")) == false) {
                    indexList.add(tl);
                }
            }
        }
        return indexList;
    }

    /**
     * Checks if a node has children or not
     * @param tokenList all tokens
     * @param headValue head attribute value of a certain node
     * @return true of false
     */
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

    /**
     * Checks if a list contains only the tokens attached to the root node directly
     * @param tokenList all tokens
     * @return true or false
     */
    public static boolean hasOnlyRootTokens(List<Element> tokenList) {
        boolean onlyRootTokens = false;
        int rootNodeCount = 0;
        for (int tl = 0; tl < tokenList.size(); tl++) {
            Element curElement = tokenList.get(tl);
            if (curElement.getAttribute("head").equals("0")) {
                //onlyRootTokens = true;
                rootNodeCount += 1;
            }
            /*else {
                onlyRootTokens = false;
            }*/
        }
        if (tokenList.size() == rootNodeCount) {
            onlyRootTokens = true;
        }
        return onlyRootTokens;
    }


    /**
     * Creates a hierarchical structure of tokens based on the flat one
     * @param allTokens all tokens
     * @return a nested list of tokens
     */
    public static List<Element> buildDepTree(List<Element> allTokens) throws ParserConfigurationException {

        while (hasOnlyRootTokens(allTokens) == false) {
            for (int at = 0; at < allTokens.size(); at++) {
                Element curToken = allTokens.get(at);

                List<Integer> childIndex = getChildIndex(allTokens, curToken.getAttribute("number"));
                //System.out.println(childIndex);
                if (childIndex.size() > 0) {
                    Element updatedToken = allTokens.get(at);
                    for (int ci = 0; ci < childIndex.size(); ci++) {
                        Element childToken = allTokens.get(childIndex.get(ci));
                        updatedToken.appendChild(childToken);
                    }
                    allTokens.set(at, updatedToken);

                    List<Element> resultList = new ArrayList<Element>();
                    for (int rl = 0; rl < allTokens.size(); rl++) {
                        if (childIndex.contains(rl) == false) {
                            resultList.add(allTokens.get(rl));
                        }
                    }
                    allTokens.clear();
                    for (int rl = 0; rl < resultList.size(); rl++) {
                        allTokens.add(resultList.get(rl));
                    }
                    //allTokens = resultList;
                    resultList.clear();
                    break;
                }
            }
        }
        return allTokens;
    }

    /**
     * Adds "num", "start", and "finish" attributes needed for TüNDRA. It also removes unnecessary ones
     * @param inputNode a node
     */
    public static void addTundraSpecificAttributes(Element inputNode) {
        //inputNode.removeAttribute("head");
        inputNode.removeAttribute("number");

        startNum++;
        inputNode.setAttribute("num", String.valueOf(startNum));
        minOrder = 0;
        maxOrder = 0;
        getStartFinishAttributes(inputNode);
        inputNode.setAttribute("start", String.valueOf(minOrder));
        inputNode.setAttribute("finish", String.valueOf(maxOrder));

        if (inputNode.hasChildNodes()) {
            NodeList treeElList = inputNode.getChildNodes();
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    addTundraSpecificAttributes(eElement);
                }
            }
        }
    }

    /**
     * Finds the values of "start" and "finish" attributes needed for TüNDRA
     * @param inputNode a node
     */
    public static void getStartFinishAttributes(Element inputNode) {
        String orderString = inputNode.getAttribute("order");
        if (orderString.length() > 0) {
            Integer curOrder = Integer.valueOf(orderString);
            if (minOrder == 0) {
                minOrder = curOrder;
            }
            if (curOrder < minOrder) {
                minOrder = curOrder;
            }

            if (maxOrder == 0) {
                maxOrder = curOrder;
            }
            if (curOrder > maxOrder) {
                maxOrder = curOrder;
            }
        }

        if (inputNode.hasChildNodes()) {
            NodeList treeElList = inputNode.getChildNodes();
            for (int te = 0; te < treeElList.getLength(); te++) {
                Node treeNode = treeElList.item(te);
                if (treeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) treeNode;
                    getStartFinishAttributes(eElement);
                }
            }
        }
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

        if ((args.length == 1) || (args.length == 2)) {
            String inputFolder = args[0];
            String outputFile = "";

            List<String> treebankFolders = new ArrayList<>();

            if (args.length == 1) {
                treebankFolders = getUDFolderList(inputFolder);
            }
            else {
                treebankFolders.add(inputFolder);
                outputFile = args[1];
            }

            for (int tf = 0; tf < treebankFolders.size(); tf++) {
                String folderName = treebankFolders.get(tf);
                if (args.length == 1) {
                    outputFile = inputFolder + folderName + ".xml";
                }

                System.out.println("");
                System.out.println((tf+1) + " out of " + treebankFolders.size());
                System.out.println("Opening the folder containing CONLLU files: " + folderName);
                System.out.println("**********************************************************");

                // First we need to create a new XML file for the output
                DocumentBuilderFactory dbOutputFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dOutputBuilder = dbOutputFactory.newDocumentBuilder();

                // root elements
                Document docOutput = dOutputBuilder.newDocument();
                Element rootElement = docOutput.createElement("treebank");
                docOutput.appendChild(rootElement);

                Integer sentenceCounter = 0;
                Integer tokenTotal = 0;

                // Reading files from a given folder (we are going to merge them together in one treebank file)
                List<String> conlluFiles = new ArrayList<>();
                if (args.length == 1) {
                    conlluFiles = getUDFileList(inputFolder + folderName);
                }
                else {
                    conlluFiles = getUDFileList(folderName);
                }
                for (int ci = 0; ci < conlluFiles.size(); ci++) {
                    System.out.println(tokenTotal);
                    System.out.print("Reading the source file: " + conlluFiles.get(ci));

                    String filePath = "";
                    if (args.length == 1) {
                        filePath = inputFolder + folderName;
                    }
                    else {
                        filePath = inputFolder;
                    }

                    //List<List<String[]>> sentenceList = new ArrayList<List<String[]>>();
                    List<List<List<String>>> sentenceList = new ArrayList<List<List<String>>>();
                    try {
                        /*FileInputStream fs = new FileInputStream(filePath + "/" + conlluFiles.get(ci));
                        InputStreamReader ir = new InputStreamReader(fs, "UTF-8");
                        BufferedReader br = new BufferedReader(ir);*/
                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath + "/" + conlluFiles.get(ci)), "UTF-8"), 8192);
                        String line;
                        while ((line = br.readLine()) != null) {
                            // processing each line
                            line = line.trim();
                            //List<String[]> lineList = new ArrayList<String[]>();
                            List<List<String>> lineList = new ArrayList<List<String>>();
                            if ((line.length() > 0) && (line.startsWith("#") == false)) { // not a comment or empty line
                                //String[] lineArray = line.split("\t"); // split by TAB characters
                                List<String> lineArray = fastSplit(line, "\t"); // split by TAB characters
                                //lineArray[0] = lineArray[0].trim();
                                lineArray.set(0,lineArray.get(0).trim());
                                //if (lineArray[0].equals("1")) { // first token of a sentence
                                if (lineArray.get(0).equals("1")) { // first token of a sentence
                                    sentenceCounter++;
                                }
                                lineList.add(lineArray);
                                sentenceList.add(lineList);

                            }

                            if ((line.length() == 0)) { // Empty line or end of file means a sentence boundary
                                Element sentElement = docOutput.createElement("sent"); // Adding a sentence node
                                sentElement.setAttribute("id", "st" + sentenceCounter);
                                Element consElement = docOutput.createElement("cons"); // Adding the main constituent node
                                // Setting attributes for the main constituent
                                //consElement.setAttribute("start", "1");
                                consElement.setAttribute("_root", "true");
                                //consElement.setAttribute("num", "1");
                                consElement.setAttribute("cat", "ROOT");

                                List<Element> allTokenList = getTokenList(sentenceList, docOutput, tokenTotal);
                                //tokenCounter += allTokenList.size();
                                Integer tokensFound = allTokenList.size();
                                //System.out.println(tokensFound);
                                //tokenTotal = tokenTotal + tokensFound;
                                tokenTotal = tokenTotal + tokensFound;
                                List<Element> depTreeList = buildDepTree(allTokenList);

                                for (int dt = 0; dt < depTreeList.size(); dt++) {
                                    consElement.appendChild(depTreeList.get(dt));
                                }
                                //NodeList schildern = rootElement.getChildNodes();
                                //System.out.println(schildern.getLength());
                                addTundraSpecificAttributes(consElement);
                                sentElement.appendChild(consElement);
                                rootElement.appendChild(sentElement);
                                sentenceList.clear();

                            }

                        /*if (sentenceCounter>2) {
                            break;
                        }*/
                        }
                        //fs.close();
                        //ir.close();
                        br.close();
                    }
                    catch(Exception e){
                        System.err.println("Error: Target File Cannot Be Read");
                    }

                /*if (sentenceCounter>2) {
                    break;
                }*/
                    System.out.println(" - done");

                }

                // saving our results into XML
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // setting line breaks
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(docOutput);
                StreamResult result = new StreamResult(new File(outputFile));
                transformer.transform(source, result);
                System.out.println("Writing data into the file: " + outputFile);
                tokenTotal = 0;
            }
        }
    }
}
