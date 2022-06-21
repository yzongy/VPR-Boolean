import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.io.GenbankSequenceParser;
import org.sbolstandard.core2.*;

import java.io.*;
import java.net.URI;
import java.util.*;

public class GenBank2BoolNet {
    public static void main( String[] args ) throws IOException, SBOLConversionException, SBOLValidationException {
        String Path = new File("").getAbsolutePath();
        String genomeDir=Path+"/genome_GenBank/";
        String sbol_fbcModel ="sbolOfYeast_8.xml";
        String sbol_GRN = "sbolOfGRN_GoldStandard.xml";
        String output_BoolNet = "wildtype_GoldStandarGRNbased.txt";

        String database = sbol_GRN;
        SBOLDocument sbol_database = getSBOLData(database);
        String outputSBOL = database.substring(6,database.length()-4)+"_based_model.xml ";

        ArrayList database_idList = getComponentDefinitionIDList(sbol_database);
        System.out.println("databaseList: "+database_idList);
        ArrayList<String> input_geneList= getGeneListofGenome(genomeDir);

        ArrayList<String> genesDeletedfromSCRaMbLED = getDelList(input_geneList,database_idList);
        ArrayList<String> genesDuplicatefromSCRaMbLED = getDuplicateList(input_geneList,database_idList);

        SBOLWriter.write(getSBOLofInputGenome(sbol_database,genesDeletedfromSCRaMbLED,genesDuplicatefromSCRaMbLED),outputSBOL);
        SBOL2BoolNet(outputSBOL,output_BoolNet);
    }

    //Storing all genes from a genbank file in a list
    public static ArrayList<String> getGeneListofChr(String gbFile) throws IOException {
        ArrayList<String> list = new ArrayList<String>();
        BufferedReader br  = new BufferedReader(new FileReader(gbFile));
        GenbankSequenceParser parser = new GenbankSequenceParser();
        String seq = parser.getSequence(br,0);// just need to let the parser know it's reading br
        List<AbstractFeature> geneList = parser.getFeatures("gene");
        Iterator<AbstractFeature> it = geneList.iterator();
        while (it.hasNext()){
            ArrayList<Qualifier> test = (ArrayList) it.next().getQualifiers().get("locus_tag");
            if(test.size()>1){
                System.out.println("multiple gene!!!!!!");
            }
            else{
                list.add(test.get(0).getValue());
            }
        }

        //Creating a muatated chromosome to test the whole workflow
//        artificialChrPlugin(getIdCopyMap_Artificial("dirEvo_BottomPatterns.csv"),list);

        return list;
    }

    //Storing all genes from a genome directory with chromosomes
    public static ArrayList<String> getGeneListofGenome(String genomeDir) throws IOException {
        ArrayList<String> list = new ArrayList<String>();
        File folder = new File(genomeDir);
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            System.out.println("Reading chromosome file "+file.getName());
            list.addAll(getGeneListofChr(genomeDir+file.getName()));
        }
        System.out.println("The number of genes : "+list.size());
        System.out.println("Genome List: "+list);
        return list;
    }

    //Read the SBOL database
    public static SBOLDocument getSBOLData(String sbolFile) throws IOException, SBOLConversionException, SBOLValidationException {
        InputStream is = new FileInputStream(sbolFile);
        SBOLDocument sbol = new SBOLReader().read(is);
        return sbol;
    }

    //return a list with displayIDs of all component definitions
    public static ArrayList<String> getComponentDefinitionIDList(SBOLDocument sbol){
        ArrayList<String> idList = new ArrayList<String>();
        for(ComponentDefinition cd :sbol.getComponentDefinitions()){
            if(cd.getTypes().contains(ComponentDefinition.PROTEIN)){
                idList.add(cd.getDisplayId());
            }
        }
        return idList;
    }

    //check the SBOL database if it contains a given gene
    public static Boolean containsGene(ArrayList database_idList,String gene){
        Boolean contains = null;
        gene.replaceAll("-","_");// SBOL do not allow dash hence, YBL003C-A was stored as YBL003C_A
        if (database_idList.contains("G_"+gene) | database_idList.contains(gene)){
            contains = true;
        }else{
            contains = false;
        }
        return contains;
    }

    //find genes exist in databases but not in the input sequenced genelist
    public static void removeGene(String gene, ArrayList<String> tempList){
        if (tempList.contains(gene)|tempList.contains("G_"+gene)){tempList.remove(gene);}
    }

    //find genes exist in databases but not in the input sequenced genelist
    public static ArrayList<String> getDelList(ArrayList<String> input_geneList, ArrayList<String> database_idList){
        ArrayList<String> del_tempList = new ArrayList<String>(database_idList);
        for (String gene : input_geneList){
            if(containsGene(database_idList,gene)){
//                System.out.println(gene +" still exists");
                removeGene(gene,del_tempList);
            }else{
//                System.out.println(gene + " not in the SBOL database");
            }
        }
        System.out.println("Deletion: "+del_tempList);
        return del_tempList;
    }

    public static ArrayList<String> getDuplicateList(ArrayList<String> input_geneList, ArrayList<String> database_idList){
        ArrayList<String> duplicateList = new ArrayList<String>();
        for (String gene : database_idList){
            int copyNumber = Collections.frequency(input_geneList,cleanGeneName(gene));
            for (int i =1; i<copyNumber ;i++){
                duplicateList.add(gene);
//                System.out.println(gene+" duplicated");
            }
        }
        System.out.println("Duplication: "+duplicateList);
        return duplicateList;
    }

    public static String cleanGeneName(String gene){
        String cleanGeneName = null;
        if (gene.startsWith("G_")){
            cleanGeneName = gene.substring(2);
        }
        else{
            cleanGeneName = gene;
        }
        return cleanGeneName;
    }

    //return the SBOL file of the input genome, but only consider genes in GRN/fbcModel
    public static SBOLDocument getSBOLofInputGenome(SBOLDocument sbol_database,ArrayList<String> genesDeletedfromSCRaMbLED, ArrayList<String> genesDuplicatefromSCRaMbLED) throws SBOLValidationException {
        SBOLDocument sbol_Altered = new SBOLDocument();
        sbol_Altered.createCopy(sbol_database);
        sbol_Altered.setDefaultURIprefix("https://synbiohub.org/user/zyang22");

        if(sbol_Altered.getModuleDefinition("FBC","2")!=null){
            ModuleDefinition md = sbol_Altered.getModuleDefinition("FBC","2");
            deleteCDsAndFCs(sbol_Altered,genesDeletedfromSCRaMbLED,md);
            addDuplicatedCDsAndFCs(sbol_Altered,genesDuplicatefromSCRaMbLED,md);
        }else if(sbol_Altered.getModuleDefinition("GRN","2")!=null){
            ModuleDefinition md = sbol_Altered.getModuleDefinition("GRN","2");
            deleteCDsAndFCs(sbol_Altered,genesDeletedfromSCRaMbLED,md);
            addDuplicatedCDsAndFCs(sbol_Altered,genesDuplicatefromSCRaMbLED,md);
        }


        return sbol_Altered;
    }

    //delete CDs, FCs and interactions
    public static void deleteCDsAndFCs(SBOLDocument sbol_Altered,ArrayList<String> genesDeletedfromSCRaMbLED, ModuleDefinition md) throws SBOLValidationException {
        for(String del_gene : genesDeletedfromSCRaMbLED){
            System.out.println("Start deleting "+ del_gene);
            if(sbol_Altered.getComponentDefinition(del_gene,"2")!=null){
                System.out.println("deleting " + del_gene);
                for(Interaction interaction: md.getInteractions()){
                    //find the interaction has this participation
                    if ( interaction.getParticipation(del_gene+"_fc") !=null){
                        md.removeInteraction(interaction);
                    }
                }
                FunctionalComponent rmFC= md.getFunctionalComponent(del_gene+"_fc");
                System.out.println("remove FC "+rmFC);
                md.removeFunctionalComponent(rmFC);
                sbol_Altered.removeComponentDefinition(sbol_Altered.getComponentDefinition(del_gene,"2"));
            }else if(sbol_Altered.getComponentDefinition("G_"+del_gene,"2")!=null){
                for(Interaction interaction: md.getInteractions()){
                    //find the interaction has this participation
                    if ( interaction.getParticipation("G_"+del_gene+"_fc") !=null){
                        md.removeInteraction(interaction);
                    }
                }
                FunctionalComponent rmFC= md.getFunctionalComponent("G_"+del_gene+"_fc");
                md.removeFunctionalComponent(rmFC);
                sbol_Altered.removeComponentDefinition(sbol_Altered.getComponentDefinition("G_"+del_gene,"2"));
            }
        }
    }

    //add duplicated CDs, FCs and interactions
    public static void addDuplicatedCDsAndFCs(SBOLDocument sbol_Altered, ArrayList<String> genesDuplicatefromSCRaMbLED, ModuleDefinition md) throws SBOLValidationException {
        System.out.println("adding duplicated genes");
        //create a temp MD to avoid concurrent issues
        SBOLDocument tempSBOL = new SBOLDocument();
        tempSBOL.createCopy(sbol_Altered);
        tempSBOL.setDefaultURIprefix("https://synbiohub.org/user/zyang22");

        if(tempSBOL.getModuleDefinition("GRN","2")!=null){
            ModuleDefinition tempMd = tempSBOL.getModuleDefinition("GRN","2");
            ArrayList<String> checkMultiGenes = new ArrayList<String>();
            HashMap<String,ArrayList<String>> doubleDupPairs = new HashMap<String,ArrayList<String>>();
            for(String duplicate_gene : genesDuplicatefromSCRaMbLED){
                //ignore duplicated gene in this list
                if(checkMultiGenes.contains(duplicate_gene)){continue;}
                checkMultiGenes.add(duplicate_gene);
                //do the work
                int extraCopyNum = Collections.frequency(genesDuplicatefromSCRaMbLED,duplicate_gene);
                for (int i = 0; i<extraCopyNum; i++ ){
                    System.out.println("adding "+ duplicate_gene);
                        sbol_Altered.createComponentDefinition(duplicate_gene+"_ExtraCopy_"+(i+1),sbol_Altered.getComponentDefinition(duplicate_gene,"2").getTypes());
                        FunctionalComponent fc = md.getFunctionalComponent(duplicate_gene+"_fc");
                        FunctionalComponent dupNewfc = md.createFunctionalComponent(duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc",fc.getAccess(),duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc",fc.getDirection());
                        System.out.println("fc："+ dupNewfc.getDisplayId());
                        for(Interaction interaction : tempMd.getInteractions()){
                            if(!checkDoubleDuplicate(genesDuplicatefromSCRaMbLED,interaction)){
                                if (interaction.getParticipation(duplicate_gene+"_fc")!=null && interaction.getParticipation(duplicate_gene+"_fc").getRoles().stream().anyMatch(listOfKeyRoles()::contains)){
                                    System.out.println("Adding interaction: "+ interaction.getDisplayId());
                                    if(md.getInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1)) == null) {
                                        Interaction extraIn = md.createInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1),interaction.getTypes());
                                        for(Participation par : interaction.getParticipations()){
                                            if(par.getDisplayId().equals(duplicate_gene+"_fc")){
                                                extraIn.createParticipation(duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc",duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc", interaction.getParticipation(duplicate_gene+"_fc").getRoles());
                                            }else{
                                                extraIn.createParticipation(par.getDisplayId(),par.getDisplayId(),par.getRoles());
                                            }
                                        }
                                    }
                                }
                                else if(interaction.getParticipation(duplicate_gene+"_fc")!=null && interaction.getParticipation(duplicate_gene+"_fc").getRoles().contains(SystemsBiologyOntology.INTERACTOR)){// GRN targets
                                    if(md.getInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1)) == null){
                                        System.out.println("Adding Interactor duplication interaction: "+ interaction.getDisplayId());
                                        Interaction extraIn = md.createInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1),interaction.getTypes());
                                        for(Participation par : interaction.getParticipations()){
                                            if(par.getDisplayId().equals(duplicate_gene+"_fc")){
                                                extraIn.createParticipation(duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc",duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc", interaction.getParticipation(duplicate_gene+"_fc").getRoles());
                                            }else{
                                                extraIn.createParticipation(par.getDisplayId(),par.getDisplayId(),par.getRoles());
                                            }
                                        }
                                    }
                                }
                            }
                            else if(checkDoubleDuplicate(genesDuplicatefromSCRaMbLED,interaction)){
                                storeDoubleDupPairs(doubleDupPairs,interaction); // deal with them later
                            }
                        }
                }
            }
            //Todo deal with pairs in HashMap doubleDupPairs
            for(Map.Entry<String, ArrayList<String>> pair: doubleDupPairs.entrySet()){
                String regulator = pair.getKey();
                ArrayList<String> targetList = pair.getValue();
                int extraCopyNum_Reg = Collections.frequency(genesDuplicatefromSCRaMbLED,regulator);
                for(String target : targetList){
                    int extraCopyNum_Tar = Collections.frequency(genesDuplicatefromSCRaMbLED,target);
                    Interaction originalIn = md.getInteraction(regulator+"_regulates_"+target);
                    Participation originalParReg = originalIn.getParticipation(regulator+"_fc");
                    Participation originalParTar = originalIn.getParticipation(target+"_fc");
                    //Double loops to iterate through extra copies pairs， note that coresponding FCs have been created
                    for(int numReg = 1; numReg<=extraCopyNum_Reg;numReg++){
                        for(int numTar = 1; numTar<=extraCopyNum_Tar;numTar++){
                            String interactionSuffix = "_ExtraInteraction_with_"+regulator+"_ExtraCopy_"+numReg+"_regulates_"+target+"_ExtraCopy_"+numTar;
                            Interaction newIn = md.createInteraction(originalIn.getDisplayId()+interactionSuffix,originalIn.getTypes());
                            newIn.createParticipation(regulator+"_ExtraCopy_"+numReg+"_fc",regulator+"_ExtraCopy_"+numReg+"_fc", originalParReg.getRoles());
                            newIn.createParticipation(target+"_ExtraCopy_"+numTar+"_fc",target+"_ExtraCopy_"+numTar+"_fc",originalParTar.getRoles());
                        }
                    }
                    //original regulator vs target copies
                    for(int numTar = 1; numTar<=extraCopyNum_Tar;numTar++){
                        String interactionSuffix = "_ExtraInteraction_with_"+regulator+"_regulates_"+target+"_ExtraCopy_"+numTar;
                        Interaction newIn = md.createInteraction(originalIn.getDisplayId()+interactionSuffix,originalIn.getTypes());
                        newIn.createParticipation(regulator+"_fc",regulator+"_fc", originalParReg.getRoles());
                        newIn.createParticipation(target+"_ExtraCopy_"+numTar+"_fc",target+"_ExtraCopy_"+numTar+"_fc",originalParTar.getRoles());
                    }
                    //original target vs regulator copies
                    for(int numReg = 1; numReg<=extraCopyNum_Reg;numReg++){
                        String interactionSuffix = "_ExtraInteraction_with_"+regulator+"_ExtraCopy_"+numReg+"_regulates_"+target;
                        Interaction newIn = md.createInteraction(originalIn.getDisplayId()+interactionSuffix,originalIn.getTypes());
                        newIn.createParticipation(regulator+"_ExtraCopy_"+numReg+"_fc",regulator+"_ExtraCopy_"+numReg+"_fc", originalParReg.getRoles());
                        newIn.createParticipation(target+"_fc",target+"_fc",originalParTar.getRoles());
                    }
                }
            }
        }
        else if(sbol_Altered.getModuleDefinition("FBC","2")!=null){
                ModuleDefinition tempMd = tempSBOL.getModuleDefinition("FBC","2");
                ArrayList<String> checkMultiGenes = new ArrayList<String>();
                for(String duplicate_gene : genesDuplicatefromSCRaMbLED){
                    //ignore duplicated gene in this list
                    if(checkMultiGenes.contains(duplicate_gene)){
                        continue;
                    }
                    checkMultiGenes.add(duplicate_gene);
                    //do the work
                    int extraCopyNum = Collections.frequency(genesDuplicatefromSCRaMbLED,duplicate_gene);
                    for (int i = 0; i<extraCopyNum; i++ ){
                        sbol_Altered.createComponentDefinition("G_"+duplicate_gene+"_ExtraCopy_"+(i+1),sbol_Altered.getComponentDefinition("G_"+duplicate_gene,"2").getTypes());
                        FunctionalComponent fc = md.getFunctionalComponent("G_"+duplicate_gene+"_fc");
                        md.createFunctionalComponent("G_"+duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc",fc.getAccess(),"G_"+duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc",fc.getDirection());
                        for(Interaction interaction : tempMd.getInteractions()){
                            if (interaction.getParticipation(duplicate_gene+"_fc")!=null && interaction.getParticipation(duplicate_gene+"_fc").getRoles().stream().anyMatch(listOfKeyRoles()::contains)){
                                Interaction extraIn = md.createInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1),interaction.getTypes());
                                for(Participation par : interaction.getParticipations()){
                                    if(par.getDisplayId() == "G_"+duplicate_gene+"_fc"){
                                        extraIn.createParticipation("G_"+duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc","G_"+duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc", interaction.getParticipation("G_"+duplicate_gene).getRoles());
                                    }else{
                                        extraIn.createParticipation(par.getDisplayId(),par.getDisplayId(),par.getRoles());
                                    }
                                }
                            }
                        }
                    }
                }

        }

    }

    //key roles trigers an interaction
    public static ArrayList<URI> listOfKeyRoles(){
        ArrayList<URI> list = new ArrayList<URI>();
        list.add(SystemsBiologyOntology.INHIBITOR);
        list.add(SystemsBiologyOntology.STIMULATOR);
        list.add(SystemsBiologyOntology.CATALYST);
        list.add(SystemsBiologyOntology.REACTANT);
        return list;
    }

    public static ArrayList<URI> listOfInhibitors(){
        ArrayList<URI> list = new ArrayList<URI>();
        list.add(SystemsBiologyOntology.INHIBITOR);
        return list;
    }

    //convert the sbol file of input genome to BoolNet
    public static void SBOL2BoolNet(String outputSBOL, String outputBoolNet) throws IOException, SBOLConversionException, SBOLValidationException {
        System.out.println("Creating BoolNet");
        InputStream inputstream = new FileInputStream(outputSBOL);
        SBOLDocument sbolDoc = new SBOLReader().read(inputstream);
        File boolNet = new File(outputBoolNet);
        try(PrintWriter pw = new PrintWriter(new FileWriter(boolNet),false)){
            pw.println("#"+outputSBOL);
            for( ModuleDefinition md: sbolDoc.getModuleDefinitions()){
                System.out.println("Number of FCs: "+ md.getFunctionalComponents().size());
                HashMap<String, ArrayList<String>> relation_NOT = new HashMap<String, ArrayList<String>>();
                HashMap<String, ArrayList<ArrayList<String>>> relation = new HashMap<String, ArrayList<ArrayList<String>>>();
                ArrayList<String> specialReactions = new ArrayList<String>();// to store fbc reactions that contains only reactants and no products
                for (Interaction interaction : md.getInteractions()) {

                    //Store info into lists
                    ArrayList<String> ReactantOrCatalystOrStimulatorID = new ArrayList();
                    ArrayList<String> inhibitorLists = new ArrayList<String>();
                    String inhibitor = null;
                    ArrayList<ArrayList<String>> reactantsAndCatalystAndStimulatorList = new ArrayList<ArrayList<String>>();
                    String productName = null;
                    for (Participation participation : interaction.getParticipations()) {
                        if (participation.getRoles().contains(SystemsBiologyOntology.PRODUCT) | participation.getRoles().contains(SystemsBiologyOntology.INTERACTOR)) { // product in fbc or gene being regulated in GRN
                            productName = getNameOrID(participation);
                            continue;
                        }
                        if (participation.getRoles().contains(SystemsBiologyOntology.INHIBITOR)) {//GRN regulators
                            inhibitor = participation.getDisplayId();
                            inhibitorLists.add(inhibitor);
                            continue;
                        }
                        if(listOfKeyRoles().stream().anyMatch(participation.getRoles()::contains) && !participation.getRoles().contains(SystemsBiologyOntology.INHIBITOR)) {
                            ReactantOrCatalystOrStimulatorID.add(getNameOrID(participation));
                        }
                    }

                    //Storing info into hashmaps
                    if (productName==null && !ReactantOrCatalystOrStimulatorID.isEmpty()){// fbc special reactions that only contain reactants but no product
                        specialReactions.addAll(ReactantOrCatalystOrStimulatorID);
                    }

                    if (relation_NOT.get(productName) == null && inhibitor!=null && productName!=null) {
                        relation_NOT.put(productName, inhibitorLists);
                    }
                    else if (relation_NOT.get(productName) != null && !inhibitorLists.isEmpty()) {
                        relation_NOT.get(productName).addAll(inhibitorLists);
                    }

                    if (relation.get(productName) == null && !ReactantOrCatalystOrStimulatorID.isEmpty() && productName!=null) {//"productName!=null"in case fbc reaction only has reactant
                        reactantsAndCatalystAndStimulatorList.add(ReactantOrCatalystOrStimulatorID);
                        relation.put(productName, reactantsAndCatalystAndStimulatorList);
                    }
                    else if (relation.get(productName) != null && !ReactantOrCatalystOrStimulatorID.isEmpty()) {
                        relation.get(productName).add(ReactantOrCatalystOrStimulatorID);
                    }

                }

                //Write the file
                for (String key : relation.keySet()) { //for a product in the relation hashmap
                    Boolean MR = multipleRelations(key, relation_NOT, relation);
                    pw.println();
                    pw.print(key+"*= ");
                    ArrayList<ArrayList<String>> reactantList = relation.get(key);
                    if(MR){
                        pw.print("(");
                        for (int rList = 0; rList< reactantList.size(); rList++)  {
                            for(int r = 0; r<reactantList.get(rList).size(); r++) {
                                pw.print(reactantList.get(rList).get(r));
                                if( (r!= reactantList.get(rList).size()-1) && (reactantList.get(rList).get(r+1) != null)){
                                    pw.print(" and ");
                                }else{
                                    pw.print(")");
                                }
                            }
                            if( (rList+1) != reactantList.size() && reactantList.get(rList+1)!=null){
                                pw.print(" or (");
                            }
                        }
                        if(relation_NOT.get(key)!=null){
                            pw.print(" and not (");
                            pw.print(relation_NOT.get(key).get(0));
                            for (int in = 1 ; in< relation_NOT.get(key).size(); in ++){
                                pw.print(" or "+relation_NOT.get(key).get(in));
                            }
                            pw.print(")");
                        }
                    }
                    else if (!MR){// Only one list in Arraylist<Arraylist<String>>
                        pw.print("(");
                        ArrayList<String> reactants = relation.get(key).get(0);
                        for(int r = 0; r<reactants.size(); r++) {
                            pw.print(reactants.get(r));
                            if( (r!= reactants.size()-1) && (reactants.get(r+1) != null)){
                                pw.print(" and ");
                            }else{
                                pw.print(")");
                            }
                        }
                    }
                }

                pw.write("\n");
                for (String key : relation_NOT.keySet()){
                    if (!relation.keySet().contains(key)){// a product only in the relation_not hashmap but not in relation hashmap
                        pw.print(key+"*= ");
                        pw.print("not ");
                        if(relation_NOT.get(key).size()>1){
                            pw.print("(");
                            pw.print(relation_NOT.get(key).get(0));
                            for(int i =1; i<relation_NOT.get(key).size(); i++){
                                pw.print(" or "+ relation_NOT.get(key).get(i));
                            }
                            pw.print(")");
                        }else{
                            pw.print(relation_NOT.get(key).get(0));
                        }
                        pw.write("\n");
                    }
                    else if(relation.keySet().contains(key)){
                        continue; // ignore, since it has been processed in relation hashmap
                    }
                }

                //set regulator/reactant nodes that do not regulated/produced by other nodes as active
                ArrayList incaseOverlap = new ArrayList(); // in case a species is both a regulator and special reactant(see next for loop)
                for (FunctionalComponent fc : md.getFunctionalComponents()){
                    String name = fc.getDisplayId();
                    if (!relation.keySet().contains(name) & !relation_NOT.keySet().contains(name)){
                        incaseOverlap.add(name);
                        pw.write(name+"*= 1"+"\n");
                    }
                }

                //set reactants of fbc special reactions(with reactants only but no product) as 1
                for(String reactant : specialReactions){
                    if(!relation.containsKey(reactant) & !incaseOverlap.contains(reactant)){
                        System.out.println("special: "+ reactant);
                        pw.write(reactant+"*= 1"+"\n");
                    }
                }

                //export to csv file to facilitate visualisation with Cytoscape
                export2CSV(relation_NOT,relation,outputBoolNet);
            }
        }
    }

        //Mutiple relations
    public static Boolean multipleRelations(String key,HashMap<String, ArrayList<String>> relation_NOT, HashMap<String, ArrayList<ArrayList<String>>> relation ){
        Boolean multipleRelations = null;
        if (relation_NOT.get(key) !=null && relation.get(key)!= null) {
            multipleRelations = true;
        }else if(relation_NOT.get(key)!= null && relation_NOT.get(key).size()>1){
            multipleRelations = true;
        }else if(relation.get(key)!=null && relation.get(key).size()>1){
            multipleRelations =true;
        }
        else{
            multipleRelations = false;
        }
        return multipleRelations;
    }

    public static String getNameOrID(Participation p){
        String nameOrID = new String();
//        if (p.isSetName()){
//            nameOrID=p.getName();
//        }else{
            nameOrID = p.getDisplayId();
//        }
        return nameOrID;
    }

    public static String getNameOrID(FunctionalComponent fc){
        String nameOrID = new String();
//        if (fc.isSetName()){
//            nameOrID=fc.getName();
//        }else{
            nameOrID = fc.getDisplayId();
//        }
        return nameOrID;
    }

    public static HashMap<String, Integer> getIdCopyMap_Artificial(String csvFile) throws IOException {
        HashMap<String, Integer> IdcopyMap = new HashMap<String, Integer>();
        ArrayList<String []> masterList = new ArrayList();
        BufferedReader reader =new BufferedReader(new FileReader(csvFile));
        String line = "";
        while((line=reader.readLine())!=null){
            String [] tokens =line.trim().split(",");
            masterList.add(tokens);
        }
        for (int i = 0; i < masterList.get(0).length; i++){
            IdcopyMap.put(masterList.get(0)[i],Integer.valueOf(masterList.get(1)[i]));
        }
        return  IdcopyMap;
    }

    public static void artificialChrPlugin(HashMap<String,Integer> IdcopyMap,ArrayList<String> geneList){
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.addAll(geneList);
        for(String gene : tempList){
            if (IdcopyMap.get(gene)!=null){
                Integer copyNumber = IdcopyMap.get(gene);
                if(copyNumber==0){
                    geneList.remove(gene);
                }else if(copyNumber>1){
                    for(int i = 2; i<=copyNumber;i++){
                        geneList.add(gene);
                    }
                }
            }
        }
    }

    //deal with both geneA and geneB got duplicated
    public static void doubleDuplicatePlugin(ModuleDefinition md, Interaction interaction, int i, String duplicate_gene) throws SBOLValidationException {
        while(md.getInteraction(interaction.getDisplayId() + "_ExtraInteraction_" + (i + 1)) != null){
            try{
                Interaction in = md.createInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1),interaction.getTypes());
                System.out.println("reach here!!!!!!!!!");
                break;
            }catch(SBOLValidationException e){
                i++;
            }
        }
        Interaction extraIn = md.createInteraction(interaction.getDisplayId()+"_ExtraInteraction_"+(i+1),interaction.getTypes());
        for(Participation par : interaction.getParticipations()){
            if(par.getDisplayId().equals(duplicate_gene+"_fc")){
                System.out.println("fc to be created as par: "+duplicate_gene+"_ExtraCopy_"+(i+1)+"_fc");
                extraIn.createParticipation(duplicate_gene+"_ExtraCopy_"+i+"_fc",duplicate_gene+"_ExtraCopy_"+i+"_fc", interaction.getParticipation(duplicate_gene+"_fc").getRoles());
            }else{
                extraIn.createParticipation(par.getDisplayId(),par.getDisplayId(),par.getRoles());
            }
        }

    }

    public static Boolean checkDoubleDuplicate(ArrayList<String> genesDuplicatefromSCRaMbLED, Interaction interaction){
        Integer count  = 0;
        for(Participation par : interaction.getParticipations()){
            if(genesDuplicatefromSCRaMbLED.contains(par.getDisplayId().substring(0, par.getDisplayId().length() - 3))){count++;}
        }
        Boolean b = null;
        if(count==2){b = true;}
        else{b = false;}
        return b;
    }

    public static void storeDoubleDupPairs(HashMap<String,ArrayList<String>> doubleDupPairs, Interaction interaction){
        String regulater =null;
        String target =null;
        System.out.println("Interaction: "+interaction.getDisplayId());
        for(Participation par: interaction.getParticipations()){
            if(par.getRoles().contains(SystemsBiologyOntology.STIMULATOR) |par.getRoles().contains(SystemsBiologyOntology.INHIBITOR) ){
                String tempRegulater = par.getDisplayId();
                regulater = tempRegulater.substring(0,tempRegulater.length()-3); //remove"_fc"
            }else if(par.getRoles().contains(SystemsBiologyOntology.INTERACTOR)){
                String tempTarget = par.getDisplayId();
                target = tempTarget.substring(0,tempTarget.length()-3);
            }
        }
        // create <regulater, targetList> pair if not exist
        if(doubleDupPairs.get(regulater) == null){
            ArrayList<String> targetList = new ArrayList<String>();
            targetList.add(target);
            doubleDupPairs.put(regulater,targetList);
        }

        //if target isn't in the targetList, add it to the list
        System.out.println("doubleDupPairs.get(regulater) = "+ doubleDupPairs.get(regulater));
        if(!doubleDupPairs.get(regulater).contains(target)){
            doubleDupPairs.get(regulater).add(target);
        }
        ArrayList<String> list = doubleDupPairs.get(regulater);
        doubleDupPairs.put(regulater,list);
    }

    public static void export2CSV( HashMap<String, ArrayList<String>> relation_NOT, HashMap<String, ArrayList<ArrayList<String>>> relation, String outputFile){
        outputFile = outputFile.substring(0, outputFile.lastIndexOf('.'));
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(outputFile+".csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder builder = new StringBuilder();
        String columnNamesList = "Target,Source,Weight";
        builder.append(columnNamesList +"\n");
        for (String target : relation_NOT.keySet()){
            for(String source : relation_NOT.get(target)){
                builder.append(trim(target) + "," + trim(source) + ",-1" + "\n");
            }
        }
        for(String target : relation.keySet()){
            for(ArrayList<String> list : relation.get(target)){
                for(String source : list){
                    builder.append(trim(target) + "," + trim(source) + ",1" + "\n");
                }
            }
        }
        pw.write(builder.toString());
        pw.close();
    }

    public static String trim(String IDwithFC){
        if (IDwithFC.endsWith("_fc")){
            IDwithFC = IDwithFC.replace("_fc", "");
        }
        return IDwithFC;
    }
}

