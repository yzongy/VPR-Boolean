import org.apache.log4j.BasicConfigurator;
import org.sbml.jsbml.*;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.*;
import org.sbolstandard.core2.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URI;


import java.util.ArrayList;
import java.util.List;

//This class can create SBOL file storing information from the FBC SBML model
public class fbcSBML2SBOL {


    public static void main(String [] args) throws SBOLValidationException, IOException, SBOLConversionException, XMLStreamException {
        BasicConfigurator.configure();
        String inputFBC = "yeast_8.3.5.xml";
        String uriPrefix = "https://synbiohub.org/user/zyang22";
        URI base=URI.create(uriPrefix);
        SBOLDocument SBOLdoc=new SBOLDocument();
        SBOLdoc.setDefaultURIprefix(uriPrefix);
        ModuleDefinition moduleDef = SBOLdoc.createModuleDefinition(base.toString(),"FBC","2");

        //Read SBML model and fbc extensions
        SBMLReader sbmlReader = new SBMLReader();
        SBMLDocument doc  = sbmlReader.readSBML(inputFBC);
        Model SBMLmodel = doc.getModel();
        FBCModelPlugin fbcModel = (FBCModelPlugin) SBMLmodel.getExtension(FBCConstants.namespaceURI);

        //create CD from list of species and gene products
        for(int i=0; i < SBMLmodel.getNumSpecies(); i++){
            String displayId = SBMLmodel.getSpecies(i).getId();
            String name = SBMLmodel.getSpecies(i).getName();
            addComponentDefinition(SBOLdoc,displayId,ComponentDefinition.SMALL_MOLECULE,base,name);
        }
        for(int i=0; i < fbcModel.getGeneProductCount(); i++){
            String displayId = fbcModel.getGeneProduct(i).getId();
            String name = fbcModel.getGeneProduct(i).getName();
            addComponentDefinition(SBOLdoc,displayId,ComponentDefinition.PROTEIN,base,name);
        }

        //create interactions from the SBML model
        List<String> fcList =  new ArrayList<String>();
        for(int i = 0; i <SBMLmodel.getNumReactions();i++){
            Reaction r = SBMLmodel.getReaction(i);
            String interactionIdPrefix = r.getId();

            //add FunctionalComponents (reactants)
            List displayIdList_reactants = new ArrayList();
            for(int j=0;j<r.getNumReactants();j++){
                String displayId = r.getListOfReactants().get(j).getSpecies();
                if (!fcList.contains(displayId)){
                    fcList.add(displayId);
                    addFunctionalComponent(moduleDef,displayId,DirectionType.IN,uriPrefix);
                }
                //add participants
//                interaction.createParticipation(displayId+"_fc",displayId+"_fc",SystemsBiologyOntology.REACTANT);
                displayIdList_reactants.add(displayId+"_fc");
            }

            //add FunctionalComponents (products)
            List displayIdList_products = new ArrayList();
            for(int j=0;j<r.getNumProducts();j++) {
                String displayId = r.getListOfProducts().get(j).getSpecies();
                if(!fcList.contains(displayId)){
                    fcList.add(displayId);
                    addFunctionalComponent(moduleDef,displayId,DirectionType.OUT,uriPrefix);
                }
                //add participants
//                interaction.createParticipation(displayId+"_fc",displayId+"_fc",SystemsBiologyOntology.PRODUCT);
                displayIdList_products.add(displayId+"_fc");
            }

            System.out.println("reaction number: " + r.getId());

            //add FunctionalComponents (gene products)
            FBCReactionPlugin fbcReactionPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.namespaceURI);
            List<List<GeneProduct>> masterList = new ArrayList();
            if(fbcReactionPlugin.isSetGeneProductAssociation()){
                org.sbml.jsbml.ext.fbc.Association  association = fbcReactionPlugin.getGeneProductAssociation().getAssociation();
//                ASTNode geneAssociationString = processAssociation(fbcReactionPlugin.getGeneProductAssociation().getAssociation());

//                //Only one gene product and no logical operator.
//                if(association instanceof GeneProductRef){
//                    GeneProductRef geneProductRef = (GeneProductRef)association;
//                    GeneProduct geneProduct = (GeneProduct) association.getModel().getSBaseById(geneProductRef.getGeneProduct());
//                    if (geneProduct != null) {
//                        String displayId = geneProduct.getId();
//                        if (!fcList.contains(displayId)){
//                            fcList.add(displayId);
//                            addFunctionalComponent(moduleDef,displayId,DirectionType.IN,uriPrefix);
//                        }
//                        interaction.createParticipation(displayId+"_fc",displayId+"_fc",SystemsBiologyOntology.PROTEIN_COMPLEX_FORMATION);
//                    }
//                }
//
//                //With logical operators
//                if(association instanceof LogicalOperator){
////                    System.out.println("list of asso:"+((LogicalOperator) association).getListOfAssociations().get());
//                    Association lower  = ((LogicalOperator) association).getAssociation(0);
//                    if(lower instanceof GeneProductRef){
//                        //add gene product as participant
//
//                        if(association instanceof Or){//<fbc:or> (fbc:geneProductRef) (fbc:geneProductRef) <fbc:or>
//                            System.out.println("asso: "+((Or) association).getAssociationCount());
//                            for(int num=0;num<((Or) association).getAssociationCount();num++)
//                                if(((Or) association).getAssociation(num) !=null){
//                                    if(((Or) association).getAssociation(num) instanceof LogicalOperator){}
//                                    else{}
//
//                                    GeneProductRef gpr = (GeneProductRef) ((Or) association).getAssociation(num);
//                                    GeneProduct geneProduct = (GeneProduct) association.getModel().getSBaseById(((GeneProductRef) gpr).getGeneProduct());
//                                    String displayId = geneProduct.getId();
//                                    if (!fcList.contains(displayId)){
//                                        fcList.add(displayId);
//                                        addFunctionalComponent(moduleDef,displayId,DirectionType.IN,uriPrefix);
//                                    }
//                                    Integer branchNum = num;
//                                    //add the first gene product
//                                    if(branchNum == 0){
//                                        interaction.createParticipation(displayId+"_fc",displayId+"_fc",SystemsBiologyOntology.PROTEIN_COMPLEX_FORMATION);
//                                    }
//                                    //branches of interaction for every other product
//                                    else if (branchNum >0){
//                                        createBranches(moduleDef,interaction,branchNum,displayIdList_reactants,displayIdList_products);
//                                    }
//                            }
//                        }
//                        if(association instanceof And){ //<fbc:and> (fbc:geneProductRef) (fbc:geneProductRef) <fbc:and>
//                            for(int num=0;num<((And) association).getAssociationCount();num++){
//                                if(((And) association).getAssociation(num) !=null){
//                                    Association product  = ((LogicalOperator) association).getAssociation(num);
//                                    GeneProduct geneProduct = (GeneProduct) association.getModel().getSBaseById(((GeneProductRef) product).getGeneProduct());
//                                    String displayId = geneProduct.getId();
//                                    if(!fcList.contains(displayId)){
//                                        fcList.add(displayId);
//                                        addFunctionalComponent(moduleDef,displayId,DirectionType.IN,uriPrefix);
//                                    }
//                                    interaction.createParticipation(displayId+"_fc",displayId+"_fc",SystemsBiologyOntology.PROTEIN_COMPLEX_FORMATION);
//                                }
//                            }
//
//                        }
//
//                    }
//
//
//                }
                masterList = getMasterListOfGeneProducts(association);
                for(int j=0; j<masterList.size(); j++){
                    for(GeneProduct gp : masterList.get(j)){//create FC for gene products
                        if(!fcList.contains(gp.getId())){
                            addFunctionalComponent(moduleDef,gp.getId(),DirectionType.IN,uriPrefix);
                            fcList.add(gp.getId());
                        }
                    }
                    Interaction branchInteraction = addInteraction(moduleDef,interactionIdPrefix+"_branch_"+j);
                    createInteractionParticipantions(SBOLdoc,branchInteraction,displayIdList_reactants,displayIdList_products,masterList.get(j),true);
                }
            }else if (!fbcReactionPlugin.isSetGeneProductAssociation()){
                Interaction interaction = addInteraction(moduleDef,interactionIdPrefix);
                createInteractionParticipantions(SBOLdoc,interaction,displayIdList_reactants,displayIdList_products,null, false);
            }


        }

        for(FunctionalComponent fc : moduleDef.getFunctionalComponents()){
            String fcID = fc.getDisplayId();
            String cdID = fcID.substring(0,fcID.length()-3);
            String name =SBOLdoc.getComponentDefinition(cdID,"2").getName();
            fc.setName(name);
        }


        System.out.println("fcList: "+fcList);
        SBOLWriter.write(SBOLdoc, "sbolOfYeast_8.xml");



    }

    public static void addComponentDefinition(SBOLDocument SBOLdoc, String displayId, URI type, URI base, String name) throws SBOLValidationException {
        ComponentDefinition CD = SBOLdoc.createComponentDefinition(base.toString(), displayId, "2", type);
        CD.setName(name);
    }

    public static Interaction addInteraction(ModuleDefinition moduleDef, String interactionId) throws SBOLValidationException {
        Interaction interaction = moduleDef.createInteraction(interactionId,SystemsBiologyOntology.BIOCHEMICAL_REACTION);
        return interaction;
    }

    public static FunctionalComponent addFunctionalComponent(ModuleDefinition moduleDef, String DisplayId, DirectionType direction, String uriBase) throws SBOLValidationException {
        String uri = uriBase+"/"+DisplayId;
        FunctionalComponent fc = moduleDef.createFunctionalComponent(DisplayId+"_fc",AccessType.PRIVATE, URI.create(uri),direction);
        return fc;
    }

    public static void createInteractionParticipantions(SBOLDocument SBOLdoc, Interaction branchInteraction, List<String> reactants, List<String> products, List<GeneProduct> geneList, Boolean isSetGPA) throws SBOLValidationException {
        for (String reactant :reactants) {
            Participation p = branchInteraction.createParticipation(reactant,reactant,SystemsBiologyOntology.REACTANT);
            p.setName(findName(cleanID(reactant),SBOLdoc));
        }
        for (String product : products){
            Participation p = branchInteraction.createParticipation(product,product,SystemsBiologyOntology.PRODUCT);
            p.setName(findName(cleanID(product),SBOLdoc));
        }
        if(isSetGPA){
            for (GeneProduct gene : geneList){
                String geneId = gene.getId();
                Participation p = branchInteraction.createParticipation(geneId+"_fc",geneId+"_fc",SystemsBiologyOntology.CATALYST);
                p.setName(findName(geneId,SBOLdoc));
            }
        }
        if(geneList == null){

        }

    }

    //Return the master list of all combination of gene products childlist enabling  a SBML reaction
    public static List<List<GeneProduct>> getMasterListOfGeneProducts(org.sbml.jsbml.ext.fbc.Association association){
        List masterList = new ArrayList();
        if( association instanceof LogicalOperator){
            List<org.sbml.jsbml.ext.fbc.Association> subAsso = ((LogicalOperator) association).getListOfAssociations();

            if(association instanceof And){ //Assume only two layers of logical operators, only one logical in the second layer
                List<GeneProduct> singleGeneList = new ArrayList();
                List<org.sbml.jsbml.ext.fbc.Association> tempList= new ArrayList();
                Boolean isSetSecondLogical = false;
                for (org.sbml.jsbml.ext.fbc.Association asso : subAsso){
                    if (asso instanceof Or){//can't be And under And, so ignore And cases
                        List<org.sbml.jsbml.ext.fbc.Association> subSubAsso = ((Or) asso).getListOfAssociations();
                        tempList = subSubAsso;
                        isSetSecondLogical = true;
                    }else if (asso instanceof GeneProductRef){
                        singleGeneList.add(((GeneProductRef) asso).getGeneProductInstance());
                    }
                }
                if(isSetSecondLogical){
                    for(int i = 0; i < tempList.size(); i++){
                        GeneProduct gp = ((GeneProductRef) tempList.get(i)).getGeneProductInstance();
                        List<GeneProduct> childList = new ArrayList();
                        childList.addAll(singleGeneList);
                        childList.add(gp);
                        masterList.add(childList);
                    }
                }else if (!isSetSecondLogical){
                    masterList.add(singleGeneList);//No logical operator splits
                }
            }

            if(association instanceof Or){
                for (org.sbml.jsbml.ext.fbc.Association asso : subAsso){
                    if (asso instanceof GeneProductRef){
                        List<GeneProduct> childList = new ArrayList();
                        childList.add(((GeneProductRef) asso).getGeneProductInstance());
                        masterList.add(childList);//a single gene product enable the reaction
                    }else if (asso instanceof And){//do not consider Or under Or
                        List<GeneProduct> childList = new ArrayList();
                        for (int i=0; i<asso.getChildCount();i++){
                            GeneProduct gp = ((GeneProductRef) ((And) asso).getAssociation(i)).getGeneProductInstance();//Assume GPR under And, no more Or
                            childList.add(gp);
                        }
                        masterList.add(childList);
                    }
                }
            }
        }

        if(association instanceof GeneProductRef){ // Only one gene product under this SBML reaction
            List<GeneProduct> childList = new ArrayList();
            GeneProduct gp = ((GeneProductRef) association).getGeneProductInstance();
            childList.add(gp);
            masterList.add(childList);
        }


        return masterList;
    }

    public static String findName(String displayID, SBOLDocument sbol){
        System.out.println("finding name of"+ displayID);
        String name = sbol.getComponentDefinition(displayID,"2").getName();
        return name;
    }

    public static String cleanID(String ID){
        String cleanID = new String();
        if (ID.endsWith("_fc")){
             cleanID = ID.substring(0,ID.length()-3);
        }else{
            cleanID = ID;
        }
        return cleanID;
    }
}
