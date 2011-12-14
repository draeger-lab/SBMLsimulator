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
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.SBMLException;
import org.sbml.simulator.SimulationConfiguration;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.DESystem;
import org.simulator.math.odes.MultiTable;
import org.simulator.sbml.SBMLinterpreter;

import de.zbit.util.ResourceManager;

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
   * The {@link ResourceBundle} for localization.
   */
  private static final ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
  
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
    solver.setIncludeIntermediates(includeReactions);
    MultiTable solution = solver.solve(system, initialValues, timeStart, timeEnd);

    if (solver.isUnstable()) {
      throw new DerivativeException(bundle.getString("MODEL_UNSTABLE_EXCEPTION"));
    }
    return solution;
  }

  /**
   * The configuration for the simulation.
   */
  private SimulationConfiguration configuration;
  
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
  public SimulationWorker(SimulationConfiguration configuration) throws Exception {
    this.configuration = configuration;
    this.configuration.getSolver().addPropertyChangeListener(this);
  }
  
  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  protected MultiTable doInBackground() throws Exception {
    SBMLinterpreter interpreter = new SBMLinterpreter(configuration.getModel());
    solution = solveByStepSize(configuration.getSolver(), interpreter, interpreter
        .getInitialValues(), configuration.getStart(), configuration.getEnd(),
      configuration.getStepSize(), configuration.isIncludeReactions());
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
    configuration.getSolver().removePropertyChangeListener(this);
  }

  /**
   * 
   * @return
   */
  public MultiTable getSolution() {
    return solution;
  } 
  
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    getPropertyChangeSupport().firePropertyChange(evt);
  }

}
