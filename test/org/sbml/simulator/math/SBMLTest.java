/**
 * 
 */
package org.sbml.simulator.math;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.text.parser.ParseException;

/**
 * @author draeger
 */
public class SBMLTest {

    /**
     * 
     * @throws ParseException
     * @throws XMLStreamException
     * @throws SBMLException
     */
    @SuppressWarnings("deprecation")
    public SBMLTest() throws ParseException, XMLStreamException, SBMLException {
	SBMLDocument doc = new SBMLDocument(3, 1);
	Model model = doc.createModel("specRefTest");
	Compartment c = model.createCompartment("default");
	c.setSpatialDimensions(3d);
	c.setSize(1d);
	Species s1 = model.createSpecies("s1", c);
	Species s2 = model.createSpecies("s2", c);
	Reaction r1 = model.createReaction("r1");
	SpeciesReference s1Ref = r1.createReactant("s1Ref", s1);
	SpeciesReference s2Ref = r1.createProduct("s2Ref", s2);
	KineticLaw kl = r1.createKineticLaw();
	s1Ref.setConstant(false);
	s2Ref.setConstant(true);
	AssignmentRule ar = model.createAssignmentRule();
	ar.setVariable(s1Ref);
	kl.setFormula("s1Ref * s1 - 2 * s2");
	ar.setFormula("1.1 * s1Ref");
	(new SBMLWriter()).write(doc, System.out);
    }

    /**
     * @param args
     * @throws SBMLException
     * @throws XMLStreamException
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException,
	XMLStreamException, SBMLException {
	new SBMLTest();
    }

}
