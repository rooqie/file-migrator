package home.cli;

/**
 * Created by general on 2016/11/1.
 */
public enum InvocationMode {
    COMPARE_FILE_FOLDER('f'),
    COMPARE_FILE_FILE('2'),
    READ_DEFAULT('d'),
    READ_TO_FILE('r');

    private char shortName;

    InvocationMode(char shotName){
        this.shortName = shotName;
    }

}
