/*
 * $Id:  SettingPanel.java 16:40:47 Meike Aichele$
 * $URL: SettingPanel.java $
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2012 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.fba.gui;

import javax.swing.JPanel;

/**
 * this class visualizes the settings which can be made to change 
 * the result of the fba and its visualization
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class SettingPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private FBASettingPanel fbaSettings;
	private VODSettingPanel vodSettings;

	public SettingPanel() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param fbaSettings the fbaSettings to set
	 */
	public void setFbaSettingPanel(FBASettingPanel fbaSettings) {
		this.fbaSettings = fbaSettings;
	}
	/**
	 * @return the fbaSettings
	 */
	public FBASettingPanel getFbaSettingPanel() {
		return fbaSettings;
	}
	/**
	 * @param vodSettings the vodSettings to set
	 */
	public void setVodSettingPanel(VODSettingPanel vodSettings) {
		this.vodSettings = vodSettings;
	}
	/**
	 * @return the vodSettings
	 */
	public VODSettingPanel getVodSettingPanel() {
		return vodSettings;
	}
}
