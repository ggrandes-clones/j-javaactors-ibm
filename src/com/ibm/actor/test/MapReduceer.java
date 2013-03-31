package com.ibm.actor.test;

public interface MapReduceer {
	/**
	 * Map (in place) the elements of an array.
	 * 
	 * @param values elements to map
	 * @param start start position in values
	 * @param end end position in values
	 */
	void map(Object[] values, int start, int end);

	/**
	 * Reduce the elements of an array.
	 * 
	 * @param values elements to reduce
	 * @param start start position in values
	 * @param end end position in values
	 * @param target place to set reduced value
	 * @param posn position in target to place the value
	 */
	void reduce(Object[] values, int start, int end, Object[] target, int posn);
}