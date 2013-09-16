package de.tuebingen.uni.sfs.clarin.tundra.tcf;

import java.util.ArrayList;

import eu.clarin.weblicht.wlfxb.tc.api.*;


public class DepNode {
	private Token data;
	private String function;
	private String id;
	private ArrayList<DepNode> children;
	private int order;
	private int start;
	private int finish;
	
	public DepNode() {
		data = null;
		function = null;
		children = new ArrayList<DepNode>();
		order = -1;
		start = -1;
		finish = -1;
	}
	
	public DepNode(Token d) {
		data = d;
		function = null;
		children = new ArrayList<DepNode>();
		order = -1;
		start = -1;
		finish = -1;
	}
	
	public DepNode(Token d, String function) {
		this.data = d;
		this.function = function;
		this.children = new ArrayList<DepNode>();
		this.order = -1;
		this.start = -1;
		this.finish = -1;
	}
	
	public void addChild(DepNode dc) {
		this.children.add(dc);
	}

	/**
	 * @return the data
	 */
	public Token getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Token data) {
		this.data = data;
	}

	/**
	 * @return the function
	 */
	public String getFunction() {
		return function;
	}

	/**
	 * @param function the function to set
	 */
	public void setFunction(String function) {
		this.function = function;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the children
	 */
	public ArrayList<DepNode> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(ArrayList<DepNode> children) {
		this.children = children;
	}

	/**
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * @return the finish
	 */
	public int getFinish() {
		return finish;
	}

	/**
	 * @param finish the finish to set
	 */
	public void setFinish(int finish) {
		this.finish = finish;
	}
	
	

}
