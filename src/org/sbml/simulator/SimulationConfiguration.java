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
package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

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
   * The end time for the current simulation.
   */
  private double end;

	/**
   * Include reactions in the output or not.
   */
  private boolean includeReactions;

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
   * The step size for the current simulation.
   */
  private double stepSize;
    
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
   * Copy constructor.
   * 
   * @param sc
   */
	public SimulationConfiguration(SimulationConfiguration sc) {
		this(sc.getModel(), sc.getSolver(), sc.getStart(), sc.getEnd(), sc
				.getStepSize(), sc.isIncludeReactions());
	}
  
  /* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new SimulationConfiguration(this);
	}
  
  /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// Check if the given object is a pointer to precisely the same object:
		if (super.equals(obj)) { 
			return true; 
		}
		// Check if the given object is of identical class and not null: 
		if ((obj == null) || (!getClass().equals(obj.getClass()))) { 
			return false; 
		}
		if (obj instanceof SimulationConfiguration) {
		  SimulationConfiguration conf = (SimulationConfiguration) obj;
		  boolean equal = true;
		  equal &= start == conf.getStart();
		  equal &= end == conf.getEnd();
		  equal &= includeReactions && conf.isIncludeReactions();
		  equal &= stepSize == conf.getStepSize();
		  equal &= isSetModel() == conf.isSetModel();
		  if (equal && isSetModel()) {
		  	equal &= model.equals(conf.getModel());
		  }
		  equal &= isSetSolver() == conf.isSetSolver();
		  if (equal && isSetSolver()) {
		  	equal &= solver.equals(conf.getSolver());
		  }
		  return equal;
		}
		return false;
	}
  
  /**
   * @return the end
   */
  public double getEnd() {
    return end;
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
   * @return the stepSize
   */
  public double getStepSize() {
    return stepSize;
  }
  
  /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 919;
		int hashCode = getClass().getName().hashCode();
		hashCode += prime * Double.valueOf(start).hashCode();
		hashCode += prime * Double.valueOf(end).hashCode();
		hashCode += prime * Boolean.valueOf(includeReactions).hashCode();
		hashCode += prime * Double.valueOf(stepSize).hashCode();
		if (isSetModel()) {
			hashCode += prime * model.hashCode();
		}
		if (isSetSolver()) {
			hashCode += prime * solver.hashCode();
		}
		return hashCode;
	}
  
  /**
   * @return the includeReactions
   */
  public boolean isIncludeReactions() {
    return includeReactions;
  }
  
  /**
	 * 
	 * @return
	 */
	public boolean isSetModel() {
		return model != null;
	}
  
  /**
	 * 
	 * @return
	 */
	public boolean isSetSolver() {
		return solver != null;
	}
    
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
		String property = evt.getPropertyName();
		SBPreferences prefs = SBPreferences.getPreferencesFor(SimulationOptions.class);
		boolean change = false;
		logger.fine(evt.getPropertyName() + "\t" + evt.getNewValue());

		if (property.equals("model")) {
			
			model = (Model) evt.getNewValue();
			
		} else if (SimulationOptions.ODE_SOLVER.toString().equals(property)) {
			
			solver = (AbstractDESSolver) evt.getNewValue();
			prefs.put(SimulationOptions.ODE_SOLVER, solver.getClass().getName());
			change = true;
			
		} else if (SimulationOptions.SIM_START_TIME.toString().equals(property)) {
			
			start = ((Number) evt.getNewValue()).doubleValue();
			prefs.put(SimulationOptions.SIM_START_TIME, start);
			change = true;
			
		} else if (SimulationOptions.SIM_END_TIME.toString().equals(property)) {
			
			end = ((Number) evt.getNewValue()).doubleValue();
			prefs.put(SimulationOptions.SIM_END_TIME, end);
			change = true;
			
		} else if (SimulationOptions.SIM_STEP_SIZE.toString().equals(property)) {
			
			stepSize = ((Number) evt.getNewValue()).doubleValue();
			prefs.put(SimulationOptions.SIM_STEP_SIZE, stepSize);
			change = true;
			
		} else if (property.equals("includeReactions")) {
			
			includeReactions = ((Boolean) evt.getNewValue()).booleanValue();
			
		}
		
		if (change) {
			try {
				prefs.flush();
			} catch (BackingStoreException exc) {
				logger.warning(exc.getLocalizedMessage());
			}
		}
	}
  
  /* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append('[');
		sb.append("start=");
		sb.append(start);
		sb.append(",end=");
		sb.append(end);
		sb.append(",stepSize=");
		sb.append(stepSize);
		sb.append(",solver=");
		sb.append(solver.toString());
		sb.append(",includeReactions=");
		sb.append(includeReactions);
		sb.append(",model=");
		sb.append(model);
		sb.append(']');
		return sb.toString();
	}
  
}
