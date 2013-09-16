package de.tuebingen.uni.sfs.clarin.tundra.tcf;

public class MissingLayerException extends Exception {
	/**
	 * Missing layer exception default constructor.
	 */
	public MissingLayerException() {
		super("A required annotation layer is missing");
	}

	/**
	 * Missing layer exception with string argument.
	 * @param msg a message
	 */
	public MissingLayerException(String msg) {
		super(msg);
	}
}
