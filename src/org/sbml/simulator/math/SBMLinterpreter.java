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
package org.sbml.simulator.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Assignment;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.CallableSBase;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.Variable;
import org.sbml.jsbml.util.Maths;
import org.sbml.jsbml.util.compilers.ASTNodeCompiler;
import org.sbml.jsbml.util.compilers.ASTNodeValue;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.jsbml.validator.OverdeterminationValidator;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.DESAssignment;
import org.sbml.simulator.math.odes.EventDESystem;
import org.sbml.simulator.math.odes.FastProcessDESystem;
import org.sbml.simulator.math.odes.IntegrationException;
import org.sbml.simulator.math.odes.RichDESystem;

import eva2.tools.math.RNG;

/**
 * <p>
 * This DifferentialEquationSystem takes a model in SBML format and maps it to a
 * data structure that is understood by the {@link AbstractDESSolver} of EvA2.
 * Therefore, this class implements all necessary functions expected by SBML.
 * </p>
 * 
 * @author Alexander D&ouml;rr
 * @author Andreas Dr&auml;ger
 * @author Dieudonn&eacute; Motsou Wouamba
 * @date 2007-09-06
 * @version $Rev$
 * @since 1.0
 */
public class SBMLinterpreter implements ASTNodeCompiler, EventDESystem,
		RichDESystem, FastProcessDESystem, FirstOrderDifferentialEquations {

    /**
     * 
     */
    private static final Logger logger = Logger.getLogger(SBMLinterpreter.class
	    .getName());
    
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 3453063382705340995L;

	/**
	 * Contains a list of all algebraic rules transformed to assignment rules
	 * for further processing
	 */
	private List<AssignmentRule> algebraicRules;

	/**
	 * Hashes the name of all species located in a compartment to the position
	 * of their compartment in the Y vector. When a species has no compartment,
	 * it is hashed to null.
	 */
	private Map<String, Integer> compartmentHash;

	/**
	 * This field is necessary to also consider local parameters of the current
	 * reaction because it is not possible to access these parameters from the
	 * model. Hence we have to memorize an additional reference to the Reaction
	 * and thus to the list of these parameters.
	 */
	protected Reaction currentReaction;

	/**
	 * Holds the current time of the simulation
	 */
	private double currentTime;

	/**
	 * This array stores for every event an object of EventInProcess that is
	 * used to handel event processing during simulation
	 */
	private EventInProcess events[];

	/**
	 * This table is necessary to store the values of arguments when a function
	 * definition is evaluated. For an identifier of the argument the
	 * corresponding value will be stored.
	 */
	private Map<String, Double> funcArgs;

	/**
	 * An array, which stores all computed initial values of the model. If this
	 * model does not contain initial assignments, the initial values will only
	 * be taken once from the information stored in the model. Otherwise they
	 * have to be computed again as soon as the parameter values of this model
	 * are changed, because the parameters may influence the return values of
	 * the initial assignments.
	 */
	protected double[] initialValues;

	/**
	 * An array, which stores for each constraint the list of times, in which
	 * the constraint was violated during the simulation.
	 */
	protected List<Double>[] listOfContraintsViolations;

	/**
	 * The model to be simulated.
	 */
	protected Model model;

	/**
	 * Hashes the name of all compartments, species, and global parameters to an
	 * value object which contains the position in the Y vector
	 */
	private Map<String, Integer> symbolHash;

	/**
	 * An array of strings that memorizes at each position the identifier of the
	 * corresponding element in the Y array.
	 */
	private String[] symbolIdentifiers;

	/**
	 * An array of the velocities of each reaction within the model system.
	 * Holding this globally saves many new memory allocations during simulation
	 * time.
	 */
	protected double[] v;

	/**
	 * This {@link Map} saves the current stoichiometric coefficients for those
	 * {@link SpeciesReference} objects that are a target to an
	 * {@link Assignment}.
	 */
	protected Map<String, Double> stoichiometricCoefHash;

	/**
	 * An array of the current concentration of each species within the model
	 * system.
	 */
	protected double[] Y;

	/**
	 * A boolean indicating whether the solver is currently processing fast
	 * reactions or not
	 */
	private boolean isProcessingFastReactions = false;

	/**
	 * A boolean indicating whether a model has fast reactions or not.
	 */
	private boolean hasFastReactions = false;

	/**
	 * Stores the indices of the events trigged for the current point in time.
	 */
	private List<Integer> runningEvents;

	/**
	 * Stores the indices of the events trigged for a future point in time.
	 */
	private List<Integer> delayedEvents;

	/**
	 * <p>
	 * This constructs a new DifferentialEquationSystem for the given SBML
	 * model. Note that only a maximum of <code>Integer.MAX_VALUE</code> species
	 * can be simulated. If the model contains more species, this class is not
	 * applicable.
	 * </p>
	 * <p>
	 * Note that currently, units are not considered.
	 * </p>
	 * 
	 * @param model
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 */
	public SBMLinterpreter(Model model) throws ModelOverdeterminedException,
			SBMLException {
	    logger.log(Level.INFO, "Logger works");
		this.model = model;
		this.v = new double[this.model.getListOfReactions().size()];
		this.init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#abs(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue abs(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.abs(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#and(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue and(List<ASTNode> nodes) throws SBMLException {
		for (ASTNode node : nodes) {
			if (!node.compile(this).toBoolean()) {
				return getConstantFalse();
			}
		}
		return getConstantTrue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arccos(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arccos(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.acos(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arccosh(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arccosh(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arccosh(node.compile(this).toDouble()),
				this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arccot(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arccot(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arccot(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arccoth(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arccoth(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arccoth(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arccsc(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arccsc(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arccsc(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arccsch(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arccsch(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arccsch(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arcsec(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arcsec(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arcsec(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arcsech(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arcsech(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arcsech(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arcsin(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arcsin(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.asin(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arcsinh(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arcsinh(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arcsinh(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arctan(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arctan(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.atan(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#arctanh(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue arctanh(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.arctanh(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#ceiling(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue ceiling(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.ceil(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#compile(org.sbml.jsbml.Compartment)
	 */
	public ASTNodeValue compile(Compartment c) {
		return new ASTNodeValue(c.getSize(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#compile(double, int,
	 * java.lang.String)
	 */
	public ASTNodeValue compile(double mantissa, int exponent, String units) {
		return new ASTNodeValue(mantissa * Math.pow(10, exponent), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#compile(double, java.lang.String)
	 */
	public ASTNodeValue compile(double value, String units) {
		// TODO: units!
		return new ASTNodeValue(value, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#compile(int, java.lang.String)
	 */
	public ASTNodeValue compile(int value, String units) {
		// TODO: units!
		return new ASTNodeValue(value, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(org.sbml.jsbml.
	 * CallableSBase)
	 */
	public ASTNodeValue compile(CallableSBase nsb) throws SBMLException {
		Integer symbolIndex;
		if (nsb instanceof Species) {
			Species s = (Species) nsb;
			symbolIndex = symbolHash.get(nsb.getId());
			if (getCompartmentValueOf(nsb.getId()) == 0d) {
				return new ASTNodeValue(Y[symbolIndex], this);
			}

			if (s.isSetInitialAmount() && !s.getHasOnlySubstanceUnits()) {
				return new ASTNodeValue(Y[symbolIndex]
						/ getCompartmentValueOf(nsb.getId()), this);
			}

			if (s.isSetInitialConcentration() && s.getHasOnlySubstanceUnits()) {
				// return new ASTNodeValue(Y[symbolIndex], this);

				return new ASTNodeValue(Y[symbolIndex]
						* getCompartmentValueOf(nsb.getId()), this);
			}

			return new ASTNodeValue(Y[symbolIndex], this);
			// return new ASTNodeValue(Y[symbolIndex]
			// / getCompartmentValueOf(nsb.getId()), this);

		} else if (nsb instanceof SpeciesReference) {
			SpeciesReference sr = (SpeciesReference) nsb;
			if ((sr.getLevel() >= 3)
					&& this.stoichiometricCoefHash.containsKey(sr.getId())) {
				return new ASTNodeValue(this.stoichiometricCoefHash.get(sr
						.getId()), this);
			} else if (sr.isSetStoichiometryMath()) {
				return new ASTNodeValue(sr.getStoichiometryMath().getMath()
						.compile(this).toDouble(), this);
			} else {
				return new ASTNodeValue(sr.getStoichiometry(), this);
			}
		}

		else if (nsb instanceof Compartment || nsb instanceof Parameter
				|| nsb instanceof LocalParameter) {
			if (nsb instanceof LocalParameter) {
				LocalParameter p = (LocalParameter) nsb;
				// parent: list of parameter; parent of parent: kinetic law
				SBase parent = p.getParentSBMLObject().getParentSBMLObject();
				if (parent instanceof KineticLaw) {
					ListOf<LocalParameter> params = ((KineticLaw) parent)
							.getListOfParameters();
					for (int i = 0; i < params.size(); i++) {
						if (p.getId() == params.get(i).getId()) {
							return new ASTNodeValue(params.get(i).getValue(),
									this);
						}
					}
				}
			}

			symbolIndex = symbolHash.get(nsb.getId());
			return new ASTNodeValue(symbolIndex != null ? Y[symbolIndex]
					: Double.NaN, this);

		} else if (nsb instanceof FunctionDefinition) {
			return function((FunctionDefinition) nsb, new LinkedList<ASTNode>());
		} else if (nsb instanceof Reaction) {
			Reaction r = (Reaction) nsb;
			if (r.isSetKineticLaw()) {
				return r.getKineticLaw().getMath().compile(this);
			}
		}
		return new ASTNodeValue(Double.NaN, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#compile(java.lang.String)
	 */
	public ASTNodeValue compile(String name) {
		if (funcArgs != null && funcArgs.containsKey(name)) {
			// replace the name by the associated value of the argument
			return new ASTNodeValue(funcArgs.get(name).doubleValue(), this);
		} else if (symbolHash.containsKey(name)) {
			return new ASTNodeValue(Y[symbolHash.get(name)], this);
		}
		return new ASTNodeValue(String.valueOf(name), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#cos(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue cos(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.cos(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#cosh(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue cosh(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.cosh(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#cot(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue cot(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.cot(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#coth(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue coth(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.coth(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#csc(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue csc(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.csc(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#csch(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue csch(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.csch(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.jsbml.util.compilers.ASTNodeCompiler#delay(java.lang.String,
	 * org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode, java.lang.String)
	 */
	public ASTNodeValue delay(String delayName, ASTNode x, ASTNode delay,
			String timeUnits) throws SBMLException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#equal(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue eq(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(left.compile(this).toDouble() == right.compile(
				this).toDouble(), this);
	}

	/**
	 * Evaluates the algebraic rules of the given model to assignment rules
	 * 
	 * @param ar
	 * @param changeRate
	 * @throws ModelOverdeterminedException
	 */
	private void evaluateAlgebraicRule() throws ModelOverdeterminedException {
		OverdeterminationValidator odv = new OverdeterminationValidator(model);
		// model has not to be overdetermined (violation of the SBML
		// specifications)
		if (odv.isOverdetermined()) {
			throw new ModelOverdeterminedException();
		}
		// create assignment rules out of the algebraic rules
		AlgebraicRuleConverter arc = new AlgebraicRuleConverter(odv
				.getMatching(), model);
		algebraicRules = arc.getAssignmentRules();
	}

	/**
	 * Evaluates the assignment rules of the given model. This method is not to
	 * be used at timepoints > 0 because the new value is directly written into
	 * the changeRate array which is only valid at the starting point of the
	 * simulation. At later time points, the solver takes care of assignment
	 * rules with the help of the method processAssignmentRules
	 * 
	 * @param as
	 * @param Y
	 * @throws SBMLException
	 */
	private void evaluateAssignmentRule(AssignmentRule as, double changeRate[])
			throws SBMLException {
		// get symbol and assign its new value
		Integer speciesIndex = symbolHash.get(as.getVariable());
		if (speciesIndex != null) {
			changeRate[speciesIndex.intValue()] = processAssignmentVaribale(as
					.getVariable(), as.getMath());
		} else if (model.findSpeciesReference(as.getVariable()) != null) {
			SpeciesReference sr = model.findSpeciesReference(as.getVariable());
			if (sr.getConstant() == false) {
				stoichiometricCoefHash.put(sr.getId(), as.getMath().compile(
						this).toDouble());
			}
		}
	}

	/**
	 * Evaluates the rate rules of the given model
	 * 
	 * @param rr
	 * @param changeRate
	 * @throws SBMLException
	 */
	private void evaluateRateRule(RateRule rr, double changeRate[])
			throws SBMLException {

		// get symbol and assign its new rate
		Integer speciesIndex = symbolHash.get(rr.getVariable());
		// changeRate[speciesIndex] = rr.getMath().compile(this).toDouble();
		if (speciesIndex != null) {
			changeRate[speciesIndex.intValue()] = processAssignmentVaribale(rr
					.getVariable(), rr.getMath());
			// when the size of a compartment changes, the concentrations of the
			// species located in this compartment have to change as well
			if (compartmentHash.containsValue(speciesIndex)) {
				updateSpeciesConcentration(speciesIndex, changeRate);
			}
		} else if (model.findSpeciesReference(rr.getVariable()) != null) {
			SpeciesReference sr = model.findSpeciesReference(rr.getVariable());
			if (sr.getConstant() == false) {
				stoichiometricCoefHash.put(sr.getId(), rr.getMath().compile(
						this).toDouble());
			}
		}
	}

	/**
	 * Updates the concentration of species due to a change in the size of their
	 * compartment
	 * 
	 * @param compartmentIndex
	 */
	private void updateSpeciesConcentration(int compartmentIndex,
			double changeRate[]) {
		int speciesIndex;
		Species s;
		for (Entry<String, Integer> entry : compartmentHash.entrySet()) {
			if (entry.getValue() == compartmentIndex) {
				s = model.getSpecies(entry.getKey());
				if (s.isSetInitialConcentration()) {
					speciesIndex = symbolHash.get(entry.getKey());
					changeRate[speciesIndex] = -changeRate[compartmentIndex]
							* Y[speciesIndex] / Y[compartmentIndex];
				}

			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#exp(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue exp(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.exp(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#factorial(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue factorial(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.factorial(node.compile(this).toDouble()),
				this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#floor(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue floor(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.floor(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#frac(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue frac(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(left.compile(this).toDouble()
				/ right.compile(this).toDouble(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#frac(int, int)
	 */
	public ASTNodeValue frac(int numerator, int denominator) {
		return new ASTNodeValue((double) numerator / (double) denominator, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#function(java.lang.String,
	 * org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue function(FunctionDefinition function,
			List<ASTNode> arguments) throws SBMLException {
		ASTNode lambda = function.getMath();
		Hashtable<String, Double> argValues = new Hashtable<String, Double>();
		for (int i = 0; i < arguments.size(); i++) {
			argValues.put(lambda.getChild(i).compile(this).toString(),
					arguments.get(i).compile(this).toDouble());
		}
		funcArgs = argValues;
		ASTNodeValue value = lambda.getRightChild().compile(this);
		funcArgs.clear();
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#geq(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue geq(ASTNode nodeleft, ASTNode noderight)
			throws SBMLException {
		return new ASTNodeValue(nodeleft.compile(this).toDouble() >= noderight
				.compile(this).toDouble(), this);
	}

	/**
	 * Checks if the given symbol id refers to a species and returns the value
	 * of its compartment or 1d otherwise
	 * 
	 * @param symbol
	 * @param val
	 * @return
	 */
	private double getCompartmentValueOf(String symbol) {
		Integer compartmentIndex = compartmentHash.get(symbol);

		// Is species with compartment
		if (compartmentIndex != null) {
			if (Y[compartmentIndex] != 0d) {
				return Y[compartmentIndex];
			}
		}

		// Is compartment or parameter or there is no compartment for this
		// species
		return 1d;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getConstantAvogadro(java.lang.String)
	 */
	public ASTNodeValue getConstantAvogadro(String name) {
		return new ASTNodeValue(Maths.AVOGADRO, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getConstantE()
	 */
	public ASTNodeValue getConstantE() {
		return new ASTNodeValue(Math.E, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getConstantFalse()
	 */
	public ASTNodeValue getConstantFalse() {
		return new ASTNodeValue(false, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getConstantPi()
	 */
	public ASTNodeValue getConstantPi() {
		return new ASTNodeValue(Math.PI, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getConstantTrue()
	 */
	public ASTNodeValue getConstantTrue() {
		return new ASTNodeValue(true, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.DESystem#getDESystemDimension()
	 */
	public int getDESystemDimension() {
		return this.initialValues.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.DESystem#getIdentifiers()
	 */
	public String[] getIdentifiers() {
		return symbolIdentifiers;
	}

	/**
	 * Returns the initial values of the model to be simulated.
	 * 
	 * @return Returns the initial values of the model to be simulated.
	 */
	public double[] getInitialValues() {
		return this.initialValues;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.RichDESystem#getIntermediateIds()
	 */
	public String[] getAdditionalValueIds() {
		String ids[] = new String[v.length];
		int i = 0;
		for (Reaction r : model.getListOfReactions()) {
			ids[i++] = r.getId();
		}
		return ids;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.RichDESystem#getIntermediates(double, double[])
	 */
	public double[] getAdditionalValues(double t, double[] Y)
			throws IntegrationException {
		if ((t - currentTime > 1E-15)
				|| ((Y != this.Y) && !Arrays.equals(Y, this.Y))) {
			/*
			 * We have to compute the system for the given state. But we are not
			 * interested in the rates of change, but only in the reaction
			 * velocities. Therefore, we throw away the results into a senseless
			 * array.
			 */
			getValue(t, Y);
		}
		return v;
	}

	/**
	 * Returns the model that is used by this object.
	 * 
	 * @return Returns the model that is used by this object.
	 */
	public Model getModel() {
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getNegativeInfinity()
	 */
	public ASTNodeValue getNegativeInfinity() {
		return new ASTNodeValue(Double.NEGATIVE_INFINITY, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.EventDESystem#getNumEvents()
	 */
	public int getNumEvents() {
		return model.getNumEvents();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.RichDESystem#getNumIntermediates()
	 */
	public int getNumAdditionalValues() {
		return v.length;
	}

	/**
	 * This method tells you the complete number of parameters within the model.
	 * It counts the global model parameters and all local parameters
	 * (parameters within a kinetic law).
	 * 
	 * @return The total number of model parameters. Note that this number is
	 *         limited to an <code>int</code> value, whereas the SBML model may
	 *         contain <code>int</code> values.
	 */
	public int getNumParameters() {
		int p = (int) model.getNumParameters();
		for (int i = 0; i < model.getNumReactions(); i++) {
			KineticLaw k = model.getReaction(i).getKineticLaw();
			if (k != null) {
				p += k.getNumParameters();
			}
		}
		return p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.EventDESystem#getNumRules()
	 */
	public int getNumRules() {
		return model.getNumRules();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.EventDESystem#getPositionOfParameters()
	 */
	public int getPositionOfParameters() {
		return model.getNumCompartments() + model.getNumSpecies() - 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#getPositiveInfinity()
	 */
	public ASTNodeValue getPositiveInfinity() {
		return new ASTNodeValue(Double.POSITIVE_INFINITY, this);
	}

	/**
	 * Returns the timepoint where the simulation is currently situated
	 * 
	 * @return
	 */
	public double getTime() {
		return currentTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.DESystem#getValue(double, double[])
	 */
	public double[] getValue(double time, double[] Y)
			throws IntegrationException {
		// create a new array with the same size of Y where the rate of change
		// is stored for every symbol in the simulation
		double changeRate[] = new double[Y.length];
		getValue(time, Y, changeRate);
		return changeRate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.DESystem#getValue(double, double[], double[])
	 */
	public void getValue(double time, double[] Y, double[] changeRate)
			throws IntegrationException {
		this.currentTime = time;
		this.Y = Y;

		if (model.getNumEvents() > 0) {
			this.runningEvents.clear();
		}

		// make sure not to have invalid older values in the change rate
		Arrays.fill(changeRate, 0d);

		try {
			/*
			 * Compute changes due to reactions
			 */
			processVelocities(changeRate);

			/*
			 * Compute changes due to rules
			 */
			processRules(changeRate);

			/*
			 * Check the model's constraints
			 */
			for (int i = 0; i < (int) model.getNumConstraints(); i++) {
				if (model.getConstraint(i).getMath().compile(this).toBoolean()) {
					listOfContraintsViolations[i].add(Double.valueOf(time));
				}
			}

		} catch (SBMLException exc) {
			throw new IntegrationException(exc);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#greaterThan(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue gt(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(left.compile(this).toDouble() > right.compile(
				this).toDouble(), this);
	}

	/**
	 * <p>
	 * This method initializes the differential equation system for simulation.
	 * In more detail: the initial amounts or concentration will be assigned to
	 * every species or initialAssignments if any are executed.
	 * </p>
	 * <p>
	 * To save computation time the results of this method should be stored in
	 * an array. Hence this method must only be called once. However, if the
	 * SBML model to be simulated contains initial assignments, this can lead to
	 * wrong simulation results because initial assignments may depend on
	 * current parameter values.
	 * </p>
	 * 
	 * @return An array containing the initial values of this model. Note that
	 *         this is not necessarily equal to the initial values stored in the
	 *         SBML file as this method also evaluates initial assignments if
	 *         there are any.
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 */
	@SuppressWarnings("unchecked")
	public void init() throws ModelOverdeterminedException, SBMLException {
		int i;
		symbolHash = new HashMap<String, Integer>();
		compartmentHash = new HashMap<String, Integer>();
		Integer compartmentIndex, yIndex = 0;
		currentTime = 0d;

		this.stoichiometricCoefHash = new HashMap<String, Double>();
		this.Y = new double[model.getNumCompartments() + model.getNumSpecies()
				+ model.getNumParameters()];
		this.symbolIdentifiers = new String[Y.length];

		/*
		 * Save starting values of the model's compartment in Y
		 */
		for (i = 0; i < model.getNumCompartments(); i++) {
			Compartment c = model.getCompartment(i);

			if (Double.isNaN(c.getSize())) {
				Y[yIndex] = 0;
			} else {
				Y[yIndex] = c.getSize();
			}

			symbolHash.put(c.getId(), yIndex);
			symbolIdentifiers[yIndex] = c.getId();
			yIndex++;
		}

		// Due to unset initial amount or concentration of species try to set
		// one of them
		Species majority = determineMajorSpeciesAttributes();

		/*
		 * Save starting values of the model's species in Y and link them with
		 * their compartment
		 */
		for (i = 0; i < model.getNumSpecies(); i++) {
			Species s = model.getSpecies(i);
			compartmentIndex = symbolHash.get(s.getCompartment());

			// Set initial amount or concentration when not already done
			if (!s.isSetInitialAmount() && !s.isSetInitialConcentration()) {
				if (majority.isSetInitialAmount()) {
					s.setInitialAmount(0d);
				} else {
					s.setInitialConcentration(0d);
				}

				s.setHasOnlySubstanceUnits(majority.getHasOnlySubstanceUnits());
			}

			if (s.isSetInitialAmount()) {
				Y[yIndex] = s.getInitialAmount();
			} else {
				Y[yIndex] = s.getInitialConcentration();
			}
			symbolHash.put(s.getId(), yIndex);
			compartmentHash.put(s.getId(), compartmentIndex);
			symbolIdentifiers[yIndex] = s.getId();
			yIndex++;
		}

		/*
		 * Save starting values of the model's parameter in Y
		 */
		for (i = 0; i < model.getNumParameters(); i++) {
			Parameter p = model.getParameter(i);

			Y[yIndex] = p.getValue();
			symbolHash.put(p.getId(), yIndex);
			symbolIdentifiers[yIndex] = p.getId();
			yIndex++;
		}

		/*
		 * Initial assignments
		 */
		processInitialAssignments();

		/*
		 * Evaluate Constraints
		 */
		if (model.getNumConstraints() > 0) {
			this.listOfContraintsViolations = (List<Double>[]) new LinkedList<?>[(int) model
					.getNumConstraints()];
			for (i = 0; i < (int) model.getNumConstraints(); i++) {
				if (listOfContraintsViolations[i] == null) {
					this.listOfContraintsViolations[i] = new LinkedList<Double>();
				}
				if (model.getConstraint(i).getMath().compile(this).toBoolean()) {
					this.listOfContraintsViolations[i].add(Double.valueOf(0d));
				}
			}
		}

		/*
		 * Initialize Events
		 */
		if (model.getNumEvents() > 0) {
			// this.events = new ArrayList<EventWithPriority>();
			this.events = new EventInProcess[model.getNumEvents()];
			this.runningEvents = new LinkedList<Integer>();
			this.delayedEvents = new LinkedList<Integer>();
			initEvents();
		}

		/*
		 * Algebraic Rules
		 */
		for (i = 0; i < (int) model.getNumRules(); i++) {
			if (model.getRule(i).isAlgebraic()) {
				evaluateAlgebraicRule();
				break;
			}
		}

		/*
		 * Check for fast reactions & update math of kinetic law to avoid wrong
		 * links concerning local parameters
		 */
		for (i = 0; i < model.getNumReactions(); i++) {
			if (model.getReaction(i).isFast() && !hasFastReactions) {
				hasFastReactions = true;
			}
			if (model.getReaction(i).getKineticLaw() != null) {
				if (model.getReaction(i).getKineticLaw()
						.getListOfLocalParameters().size() > 0) {
					model.getReaction(i).getKineticLaw().getMath()
							.updateVariables();
				}
			}

		}

		/*
		 * All other rules
		 */
		processRules(Y);

		/*
		 * Process initial assignments a 2nd time because there can be rules
		 * dependent on initial assignments and vice versa, so one of both has
		 * to be evaluated twice at the start
		 */
		processInitialAssignments();

		// save the initial values of this system
		initialValues = new double[Y.length];
		System.arraycopy(Y, 0, initialValues, 0, initialValues.length);

	}

	/**
	 * Initializes the events of the given model. An Event that triggers at t =
	 * 0 must not fire. Only when it triggers at t > 0
	 * 
	 * @throws SBMLException
	 */
	private void initEvents() throws SBMLException {
		for (int i = 0; i < model.getNumEvents(); i++) {

			if (model.getEvent(i).getDelay() == null) {
				events[i] = new EventInProcess(model.getEvent(i).getTrigger()
						.getInitialValue());
			} else {
				events[i] = new EventInProcessWithDelay(model.getEvent(i)
						.getTrigger().getInitialValue());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#lambda(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue lambda(List<ASTNode> nodes) throws SBMLException {
		double d[] = new double[Math.max(0, nodes.size() - 1)];
		for (int i = 0; i < nodes.size() - 1; i++) {
			d[i++] = nodes.get(i).compile(this).toDouble();
		}
		// TODO: what happens with d?
		ASTNodeValue function = nodes.get(nodes.size() - 1).compile(this);
		return function;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#lessEqual(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue leq(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(left.compile(this).toDouble() <= right.compile(
				this).toDouble(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#ln(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue ln(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.ln(node.compile(this).toDouble()), this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#log(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue log(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.log(node.compile(this).toDouble()), this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#log(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue log(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(Maths.log(left.compile(this).toDouble(), right
				.compile(this).toDouble()), this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#lessThan(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue lt(ASTNode nodeleft, ASTNode noderight)
			throws SBMLException {
		return new ASTNodeValue(nodeleft.compile(this).toDouble() < noderight
				.compile(this).toDouble(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#minus(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue minus(List<ASTNode> nodes) throws SBMLException {
		double value = 0d;
		if (nodes.size() > 0) {
			value = nodes.get(0).compile(this).toDouble();
		}
		for (int i = 1; i < nodes.size(); i++) {
			value -= nodes.get(i).compile(this).toDouble();
		}
		return new ASTNodeValue(value, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#notEqual(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue neq(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(left.compile(this).toDouble() != right.compile(
				this).toDouble(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#not(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue not(ASTNode node) throws SBMLException {
		return node.compile(this).toBoolean() ? getConstantFalse()
				: getConstantTrue();
	}

	// /**
	// *
	// * @param lambda
	// * @param names
	// * @param d
	// * @return
	// */
	// private ASTNode replace(ASTNode lambda, Hashtable<String, Double>
	// args) {
	// String name;
	// for (ASTNode child : lambda.getListOfNodes())
	// if (child.isName() && args.containsKey(child.getName())) {
	// name = child.getName();
	// child.setType(ASTNode.Type.REAL);
	// child.setValue(args.get(name));
	// } else if (child.getNumChildren() > 0)
	// child = replace(child, args);
	// return lambda;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#or(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue or(List<ASTNode> nodes) throws SBMLException {
		for (ASTNode node : nodes) {
			if (node.compile(this).toBoolean()) {
				return getConstantTrue();
			}
		}
		return getConstantFalse();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#piecewise(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue piecewise(List<ASTNode> nodes) throws SBMLException {
		int i;
		for (i = 1; i < nodes.size() - 1; i += 2) {
			if (nodes.get(i).compile(this).toBoolean()) {
				return new ASTNodeValue(nodes.get(i - 1).compile(this)
						.toDouble(), this);
			}
		}
		return new ASTNodeValue(nodes.get(i - 1).compile(this).toDouble(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#plus(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue plus(List<ASTNode> nodes) throws SBMLException {
		double value = 0d;
		for (ASTNode node : nodes) {
			value += node.compile(this).toDouble();
		}
		return new ASTNodeValue(value, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#pow(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue pow(ASTNode left, ASTNode right) throws SBMLException {
		return new ASTNodeValue(Math.pow(left.compile(this).toDouble(), right
				.compile(this).toDouble()), this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.EventDESystem#processAssignmentRules(double,
	 * double[], double[])
	 */
	public ArrayList<DESAssignment> processAssignmentRules(double t, double Y[])
			throws IntegrationException {
		ArrayList<DESAssignment> assignmentRules = new ArrayList<DESAssignment>();
		Integer symbolIndex;

		try {
			for (int i = 0; i < model.getNumRules(); i++) {
				Rule rule = model.getRule(i);
				if (rule.isAssignment()) {
					AssignmentRule as = (AssignmentRule) rule;
					symbolIndex = symbolHash.get(as.getVariable());
					if (symbolIndex != null) {
						assignmentRules.add(new DESAssignment(t, symbolIndex,
								processAssignmentVaribale(as.getVariable(), as
										.getMath())));
					} else if (model.findSpeciesReference(as.getVariable()) != null) {
						SpeciesReference sr = model.findSpeciesReference(as
								.getVariable());
						if (sr.getConstant() == false) {
							stoichiometricCoefHash.put(sr.getId(),
									processAssignmentVaribale(as.getVariable(),
											as.getMath()));
						}
					}
				}
			}
			if (algebraicRules != null) {
				for (AssignmentRule as : algebraicRules) {
					symbolIndex = symbolHash.get(as.getVariable());
					if (symbolIndex != null) {
						assignmentRules.add(new DESAssignment(t, symbolIndex,
								processAssignmentVaribale(as.getVariable(), as
										.getMath())));
					} else if (model.findSpeciesReference(as.getVariable()) != null) {
						SpeciesReference sr = model.findSpeciesReference(as
								.getVariable());
						if (sr.getConstant() == false) {
							stoichiometricCoefHash.put(sr.getId(),
									processAssignmentVaribale(as.getVariable(),
											as.getMath()));
						}
					}
				}
			}
		} catch (SBMLException exc) {
			throw new IntegrationException(exc);
		}

		return assignmentRules;
	}

	/**
	 * Processes the variable of an assignment in terms of determining whether
	 * the variable references to a species or not and if so accounts the
	 * compartment in an appropriate way.
	 * 
	 * @param variable
	 * @param math
	 * @return
	 * @throws SBMLException
	 */
	private double processAssignmentVaribale(String variable, ASTNode math)
			throws SBMLException {
		double compartmentValue, result = 0d;
		Species s;
		if (compartmentHash.containsKey(variable)) {
			s = model.getSpecies(variable);
			if (s.isSetInitialAmount() && !s.getHasOnlySubstanceUnits()) {
				compartmentValue = getCompartmentValueOf(s.getId());
				result = math.compile(this).toDouble() * compartmentValue;
			} else if (s.isSetInitialConcentration()
					&& s.getHasOnlySubstanceUnits()) {
				compartmentValue = getCompartmentValueOf(s.getId());
				result = math.compile(this).toDouble() / compartmentValue;
			} else {
				result = math.compile(this).toDouble();
			}

		} else {
			result = math.compile(this).toDouble();
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.simulator.math.odes.EventDESystem#getEventAssignments(double,
	 * double[])
	 */
	public List<DESAssignment> getEventAssignments(double t, double[] Y)
			throws IntegrationException {

		if (model.getNumEvents() == 0) {
			return null;
		}

		// change Y because of different priorites and reevaluation of
		// trigger/priority
		// after the execution of events
		this.Y = Y;
		this.currentTime = t;
		Double priority, execTime = 0d;
		Double triggerTimeValues[];
		Event ev;
		int i = 0, index;
		Boolean persistent, aborted;
		HashSet<Double> priorities = new HashSet<Double>();
		double count = 0;

		try {

			// recheck trigger of events that have fired for this point in time
			// but have not been executed yet
			while (i < runningEvents.size()) {
				index = runningEvents.get(i);
				ev = model.getEvent(index);
				persistent = ev.getTrigger().getPersistent();
				if (!persistent) {
					if (!ev.getTrigger().getMath().compile(this).toBoolean()) {
						runningEvents.remove(i);
						i--;
					}
				}
				i++;
			}

			i = 0;
			// check events that have fired at an earlier point in time but have
			// not been executed yet due to a delay
			while (i < delayedEvents.size()) {
				index = delayedEvents.get(i);
				ev = model.getEvent(index);
				if (events[index].getTime() <= currentTime) {
					aborted = false;
					persistent = ev.getTrigger().getPersistent();
					if (!persistent) {
						if (!ev.getTrigger().getMath().compile(this)
								.toBoolean()) {
							delayedEvents.remove(i);
							events[index].aborted();
							i--;
							aborted = true;
						}
					}

					if (!aborted) {
						if (ev.getPriority() != null) {
							priority = ev.getPriority().getMath().compile(this)
									.toDouble();
							if (!priorities.contains(priority)) {
								count++;
								priorities.add(priority);
							}
							events[index].changePriority(priority);
						}
						runningEvents.add(index);
						delayedEvents.remove(i);
						i--;

					}
				}
				i++;
			}

			// check the trigger of all events in the model
			for (i = 0; i < model.getNumEvents(); i++) {
				ev = model.getEvent(i);
				if (ev.getTrigger().getMath().compile(this).toBoolean()) {
					// event has not fired recently -> can fire
					if (!events[i].getFireStatus()) {
						// event has a delay
						execTime = currentTime;
						if (ev.getDelay() != null) {
							execTime += ev.getDelay().getMath().compile(this)
									.toDouble();
							delayedEvents.add(i);
						} else {
							if (ev.getPriority() != null) {
								priority = ev.getPriority().getMath().compile(
										this).toDouble();

								if (!priorities.contains(priority)) {
									count++;
									priorities.add(priority);
								}
								events[i].changePriority(priority);
							}

							runningEvents.add(i);
						}
						triggerTimeValues = null;
						if (ev.getUseValuesFromTriggerTime()) {
							triggerTimeValues = new Double[ev
									.getNumEventAssignments()];
							// store values from trigger time for later
							// execution
							for (int j = 0; j < ev.getNumEventAssignments(); j++) {
								triggerTimeValues[j] = processAssignmentVaribale(
										ev.getEventAssignment(j).getVariable(),
										ev.getEventAssignment(j).getMath());
							}

						}

						events[i].addValues(triggerTimeValues, execTime);

						events[i].fired();
					}

				}
				// event has fired recently -> can not fire
				else {
					events[i].recovered();
				}

			}

			// there are events to fire
			if (runningEvents.size() > 0) {
				return processEvents(priorities, count);
			}
			// nothing to do
			else {
				return null;
			}

		} catch (SBMLException exc) {
			throw new IntegrationException(exc);
		}

	}

	/**
	 * This method creates assignments from the events currently stored in the
	 * associated HashMap with respect to their priority.
	 * 
	 * 
	 * @param priorities
	 * @return
	 */
	private List<DESAssignment> processEvents(HashSet<Double> priorities,
			double count) throws IntegrationException {
		List<DESAssignment> assignments = new ArrayList<DESAssignment>();
		List<Integer> highOrderEvents, events;
		Integer symbolIndex;
		ASTNode assignment_math;
		Event event;
		Variable variable;
		double newVal, highestPriority;
		Double[] array;
		int index;
		// check if more than one event has a priority set at this point in time
		if (count > 1) {
			highOrderEvents = new LinkedList<Integer>();
			array = priorities.toArray(new Double[priorities.size()]);
			Arrays.sort(array);
			highestPriority = array[array.length - 1];
			// get event with the current highest priority
			for (int i = 0; i < this.runningEvents.size(); i++) {
				if (this.events[i].getPriority() == highestPriority) {
					highOrderEvents.add(i);
				}
			}
			// pick one event randomly, as a matter of fact remove all event
			// except the picked one
			if (highOrderEvents.size() > 1) {
				pickRandomEvent(highOrderEvents);
			}
			events = highOrderEvents;

		} else {
			events = this.runningEvents;
		}
		try {

			// execute the events chosen for execution
			while (events.size() > 0) {
				index = events.get(0);
				event = model.getEvent(index);
				// event does not use values from trigger time
				if (!event.getUseValuesFromTriggerTime()) {
					for (int j = 0; j < event.getNumEventAssignments(); j++) {
						assignment_math = event.getEventAssignment(j).getMath();
						variable = event.getEventAssignment(j)
								.getVariableInstance();

						if (model.findSpeciesReference(variable.getId()) != null) {
							String id = variable.getId();
							SpeciesReference sr = model
									.findSpeciesReference(id);
							newVal = assignment_math.compile(this).toDouble();

							if (sr.getConstant() == false) {
								stoichiometricCoefHash.put(id, newVal);
							}

						} else {
							symbolIndex = symbolHash.get(variable.getId());
							newVal = processAssignmentVaribale(
									variable.getId(), assignment_math);

							assignments.add(new DESAssignment(currentTime,
									symbolIndex, newVal));
						}

					}
				} else {
					// event uses values from trigger time -> get stored values
					// from the HashMap
					Double[] triggerTimeValues = this.events[index].getValues();

					for (int j = 0; j < event.getNumEventAssignments(); j++) {
						assignment_math = event.getEventAssignment(j).getMath();
						variable = event.getEventAssignment(j)
								.getVariableInstance();
						newVal = triggerTimeValues[j];

						if (model.findSpeciesReference(variable.getId()) != null) {
							String id = variable.getId();
							SpeciesReference sr = model
									.findSpeciesReference(id);
							if (sr.getConstant() == false) {
								stoichiometricCoefHash.put(id, newVal);
							}
						} else {
							symbolIndex = symbolHash.get(variable.getId());
							assignments.add(new DESAssignment(currentTime,
									symbolIndex, newVal));
						}
					}
				}
				this.events[index].executed();
				this.runningEvents.remove(0);

			}

		} catch (SBMLException exc) {
			throw new IntegrationException(exc);
		}

		return assignments;
	}

	private void pickRandomEvent(List<Integer> highOrderEvents) {
		int length = highOrderEvents.size();
		int random = RNG.randomInt(0, length - 1);
		Integer winner = highOrderEvents.get(random);

		highOrderEvents.clear();
		highOrderEvents.add(winner);

	}

	/**
	 * Processes the initial assignments of the model
	 * 
	 * @throws SBMLException
	 */
	private void processInitialAssignments() throws SBMLException {
		for (int i = 0; i < model.getNumInitialAssignments(); i++) {
			InitialAssignment iA = model.getInitialAssignment(i);
			Integer index = null;
			if (iA.isSetMath() && iA.isSetVariable()) {
				if (model.getSpecies(iA.getVariable()) != null) {
					Species s = model.getSpecies(iA.getVariable());
					double compartmentValue;
					String id = s.getId();
					index = symbolHash.get(id);

					if (compartmentHash.containsKey(id)) {
						if (s.isSetInitialAmount()
								&& !s.getHasOnlySubstanceUnits()) {
							compartmentValue = getCompartmentValueOf(id);
							this.Y[index] = iA.getMath().compile(this)
									.toDouble()
									* compartmentValue;
						} else if (s.isSetInitialConcentration()
								&& s.getHasOnlySubstanceUnits()) {
							compartmentValue = getCompartmentValueOf(id);
							this.Y[index] = iA.getMath().compile(this)
									.toDouble()
									/ compartmentValue;
						} else {
							this.Y[index] = iA.getMath().compile(this)
									.toDouble();
						}

					} else {
						this.Y[index] = iA.getMath().compile(this).toDouble();
					}

				} else if (model.getCompartment(iA.getVariable()) != null) {
					Compartment c = model.getCompartment(iA.getVariable());
					index = symbolHash.get(c.getId());
					this.Y[index] = iA.getMath().compile(this).toDouble();
				} else if (model.getParameter(iA.getVariable()) != null) {
					Parameter p = model.getParameter(iA.getVariable());
					index = symbolHash.get(p.getId());
					this.Y[index] = iA.getMath().compile(this).toDouble();
				} else if (model.findSpeciesReference(iA.getVariable()) != null) {
					SpeciesReference sr = model.findSpeciesReference(iA
							.getVariable());
					stoichiometricCoefHash.put(sr.getId(), iA.getMath()
							.compile(this).toDouble());
				} else {
					System.err
							.println("The model contains an initial assignment for a component other than species, compartment, parameter or species reference.");
				}
			}
		}
	}

	/**
	 * Due to missing information about the attributes of species set by initial
	 * Assignments, a majorty vote of all other species is performed to
	 * determine the attributes.
	 * 
	 * @return
	 */
	private Species determineMajorSpeciesAttributes() {
		Species majority = new Species(model.getLevel(), model.getVersion());
		int concentration = 0, amount = 0, substanceUnits = 0;

		for (Species species : model.getListOfSpecies()) {
			if (species.isSetInitialAmount()) {
				amount++;
			} else if (species.isSetInitialConcentration()) {
				concentration++;
			}
			if (species.hasOnlySubstanceUnits()) {
				substanceUnits++;
			}
		}
		if (amount >= concentration) {
			majority.setInitialAmount(0.0);
		} else {
			majority.setInitialConcentration(0.0);
		}

		if (substanceUnits > (model.getNumSpecies() - substanceUnits)) {
			majority.setHasOnlySubstanceUnits(true);
		} else {
			majority.setHasOnlySubstanceUnits(false);
		}
		return majority;

	}

	/**
	 * @param changeRate
	 * @throws SBMLException
	 */
	private void processRules(double[] changeRate) throws SBMLException {
		// evaluation of assignment rules through the DESystem itself
		// only at time point 0d, at time points >=0d the solver carries on
		// with this task. Assignment rules are only processed during
		// initialization in this class. During this process the passed array
		// changerate is not the actual changerate but the Y vector since there
		// is no change at timepoint zero

		for (int i = 0; i < model.getNumRules(); i++) {
			Rule rule = model.getRule(i);
			if (rule.isRate() && currentTime > 0d) {
				RateRule rr = (RateRule) rule;
				evaluateRateRule(rr, changeRate);
			} else if (rule.isAssignment() && currentTime == 0d) {
				AssignmentRule as = (AssignmentRule) rule;
				evaluateAssignmentRule(as, changeRate);
			} else /* if (rule.isScalar()) */{
				// a rule is scalar if it is an assignment rule.
			}
		}
		// process list of algebraic rules
		if (algebraicRules != null && currentTime == 0d) {
			for (AssignmentRule as : algebraicRules) {
				evaluateAssignmentRule(as, changeRate);
			}

		}

	}

	/**
	 * This method computes the multiplication of the stoichiometric matrix of
	 * the given model system with the reaction velocities vector passed to this
	 * method. Note, the stoichiometric matrix is only constructed implicitely
	 * by running over all reactions and considering all participating reactants
	 * and products with their according stoichiometry or stoichiometric math.
	 * 
	 * @param velocities
	 *            An array of reaction velocities at the current time.
	 * @param Y
	 * @return An array containing the rates of change for each species in the
	 *         model system of this class.
	 * @throws SBMLException
	 */
	protected void processVelocities(double[] changeRate) throws SBMLException {
		int reactionIndex, sReferenceIndex, speciesIndex;
		Species species;
		SpeciesReference speciesRef;
		HashSet<String> inConcentration = new HashSet<String>();

		// Velocities of each reaction.
		if (hasFastReactions) {
			for (int i = 0; i < v.length; i++) {
				currentReaction = model.getReaction(i);
				KineticLaw kin = currentReaction.getKineticLaw();
				if (kin != null
						&& isProcessingFastReactions == currentReaction
								.isFast()) {
					v[i] = kin.getMath().compile(this).toDouble();
				} else {
					v[i] = 0;
				}
			}
		}

		else {
			for (int i = 0; i < v.length; i++) {
				currentReaction = model.getReaction(i);
				KineticLaw kin = currentReaction.getKineticLaw();
				if (kin != null) {
					v[i] = kin.getMath().compile(this).toDouble();
				} else {
					v[i] = 0;
				}
			}
		}

		for (reactionIndex = 0; reactionIndex < model.getNumReactions(); reactionIndex++) {
			Reaction r = model.getReaction(reactionIndex);
			for (sReferenceIndex = 0; sReferenceIndex < r.getNumReactants(); sReferenceIndex++) {
				speciesRef = r.getReactant(sReferenceIndex);
				species = speciesRef.getSpeciesInstance();

				if (!species.getBoundaryCondition() && !species.getConstant()) {
					speciesIndex = symbolHash.get(species.getId());
					if ((speciesRef.getLevel() >= 3)
							&& (speciesRef.getId() != null)
							&& this.stoichiometricCoefHash
									.containsKey(speciesRef.getId())) {
						changeRate[speciesIndex] -= this.stoichiometricCoefHash
								.get(speciesRef.getId())
								* v[reactionIndex];
					} else if (speciesRef.isSetStoichiometryMath()) {
						changeRate[speciesIndex] -= speciesRef
								.getStoichiometryMath().getMath().compile(this)
								.toDouble()
								* v[reactionIndex];
					} else {
						changeRate[speciesIndex] -= speciesRef
								.getStoichiometry()
								* v[reactionIndex];
					}
					if (species.isSetInitialConcentration()
							&& !species.getHasOnlySubstanceUnits()) {
						inConcentration.add(species.getId());
					}
				}
			}

			for (sReferenceIndex = 0; sReferenceIndex < r.getNumProducts(); sReferenceIndex++) {
				speciesRef = r.getProduct(sReferenceIndex);
				species = speciesRef.getSpeciesInstance();

				if (!species.getBoundaryCondition() && !species.getConstant()) {
					speciesIndex = symbolHash.get(species.getId());

					if (speciesRef.getLevel() >= 3
							&& speciesRef.getId() != null
							&& this.stoichiometricCoefHash
									.containsKey(speciesRef.getId())) {
						changeRate[speciesIndex] += this.stoichiometricCoefHash
								.get(speciesRef.getId())
								* v[reactionIndex];
					} else if (speciesRef.isSetStoichiometryMath()) {
						changeRate[speciesIndex] += speciesRef
								.getStoichiometryMath().getMath().compile(this)
								.toDouble()
								* v[reactionIndex];
					} else {
						changeRate[speciesIndex] += speciesRef
								.getStoichiometry()
								* v[reactionIndex];
					}

					if (species.isSetInitialConcentration()
							&& !species.getHasOnlySubstanceUnits()) {
						inConcentration.add(species.getId());
					}
				}

			}
		}

		// When the unit of reacting specie is given mol/volume
		// then it has to be considered in the change rate that should
		// always be only in mol/time

		for (String s : inConcentration) {
			speciesIndex = symbolHash.get(s);
			changeRate[speciesIndex] = changeRate[speciesIndex]
					/ getCompartmentValueOf(s);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#root(org.sbml.jsbml.ASTNode,
	 * org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue root(ASTNode rootExponent, ASTNode radiant)
			throws SBMLException {
		return root(rootExponent.compile(this).toDouble(), radiant);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#root(double, org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue root(double rootExponent, ASTNode radiant)
			throws SBMLException {
		return new ASTNodeValue(Maths.root(radiant.compile(this).toDouble(),
				rootExponent), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#sec(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue sec(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.sec(node.compile(this).toDouble()), this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#sech(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue sech(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Maths.sech(node.compile(this).toDouble()), this);

	}

	/**
	 * This method allows to set the parameters of the model to the specified
	 * values in the given array.
	 * 
	 * @param params
	 *            An array of parameter values to be set for this model. If the
	 *            number of given parameters does not match the number of model
	 *            parameters, an exception will be thrown.
	 */
	// TODO changing the model directly not allowed / does this method still
	// make sense?
	public void setParameters(double[] params) {
		// TODO consider local parameters as well.
		// if (params.length != model.getNumParameters())
		// throw new IllegalArgumentException(
		// "The number of parameters passed to this method must "
		// + "match the number of parameters in the model.");
		int paramNum, reactionNum, localPnum;
		for (paramNum = 0; paramNum < model.getNumParameters(); paramNum++)
			model.getParameter(paramNum).setValue(params[paramNum]);
		for (reactionNum = 0; reactionNum < model.getNumReactions(); reactionNum++) {
			KineticLaw law = model.getReaction(reactionNum).getKineticLaw();
			for (localPnum = 0; localPnum < law.getNumParameters(); localPnum++)
				law.getParameter(localPnum).setValue(params[paramNum++]);
		}
		if (model.getNumInitialAssignments() > 0 || model.getNumEvents() > 0)
			try {
				init();
			} catch (Exception e) {
				// This can never happen
			}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#sin(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue sin(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.sin(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#sinh(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue sinh(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.sinh(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#sqrt(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue sqrt(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.sqrt(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#symbolTime(java.lang.String)
	 */
	public ASTNodeValue symbolTime(String timeSymbol) {
		return new ASTNodeValue(getTime(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#tan(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue tan(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.tan(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#tanh(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue tanh(ASTNode node) throws SBMLException {
		return new ASTNodeValue(Math.tanh(node.compile(this).toDouble()), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#times(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue times(List<ASTNode> nodes) throws SBMLException {
		if (nodes.size() == 0) {
			return new ASTNodeValue(0d, this);
		}
		double value = 1d;
		for (ASTNode node : nodes) {
			value *= node.compile(this).toDouble();
		}
		return new ASTNodeValue(value, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#toString(org.sbml.jsbml.ASTNode)
	 */
	public String toString(ASTNode value) {
		return value.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#uiMinus(org.sbml.jsbml.ASTNode)
	 */
	public ASTNodeValue uMinus(ASTNode node) throws SBMLException {
		return new ASTNodeValue(-node.compile(this).toDouble(), this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#unknownASTNode()
	 */
	public ASTNodeValue unknownValue() {
		return new ASTNodeValue(Double.NaN, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.jsbml.ASTNodeCompiler#xor(org.sbml.jsbml.ASTNode[])
	 */
	public ASTNodeValue xor(List<ASTNode> nodes) throws SBMLException {
		boolean value = false;
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).compile(this).toBoolean()) {
				if (value) {
					return getConstantFalse();
				} else {
					value = true;
				}
			}
		}
		return new ASTNodeValue(value, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sbml.simulator.math.odes.FastProcessDESystem#containsFastProcesses()
	 */
	public boolean containsFastProcesses() {
		return hasFastReactions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.math.odes.FastProcessDESystem#setFastProcessComputation(boolean)
	 */
	public void setFastProcessComputation(boolean isProcessing) {
		isProcessingFastReactions = isProcessing;
	}
	
	@Override
	public int getDimension() {
		return this.getDESystemDimension();
	}

	@Override
	public void computeDerivatives(double t, double[] y, double[] yDot)
			throws DerivativeException {
		try {
			System.arraycopy(this.getValue(t, y),0, yDot, 0, yDot.length);
		} catch (IntegrationException e) {
			e.printStackTrace();
		}
			
	}

}
