/**
 * 
 */
package org.sbml.simulator.math.odes;

/**
 * This {@link Exception} indicates that the integration process could not be
 * finished properly.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-08-25
 */
public class IntegrationException extends Exception {

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 2320641087420165567L;

	/**
	 * 
	 */
	public IntegrationException() {
		super();
	}

	/**
	 * @param message
	 */
	public IntegrationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public IntegrationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public IntegrationException(String message, Throwable cause) {
		super(message, cause);
	}

}
