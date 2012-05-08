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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Controller class for {@link DynamicControlPanel}. It represents the
 * controller in MVC-pattern and controls any user generated event due to the
 * control panel.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicController implements ChangeListener, ActionListener,
        ItemListener, TableModelListener {

    /**
     * Pointer to associated {@link DynamicCore}.
     */
    private DynamicCore core;

    /**
     * Pointer to associated {@link DynamicControlPanel}.
     */
    private DynamicControlPanel controlPanel;

    /**
     * Pointer to asociated {@link DynamicView}
     */
    private DynamicView view;

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
     * Sets the {@link DynamicView} for this controller.
     * @param view {@link DynamicView}
     */
    public void setView(DynamicView view) {
        this.view = view;
    }

    public void setControlerPanel(DynamicControlPanel controlPanel) {
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
                    setPlayStatus();
                    // TODO: enable button when play finished
                } else if (e.getActionCommand().equals("PAUSE")) {
                    core.pausePlay();
                    setPauseStatus();
                } else if (e.getActionCommand().equals("STOP")) {
                    core.stopPlay();
                    setStopStatus();
                } else if (e.getActionCommand().equals("TOVIDEO")) {
                    // TODO save as video with maximum sim-speed
                }
            } else if (e.getSource() instanceof JCheckBox) {
                // labels switched on/off
                view.updateGraph();
            }
        }
    }
    
    /**
     * Enables {@link DynamicControlPanel} elements accordant to play status.
     */
    public void setPlayStatus() {
        controlPanel.enablePlay(false);
        controlPanel.enableSearchBar(false);
        controlPanel.enablePause(true);
        controlPanel.enableStop(true);
        controlPanel.enableSimVeloComboBox(false);
        controlPanel.enableSimVeloSpin(false);
        controlPanel.enableVideo(false);
    }
    
    /**
     * Enables {@link DynamicControlPanel} elements accordant to pause status.
     */
    public void setPauseStatus() {
        controlPanel.enablePlay(true);
        controlPanel.enableSearchBar(true);
        controlPanel.enablePause(false);
        controlPanel.enableSimVeloComboBox(true);
        controlPanel.enableSimVeloSpin(true);
        controlPanel.enableVideo(true);
    }

    /**
     * Enables {@link DynamicControlPanel} elements accordant to stop status.
     */
    public void setStopStatus() {
        controlPanel.enablePlay(true);
        controlPanel.enableSearchBar(true);
        controlPanel.enablePause(false);
        controlPanel.enableStop(false);
        controlPanel.enableSimVeloComboBox(true);
        controlPanel.enableSimVeloSpin(true);
        controlPanel.enableVideo(true);
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
            controlPanel.setSimVeloCombo(ie.getItem());
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        // TODO each time a checkbox gets changed, this method is invoked two
        // times BUG?
        if (core != null) {
            // System.out.println("tablechange");
            view.updateGraph();
        }
    }

}
