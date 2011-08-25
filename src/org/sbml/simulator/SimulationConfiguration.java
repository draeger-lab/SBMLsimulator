/*
 * $Id: SimulationConfiguration.java 16:09:12 draeger$ $URL:
 * SimulationConfiguration.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */

package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.sbml.jsbml.Model;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.SimulationOptions;

import de.zbit.util.prefs.SBPreferences;

/**
 * This class stores all values necessary to perform a simulation.
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev$
 * @since 1.0
 */

public class SimulationConfiguration implements PropertyChangeListener {
  
  /**
   * The model for the current simulation.
   */
  private Model model;
  
  /**
   * The solver for the current simulation.
   */
  private AbstractDESSolver solver;
  
  /**
   * The start time for the current simulation.
   */
  private double start;
  
  /**
   * The end time for the current simulation.
   */
  private double end;
  
  /**
   * The step size for the current simulation.
   */
  private double stepSize;
  
  /**
   * Include reactions in the output or not.
   */
  private boolean includeReactions;
  
  /**
   * Creates a new simulation configuration for the simulation of the given
   * model.
   * 
   * @param model
   */
  public SimulationConfiguration(Model model) {
    this.model = model;
  }
  
  /**
   * @return the model
   */
  public Model getModel() {
    return model;
  }
  
  /**
   * @return the solver
   */
  public AbstractDESSolver getSolver() {
    return solver;
  }
  
  /**
   * @return the start
   */
  public double getStart() {
    return start;
  }
  
  /**
   * @return the end
   */
  public double getEnd() {
    return end;
  }
  
  /**
   * @return the stepSize
   */
  public double getStepSize() {
    return stepSize;
  }
    
  /**
   * @return the includeReactions
   */
  public boolean isIncludeReactions() {
    return includeReactions;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent
   * )
   */
  public void propertyChange(PropertyChangeEvent evt) {
    String property = evt.getPropertyName();
    SBPreferences prefs = SBPreferences
    .getPreferencesFor(SimulationOptions.class);

    if ("model".equals(property)) {
      
      model = (Model) evt.getNewValue();
      
    } else if ("solver".equals(property)) {
      
      solver = (AbstractDESSolver) evt.getNewValue();
      prefs.put(SimulationOptions.SIM_ODE_SOLVER, solver.getClass()
        .getSimpleName());      
      
    } else if ("start".equals(property)) {
      
      start = (Double) evt.getNewValue();
      prefs.put(SimulationOptions.SIM_START_TIME, start);
     
      
    } else if ("end".equals(property)) {
      
      end = (Double) evt.getNewValue();
      prefs.put(SimulationOptions.SIM_END_TIME, end);
      
    } else if ("stepSize".equals(property)) {
      
      stepSize = (Double) evt.getNewValue();
      prefs.put(SimulationOptions.SIM_STEP_SIZE, stepSize);
      
    } else if ("includeReactions".equals(property)) {
      
      includeReactions = (Boolean) evt.getNewValue();
      
    }
  }
  
}
