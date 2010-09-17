/**
 * 
 */
package org.sbml.simulator.gui;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 */
public enum SelectionCommand {
	/**
	 * Action to select all elements of one group
	 */
	ALL,
	/**
	 * Action to deselect all elements of one groupo
	 */
	NONE;

	/**
	 * Human readable text corresponding to these commands.
	 * 
	 * @return
	 */
	public String getText() {
		switch (this) {
		case ALL:
			return "Select all";
		default:
			return "Deselect all";
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getToolTip() {
		switch (this) {
		case ALL:
			return "With this button you can easily select all elements on this panel";
		default:
			return "With this button you can easily deselect all elements on this panel";
		}
	}
}
