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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

    // TODO
	
	/**
	 * List of action commands.
	 * @author Fabian Schwarzkopf
	 * @version $Rev$
	 */
	public enum Buttons implements ActionCommand{
		PLAY, PAUSE, STOP, TOVIDEO, NODESIZE, NODECOLOR;

		/* (non-Javadoc)
		 * @see de.zbit.gui.actioncommand.ActionCommand#getName()
		 */
		@Override
		public String getName() {
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
	 * List of Controlpanel objects.
	 * @author Fabian Schwarzkopf
	 * @version $Rev$
	 */
	public enum Items {
	    FAST, NORMAL, SLOW, NODESIZE, NODECOLOR;
	    
	    /**
	     * Returns localized name of this Item.
	     * @return
	     */
        public String getName(){
            return bundle.getString(this.toString());
        }
        
        /**
         * Returns speed setting for given item.
         * @param item
         * @return
         */
        public int getSpeed(Items item) {
            if (item == FAST) {
                return 5;
            } else if (item == NORMAL) {
                return 25;
            } else if (item == SLOW) {
                return 80;
            }
            return 0;
        }
        
        /**
         * Returns item to given string.
         * @param item
         * @return
         */
        public static Items getItem(String item) {
            if (item.equals(bundle.getString("FAST"))) {
                return FAST;
            } else if (item.equals(bundle.getString("NORMAL"))) {
                return NORMAL;
            } else if (item.equals(bundle.getString("SLOW"))) {
                return SLOW;
            }
            return NORMAL;
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
	 * Saves the maximum time for the timelbl.
	 * (To save some computing time).
	 */
	private double maxTime;
	
    /**
     * Field for the activation status of panel elements.
     */
	private boolean panelActivationStatus;
	
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
	private JLabel nodeLabelslbl;
	private JCheckBox nodeLabelsCB;
	private JLabel reactionLabelslbl;
	private JCheckBox reactionLabelsCB;
	private JRadioButton nodesize;
	private JRadioButton nodecolor;
	
	/**
	 * Constructs a new control panel.
	 * @param controller
	 */
	public DynamicControlPanel(DynamicController controller) {
        this.controller = controller;
        this.controller.setControlPanel(this);
		init();
	}
	
	/**
	 * Constructs a new control panel with all dependancies.
	 * @param view
	 * @param controller
	 */
	public DynamicControlPanel(DynamicView view, DynamicController controller) {
        this(controller);
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
		setTimepoint(core.getCurrTimepoint());
        Component[] elements = { play, video, searchBar, simVeloCombo,
                simVeloSpin, nodeLabelsCB, reactionLabelsCB, nodesize,
                nodecolor, nodeLabelslbl, reactionLabelslbl };
		GUITools.setEnabledForAll(true, elements);
		panelActivationStatus = true;
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
		
		nodeLabelslbl = new JLabel(bundle.getString("NODELABELS"));
		nodeLabelslbl.setName("NODELABELS"); //name for mouselistener
		nodeLabelslbl.addMouseListener(controller);
		nodeLabelsCB = new JCheckBox();
		nodeLabelsCB.addActionListener(controller);
		reactionLabelslbl = new JLabel(bundle.getString("REACTIONLABELS"));
		reactionLabelslbl.setName("REACTIONLABELS");
		reactionLabelslbl.addMouseListener(controller);
		reactionLabelsCB = new JCheckBox();
		reactionLabelsCB.addActionListener(controller);
		
		searchBar.addChangeListener(controller);
		timelbl = new JLabel(MessageFormat.format("{0}: {1}", new Object[]{bundle.getString("TIMEPOINT"), "N/A"}));
		play = GUITools.createButton(playIcon, controller, Buttons.PLAY, Buttons.PLAY.getToolTip());
		pause = GUITools.createButton(pauseIcon, controller, Buttons.PAUSE, Buttons.PAUSE.getToolTip());
		stop = GUITools.createButton(stopIcon, controller, Buttons.STOP, Buttons.STOP.getToolTip());
		video = GUITools.createButton(toVideoIcon, controller, Buttons.TOVIDEO, Buttons.TOVIDEO.getToolTip());
		
		simVelolbl = new JLabel(bundle.getString("SIMULATIONSPEED"));
		simVeloCombo = new JComboBox();
		simVeloCombo.addItem(Items.FAST.getName());
		simVeloCombo.addItem(Items.NORMAL.getName());
		simVeloCombo.addItem(Items.SLOW.getName());
		simVeloCombo.addItemListener(controller);		
		simVeloSpin = new JSpinner();
		setSimVeloCombo(Items.NORMAL); //by default 'normal speed'
	
		nodesize = new JRadioButton(Buttons.NODESIZE.getName());
		nodesize.setActionCommand(Buttons.NODESIZE.toString());
		nodesize.addActionListener(controller);
		nodesize.setSelected(true);
		nodecolor = new JRadioButton(Buttons.NODECOLOR.getName());
		nodecolor.setActionCommand(Buttons.NODECOLOR.toString());
		nodecolor.addActionListener(controller);
		JPanel manipulatorsPane = new JPanel();
		manipulatorsPane.setLayout(new BorderLayout());
		manipulatorsPane.add(nodesize, BorderLayout.LINE_START);
		manipulatorsPane.add(nodecolor, BorderLayout.AFTER_LAST_LINE);
		manipulatorsPane.setBorder(BorderFactory.createTitledBorder(bundle.getString("MANIPULATORCHOOSER")));
		manipulatorsPane.setMinimumSize(new Dimension(80, 20));
		
		addComponent(gbl, searchBar, 	0, 0, 7, 1, GridBagConstraints.CENTER, 	GridBagConstraints.HORIZONTAL, 	1, 0, new Insets(0,0,0,0));
		addComponent(gbl, play, 		0, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, pause, 		1, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, stop, 		2, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, video, 		3, 1, 1, 1, GridBagConstraints.WEST,	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, timelbl, 		3, 1, 2, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVelolbl,	0, 2, 3, 1, GridBagConstraints.WEST,	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVeloCombo,	3, 2, 1, 1, GridBagConstraints.CENTER,  GridBagConstraints.HORIZONTAL,	1, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVeloSpin,	4, 2, 1, 1, GridBagConstraints.EAST, 	GridBagConstraints.BOTH, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, reactionLabelslbl,6, 2, 1, 1, GridBagConstraints.WEST,    GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,0));
        addComponent(gbl, reactionLabelsCB, 5, 2, 1, 1, GridBagConstraints.EAST,    GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
		addComponent(gbl, nodeLabelslbl,    6, 1, 1, 1, GridBagConstraints.WEST,    GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,0));
		addComponent(gbl, nodeLabelsCB,     5, 1, 1, 1, GridBagConstraints.EAST,    GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
		addComponent(gbl, manipulatorsPane, 7, 0, 3, 3, GridBagConstraints.CENTER,  GridBagConstraints.BOTH,        0, 0, new Insets(2,2,2,2));

        Component[] elements = { play, pause, stop, video, searchBar,
                simVeloCombo, simVeloSpin, nodeLabelsCB, reactionLabelsCB,
                nodesize, nodecolor, nodeLabelslbl, reactionLabelslbl };
		GUITools.setEnabledForAll(false, elements);
		panelActivationStatus = false;
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
	public void setSimVeloCombo(Items item) {
	    simVeloCombo.setSelectedItem(item.getName());
	    simVeloSpin.setValue(item.getSpeed(item));
	}
	
    /**
     * Enables {@link DynamicControlPanel} elements accordant to play status.
     */
    public void setPlayStatus() {
        play.setEnabled(false);
        searchBar.setEnabled(false);
        pause.setEnabled(true);
        stop.setEnabled(true);
        simVeloCombo.setEnabled(false);
        simVeloSpin.setEnabled(false);
        video.setEnabled(false);
    }
	
    /**
     * Enables {@link DynamicControlPanel} elements accordant to pause status.
     */
    public void setPauseStatus() {
        play.setEnabled(true);
        searchBar.setEnabled(true);
        pause.setEnabled(true);
        simVeloCombo.setEnabled(true);
        simVeloSpin.setEnabled(true);
        video.setEnabled(true);
    }
    
    /**
     * Enables {@link DynamicControlPanel} elements accordant to stop status.
     */
    public void setStopStatus() {
        play.setEnabled(true);
        searchBar.setEnabled(true);
        pause.setEnabled(true);
        stop.setEnabled(true);
        simVeloCombo.setEnabled(true);
        simVeloSpin.setEnabled(true);
        video.setEnabled(true);
    }
	
	/**
	 * Returns selection state of nodelabels-checkbox.
	 * @return selections state of nodelabels-checkbox
	 */
	public boolean getSelectionStateOfNodeLabels() {
	    return nodeLabelsCB.isSelected();
	}
	
	/**
	 * Sets selection state of nodelabels-checkbox.
	 * @param bool
	 */
	public void setSelectionStateOfNodeLabels(boolean bool){
	    nodeLabelsCB.setSelected(bool);
	}
	
	/**
     * Returns selection state of reactionlabels-checkbox.
     * @return selections state of reactionlabels-checkbox
     */
    public boolean getSelectionStateOfReactionLabels() {
        return reactionLabelsCB.isSelected();
    }
    
    /**
     * Sets selection state of reactionlabels-checkbox.
     * @param bool
     */
    public void setSelectionStateOfReactionLabels(boolean bool){
        reactionLabelsCB.setSelected(bool);
    }
	
	/**
	 * Sets the selection state of the nodesize-radiobutton.
	 * @param bool
	 */
	public void setNodesizeSelectionState(boolean bool){
	    nodesize.setSelected(bool);
	}
	
	/**
	 * Returns selection state of nodesize-checkbox.
	 * @return
	 */
	public boolean getSelectionStateOfNodesize(){
	    return nodesize.isSelected();
	}
	
	/**
	 * Sets the selection state of the nodecolor-radiobutton.
	 * @param bool
	 */
	public void setNodecolorSelectionState(boolean bool){
	    nodecolor.setSelected(bool);
	}
	
	/**
	 * Returns selections state of nodecolor-checkbox.
	 * @param bool
	 * @return
	 */
	public boolean getSelectionStateOfNodecolor(){
	    return nodecolor.isSelected();
	}
	
	/**
	 * Returns the activation status of this {@link DynamicControlPanel}.
	 * @return
	 */
	public boolean getPanelActivationStatus(){
	    return panelActivationStatus;
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
