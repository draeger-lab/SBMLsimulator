/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 * @version $Rev$
 * @since 1.0
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
