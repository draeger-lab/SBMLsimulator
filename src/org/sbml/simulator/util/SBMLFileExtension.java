package org.sbml.simulator.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

public class SBMLFileExtension{
  public static void extendModel(String inputFile, String outputFile) throws XMLStreamException, SBMLException, IOException {
    SBMLDocument doc = (new SBMLReader()).readSBML(inputFile);
    
    Compartment cytosol = null;
    for(Compartment c: doc.getModel().getListOfCompartments()) {
      if(c.getName().contains("cyto") || c.getName().contains("Cyto")) {
        cytosol=c;
      }
    }
    if(cytosol==null) {
      cytosol = doc.getModel().createCompartment("cyto");
      cytosol.setName("cyto");
    }
    
    Species degraded = doc.getModel().createSpecies("emptySet", cytosol);
    degraded.setName("Degraded");
    degraded.setMetaId(degraded.getId());
    degraded.setSBOTerm(291);
    
    
    List<Species> speciesInModel = new LinkedList<Species>();
    for(Species s: doc.getModel().getListOfSpecies()) {
      speciesInModel.add(s);
    }
    
    int counter=0;
    for(Species s: speciesInModel) {
      if(s.getName().contains("(gene)")) {
        s.setSBOTerm(243);
      }
      else if(s.getName().contains("(RNA)")) {
        s.setSBOTerm(278);
        
        //add cytosol RNA
        Species cytosolRNA = doc.getModel().createSpecies(s.getId() +"_cyto", cytosol);
        cytosolRNA.setName(s.getName());
        cytosolRNA.setMetaId(cytosolRNA.getId());
        cytosolRNA.setSBOTerm(278);
        
        Annotation annotation = s.getAnnotation();
        if(annotation != null) {
          cytosolRNA.setAnnotation(annotation);
        }
        
        //add transport reaction
        Reaction transportReaction = doc.getModel().createReaction("re_Transport" + counter);
        transportReaction.setName(transportReaction.getId());
        transportReaction.addReactant(new SpeciesReference(s));
        transportReaction.addProduct(new SpeciesReference(cytosolRNA));
        transportReaction.setReversible(false);
        transportReaction.setSBOTerm(185);
        
        //add protein
        String proteinName = s.getName();
        proteinName=proteinName.replaceFirst(" \\(RNA\\)","");
        Species protein = doc.getModel().createSpecies(proteinName,cytosol);
        protein.setName(proteinName);
        protein.setMetaId(protein.getId());
        System.out.println(protein);
        
        //add translation
        Reaction translation = doc.getModel().createReaction("re_Translation" + counter);
        translation.setName(translation.getId());
        translation.addReactant(new SpeciesReference(degraded));
        translation.setSBOTerm(184);
        translation.setReversible(false);
        translation.addProduct(new SpeciesReference(protein));
        translation.addModifier(new ModifierSpeciesReference(cytosolRNA));
        
        
        counter++;
      }
    }
    
    SBMLWriter w = new SBMLWriter();
    w.write(doc, outputFile);
  }
}
