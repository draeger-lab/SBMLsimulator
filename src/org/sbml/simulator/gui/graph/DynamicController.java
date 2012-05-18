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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.simulator.gui.graph.DynamicControlPanel.Items;

import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
 * Controller class for {@link DynamicControlPanel}. It represents the
 * controller in MVC-pattern and controls any user generated event due to the
 * control panel.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicController implements ChangeListener, ActionListener,
        ItemListener, TableModelListener, PreferenceChangeListener {
    /**
     * Pointer to associated {@link DynamicCore}.
     */
    private DynamicCore core;

    /**
     * Pointer to associated {@link DynamicControlPanel}.
     */
    private DynamicControlPanel controlPanel;

    /**
     * Pointer to asociated {@link DynamicView}.
     */
    private DynamicView view;

    /**
     * Constructs a new {@link DynamicController} with the corresponding
     * {@link DynamicView}.
     * 
     * @param view
     */
    public DynamicController(DynamicView view) {
        this.view = view;
    }

    /**
     * Sets the {@link DynamicCore} of this controller.
     * 
     * @param core
     *            {@link DynamicCore}
     */
    public void setCore(DynamicCore core) {
        this.core = core;
    }

    /**
     * Sets the corresponding {@link DynamicControlPanel}.
     * 
     * @param controlPanel
     */
    public void setControlPanel(DynamicControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (core != null) {
            if (e.getSource() instanceof JButton) {
                if (e.getActionCommand().equals("PLAY")) {
                    core.setPlayspeed(controlPanel.getSimulationSpeed());
                    core.play();
                    controlPanel.setPlayStatus();
                } else if (e.getActionCommand().equals("PAUSE")) {
                    core.pausePlay();
                    controlPanel.setPauseStatus();
                } else if (e.getActionCommand().equals("STOP")) {
                    core.stopPlay();
                    controlPanel.setStopStatus();
                } else if (e.getActionCommand().equals("TOVIDEO")) {
                    // TODO save as video with maximum sim-speed
                }
            } else if (e.getSource() instanceof JCheckBox) {
                // labels switched on/off
                view.updateGraph();
            }
        }
        if (e.getSource() instanceof JRadioButton) {
            if (e.getActionCommand().equals("NODESIZE")) {
                controlPanel.setNodecolorSelectionState(false);
                controlPanel.setNodesizeSelectionState(true);
                view.setGraphManipulator(getSelectedGraphManipulator());
                view.updateGraph();
            } else if (e.getActionCommand().equals("NODECOLOR")) {
                controlPanel.setNodesizeSelectionState(false);
                controlPanel.setNodecolorSelectionState(true);
                view.setGraphManipulator(getSelectedGraphManipulator());
                view.updateGraph();
            }
        }
    }
    
    /**
     * Returns user selected {@link GraphManipulator} with selected options.
     * If there isn't yet a {@link DynamicCore} assigned, return is null.
     * @return
     */
    public GraphManipulator getSelectedGraphManipulator(){
        if (core != null) {
            SBPreferences prefs = SBPreferences.getPreferencesFor(GraphOptions.class);
            // get current options
            float reactionsMinLineWidth = prefs.getFloat(GraphOptions.MIN_LINE_WIDTH);
            float reactionsMaxLineWidth = prefs.getFloat(GraphOptions.MAX_LINE_WIDTH);
            if (controlPanel.getSelectionStateOfNodesize()) {
                // get current options
                double minNodeSize = prefs.getDouble(GraphOptions.MIN_NODE_SIZE);
                double maxNodeSize = prefs.getDouble(GraphOptions.MAX_NODE_SIZE);
                return new ManipulatorOfNodeSize(view.getGraph(),
                        view.getSBMLDocument(), core.getMinMaxOfIDs(view
                                .getSelectedSpecies()),
                        core.getMinMaxOfIDs(view.getSelectedReactions()),
                        minNodeSize, maxNodeSize, reactionsMinLineWidth,
                        reactionsMaxLineWidth);
            } else if (controlPanel.getSelectionStateOfNodecolor()) {
                //get current options
                Color color1 = Option.parseOrCast(Color.class, prefs.get(GraphOptions.COLOR1));
                Color color2 = Option.parseOrCast(Color.class, prefs.get(GraphOptions.COLOR2));
                double nodeSize = prefs.getDouble(GraphOptions.COLOR_NODE_SIZE);
                
                return new ManipulatorOfNodeColor(view.getGraph(),
                        view.getSBMLDocument(), core.getMinMaxOfIDs(view
                                .getSelectedSpecies()),
                        core.getMinMaxOfIDs(view.getSelectedReactions()),
                        nodeSize, color1, color2, reactionsMinLineWidth,
                        reactionsMaxLineWidth);
            }

            // in any other case return nodesize manipulator per default.
            return new ManipulatorOfNodeSize(view.getGraph(),
                    view.getSBMLDocument(), core.getMinMaxOfIDs(view
                            .getSelectedSpecies()), core.getMinMaxOfIDs(view
                            .getSelectedReactions()));
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        if (core != null) {
            if (e.getSource() instanceof JSlider) {
                int timepoint = ((JSlider) e.getSource()).getValue();
                core.setCurrTimepoint(timepoint);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            controlPanel
                    .setSimVeloCombo(Items.getItem(ie.getItem().toString()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.
     * TableModelEvent)
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        if(core != null){
            view.setGraphManipulator(getSelectedGraphManipulator());
            view.updateGraph();
        }
    }

    /* (non-Javadoc)
     * @see java.util.prefs.PreferenceChangeListener#preferenceChange(java.util.prefs.PreferenceChangeEvent)
     */
    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        // immediately change graph visualization
        if (controlPanel.getSelectionStateOfNodesize()) {
            if (evt.getKey().equals("MAX_NODE_SIZE")
                    || evt.getKey().equals("MIN_NODE_SIZE")
                    || evt.getKey().equals("MIN_LINE_WIDTH")
                    || evt.getKey().equals("MAX_LINE_WIDTH")) {
                view.setGraphManipulator(getSelectedGraphManipulator());
            }
        } else if (controlPanel.getSelectionStateOfNodecolor()) {
            if (evt.getKey().equals("COLOR1") || evt.getKey().equals("COLOR2")
                    || evt.getKey().equals("COLOR_NODE_SIZE")
                    || evt.getKey().equals("MIN_LINE_WIDTH")
                    || evt.getKey().equals("MAX_LINE_WIDTH")) {
                view.setGraphManipulator(getSelectedGraphManipulator());
            }
        }

        if (evt.getKey().equals("SHOW_NODE_LABELS")) {
            controlPanel.setSelectionStateOfNodeLabels(SBPreferences
                    .getPreferencesFor(GraphOptions.class).getBoolean(
                            GraphOptions.SHOW_NODE_LABELS));
            view.updateGraph();
        }

        if (evt.getKey().equals("SHOW_REACTION_LABELS")) {
            controlPanel.setSelectionStateOfReactionLabels(SBPreferences
                    .getPreferencesFor(GraphOptions.class).getBoolean(
                            GraphOptions.SHOW_REACTION_LABELS));
            view.updateGraph();
        }
    }

}
