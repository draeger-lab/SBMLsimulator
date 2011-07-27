package org.sbml.simulator.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.AbstractSBase;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;

public class ModelMerging {
  
  public static void mergeModels(String[] modelFiles) throws XMLStreamException,
    IOException, SBMLException {
    if(modelFiles.length==0) {
      return;
    }
    
    SBMLDocument[] docs = new SBMLDocument[modelFiles.length];
    for(int i=0;i!=modelFiles.length;i++) {
      docs[i] = (new SBMLReader()).readSBML(modelFiles[i]);
    }
    
    int level = docs[0].getLevel();
    int version = docs[0].getVersion();
    SBMLDocument newDoc = new SBMLDocument(level,version);
    newDoc.createModel("newModel");
    
    Map<Annotation, List<AbstractSBase>> compartmentAnnotationMap = new HashMap<Annotation, List<AbstractSBase>>();
    Map<Annotation, List<AbstractSBase>> speciesAnnotationMap = new HashMap<Annotation, List<AbstractSBase>>();
    Map<Annotation, List<AbstractSBase>> reactionAnnotationMap = new HashMap<Annotation, List<AbstractSBase>>();
    
    for (int j = 0; j < docs.length; j++) {
      Model currentModel = docs[j].getModel();
      //compartments
      for (int n = 0; n != currentModel.getNumCompartments(); n++) {
        Compartment comp = currentModel.getCompartment(n);
        if (newDoc.getModel().getCompartment(comp.getId()) == null) {
          Compartment c = comp.clone();
          c.setLevel(level);
          c.setVersion(version);
          newDoc.getModel().addCompartment(c);
          if (c.hasValidAnnotation()) {
            List<AbstractSBase> list = compartmentAnnotationMap.get(c
                .getAnnotation());
            if (list == null) {
              list = new LinkedList<AbstractSBase>();
            }
            list.add(c);
            compartmentAnnotationMap.put(c.getAnnotation(), list);
          }
        }
      }
      
      //species
      for (int n = 0; n != currentModel.getNumSpecies(); n++) {
        Species spec = currentModel.getSpecies(n);
        if (newDoc.getModel().getCompartment(spec.getId()) == null) {
          Species s = spec.clone();
          s.setLevel(level);
          s.setVersion(version);
          newDoc.getModel().addSpecies(s);
          if (s.hasValidAnnotation()) {
            List<AbstractSBase> list = speciesAnnotationMap.get(s
                .getAnnotation());
            if (list == null) {
              list = new LinkedList<AbstractSBase>();
            }
            list.add(s);
            speciesAnnotationMap.put(s.getAnnotation(), list);
          }
        }
      }
      
      //reactions
      for (int n = 0; n != currentModel.getNumReactions(); n++) {
        Reaction reac = currentModel.getReaction(n);
        if ((reac.getId().equals(""))
            || (newDoc.getModel().getReaction(reac.getId())) == null) {
          Reaction r = reac.clone();
          r.setLevel(level);
          r.setVersion(version);
          newDoc.getModel().addReaction(r);
          if (r.hasValidAnnotation()) {
            List<AbstractSBase> list = reactionAnnotationMap.get(r
                .getAnnotation());
            if (list == null) {
              list = new LinkedList<AbstractSBase>();
            }
            list.add(r);
            reactionAnnotationMap.put(r.getAnnotation(), list);
          }
        }
      }
      
    }
    
    //remove elements with the same annotation and SBOTerm
    //reactions
    removeDuplicateElements(newDoc, reactionAnnotationMap);
    
    //reactions
    removeDuplicateElements(newDoc, speciesAnnotationMap);
    
    //reactions
    removeDuplicateElements(newDoc, compartmentAnnotationMap);
    
    SBMLWriter w = new SBMLWriter();
    w.write(newDoc, "files\\mergedModel.xml");
    
  }
  
  private static void removeDuplicateElements(SBMLDocument doc,
    Map<Annotation, List<AbstractSBase>> annotationMap) {
    Set<AbstractSBase> elementsToRemove = new HashSet<AbstractSBase>();
    for (Annotation a : annotationMap.keySet()) {
      List<AbstractSBase> elements = annotationMap.get(a);
      if (elements.size() > 1) {
        for (int n1 = 0; n1 != elements.size(); n1++) {
          for (int n2 = n1 + 1; n2 != elements.size(); n2++) {
            if (checkEquality(elements.get(n1), elements.get(n2))) {
              elementsToRemove.add(elements.get(n2));
            }
          }
        }
      }
    }
    for (AbstractSBase element : elementsToRemove) {
      if (element instanceof Compartment) {
        doc.getModel().removeCompartment(((Compartment) element).getId());
      } else if (element instanceof Reaction) {
        doc.getModel().removeReaction((Reaction) element);
      } else if (element instanceof Species) {
        doc.getModel().removeSpecies((Species) element);
      }
    }
  }
  
  private static boolean checkEquality(AbstractSBase abstractSBase,
    AbstractSBase abstractSBase2) {
    if (abstractSBase.getSBOTerm() == abstractSBase2.getSBOTerm()) {
      if ((abstractSBase instanceof Reaction)
          && (abstractSBase2 instanceof Reaction)) {
        Reaction r1 = (Reaction) abstractSBase;
        Reaction r2 = (Reaction) abstractSBase2;
        
        //determine compartment of reaction 1
        Compartment c1 = r1.getCompartmentInstance();
        if (c1 == null) {
          if (r1.getNumReactants() > 0) {
            Species s = r1.getReactant(0).getSpeciesInstance();
            if (s != null) {
              c1 = s.getCompartmentInstance();
            }
          }
        }
        if (c1 == null) {
          if (r1.getNumProducts() > 0) {
            Species s = r1.getProduct(0).getSpeciesInstance();
            if (s != null) {
              c1 = s.getCompartmentInstance();
            }
          }
        }
        //determine compartment of reaction 2
        Compartment c2 = r2.getCompartmentInstance();
        if (c2 == null) {
          if (r2.getNumReactants() > 0) {
            Species s = r2.getReactant(0).getSpeciesInstance();
            if (s != null) {
              c2 = s.getCompartmentInstance();
            }
          }
        }
        if (c2 == null) {
          if (r2.getNumProducts() > 0) {
            Species s = r2.getProduct(0).getSpeciesInstance();
            if (s != null) {
              c2 = s.getCompartmentInstance();
            }
          }
        }
        if (c1 == c2) { return true; }
        
      } else if ((abstractSBase instanceof Species)
          && (abstractSBase2 instanceof Species)) {
        Species s1 = (Species) abstractSBase;
        Species s2 = (Species) abstractSBase2;
        if ((s1.getSBOTerm() == s2.getSBOTerm())
            && (s1.getCompartmentInstance() == s2.getCompartmentInstance())) { return true; }
      }
    } else if ((abstractSBase instanceof Compartment)
        && (abstractSBase2 instanceof Compartment)) {
      if (abstractSBase.getSBOTerm() == abstractSBase2.getSBOTerm()) { return true; }
    }
    return false;
  }
  
  public static void main(String[] args) throws XMLStreamException, IOException, SBMLException {
    String[] files = {"files\\CAR_PXR_annotated.xml","files\\HepatoNet1_annotated.xml"}; 
    mergeModels(files);
  }
  
}
