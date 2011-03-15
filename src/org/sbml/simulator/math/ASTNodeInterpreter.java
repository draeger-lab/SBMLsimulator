/*
 * $Id:  ASTNodeInterpreter.java 15:53:40 draeger$
 * $URL: ASTNodeInterpreter.java $
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

import java.util.List;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.CallableSBase;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.util.compilers.ASTNodeCompiler;
import org.sbml.jsbml.util.compilers.ASTNodeValue;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class ASTNodeInterpreter implements ASTNodeCompiler {

    /**
     * 
     */
    private ValueHolder valueHolder;
    
    /**
     * 
     * @param valueHolder
     */
    public ASTNodeInterpreter(ValueHolder valueHolder) {
	this.valueHolder = valueHolder;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#abs(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue abs(ASTNode node) throws SBMLException {
	return new ASTNodeValue(Math.abs(node.compile(this).toDouble()), this);
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#and(java.util.List)
     */
    public ASTNodeValue and(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arccos(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arccos(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arccosh(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arccosh(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arccot(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arccot(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arccoth(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arccoth(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arccsc(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arccsc(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arccsch(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arccsch(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arcsec(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arcsec(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arcsech(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arcsech(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arcsin(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arcsin(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arcsinh(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arcsinh(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arctan(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arctan(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#arctanh(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue arctanh(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#ceiling(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue ceiling(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(org.sbml.jsbml.Compartment)
     */
    public ASTNodeValue compile(Compartment c) {
	return new ASTNodeValue(valueHolder.getCurrentCompartmentSize(c.getId()), this);
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(double, int, java.lang.String)
     */
    public ASTNodeValue compile(double mantissa, int exponent, String units) {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(double, java.lang.String)
     */
    public ASTNodeValue compile(double real, String units) {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(int, java.lang.String)
     */
    public ASTNodeValue compile(int integer, String units) {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(org.sbml.jsbml.CallableSBase)
     */
    public ASTNodeValue compile(CallableSBase variable) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#compile(java.lang.String)
     */
    public ASTNodeValue compile(String name) {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#cos(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue cos(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#cosh(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue cosh(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#cot(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue cot(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#coth(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue coth(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#csc(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue csc(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#csch(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue csch(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#delay(java.lang.String, org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode, java.lang.String)
     */
    public ASTNodeValue delay(String delayName, ASTNode x, ASTNode delay,
	String timeUnits) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#eq(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue eq(ASTNode left, ASTNode right) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#exp(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue exp(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#factorial(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue factorial(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#floor(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue floor(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#frac(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue frac(ASTNode numerator, ASTNode denominator)
	throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#frac(int, int)
     */
    public ASTNodeValue frac(int numerator, int denominator)
	throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#function(org.sbml.jsbml.FunctionDefinition, java.util.List)
     */
    public ASTNodeValue function(FunctionDefinition functionDefinition,
	List<ASTNode> args) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#geq(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue geq(ASTNode left, ASTNode right) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getConstantAvogadro(java.lang.String)
     */
    public ASTNodeValue getConstantAvogadro(String name) {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getConstantE()
     */
    public ASTNodeValue getConstantE() {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getConstantFalse()
     */
    public ASTNodeValue getConstantFalse() {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getConstantPi()
     */
    public ASTNodeValue getConstantPi() {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getConstantTrue()
     */
    public ASTNodeValue getConstantTrue() {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getNegativeInfinity()
     */
    public ASTNodeValue getNegativeInfinity() throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#getPositiveInfinity()
     */
    public ASTNodeValue getPositiveInfinity() {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#gt(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue gt(ASTNode left, ASTNode right) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#lambda(java.util.List)
     */
    public ASTNodeValue lambda(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#leq(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue leq(ASTNode left, ASTNode right) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#ln(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue ln(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#log(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue log(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#log(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue log(ASTNode base, ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#lt(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue lt(ASTNode left, ASTNode right) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#minus(java.util.List)
     */
    public ASTNodeValue minus(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#neq(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue neq(ASTNode left, ASTNode right) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#not(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue not(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#or(java.util.List)
     */
    public ASTNodeValue or(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#piecewise(java.util.List)
     */
    public ASTNodeValue piecewise(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#plus(java.util.List)
     */
    public ASTNodeValue plus(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#pow(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue pow(ASTNode base, ASTNode exponent)
	throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#root(org.sbml.jsbml.ASTNode, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue root(ASTNode rootExponent, ASTNode radiant)
	throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#root(double, org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue root(double rootExponent, ASTNode radiant)
	throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#sec(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue sec(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#sech(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue sech(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#sin(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue sin(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#sinh(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue sinh(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#sqrt(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue sqrt(ASTNode radiant) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#symbolTime(java.lang.String)
     */
    public ASTNodeValue symbolTime(String time) {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#tan(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue tan(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#tanh(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue tanh(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#times(java.util.List)
     */
    public ASTNodeValue times(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#uMinus(org.sbml.jsbml.ASTNode)
     */
    public ASTNodeValue uMinus(ASTNode value) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#unknownValue()
     */
    public ASTNodeValue unknownValue() throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

    /* (non-Javadoc)
     * @see org.sbml.jsbml.util.compilers.ASTNodeCompiler#xor(java.util.List)
     */
    public ASTNodeValue xor(List<ASTNode> values) throws SBMLException {
	// TODO Auto-generated method stub
	return null;
    }

}
