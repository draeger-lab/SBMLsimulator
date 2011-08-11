package org.sbml.simulator.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;

import de.zbit.kegg.KeggAdaptor;
import de.zbit.kegg.KeggFunctionManagement;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.KeggInfos;
import de.zbit.kegg.KeggQuery;
import de.zbit.util.CustomObject;
import de.zbit.util.EscapeChars;
import de.zbit.util.InfoManagement;
import de.zbit.util.ProgressBar;

/**
 * @author Roland Keller
 * @author Clemens Wrzodek
 * @version $Rev$
 * @since 1.0
 */
public class Annotate {
  
  /**
   * String to use for quotation start marks in SBML descriptions/ titles.
   */
  public static String quotStart = "&#8220;"; // "\u201C";//"&#8220;"; // &ldquo;
  
  /**
   * String to use for quotation end marks in SBML descriptions/ titles.
   */
  public static String quotEnd = "&#8221;"; // "\u201D";//"&#8221;"; // &rdquo;
  
  private final static String notesStartString = "<notes><body xmlns=\"http://www.w3.org/1999/xhtml\">";
  private final static String notesEndString = "</body></notes>";
  
  /**
   * 
   * @param inputFile
   * @param outputFile
   * @throws XMLStreamException
   * @throws SBMLException
   * @throws IOException 
   */
  public static void automaticAnnotation(String inputFile, String outputFile) throws XMLStreamException, SBMLException, IOException {
    SBMLDocument doc = (new SBMLReader()).readSBML(inputFile);
    
    //KeggAdaptor adap = new KeggAdaptor();
    KeggAdaptor.printEachOutputToScreen = false;
    
    // Load caches
    KeggFunctionManagement fmanager = null;
    try {
      fmanager = (KeggFunctionManagement) KeggFunctionManagement
          .loadFromFilesystem("kgfc.dat");
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (fmanager == null) fmanager = new KeggFunctionManagement();
    
    KeggInfoManagement manager = null;
    try {
      manager = (KeggInfoManagement) KeggInfoManagement
          .loadFromFilesystem("kgim.dat");
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (manager == null) manager = new KeggInfoManagement();
    
    int total = 0, matched = 0, ambigous = 0, notMatched = 0;
    ProgressBar prog = new ProgressBar(doc.getModel().getListOfSpecies().size());
    
    for (Species spec : doc.getModel().getListOfSpecies()) {
      total++;
      prog.DisplayBar();
      boolean foundExactHit = false;
      
      String symbol = spec.getName().trim();
      List<String> symbols = new LinkedList<String>();
      symbols.add(symbol);
      symbol = symbol.replaceAll("_", " ").replaceAll("-", " ");
      symbols.add(symbol);
      symbol = symbol.replaceAll("\\(.\\)", "").replaceAll("\\(..\\)", "")
          .replaceAll("\\(gene\\)", "").replaceAll("\\(RNA\\)", "")
          .replaceAll("\\(p\\)", "").replaceAll("\\(s\\)", "")
          .replaceAll("\\(r\\)", "").replaceAll("\\(l\\)", "")
          .replaceAll("\\(n\\)", "").replaceAll("\\(b\\)", "")
          .replaceAll("\\(PG\\)", "");
      symbols.add(symbol);
      String symbol2 = symbol.replaceAll("PP", "-bisphosphate");
      symbols.add(symbol2);
      symbol2 = symbol.replaceAll("PP", "diphosphate");
      symbols.add(symbol2);
      symbol2 = symbol.replaceAll("P", "-phosphate");
      symbols.add(symbol2);
      symbol2 = symbol.replaceAll("Pase", " phosphatase");
      symbols.add(symbol2);
      
      KeggQuery query = null;
      
      CustomObject<Object> res = null;
      String resultingSymbol = null;
      for (String s : symbols) {
        //Species is gene or RNA
        if (spec.getSBOTerm() == 243 || spec.getSBOTerm() == 278) {
          query = new KeggQuery(KeggQuery.genericFind, "GENES " + s);
          res = fmanager.getInformation(query);
        }

        //Species is not a gene and not degraded
        else if (spec.getSBOTerm() != 291) {
          query = new KeggQuery(KeggQuery.genericFind, "COMPOUND " + s);
          res = fmanager.getInformation(query);
        }
        
        if (res != null) {
          resultingSymbol = s;
          break;
        }
      }
      
      if (res != null && res.getObject() != null
          && res.getObject().toString().length() > 0) {
        String result = res.getObject().toString();
        String[] matches = result.split("\n");
        for (String match : matches) {
          String[] matchSplit = match.split(" ", 2);
          if (matchSplit.length < 2) continue;
          String[] hitSymbols = matchSplit[1].split(";");
          for (String hitSymbol : hitSymbols) {
            if (hitSymbol.trim().equalsIgnoreCase(resultingSymbol)) {
              // Exact hit! ADD Miriam URNs
              //System.out.println(spec.getId() + " => " + matchSplit[0]);
              matched++;
              foundExactHit = true;
              break;
            }
          }
          if (foundExactHit) {
            annotate(spec, matchSplit[0], manager);
            break;
          }
        }
        if (!foundExactHit) {
          ambigous++;
          int i = 0, j = 0;
          annotate(spec, matches[i].split(" ", 2)[j], manager);
        }
      } else {
        notMatched++;
        System.out.println(spec.getName());
      }
      
    }
    InfoManagement.saveToFilesystem("kgim.dat", manager);
    InfoManagement.saveToFilesystem("kgfc.dat", fmanager);
    
    // report
    System.out.println("Annotation results:");
    System.out.println("Total: " + total + " Matched: " + matched
        + " Ambigous: " + ambigous + " Unmatched: " + notMatched);
    
    SBMLWriter w = new SBMLWriter();
    w.write(doc, outputFile);
    
  }
  
  public static void annotate(Species spec, String ko_id,
    KeggInfoManagement manager) {
    //KeggInfos infos = new KeggInfos(ko_id, manager);
    
    CVTerm cvtKGID = new CVTerm();
    cvtKGID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtKGID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtEntrezID = new CVTerm();
    cvtEntrezID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtEntrezID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtOmimID = new CVTerm();
    cvtOmimID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtOmimID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtHgncID = new CVTerm();
    cvtHgncID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtHgncID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtEnsemblID = new CVTerm();
    cvtEnsemblID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtEnsemblID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtUniprotID = new CVTerm();
    cvtUniprotID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtUniprotID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtChebiID = new CVTerm();
    cvtChebiID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtChebiID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtDrugbankID = new CVTerm();
    cvtDrugbankID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtDrugbankID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtGoID = new CVTerm();
    cvtGoID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtGoID.setBiologicalQualifierType(Qualifier.BQB_IS_VERSION_OF);
    CVTerm cvtHGNCID = new CVTerm();
    cvtHGNCID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtHGNCID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtPubchemID = new CVTerm();
    cvtPubchemID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtPubchemID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvt3dmetID = new CVTerm();
    cvt3dmetID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvt3dmetID.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm cvtReactionID = new CVTerm();
    cvtReactionID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtReactionID.setBiologicalQualifierType(Qualifier.BQB_IS_DESCRIBED_BY);
    CVTerm cvtTaxonomyID = new CVTerm();
    cvtTaxonomyID.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    cvtTaxonomyID.setBiologicalQualifierType(Qualifier.BQB_OCCURS_IN);
    // New as of oktober 2010:
    CVTerm PDBeChem = new CVTerm();
    PDBeChem.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    PDBeChem.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm GlycomeDB = new CVTerm();
    GlycomeDB.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    GlycomeDB.setBiologicalQualifierType(Qualifier.BQB_IS);
    CVTerm LipidBank = new CVTerm();
    LipidBank.setQualifierType(Type.BIOLOGICAL_QUALIFIER);
    LipidBank.setBiologicalQualifierType(Qualifier.BQB_IS);
    
    // Add Kegg-id Miriam identifier
    String kgMiriamEntry = KeggInfos.getMiriamURIforKeggID(ko_id);
    if (kgMiriamEntry != null) cvtKGID.addResource(kgMiriamEntry);
    
    // Retrieve further information via Kegg API -- Be careful: very slow! Precache all queries at top of this function!
    KeggInfos infos = new KeggInfos(ko_id, manager);
    if (infos.queryWasSuccessfull()) {
      
      // HTML Information
      StringBuffer notes = new StringBuffer(notesStartString);
      if ((infos.getDefinition() != null) && (infos.getName() != null)) {
        notes.append(String.format(
          "<p><b>Description for %s%s%s:</b> %s</p>\n", quotStart,
          EscapeChars.forHTML(infos.getName()), quotEnd,
          EscapeChars.forHTML(infos.getDefinition().replace("\n", " "))));
      } else if (infos.getName() != null) {
        notes.append(String.format("<p><b>%s</b></p>\n",
          EscapeChars.forHTML(infos.getName())));
      }
      if (infos.containsMultipleNames())
        notes.append(String.format("<p><b>All given names:</b><br/>%s</p>\n",
          EscapeChars.forHTML(infos.getNames().replace(";", ""))));
      if (infos.getCas() != null)
        notes.append(String.format("<p><b>CAS number:</b> %s</p>\n",
          infos.getCas()));
      if (infos.getFormula() != null)
        notes.append(String.format("<p><b>Formula:</b> %s</p>\n",
          EscapeChars.forHTML(infos.getFormula())));
      if (infos.getMass() != null)
        notes
            .append(String.format("<p><b>Mass:</b> %s</p>\n", infos.getMass()));
      notes.append(notesEndString);
      spec.appendNotes(notes.toString());
      
      // Parse "NCBI-GeneID:","UniProt:", "Ensembl:", ...
      if (infos.getEnsembl_id() != null)
        appendAllIds(infos.getEnsembl_id(), cvtEnsemblID,
          KeggInfos.miriam_urn_ensembl);
      if (infos.getChebi() != null)
        appendAllIds(infos.getChebi(), cvtChebiID, KeggInfos.miriam_urn_chebi,
          "CHEBI:");
      if (infos.getDrugbank() != null)
        appendAllIds(infos.getDrugbank(), cvtDrugbankID,
          KeggInfos.miriam_urn_drugbank);
      if (infos.getEntrez_id() != null)
        appendAllIds(infos.getEntrez_id(), cvtEntrezID,
          KeggInfos.miriam_urn_entrezGene);
      if (infos.getGo_id() != null) appendAllGOids(infos.getGo_id(), cvtGoID);
      if (infos.getHgnc_id() != null)
        appendAllIds(infos.getHgnc_id(), cvtHGNCID, KeggInfos.miriam_urn_hgnc,
          "HGNC:");
      
      if (infos.getOmim_id() != null)
        appendAllIds(infos.getOmim_id(), cvtOmimID, KeggInfos.miriam_urn_omim);
      if (infos.getPubchem() != null)
        appendAllIds(infos.getPubchem(), cvtPubchemID,
          KeggInfos.miriam_urn_PubChem_Substance);
      
      if (infos.getThree_dmet() != null)
        appendAllIds(infos.getThree_dmet(), cvt3dmetID,
          KeggInfos.miriam_urn_3dmet);
      if (infos.getUniprot_id() != null)
        appendAllIds(infos.getUniprot_id(), cvtUniprotID,
          KeggInfos.miriam_urn_uniprot);
      
      if (infos.getReaction_id() != null)
        appendAllIds(infos.getReaction_id(), cvtReactionID,
          KeggInfos.miriam_urn_kgReaction);
      if (infos.getTaxonomy() != null)
        appendAllIds(infos.getTaxonomy(), cvtTaxonomyID,
          KeggInfos.miriam_urn_taxonomy);
      
      if (infos.getPDBeChem() != null)
        appendAllIds(infos.getPDBeChem(), PDBeChem,
          KeggInfos.miriam_urn_PDBeChem);
      if (infos.getGlycomeDB() != null)
        appendAllIds(infos.getGlycomeDB(), GlycomeDB,
          KeggInfos.miriam_urn_GlycomeDB);
      if (infos.getLipidBank() != null)
        appendAllIds(infos.getLipidBank(), LipidBank,
          KeggInfos.miriam_urn_LipidBank);
    }
    
    // Add all non-empty ressources.
    if (cvtKGID.getNumResources() > 0) spec.addCVTerm(cvtKGID);
    if (cvtEntrezID.getNumResources() > 0) spec.addCVTerm(cvtEntrezID);
    if (cvtOmimID.getNumResources() > 0) spec.addCVTerm(cvtOmimID);
    if (cvtHgncID.getNumResources() > 0) spec.addCVTerm(cvtHgncID);
    if (cvtEnsemblID.getNumResources() > 0) spec.addCVTerm(cvtEnsemblID);
    if (cvtUniprotID.getNumResources() > 0) spec.addCVTerm(cvtUniprotID);
    if (cvtChebiID.getNumResources() > 0) spec.addCVTerm(cvtChebiID);
    if (cvtDrugbankID.getNumResources() > 0) spec.addCVTerm(cvtDrugbankID);
    if (cvtGoID.getNumResources() > 0) spec.addCVTerm(cvtGoID);
    if (cvtHGNCID.getNumResources() > 0) spec.addCVTerm(cvtHGNCID);
    if (cvtPubchemID.getNumResources() > 0) spec.addCVTerm(cvtPubchemID);
    if (cvt3dmetID.getNumResources() > 0) spec.addCVTerm(cvt3dmetID);
    if (cvtReactionID.getNumResources() > 0) spec.addCVTerm(cvtReactionID);
    if (cvtTaxonomyID.getNumResources() > 0) spec.addCVTerm(cvtTaxonomyID);
    if (PDBeChem.getNumResources() > 0) spec.addCVTerm(PDBeChem);
    if (GlycomeDB.getNumResources() > 0) spec.addCVTerm(GlycomeDB);
    if (LipidBank.getNumResources() > 0) spec.addCVTerm(LipidBank);
    
    if (spec.getAnnotation().getNumCVTerms() > 0 && !spec.isSetMetaId()) {
      spec.setMetaId("meta_" + spec.getId());
    }
  }
  
  /**
   * Adds one or multiple goIDs to a CVTerm. Will automatically split the IDs
   * and append the MIRIAM URN.
   * 
   * @param goIDs
   *        - divided by space. Every id is exactly 7 digits long.
   * @param mtGoID
   *        - CVTerm to add MIRIAM URNs with GO ids to.
   */
  private static void appendAllGOids(String goIDs, CVTerm mtGoID) {
    for (String go_id : goIDs.split(" ")) {
      if (go_id.length() != 7 || !containsOnlyDigits(go_id)) continue; // Invalid GO id.
      String urn = KeggInfos.getGo_id_with_MiriamURN(go_id);
      if (!mtGoID.getResources().contains(urn)) {
        mtGoID.addResource(urn);
      }
    }
  }
  
  /**
   * Checks wether a Strings consists just of digits or not.
   * 
   * @param myString
   *        - String to check.
   * @return true if and only if every Character in the given String is a digit.
   */
  private static boolean containsOnlyDigits(String myString) {
    char[] ch = myString.toCharArray();
    for (char c : ch)
      if (!Character.isDigit(c)) return false;
    return true;
  }
  
  /**
   * Append all IDs with Miriam URNs to a CV term. Multiple IDs are separated by
   * a space. Only the part behind the ":" will be added (if an ID contains a
   * ":").
   * 
   * @param IDs
   * @param myCVterm
   * @param miriam_URNPrefix
   */
  private static void appendAllIds(String IDs, CVTerm myCVterm,
    String miriam_URNPrefix) {
    if (IDs == null || IDs.length() < 1) return;
    for (String id : IDs.split(" ")) {
      String urn = miriam_URNPrefix + KeggInfos.suffix(id);
      if (!myCVterm.getResources().contains(urn)) {
        myCVterm.addResource(urn);
      }
    }
  }
  
  /**
   * Append all IDs with Miriam URNs to a CV term. Multiple IDs are separated by
   * a space. All ids are required to contain a ":". If not,
   * mayContainDoublePointButAppendThisStringIfNot will be used. E.g.
   * "[mayContainDoublePointButAppendThisStringIfNot]:[ID]" or [ID] if it
   * contains ":".
   * 
   * @param IDs
   * @param myCVterm
   * @param miriam_URNPrefix
   * @param mayContainDoublePointButAppendThisStringIfNot
   */
  private static void appendAllIds(String IDs, CVTerm myCVterm,
    String miriam_URNPrefix,
    String mayContainDoublePointButAppendThisStringIfNot) {
    // Trim double point from
    // 'mayContainDoublePointButAppendThisStringIfNot' eventually.
    String s = mayContainDoublePointButAppendThisStringIfNot;
    if (s.endsWith(":")) {
      s = s.substring(0, s.length() - 1);
    }
    
    // Add every id to CVTerm.
    for (String id : IDs.split(" ")) {
      // Add prefix + id (with or without ":").
      String urn = miriam_URNPrefix
          + (id.contains(":") ? id.trim() : s + ":" + id.trim());
      if (!myCVterm.getResources().contains(urn)) {
        myCVterm.addResource(urn);
      }
    }
  }
  
  
}
