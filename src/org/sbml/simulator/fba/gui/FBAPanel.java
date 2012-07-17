/*
 * $Id:  FBAPanel.java 16:38:58 Meike Aichele$
 * $URL$
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.fba.controller.Constraints;
import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;
import org.sbml.simulator.gui.LegendPanel;
import org.sbml.simulator.gui.SimulationPanel;


/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FBAPanel extends JPanel implements ActionListener, TableModelListener{


	/**
	 * the FBAPanel consists of 3 components:
	 * - a ChartPanel, which shows the computed data of fba in a table
	 * - a SettingPanel, which shows the setting that can be made to change the result of fba
	 *   and its visualization (the VODPanel)
	 * - a VODPanel, which visualizes the results of fba in a diagram.
	 */
	private ChartPanel chart;
	private FBASettingPanel settings;
	private VODPanel vod;
	private SBMLDocument currentDoc;
	private FluxBalanceAnalysis fba;
	private SimulationPanel simPanel;
	private File gibbsFile;
	private File concFile;
	private static final long serialVersionUID = 1L;

	public FBAPanel (SBMLDocument document, File ... files) {
		super(new BorderLayout());
		try {
			fba = new FluxBalanceAnalysis(document);
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.chart = new ChartPanel();
		this.settings = new FBASettingPanel();
		this.vod = new VODPanel();
		init();
		// TODO: call FluxBalanceAnalysis
	}

	public FBAPanel(SBMLDocument sbmlDocument) {
		super(new BorderLayout());
		currentDoc = sbmlDocument;
		this.chart = new ChartPanel();
		this.settings = new FBASettingPanel(this);
		this.vod = new VODPanel();
		setVisible(true);
	}


	private void init() {
		JSplitPane jsp = new JSplitPane();
		vod.setBorder(BorderFactory.createLoweredBevelBorder());

		// legend panel
		LegendPanel legendPanel = new LegendPanel(currentDoc.getModel(), true);
		legendPanel.addTableModelListener(this);
		legendPanel.setBorder(BorderFactory.createLoweredBevelBorder());

		// split left components
		JSplitPane topDown = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
				legendPanel, settings);
		topDown.setDividerLocation(topDown.getDividerLocation() + 100);

		// split all
		jsp.setLeftComponent(topDown);
		jsp.setRightComponent(vod);
		jsp.setDividerLocation(topDown.getDividerLocation() + 200);

		add(jsp);
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
	public void setFBASettingPanel(FBASettingPanel settings) {
		this.settings = settings;
	}

	/**
	 * @return the settings
	 */
	public FBASettingPanel getFBASettingPanel() {
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

	/**
	 * @return the fba
	 */
	public FluxBalanceAnalysis getFba() {
		return fba;
	}

	/**
	 * @param simPanel
	 */
	public void setSimulatorPanel(SimulationPanel simPanel) {
		this.simPanel = simPanel;
		init();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tableChanged(TableModelEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the currentDoc
	 */
	public SBMLDocument getCurrentDoc() {
		return currentDoc;
	}

	/**
	 * @param currentDoc the currentDoc to set
	 */
	public void setCurrentDoc(SBMLDocument currentDoc) {
		this.currentDoc = currentDoc;
	}

	/**
	 * @param gibbsFile the gibbsFile to set
	 */
	public void setGibbsFile(File gibbsFile) {
		this.gibbsFile = gibbsFile;
	}

	/**
	 * @return the gibbsFile
	 */
	public File getGibbsFile() {
		return gibbsFile;
	}

	/**
	 * @param concFile the concFile to set
	 */
	public void setConcFile(File concFile) {
		this.concFile = concFile;
	}

	/**
	 * @return the concFile
	 */
	public File getConcFile() {
		return concFile;
	}

}
