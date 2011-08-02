package org.sbml.simulator.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.util.filters.CVTermFilter;

public class ModelFilter {
  
  /**
   * 
   * @param modelFile
   * @param filterFile
   * @param col
   * @param containsHeader
   * @param outputFile
   * @throws XMLStreamException
   * @throws IOException
   * @throws SBMLException
   */
  public static void filterModel(String modelFile, String filterFile, int col,
    boolean containsHeader, String outputFile) throws XMLStreamException,
    IOException, SBMLException {
    SBMLDocument doc = (new SBMLReader()).readSBML(modelFile);
    
    BufferedReader reader = new BufferedReader(new FileReader(filterFile));
    
    List<String> stringList = new LinkedList<String>();
    try {
      String line = null;
      if (containsHeader) {
        line = reader.readLine();
      }
      while ((line = reader.readLine()) != null) {
        String[] splits = line.split("\t");
        if (splits.length > col) {
          stringList.add(splits[col]);
        }
      }
      reader.close();
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    //search model for species
    Set<String> speciesSet = new HashSet<String>();
    readSpecies(speciesSet, stringList, doc);
    
    //add species and their compartments
    SBMLDocument newDoc = createFilteredSBMLDocument(speciesSet, doc);
    
    Map<Species, Set<Reaction>> speciesToReaction = new HashMap<Species, Set<Reaction>>();
    Map<Reaction, Set<Species>> reactionToSpecies = new HashMap<Reaction, Set<Species>>();
    createMaps(speciesToReaction, reactionToSpecies, doc);
    
    //add reactions and their contained species
    Set<Species> speciesToAdd = new HashSet<Species>();
    Set<Reaction> reactionsToAdd = new HashSet<Reaction>();
    
    determineReactionsAndSpeciesToAdd(speciesToAdd, reactionsToAdd, speciesSet,
      speciesToReaction, reactionToSpecies);
    
    for (Species sp : speciesToAdd) {
      if (newDoc.getModel().getSpecies(sp.getId()) == null) {
        newDoc.getModel().addSpecies(sp);
      }
    }
    
    for (Reaction r : reactionsToAdd) {
      newDoc.getModel().addReaction(r);
    }
    
    //TODO other SBML elements
    
    SBMLWriter w = new SBMLWriter();
    w.write(newDoc, outputFile);
    System.out.println(doc.getModel().getNumSpecies() + " "
        + doc.getModel().getNumReactions());
    System.out.println(newDoc.getModel().getNumSpecies() + " "
        + newDoc.getModel().getNumReactions());
  }
  
  private static void determineReactionsAndSpeciesToAdd(
    Set<Species> speciesToAdd, Set<Reaction> reactionsToAdd,
    Set<String> speciesSet, Map<Species, Set<Reaction>> speciesToReaction,
    Map<Reaction, Set<Species>> reactionToSpecies) {
    for (Reaction r : reactionToSpecies.keySet()) {
      boolean productContained = false;
      boolean reactantContained = false;
      
      for (int j = 0; j != r.getNumReactants(); j++) {
        String id = r.getReactant(j).getSpecies();
        if (speciesSet.contains(id)) {
          reactantContained = true;
          break;
        }
      }
      
      for (int j = 0; j != r.getNumProducts(); j++) {
        String id = r.getProduct(j).getSpecies();
        if (speciesSet.contains(id)) {
          productContained = true;
          break;
        }
      }
      
      if (reactantContained && productContained) {
        speciesToAdd.addAll(reactionToSpecies.get(r));
        reactionsToAdd.add(r);
      } else if (reactantContained) {
        addConnections(r, speciesToAdd, reactionsToAdd, speciesToReaction,
          reactionToSpecies, speciesSet);
        
      }
      
    }
    
  }
  
  private static void addConnections(Reaction r, Set<Species> speciesToAdd,
    Set<Reaction> reactionsToAdd,
    Map<Species, Set<Reaction>> speciesToReaction,
    Map<Reaction, Set<Species>> reactionToSpecies, Set<String> speciesSet) {
    for (int j = 0; j != r.getNumProducts(); j++) {
      
      Species sp = r.getProduct(j).getSpeciesInstance();
      for (Reaction r2 : speciesToReaction.get(sp)) {
        for (Species sp2 : reactionToSpecies.get(r2)) {
          if ((r2.hasProduct(sp2)) && (speciesSet.contains(sp2.getId()))) {
            speciesToAdd.addAll(reactionToSpecies.get(r));
            speciesToAdd.addAll(reactionToSpecies.get(r2));
            reactionsToAdd.add(r);
            reactionsToAdd.add(r2);
          }
        }
      }
    }
    
  }
  
  private static void createMaps(Map<Species, Set<Reaction>> speciesToReaction,
    Map<Reaction, Set<Species>> reactionToSpecies, SBMLDocument doc) {
    for (int i = 0; i != doc.getModel().getNumReactions(); i++) {
      Reaction r = doc.getModel().getReaction(i);
      Set<Species> species = new HashSet<Species>();
      
      for (int j = 0; j != r.getNumReactants(); j++) {
        Species sp = r.getReactant(j).getSpeciesInstance();
        species.add(sp);
        Set<Reaction> reactions = speciesToReaction.get(species);
        if (reactions == null) {
          reactions = new HashSet<Reaction>();
        }
        reactions.add(r);
        speciesToReaction.put(sp, reactions);
      }
      for (int j = 0; j != r.getNumProducts(); j++) {
        Species sp = r.getProduct(j).getSpeciesInstance();
        species.add(sp);
        Set<Reaction> reactions = speciesToReaction.get(species);
        if (reactions == null) {
          reactions = new HashSet<Reaction>();
        }
        reactions.add(r);
        speciesToReaction.put(sp, reactions);
      }
      for (int j = 0; j != r.getNumModifiers(); j++) {
        Species sp = r.getModifier(j).getSpeciesInstance();
        species.add(sp);
        Set<Reaction> reactions = speciesToReaction.get(species);
        if (reactions == null) {
          reactions = new HashSet<Reaction>();
        }
        reactions.add(r);
        speciesToReaction.put(sp, reactions);
      }
      reactionToSpecies.put(r, species);
      
    }
    
  }
  
  private static SBMLDocument createFilteredSBMLDocument(
    Set<String> speciesSet, SBMLDocument doc) {
    //create new model
    SBMLDocument newDoc = new SBMLDocument(doc.getLevel(), doc.getVersion());
    newDoc.createModel("newModel");
    for (String id : speciesSet) {
      Species sp = doc.getModel().getSpecies(id);
      newDoc.getModel().addSpecies(sp);
      String compartmentId = sp.getCompartment();
      if (!newDoc.getModel().containsCompartment(compartmentId)) {
        newDoc.getModel().addCompartment(
          doc.getModel().getCompartment(compartmentId));
      }
      
    }
    return newDoc;
    
  }
  
  private static void readSpecies(Set<String> speciesSet,
    List<String> stringList, SBMLDocument doc) {
    for (int i = 0; i != doc.getModel().getNumSpecies(); i++) {
      Species sp = doc.getModel().getSpecies(i);
      
      for (String s : stringList) {
        //id,name
        if (sp.getId().equals(s) || sp.getName().equals(s)) {
          speciesSet.add(sp.getId());
          break;
        }
        //annotation
        if ((new CVTermFilter(Qualifier.BQB_IS, s)).accepts(sp)) {
          speciesSet.add(sp.getId());
          break;
        }
      }
    }
  }
  
  public static void main(String[] args) throws XMLStreamException,
    IOException, SBMLException {
    filterModel("files/HepatoNet1_annotated.xml", "files/Hofmann.txt", 1,
      false, "files/filteredModel.xml");
  }
}
