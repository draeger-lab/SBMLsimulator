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
package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import org.sbml.jsbml.Model;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.DESSolver;

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
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SimulationConfiguration.class.getName());
    
  /**
   * The model for the current simulation.
   */
  private Model model;
  
  /**
   * The solver for the current simulation.
   */
  private DESSolver solver;
  
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
   * {@link Model}.
   * 
   * @param model
   */
  public SimulationConfiguration(Model model) {
    this.model = model;
    this.start = 0d;
    this.end = 0d;
    this.stepSize = 0d;
    this.includeReactions = true;
    this.solver = null;
  }
  
  /**
   * Convenient constructor that creates a fully specified
   * {@link SimulationConfiguration} for the given {@link Model} including all
   * other features.
   * 
   * @param model
   * @param solver
   * @param timeStart
   * @param timeEnd
   * @param stepSize
   * @param includeReactions
   */
  public SimulationConfiguration(Model model, DESSolver solver,
    double timeStart, double timeEnd, double stepSize, boolean includeReactions) {
    this(model);
    this.solver = solver;
    this.start = timeStart;
    this.end = timeEnd;
    this.stepSize = stepSize;
    this.includeReactions = includeReactions;
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
  public DESSolver getSolver() {
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
  
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
		String property = evt.getPropertyName();
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(SimulationOptions.class);
		logger.finest(evt.getPropertyName() + "\t" + evt.getNewValue());

		if ("model".equals(property)) {
			
			model = (Model) evt.getNewValue();
			
		} else if (SimulationOptions.ODE_SOLVER.toString().equals(property)) {
			
			solver = (AbstractDESSolver) evt.getNewValue();
			prefs.put(SimulationOptions.ODE_SOLVER, solver.getClass().getName());
			
		} else if (SimulationOptions.SIM_START_TIME.toString().equals(property)) {
			
			start = (Double) evt.getNewValue();
			prefs.put(SimulationOptions.SIM_START_TIME, start);
			
		} else if (SimulationOptions.SIM_END_TIME.toString().equals(property)) {
			
			end = (Double) evt.getNewValue();
			prefs.put(SimulationOptions.SIM_END_TIME, end);
			
		} else if (SimulationOptions.SIM_STEP_SIZE.toString().equals(property)) {
			
			stepSize = (Double) evt.getNewValue();
			prefs.put(SimulationOptions.SIM_STEP_SIZE, stepSize);
			
		} else if ("includeReactions".equals(property)) {
			
			includeReactions = (Boolean) evt.getNewValue();
			
		}
	}
  
}
