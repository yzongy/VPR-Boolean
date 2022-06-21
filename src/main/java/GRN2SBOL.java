//Converting GRN matrix to SBOL format
import org.apache.log4j.BasicConfigurator;
import org.sbolstandard.core2.*;
import org.virtualparts.sbol.SBOLInteraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GRN2SBOL {
    public static void main(String[] args) throws SBOLValidationException, SBOLConversionException, IOException {
        //create ModuleDefinition
        BasicConfigurator.configure();
        String uriPrefix = "https://synbiohub.org/user/zyang22";
        URI base=URI.create(uriPrefix);
        SBOLDocument SBOLdoc=new SBOLDocument();
        SBOLdoc.setDefaultURIprefix(uriPrefix);
        ModuleDefinition moduleDef = SBOLdoc.createModuleDefinition(base.toString(),"GRN","2");

        File GRN = new File("GoldstandardGRN.tsv");
        ArrayList<String[]> arrayGRN= tsvr(GRN);

        //create CDs and FCs for transcription factors
        List<String> TF_displayIDList = new ArrayList<String>();
        for (int TF_num =1; TF_num<arrayGRN.get(0).length;TF_num++) {
            String TF_displayID = getElement(arrayGRN.get(0),TF_num);
            TF_displayID = cleanString(TF_displayID);
            TF_displayIDList.add(TF_displayID);
            System.out.println("ID: "+TF_displayID);
            SBOLdoc.createComponentDefinition(uriPrefix,TF_displayID, "2",ComponentDefinition.PROTEIN);
            addFunctionalComponent(moduleDef,TF_displayID,DirectionType.NONE,uriPrefix);
        }

        for(int targetGene=1; targetGene< arrayGRN.size(); targetGene++) {
            String[] targetGene_Array = arrayGRN.get(targetGene);
            String TG_displayID = cleanString(getElement(targetGene_Array, 0));
            if(TF_displayIDList.contains(TG_displayID)){// if the TG has been add as CD and FC as TF, skip the CD and FC creation
            }else{
                System.out.println("TG_ID: "+TG_displayID);
                SBOLdoc.createComponentDefinition(uriPrefix, TG_displayID, "2", ComponentDefinition.PROTEIN);// create CDs for target genes
                addFunctionalComponent(moduleDef,TG_displayID,DirectionType.NONE,uriPrefix);//create FCs for target genes
            }
            //check the array for regulation info
            for(int regulator=1; regulator<targetGene_Array.length; regulator++) {
                String value = Arrays.asList(targetGene_Array).get(regulator);
                Integer geneRegNum = Integer.valueOf(value);
                Boolean activateOrinhibit = null;
                if(geneRegNum == 0){continue;}
                else {
                    if(geneRegNum == 1){activateOrinhibit = true;}
                    if(geneRegNum == -1){activateOrinhibit = false;}
                    String interacrtionID = getElement(arrayGRN.get(0),regulator)+"_regulates_"+getElement(targetGene_Array,0);
                    interacrtionID = cleanString(interacrtionID);
                    System.out.println("interactionID: "+interacrtionID);
                    Interaction interaction = addInteraction(moduleDef,interacrtionID,activateOrinhibit); // create interaction
                    addTFParticipant(interaction,cleanString(getElement(arrayGRN.get(0),regulator)+"_fc"), activateOrinhibit);// create participant for TF
                    //create participant for target genes
                    interaction.createParticipation(cleanString(getElement(targetGene_Array,0)+"_fc"), cleanString(getElement(targetGene_Array,0)+"_fc"),SystemsBiologyOntology.INTERACTOR);

                }
            }
        }
        SBOLWriter.write(SBOLdoc, "sbolOfGRN.xml");
    }

    public static String getElement(String[] arrayOfInts, int index) {
        return arrayOfInts[index];
    }

    public static String cleanString(String TF_displayID){
        TF_displayID = TF_displayID.replaceAll("\\.","_");
        TF_displayID = TF_displayID.replaceAll("-","_");
        return TF_displayID.replaceAll("\"", "");
    }

    public static FunctionalComponent addFunctionalComponent(ModuleDefinition moduleDef, String DisplayId, DirectionType direction, String uriBase) throws SBOLValidationException {
        String uri = uriBase+"/"+DisplayId;
        FunctionalComponent fc = moduleDef.createFunctionalComponent(DisplayId+"_fc",AccessType.PRIVATE, URI.create(uri),direction);
        return fc;
    }

    public static Interaction addInteraction(ModuleDefinition moduleDef, String interactionId, Boolean activateOrinhibit) throws SBOLValidationException {
        Interaction interaction = null;
        if(activateOrinhibit){ interaction = moduleDef.createInteraction(interactionId,SystemsBiologyOntology.STIMULATION);}
        if(!activateOrinhibit){ interaction = moduleDef.createInteraction(interactionId,SystemsBiologyOntology.INHIBITION);}
        return interaction;
    }

    public static Participation addTFParticipant(Interaction interaction, String displayId, Boolean activateOrinhibit) throws SBOLValidationException {
        Participation participant = null;
        if (activateOrinhibit){interaction.createParticipation(displayId, displayId,SystemsBiologyOntology.STIMULATOR);}
        if(!activateOrinhibit){interaction.createParticipation(displayId, displayId,SystemsBiologyOntology.INHIBITOR);}
        return participant;
    }

    //https://stackoverflow.com/questions/26460248/how-to-read-tsv-file-from-java-and-display-in-table-format/30354219
    public static ArrayList<String[]> tsvr(File test2) {
        ArrayList<String[]> Data = new ArrayList<>(); //initializing a new ArrayList out of String[]'s
        try (BufferedReader TSVReader = new BufferedReader(new FileReader(test2))) {
            String line = null;
            while ((line = TSVReader.readLine()) != null) {
                if(line.contains("\\.\\d")){
                    //skip gene with name like YHL001W.1
                }else{
                    String[] lineItems = line.split("\t"); //splitting the line and adding its items in String[]
                    Data.add(lineItems); //adding the splitted line array to the ArrayList
                }
            }
        } catch (Exception e) {
            System.out.println("Something went wrong");
        }
//        Data.forEach(array -> System.out.println(Arrays.toString(array)));
        return Data;
    }

}
