/**
 * 
 */
package org.sbml.simulator.math.odes;

import java.util.List;

/**
 * This Class represents an event-driven DES
 * 
 * @author Alexander D&ouml;rr
 * @author Andreas Dr&auml;ger
 * @date 2010-02-04
 * 
 */
public interface EventDESystem extends DESystem {

	/**
	 * Counts the number of events in this system.
	 * 
	 * @return The number of events that are to be checked and potentially
	 *         evaluated in each time point.
	 */
	public int getNumEvents();

	/**
	 * Counts the number of rules to be evaluated in each time point.
	 * 
	 * @return The number of rules in the system.
	 */
	public int getNumRules();

	/**
	 * 
	 * @param t
	 *            The current simulation time.
	 * @param Y
	 *            The current change of the system.
	 * @return
	 * @throws IntegrationException
	 */
	public List<DESAssignment> processAssignmentRules(double t, double Y[])
			throws IntegrationException;

	/**
	 * Returns an array with delays (entries >=0) for the events triggered
	 * either by the time t or by the concentrations of the species stored in Y.
	 * The new values for the species are stored in res. The positions in the
	 * array returned by this method correspond to the positions in Y/res.
	 * 
	 * @param t
	 *            The current simulation time.
	 * @param Y
	 *            The current change of the system.
	 * 
	 * @return Returns an array with delays for the change of concentration due
	 *         to events
	 * @throws IntegrationException
	 */
	public List<DESAssignment> processEvents(double t, double Y[])
			throws IntegrationException;

}
