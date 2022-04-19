package org.example;

import org.apache.commons.cli.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class Main {
/*
TODO: сделать обработку файлов по маске
* */


    public static List<String> expression = new ArrayList<>(); // Expressions that should be found in text
    public static List<String> filePath = new ArrayList<>(); // Path to file or directory's

    /**
     * Display usage information
     */
    public static void printHelp(Options options){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("This is a simple little grep - Search for [Expression] in the input [File]\n" +
                    "Usage: grep [-n] [-f File] [Expression] [File]",options);
            System.exit(0);
        }
    /**
     * Build Options and return it
     * @return Options object containing lis of defined options
     */
    public static Options optionBuilder(){
        Options options = new Options();

        options.addOption("A","after-contex",true,"Print num lines of leading context after each match.  See also the -A and -C options");
        options.addOption("B","before-context",true,"Print num lines of leading context before each match.  See also the -A and -C options");
        options.addOption("C","context",true,"Print num lines of leading and trailing context surrounding each match.  The default is 2 and is equivalent to -A 2 -B 2.");
        options.addOption("e","regexp",true, """
                            Specify a pattern used during the search of the input: an input
                            line is selected if it matches any of the specified patterns.
                            This option is most useful when multiple -e options are used to
                            specify multiple patterns, or when a pattern begins with a dash
                            (`-').""");
        options.addOption("f","file",true,"File path to a source file");
        options.addOption("help",false,"Print a brief help message.");
        options.addOption("n","line-number",false, """
                             Each output line is preceded by its relative line number in the
                             file, starting at line 1.  The line number counter is reset for
                             each file processed.
                             is specified.""");
        options.addOption("H",false,"Always print filename headers with output lines");
        options.addOption("h","no-filename",false,"Never print filename headers (i.e. filenames) with output lines.");
        options.addOption("m","max-count",true,"Stop reading the file after specified matches.");
        options.addOption("r","recursive",false,"Recursively search subdirectories listed.");
        return options;
    }

    /**
     * Tries to parse input args with the created list of Options
     * USes CLI and manual handling of CLI unparsed arguments
     * Throws an exception if there is an error parsing all args
     * @param options list of defined options to parse
     * @param args args list that need to be parsed
     * @return CommandLineParser object with parsed Options.
     */
    public static CommandLine optionParser(Options options, String[] args){
        CommandLineParser parser = new DefaultParser();

        try {
            //Try to parse defined options and arguments
            CommandLine cLine = parser.parse(options, args);

            //TODO:move help initialisation to proper class
            if(cLine.hasOption("h")){
                printHelp(options);
            }

            //TODO refactor code of saving values below
            //TODO check if it's possible to parse quoted values
            if(cLine.hasOption("f")){
                String[] optValues = cLine.getOptionValues("f");
                for (String v: optValues){
                    if(v.startsWith("'") && v.endsWith("'") || v.startsWith("\"") && v.endsWith("\"")){
                        v = v.substring(1, v.length()-1);
                    }
                    filePath.add(v);
                }
            }

            if(cLine.hasOption("e")){
                String[] optValues = cLine.getOptionValues("e");
                for (String v: optValues){
                    if(v.startsWith("'") && v.endsWith("'") || v.startsWith("\"") && v.endsWith("\"")){
                        v = v.substring(1, v.length()-1);
                    }
                    expression.add(v);
                }
            }

            String[] leftArgs = cLine.getArgs();
            if (leftArgs.length>0){ //if there are unparsed args
                /* handle left args
                 * waiting for:
                 * [Expression] and [File1] [file2] [...]
                 * -e [Expression] -e [Expression2] [File1] [file2] [...]
                 *  last option can be only file
                 * if -f option is defined then -e must be also defined and all left args should be files
                 * check there is args
                 * if - e is not defined then take first param and append it until you get a quoted param
                 *  if no args -e and -f must be defined
                 */
                boolean hasOptionExpr = cLine.hasOption("e");
                char delimiter = 'n';
                String exprBuilder = "";

                for (String leftArg : leftArgs) { //for each left arg

                    if (delimiter == 'n') {
                        if (leftArg.startsWith("'") || leftArg.startsWith("\"")) { //value has quotes
                            delimiter = leftArg.charAt(0);
                            if (leftArg.endsWith(String.valueOf(delimiter))){
                                delimiter = 'n';
                                //Save arg
                                if (!hasOptionExpr) {
                                    expression.add(leftArg.substring(1,leftArg.length()-1));
                                    hasOptionExpr = true;
                                } else {
                                    filePath.add(leftArg.substring(1,leftArg.length()-1));
                                }
                            }
                            else {
                                exprBuilder = leftArg.substring(1); //remove first quote
                            }
                        } else {   //Save the arg that has no quotes
                            if (!hasOptionExpr) {
                                expression.add(leftArg);
                                hasOptionExpr = true;
                            } else {
                                filePath.add(leftArg);
                            }
                        }
                    } else {   //if arg is part of quoted delimited arg
                        if (leftArg.endsWith(String.valueOf(delimiter))) { //it is a tail
                            exprBuilder = exprBuilder.concat(" " + leftArg.substring(0, leftArg.length() - 1));
                            delimiter = 'n';
                            //Save arg
                            if (!hasOptionExpr) {
                                expression.add(exprBuilder);
                                hasOptionExpr = true;
                            } else {
                                filePath.add(exprBuilder);
                            }
                        } else {   //it is a middle part
                            exprBuilder = exprBuilder.concat(" " + leftArg);
                        }
                    }
                }
                if (delimiter!='n') { //Check that there is no unparsed params
                    throw new ParseException ("Error parsing argument "+ exprBuilder);
                }
            }
            else if (!cLine.hasOption("e") && expression.size()==0 || !cLine.hasOption("f") && filePath.size()==0){
                throw new ParseException ("At least one expression or file must be defined");
            }

            return cLine;
        }
        catch (ParseException e) {
            System.err.println("Unexpected exception while parsing args: " + e.getMessage());
            e.printStackTrace();
            System.exit(0); //TODO check err exit value
        }
        return null;
    }

    public static void printResult(String string, int matchLineNumber, File file,CommandLine commandLine) {
        printResult(string, matchLineNumber, file, commandLine,false);
    }

    /***
     * Prints matching line or context lines
     * for match: filePath//fileName:[matchlineNumber]:[string]
     * for contect: filePath//fileName-[matchlineNumber]-[string]
     * @param string - String that should be print
     * @param matchLineNumber - line index number
     * @param file - file that have a matching line
     * @param isContext - if true then print a context format
     * @param commandLine - CommandLine Class that contains Options
     */
    public static void printResult(String string, int matchLineNumber, File file, CommandLine commandLine, boolean isContext) {
        // print filepath and filename if there is more than one file or have a specified option
        if (!commandLine.hasOption("h") && (filePath.size() > 1 || commandLine.hasOption("H"))) {
            if (commandLine.hasOption("r")) {
                System.out.print(file.getPath().replace(file.getName(),"")+ "/");
            }
            System.out.print(file.getName());
            //print delimiter
            if (isContext) {
                System.out.print("-");
            } else {
                System.out.print(":");
            }
        }
        // print matching line
        if (commandLine.hasOption("n")) { // print line number
            System.out.print(matchLineNumber);

            //print delimiter
            if (isContext) {
                System.out.print("-");
            } else {
                System.out.print(":");
            }
        }
        //print content
        System.out.println(string);
    }
    /***
     * Searches expr through the file and print matches depend on CommandLine options
     * @param fileContentList - file
     * @param commandLine - CommandLine with parsed options
     * @param file - File name
     */
    public static void exprFinder(List<String> fileContentList, CommandLine commandLine, File file) {

        ListIterator <String> fileContentIterator = fileContentList.listIterator();
        boolean match = false;
        int lineNum = 0;
        int resultCount=0;
        int ResultLimit = -1;

        int afterContextSize = 0;
        int beforeContextSize = 0;
        List<String>  afterContext = Collections.emptyList();
        List<String>  beforeContext = Collections.emptyList();

        //define before and after context size
        if(commandLine.hasOption("A")){
            afterContextSize = Integer.parseInt(commandLine.getOptionValue("A"));
        }
        if(commandLine.hasOption("B")){
            beforeContextSize = Integer.parseInt(commandLine.getOptionValue("B"));
        }
        if (commandLine.hasOption("C")){
            afterContextSize = beforeContextSize = Integer.parseInt(commandLine.getOptionValue("C"));
        }
        // define output occurrences limit
        if (commandLine.hasOption("m")){
            ResultLimit = Integer.parseInt(commandLine.getOptionValue("m"));
        }
        //while there is next element and limit to ResultLimit
        while (fileContentIterator.hasNext() && (ResultLimit ==-1 || resultCount <= ResultLimit)) {

            String line = fileContentIterator.next();
            //search for expressions in current line
            for (String expr : expression) {
                if (line.contains(expr)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                resultCount++;
                //print before context
                // while there is previous index in list(there is at least one) and while not exceed context size
                if(beforeContextSize>0){
                    System.out.println("--");
                    ListIterator<String> beforeContextIterator = fileContentList.listIterator(fileContentIterator.previousIndex());
                    for(int i=0; beforeContextIterator.hasPrevious() && i < beforeContextSize;i++){
                        printResult(beforeContextIterator.previous(),lineNum,file,commandLine,true);
                    }
                }
                //print match
                printResult(line,lineNum,file,commandLine);
                //print after context
                //while there is next index in list(check there is one) and while not exceed context size, start from a current position
                if (afterContextSize >0){
                    if (fileContentIterator.hasNext()){
                        ListIterator<String> afterContextIterator = fileContentList.listIterator(fileContentIterator.nextIndex());
                        for(int i=0; afterContextIterator.hasNext() && i<afterContextSize;i++){
                            printResult(afterContextIterator.next(),lineNum,file,commandLine,true);
                        }
                    }
                    System.out.println("--");
                }
                match = false;
            }
            lineNum++;
        }
        /*
        if(resultCount == 0 && !commandLine.hasOption("h")){
            System.out.println(file.getPath()+"/"+file.getPath()+" matches");
        }
        */
    }

    public static void main(String[] args) {
        //args = new String[] {"-n","-C","2","-f","\"/Users/admin/downloads/Test/\"", "\'(i)'","/Users/admin/IdeaProjects/grep/src/main/resources/testgrep.txt" };
        CommandLine cLine = optionParser(optionBuilder(),args); //try to parse option and build the option list

        ListIterator<String> fileIterator = filePath.listIterator();

        while (fileIterator.hasNext()){

            String fPath = (String) fileIterator.next();
            File file = new File(fPath);
            if(!file.exists()){
                System.out.println("File " + fPath + " does not exist.");
            }
            else {
                if(file.isDirectory()){ //handle file is a directory
                    if (!cLine.hasOption("r")){
                        System.out.println(file + ": Is a directory");
                    }
                    else{   //handle adding new files from directory
                        for(String fList : Objects.requireNonNull(file.list())){ //add founded new file and return iterator to handle it
                            fileIterator.add(file +"/" + fList);
                            fileIterator.previous();
                        }
                    }

                }
                else { //handle a file
                    try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                        List<String> fileContentList = fileReader.lines().collect(Collectors.toList());
                        exprFinder(fileContentList,cLine,file);
                    }
                catch (IOException e) {
                    System.err.println("File error " + e.getMessage());
                    e.printStackTrace();
                    }
                }
            }
        }
    }
}


/*
* ctrl + j = doc
* com opt v vars
* */
