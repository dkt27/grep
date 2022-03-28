package org.example;

//import org.apache.commons.cli.Option;

import java.io.*;

public class Main {

    public String findPatern(String str, String patern){
        if(str.contains(patern)) {
            return str;
        }
        else{
            return null;
        }
    }
    public static void main(String[] args) {
      //  Option.builder().argName("c").longOpt("return-count")

        //simple params parser
        String searchPatern ="";
        String filePath = "";
        /*for(int i=0;i<args.length;i++){
            if(args[i] == "-f" || args[i] == "--f" || args[i] == "-file" ) {
                filePath = args[i + 1];
                i++;
                continue;
            }
            else
            {
                searchPatern = args[i];
            }
        }*/
        if(args.length<1){
            System.err.println("Invalid parameters nunber");
        }
        else{
            searchPatern = args[0];
            searchPatern = searchPatern.replaceAll("[']","");
            filePath = args[1];
            filePath = filePath.replaceAll("[']","");
            //debug
            System.out.println("debug filepath is " + filePath);
            System.out.println("searchPatern = " + searchPatern);
        }


        File file = new File(filePath);
        if(!file.exists()){
            System.err.println("File [path] does not exist.");
        }
        else {
            try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        if(line.contains(searchPatern)){
                            System.out.println(line);
                        }
                    }

                } catch (IOException e){
                    System.err.println("File error " + e.getMessage());
                    e.printStackTrace();
                }
            }


        }

    }


/*
* ctrl + j = doc
* com opt v vars
* */
