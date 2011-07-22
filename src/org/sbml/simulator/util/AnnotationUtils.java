/*
 * $Id$URL: AddAnnotations.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */

package org.sbml.simulator.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;

import de.zbit.kegg.KeggInfoManagement;
import de.zbit.util.InfoManagement;
import de.zbit.util.ProgressBar;

/**
 * Adds annotations (compartment, reaction, species) to a SBML model that are
 * given in tab-delimited format.
 */
/**
 * @author Roland Keller
 * @version $Rev$
 * @since
 */
public class AnnotationUtils {
  
  public static void addAnnotations(String inputFile, String outputFile,
    String annotationFile, int col, int altCol) throws FileNotFoundException,
    XMLStreamException, SBMLException {
    
    SBMLDocument doc = (new SBMLReader()).readSBML(inputFile);
    
    BufferedReader reader = new BufferedReader(new FileReader(annotationFile));
    
    //determine columns for annotations
    int colAnnotation = 1;
    int altColAnnotation = -1;
    if (col > 0) {
      colAnnotation = col;
    }
    if (altCol > 0) {
      altColAnnotation = altCol;
    }
    
    //read annotations
    Map<String, List<String>> annotationMap = new HashMap<String, List<String>>();
    
    try {
      String line = reader.readLine();
      while ((line = reader.readLine()) != null) {
        
        String[] split = line.split("\t");
        String annotation = null;
        
        List<String> list = annotationMap.get(split[0]);
        if (list == null) {
          list = new LinkedList<String>();
        }
        if (split.length > colAnnotation) {
          annotation = split[colAnnotation];
        }
        if (!(annotation == null) && !(annotation.equals(""))) {
          list.add(annotation);
        }
        
        if ((altColAnnotation != -1) && (split.length > altColAnnotation)) {
          annotation = split[altColAnnotation];
        }
        if (!(annotation == null) && !(annotation.equals(""))) {
          list.add(annotation);
        }
        if (list.size() != 0) {
          annotationMap.put(split[0], list);
        }
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    
    KeggInfoManagement manager = null;
    try {
      manager = (KeggInfoManagement) KeggInfoManagement
          .loadFromFilesystem("kgim.dat");
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    Object[] elements = new Object[doc.getModel().getNumSpecies()
        + doc.getModel().getNumReactions()];
    System.arraycopy(doc.getModel().getListOfSpecies().toArray(), 0, elements,
      0, doc.getModel().getNumSpecies());
    System.arraycopy(doc.getModel().getListOfReactions().toArray(), 0,
      elements, doc.getModel().getNumSpecies(), doc.getModel()
          .getNumReactions());
    System
        .arraycopy(doc.getModel().getListOfCompartments().toArray(), 0,
          elements, doc.getModel().getNumReactions()
              + doc.getModel().getNumSpecies(), doc.getModel()
              .getNumCompartments());
    if (manager == null) manager = new KeggInfoManagement();
    
    int total = 0, matched = 0, notMatched = 0;
    ProgressBar prog = new ProgressBar(elements.length);
    
    for (Object el : elements) {
      AbstractNamedSBase element = (AbstractNamedSBase) el;
      
      total++;
      prog.DisplayBar();
      if (element.getAnnotation().isSetAnnotation()) {
        matched++;
        continue;
      }
      
      String symbol = element.getId();
      Set<String> annotations = new HashSet<String>();
      
      List<String> l1 = annotationMap.get(symbol);
      if (l1 != null) {
        annotations.addAll(l1);
      }
      
      symbol = element.getName();
      List<String> l2 = annotationMap.get(symbol);
      if (l2 != null) {
        annotations.addAll(l2);
      }
      
      if (!element.isSetMetaId()) {
        element.setMetaId("meta_" + element.getId());
      }
      
      for (String annotation : annotations) {
        if (element instanceof Species) {
          if ((annotation.startsWith("P")) || (annotation.startsWith("Q"))
              || (annotation.startsWith("O"))) {
            element.addCVTerm(new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER,
              CVTerm.Qualifier.BQB_IS, "urn:miriam:uniprot:" + annotation));
          } else {
            Annotate.annotate((Species) element, annotation, manager);
          }
        } else if (element instanceof Reaction) {
          if (annotation.startsWith("R")) {
            element
                .addCVTerm(new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER,
                  CVTerm.Qualifier.BQB_IS, "urn:miriam:kegg.reaction:"
                      + annotation));
          } else if (annotation.startsWith("EC")) {
            element.addCVTerm(new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER,
              CVTerm.Qualifier.BQB_IS, "urn:miriam:ec-code:" + annotation));
            
          }
        } else if (element instanceof Compartment) {
          if (annotation.startsWith("GO")) {
            element
                .addCVTerm(new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER,
                  CVTerm.Qualifier.BQB_IS, "urn:miriam:obo.go"
                      + annotation));
          }
        }
      }
      
      if (element.isSetAnnotation()) {
        matched++;
      } else {
        notMatched++;
        System.out.println(symbol);
      }
      
    }
    InfoManagement.saveToFilesystem("kgim.dat", manager);
    
    // report
    System.out.println("Annotation results:");
    System.out.println("Total: " + total + " Matched: " + matched
        + " Unmatched: " + notMatched);
    
    SBMLWriter w = new SBMLWriter();
    w.write(doc, outputFile);
    
  }
  
  /**
   * 
   * @param inputFile
   * @param outputFile
   * @param modifierFile
   * @param annotationModifiers
   * @throws FileNotFoundException
   * @throws XMLStreamException
   */
  public static void addModifiers(String inputFile, String outputFile,
    String modifierFile, String annotationModifiers)
    throws FileNotFoundException, XMLStreamException {
    //read species for catalyzed reactions
    SBMLDocument doc = (new SBMLReader()).readSBML(inputFile);
    
    Map<String, List<String>> modifiers = new HashMap<String, List<String>>();
    BufferedReader reader = new BufferedReader(new FileReader(modifierFile));
    String l;
    try {
      while ((l = reader.readLine()) != null) {
        String[] s = l.split("\t");
        for (int i = 1; i < s.length; i++) {
          if (!s[i].equals("")) {
            List<String> list = modifiers.get(s[i]);
            if (list == null) {
              list = new LinkedList<String>();
              list.add(s[0]);
            }
            modifiers.put(s[i], list);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    //add modifiers 
    for (Object o : doc.getModel().getListOfReactions().toArray()) {
      Reaction r = (Reaction) o;
      for (int i = 0; i != r.getAnnotation().getNumCVTerms(); i++) {
        for (String ann : r.getAnnotation().getCVTerm(i)
            .filterResources("urn:miriam:ec-code:")) {
          String annotation = ann;
          System.out.println(annotation);
          if (modifiers.get(annotation) != null) {
            for (String modifierID : modifiers.get(annotation)) {
              Species modifier = doc.getModel().getSpecies(modifierID);
              if (modifier == null) {
                modifier = doc.getModel().createSpecies(modifierID);
                Compartment c = r.getCompartmentInstance();
                if (c == null) {
                  if (r.getNumReactants() > 0) {
                    Species s = r.getReactant(0).getSpeciesInstance();
                    if (s != null) {
                      c = s.getCompartmentInstance();
                    }
                  }
                }
                if (c == null) {
                  if (r.getNumProducts() > 0) {
                    Species s = r.getProduct(0).getSpeciesInstance();
                    if (s != null) {
                      c = s.getCompartmentInstance();
                    }
                  }
                }
                if (c != null) {
                  modifier.setCompartment(c);
                }
                modifier.setName(modifierID);
                modifier.setMetaId(modifier.getId());
                //TODO SBOTerms
                //TODO annotations
                
              }
              r.addModifier(new ModifierSpeciesReference(modifier));
            }
          }
        }
      }
    }
  }
  public static void main(String[] args) throws FileNotFoundException, XMLStreamException, SBMLException {
      String file1="files\\CAR_PXR_2_4.xml";
      String file2="files\\HepatoNet1.xml";
      String file3="files\\CAR_PXR_annotated";
      String file4="files\\HepatoNet1_annotated";
      
      String annotationFile1="files\\annotations_HepatoNet1.txt";
      String annotationFile2="files\\Reactions_HepatoNet1.txt";
      String annotationFile3="files\\Compartments_HepatoNet1.txt";
      
      String annotationFile4="files\\annotations_CAR_PXR.txt";
      String annotationFile5="files\\Compartments_CAR_PXR.txt";
      
      String modifierFile="files\\CAR_PXR_catalysis.txt";
      
      //annotate HepatoNet
      Annotate.automaticAnnotation(file2, file4);
      
      //species
      addAnnotations(file4,file4,annotationFile1,-1,-1);
      
      //reactions
      addAnnotations(file4,file4,annotationFile2,5,3);
      
      //compartments
      addAnnotations(file4,file4,annotationFile3,-1,-1);
      
      //add modifiers
      addModifiers(file4,file4,modifierFile,annotationFile4);
      
      
      //extend other model 
      SBMLFileExtension.extendModel(file1, file3);
      
      //annotate other model
      Annotate.automaticAnnotation(file1, file2);
      
      //species
      addAnnotations(file3,file3,annotationFile4,-1,-1);
      
      //compartments
      addAnnotations(file3,file3,annotationFile5,-1,-1);
      
      
      
  }
}
