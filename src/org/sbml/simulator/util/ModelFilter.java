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

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.filters.CVTermFilter;

public class ModelFilter {
  
  /**
   * 
   * @param modelFile
   * @param filterFile
   * @param col
   * @param containsHeader
   * @param outputFile
   * @param merge
   * @param strict
   * @throws XMLStreamException
   * @throws IOException
   * @throws SBMLException
   */
  public static void filterModel(String modelFile, String filterFile, int col,
    boolean containsHeader, String smallMoleculesFile, int col2,
    boolean containsHeader2, boolean strict, boolean merge, String outputFile)
    throws XMLStreamException, IOException, SBMLException {
    SBMLDocument doc = (new SBMLReader()).readSBML(modelFile);
    
    //read file with species
    BufferedReader reader = new BufferedReader(new FileReader(filterFile));
    
    List<String> stringList = new LinkedList<String>();
    List<String> stringList2 = new LinkedList<String>();
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
    
    //read file with small molecules
    if (smallMoleculesFile != null) {
      BufferedReader reader2 = new BufferedReader(new FileReader(
        smallMoleculesFile));
      
      try {
        String line = null;
        if (containsHeader2) {
          line = reader2.readLine();
        }
        while ((line = reader2.readLine()) != null) {
          String[] splits = line.split("\t");
          if (splits.length > col2) {
            stringList2.add(splits[col2]);
          }
        }
        reader2.close();
        
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    //search model for species
    Set<String> speciesSet = new HashSet<String>();
    readSpecies(speciesSet, stringList, doc);
    
    Set<String> smallMoleculesSet = new HashSet<String>();
    readSpecies(smallMoleculesSet, stringList2, doc);
    
    //create hash maps
    Map<Species, Set<Reaction>> speciesToReaction = new HashMap<Species, Set<Reaction>>();
    Map<Reaction, Set<Species>> reactionToSpecies = new HashMap<Reaction, Set<Species>>();
    createMaps(speciesToReaction, reactionToSpecies, doc);
    
    //determine reactions and species to add to the filtered model
    Set<Species> speciesToAdd = new HashSet<Species>();
    Set<Reaction> reactionsToAdd = new HashSet<Reaction>();
    
    Set<String> setOfAllSpecies = new HashSet<String>();
    setOfAllSpecies.addAll(speciesSet);
    setOfAllSpecies.addAll(smallMoleculesSet);
    determineReactionsAndSpeciesToAdd(speciesToAdd, reactionsToAdd,
      setOfAllSpecies, speciesToReaction, reactionToSpecies, strict, merge);
    
    addImportantTransportReactions(speciesToAdd, reactionsToAdd,
      reactionToSpecies);
    //add species and their compartments
    SBMLDocument newDoc = createFilteredSBMLDocument(speciesToAdd,
      reactionsToAdd, doc);
    
    //TODO other SBML elements
    
    SBMLWriter w = new SBMLWriter();
    w.write(newDoc, outputFile);
    System.out.println(doc.getModel().getNumSpecies() + " "
        + doc.getModel().getNumReactions());
    System.out.println(newDoc.getModel().getNumSpecies() + " "
        + newDoc.getModel().getNumReactions());
  }
  
  private static void addImportantTransportReactions(Set<Species> speciesToAdd,
    Set<Reaction> reactionsToAdd, Map<Reaction, Set<Species>> reactionToSpecies) {
    Set<Reaction> transportReactions = new HashSet<Reaction>();
    Set<Reaction> transportReactionsToAdd = new HashSet<Reaction>();
    for (Reaction r : reactionToSpecies.keySet()) {
      if (r.getSBOTerm() == 185) {
        transportReactions.add(r);
      }
    }
    boolean reactionAdded = true;
    while (reactionAdded) {
      reactionAdded=false;
      transportReactionsToAdd.clear();
      for (Reaction r : transportReactions) {
        boolean reactantsContained = true;
        boolean productsContained = true;
        
        for (SpeciesReference sr : r.getListOfReactants()) {
          Species sp = sr.getSpeciesInstance();
          if (!speciesToAdd.contains(sp)) {
            reactantsContained = false;
            break;
          }
        }
        
        for (SpeciesReference sr : r.getListOfProducts()) {
          Species sp = sr.getSpeciesInstance();
          if (!speciesToAdd.contains(sp)) {
            productsContained = false;
            break;
          }
        }
        if (reactantsContained || productsContained) {
          reactionAdded = true;
          transportReactionsToAdd.add(r);
          speciesToAdd.addAll(reactionToSpecies.get(r));
        }
      }
      if(reactionAdded) {
        reactionsToAdd.addAll(transportReactionsToAdd);
        transportReactions.removeAll(transportReactionsToAdd);
      }
    }
  }
  
  private static void determineReactionsAndSpeciesToAdd(
    Set<Species> speciesToAdd, Set<Reaction> reactionsToAdd,
    Set<String> speciesSet, Map<Species, Set<Reaction>> speciesToReaction,
    Map<Reaction, Set<Species>> reactionToSpecies, boolean strict, boolean merge) {
    for (Reaction r : reactionToSpecies.keySet()) {
      
      if (r.getSBOTerm() != 185) {
        addInterestingReactions(r, speciesSet, reactionsToAdd, strict, merge,
          speciesToReaction);
      }
    }
    for (Reaction r : reactionsToAdd) {
      speciesToAdd.addAll(reactionToSpecies.get(r));
    }
    
  }
  
  private static void addInterestingReactions(Reaction r,
    Set<String> speciesSet, Set<Reaction> reactionsToAdd, boolean strict,
    boolean merge, Map<Species, Set<Reaction>> speciesToReaction) {
    if (strict && reactionInterestingStrict(r, speciesSet)) {
      reactionsToAdd.add(r);
    } else if (!strict && reactionInteresting(r, speciesSet)) {
      reactionsToAdd.add(r);
    } else {
      testConnections(r, reactionsToAdd, speciesToReaction, speciesSet, strict,
        merge);
      
    }
    
  }
  
  private static boolean reactionInteresting(Reaction r, Set<String> speciesSet) {
    boolean productContained = false;
    boolean reactantContained = false;
    
    for (SpeciesReference sr : r.getListOfReactants()) {
      String id = sr.getSpecies();
      if (speciesSet.contains(id)) {
        reactantContained = true;
        break;
      }
    }
    
    for (SpeciesReference sr : r.getListOfProducts()) {
      String id = sr.getSpecies();
      if (speciesSet.contains(id)) {
        productContained = true;
        break;
      }
    }
    if (reactantContained && productContained) {
      return true;
    } else {
      return false;
    }
  }
  
  private static boolean reactionInterestingStrict(Reaction r,
    Set<String> speciesSet) {
    boolean productsContained = true;
    boolean reactantsContained = true;
    
    for (SpeciesReference sr : r.getListOfReactants()) {
      String id = sr.getSpecies();
      if (!speciesSet.contains(id)) {
        reactantsContained = false;
        break;
      }
    }
    
    for (SpeciesReference sr : r.getListOfProducts()) {
      String id = sr.getSpecies();
      if (!speciesSet.contains(id)) {
        productsContained = false;
        break;
      }
    }
    
    if (reactantsContained && productsContained) {
      return true;
    } else {
      return false;
    }
  }
  
  private static void testConnections(Reaction r, Set<Reaction> reactionsToAdd,
    Map<Species, Set<Reaction>> speciesToReaction, Set<String> speciesSet,
    boolean strict, boolean merge) {
    
    //TODO: reversible reactions
    
    //find reactions where all products of r take part
    Set<Reaction> commonInterestingReactions = null;
    for (SpeciesReference sr : r.getListOfProducts()) {
      Species sp = sr.getSpeciesInstance();
      Set<Reaction> interestingReactions = new HashSet<Reaction>();
      if (speciesToReaction.get(sp) != null) {
        for (Reaction r2 : speciesToReaction.get(sp)) {
          if (r2.hasReactant(sp)) {
            interestingReactions.add(r2);
          }
        }
      }
      if (commonInterestingReactions == null) {
        commonInterestingReactions = new HashSet<Reaction>();
        commonInterestingReactions.addAll(interestingReactions);
      } else {
        commonInterestingReactions.retainAll(interestingReactions);
      }
    }
    
    for (Reaction r2 : commonInterestingReactions) {
      if (r2.getSBOTerm() != 185) {
        List<SpeciesReference> list = new LinkedList<SpeciesReference>();
        list.addAll(r2.getListOfReactants());
        list.removeAll(r.getListOfProducts());
        if (list.size() == 0) {
          Reaction newReaction = combineReactions(r, r2);
          
          boolean addReaction = false;
          if (newReaction != null) {
            if (strict) {
              addReaction = reactionInterestingStrict(newReaction, speciesSet);
            } else {
              addReaction = reactionInteresting(newReaction, speciesSet);
            }
          }
          if (addReaction) {
            if (merge) {
              reactionsToAdd.add(newReaction);
            } else {
              reactionsToAdd.add(r);
              reactionsToAdd.add(r2);
            }
          }
        }
      }
    }
  }
  
  private static Reaction combineReactions(Reaction r, Reaction r2) {
    Reaction newReaction = new Reaction(r.getId() + "_" + r2.getId(),
      r.getLevel(), r.getVersion());
    newReaction.setName(r.getName() + "_" + r2.getName());
    String compartmentId = r.getCompartment();
    if (!compartmentId.equals("")) {
      newReaction.setCompartment(compartmentId);
    }
    newReaction.setFast(r.getFast() && r2.getFast());
    newReaction.setReversible(r.getReversible() & r2.getReversible());
    newReaction.setListOfModifiers(r.getListOfModifiers());
    newReaction.setListOfReactants(r.getListOfReactants());
    newReaction.setListOfProducts(r2.getListOfProducts());
    if ((r.isSetSBOTerm() && r2.isSetSBOTerm())
        && (r.getSBOTerm() == r2.getSBOTerm())) {
      newReaction.setSBOTerm(r.getSBOTerm());
    }
    newReaction.setKineticLaw(r.getKineticLaw());
    
    HashMap<String, Double> leftStoichiometry = new HashMap<String, Double>();
    HashMap<String, Double> rightStoichiometry = new HashMap<String, Double>();
    for (SpeciesReference sr : r.getListOfProducts()) {
      String id = sr.getSpecies();
      double st = sr.getStoichiometry();
      Double currentStoichiometry = leftStoichiometry.get(id);
      if (currentStoichiometry == null) {
        leftStoichiometry.put(id, st);
      } else {
        leftStoichiometry.put(id, st + currentStoichiometry);
      }
    }
    for (SpeciesReference sr : r2.getListOfReactants()) {
      String id = sr.getSpecies();
      double st = sr.getStoichiometry();
      Double currentStoichiometry = rightStoichiometry.get(id);
      if (currentStoichiometry == null) {
        rightStoichiometry.put(id, st);
      } else {
        rightStoichiometry.put(id, st + currentStoichiometry);
      }
      
    }
    
    boolean ratioSet = false;
    double ratio = -1;
    for (String id : leftStoichiometry.keySet()) {
      double left = leftStoichiometry.get(id);
      double right = rightStoichiometry.get(id);
      if (ratioSet == false) {
        ratioSet = true;
        ratio = left / right;
      } else {
        if (left / right != ratio) { return null; }
      }
    }
    for (SpeciesReference sr : newReaction.getListOfProducts()) {
      sr.setStoichiometry(sr.getStoichiometry() * ratio);
    }
    
    for (CVTerm ct : r.getCVTerms()) {
      newReaction.addCVTerm(ct);
    }
    for (CVTerm ct : r2.getCVTerms()) {
      newReaction.addCVTerm(ct);
    }
    return newReaction;
  }
  
  private static void createMaps(Map<Species, Set<Reaction>> speciesToReaction,
    Map<Reaction, Set<Species>> reactionToSpecies, SBMLDocument doc) {
    for (Reaction r : doc.getModel().getListOfReactions()) {
      Set<Species> species = new HashSet<Species>();
      
      for (SpeciesReference sr : r.getListOfReactants()) {
        Species sp = sr.getSpeciesInstance();
        species.add(sp);
        Set<Reaction> reactions = speciesToReaction.get(species);
        if (reactions == null) {
          reactions = new HashSet<Reaction>();
        }
        reactions.add(r);
        speciesToReaction.put(sp, reactions);
      }
      for (SpeciesReference sr : r.getListOfProducts()) {
        Species sp = sr.getSpeciesInstance();
        species.add(sp);
        Set<Reaction> reactions = speciesToReaction.get(species);
        if (reactions == null) {
          reactions = new HashSet<Reaction>();
        }
        reactions.add(r);
        speciesToReaction.put(sp, reactions);
      }
      for (ModifierSpeciesReference sr : r.getListOfModifiers()) {
        Species sp = sr.getSpeciesInstance();
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
    Set<Species> speciesToAdd, Set<Reaction> reactionsToAdd, SBMLDocument doc) {
    //create new model
    SBMLDocument newDoc = new SBMLDocument(doc.getLevel(), doc.getVersion());
    newDoc.createModel("newModel");
    
    for (Species sp : speciesToAdd) {
      if (newDoc.getModel().getSpecies(sp.getId()) == null) {
        newDoc.getModel().addSpecies(sp);
      }
      String compartmentId = sp.getCompartment();
      if (!newDoc.getModel().containsCompartment(compartmentId)) {
        newDoc.getModel().addCompartment(
          doc.getModel().getCompartment(compartmentId));
      }
    }
    
    for (Reaction r : reactionsToAdd) {
      newDoc.getModel().addReaction(r);
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
      false, "files/Small_molecules.txt", 1, false, true, true,
      "files/filteredModel.xml");
  }
}
