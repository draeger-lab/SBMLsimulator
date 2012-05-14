/*
 * $Id:  FBAPanel.java 16:38:58 Meike Aichele$
 * $URL: FBAPanel.java $
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

import org.sbml.jsbml.SBMLDocument;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FBAPanel extends JPanel{

	/**
	 * the FBAPanel consists of 3 components:
	 * - a ChartPanel, which shows the computed data of fba in a table
	 * - a SettingPanel, which shows the setting that can be made to change the result of fba
	 *   and its visualization (the VODPanel)
	 * - a VODPanel, which visualizes the results of fba in a diagram.
	 */
	private ChartPanel chart;
	private SettingPanel settings;
	private VODPanel vod;
	private SBMLDocument document;
	private static final long serialVersionUID = 1L;
	
	public FBAPanel (SBMLDocument document) {
		this.chart = new ChartPanel();
		this.settings = new SettingPanel();
		this.vod = new VODPanel();
		this.document = document;
		// TODO: call FluxBalanceAnalysis
	}

	/**
	 * @param chart the chart to set
	 */
	public void setChartPanel(ChartPanel chart) {
		this.chart = chart;
	}

	/**
	 * @return the chart
	 */
	public ChartPanel getChartPanel() {
		return chart;
	}

	/**
	 * @param settings the settings to set
	 */
	public void setSettingPanel(SettingPanel settings) {
		this.settings = settings;
	}

	/**
	 * @return the settings
	 */
	public SettingPanel getSettingPanel() {
		return settings;
	}

	/**
	 * @param vod the vod to set
	 */
	public void setVODPanel(VODPanel vod) {
		this.vod = vod;
	}

	/**
	 * @return the vod
	 */
	public VODPanel getVODPanel() {
		return vod;
	}

}
