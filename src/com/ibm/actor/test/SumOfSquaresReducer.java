package com.ibm.actor.test;

import java.math.BigInteger;

/**
 * A MapReducer that calculates the sum of squares of a list of integers.
 * 
 * @author BFEIGENB
 *
 */
public class SumOfSquaresReducer implements MapReduceer {
	@Override
	public void map(Object[] values, int start, int end) {
		for (int i = start; i <= end; i++) {
			values[i] = ((BigInteger) values[i]).multiply((BigInteger) values[i]);
			sleep(200); // fake taking time
		}
	}

	@Override
	public void reduce(Object[] values, int start, int end, Object[] target, int posn) {
		BigInteger res = new BigInteger("0");
		for (int i = start; i <= end; i++) {
			res = res.add((BigInteger) values[i]);
			sleep(100); // fake taking time
		}
		target[posn] = res;
	}

	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}