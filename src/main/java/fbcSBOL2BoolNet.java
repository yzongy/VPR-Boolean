//Designed for constructing BooleanNet file from the wildtype fbc model as a special case
import org.sbolstandard.core2.*;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class fbcSBOL2BoolNet {

    public static void main(String [] args) throws SBOLValidationException, IOException, SBOLConversionException {
        InputStream inputstream = new FileInputStream("sbolOfYeast_8.xml");
        SBOLDocument sbolDoc = new SBOLReader().read(inputstream);
        System.out.println("Number of Components:" + sbolDoc.getComponentDefinitions().size());
        URI moduleURI = URI.create("https://synbiohub.org/user/zyang22/exampleFBC/2");
        URI productURI = URI.create("http://identifiers.org/biomodels.sbo/SBO:0000011");
        URI reactantURI = URI.create("http://identifiers.org/biomodels.sbo/SBO:0000010");
        URI geneProductURI = URI.create("http://identifiers.org/biomodels.sbo/SBO:0000526");


        File boolNet = new File("/Users/yzongy/vpr2/virtualparts.boolean/boolNet.txt");
        List<List<String>> productReactants = new ArrayList(); // [[product1,reactanta,reactantb];[product2, reactantc,reactantd]]
        try(PrintWriter pw = new PrintWriter(new FileWriter(boolNet),false)){
            pw.println("#BoolNet of Yeast_8");

            for (Interaction interaction : sbolDoc.getModuleDefinition(moduleURI).getInteractions()) {

                //Store info into lists
                List<String> tempProductID = new ArrayList();
                List<String> tempReactantID = new ArrayList();
                List<String> tempGeneProductID = new ArrayList();
                for (Participation participation : interaction.getParticipations()) {
                    if (participation.getRoles().contains(productURI)) {
                        tempProductID.add(participation.getDisplayId());
                    } else if (participation.getRoles().contains(reactantURI)) {
                        tempReactantID.add(participation.getDisplayId());
                    } else if (participation.getRoles().contains(geneProductURI)) {
                        tempGeneProductID.add(participation.getDisplayId());
                    }
                }
                //Store all interactions in a big list
                for (String product : tempProductID) {
                    List<String> anInteraction = new ArrayList();
                    anInteraction.add(0, product);
                    anInteraction.addAll(tempReactantID);
                    anInteraction.addAll(tempGeneProductID);
                    productReactants.add(anInteraction);
                }
            }

            //Write the file
            List<String> pList = new ArrayList<String>();
            for (int i = 0; i < productReactants.size(); i++){
                Boolean multipleProducts = false;
                String p = productReactants.get(i).get(0);
                if(pList.contains(p)){
                    continue;
                }else{
                    pList.add(p);
                }
                Integer count = 0;
                List<List<String>> listofExtraTempllists  =new ArrayList();
                for (List<String> tempList : productReactants){
                    if(p.equals(tempList.get(0))){
                        count++;
                        if(count>1){
                            listofExtraTempllists.add(tempList);
                        }//store the multiple interactions related to this product
                    }
                    if(count>1){//two or more templist starts with this product
                        multipleProducts = true;
                    }
                }


                pw.println();
                pw.print(p+"*= ");
                if(multipleProducts){
                    pw.print(" (");
                    for(int num = 1; num<productReactants.get(i).size(); num++){//write the first interaction of this product
                        pw.print(productReactants.get(i).get(num));
                        if((num != productReactants.get(i).size()-1)&&(productReactants.get(i).get(num+1)!=null)){
                            pw.print(" and ");
                        }else{
                            pw.print(") ");
                        }
                    }
                    for(List<String> tempList : listofExtraTempllists){
                        pw.print("or (");
                        for(int num=1; num<tempList.size(); num++){
                            pw.print(tempList.get(num));
                            if((num != tempList.size()-1)&&(tempList.get(num+1)!=null)){
                                pw.print(" and ");
                            }else{
                                pw.print(") ");
                            }
                        }
                    }
                }else if (!multipleProducts){// no other interactions with the same product
                    for(int num = 1; num<productReactants.get(i).size(); num++){
                        pw.print(productReactants.get(i).get(num));
                        if((num != productReactants.get(i).size()-1)&&(productReactants.get(i).get(num+1)!=null)){
                            pw.print(" and ");
                        }
                    }
                }

            }


            pw.flush();
            pw.close();
        }
    }

}
