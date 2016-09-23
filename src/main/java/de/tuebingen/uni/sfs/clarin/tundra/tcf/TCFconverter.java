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
 * 
 * I made some changes for morphology -- Scott
 * 
 * Changed to create a 'fake' dependency tree if dependency layer is missing 
 * Changed to include named entity tags and color highlighting from TCF in TundraXML
 * -- Valentin
 */

import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.api.*;
import eu.clarin.weblicht.wlfxb.tc.xb.*;
import eu.clarin.weblicht.wlfxb.xb.WLData;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

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
    private SentencesLayerStored sl;
    private NamedEntitiesLayerStored nel;
    private List<String> neCatColors;
    private List<String> allNECats;

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
    private ArrayList<DepNode> fakeDependencyList;
	private HashMap<Integer, String> textValues;
    private int lastOrder;

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
			boolean constituencyTree) throws IOException, UnknownTokenException {
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

		// Reading a corpus
		FileInputStream fis = new FileInputStream(fileNameIn);
		WLData wld = WLDObjector.read(fis);
		TextCorpusStored tc = wld.getTextCorpus(); 

		// get necessary annotation layers
		TextLayerStored textLayer = tc.getTextLayer();
		if (textLayer != null) {
			text = textLayer.getText();
		} else {
			text = "";
		}
		ll = tc.getLemmasLayer();
		ml = tc.getMorphologyLayer();
		ptl = tc.getPosTagsLayer();
		nel = tc.getNamedEntitiesLayer();
        getAllNECategories();

		cpl = tc.getConstituentParsingLayer();
		if (cpl == null) {
			tl = tc.getTokensLayer();
			dpl = tc.getDependencyParsingLayer();
			if (dpl == null) {
				sl = tc.getSentencesLayer();
				createFakeDependencyTree();
			} else {
				createDependencyTree();
			}
		} else {
			tl = null;
			dpl = null;
			createConstituencyTree();
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
			boolean constituencyTree) throws IOException, UnknownTokenException {

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
		TextLayerStored textLayer = tc.getTextLayer();
		if (textLayer != null) {
			text = textLayer.getText();
		} else {
			text = "";
		}
		ll = tc.getLemmasLayer();
		ml = tc.getMorphologyLayer();
		ptl = tc.getPosTagsLayer();
		nel = tc.getNamedEntitiesLayer();
        if (nel != null) {
            getAllNECategories();
        }

		cpl = tc.getConstituentParsingLayer();
		if (cpl == null) {
			tl = tc.getTokensLayer();
			dpl = tc.getDependencyParsingLayer();
			if (dpl == null) {
				sl = tc.getSentencesLayer();
				createFakeDependencyTree();
			} else {
				createDependencyTree();
			}
		} else {
			tl = null;
			dpl = null;
			createConstituencyTree();
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
                //System.err.println("Making dependency treebank...");
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
        
        private void createFakeDependencyTree()
                throws IOException, UnknownTokenException {
            out.write("<?xml version=\"1.0\"?>\n");
		out.write("<corpus>\n");
                //System.err.println("Making dependency treebank...");
		for (int i = 0; i < sl.size(); i++) {
			createFakeDependencyList(i);
			DepNode root = new DepNode();
			root.setId("ROOT");
			buildFakeTree(root);
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
			if(nel != null){
				writeNamedEntityInfo(t);
			}
			if (ml != null && ml.getAnalysis(t) != null) {
				Feature[] fs = ml.getAnalysis(t).getFeatures();
				Set<String> added = new HashSet<String>(); //to prevent morphology overloading
				for (int j = 0; j < fs.length; j++) {
					String name = "morph" + fs[j].getName();
					if (!added.contains(name)) {
						name = name.replaceAll("[\\s<>\"'&]+", "");
						curSent.append(formatAttr(name,
						fs[j].getValue()));
						added.add(name);
					}
					else {
						System.err.println("Double morphological attribute '" + name + "' on token #" + Integer.toString(num));
					}
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
	 * Build a DepNode tree from the current <i>fakeDependencyList</i>.
	 * @param node start node - the artificial root node
	 */
        private void buildFakeTree(DepNode node) {
		for(DepNode dn : fakeDependencyList){
                    node.addChild(dn);
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
	 * Create the fake dependency list for the sentence at 
	 * <i>sentIndex</i>.
	 * @param sentIndex index of the sentence
	 */
        private void createFakeDependencyList(int sentIndex){
            fakeDependencyList = new ArrayList<DepNode>();
            Token[] st = sl.getTokens(sl.getSentence(sentIndex));            
            for(Token t : st){               
                DepNode node = new DepNode(t);
                Token data = node.getData();
		if (data != null) {
			node.setId(data.getID());
			node.setOrder(data.getOrder());
		}               
                fakeDependencyList.add(node);               
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
                //System.err.println("Making constituency treebank...");
                lastOrder = 0;
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
                if (level == 0) {
                    appendConstituencySent(c, indentation, children, level);
                } else if (children == null) {
                    appendConstituencyTerm(c, indentation);
                } else {
                    appendConstituencyCons(c, indentation, children, level);
                }
                
//		if (children == null) {
//			appendConstituencyTerm(c, indentation);
//		} else {
//			if (level == 0) {
//				appendConstituencySent(c, indentation, children, level);
//			} else {
//				appendConstituencyCons(c, indentation, children, level);
//			}
//		}
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
                //added to censor traces until I can make a fix
                if ((cpl.getTokens(c) == null) || (cpl.getTokens(c).length == 0)) {
                    return;
                }
		curSent.append(indentation + "<cons");
		curSent.append(formatAttr("num", Integer.toString(num)));
		if (c.getCategory() != null)
			curSent.append(String.format(" cat=\"%s\"", c.getCategory()));
		if (c.getEdge() != null)
			curSent.append(String.format(" edge=\"%s\"", c.getEdge()));
		if (cpl.getTokens(c) != null) {
                        if (cpl.getTokens(c).length == 0) {
                            //empty constituent means trace element!!
                            //word order is 1 + last token or 0 if no last token
                            curSent.append(formatAttr("trace", "true"));
                            curSent.append(formatAttr("start", Integer.toString(lastOrder + 1)));
                            curSent.append(formatAttr("finish", Integer.toString(lastOrder + 1)));
                        } else {
                            //order value of the first token in the constituent
                            curSent.append(formatAttr("start", Integer.toString(
                                            cpl.getTokens(c)[0].getOrder())));
                            //order value of the last token in the constituent
                            curSent.append(formatAttr("finish", Integer.toString(
                                            cpl.getTokens(c)[cpl.getTokens(c).length - 1].getOrder())));
                        }
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
                //System.err.println("sentenceID=" + sentenceID);
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
		if ((cpl.getTokens(c) != null) && (cpl.getTokens(c).length > 0)) {
			//order value of the first token in the constituent
			curSent.append(formatAttr("start", Integer.toString(
					cpl.getTokens(c)[0].getOrder())));
			//order value of the last token in the constituent
			curSent.append(formatAttr("finish", Integer.toString(
					cpl.getTokens(c)[cpl.getTokens(c).length - 1].getOrder())));
		}
                curSent.append(formatAttr("_root", "true"));
		curSent.append(">\n");

                if (children != null) {
                    for (int i = 0; i < children.length; i++) {
                        appendConstituencyElement(children[i], level+2);
                    }
                }
		curSent.append(indent(level+1) + "</cons>\n");
		curSent.append(indentation + "</sent>\n");
	}

	/**
	 * Append a <i>token</i> element to <i>curSent</i>.
	 * @param c a Constituent from the tcf constituency parse
	 * @param indentation the correct indentation for the current depth
	 */
	private void appendConstituencyTerm(Constituent c, String indentation) throws UnknownTokenException {
		Token[] t = cpl.getTokens(c);
		if (t.length == 0) {
			//trace terminal element
			curSent.append(indentation + "<term");
			curSent.append(formatAttr("num", Integer.toString(num)));
			curSent.append(formatAttr("trace", "true"));
			curSent.append(formatAttr("token", c.getCategory()));
			if (c.getEdge() != null) {
				curSent.append(String.format(" edge=\"%s\"", c.getEdge()));
			}
			String order = Integer.toString(lastOrder + 1);
			curSent.append(formatAttr("order", order));
			curSent.append(formatAttr("start", order));
			curSent.append(formatAttr("finish", order));
			curSent.append(formatAttr("text", " -" + c.getCategory() + "- "));
			curSent.append("/>\n");
			num += 1;
		}
		else {
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
					if(nel != null){
						writeNamedEntityInfo(t[i]);
					}
					t[i].getID();
				}
				// append morph attributes
				if (ml != null && ml.getAnalysis(t[i]) != null) {
					Feature[] fs = ml.getAnalysis(t[i]).getFeatures();
					Set<String> added = new HashSet<String>(); //to prevent morphology overloading
					for (int j = 0; j < fs.length; j++) {
						String name = "morph" + fs[j].getName();
						if (!added.contains(name)) {
							name = name.replaceAll("[\\s<>\"'&]+", "");
							curSent.append(formatAttr(name,
							fs[j].getValue()));
							added.add(name);
						}
						else {
							System.err.println("Double morphological attribute '" + name + "' on token #" + Integer.toString(num));
						}
					}
				}
				if (c.getEdge() != null)
				curSent.append(String.format(" edge=\"%s\"", c.getEdge()));
				String order = Integer.toString(t[i].getOrder());
				lastOrder = t[i].getOrder();
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
	}


    /**
     * Generates a random color and mixes it with the provided color
     * @param constantColor color provided to be mixed with the random color
     */
    public String generateDistinctColor(Color constantColor) {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        if (constantColor != null) {
            red = (red + constantColor.getRed())/2;
            green = (green + constantColor.getGreen())/2;
            blue = (blue + constantColor.getBlue())/2;
        }

        Color color = new Color(red, green, blue); // RGB representation
        String hex = String.format("#%02x%02x%02x", red, green, blue); // HEX representation

        return hex;
    }

	private void getAllNECategories() {
		allNECats = new ArrayList<>(); // list of unique NE categories
        neCatColors = new ArrayList<>(); // list of NE category colors

        if (nel != null) {
            allNECats.addAll(nel.getFoundTypes()); // adding all available NE categories from TCF

            // Generated random distinct colors for each of the found categories
            for (int nc = 0; nc < allNECats.size(); nc++) {
                neCatColors.add(generateDistinctColor(new Color(255, 255, 255)));
            }
        }
	}
	/**
	 * Append named entity attribute and corresponding color attribute for a token
	 * ( Call only if NamedEntityLayer is present )
	 * @param t the token being written
	 */
	private void writeNamedEntityInfo(Token t){
		if(nel.getEntity(t)!=null){
            String curCat = nel.getEntity(t).getType();
		    curSent.append(formatAttr("_ne", curCat));

            Integer curCatIndex = allNECats.indexOf(curCat);
            if (curCatIndex > -1) {
                curSent.append(formatAttr("_color", neCatColors.get(curCatIndex)));
            }

            /*
            // green
			if(nel.getEntity(t).getType().equals("GPE")){
				curSent.append(formatAttr("_color", "#00ff80"));
			}
			// pink
			else if(nel.getEntity(t).getType().equals("PER")){
				curSent.append(formatAttr("_color", "#ff80ff"));
			}
			// yellow
			else if(nel.getEntity(t).getType().equals("LOC")){
				curSent.append(formatAttr("_color", "#ffff40"));
			}
			// blue
			else if(nel.getEntity(t).getType().equals("ORG")){
				curSent.append(formatAttr("_color", "#0080ff"));
			}
			// orange
			else if(nel.getEntity(t).getType().equals("OTH")){
				curSent.append(formatAttr("_color", "#ff8000"));
			}
			else{ //same color as 'other' category
				curSent.append(formatAttr("_color", "#ff8000"));
			}
			*/
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
		value = value.replaceAll("&", "&amp;");
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
						consParse = true;
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
		} catch (UnknownTokenException e) {
			System.out.println(e.getMessage());
		}/* catch (MissingLayerException e) {
			System.out.println(e.getMessage());
		}*/


	}
}
