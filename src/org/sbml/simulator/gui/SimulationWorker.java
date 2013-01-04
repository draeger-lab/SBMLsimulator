/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.simulator.SimulationConfiguration;
import org.simulator.math.odes.AdaptiveStepsizeIntegrator;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.DESystem;
import org.simulator.math.odes.MultiTable;
import org.simulator.sbml.SBMLinterpreter;

import de.zbit.util.ResourceManager;
import de.zbit.util.Timer;

/**
 * Performs a dynamic simulation in background.
 * 
 * @author Andreas Dr&auml;ger
 * @author Philip Stevens
 * @author Max Zwie&szlig;ele
 * @date 2010-09-17
 * @version $Rev$
 * @since 1.0
 */
public class SimulationWorker extends SwingWorker<MultiTable, MultiTable> implements PropertyChangeListener {

	/**
   * The {@link ResourceBundle} for localization.
   */
  private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
  
  /**
	 * A {@link Logger} for this class
	 */
  private static final transient Logger logger = Logger.getLogger(SimulationWorker.class.getName());
  
  /**
   * 
   * @param solver
   * @param system
   * @param initialValues
   * @param timeStart
   * @param timeEnd
   * @param stepSize
   * @param includeReactions
   * @param absTol ignored if solver is not an instance of {@link AdaptiveStepsizeIntegrator}
   * @param relTol ignored if solver is not an instance of {@link AdaptiveStepsizeIntegrator}
   * @return
   * @throws SBMLException
   * @throws DerivativeException
   */
  public static MultiTable solveByStepSize(DESSolver solver, DESystem system, double[] initialValues, double timeStart,
    double timeEnd, double stepSize, boolean includeReactions, double absTol, double relTol)
      throws SBMLException,
      DerivativeException {
    
    solver.setStepSize(stepSize);
    solver.setIncludeIntermediates(includeReactions);
    if (solver instanceof AdaptiveStepsizeIntegrator) {
    	AdaptiveStepsizeIntegrator integrator = (AdaptiveStepsizeIntegrator) solver;
    	integrator.setAbsTol(absTol);
    	integrator.setRelTol(relTol);
    }
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
    this.timer = new Timer();
  }
  
  /**
   * 
   */
  private Timer timer;
  private Thread computationThread;
  
  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  protected MultiTable doInBackground() throws ModelOverdeterminedException{
  	timer.reset();
    
    try {
    	computationThread = Thread.currentThread();
    	SBMLinterpreter interpreter = new SBMLinterpreter(configuration.getModel());
    	DESSolver solver = configuration.getSolver().clone();
    	solver.addPropertyChangeListener(this);
			solution = solveByStepSize(solver,
				interpreter, interpreter.getInitialValues(), configuration.getStart(),
				configuration.getEnd(), configuration.getStepSize(),
				configuration.isIncludeReactions(), configuration.getAbsTol(),
				configuration.getRelTol());   
    	return solution;
    } catch (DerivativeException exc) {
    	logger.warning(exc.getLocalizedMessage());
    	return null;
    }
  }
  
  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#done()
   */
  @Override
  protected void done() {
		double time = timer.getAndReset(false);
  	if (isCancelled()) {
			if ((computationThread != null) && computationThread.isAlive()
					&& !computationThread.isInterrupted()) {
				logger.fine("Thread was not yet interrupted!");
				computationThread.interrupt();
			}
			logger.info(MessageFormat.format(bundle.getString("SIMULATION_CANCELED"), time));
			firePropertyChange("done", null, null);
		} else {
			MultiTable result = null;
			String logMessage = null;
			try {
				result = get();
				firePropertyChange("done", null, result);
			} catch (InterruptedException exc) {
				logMessage = exc.getLocalizedMessage();
			} catch (ExecutionException exc) {
				logMessage = exc.getLocalizedMessage();
			}
			configuration.getSolver().removePropertyChangeListener(this);
			if (logMessage != null) {
				logger.warning(logMessage);
			} else if (result != null) {
				logger.info(MessageFormat.format(bundle.getString("SIMULATION_TIME"), time));
			}
		}
  }
  
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    getPropertyChangeSupport().firePropertyChange(evt);
  }

}
