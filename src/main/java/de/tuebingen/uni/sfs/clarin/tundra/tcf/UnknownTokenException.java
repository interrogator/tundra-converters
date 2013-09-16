package de.tuebingen.uni.sfs.clarin.tundra.tcf;

public class UnknownTokenException extends Exception {
	/*
	 * Unknown token exception default constructor.
	 */
	public UnknownTokenException() {
		super("Token wasn't found in the text");
	}

	/**
	 * Unknown token exception with string argument.
	 * @param msg a message
	 */
	public UnknownTokenException(String msg) {
		super(msg);
	}
}
