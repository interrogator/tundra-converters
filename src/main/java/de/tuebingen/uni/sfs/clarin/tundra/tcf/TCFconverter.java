package de.tuebingen.uni.sfs.clarin.tundra.tcf;

/**
 * Course:      Text Technology
 * Assignment:  Semester Project
 * Author:      Tobias Kolditz
 * Description: TCFconverter
 *
 * Honor Code:  I pledge that this program represents my own work.
 *              I received help from:
 *              
 *          	Chris Culy
 *              
 *              in designing and debugging my program.
 */

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import eu.clarin.weblicht.wlfxb.io.*;
import eu.clarin.weblicht.wlfxb.tc.api.*;
import eu.clarin.weblicht.wlfxb.tc.xb.*;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import eu.clarin.weblicht.wlfxb.tc.xb.LemmasLayerStored;
import java.io.FileOutputStream;

public class TCFconverter {
	private static final String HELP = 
			"The TCFconverter converts TCF to Scott Martens' format.\n" +
					"Invocation: java -jar TCFconverter [OPTION] INPUT_FILE " +
					"[OUTPUT_FILE]\nAvailable options:\n" +
					"\t-c\tcreate a constituency tree, OR\n" + 
					"\t-d\tcreate a dependency tree\n" + 
					"\n" + 
					"If no option is specified, " +
					"a constituency tree is created by default.\n" +
					"If no output file is specified, the output is written to STDOUT.";

	private ConstituentParsingLayerStored cpl;
	private DependencyParsingLayerStored dpl;
	private LemmasLayerStored ll;
	private MorphologyLayerStored ml;
	private PosTagsLayerStored ptl;
	private TokensLayerStored tl;

	private StringBuilder curSent;
	private String text;
	private int oldInd;
	private int curInd;
	private int sentenceID;
	private int num;
	private BufferedWriter out;
	private boolean checkVar;
	private String lastTextValue;
	private HashMap<String, ArrayList<DepNode>> dependencyHashMap;
	private HashMap<Integer, String> textValues;

	public String warnings;

	/**
	 * Constructor for a given input file name (output to STDOUT). If the
	 * second argument is <i>true</i>, a constituency tree is created, 
	 * if it is <i>false</i>, a dependency tree is created instead.
	 * @param fileNameIn name of the input file
	 * @param constituencyTree <i>true</i> if a constituency tree shall be 
	 * created, <i>false</i> if a dependency tree shall be created
	 * @throws WLFormatException
	 * @throws IOException
	 */
	public TCFconverter(String fileNameIn,  
			boolean constituencyTree) throws WLFormatException,
			IOException, UnknownTokenException, MissingLayerException {

		curSent = new StringBuilder(); //output for sentence currently processed
		sentenceID = 1; // attribute for sentences
		num = 0; // attribute for cons and token elements
		//index of the first character of the last word found in 
		//getTextValue() 
		oldInd = 0;
		curInd = 0; // start index for indexOf in getTextValue()
		out = new BufferedWriter(new OutputStreamWriter(System.out));
		// hash map with ids of governing tokens (keys) and 
		// DepNodes of governed tokens (values)
		dependencyHashMap = null; 
		// hash map with order values of tokens (keys) and
		// their text values (values)
		textValues = null; 
		checkVar = true; //false iff something goes wrong with the text value
		lastTextValue = ""; //last text value found
		warnings = "";

		//read corpus
		FileInputStream fis = new FileInputStream(fileNameIn);
		WLData wld = WLDObjector.read(fis);
		TextCorpusStored tc = wld.getTextCorpus(); 

		// get necessary annotation layers
		text = tc.getTextLayer().getText();
		ll = tc.getLemmasLayer();
		ml = null; //tc.getMorphologyLayer(); skipping morph until I can debug
		ptl = tc.getPosTagsLayer();
		if (constituencyTree) {
			cpl = tc.getConstituentParsingLayer();
			if (cpl == null) {
				throw new MissingLayerException(
						"The constituent parsing layer is missing!");
			}
			tl = null;
			dpl = null;
			createConstituencyTree();
		} else {
			cpl = null;
			tl = tc.getTokensLayer();
			if (tl == null) {
				throw new MissingLayerException("The token layer is missing!");
			}
			dpl = tc.getDependencyParsingLayer();
			if (dpl == null) {
				throw new MissingLayerException(
						"The dependency parsing layer is missing!");
			}
			createDependencyTree();
		}
	}


	/**
	 * Constructor for given input and output file names. If the third 
	 * argument is <i>true</i>, a constituency tree is created, 
	 * if it is <i>false</i>, a dependency tree is created instead.
	 * @param fileNameIn name of the input file
	 * @param fileNameOut name for the output file
	 * @param constituencyTree <i>true</i> if a constituency tree shall be 
	 * created, <i>false</i> if a dependency tree shall be created
	 * @throws WLFormatException
	 * @throws IOException
	 */
	public TCFconverter(String fileNameIn, String fileNameOut, 
			boolean constituencyTree) throws WLFormatException,
			IOException, UnknownTokenException, MissingLayerException {

		curSent = new StringBuilder(); //output for sentence currently processed
		sentenceID = 1; // attribute for sentences
		num = 0; // attribute for cons and token elements
		//index of the first character of the last word found in 
		//getTextValue() 
		oldInd = 0;
		curInd = 0; // start index for indexOf in getTextValue()
		out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileNameOut), "UTF-8"));
		// hash map with ids of governing tokens (keys) and 
		// DepNodes of governed tokens (values)
		dependencyHashMap = null; 
		// hash map with order values of tokens (keys) and
		// their text values (values)
		textValues = null; 
		checkVar = true; //false iff something goes with getting the textValue
		lastTextValue = ""; //last text value found
		warnings = "";

		//read corpus
		FileInputStream fis = new FileInputStream(fileNameIn);
		WLData wld = WLDObjector.read(fis);
		TextCorpusStored tc = wld.getTextCorpus(); 

		// get necessary annotation layers
		text = tc.getTextLayer().getText();
		ll = tc.getLemmasLayer();
		ml = null; //tc.getMorphologyLayer(); skipping morph until I can debug
		ptl = tc.getPosTagsLayer();
		if (constituencyTree) {
			cpl = tc.getConstituentParsingLayer();
			if (cpl == null) {
				throw new MissingLayerException(
						"The constituent parsing layer is missing!");
			}
			tl = null;
			dpl = null;
			createConstituencyTree();
		} else {
			cpl = null;
			tl = tc.getTokensLayer();
			if (tl == null) {
				throw new MissingLayerException("The token layer is missing!");
			}
			dpl = tc.getDependencyParsingLayer();
			if (dpl == null) {
				throw new MissingLayerException(
						"The dependency parsing layer is missing!");
			}
			createDependencyTree();
		}
	}

	/**
	 * Create dependency tree output.
	 * @throws IOException
	 */
	private void createDependencyTree() 
			throws IOException, UnknownTokenException {
		out.write("<?xml version=\"1.0\"?>\n");
		out.write("<corpus>\n");
		for (int i = 0; i < dpl.size(); i++) {
			createDependencyHashMap(i);
			DepNode root = new DepNode();
			root.setId("ROOT");		
			buildTree(root);
			setStartFinishValues(root);
			appendDependencySent(root, 0);
			out.write(curSent.toString());
			sentenceID += 1;
			curSent = new StringBuilder();
		}
		out.write("</corpus>");
		out.close();
	}

	/**
	 * Append a dependency sent element to curSent.
	 * @param root the root of the DepNode tree for this sentence
	 * @param level the depth in the tree (used for correct indentation)
	 */
	private void appendDependencySent(DepNode root, int level)
			throws UnknownTokenException {
		textValues = new HashMap<Integer, String>();
		for (int i = root.getStart(); i <= root.getFinish(); i++) {
			textValues.put(i, getTextValue(tl.getToken(i).getString()));
		}
		String indentSent = indent(level);
		curSent.append(indentSent + "<sent");
		curSent.append(formatAttr("id", "st" + sentenceID) + ">\n");
		String indentCons = indent(level + 1);
		curSent.append(indentCons + "<cons");
		curSent.append(formatAttr("num", Integer.toString(num)));
		curSent.append(formatAttr("cat", "START"));
		num += 1;
		curSent.append(formatAttr("start", Integer.toString(root.getStart())));
		curSent.append(formatAttr("finish", Integer.toString(root.getFinish())));
                //added to deal with root bug in 7.2 
                curSent.append(formatAttr("_root", "true"));
		curSent.append(">\n");

		for (DepNode dp: root.getChildren()) {
			appendDependencyToken(dp, level + 2);
		}

		curSent.append(indentCons + "</cons>\n");
		curSent.append(indentSent + "</sent>\n");
	}

	/**
	 * Append a dependency token element to <i>curSent</i>.
	 * @param node a DepNode
	 * @param level current depth in the DepNode tree
	 */
	private void appendDependencyToken(DepNode node, int level) {
		String indentation = indent(level);
		curSent.append(indentation + "<token");
		curSent.append(formatAttr("num", Integer.toString(num)));
		num += 1;
		Token t = node.getData();
		if (t != null) {
			if (t.getString() != null) {
				curSent.append(formatAttr("token", t.getString()));
			}
			if (ll != null && ll.getLemma(t) != null) {
				curSent.append(formatAttr("lemma", ll.getLemma(t).getString()));
			}
			if (ptl != null && ptl.getTag(t) != null) {
				curSent.append(formatAttr("pos", ptl.getTag(t).getString()));
			}
			if (ml != null && ml.getAnalysis(t) != null) {
				Feature[] fs = ml.getAnalysis(t).getFeatures();
				for (int j = 0; j < fs.length; j++) {
					String name = "morph" + fs[j].getName();
					name = name.replaceAll("[\\s<>\"'&]+", "");
					curSent.append(formatAttr(name, 
							fs[j].getValue()));
				}
			}
		}
		if (node.getFunction() != null) {
			curSent.append(formatAttr("edge", node.getFunction()));
		}
		curSent.append(formatAttr("start", Integer.toString(node.getStart())));
		curSent.append(formatAttr("finish", Integer.toString(node.getFinish())));
		curSent.append(formatAttr("order", Integer.toString(node.getOrder())));
		if (node.getOrder() != -1) {
			curSent.append(formatAttr("text", textValues.get(node.getOrder())));
		}
		curSent.append(">\n");
		for (DepNode dn: node.getChildren()) {
			appendDependencyToken(dn, level + 1);
		}
		curSent.append(indentation + "</token>\n");
	}

	/**
	 * Build a DepNode tree from the current <i>dependencyHashMap</i>.
	 * @param node start node
	 */
	private void buildTree(DepNode node) {
		Token data = node.getData();
		if (data != null) {
			node.setId(data.getID());
			node.setOrder(data.getOrder());
		}
		if (dependencyHashMap.containsKey(node.getId())) {
			for (DepNode dp: dependencyHashMap.get(node.getId())) {
				node.addChild(dp);
				buildTree(dp);
			}
		}
	}

	/**
	 * Traverse an existing DepNode tree and set all <i>start</i> and 
	 * <i>finish</i> values.
	 * @param node a start node
	 * @return {start, finish}
	 */
	private int[] setStartFinishValues(DepNode node) {
		int order = node.getOrder();
		if (node.getChildren().size() == 0) {
			node.setStart(order);
			node.setFinish(order);
			int[] rvalLeaf = {order, order};
			return rvalLeaf;
		} else {
			int[] rvalNonLeaf = {Integer.MAX_VALUE, Integer.MIN_VALUE};
			if (order != -1) {
				if (order < rvalNonLeaf[0]) {
					rvalNonLeaf[0] = order;
				}
				if (order > rvalNonLeaf[1]) {
					rvalNonLeaf[1] = order;
				}
			}
			for (DepNode dn: node.getChildren()) {
				int[] curValue = setStartFinishValues(dn);
				if (curValue[0] < rvalNonLeaf[0]) {
					rvalNonLeaf[0] = curValue[0];
				}
				if (curValue[1] > rvalNonLeaf[1]) {
					rvalNonLeaf[1] = curValue[1];
				}
			}
			node.setStart(rvalNonLeaf[0]);
			node.setFinish(rvalNonLeaf[1]);
			return rvalNonLeaf;
		}
	}

	/**
	 * Create the dependency hash map for the dependency parse at 
	 * <i>parseIndex</i>.
	 * @param parseIndex index of the parse
	 */
	private void createDependencyHashMap(int parseIndex) {
		dependencyHashMap = new HashMap<String, ArrayList<DepNode>>();
		DependencyParse s1 = dpl.getParse(parseIndex);
		Dependency[] dep = s1.getDependencies();
		// fix for stanford parser
		Set<Token> keySet = new HashSet<Token>();
		Set<Token> valueSet = new HashSet<Token>();
		boolean hasRoot = false;
		for (int i = 0; i < dep.length; i++) {
			String key;
			//if dependency relation has a governing token, use its ID as key
			if (dpl.getGovernorTokens(dep[i]) != null) {
				Token[] gt = dpl.getGovernorTokens(dep[i]);
				key = gt[0].getID();
				if (!hasRoot && !keySet.contains(gt[0])) {
					keySet.add(gt[0]);
				}
			} else { // else: it's the root dependency
				hasRoot = true;
				key = "ROOT";
			}
			if (dpl.getDependentTokens(dep[i]) != null) {
				Token[] dt = dpl.getDependentTokens(dep[i]);
				for (int j = 0; j < dt.length; j++) {
					if (dt[j] != null) {
						if (!hasRoot && !valueSet.contains(dt[j])) {
							valueSet.add(dt[j]);
						}
						if (!dependencyHashMap.containsKey(key)) {
							ArrayList<DepNode> value = new ArrayList<DepNode>();
							value.add(new DepNode(dt[j], dep[i].getFunction()));
							dependencyHashMap.put(key, value);
						} else {
							dependencyHashMap.get(key).add(new DepNode(dt[j], 
									dep[i].getFunction()));
						}
					}
				}
			}
		}
		// if root couldn't be identified (stanford parser)
		if (!hasRoot) {
			keySet.removeAll(valueSet);
			Token root = keySet.iterator().next();
			ArrayList<DepNode> rootValue = new ArrayList<DepNode>();
			rootValue.add(new DepNode(root, "ROOT"));
			dependencyHashMap.put("ROOT", rootValue);
		}
	}

	/**
	 * Create constituency tree output.
	 * @throws IOException
	 */
	private void createConstituencyTree() 
			throws IOException, UnknownTokenException {
		out.write("<?xml version=\"1.0\"?>\n");
		out.write("<corpus>\n");
		for (int i=0; i < cpl.size(); i++) {
			Constituent root = cpl.getParseRoot(i);
			appendConstituencyElement(root, 0);
			out.write(curSent.toString());
			curSent = new StringBuilder();
			sentenceID += 1;
		}
		out.write("</corpus>");
		out.close();
	}

	/**
	 * Traverse the constituency parse and append the encountered 
	 * constituents to <i>curSent</i>.
	 * @param c a Constituent from the tcf constituency parse
	 * @param level the current depth in the tree
	 */
	private void appendConstituencyElement(Constituent c, int level) 
			throws UnknownTokenException{
		if (c == null) {
			return;
		}
		String indentation = indent(level);
		Constituent[] children = c.getChildren();
		if (children == null) {
			appendConstituencyTerm(c, indentation);
		} else {
			if (level == 0) {
				appendConstituencySent(c, indentation, children, level);
			} else {
				appendConstituencyCons(c, indentation, children, level);
			}
		}
	}

	/**
	 * Append a <i>cons</i> element to <i>curSent</i>.
	 * @param c a Constituent from the tcf constituency parse
	 * @param indentation the correct indentation for the current depth
	 * @param children the children of <i>c</i>
	 * @param level the current depth in the tree 
	 */
	private void appendConstituencyCons(Constituent c, 
			String indentation, Constituent[] children, int level) 
					throws UnknownTokenException {
		curSent.append(indentation + "<cons");
		curSent.append(formatAttr("num", Integer.toString(num)));
		if (c.getCategory() != null)
			curSent.append(String.format(" cat=\"%s\"", c.getCategory()));
		if (c.getEdge() != null)
			curSent.append(String.format(" edge=\"%s\"", c.getEdge()));
		if (cpl.getTokens(c) != null) {
			//order value of the first token in the constituent
			curSent.append(formatAttr("start", Integer.toString(
					cpl.getTokens(c)[0].getOrder())));
			//order value of the last token in the constituent
			curSent.append(formatAttr("finish", Integer.toString(
					cpl.getTokens(c)[cpl.getTokens(c).length - 1].getOrder())));
		}
		curSent.append(">\n");
		num += 1;
		for (int i = 0; i < children.length; i++) {
			appendConstituencyElement(children[i], level+1);
		}
		curSent.append(indentation + "</cons>\n");
	}

	/**
	 * Append a <i>sent</i> element to <i>curSent</i>.
	 * @param c a Constituent from the tcf constituency parse
	 * @param indentation the correct indentation for the current depth
	 * @param children the children of <i>c</i>
	 * @param level the current depth in the tree 
	 */
	private void appendConstituencySent(Constituent c, String indentation, 
			Constituent[] children, int level) 
					throws UnknownTokenException {
		curSent.append(indentation + "<sent");
		curSent.append(formatAttr("id", "st" + sentenceID));
		curSent.append(">\n");

		curSent.append(indent(level+1) + "<cons");
		curSent.append(formatAttr("num", Integer.toString(num)));
		num += 1;
		if (c.getCategory() != null)
			curSent.append(String.format(" cat=\"%s\"", c.getCategory()));
		if (c.getEdge() != null)
			curSent.append(String.format(" edge=\"%s\"", c.getEdge()));
		if (cpl.getTokens(c) != null) {
			//order value of the first token in the constituent
			curSent.append(formatAttr("start", Integer.toString(
					cpl.getTokens(c)[0].getOrder())));
			//order value of the last token in the constituent
			curSent.append(formatAttr("finish", Integer.toString(
					cpl.getTokens(c)[cpl.getTokens(c).length - 1].getOrder())));
		}

		curSent.append(">\n");

		for (int i = 0; i < children.length; i++) {
			appendConstituencyElement(children[i], level+2);
		}
		curSent.append(indent(level+1) + "</cons>\n");
		curSent.append(indentation + "</sent>\n");
	}

	/**
	 * Append a <i>token</i> element to <i>curSent</i>.
	 * @param c a Constituent from the tcf constituency parse
	 * @param indentation the correct indentation for the current depth
	 */
	private void appendConstituencyTerm(Constituent c, String indentation) 
			throws UnknownTokenException{
		Token[] t = cpl.getTokens(c);
		for (int i = 0; i < t.length; i++) {
			curSent.append(indentation + "<term");
			curSent.append(formatAttr("num", Integer.toString(num)));
			if (t[i] != null) {
				curSent.append(formatAttr("token", t[i].getString()));
				if (ll != null && ll.getLemma(t[i]) != null) {
					curSent.append(formatAttr(
							"lemma", ll.getLemma(t[i]).getString()));
				}
				if (ptl != null && ptl.getTag(t[i]) != null) {
					curSent.append(formatAttr(
							"pos", ptl.getTag(t[i]).getString()));
				}
				t[i].getID();
			}
			// append morph attributes
			if (ml != null && ml.getAnalysis(t[i]) != null) {
				Feature[] fs = ml.getAnalysis(t[i]).getFeatures();
				for (int j = 0; j < fs.length; j++) {
					String name = "morph" + fs[j].getName();
					name = name.replaceAll("[\\s<>\"'&]+", "");
					curSent.append(formatAttr(name, 
							fs[j].getValue()));
				}
			}
			if (c.getEdge() != null)
				curSent.append(String.format(" edge=\"%s\"", c.getEdge()));
			String order = Integer.toString(t[i].getOrder());
			curSent.append(formatAttr("order", order));
			curSent.append(formatAttr("start", order));
			curSent.append(formatAttr("finish", order));
			// find whitespace in the text
			String textValue = getTextValue(t[i].getString());
			curSent.append(formatAttr("text", textValue));
			curSent.append("/>\n");
			num += 1;
		}
	}

	/**
	 * Return the text value for <i>token</i>. (Works only if invoked 
	 * with tokens in the order of their appearance in the text!)
	 * @param token the next token after <i>curInd</i>
	 * @return the value of the text attribute in a token element
	 */
	private String getTextValue(String token) throws UnknownTokenException {
		String rval = " " + token;
		curInd = text.indexOf(token, oldInd);
		if (curInd == -1) {
			warnings += String.format(
					"\nWarning: The token '%s' was not found in the text layer", 
					token);
		} else if (checkVar) {
			int nrOfInterveningWords = 
					text.substring(oldInd, curInd).split("\\s+").length;
			if (nrOfInterveningWords > 3) {
				checkVar = false;
				warnings += String.format(
						"\nWarning: Probably something is wrong with the token '%s'", 
						token);
			} else {
				String whitespace = leadingWhitespace(curInd);
				boolean missingSpace = whitespace.length() == 0 &&
						lastTextValue.length() > 0 &&
						Pattern.matches("\\p{L}", lastTextValue.substring(
								lastTextValue.length()-1)) &&
								Pattern.matches("\\p{Ll}", 
										token.substring(0, 1));

				if (!missingSpace) {
					rval = whitespace + token;
					oldInd = curInd + token.length();
				}
			}
		}
		lastTextValue = rval;
		return rval;
	}

	private String leadingWhitespace(int index) {
		String rval = "";
		if (index > 0) {
			String curChar = text.substring(index-1, index);
			while (Pattern.matches("\\s", curChar)) {
				rval = curChar + rval;
				index -= 1;
				if (index > 0) {
					curChar = text.substring(index-1, index);
				}
			}
		}
		return rval;
	}

	/**
	 * Return a formatted attribute.
	 * @param name an attribute name
	 * @param value an attribute value
	 * @return a formatted attribute
	 */
	private String formatAttr(String name, String value) {
		value = value.replaceAll("<", "&lt;");
		value = value.replaceAll(">", "&gt;");
		value = value.replaceAll("\"", "&quot;");
		//value = value.replaceAll("&", "&amp;");
		value = value.replaceAll("'", "&apos;");
		return String.format(" %s=\"%s\"", name, value);
	}

	/**
	 * Return the indentation for a given <i>level</i>.
	 * @param level current level
	 * @return the indentation for a given <i>level</i>
	 */
	private String indent(int level) {
		StringBuilder indentation = new StringBuilder();
		for (int i = 0; i < level; i++) {
			indentation.append("  ");
		}
		return indentation.toString();
	}

	/**
	 * Main method (demo of this program).
	 * @param args (-c: constituency tree output/ -d: dependency tree output); 
	 * input file name; (output file name)
	 */
	public static void main(String[] args) {
		if (args == null) {
			System.err.println("Missing argument(s)!\n");
			System.err.println(HELP);
			return;
		} 
		if (args.length > 3) {
			System.err.println("Too many arguments!\n");
			System.err.println(HELP);
			return;
		}
		String[] names = new String[2];
		boolean consParse = true;
		int namesIndex = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--help")) {
				System.out.println(HELP);
				return;
			}
			if (args[i].startsWith("-")) {				
				for (int j = 1; j < args[i].length(); j++) {
					char curChar = args[i].charAt(j);
					if (curChar == 'c') {
					} else if (curChar == 'd') {
						consParse = false;
					} else if (curChar == 'h') {
						System.out.print(HELP);
						return;
					} else {
						System.err.println("Unknown command line switch!\n");
						System.err.println(HELP);
						return;
					}
				}
			} else {
				if (namesIndex < 2) {
					names[namesIndex] = args[i];
					namesIndex++;
				} else {
					System.err.println("Too many file name arguments!\n");
					System.err.println(HELP);
					return;
				}
			}
		} 
		if (namesIndex == 0) {
			System.err.println("No input file specified!\n");
			System.err.println(HELP);
			return;
		}
		try {
			System.err.println("Creating XML...");
			TCFconverter c;
			if (namesIndex == 1) {
				c = new TCFconverter(names[0], consParse);
			} else {
				c = new TCFconverter(
						names[0], names[1], consParse);
			}
			System.err.println("\nDone." + c.warnings);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (WLFormatException e) {
			System.out.println(e.getMessage());
		} catch (UnknownTokenException e) {
			System.out.println(e.getMessage());
		} catch (MissingLayerException e) {
			System.out.println(e.getMessage());
		}


	}
}