import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.io.GenbankSequenceParser;

import java.io.*;
import java.util.*;

public class createArtificialGenBank {
    public static void main( String[] args ) throws IOException {
        String Path = new File("").getAbsolutePath();
        String genomeDir=Path+"/genome_GenBank/";

        File folder = new File(genomeDir);
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            BufferedReader br  = new BufferedReader(new FileReader(file));
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
                }
            }
            System.out.println("Reading chromosome file "+file.getName());
        }
        getIdCopyMap_Artificial("dirEvo_TopPatterns.csv");
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
}
