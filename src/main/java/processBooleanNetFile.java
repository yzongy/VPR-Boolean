import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Add intermediate nodes to split rules with many regulators that slows down pystablemotifs
public class processBooleanNetFile {
    public static void main( String[] args ) throws IOException {
        String fileName = "BottomStrain_GRNbased.txt";

        //create a new file
        File dest = new File("processed_"+fileName);

        //Read original file and write the new file
        try(PrintWriter pw = new PrintWriter(new FileWriter(dest),false)) {
            try (BufferedReader touch = new BufferedReader(new FileReader(fileName))) {
                if(touch.readLine().contains("GRN")){//genbank based on GRN model
                    touch.close();
                    try (BufferedReader br = new BufferedReader(new FileReader(fileName))){
                        String line;
                        while ((line = br.readLine()) != null) {
                            Integer orNum = count(" or ",line);
                            if(orNum >5){
                                System.out.println(line);
                                printProcessedLine(pw,splitline(line));
                            }else{
                                pw.println(line);
                            }
                        }
                    }
                }else{// genbank based on FBC model
                    try (BufferedReader br = new BufferedReader(new FileReader(fileName))){
                        String line;
                        while ((line = br.readLine()) != null) {
                            Integer orNum = count("\\) or \\(",line);
                            ArrayList<String> nodes = splitLine_fbc(line);
                            if(orNum > 4){
                                System.out.println(line);
                                System.out.println("split: "+ nodes);
                                printProcessedLine_fbc(pw,line,nodes);
                            }else{
                                pw.println(line);
                            }
                        }
                    }
                    printAndNodesProcessedLines(dest);
                }
            }
        }
    }

    public static int count(String word, String line){
        Pattern pattern = Pattern.compile(word);
        Matcher matcher = null;
        if(line.contains("and not")){
            String subString = StringUtils.substringBetween(line,"*=","and not");
            matcher = pattern.matcher(subString);
        }else{
            matcher = pattern.matcher(line);
        }

        int counter = 0;
        while (matcher.find())
            counter++;
        return counter;
    }

    public static int countOrInAndNot(String or, String andNot){
        Pattern pattern = Pattern.compile(or);
        Matcher matcher = null;
        matcher = pattern.matcher(andNot);
        int counter = 0;
        while (matcher.find())
            counter++;
        return counter;
    }

    public static ArrayList<String> splitline(String line){
        ArrayList<String> splitLine = new ArrayList<String>();
        String[] splited = null;
        if (line.contains("and not")){
            String subString = StringUtils.substringBetween(line,"*=","and not");
            splited = subString.split("\\s+");
        }else{
            splited = line.split("\\s+");
        }

        //find split sites
        ArrayList<Integer> splitSites = new ArrayList<Integer>();
        splitSites.add(0);
        Integer count = 0;
        for(int i = 0; i< splited.length; i++){
            if (splited[i].equals("or")){
                count ++;
                if(count%4 ==0 && count>3){
                    splitSites.add(i);
                }
            }
        }

        //chop line and add parts to splitLine
        String[] breakLine = line.split("\\s+");
        List<String> wordList = new ArrayList<String>();
        wordList = Arrays.asList(breakLine);
        for(int i = 0; i<splitSites.size()-1; i++){
            Integer site = splitSites.get(i);
            String fuse  = fuseString(wordList.subList(site,splitSites.get(i+1)));
            splitLine.add(fuse);
        }
        Integer lastSite = splitSites.get(splitSites.size()-1);

        if(wordList.contains("and")){
            String last = fuseString(wordList.subList(lastSite, wordList.indexOf("and")));
            splitLine.add(last);
            splitLine.add(fuseString(wordList.subList(wordList.indexOf("and"), wordList.size())));
        }else{
            String last = fuseString(wordList.subList(lastSite, wordList.size()));
            splitLine.add(last);
        }

        System.out.println("splitLine: "+splitLine);

        return splitLine;
    }

    public static String fuseString(List<String> wordList){
        String fuse = wordList.get(0);
        for(int i =1; i<wordList.size(); i++){
            fuse = fuse+" "+wordList.get(i);
        }
        return fuse;
    }

    public static void printProcessedLine(PrintWriter pw, ArrayList<String> splitLine){
        pw.write(splitLine.get(0));
        ArrayList<String> newNameList = new ArrayList<String>();
        ArrayList<String> interList = new ArrayList<String>();
        ArrayList<String> andNotList = new ArrayList<String>();
        String gene = splitLine.get(0).substring(0,splitLine.get(0).indexOf("*"));
        for (int i =1; i<splitLine.size(); i++){
            if(splitLine.get(i).startsWith("or")){
                String newName = gene+"_intermediate_" + i;
                String intermediate = splitLine.get(i).substring(3);
                newNameList.add(newName);
                interList.add(intermediate);
            }else if(splitLine.get(i).startsWith("and not")){
                String andNot = splitLine.get(i);
                andNotList.add(andNot);
            }
        }

        for(String newName : newNameList){
            pw.write(" or ("+newName+")");
        }
        if(!andNotList.isEmpty()){
            System.out.println("andNotList: "+andNotList);
            for(String andNot: andNotList){//assume only one andNot rule here
                if(countOrInAndNot(" or ",andNot)>1){
                    pw.write(" and not ("+gene+"_andNotIntermediate)");
                    pw.write("\n");
                    if(countOrInAndNot(" or ",andNot)>5){ // in case a long andNot list, split it further
                        printLongAndNot(pw, andNot, gene);
                    }else{ // if not a long andNot list just write it straight away
                        pw.write(gene+"_andNotIntermediate*="+ andNot.substring(7));
                    }
                }else{
                    pw.write(" "+andNot);
                }
            }
        }
        pw.write("\n");

        for(int i = 0; i< interList.size();i++){
            pw.write(newNameList.get(i)+"*= "+interList.get(i));
            pw.write("\n");
        }
    }

    public static ArrayList<String> splitLine_fbc(String fbcLine){
        ArrayList<String> line = new ArrayList<String>();
//        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Pattern pattern = Pattern.compile("\\(([^()]*|\\([^()]*\\))*\\)");
        Matcher matcher = pattern.matcher(fbcLine);
        while(matcher.find()){
            for(int i = 0; i<matcher.groupCount();i++){
                line.add(matcher.group(i));
            }
        }
        if(line.isEmpty()){
            System.out.println("No matched brackets in "+fbcLine);
        }
        return line;
    }

    public static void printProcessedLine_fbc(PrintWriter pw,String line, ArrayList<String> splitLine_fbc){
        String start = line.substring(0, line.indexOf("*"));

        //replace multi nodes in a parenthesis with a lowerLevel node
        HashMap<String,String> lowerLevel = new HashMap<String,String>();
        Integer lowerLevelCount = 0;
        for(int i=0;i<splitLine_fbc.size();i++){
            if(splitLine_fbc.get(i).contains(" or ") |splitLine_fbc.get(i).contains(" and ")){
                lowerLevelCount++;
                lowerLevel.put(start+"_lowerLevel_"+lowerLevelCount, splitLine_fbc.get(i));
                splitLine_fbc.set(i,"("+start+"_lowerLevel_"+lowerLevelCount+")");
            }
        }
        if(!lowerLevel.isEmpty()){
            for(String key : lowerLevel.keySet()){
                pw.write(key+"*= " +lowerLevel.get(key)); // prefix_lowerLevel_1*= A and B and C and D and E and F and G
                pw.write("\n");
            }
        }

        //process long lines with many nodes, use super level and higher level nodes
        if(splitLine_fbc.size()>12){
            Integer max = 6;
            Integer interCount = 0;
            for(int i = 0; i< splitLine_fbc.size();i++){//add intermediate nodes
                if(i%max==0){
                    interCount++;
                    pw.write(start+"_intermediate_"+interCount+"*= "+splitLine_fbc.get(i));
                    for(int j=1;j<max;j++){
                        if(i+j< splitLine_fbc.size()){
                            pw.write(" or "+splitLine_fbc.get(i+j));
                        }
                    }
                    pw.write("\n");
                }
            }
            if(interCount>max){// too many inetrmediates then add higher level of intermediates
                Integer higherCount = 0;
                for(int i =1;i<=interCount;i++){ //add higher level intermediate nodes
                    if(i%max==1){
                        higherCount++;
                        pw.write(start+"_higherLevel_intermediate_"+higherCount+"*= "+ start+"_intermediate_"+i);
                        for(int j=1;j<max;j++){
                            if(i+j<interCount){
                                pw.write(" or "+start+"_intermediate_"+(i+j));
                            }
                        }
                        pw.write("\n");
                    }
                }
                if(higherCount<=max){
                    pw.write(start+"*= "+ start+"_higherLevel_intermediate_1");// add the top level
                    for(int i = 2; i<=higherCount;i++){
                        pw.write(" or "+ start+"_higherLevel_intermediate_"+i);
                    }
                    pw.write("\n");
                }else if(higherCount> max){//in case extreme situation that too many higherLevel nodes, split into 2 superLevel nodes
                    pw.write(start+"_superLevel_intermediate_1*= "+ start+"_higherLevel_intermediate_1");
                    for(int i=2; i<=max; i++){
                        pw.write(" or "+start+"_higherLevel_intermediate_"+i);
                    }
                    pw.write("\n");

                    pw.write(start+"_superLevel_intermediate_2*= "+ start+"_higherLevel_intermediate_"+(max+1));
                    for(int i=max+2; i<=higherCount;i++){
                        pw.write(" or "+start+"_higherLevel_intermediate_"+i);
                    }
                    pw.write("\n");

                    pw.write(start+"*= "+ start+"_superLevel_intermediate_1"+" or "+start+"_superLevel_intermediate_2");
                    pw.write("\n");
                }

            }else if(interCount<=max){// if not too many intermediates, just use one level of intermediates
                pw.write(start+"*= "+ start+"_intermediate_1");
                for(int i =2; i<=interCount;i++){
                    pw.write(" or "+ start+"_intermediate_"+i);
                }
                pw.write("\n");
            }
        }

        //process relatively short lines with intermediate (and higherLevel) nodes
        if(splitLine_fbc.size()<=12){
            ArrayList<Integer> temp = new ArrayList<>();
            pw.write(start+ "*= "+ splitLine_fbc.get(0));
            for (int i = 1; i<splitLine_fbc.size(); i++){
                if(i<4){
                    pw.write(" or "+splitLine_fbc.get(i));
                }else if(i%4==0){
                    pw.write(" or "+start+"_intermediate_"+ i/4);
                    temp.add(i);
                }
            }
            pw.write("\n");
            for(int tempIndex = 0; tempIndex<temp.size();tempIndex++){
                pw.write(start+"_intermediate_"+(tempIndex+1)+"*=");
                pw.write(" "+ splitLine_fbc.get(temp.get(tempIndex)));
                for(int j = 1; j<4; j++){
                    if(temp.get(tempIndex)+j< splitLine_fbc.size()){
                        pw.write(" or "+ splitLine_fbc.get(temp.get(tempIndex)+j));
                    }
                }
                pw.write("\n");
            }
        }
    }

    //from (A and B and C and D and E and F and G) to (prefix_bottomLevel_1 and prefix_bottomLevel_2)
    public static HashMap<String,String> splitLine_fbc_AndNodes(String prefixLowerLevel, String andNodes){
        String[] nodes = andNodes.split(" and ");
        HashMap<String, String> bottomNodesMap = new HashMap<String,String>();
        Integer num = 0;
        for(int i=0; i<nodes.length; i++){
            if(i%5 == 0){
                num++;
                String bottomNodes = new String();
                bottomNodes=bottomNodes+nodes[i];
                for(int count=1;count<5;count++){
                    if(i+count<nodes.length){
                        bottomNodes=bottomNodes+" and "+nodes[i+count];
                    }
                }
                System.out.println("bottomLevel and Nodes: "+bottomNodes);
                bottomNodesMap.put(prefixLowerLevel+"_bottomLevel_"+num,bottomNodes);
            }
        }
        System.out.println("splited AND nodes of: "+bottomNodesMap);
        System.out.println("Original line: "+andNodes);
        return bottomNodesMap;
    }

    public static void printAndNodesProcessedLines(File origin) throws IOException {
        File temp = new File("tempFile.txt");

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp), false)) {
            try (BufferedReader br = new BufferedReader(new FileReader(origin))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if(count(" and ",line)>5){
                        String start = line.substring(0, line.indexOf("*"));
                        String nodes = line.substring(line.indexOf("(")+1, line.indexOf(")"));
                        HashMap<String,String> bottomNodesMap = splitLine_fbc_AndNodes(start,nodes);
                        String[] keyArray = bottomNodesMap.keySet().toArray(new String[bottomNodesMap.keySet().size()]);
                        //write the lowerLevel nodes
                        pw.write(start+"*= ("+ keyArray[0]);
                        for(int i=1;i<keyArray.length;i++){
                            pw.write(" and "+keyArray[i]);
                        }
                        pw.write(")");
                        pw.write("\n");
                        //write the bottomLevel nodes
                        for(String key: bottomNodesMap.keySet()){
                            pw.write(key+"*= ("+bottomNodesMap.get(key)+")"+"\n");
                        }
                    }else{
                        pw.write(line+"\n");
                    }
                }

            }
        }
        origin.delete();
        temp.renameTo(origin);
    }

    private static void printLongAndNot(PrintWriter pw, String andNot, String gene){
        String cleanLine = andNot.substring(8).replaceAll("[()]", "");
        String[] splited = cleanLine.split(" or ");
        Integer numAndNotGene = splited.length;
        ArrayList<String> andNotLowerList = new ArrayList<String>();
        //break the long line and store in a list
        for(int i = 0; i<numAndNotGene;i++){
            String andNotLower = "";
            if(i%4 == 0){
                andNotLower = splited[i];
                for(int j = 1; j < 4;j++){
                    if(i+j<numAndNotGene){
                        andNotLower = andNotLower+" or "+splited[i+j];
                    }
                }
                andNotLowerList.add(andNotLower);//[ "A or B or C or D" , "E or F or G or H" ]
            }
        }

        //write andNotIntermediate*= lowerIntermediate_1 + lowerIntermediate_2
        Integer numOfLower = andNotLowerList.size();
        String line = gene+"_andNotIntermediate*= " + gene+"_andNotLowerIntermediate_1";
        for(int i=2; i<=numOfLower; i++){
            line = line +" or "+gene+"_andNotLowerIntermediate_"+i;
        }
        pw.write(line);
        pw.write("\n");

        //write andNotLowerIntermediate_1 = (A) or (B) or (C) or (D)
        for(int i=0; i<numOfLower; i++){
            String lower  = gene+"_andNotLowerIntermediate_"+(i+1)+"*= (";
            lower = lower + andNotLowerList.get(i);
            pw.write(lower+")");
            //skip the last "\n" (there is one outside this method)
            if(i == numOfLower-1){

            }else{
                pw.write("\n");
            }
        }
    }
}
