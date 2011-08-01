package org.sbml.simulator.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.AbstractSBase;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;

public class ModelMerging {
  
  public static void mergeModels(String[] modelFiles)
    throws XMLStreamException, IOException, SBMLException {
    if (modelFiles.length == 0) { return; }
    
    SBMLDocument[] docs = new SBMLDocument[modelFiles.length];
    for (int i = 0; i != modelFiles.length; i++) {
      docs[i] = (new SBMLReader()).readSBML(modelFiles[i]);
    }
    
    int level = docs[0].getLevel();
    int version = docs[0].getVersion();
    SBMLDocument newDoc = new SBMLDocument(level, version);
    newDoc.createModel("newModel");
    
    Map<String, List<AbstractSBase>> compartmentAnnotationMap = new HashMap<String, List<AbstractSBase>>();
    Map<String, List<AbstractSBase>> speciesAnnotationMap = new HashMap<String, List<AbstractSBase>>();
    Map<String, List<AbstractSBase>> reactionAnnotationMap = new HashMap<String, List<AbstractSBase>>();
    
    for (int j = 0; j < docs.length; j++) {
      Model currentModel = docs[j].getModel();
      
      //compartments
      for (int n = 0; n != currentModel.getNumCompartments(); n++) {
        Compartment c = currentModel.getCompartment(n);
        List<CVTerm> cvTerms = c.getCVTerms();
        Map<String,String> namespaces =null;
        if(c.getAnnotation()!=null) {
         namespaces = c.getAnnotation().getAnnotationNamespaces(); 
        }
        c.setParentSBML(null);
        c.setLevel(level);
        c.setVersion(version);
        c.setId("C" + j + "_" + n + "_" + c.getId());
        c.setMetaId(c.getId());
        if (cvTerms.size() != 0) {
          Annotation a=new Annotation(cvTerms);
          a.setAnnotationNamespaces(namespaces);
          c.setAnnotation(a);
        }
        newDoc.getModel().addCompartment(c);
        for (int cv = 0; cv != c.getNumCVTerms(); cv++) {
          CVTerm current = c.getCVTerm(cv);
          List<AbstractSBase> list = compartmentAnnotationMap.get(current
              .toString());
          if (list == null) {
            list = new LinkedList<AbstractSBase>();
          }
          list.add(c);
          compartmentAnnotationMap.put(current.toString(), list);
        }
        
      }
      
      //species
      for (int n = 0; n != currentModel.getNumSpecies(); n++) {
        Species s = currentModel.getSpecies(n);
        List<CVTerm> cvTerms = s.getCVTerms();
        s.setParentSBML(null);
        s.setLevel(level);
        s.setVersion(version);
        s.setId("S" + j + "_" + n + "_" + s.getId());
        s.setMetaId(s.getId());
        if (cvTerms.size() != 0) {
          s.setAnnotation(new Annotation(cvTerms));
        }
        newDoc.getModel().addSpecies(s);
        for (int cv = 0; cv != s.getNumCVTerms(); cv++) {
          CVTerm current = s.getCVTerm(cv);
          List<AbstractSBase> list = speciesAnnotationMap.get(current
              .toString());
          if (list == null) {
            list = new LinkedList<AbstractSBase>();
          }
          list.add(s);
          speciesAnnotationMap.put(current.toString(), list);
        }
        
      }
      
      //reactions
      for (int n = 0; n != currentModel.getNumReactions(); n++) {
        Reaction r = currentModel.getReaction(n);
        List<CVTerm> cvTerms = r.getCVTerms();
        r.setParentSBML(null);
        r.setLevel(level);
        r.setVersion(version);
        r.setId("C" + j + "_" + n + "_" + r.getId());
        r.setMetaId(r.getId());
        if (cvTerms.size() != 0) {
          r.setAnnotation(new Annotation(cvTerms));
        }
        newDoc.getModel().addReaction(r);
        for (int cv = 0; cv != r.getNumCVTerms(); cv++) {
          CVTerm current = r.getCVTerm(cv);
          List<AbstractSBase> list = reactionAnnotationMap.get(current
              .toString());
          if (list == null) {
            list = new LinkedList<AbstractSBase>();
          }
          list.add(r);
          reactionAnnotationMap.put(current.toString(), list);
        }
        
      }
      
    }
    
    //remove elements with the same annotation and SBOTerm
    
    //compartments
    removeDuplicateElements(newDoc, compartmentAnnotationMap);
    
    //species
    removeDuplicateElements(newDoc, speciesAnnotationMap);
    
    //reactions
    removeDuplicateElements(newDoc, reactionAnnotationMap);
    SBMLWriter w = new SBMLWriter();
    w.write(newDoc, "files/mergedModel.xml");
    
  }
  
  private static void removeDuplicateElements(SBMLDocument doc,
    Map<String, List<AbstractSBase>> annotationMap) {
    
    for (String term : annotationMap.keySet()) {
      List<AbstractSBase> elements = annotationMap.get(term);
      if (elements.size() > 1) {
        for (int n1 = 0; n1 != elements.size(); n1++) {
          for (int n2 = n1 + 1; n2 != elements.size(); n2++) {
            if (checkEquality(elements.get(n1), elements.get(n2))) {
              merge(elements.get(n1), elements.get(n2), doc);
            }
          }
        }
      }
    }
  }
  
  private static void merge(AbstractSBase abstractSBase,
    AbstractSBase abstractSBase2, SBMLDocument doc) {
    if ((abstractSBase instanceof Reaction)
        && (abstractSBase2 instanceof Reaction)) {
      Reaction r = (Reaction) abstractSBase2;
      doc.getModel().removeReaction(r);
      
    } else if ((abstractSBase instanceof Species)
        && (abstractSBase2 instanceof Species)) {
      Species s1 = (Species) abstractSBase;
      Species s2 = (Species) abstractSBase2;
      for (int i = 0; i != doc.getModel().getNumReactions(); i++) {
        Reaction r = doc.getModel().getReaction(i);
        for (int j = 0; j != r.getNumProducts(); j++) {
          if (r.getProduct(j).getSpeciesInstance() == s2) {
            r.getProduct(j).setSpecies(s1);
          }
        }
        for (int j = 0; j != r.getNumReactants(); j++) {
          if (r.getReactant(j).getSpeciesInstance() == s2) {
            r.getReactant(j).setSpecies(s1);
          }
        }
        for (int j = 0; j != r.getNumModifiers(); j++) {
          if (r.getModifier(j).getSpeciesInstance() == s2) {
            r.getModifier(j).setSpecies(s1);
          }
        }
      }
      doc.getModel().removeSpecies(s2);
    } else if ((abstractSBase instanceof Compartment)
        && (abstractSBase2 instanceof Compartment)) {
      Compartment c1 = (Compartment) abstractSBase;
      Compartment c2 = (Compartment) abstractSBase2;
      
      for (int i = 0; i != doc.getModel().getNumSpecies(); i++) {
        Species s = doc.getModel().getSpecies(i);
        if (s.getCompartmentInstance() == c2) {
          s.setCompartment(c1);
        }
      }
      for (int i = 0; i != doc.getModel().getNumReactions(); i++) {
        Reaction r = doc.getModel().getReaction(i);
        if (r.getCompartmentInstance() != null
            && r.getCompartmentInstance() == c2) {
          r.setCompartment(c1);
        }
      }
      doc.getModel().removeCompartment(c2.getId());
    }
  }
  
  private static boolean checkEquality(AbstractSBase abstractSBase,
    AbstractSBase abstractSBase2) {
    if (abstractSBase.getSBOTerm() == abstractSBase2.getSBOTerm()) {
      if ((abstractSBase instanceof Reaction)
          && (abstractSBase2 instanceof Reaction)) {
        Reaction r1 = (Reaction) abstractSBase;
        Reaction r2 = (Reaction) abstractSBase2;
        if (r1.getId().split("_")[0].equals(r2.getId().split("_")[0])) { return false; }
        
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
        if (s1.getId().split("_")[0].equals(s2.getId().split("_")[0])) { return false; }
        if ((s1.getSBOTerm() == s2.getSBOTerm())
            && (s1.getCompartmentInstance() == s2.getCompartmentInstance())) { return true; }
      }
    } else if ((abstractSBase instanceof Compartment)
        && (abstractSBase2 instanceof Compartment)) {
      if (((Compartment) abstractSBase).getId().split("_")[0]
          .equals(((Compartment) abstractSBase2).getId().split("_")[0])) { return false; }
      if (abstractSBase.getSBOTerm() == abstractSBase2.getSBOTerm()) { return true;

      }
    }
    return false;
  }
  
  public static void main(String[] args) throws XMLStreamException,
    IOException, SBMLException {
    String[] files = { "files/CAR_PXR_annotated.xml",
        "files/HepatoNet1_annotated.xml" };
    mergeModels(files);
  }
  
}
