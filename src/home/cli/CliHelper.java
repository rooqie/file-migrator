package home.cli;

import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import org.apache.commons.cli.*;

import static java.lang.System.out;
import static java.lang.System.exit;

/**
 * Created by general on 2016/11/1.
 */
public class CliHelper {
    private static Options options = new Options();
    private static InvocationMode mode = null;

    private static String inputFile = null;
    private static String outputFile = null;
    private static String targetPath = null;


    private static void prepare(){
        Option targetPath = new Option("p", "path", true, "Folder to walk through.");
        Option outputFile = new Option("o", "output", true, "Create a database file with a given name or append to existing one.");
        Option inputFile = new Option("i", "input", true, "Read database entries from input file and compare to an output file or a dir tree.");
        Option help = new Option("h", "Print this message.");

        help.setLongOpt("help");

        targetPath.setArgs(1);
        targetPath.setArgName("DIR");
        outputFile.setArgs(1);
        outputFile.setArgName("DB_OUT");
        inputFile.setArgs(1);
        inputFile.setArgName("DB_IN");

        options.addOption(targetPath);
        options.addOption(outputFile);
        options.addOption(inputFile);
        options.addOption(help);

    }


    public static void initFromCommandLine(String[] args){
        // DEFINITION
        prepare();

        // PARSING
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = null;
        try {
            cmdLine = parser.parse(options,args);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        //INTERROGATION

        if (cmdLine.hasOption("help")){
            showHelp();
        } else if (cmdLine.hasOption('i')){
            inputFile = cmdLine.getOptionValue('i');
            // if we have input file we have to give something to check against so we must have either -o or -p but not both.
            if ((cmdLine.hasOption('o') && !cmdLine.hasOption('p')) || (cmdLine.hasOption('p') && !cmdLine.hasOption('o'))) {
                outputFile = cmdLine.getOptionValue('o');
                targetPath = cmdLine.getOptionValue('p');
                if (outputFile != null){
                    mode = InvocationMode.COMPARE_FILE_FILE;
                } else { // output file was not given, so we must have a target path.
                    mode = InvocationMode.COMPARE_FILE_FOLDER;
                }
            }
        } else if (!cmdLine.hasOption('p')) {
            // nothing to work with: we are not comparing & we are not reading
            showHelp();
        } else {
            if (!cmdLine.hasOption('o')){
                targetPath = cmdLine.getOptionValue('p');
                mode = InvocationMode.READ_DEFAULT;
            } else {
                targetPath = cmdLine.getOptionValue('p');
                outputFile = cmdLine.getOptionValue('o');
                mode = InvocationMode.READ_TO_FILE;
            }
        }
        }

    private static void showHelp(){
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar migrator.jar", options);
        exit(0);
    }

    public static InvocationMode getMode() {
        return mode;
    }

    public static String getInputFile() {
        return inputFile;
    }

    public static String getOutputFile() {
        return outputFile;
    }

    public static String getTargetPath() {
        return targetPath;
    }
}
