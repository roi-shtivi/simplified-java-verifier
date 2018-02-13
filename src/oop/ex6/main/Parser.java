package oop.ex6.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that parse the s-java file.
 */
class Parser {

    // Useful value's.
    private final static String STRING = "String", FINAL = "final", EMPTY_STRING = "", EQUAL = "=";
    private final static String START_LOOP = "while", START_CONDITION = "if", START_FUNCTION = "void";
    private final static String RETURN = "return", END_FILE_NAME = ".sjava", START_COMMENT = "//";
    private final static String COMMA = ",", GOOD_BRACKET = " ( ", END_BRACES = "}";
    private final static char START_BRACKETS = '(', END_BRACKETS = ')', SEMICOLON = ';';
    private static final int GLOBAL_DEPTH = 0, LENGTH_EQUALITY = 2;

    // Errors string's.
    private final static String BAD_FORMAT_ERROR = "Bad format line.";
    private final static String TYPE_ERROR_MESSAGE = "Illegal type of value.";
    private final static String RETURN_ERROR = "Un legal return format.";
    private final static String BAD_METHOD_FORMAT_ERROR = "Bad method format.";
    private final static String UNSUPPORTED_COMMAND = "Unsupported command.";
    private final static String NAME_ERROR_MESSAGE = "Illegal name variable.";
    private final static String INITIALIZE_ERROR_MESSAGE = "Final must initialize.";
    private final static String ILLEGAL_METHOD_CALL_ERROR = "Unknown method call.";
    private final static String CONDITION_ERROR = "Cant start if condition out of a method.";
    private final static String LOOP_ERROR = "Cant start if loop out of a method.";
    private final static String FILE_TYPE_ERROR = "Un support type of file.";
    private final static String DUPLICATION_VARIABLES_NAMES = "Two variables cannot have the same name.";
    private final static String METHOD_DECLARATION = "Illegal method declaration.";
    private final static String CLOSE_METHOD_ERROR = "Method never closed the Brackets.";
    private final static String INNER_METHOD_ERROR = "Can't create method in another method.";


    // Pattern's string's.
    private static final String SPACES = "\\s+";
    private static final String BAD_BRACKET = "\\s*\\(\\s*";
    private static final String FIRST_WORD = "\\S+";
    private static final String METHOD_NAME = "\\S+[\\s\\S]*\\(";
    private static final String LEGAL_END = ";\\s*";
    private static final String END_BLOCK = "\\s*}\\s*";
    private static final String START_BLOCK = "\\s*\\S+\\s*\\{\\s*";
    private static final String START_BLOCK_NEW = ".+\\{\\s*";
    private static final String SINGLE_NAME = "\\s*\\S+\\s*";
    private static final String IS_STRING = "\".*\"";
    private static final String LEGAL_RETURN = "\\s*\\breturn\\b\\s*";
    private static final String EMPTY_ROW = "\\s*;?\\s*";
    private static final String SPACE_ROW = "\\s*";
    private static final String METHOD_CALL = "[a-zA-Z]+\\w*\\(";
    private static final String LEGAL_METHOD_RETURN = "\\s*return\\s*;\\s*";
    private static final String PARAMETERS = "\\(.*\\)";


    // Patterns
    private static Pattern singleName = Pattern.compile(SINGLE_NAME);
    private static Pattern firstWordPattern = Pattern.compile(FIRST_WORD);
    private static Pattern methodName = Pattern.compile(METHOD_NAME);
    private static Pattern legalEnd = Pattern.compile(LEGAL_END);
    private static Pattern endBlockPattern = Pattern.compile(END_BLOCK);
    private static Pattern startBlockPattern = Pattern.compile(START_BLOCK);
    private static Pattern isString = Pattern.compile(IS_STRING);
    private static Pattern returnPattern = Pattern.compile(LEGAL_RETURN);
    private static Pattern emptyRowPattern = Pattern.compile(EMPTY_ROW);
    private static Pattern spaceRowPattern = Pattern.compile(SPACE_ROW);
    private static Pattern methodCallPattern = Pattern.compile(METHOD_CALL);
    private static Pattern legalMethodReturn = Pattern.compile(LEGAL_METHOD_RETURN);
    private static Pattern newStartBlock = Pattern.compile(START_BLOCK_NEW);
    private static Pattern extractParameters = Pattern.compile(PARAMETERS);


    // Field's of Parser.
    private HashMap<String, Method> methods;
    static ArrayList<HashMap<String, Variable>> variables;

    /**
     * The constructor.
     *
     * @throws IOException
     */
    Parser() throws IOException {
        methods = new HashMap<>();
        variables = new ArrayList<>();
        variables.add(new HashMap<>());
    }

    private boolean isLegalFile(String path) {
        return path.endsWith(END_FILE_NAME);
    }

    /*
     * Extract the string that describe the parameter's from the line.
     *
     * @param line       The line that contain the describe of the parameter's.
     * @param numberLine The number of the line in the file.
     * @return String of the parameter's.
     * @throws IllegalException
     */
    private static String extractInnerBrackets(String line, int numberLine) throws IllegalException {
        int startIndex = line.indexOf(START_BRACKETS), endIndex = line.indexOf(END_BRACKETS);
        if (startIndex < endIndex) {
            return line.substring(startIndex + 1, endIndex);
        } else {
            throw new IllegalException(METHOD_DECLARATION, numberLine);
        }
    }

    /**
     * Give a string the method return the first separated by spaces word
     *
     * @param string     the string to extract from
     * @param numberLine the number line of the string.
     * @return the first word in the string
     * @throws IllegalException
     */
    static String extractFirstWord(String string, int numberLine) throws IllegalException {
        try {
            Matcher matcher = firstWordPattern.matcher(string);
            if (matcher.find()) {
                return string.substring(matcher.start(), matcher.end());
            } else {
                throw new IllegalException(BAD_FORMAT_ERROR, numberLine);
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalException(BAD_FORMAT_ERROR, numberLine);
        }
    }

    /*
     * Giving a String line the function will extract the method name in it.
     *
     * @param line The line of the method.
     * @param numberLine  the number line of the string.
     * @return The name of the method.
     * @throws IllegalException
     */
    private static String extractMethodName(String line, int numberLine) throws IllegalException {
        Matcher matcher = methodName.matcher(line);
        if (line.equals(EMPTY_STRING))
            throw new IllegalException(BAD_FORMAT_ERROR, numberLine);
        if (matcher.find()) {
            line = line.substring(matcher.start(), matcher.end() - 1);
            String[] parts = line.split(SPACES);
            if (parts.length > 1) {
                throw new IllegalException(BAD_FORMAT_ERROR, numberLine);
            } else {
                return parts[0];
            }
        } else {
            throw new IllegalException(BAD_FORMAT_ERROR, numberLine);
        }
    }

    /*
     * get line of variable initialing and update the variable array.
     *
     * @param depth      The depth of the block that the variables bolong to.
     * @param line       The line of the declare of the variable's.
     * @param lineNumber The number line of the string.
     * @param firstWord  The first word in the row.
     * @throws IllegalException
     */
    private void updateVariables(int depth, String line, int lineNumber, String firstWord) throws IllegalException {
        String varType;
        boolean isFinal = false;
        if (firstWord.equals(FINAL)) {
            isFinal = true;
            line = line.substring(line.indexOf(FINAL) + FINAL.length());
            varType = extractFirstWord(line, lineNumber);
            if (!Variable.isLegalVariableType(varType)) {
                throw new IllegalException(TYPE_ERROR_MESSAGE, lineNumber);
            }
        } else {
            varType = firstWord;
        }
        line = line.substring(line.indexOf(varType) + varType.length());
        String[] parts = line.split(COMMA);
        for (String part : parts) {
            if (!part.contains(EQUAL)) { //var assignment without value.
                if (isFinal) {
                    throw new IllegalException(INITIALIZE_ERROR_MESSAGE, lineNumber);
                }
                Matcher singleNameMatcher = singleName.matcher(part);
                if (singleNameMatcher.matches()) {
                    String varName = extractFirstWord(part, lineNumber);
                    if (!Variable.isLegalVariableName(varName)) {
                        throw new IllegalException(NAME_ERROR_MESSAGE, lineNumber);
                    }
                    Variable newVar = new Variable(varType, varName, lineNumber, isFinal);
                    if (!addVariable(newVar, depth)) {
                        throw new IllegalException(DUPLICATION_VARIABLES_NAMES, lineNumber);
                    }
                } else {
                    throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
                }
            } else { //var assignment with value.
                String[] equal = part.split(EQUAL);
                String varName = extractFirstWord(equal[0], lineNumber);
                Variable newVar = new Variable(varType, varName, lineNumber, isFinal);
                if (!addVariable(newVar, depth)) {
                    throw new IllegalException(DUPLICATION_VARIABLES_NAMES, lineNumber);
                }
                assignmentValue(EQUAL + equal[1], newVar, lineNumber);
            }
        }
    }

    /*
     * Assignment value to variable.
     *
     * @param assignment THe String of what to assignment to the variable.
     * @param variable   The variable to assignment the value.
     * @param lineNumber The number line of the string.
     * @throws IllegalException THe line format was illegal.
     */
    private void assignmentValue(String assignment, Variable variable, int lineNumber) throws IllegalException {
        String[] parameters = assignment.split(EQUAL);
        String varValue;
        if (parameters.length == LENGTH_EQUALITY) {
            Matcher matcher = spaceRowPattern.matcher(parameters[0]);
            if (matcher.matches()) {
                switch (variable.getType()) {
                    case STRING:
                        Matcher isStringMatcher = isString.matcher(parameters[1]);
                        boolean stringMatch = isStringMatcher.find();
                        if (stringMatch) {
                            varValue = parameters[1].substring(isStringMatcher.start(), isStringMatcher.end());
                        } else {
                            throw new IllegalException(TYPE_ERROR_MESSAGE, lineNumber);
                        }
                        break;
                    default:
                        varValue = extractFirstWord(parameters[1], lineNumber);
                        break;
                }
                parameters[1] = parameters[1].substring(parameters[1].indexOf(varValue) + varValue.length());
                Matcher spaceRowMatcher = spaceRowPattern.matcher(parameters[1]);
                if (spaceRowMatcher.matches()) {
                    variable.setValue(varValue, lineNumber);
                } else {
                    throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
                }
            } else {
                throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
            }
        } else {
            throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
        }
    }

    /**
     * Go other all of the s-java file and do the first analysis.
     *
     * @param path The path tp the s-java file.
     * @throws IOException      The file does'nt exist or the file isn't sjava file.
     * @throws IllegalException The file contain illegal command.
     */
    void analyzerFile(String path) throws IOException, IllegalException {
        if (!isLegalFile(path)) {
            throw new IOException(FILE_TYPE_ERROR);
        }
        File sJavaFile = new File(path);
        Scanner input = new Scanner(sJavaFile);
        String row;
        int lineNumber = 1, counterBlocks = 1, firstMethodLine = 1;
        ArrayList<String> rows = null;
        String word, parameters = EMPTY_STRING, methodName = EMPTY_STRING, subLine;
        Matcher firstWord;
        while (input.hasNext()) {
            row = input.nextLine();
            if (rows == null) {
                firstWord = firstWordPattern.matcher(row);
                if (firstWord.find()) {
                    word = row.substring(firstWord.start(), firstWord.end());
                    switch (word) {
                        case START_CONDITION:
                            throw new IllegalException(CONDITION_ERROR, lineNumber);
                        case START_LOOP:
                            throw new IllegalException(LOOP_ERROR, lineNumber);
                        case START_FUNCTION:
                            subLine = row.substring(firstWord.end());
                            rows = new ArrayList<>();
                            firstMethodLine = lineNumber;
                            parameters = extractInnerBrackets(subLine, lineNumber);
                            methodName = extractMethodName(subLine, lineNumber);
                            if (!Method.isLegalMethodName(methodName)) {
                                throw new IllegalException("", lineNumber);
                            }
                            Matcher startBlockMatcher = startBlockPattern.matcher(row);
                            if (!startBlockMatcher.find(row.indexOf(START_BRACKETS) + 1)) {
                                throw new IllegalException(EMPTY_STRING, lineNumber);
                            }
                            break;
                        default:
                            analyzeRow(row, GLOBAL_DEPTH, lineNumber, word);
                            break;
                    }
                }
            } else {
                counterBlocks = blockRunner(rows, row, counterBlocks);
                if (counterBlocks == 0) {
                    Method method = new Method(rows, methodName, firstMethodLine, parameters, GLOBAL_DEPTH
                            + 1);
                    rows = null;
                    counterBlocks = 1;
                    methods.put(method.getName(), method);
                }
            }
            lineNumber++;
        }
        input.close();
        if (rows != null) {
            throw new IllegalException(CLOSE_METHOD_ERROR, firstMethodLine);
        }
    }

    /**
     * Parse all of the Block's
     *
     * @return true if al of the ,method's was valid, false otherwise.
     */
    boolean parseMethods() throws IllegalException {
        for (Method method : methods.values()) {
            parseMethod(method);
        }
        return false;
    }


    /*
     * Analyze one row, assuming that the row isn't start of block or the end of the block.
     *
     * @param row        THe row to analyze.
     * @param depth      The depth of the block that the row of his rows.
     * @param lineNumber The number of the line in the full s-java file.
     * @throws IllegalException The line is not legal.
     */
    private void analyzeRow(String row, int depth, int lineNumber, String firstWord) throws IllegalException {
        if (firstWord == null) {
            firstWord = EMPTY_STRING;
        }
        row = row.replaceAll(BAD_BRACKET, GOOD_BRACKET);
        Matcher endRow = legalEnd.matcher(row);
        Matcher methodCallMatcher = methodCallPattern.matcher(row);
        if (firstWord.startsWith(START_COMMENT)) {
            return;
        } else if (endRow.find()) {
            if (row.indexOf(SEMICOLON) != row.lastIndexOf(SEMICOLON)) {
                throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
            } else {
                row = row.substring(0, endRow.start());
            }
        } else {
            throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
        }
        firstWord = extractFirstWord(row, lineNumber);
        if (firstWord.equals(RETURN)) {
            if (!isLegalReturn(row)) {
                throw new IllegalException(RETURN_ERROR, lineNumber);
            }
        } else if (firstWord.equals(FINAL) || Variable.isLegalVariableType(firstWord)) {
            updateVariables(depth, row, lineNumber, firstWord);
        } else if (Variable.isLegalVariableName(firstWord)) {
            Variable variable = getVariable(firstWord);
            Method method = methods.get(firstWord);
            if (variable != null) {
                row = row.substring(row.indexOf(firstWord) + firstWord.length());
                assignmentValue(row, variable, lineNumber);
            } else if (method != null) {
                method.calledThisMethod(extractParameters(row, lineNumber), lineNumber);
            } else {
                throw new IllegalException(UNSUPPORTED_COMMAND, lineNumber);
            }
        } else if (methodCallMatcher.find()) { // check if a known method has been called.
            String methodName = row.substring(methodCallMatcher.start(), methodCallMatcher.end() - 1);
            if (!methods.containsKey(methodName)) {
                throw new IllegalException(ILLEGAL_METHOD_CALL_ERROR, lineNumber);
            }
        } else {
            throw new IllegalException(UNSUPPORTED_COMMAND, lineNumber);
        }
    }

    /*
     * Parse a single block.
     * @param block The block to parse.
     * @throws IllegalException
     */
    private void parseBlock(Block block) throws IllegalException {
        int lineNumber = block.getOriginLine() + 1, counterBlock = 0, firstNewBlockLine = lineNumber;
        ArrayList<String> rows = null;
        String firstWord, condition = EMPTY_ROW;
        for (String row : block.getRows()) {
            row = row.replaceAll(BAD_BRACKET, GOOD_BRACKET);
            Matcher emptyRowMatcher = emptyRowPattern.matcher(row);
            Matcher firstWordMatcher = firstWordPattern.matcher(row);
            if (firstWordMatcher.find()) {
                firstWord = row.substring(firstWordMatcher.start(), firstWordMatcher.end());
            } else if (row.startsWith(START_COMMENT) || emptyRowMatcher.matches()) {
                return;
            } else {
                throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
            }
            if (rows == null) {
                if (row.contains(END_BRACES)) {
                    throw new IllegalException(BAD_FORMAT_ERROR, lineNumber);
                }
                switch (firstWord) {
                    case START_FUNCTION:
                        throw new IllegalException(INNER_METHOD_ERROR, lineNumber);
                    case START_CONDITION:
                    case START_LOOP:
                        rows = new ArrayList<>();
                        condition = extractInnerBrackets(row, lineNumber);
                        firstNewBlockLine = lineNumber;
                        counterBlock++;
                        break;
                    default:
                        analyzeRow(row, block.getDepth(), lineNumber, firstWord);
                        break;
                }
            } else {
                counterBlock = blockRunner(rows, row, counterBlock);
                if (counterBlock == 0) {
                    Block newBlock = new ConditionBlock(rows, firstNewBlockLine, condition, block.getDepth() + 1);
                    rows = null;
                    parseBlock(newBlock);
                }
            }
            lineNumber++;
        }

    }

    /*
     * Add row that belong to a block to the block.
     *
     * @param rows The array-list of the rows of the new block.
     * @param row The current line.
     * @param counterBlocks Counter of depth of the inner blocks.
     * @return The update counterBlocks after the current row.
     */
    private int blockRunner(ArrayList<String> rows, String row, int counterBlocks) {
        Matcher startBlock, endBlock;
        startBlock = newStartBlock.matcher(row);
        endBlock = endBlockPattern.matcher(row);
        if (startBlock.matches()) {
            counterBlocks++;
            rows.add(row);
        } else if (endBlock.matches()) {
            counterBlocks--;
            if (counterBlocks > 0) {
                rows.add(row);
            } else {
                return counterBlocks;
            }
        } else { // if not start of block and not end of block the line is in the block.
            rows.add(row);
        }
        return counterBlocks;
    }

    /*
     * Check if the last row is legal return.
     *
     * @param row The last row of the function
     * @return true if this line of return is legal, false otherwise.
     */
    private boolean isLegalReturn(String row) {
        Matcher matcher = returnPattern.matcher(row);
        return matcher.matches();
    }

    /**
     * Find the variable.
     *
     * @param name The variable name.
     * @return The variable if exists, else null.
     */
    static Variable getVariable(String name) {
        Variable variable = null;
        for (int index = variables.size() - 1; index >= 0; index--) {
            variable = variables.get(index).get(name);
            if (variable != null) {
                break;
            }
        }
        return variable;
    }

    /*
     * Parse a single method.
     *
     * @param method The method to parse.
     * @throws IllegalException
     */
    private void parseMethod(Method method) throws IllegalException {
        int lastRowIndex = method.getRows().size() - 1;
        String lastLine = method.getRows().get(lastRowIndex);
        Matcher legalEnd = legalMethodReturn.matcher(lastLine);
        if (legalEnd.matches()) {
            method.getRows().remove(lastRowIndex);
            parseBlock(method);
        } else {
            throw new IllegalException(BAD_METHOD_FORMAT_ERROR, method.getOriginLine());
        }
    }

    /**
     * This function get a variable can check add it to the right depth place if possible.
     *
     * @param variable The variable we want to add in the giving depth
     * @param depth    The depth of the block.
     * @return true if no variable contains in the same depth, false otherwise.
     */
    static boolean addVariable(Variable variable, int depth) {
        if (variables.get(depth).containsKey(variable.getName())) {
            return false;
        } else {
            variables.get(depth).put(variable.getName(), variable);
            return true;
        }
    }

    /*
     * @param row The row of call to the method.
     * @param lineNumber  The number of the current line in the sjava file.
     * @return The sub-string that hold the parameters.
     * @throws IllegalException There isn't brackets in the row.
     */
    private String extractParameters(String row, int lineNumber) throws IllegalException {
        Matcher matcher = extractParameters.matcher(row);
        if (matcher.find()) {
            return row.substring(matcher.start() + 1, matcher.end() - 1);
        } else {
            throw new IllegalException(EMPTY_STRING, lineNumber);
        }
    }
}