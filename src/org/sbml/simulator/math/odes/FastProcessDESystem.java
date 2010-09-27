/**
 * 
 */
package org.sbml.simulator.math.odes;

/**
 * @author Andreas Dr&auml;ger
 * @author Alexander D&ouml;rr
 * @date 2010-09-27
 */
public interface FastProcessDESystem extends DESystem {

	/**
	 * 
	 * @return
	 */
	public boolean containsFastProcesses();

	/**
	 * 
	 * @param isProcessing
	 */
	public void setFastProcessComputation(boolean isProcessing);
	
}
