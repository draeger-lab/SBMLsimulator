/*
 * Copyright (c) ZBiT, University of T&uuml;bingen, Germany
 * Compiler: JDK 1.6.0
 * Oct 29, 2007
 * Compiler: JDK 1.6.0
 */
package org.sbml.simulator.util;


/**
 * This class contains a multitude of well defined mathematical functions
 * like the faculty, logarithms and several trigonometric functions.
 *
 * @since 2.0
 * @version
 * @author Andreas Dr&auml;ger
 * @author Diedonne Wouamba
 * @date Oct 29, 2007
 **/
public final class Functions extends eva2.tools.math.Mathematics {

	/**
	 * There shouldn't be any instances of this class.
	 */
	private Functions() {}

  /**
   * This method computes the logarithm of a number x to a giving base b.
   *
   * @param number
   * @param base
   * @return
   */
  public static final double logarithm(double number, double base) {
    return Math.log(number) / Math.log(base);
  }


  /**
   * This method computes the factorial! function.
   *
   * @param n
   * @return
   */
  public static final double factorial(double n) {
    if ((n == 0) || (n == 1)) return 1;
    return n * factorial(n - 1);
  }

  
	/**
	 * This just computes the minimum of three integer values.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return Gives the minimum of three integers
	 */
	public static int min(int x, int y, int z) {
		if ((x < y) && (x < z))
			return x;
		if (y < z)
			return y;
		return z;
	}

}
