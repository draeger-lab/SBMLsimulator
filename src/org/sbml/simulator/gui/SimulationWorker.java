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
package org.sbml.simulator.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.DESystem;
import org.simulator.math.odes.MultiTable;
import org.simulator.sbml.SBMLinterpreter;

/**
 * @author Andreas Dr&auml;ger
 * @author Philip Stevens
 * @author Max Zwießele
 * @date 2010-09-17
 * @version $Rev$
 * @since 1.0
 */
public class SimulationWorker extends SwingWorker<MultiTable, MultiTable> implements PropertyChangeListener{

	/**
	 * A {@link Logger} for this class
	 */
  private static final Logger logger = Logger.getLogger(SimulationWorker.class.getName());
  
  /**
   * 
   * @param solver
   * @param system
   * @param initialValues
   * @param timeStart
   * @param timeEnd
   * @param stepSize
   * @param includeReactions
   * @return
   * @throws SBMLException
   * @throws IntegrationException
   */
  public static MultiTable solveByStepSize(DESSolver solver, DESystem system, double[] initialValues, double timeStart,
    double timeEnd, double stepSize, boolean includeReactions)
      throws SBMLException,
      DerivativeException {
    
    solver.setStepSize(stepSize);
    
    if (solver instanceof AbstractDESSolver) {
      ((AbstractDESSolver) solver)
          .setIncludeIntermediates(includeReactions);
    }
    MultiTable solution = solver.solve(system, initialValues, timeStart, timeEnd);

    if (solver.isUnstable()) {
      throw new DerivativeException("Simulation not possible because the model is unstable.");
    }
    return solution;
  }
  
  /**
   * 
   */
  private boolean includeReactions;

  /**
   * 
   */
  private SBMLinterpreter interpreter;
  
  /**
   * The integrator for the simulation
   */
  private DESSolver solver;
  
  /**
   * Configuration for the solver.
   */
  private double timeStart, timeEnd, stepSize;
  
  /**
   * The solution of the simulation
   */
  private MultiTable solution;
  
  
  /**
   * 
   * @param solver
   * @param model
   * @param timeStart
   * @param timeEnd
   * @param stepSize
   * @param includeReactions
   * @throws Exception 
   */
  public SimulationWorker(DESSolver solver, Model model, double timeStart,
    double timeEnd, double stepSize, boolean includeReactions) throws Exception {
    
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
    this.stepSize = stepSize;
    this.includeReactions = includeReactions;
    this.interpreter = new SBMLinterpreter(model);
    this.solver = solver;
    this.solver.setIncludeIntermediates(true);
    solver.addPropertyChangeListener(this);
    
  }
  
  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  protected MultiTable doInBackground() throws Exception {
    solution = solveByStepSize(solver, interpreter, interpreter.getInitialValues(),
      timeStart, timeEnd, stepSize, includeReactions);
    return solution;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#done()
   */
  @Override
  protected void done() {
    try {
      firePropertyChange("done", null, get());
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, e.getLocalizedMessage(),e);
    } catch (ExecutionException e) {     
      logger.log(Level.WARNING, e.getLocalizedMessage(),e);
    }
    solver.removePropertyChangeListener(this);
  }

  /*
   * (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    getPropertyChangeSupport().firePropertyChange(evt);
  } 
  
  /**
   * 
   * @return
   */
  public MultiTable getSolution() {
    return solution;
  }

}
