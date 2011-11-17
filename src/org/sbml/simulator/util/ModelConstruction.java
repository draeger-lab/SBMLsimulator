package org.sbml.simulator.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;


public class ModelConstruction {
 public static void modelCreation(String outputFile) throws FileNotFoundException, XMLStreamException, SBMLException {
   SBMLDocument doc= new SBMLDocument(2,4);

   Model model = doc.createModel("Testmodel2");
   Compartment comp=model.createCompartment("Compartment");
   Compartment ext=model.createCompartment("External");
   Species a = model.createSpecies("A", comp);
   Species b = model.createSpecies("B", comp);
   Species c = model.createSpecies("C", comp);
   Species d = model.createSpecies("D", comp);
   Species e = model.createSpecies("E", comp);
   Species f = model.createSpecies("F", comp);
   a.setSBOTerm(247);
   b.setSBOTerm(247);
   c.setSBOTerm(247);
   d.setSBOTerm(247);
   e.setSBOTerm(247);
   
   Species aExt = model.createSpecies("Aext", ext);
   Species eExt = model.createSpecies("Eext", ext);
   aExt.setSBOTerm(247);
   eExt.setSBOTerm(247);
   
   Reaction r1=model.createReaction("Reaction1");
   Reaction r2=model.createReaction("Reaction2");
   Reaction r3=model.createReaction("Reaction3");
   Reaction r4=model.createReaction("Reaction4");
   r1.setReversible(true);
   r2.setReversible(true);
   r3.setReversible(true);
   r4.setReversible(true);
   
   r1.addReactant(new SpeciesReference(a));
   r1.addReactant(new SpeciesReference(b));
   r1.addProduct(new SpeciesReference(c));
   
   r2.addReactant(new SpeciesReference(c));
   r2.addReactant(new SpeciesReference(d));
   r2.addProduct(new SpeciesReference(e));
   
   r3.addReactant(new SpeciesReference(b));
   r3.addReactant(new SpeciesReference(e));
   r3.addProduct(new SpeciesReference(f));
   
   r4.addReactant(new SpeciesReference(b));
   r4.addReactant(new SpeciesReference(d));
   r4.addProduct(new SpeciesReference(a));
  
   Reaction r5=model.createReaction("Diffusion1");
   Reaction r6=model.createReaction("Diffusion2");
   r5.setReversible(true);
   r6.setReversible(true);
   
   r5.addReactant(new SpeciesReference(a));
   r5.addProduct(new SpeciesReference(aExt));
   
   r6.addReactant(new SpeciesReference(e));
   r6.addProduct(new SpeciesReference(eExt));
   
   SBMLWriter writer=new SBMLWriter();
   writer.write(doc, outputFile);
   
   
 }
 
 /**
  * Set parameters to random numbers between 0.01 and 1
  * 
  * @param inputFile
  * @param outputFile
  * @throws XMLStreamException
  * @throws IOException
  * @throws SBMLException
  */
 public static void addRandomParameters(String inputFile, String outputFile) throws XMLStreamException, IOException, SBMLException {
   SBMLReader reader = new SBMLReader();
   SBMLDocument doc=reader.readSBML(inputFile);
   
   
   Random random = new Random();
   for(Species s: doc.getModel().getListOfSpecies()) {
     s.setInitialConcentration(random.nextDouble()*0.99+0.01);
   }
 
   for(Compartment c: doc.getModel().getListOfCompartments()) {
     //c.setValue(random.nextDouble()*0.99+0.01);
     c.setValue(1);
   }
 
   for(Parameter p: doc.getModel().getListOfParameters()) {
     p.setValue(random.nextDouble()*0.99+0.01);
   }
   
   for(Reaction r:doc.getModel().getListOfReactions()) {
     KineticLaw kl=r.getKineticLaw();
     if(kl!=null && kl.isSetListOfLocalParameters()) {
       for(LocalParameter lp:kl.getListOfLocalParameters()) {
         lp.setValue(random.nextDouble()*0.99+0.01);
       }
     }
   }
    SBMLWriter writer=new SBMLWriter();
   writer.write(doc, outputFile);
 }
 
 public static void main(String[] args) throws XMLStreamException, SBMLException, IOException {
   modelCreation("files/testModel2.xml");
   //addRandomParameters("files/testModel.xml","files/testModel.xml");
 }
}
