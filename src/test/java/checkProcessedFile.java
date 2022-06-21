import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class checkProcessedFile {
    public static void main( String[] args ) throws IOException {
        String fileName = "processed_output_boolNet.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
//                Integer orNum = count(" or ",line);
//                if(orNum >4){
//                    System.out.println("Long line: "+line);
//                }
//
//                if(count("\\(",line)!=count("\\)",line)){
//                    System.out.println("unmatched parenthesis in "+line);
//                }

                if(count("and",line)>5){
                    System.out.println("Long and: "+line);
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

}
