/*
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
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
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.UIManager;

import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.gui.graph.DynamicView.Manipulators;

import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.SBPreferences;

/**
 * This panel holds all elements to control the dynamic visualization.
 * It is a module of the view in MVC-pattern.
 *
 * @author Fabian Schwarzkopf
 */
public class DynamicControlPanel extends JPanel {

  /**
   * List of action commands.
   * @author Fabian Schwarzkopf
   */
  public enum Buttons implements ActionCommand {
    /**
     *
     */
    PLAY,
    /**
     *
     */
    PAUSE,
    /**
     *
     */
    STOP,
    /**
     *
     */
    TO_VIDEO,
    /**
     *
     */
    GRAPHSHOT,
    /**
     *
     */
    CONCENTRATIONGRAPH;

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
   * @version $Rev: 1178 $
   */
  public enum Items {
    /**
     *
     */
    FAST,
    /**
     *
     */
    NORMAL,
    /**
     *
     */
    SLOW,
    /**
     *
     */
    NO_CAMERA_ANIMATION;

    /**
     * Play speeds.
     */
    private static int FAST_SPEED = (int) SBPreferences.getPreferencesFor(
      GraphOptions.class).getDouble(GraphOptions.SIM_SPEED_FAST);
    private static int NORMAL_SPEED = (int) SBPreferences
        .getPreferencesFor(GraphOptions.class).getDouble(
          GraphOptions.SIM_SPEED_NORMAL);
    private static int SLOW_SPEED = (int) SBPreferences.getPreferencesFor(
      GraphOptions.class).getDouble(GraphOptions.SIM_SPEED_SLOW);

    /**
     * Returns a string array of all speed items.
     * @return
     */
    public static String[] getAllSpeedItems() {
      return new String[] { Items.FAST.getName(), Items.NORMAL.getName(),
        Items.SLOW.getName() };
    }

    /**
     * Returns a string array of all speed items and no camera animation.
     * @return
     */
    public static String[] getAllCameraAnimationItems() {
      return new String[] { Items.FAST.getName(), Items.NORMAL.getName(),
        Items.SLOW.getName(), Items.NO_CAMERA_ANIMATION.getName() };
    }

    /**
     * Returns item for given string.
     * @param item
     * @return
     */
    public static Items getItem(String item) {
      assert item != null;
      if (item.equals(bundle.getString(FAST.toString()))) {
        return FAST;
      } else if (item.equals(bundle.getString(NORMAL.toString()))) {
        return NORMAL;
      } else if (item.equals(bundle.getString(SLOW.toString()))) {
        return SLOW;
      } else if (item.equals(bundle.getString(NO_CAMERA_ANIMATION.toString()))) {
        return NO_CAMERA_ANIMATION;
      }
      return NORMAL;
    }

    /**
     * Sets speed of different items.
     * @param item
     * @param speed
     */
    public static void setSpeed(Items item, int speed) {
      if (item == FAST) {
        FAST_SPEED = speed;
      } else if (item == NORMAL) {
        NORMAL_SPEED = speed;
      } else if (item == SLOW) {
        SLOW_SPEED = speed;
      }
    }

    /**
     * Returns localized name of this Item.
     * @return
     */
    public String getName() {
      return bundle.getString(this.toString());
    }

    /**
     * Returns speed setting for given item.
     * @param item
     * @return
     */
    public static int getSpeed(Items item) {
      if (item == FAST) {
        return FAST_SPEED;
      } else if (item == NORMAL) {
        return NORMAL_SPEED;
      } else if (item == SLOW) {
        return SLOW_SPEED;
      }
      return NORMAL_SPEED;
    }
  }

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
      .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");

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

  /**
   * Refers to {@link JComboBox} name of available manipulators.
   */
  public final String MANIPULATORS_LIST = GraphOptions.VISUALIZATION_STYLE.toString();

  /**
   * Refers to {@link JComboBox} name of available data.
   */
  public final String DATA_LIST = bundle.getString("VISUALIZATION_DATA");

  /*
   * GUI elements
   */
  private JSlider searchBar;
  private JLabel timelbl;
  private JLabel status;
  private JLabel simVelolbl;
  private JLabel cameraAnimationbl;
  private JButton play;
  private JButton pause;
  private JButton stop;
  private JButton video;
  private JButton graphShot;
  private JButton concentrationGraph;
  private JComboBox simVeloCombo;
  private JComboBox manipulatorsCombo;
  private JComboBox dataCombo;
  private JComboBox cameraAnimationCombo;
  private JSpinner simVeloSpin;
  private JCheckBox nodeLabelsCB;
  private JCheckBox reactionLabelsCB;
  private JCheckBox cycleSimulationCB;

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

  /**
   * Adds an item to the data-list.
   * @param item
   */
  public void addToDataList(String item) {
    dataCombo.addItem(item);
  }

  /**
   * Returns the activation status of this {@link DynamicControlPanel}.
   * @return
   */
  public boolean getPanelActivationStatus() {
    return panelActivationStatus;
  }

  /**
   * Returns the current selected manipulator.
   * @return
   */
  public String getSelectedManipulator() {
    return manipulatorsCombo.getSelectedItem().toString();
  }

  /**
   * Returns selection state of nodelabels-checkbox.
   * @return selections state of nodelabels-checkbox
   */
  public boolean getSelectionStateOfNodeLabels() {
    return nodeLabelsCB.isSelected();
  }

  /**
   * Returns selection state of reactionlabels-checkbox.
   * @return selections state of reactionlabels-checkbox
   */
  public boolean getSelectionStateOfReactionLabels() {
    return reactionLabelsCB.isSelected();
  }

  /**
   * Get the playspeed of the simulation as chosen by the user.
   * @return value of the velocity {@link JSpinner}.
   */
  public int getSimulationSpeed() {
    try {
      return Items.getSpeed(Items.getItem(simVeloCombo.getSelectedItem().toString()));
    } catch (Exception e) {
      logger.warning(bundle.getString("FALSE_INPUT"));
      return 25; //playspeed per default
    }
  }

  public String getCameraAnimationMode() {
    return cameraAnimationCombo.getSelectedItem().toString();
  }

  /**
   * Initialize this panel.
   */
  private void init() {
    logger.fine("Entering init method");
    GridBagLayout gbl = new GridBagLayout();
    setLayout(gbl);
    SBPreferences prefs = SBPreferences.getPreferencesFor(GraphOptions.class);

    //init icons
    ImageIcon playIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_PLAY_16.png"));
    UIManager.put("GPL_PLAY_16", playIcon);
    ImageIcon pauseIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_PAUSE_16.png"));
    UIManager.put("GPL_PLAY_16", pauseIcon);
    ImageIcon stopIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_STOP_16.png"));
    UIManager.put("GPL_STOP_16", stopIcon);
    ImageIcon toVideoIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_VIDEO_16.png"));
    UIManager.put("GPL_VIDEO_16", toVideoIcon);
    ImageIcon toImageIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_IMAGE_16.png"));
    UIManager.put("GPL_IMAGE_16", toImageIcon);
    ImageIcon concentrationIcon = new ImageIcon(SimulatorUI.class.getResource("graph/GPL_CONCENTRATION.png"));
    UIManager.put("GLP_CONCENTRATION", concentrationIcon);

    //init searchbar
    searchBar = new JSlider();
    searchBar.setMajorTickSpacing(1);
    searchBar.setPaintTicks(true);
    searchBar.setValue(0);
    searchBar.setToolTipText(bundle.getString("SEARCHBAR_TOOLTIP"));
    searchBar.addChangeListener(controller);

    //init checkboxes
    nodeLabelsCB = GUITools.createJCheckBox(GraphOptions.SHOW_NODE_LABELS, prefs, controller);
    nodeLabelsCB.addActionListener(controller);
    reactionLabelsCB = GUITools.createJCheckBox(GraphOptions.SHOW_REACTION_LABELS, prefs, controller);
    reactionLabelsCB.addActionListener(controller);
    cycleSimulationCB = GUITools.createJCheckBox(GraphOptions.CYCLE_SIMULATION, prefs, controller);
    cycleSimulationCB.addActionListener(controller);

    timelbl = new JLabel(MessageFormat.format("{0}: {1}", new Object[]{bundle.getString("TIMEPOINT"), "N/A"}));
    status = new JLabel();

    //init buttons
    play = GUITools.createButton(playIcon, controller, Buttons.PLAY, Buttons.PLAY.getToolTip());
    pause = GUITools.createButton(pauseIcon, controller, Buttons.PAUSE, Buttons.PAUSE.getToolTip());
    stop = GUITools.createButton(stopIcon, controller, Buttons.STOP, Buttons.STOP.getToolTip());
    video = GUITools.createButton(toVideoIcon, controller, Buttons.TO_VIDEO, Buttons.TO_VIDEO.getToolTip());
    graphShot = GUITools.createButton(toImageIcon, controller, Buttons.GRAPHSHOT, Buttons.GRAPHSHOT.getToolTip());
    concentrationGraph = GUITools.createButton(concentrationIcon, controller, Buttons.CONCENTRATIONGRAPH, Buttons.CONCENTRATIONGRAPH.getToolTip());

    //init speedcontrol
    simVelolbl = new JLabel(GraphOptions.SIM_SPEED_CHOOSER.getDisplayName());
    simVeloCombo = new JComboBox();
    simVeloCombo.setName(GraphOptions.SIM_SPEED_CHOOSER.toString());
    simVeloCombo.setToolTipText(bundle.getString("SIM_SPEED_CHOOSER_TOOLTIP"));
    simVeloCombo.addItem(Items.FAST.getName());
    simVeloCombo.addItem(Items.NORMAL.getName());
    simVeloCombo.addItem(Items.SLOW.getName());
    simVeloCombo.addItemListener(controller);
    simVeloSpin = new JSpinner();
    simVeloSpin.setToolTipText(bundle.getString("SIMULATIONSPEED_SPINNER_TOOLTIP"));
    //		FIXME prefs are not loaded correctly
    //	  String pref = prefs.getString(GraphOptions.SIM_SPEED_CHOOSER);
    //	  assert pref != null;
    //		Items item = Items.getItem(pref);
    //		assert item != null;
    //		setSimVeloCombo(item);

    cameraAnimationbl = new JLabel(GraphOptions.SHOW_CAMERA_ANIMATION.getDisplayName());
    cameraAnimationCombo = new JComboBox();
    cameraAnimationCombo.setName(GraphOptions.SHOW_CAMERA_ANIMATION.toString());
    cameraAnimationCombo.setToolTipText(bundle.getString("SHOW_CAMERA_ANIMATION_TOOLTIP"));
    cameraAnimationCombo.addItem(Items.FAST.getName());
    cameraAnimationCombo.addItem(Items.NORMAL.getName());
    cameraAnimationCombo.addItem(Items.SLOW.getName());
    cameraAnimationCombo.addItem(Items.NO_CAMERA_ANIMATION.getName());
    cameraAnimationCombo.setSelectedItem(prefs.getString(GraphOptions.SHOW_CAMERA_ANIMATION));
    cameraAnimationCombo.addItemListener(controller);

    //init visualizer control
    manipulatorsCombo = new JComboBox(Manipulators.getAllManipulators());
    manipulatorsCombo.setName(MANIPULATORS_LIST);
    manipulatorsCombo.setToolTipText(bundle.getString("VISUALIZATION_STYLE_TOOLTIP"));
    manipulatorsCombo.addItemListener(controller);
    manipulatorsCombo.setSelectedItem(prefs.getString(GraphOptions.VISUALIZATION_STYLE));
    dataCombo = new JComboBox();
    dataCombo.setName(DATA_LIST);
    dataCombo.setToolTipText(bundle.getString("VISUALIZATION_DATA_TOOLTIP"));
    dataCombo.addItemListener(controller);
    JPanel manipulatorsPane = new JPanel();
    manipulatorsPane.setToolTipText(bundle.getString("VISUALIZATION_TOOLTIP"));
    LayoutHelper lh = new LayoutHelper(manipulatorsPane);
    lh.add(manipulatorsCombo, true);
    lh.add(dataCombo);
    manipulatorsPane.setBorder(BorderFactory.createTitledBorder(bundle.getString("GROUP_VISUALIZATION")));
    manipulatorsPane.setMinimumSize(new Dimension(80, 20));

    //layout components
    addComponent(gbl, searchBar,          0, 0, 7, 1, GridBagConstraints.CENTER, 	GridBagConstraints.HORIZONTAL, 	1, 0, new Insets(0,0,0,0));
    addComponent(gbl, play,               0, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
    addComponent(gbl, pause,              1, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
    addComponent(gbl, stop,               2, 1, 1, 1, GridBagConstraints.WEST,  	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
    addComponent(gbl, video,              3, 1, 1, 1, GridBagConstraints.EAST,	    GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
    addComponent(gbl, graphShot,          4, 1, 1, 1, GridBagConstraints.WEST,      GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, cameraAnimationbl,  5, 1, 1, 1, GridBagConstraints.WEST,      GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, cameraAnimationCombo,6, 1, 1, 1, GridBagConstraints.WEST,      GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, timelbl, 	          6, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, simVelolbl,         0, 2, 3, 1, GridBagConstraints.WEST,	    GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
    addComponent(gbl, simVeloCombo,	      3, 2, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.NONE,	    0, 0, new Insets(2,2,2,2));
    addComponent(gbl, simVeloSpin,	      4, 2, 1, 1, GridBagConstraints.WEST, 	    GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
    addComponent(gbl, status,             6, 2, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.HORIZONTAL,  1, 0, new Insets(2,2,2,2));
    addComponent(gbl, reactionLabelsCB,   8, 2, 1, 1, GridBagConstraints.WEST,      GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, nodeLabelsCB,       8, 1, 1, 1, GridBagConstraints.WEST,      GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, cycleSimulationCB,  7, 1, 1, 1, GridBagConstraints.WEST,      GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, manipulatorsPane,   10, 0, 3, 3, GridBagConstraints.CENTER,    GridBagConstraints.BOTH,        0, 0, new Insets(2,2,2,2));
    addComponent(gbl, concentrationGraph, 9, 1, 1, 1, GridBagConstraints.CENTER,    GridBagConstraints.NONE,        0, 0, new Insets(2,2,2,2));

    //disable components as long as there is no data core
    Component[] elements = { play, pause, stop, video, graphShot,
      searchBar, simVeloCombo, simVeloSpin, nodeLabelsCB, cameraAnimationCombo, cycleSimulationCB,
      concentrationGraph, reactionLabelsCB, manipulatorsCombo, dataCombo };
    GUITools.setEnabledForAll(false, elements);
    panelActivationStatus = false;

    logger.fine("DynamicControlPanel initialized.");
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
    /*
     * Bugfix: controller has to be unassigned to change boundries of
     * JSlider, otherwise boundrie changing will invoke change of JSlider
     * stateChange thus invoking timepointchange in core. Maybe timepoint of
     * that core is set to 0 if timepoint doesn't exist.
     */
    controller.setCore(null);
    searchBar.setMinimum(0);
    searchBar.setMaximum(timepointsOfSimulation.length-1);
    maxTime = core.getMaxTime();

    /*
     * Controller needs to be assigned after setting the boundries of JSlider.
     * Otherwise the current JSlider-value will change, thus invoking a change of the
     * current saved timepoint of the core (same as above).
     */
    controller.setCore(core);
    setSearchbarValue(core.getCurrTimepoint());
    Component[] elements = { play, pause, stop, video, graphShot,
      searchBar, simVeloCombo, nodeLabelsCB, cycleSimulationCB,
      concentrationGraph, reactionLabelsCB, manipulatorsCombo, dataCombo };
    GUITools.setEnabledForAll(true, elements);

    core.cameraAnimation = !cameraAnimationCombo.getSelectedItem().toString().equals(bundle.getString("NO_CAMERA_ANIMATION"));
    core.playAgain = core.cameraAnimation;

    if(!GraphOptions.CAMERA_WAY_FILE.getValue(SBPreferences.getPreferencesFor(GraphOptions.class)).toString().
        equals(GraphOptions.CAMERA_WAY_FILE.getDefaultValue().toString())) {
      cameraAnimationCombo.setEnabled(true);

      if(core.cameraAnimation && (core.getAnotherObserver() == null)) {
        core.setAnotherObserver(new CameraAnimationView());
      }
    }

    panelActivationStatus = true;
  }

  /**
   * Enables {@link DynamicControlPanel} elements accordant to pause status.
   */
  public void setPauseStatus() {
    play.setEnabled(true);
    searchBar.setEnabled(true);
    pause.setEnabled(true);
    simVeloCombo.setEnabled(true);
    video.setEnabled(true);
    graphShot.setEnabled(true);
    cycleSimulationCB.setEnabled(true);
    if(!GraphOptions.CAMERA_WAY_FILE.getValue(SBPreferences.getPreferencesFor(GraphOptions.class)).toString().
        equals(GraphOptions.CAMERA_WAY_FILE.getDefaultValue().toString())) {
      cameraAnimationCombo.setEnabled(true);
    }
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
    cameraAnimationCombo.setEnabled(false);
    video.setEnabled(false);
    graphShot.setEnabled(false);
  }

  /**
   * Sets the {@link JSlider} to the given timepoint and updates the time label.
   * @param timepoint
   */
  public void setSearchbarValue(double timepoint) {
    if (core != null) {
      searchBar.setValue(core.getIndexOfTimepoint(timepoint));
      timelbl.setText(MessageFormat.format("{0}: {1,number,0.00} / {2,number,0.00}", new Object[]{bundle.getString("TIMEPOINT"), timepoint, maxTime}));
    }
  }

  /**
   * Sets selected manipulator in {@link JComboBox}.
   * @param manipulator
   */
  public void setSelectedManipulator(Manipulators manipulator) {
    manipulatorsCombo.setSelectedItem(manipulator.getName());
  }

  /**
   * Sets {@link JComboBox} containing visualization data to given dataName.
   * Ensure that {@link JComboBox} listener changes visualized data
   * respectively.
   *
   * @param dataName
   */
  public void setSelectedVisualizationData(String dataName) {
    dataCombo.setSelectedItem(dataName);
  }

  /**
   * Sets selection state of nodelabels-checkbox.
   * @param bool
   */
  public void setSelectionStateOfNodeLabels(boolean bool) {
    nodeLabelsCB.setSelected(bool);
  }

  /**
   * Sets selection state of reactionlabels-checkbox.
   * @param bool
   */
  public void setSelectionStateOfReactionLabels(boolean bool) {
    reactionLabelsCB.setSelected(bool);
  }

  /**
   * Setting the simulation speed by {@link JComboBox} items.
   * @param item
   */
  public void setSimVeloCombo(Items item) {
    simVeloCombo.setSelectedItem(item.getName());
    simVeloSpin.setValue(Items.getSpeed(item));
  }

  /**
   * Setting the camera animation speed by {@link JComboBox} items
   * @param item
   */
  public void setCameraAnimationCombo(Items item) {
    cameraAnimationCombo.setSelectedItem(item.getName());
  }

  /**
   * Displays a given string on the {@link DynamicControlPanel}.
   * @param label
   */
  public void setStatusString(String label) {
    status.setText(label);
  }

  /**
   * Enables {@link DynamicControlPanel} elements accordant to stop status.
   */
  public void setStopStatus() {
    Component[] elements = { play, pause, stop, video, graphShot,
      searchBar, simVeloCombo, nodeLabelsCB, cycleSimulationCB, concentrationGraph, reactionLabelsCB,
      manipulatorsCombo, dataCombo };
    GUITools.setEnabledForAll(true, elements);

    if(!GraphOptions.CAMERA_WAY_FILE.getValue(SBPreferences.getPreferencesFor(GraphOptions.class)).toString().
        equals(GraphOptions.CAMERA_WAY_FILE.getDefaultValue().toString())) {
      cameraAnimationCombo.setEnabled(true);
    }
  }

  /**
   * Enables {@link DynamicControlPanel} elements accordant to video status.
   */
  public void setVideoStatus() {
    Component[] elements = { play, pause, video, graphShot,
      searchBar, simVeloCombo, nodeLabelsCB, cameraAnimationCombo, cycleSimulationCB,
      concentrationGraph, reactionLabelsCB, manipulatorsCombo, dataCombo };
    GUITools.setEnabledForAll(false, elements);
  }
}
