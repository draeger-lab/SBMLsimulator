/**
 * Title: JAVA-EVA Description: Copyright: Copyright (c) 2002 Company:
 * University of T&uuml;bingen, Computer Architecture
 *
 * @author
 * @version 1.0
 */

package org.sbml.simulator.math.odes;

import java.io.Serializable;

/**
 * A {@link DESSolver} provides algorithm for the numerical simulation of given
 * {@link DESystem}s.
 * 
 * @author Andreas Dr&auml;ger
 * @since 2.0
 * @date Sep 10, 2007
 * 
 */
public interface DESSolver extends Serializable {

	/**
	 * Obtain the currently set integration step size.
	 * 
	 * @return
	 */
	public double getStepSize();

	/**
	 * Method to check whether the solution of the numerical integration
	 * procedure contains {@link Double.NaN} values.
	 * 
	 * @return
	 */
	public boolean isUnstable();

	/**
	 * Set the integration step size.
	 * 
	 * @param stepSize
	 */
	public void setStepSize(double stepSize);

	/**
	 * Solves the given {@link DESystem} using new initial conditions in each
	 * time step. The given {@link MultiBlockTable} contains the expected
	 * solution of the solver at certain time points. The solver has the task to
	 * re-initialize the integration procedure in each given time point using
	 * the initial values from this state.
	 * 
	 * @param DES
	 * @param initConditions
	 * @return
	 * @throws IntegrationException
	 */
	public MultiBlockTable solve(DESystem DES,
			MultiBlockTable.Block initConditions) throws IntegrationException;

	/**
	 * Solves the {@link DESystem} from the begin time till the end time
	 * according to the given step size.
	 * 
	 * @param DES
	 * @param initialValues
	 * @param timeBegin
	 * @param timeEnd
	 * @return
	 * @throws IntegrationException
	 */
	public MultiBlockTable solve(DESystem DES, double[] initialValues,
			double timeBegin, double timeEnd) throws IntegrationException;

	/**
	 * Solves the given differential equation system with the step size h and
	 * the number of steps as given starting at the value x.
	 * 
	 * @param DES
	 *            The differential equation system to be solved.
	 * @param initalValues
	 *            Return value at the start point.
	 * @param x
	 *            Start argument.
	 * @param h
	 *            Step size.
	 * @param steps
	 *            Number of steps.
	 * @return A matrix containing the values of x, x + h, x + h + steps/h... in
	 *         the rows and the columns contain the return values for the
	 *         arguments.
	 * @throws IntegrationException
	 *             if something's wrong...
	 */
	public MultiBlockTable solve(DESystem DES, double[] initalValues, double x,
			double h, int steps) throws IntegrationException;

	/**
	 * Solves the given {@link DESystem} at the given time points using the
	 * currently set integration step size and the given initial values.
	 * 
	 * @param DES
	 * @param initialvalue
	 * @param timepoints
	 * @return
	 * @throws IntegrationException
	 */
	public MultiBlockTable solve(DESystem DES, double[] initialvalue,
			double[] timepoints) throws IntegrationException;

	/**
	 * 
	 * @return
	 */
	public DESSolver clone();
}
