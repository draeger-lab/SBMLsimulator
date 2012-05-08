/*
 * $Id$
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
package org.sbml.simulator.gui.graph;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.UIManager;

import org.sbml.simulator.gui.SimulatorUI;

import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.util.ResourceManager;

/**
 * This panel holds all elements to control the dynamic visualization.
 * It is a module of the view in MVC-pattern.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicControlPanel extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6692563909762370732L;
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(DynamicControlPanel.class.getName());
	
	/**
     * Localization support.
     */
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.locales.Simulator");
	
	/**
	 * List of action commands.
	 * @author Fabian Schwarzkopf
	 * @version $Rev$
	 */
	public enum Buttons implements ActionCommand{
		PLAY, PAUSE, STOP, TOVIDEO;

		/* (non-Javadoc)
		 * @see de.zbit.gui.actioncommand.ActionCommand#getName()
		 */
		@Override
		public String getName() {
//			return StringUtil.firstLetterLowerCase(toString());
		    return bundle.getString(this.toString());
		}

		/* (non-Javadoc)
		 * @see de.zbit.gui.actioncommand.ActionCommand#getToolTip()
		 */
		@Override
		public String getToolTip() {
		    return bundle.getString(this.toString() + "_TOOLTIP");
		}
	}
	
	/**
	 * Pointer to related {@link DynamicCore}.
	 */
	private DynamicCore core;
	
	/**
	 * Registered {@link DynamicController}.
	 */
	private DynamicController controller;
	
	/**
	 * Pointer to related {@link DynamicView}.
	 */
	private DynamicView view;
	
	/**
	 * Saves the maximum time for the timelbl.
	 * (To save some computing time).
	 */
	private double maxTime;
	
	/*
	 * GUI elements
	 */
	private JLabel timelbl;
	private JSlider searchBar;
	private JButton play;
	private JButton pause;
	private JButton stop;
	private JButton video;
	private JLabel simVelolbl;
	private JComboBox simVeloCombo;
	private JSpinner simVeloSpin;
	private JLabel labelslbl;
	private JCheckBox labelsCB;
	
	/**
	 * Constructs a new control panel.
	 * @param controller
	 */
	public DynamicControlPanel(DynamicController controller) {
        this.controller = controller;
        this.controller.setControlerPanel(this);
		init();
	}
	
	/**
	 * Constructs a new control panel with all dependancies.
	 * @param view
	 * @param controller
	 */
	public DynamicControlPanel(DynamicView view, DynamicController controller) {
        this(controller);
        this.view = view;
    }
	
	/**
     * Sets the related {@link DynamicCore} to this control panel and activates
     * all corresponding elements.
     * 
     * @param core
     *            {@link DynamicCore}
     */
	public void setCore(DynamicCore core) {
		this.core = core;
		double[] timepointsOfSimulation = core.getTimepoints();
		searchBar.setMinimum(0);
		searchBar.setMaximum(timepointsOfSimulation.length-1);
		maxTime = core.getMaxTime();
		
		/*
		 * Controller needs to be assigned after setting the boundries of JSlider.
		 * Otherwise the current JSlider-value will change, thus invoking a change of the 
		 * current saved timepoint of the core.
		 * (Does not cause inconsistency but leads to a change of the first setted curr core value).
		 */
		controller.setCore(core);
		controller.setView(view);
		setTimepoint(core.getCurrTimepoint());
		Component[] elements = {play, video, searchBar, simVeloCombo, simVeloSpin, labelsCB};
		GUITools.setEnabledForAll(true, elements);
	}
	
	/**
	 * Initialize this panel.
	 */
	private void init() {
		logger.fine("Entering init method");
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		
		ImageIcon playIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_PLAY_16.png"));
		UIManager.put("GPL_PLAY_16", playIcon);
		ImageIcon pauseIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_PAUSE_16.png"));
		UIManager.put("GPL_PLAY_16", pauseIcon);
		ImageIcon stopIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_STOP_16.png"));
		UIManager.put("GPL_STOP_16", stopIcon);
		ImageIcon toVideoIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_VIDEO_16.png"));
		UIManager.put("GPL_VIDEO_16", stopIcon);
		
		searchBar = new JSlider();
		searchBar.setMajorTickSpacing(1);
		searchBar.setPaintTicks(true);
		searchBar.setValue(0);
		
		labelslbl = new JLabel(bundle.getString("LABELS"));
		labelsCB = new JCheckBox();
		labelsCB.addActionListener(controller);
		
		searchBar.addChangeListener(controller);
		timelbl = new JLabel(MessageFormat.format("{0}: {1}", new Object[]{bundle.getString("TIMEPOINT"), "N/A"}));
		play = GUITools.createButton(playIcon, controller, Buttons.PLAY, Buttons.PLAY.getToolTip());
		pause = GUITools.createButton(pauseIcon, controller, Buttons.PAUSE, Buttons.PAUSE.getToolTip());
		stop = GUITools.createButton(stopIcon, controller, Buttons.STOP, Buttons.STOP.getToolTip());
		video = GUITools.createButton(toVideoIcon, controller, Buttons.TOVIDEO, Buttons.TOVIDEO.getToolTip());
		
		simVelolbl = new JLabel(bundle.getString("SIMULATIONSPEED"));
		simVeloCombo = new JComboBox();
		simVeloCombo.addItem(bundle.getString("FAST"));
		simVeloCombo.addItem(bundle.getString("NORMAL"));
		simVeloCombo.addItem(bundle.getString("SLOW"));
		simVeloCombo.addItemListener(controller);		
		simVeloSpin = new JSpinner();
		setSimVeloCombo(bundle.getString("NORMAL")); //by default 'normal speed'
		
		addComponent(gbl, searchBar, 	0, 0, 7, 1, GridBagConstraints.CENTER, 	GridBagConstraints.HORIZONTAL, 	1, 0, new Insets(0,0,0,0));
		addComponent(gbl, play, 		0, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, pause, 		1, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, stop, 		2, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, video, 		3, 1, 1, 1, GridBagConstraints.WEST,	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, timelbl, 		3, 1, 2, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVelolbl,	0, 2, 4, 1, GridBagConstraints.WEST,	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVeloCombo,	3, 2, 1, 1, GridBagConstraints.CENTER,  GridBagConstraints.HORIZONTAL,	1, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVeloSpin,	4, 2, 1, 1, GridBagConstraints.EAST, 	GridBagConstraints.BOTH, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, labelslbl,    5, 2, 1, 1, GridBagConstraints.WEST,    GridBagConstraints.NONE,        0, 0, new Insets(2,8,2,0));
		addComponent(gbl, labelsCB,     6, 2, 1, 1, GridBagConstraints.WEST,    GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));

		Component[] elements = {play, pause, stop, video, searchBar, simVeloCombo, simVeloSpin, labelsCB};
		GUITools.setEnabledForAll(false, elements);
		logger.fine("DynamicControlPanel initialized.");
	}
		
	/**
	 * Sets the {@link JSlider} to the given timepoint and updates the time label.
	 * @param timepoint
	 */
	public void setTimepoint(double timepoint) {
	    if (core != null) {
    		searchBar.setValue(core.getIndexOfTimepoint(timepoint));
    		timelbl.setText(MessageFormat.format("{0}: {1,number,0.00} / {2,number,0.00}", new Object[]{bundle.getString("TIMEPOINT"), timepoint, maxTime}));
	    }
	}
	
	/**
	 * Get the playspeed of the simulation as chosen by the user.
	 * @return value of the velocity {@link JSpinner}.
	 */
	public int getSimulationSpeed() {
	    try {
	        return ((Integer) simVeloSpin.getValue()).intValue();
	    } catch(Exception e) {
	        logger.warning(bundle.getString("FALSE_INPUT"));
	        return 200; //playspeed per default
	    }
	}
	
	/**
	 * Setting the simulation speed by {@link JComboBox} items.
	 * @param item
	 */
	public void setSimVeloCombo(Object item) {
	    simVeloCombo.setSelectedItem(item);
	    if (item.equals(bundle.getString("FAST"))) {
	        simVeloSpin.setValue(45);
	    } else if (item.equals(bundle.getString("NORMAL"))) {
	        simVeloSpin.setValue(150);
	    } else if (item.equals(bundle.getString("SLOW"))) {
	        simVeloSpin.setValue(400);
	    }
	}
	
	/**
	 * Sets enable status for the searchbar.
	 * (If the user presses play or toVideo, this method should be invoked by the controller).
	 * @param bool
	 */
	public void enableSearchBar(boolean bool) {
		searchBar.setEnabled(bool);
	}
	
	/**
	 * Sets enable status for the play button.
	 * (If the user presses play or toVideo, this method should be invoked by the controller).
	 * @param bool
	 */
	public void enablePlay(boolean bool) {
		play.setEnabled(bool);
	}

	/**
	 * Sets enable status for the stop button.
	 * (If the user presses play or toVideo, this method should be invoked by the controller).
	 * @param bool
	 */
	public void enableStop(boolean bool) {
		stop.setEnabled(bool);
	}
	
	/**
	 * Sets enable status for the pause button.
	 * (If the user presses play or toVideo, this method should be invoked by the controller).
	 * @param bool
	 */
	public void enablePause(boolean bool) {
		pause.setEnabled(bool);
	}
	
	/**
     * Sets enable status for the combobox to choose velocity.
     * (If the user presses play or toVideo, this method should be invoked by the controller).
	 * @param bool
	 */
	public void enableSimVeloComboBox(boolean bool) {
	    simVeloCombo.setEnabled(bool);
	}
	
	/**
     * Sets enable status for the spinner to choose velocity.
     * (If the user presses play or toVideo, this method should be invoked by the controller).
     * @param bool
     */
	public void enableSimVeloSpin(boolean bool) {
	    simVeloSpin.setEnabled(bool);
	}
	
	/**
     * Sets enable status for the checkbox to enable labels.
     * (If the user presses play or toVideo, this method should be invoked by the controller).
     * @param bool
     */
	public void enableLabelsCB(boolean bool) {
	    labelsCB.setEnabled(bool);
	}
	
	/**
     * Sets enable status for the video button.
     * (If the user presses play or toVideo, this method should be invoked by the controller).
     * @param bool
     */
	public void enableVideo(boolean bool) {
	    video.setEnabled(bool);
	}
	
	/**
	 * Returns selection state of labels-checkbox.
	 * @return selections state of labels-checkbox
	 */
	public boolean getSelectionStateOfLabels() {
	    return labelsCB.isSelected();
	}
	
	/**
	 * Helper to layout components.
	 * @param gbl
	 * @param c
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param anchor
	 * @param fill
	 * @param weightx
	 * @param weighty
	 * @param insets
	 */
	private void addComponent(GridBagLayout gbl, Component c, int x, int y,
            int width, int height, int anchor, int fill, double weightx, double weighty, Insets insets) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = fill;
		gbc.gridx = x; gbc.gridy = y;
		gbc.gridwidth = width; 
		gbc.gridheight = height;
		gbc.weightx = weightx; 
		gbc.weighty = weighty;
		gbc.insets = insets;
		gbc.anchor = anchor;
		gbl.setConstraints(c, gbc);
		add(c);
	}
}
