/**
 * Created by kevin on 1/16/2017.
 */
public class SudokuSolver {
    /*
    Techniques to add:
        xwing
        swordfish
        locked candidate
     */

    private String[][] possibilities,storePossibilities;
    private String sudokuBoard;
    private int[][] inferenceDataRowCount, inferenceDataColumnCount, inferenceDataBoxCount;
    private int[][][] inferenceDataRowLocations, inferenceDataColumnLocations;
    private int[][][][] inferenceDataBoxLocations;
    private boolean continueSearch;

    public SudokuSolver(){
        /*
        This is the constructor for when the gui is to be used in order to enter
        the know numbers of the sudoku puzzle
         */
        Window window = new Window(this);
    }

    public SudokuSolver(String sudokuBoard){
        /*
        This Constructor is for when you already have a specific sudoku board string that you want to run
        through the program without the use of the gui.
        (useful for debugging using the same board again and again)
         */
        createPossiblities(sudokuBoard);
        Window w = new Window(this.sudokuBoard,possibilities, "default");
        eliminateGiven();

        Window w2 = new Window(this.sudokuBoard,possibilities, "given eliminated");

        storePossibilities = new String[9][9];
        copyOverStrings(possibilities, storePossibilities);

        continueSearch = true;
        while(continueSearch){
            /*
            continueSearch is used as a flag to see if any progress is being made on the possibilities array.
            Once no progress is made then loop exits. Continue search is set to true if possibilities array changes
             */
            continueSearch = false;
            gatherFrequencyData();
            if(findSingles() == 1){
                continueSearch = true;
                continue; //want to redo frequency data if singles are found and replaced on board
            }
            //Window w5 = new Window(this.sudokuBoard,possibilities, "singles eliminated");

            findPairs();
            checkForSinglePossibilities();
            if(checkForChange(possibilities,storePossibilities)){
                continueSearch = true;
                copyOverStrings(possibilities, storePossibilities);
                continue;
            }
            //Window w6 = new Window(this.sudokuBoard,possibilities, "pairs eliminated");


            //lockedCandidate();
            if(checkForChange(possibilities,storePossibilities)){
                continueSearch = true;
                copyOverStrings(possibilities, storePossibilities);
            }
            //Window w7 = new Window(this.sudokuBoard,possibilities, "locked pairs eliminated");

            xwing();
            if(checkForChange(possibilities,storePossibilities)){
                continueSearch = true;
                copyOverStrings(possibilities, storePossibilities);
            }

        }
        //show updated puzzle solved to program's best ability
        Window w3 = new Window(this.sudokuBoard,possibilities, "Result");

    }

    public String getSudokuBoard(){
        return this.sudokuBoard;
    }

    public String[][] getPossibilities(){
        return this.possibilities;
    }

    public void createPossiblities(String sudokuBoard){
        /*
        Creates array possibilities which will hold all possible numbers for a given row and column position on the sudoku board.
        this method initializes all of theses possibilities to any position can have any number
         */
        this.sudokuBoard = sudokuBoard;
        possibilities = new String[9][9];
        char currentChar;

        for(int charLocation = 0; charLocation < 81; charLocation++){
            currentChar = sudokuBoard.charAt(charLocation);
            if(currentChar == ' '){
                int row = charLocation / 9;
                int column = charLocation % 9;
                possibilities[row][column] = "123456789";
            }
        }
    }

    public void eliminateGiven(){
        /*
        This method goes through the sudoku board and finds all positions that start with a number already in place
        and clears the possibilities for that given number in all affected spaces to its row and column
        (ie all numbers in same row, column, box)
         */

        char currentChar;
        for(int charLocation = 0; charLocation < 81; charLocation++){
            currentChar = sudokuBoard.charAt(charLocation);
            if(currentChar != ' '){  //check to see if number already at that position
                int row = charLocation / 9;
                int column = charLocation % 9;
                int[][] affectedSquares = findAffectedPositions(row,column);
                eliminateAffectedSquares(affectedSquares,currentChar);
            }
        }
    }

    public static int[][] findAffectedPositions(int rowNum, int columnNum){
        /*
        Adds all coordinates to temp that are in the same row, column, and box of rowNum, columnNum
         */
        int[][] temp = new int[30][];

        //add both squares in same column and in same row
        int currentArrayIndex = 0;
        for(int index = 0; index < 9; index++){
            if(index != columnNum) {   //don't add point of interest
                int[] coords = new int[2];
                //add new coordinate in same row
                coords[0] = rowNum;
                coords[1] = index;
                temp[currentArrayIndex++] = coords;
            }

            if(index != rowNum) {  //don't add point of interest
                int[] coords = new int[2];
                //add new coordinate in same column
                coords[0] = index;
                coords[1] = columnNum;
                temp[currentArrayIndex++] = coords;
            }
        }

        //find other coordinates for those in the same square but not in the same row/column
        //find what mulitple of 3 the row and column are just above
        int baseRowMultiple = (rowNum / 3) * 3;  //divide first(truncation finds next best lower multiple)
        int baseColumnMultiple = (columnNum / 3) * 3;

        //loop through 9 square grid and add all points not in same row / column
        int newRow, newColumn;
        for(int row = 0;row < 3;row++){
            for(int column = 0; column < 3; column++){
                if(row != rowNum % 3 && column != columnNum % 3) {
                    int[] coord = new int[2];
                    coord[0] = baseRowMultiple + row;
                    coord[1] = baseColumnMultiple + column;
                    temp[currentArrayIndex++] = coord;
                }
            }
        }

        return temp;
    }

    public void eliminateAffectedSquares(int[][] affectedSquares, char eliminating){
        /*
        Goes through all non null elements in affectedSquares and removes number ,eliminating, from
        the possibilities array at those positions
         */
        for(int[] sub: affectedSquares){
            if(sub != null){
                eliminateRowColumn(eliminating,sub[0],sub[1]);
            }else{
                break;
            }
        }
    }

    public void eliminateRowColumn(char eliminating, int rowNum, int columnNum){
        /*
        removes character eliminating from position rowNum, columnNum from the possibilities array
         */
        if(possibilities[rowNum][columnNum] != null) {
            possibilities[rowNum][columnNum] = possibilities[rowNum][columnNum].replace(eliminating, ' ');
        }
    }

    public void gatherFrequencyData(){
        /*
        gather data on where possibilities for all numbers are so that you can look at the frequency to
        infer where numbers should be
        inferenceData (Row, Column, Box)Count - first index is the first 9 items of that type, next index stands for a number(index + 1) and
                                                stored at that location is the count of the amount of finds for that
                                                specific situation
        inferenceData (Row, Clumn,Box)Locations - first 2 indexes same as above. the next index is a coordinate array
                    that references a location of a specific find for that number.For Row I only save the column found at.
                    For Column I only save row found at. For Box I save both row and column of found position
         */
        inferenceDataRowCount = new int[9][9];
        inferenceDataColumnCount = new int[9][9];
        inferenceDataBoxCount = new int[9][9];
        inferenceDataRowLocations = new int[9][9][9];
        inferenceDataColumnLocations = new int[9][9][9];
        inferenceDataBoxLocations = new int[9][9][9][2];
        int currentRowIndex, currentColumnIndex, currentBoxIndex;


        for(int i = 0; i < 9; i++){
            String searchingFor;

            //gather information for ith row
            for(int j = 0; j < 9; j++){ //go through and count for each num
                currentRowIndex = 0;
                searchingFor = "" + (j + 1);
                for(int column = 0; column < 9; column++){  // check each column and count
                    if(possibilities[i][column] != null) {
                        if (possibilities[i][column].contains(searchingFor)) {
                            inferenceDataRowCount[i][j]++;
                            inferenceDataRowLocations[i][j][currentRowIndex++] = column;
                        }
                    }
                }
            }

            //gather information for ith column
            for(int j = 0; j < 9; j++){ //go through and count for each num
                currentColumnIndex = 0;
                searchingFor = "" + (j + 1);
                for(int row = 0; row < 9; row++){  // check each row and add to count if necessary
                    if(possibilities[row][i] != null) {
                        if (possibilities[row][i].contains(searchingFor)) {
                            inferenceDataColumnCount[i][j]++;
                            inferenceDataColumnLocations[i][j][currentColumnIndex++] = row;
                        }
                    }
                }
            }

            //gather information for ith box
            //assume there are 3x3 boxes and the overarching for loop goes through them from left to right top to bottom
            int boxRow = i / 3;
            int boxColumn = i % 3;
            for(int j = 0;j < 9;j++) { //go through and count for each num
                currentBoxIndex = 0;
                searchingFor = "" + (j + 1);
                for (int row = boxRow * 3; row < boxRow * 3 + 3; row++) {  //go through each row
                    for (int column = boxColumn * 3; column < boxColumn * 3 + 3; column++) {  //go through each column
                        if(possibilities[row][column] != null){
                            if(possibilities[row][column].contains(searchingFor)){
                                inferenceDataBoxCount[i][j]++;
                                int[] coord = {row,column};
                                inferenceDataBoxLocations[i][j][currentBoxIndex++] = coord;
                            }
                        }
                    }
                }
            }
        }
    }

    public int findSingles(){
        /*
        Look through inferenceData for any counts that = 1 and then place that number into the board at that position
         */

        //first look through inference data for the row counts to look for counts that are 1
        for(int row = 0; row < 9;row++){ //each row
            for(int numIndex = 0; numIndex < 9; numIndex++){  //each num
                if(inferenceDataRowCount[row][numIndex] == 1){
                    //System.out.println("row find a single at " + row + " " + inferenceDataRowLocations[row][numIndex][0] + " of number " + (numIndex + 1));
                    String number = ("" + (numIndex + 1));
                    changeBoard(row,inferenceDataRowLocations[row][numIndex][0],number.charAt(0));
                    return 1;
                }
            }
        }

        //second look through inference data for the column counts to look for counts that are 1
        for(int column = 0; column < 9;column++){ //each row
            for(int numIndex = 0; numIndex < 9; numIndex++){  //each num
                //System.out.println("column data " + column + " of num " + (numIndex + 1) + " of count " + inferenceDataRowCount[column][numIndex]);
                if(inferenceDataColumnCount[column][numIndex] == 1){
                    //System.out.println("column find a single at " + inferenceDataColumnLocations[column][numIndex][0] + " " + column + " of number " + (numIndex + 1));
                    String number = ("" + (numIndex + 1));
                    changeBoard(inferenceDataColumnLocations[column][numIndex][0],column,number.charAt(0));
                    return 1;
                }
            }
        }

        //third look through inference data for the box counts to look for counts that are 1
        for(int box = 0; box < 9;box++){ //each row
            for(int numIndex = 0; numIndex < 9; numIndex++){  //each num
                //System.out.println("column data " + column + " of num " + (numIndex + 1) + " of count " + inferenceDataRowCount[column][numIndex]);
                if(inferenceDataBoxCount[box][numIndex] == 1){
                    //System.out.println("box find a single at " + inferenceDataBoxLocations[box][numIndex][0][0] + " " + inferenceDataBoxLocations[box][numIndex][0][1] + " of number " + (numIndex + 1));
                    String number = ("" + (numIndex + 1));
                    changeBoard(inferenceDataBoxLocations[box][numIndex][0][0],inferenceDataBoxLocations[box][numIndex][0][1],number.charAt(0));
                    return 1;
                }
            }
        }
        return 0;
    }

    public void changeBoard(int changeRow, int changeColumn, char numReplacing){
        //first find place in sudokuBoard where character must be added and put it in
        int index = 9 * changeRow + changeColumn;
        String before;
        if(index == 0){
            before = "";
        }else{
            before = sudokuBoard.substring(0,index);
        }
        sudokuBoard = before + numReplacing + sudokuBoard.substring(index + 1);

        //update possibilities array
        eliminateAffectedSquares(findAffectedPositions(changeRow,changeColumn),numReplacing);
        possibilities[changeRow][changeColumn] = null;

        //update inference data arrays by recalculating them(rather than trying to work with what I already have)
        //Window poss = new Window(sudokuBoard,possibilities);
    }

    public int findPairs(){
        /*
        find pairs of numbers(they both only occur twice at same positions) and eliminate those numbers that are at
        the same position as this hidden pair
         */

        //ROW
        //go through each row and look for two numbers that both have only two occurrences at the same positions
        for(int row = 0; row < 9; row++){  //loop through each row
            int[][] foundNumbers = new int[9][];
            int foundIndex = 0;
            for(int nums = 0; nums < 9;nums++){ //loop through each number
                if(inferenceDataRowCount[row][nums] == 2){
                    int[] temp = new int[3];
                    temp[0] = nums + 1;
                    temp[1] = inferenceDataRowLocations[row][nums][0];
                    temp[2] = inferenceDataRowLocations[row][nums][1];
                    foundNumbers[foundIndex++] = temp;
                }

            }
            //need to evaluate numbers collected if any usable information
            evaluateFoundNumbers(foundNumbers, row, true);
        }

        //COLUMN
        //go through each column and look for two numbers that both have only two occurrences at the same positions
        for(int column = 0; column < 9; column++){  //loop through each row
            int[][] foundNumbers = new int[9][];
            int foundIndex = 0;
            for(int nums = 0; nums < 9;nums++){ //loop through each number
                if(inferenceDataColumnCount[column][nums] == 2){
                    int[] temp = new int[3];
                    temp[0] = nums + 1;
                    temp[1] = inferenceDataColumnLocations[column][nums][0];
                    temp[2] = inferenceDataColumnLocations[column][nums][1];
                    foundNumbers[foundIndex++] = temp;
                }

            }
            //need to evaluate numbers collected if any usable information
            evaluateFoundNumbers(foundNumbers, column, false);
        }

        //BOX
        //go through each box and look for two numbers that both have only two occurrences at the same positions
        for(int box = 0; box < 9; box++){  //loop through each row
            int[][][] foundNumbers = new int[9][][];
            int foundIndex = 0;
            for(int nums = 0; nums < 9;nums++){ //loop through each number
                if(inferenceDataBoxCount[box][nums] == 2){
                    int[][] temp = new int[3][2];
                    temp[0][0] = nums + 1;
                    temp[1] = inferenceDataBoxLocations[box][nums][0];
                    temp[2] = inferenceDataBoxLocations[box][nums][1];
                    foundNumbers[foundIndex++] = temp;
                }

            }
            //need to evaluate numbers collected if any usable information
            evaluateFoundNumbers(foundNumbers);
        }

        return 1;
    }

    public void evaluateFoundNumbers(int[][] foundNumbers, int setCoord, boolean isRow){
        for(int numberIndex = 0;numberIndex < foundNumbers.length;numberIndex++){ //go through each number set
            if(foundNumbers[numberIndex] == null) {
                break;
            }
                for (int matchIndex = numberIndex + 1; matchIndex < foundNumbers.length; matchIndex++) {  //search for a possible match
                    if(foundNumbers[matchIndex] == null){
                        break;
                    }
                    else if (checkLocationEquality(foundNumbers[numberIndex], foundNumbers[matchIndex])) {
                        //must then change notes
                        //check to see if both locations are in the same box
                        if(foundNumbers[numberIndex][1] / 3 == foundNumbers[numberIndex][2] / 3){
                            //must go through notes in box
                        }
                        //must go through notes in row for those two positions
                        int[][] coords = new int[2][];

                        if(isRow) {
                            coords[0] = new int[]{setCoord, foundNumbers[numberIndex][1]};
                            coords[1] = new int[]{setCoord, foundNumbers[numberIndex][2]};
                        }else{
                            coords[0] = new int[]{foundNumbers[numberIndex][1], setCoord};
                            coords[1] = new int[]{foundNumbers[numberIndex][2], setCoord};
                        }
                        String[] numberString = new String[]{"" + foundNumbers[numberIndex][0], "" + foundNumbers[matchIndex][0]};
                        removeAllOtherNumbersAtPosition(numberString, coords);
                    }
                }

        }
    }

    public void evaluateFoundNumbers(int[][][] foundNumbers){
        for(int numberIndex = 0;numberIndex < foundNumbers.length;numberIndex++){ //go through each number set
            if(foundNumbers[numberIndex] == null) {
                break;
            }
            for (int matchIndex = numberIndex + 1; matchIndex < foundNumbers.length; matchIndex++) {  //search for a possible match
                if(foundNumbers[matchIndex] == null){
                    break;
                }
                else if (checkLocationEquality(foundNumbers[numberIndex], foundNumbers[matchIndex])) {
                    //must then change notes
                    //check to see if both locations are in the same row
                    if(foundNumbers[numberIndex][1][0] == foundNumbers[numberIndex][2][0]){
                        String[] numberString = new String[]{"" + foundNumbers[numberIndex][0][0], "" + foundNumbers[matchIndex][0][0]};
                        eliminateNakedPair(foundNumbers[numberIndex][1],foundNumbers[numberIndex][2], true, numberString);
                        //must go through notes in same row but different box
                    }else if(foundNumbers[numberIndex][1][1] == foundNumbers[numberIndex][2][1]) {
                        String[] numberString = new String[]{"" + foundNumbers[numberIndex][0][0], "" + foundNumbers[matchIndex][0][0]};
                        eliminateNakedPair(foundNumbers[numberIndex][1],foundNumbers[numberIndex][2], false, numberString);
                        //must go through notes in same column but different box
                    }
                    //must go through notes in row for those two positions
                    int[][] coords = new int[2][];

                    coords[0] = foundNumbers[numberIndex][1];
                    coords[1] = foundNumbers[numberIndex][2];

                    String[] numberString = new String[]{"" + foundNumbers[numberIndex][0][0], "" + foundNumbers[matchIndex][0][0]};
                    removeAllOtherNumbersAtPosition(numberString, coords);
                }
            }

        }
    }

    public void removeAllOtherNumbersAtPosition(String[] charactersString, int[][]locations){
        String numbers = "123456789";
        char[] characters = new char[charactersString.length];
        for(int index = 0; index < charactersString.length;index++){
            characters[index] = charactersString[index].charAt(0);
        }

        char currentchar;

        for(int index = 0; index < numbers.length();index++){
            currentchar = numbers.charAt(index);
            if( ! checkIfIn(currentchar,characters)){
                for(int[] coords: locations){
                    eliminateRowColumn(currentchar, coords[0],coords[1]);
                }
            }
        }
    }

    public boolean checkIfIn(char testing, char[] array){
        for(char c: array){
            if(testing == c){
                return true;
            }
        }
        return false;
    }

    public boolean checkLocationEquality(int[] one, int[] two){
        if(two[1] == one[1] || two[1] == one[2]){
            if(two[2] == one[1] || two[2] == one[2]){
                return true;
            }
        }
        return false;
    }

    public static boolean checkLocationEquality(int[][] one, int[][] two){
            for (int index = 0; index < one.length - 1; index++) {
                boolean found = false;
                for (int n = 0; n < two.length - 1; n++) {
                    if (two[n + 1] != null && one[index + 1] != null) {
                        if (two[n + 1][0] == one[index + 1][0] && two[n + 1][1] == one[index + 1][1]) {
                            found = true;
                            two[n + 1] = null;
                        }
                    }
                }
                if (!found) {
                    return false;
                }
            }
        return true;

    }

    public static boolean checkForChange(String[][] original, String[][] testing){
        for(int row = 0; row < original.length;row++){
            for(int column = 0; column < original[row].length;column++){
                if(original[row][column] == null){
                    if(testing[row][column] != null){
                        return true;
                    }
                }else if (testing[row][column] == null){
                    if(original[row][column] != null){
                        return true;
                    }
                }else if(! original[row][column].equals(testing[row][column])){
                    return true;
                }
            }
        }
        return false;
    }

    public void checkForSinglePossibilities(){
        /*
        Checks for squares that only have one possible number in it
         */
        for(int row = 0; row < possibilities.length;row++){
            for(int column = 0; column < possibilities[row].length;column++) {
                if(possibilities[row][column] != null) {
                    if (countString(possibilities[row][column], ' ') == 8) {
                        char num = possibilities[row][column].charAt(findNonSpaceChar(possibilities[row][column]));
                        eliminateAffectedSquares(findAffectedPositions(row, column), num);
                        changeBoard(row, column, num);
                    }
                }
            }
        }
    }

    public static int countString(String counting,char character){
        int count = 0;
        for(int index = 0; index < counting.length();index++){
            if(counting.charAt(index) == character){
                count++;
            }
        }
        return count;
    }

    public static int findNonSpaceChar(String possible){
        for(int index = 0; index < possible.length(); index++){
            if(possible.charAt(index) != ' '){
                return index;
            }
        }
        return -1;
    }

    public void eliminateNakedPair(int[] one, int[] two, boolean checkRow, String[] numberString){
        /*
        find positions that are affected by a naked pair and eliminate the characters of the numbers(stored in
        numberString) that make up the naked pair
         */

        int[][] affectedPositions = new int[6][2];
        int index = 0;
        if(checkRow){  //check row for affected posiions
            for(int i = 0; i < 9; i++){
                if(i != one[1] && i != two[1] && i / 3 != one[1] / 3){
                    int[] temp = {one[0], i};
                    affectedPositions[index++] = temp;
                }
            }
            eliminateAffectedSquares(affectedPositions, numberString[0].charAt(0));
            eliminateAffectedSquares(affectedPositions, numberString[1].charAt(0));
        }else{  //check column for affected positions
            for(int i = 0; i < 9; i++){
                if(i != one[0] && i != two[0] && i / 3 != one[0] / 3){
                    int[] temp = {i, one[1]};
                    affectedPositions[index++] = temp;
                }
            }
            eliminateAffectedSquares(affectedPositions, numberString[0].charAt(0));
            eliminateAffectedSquares(affectedPositions, numberString[1].charAt(0));
        }

    }

    public void copyOverStrings(String[][] base, String[][] copyTo){
        /*
        copy all strings from base to copyTo assuming copyTo has enough space
         */

        for(int i = 0; i < base.length; i++){
            for(int j = 0; j < base[i].length; j++){
                copyTo[i][j] = base[i][j];
            }
        }
    }

    public void xwing(){
        /*
        looks for xwing patterns which is where there are 2 rows/columns where the same number occurs twice at the same
        column/row (respectively)
         */

        //ROW   i = row , j = num - 1
        //find which numbers have at least 2 rows where there are only 2 occurrences
        int[][] countRow = new int[9][9];
        /*
        for int[][] count: first index stands for numbers 1-9. For the second index the first position (i.e. count[i][0])
        counts the amount of rows that fit the condition while each other position holds the specific row position
         */
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                if(inferenceDataRowCount[i][j] == 2){
                    countRow[j][0]++;
                    countRow[j][countRow[j][0]] = i;
                }
            }
        }
        //go through each number that has at least 2 rows with count 2
        for(int i = 0; i < 9; i++){
            if(countRow[i][0] >= 2){
                //find all positions for this number that fit criteria
                int[][] positions = new int[2 * countRow[i][0]][2];
                int currentIndex = 0;
                for(int j = 0; j < countRow[i][0]; j++){
                    int row = countRow[i][j + 1];
                    int[] square = {row, inferenceDataRowLocations[row][i][0]};
                    int[] square2 = {row, inferenceDataRowLocations[row][i][1]};
                    positions[currentIndex++] = square;
                    positions[currentIndex++] = square2;
                }
                searchForMatch(positions, true, i);
            }
        }


        //COLUMN
        int[][] countColumn = new int[9][9];

        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                if(inferenceDataColumnCount[i][j] == 2){
                    countColumn[j][0]++;
                    countColumn[j][countColumn[j][0]] = i;
                }
            }
        }

        for(int i = 0; i < 9; i++){
            if(countColumn[i][0] >= 2){
                //find all positions for this number that fit criteria
                int[][] positions = new int[2 * countColumn[i][0]][2];
                int currentIndex = 0;
                for(int j = 0; j < countColumn[i][0]; j++){
                    int column = countColumn[i][j + 1];
                    int[] square = {inferenceDataColumnLocations[column][i][0], column};
                    int[] square2 = {inferenceDataColumnLocations[column][i][1], column};
                    positions[currentIndex++] = square;
                    positions[currentIndex++] = square2;
                }
                searchForMatch(positions, false, i);
            }
        }
    }

    public void searchForMatch(int[][] positions, boolean matchColumn, int numOfInterest){

        for(int i = 0; i < positions.length; i = i +2){
            for(int j = i + 2; j < positions.length; j = j + 2){
                if(matchColumn){
                    int[] columnNums = {positions[i][1], positions[i + 1][1]};
                    int[] exceptions = {positions[i][0], positions[j][0]};
                    if(positions[i][1] == positions[j][1] && positions[i + 1][1] == positions[j + 1][1]){
                        removeFromColumn(columnNums, numOfInterest, exceptions);
                    }else if(positions[i][1] == positions[j + 1][1] && positions[i + 1][1] == positions[j][1]){
                        removeFromColumn(columnNums, numOfInterest, exceptions);
                    }
                }else{
                    int[] rowNums = {positions[i][0], positions[i + 1][0]};
                    int[] exceptions = {positions[i][1], positions[j][1]};
                    if(positions[i][0] == positions[j][0] && positions[i + 1][0] == positions[j + 1][0]){
                        removeFromRow(rowNums, numOfInterest, exceptions);
                    }else if(positions[i][0] == positions[j + 1][0] && positions[i + 1][0] == positions[j][0]){
                        removeFromRow(rowNums, numOfInterest, exceptions);
                    }
                }
            }
        }
    }

    public void removeFromRow(int[] rowNums, int eliminating, int[] exceptions){

        int[][] eliminatePositions = new int[21][2];
        int index = 0;
        for(int i = 0; i < rowNums.length; i++){
            int row = rowNums[i];
            for(int j = 0; j < inferenceDataRowCount[row][eliminating]; j++){
                int column = inferenceDataRowLocations[row][eliminating][j];
                if(! containsInt(column, exceptions)){
                    int[] a = {row, column};
                    eliminatePositions[index++] = a;
                }
            }
        }
        String eliminate = "" + (eliminating + 1);
        eliminateAffectedSquares(eliminatePositions, eliminate.charAt(0));
    }

    public void removeFromColumn(int[] columnNums, int eliminating, int[] exceptions){
        int[][] eliminatePositions = new int[21][2];
        int index = 0;
        for(int i = 0; i < columnNums.length; i++){
            int column = columnNums[i];
            for (int j = 0; j < inferenceDataColumnCount[column][eliminating]; j++) {
                int row = inferenceDataColumnLocations[column][eliminating][j];
                if (!containsInt(row, exceptions)) {
                    int[] a = {row, column};
                    eliminatePositions[index++] = a;
                }
            }
        }
        String eliminate = "" + (eliminating + 1);
        eliminateAffectedSquares(eliminatePositions, eliminate.charAt(0));
    }

    public boolean containsInt(int a , int[] b){
        for(int sub: b){
            if(sub == a){
                return true;
            }
        }
        return false;
    }

    public void lockedCandidate(){
        /*
        The idea behind this logic is that if in a row all possible positions of a number exist in the same box then
        that number can not be in the rest of that box. Used same way in consideration to rows. Also checks boxes to see
        if the only possible postions of a number is in the same row/column which means that the number could not occur
        at any position along that specific row/column.
         */

        //ROW
        //check row counts for 2 or 3 and add them to an array if they exist in same box
        int[][] matchRow = new int[81][3];  //second index: {number(index form), row, box #}
        int matchIndex = 0;
        for(int row = 0; row < 9; row++) {
            for (int num = 0; num < 9; num++) {
                int test;
                int count = inferenceDataRowCount[row][num];
                if (count == 2 || count == 3) {
                    test = inferenceDataRowLocations[row][num][0] / 3;
                    int flag = 0;
                    for (int columns = 0; columns < count - 1; columns++) {
                        if (test != (inferenceDataRowLocations[row][num][columns + 1] / 3)) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 1) {
                        continue;
                    }
                    int[] info = {num, row, (row / 3) * 3 + (inferenceDataRowLocations[row][num][1] / 3)};
                    matchRow[matchIndex++] = info;
                }
            }
        }
        for(int index = 0; index < matchIndex; index++) {
            int[] current = matchRow[index];
            removeFromBox(current[2], current[0], current[1], true);
        }

        //Column
        //check column counts for 2 or 3 and add them to an array if they exist in the same box
        int[][] matchColumn = new int[81][3];  //second index: {number(index form), column, box #}
        matchIndex = 0;
        for(int column = 0; column < 9; column++) {
            for(int num = 0; num < 9; num++){
                int test;
                int count = inferenceDataColumnCount[column][num];
                if(count == 2 || count == 3) {
                    test = inferenceDataColumnLocations[column][num][0] / 3;
                    int flag = 0;
                    for(int columns = 0; columns < count - 1;columns++){
                        if (test != (inferenceDataColumnLocations[column][num][columns + 1] / 3)){
                            flag = 1;
                            break;
                        }
                    }
                    if(flag == 0){
                        int row = inferenceDataColumnLocations[column][num][1];
                        int[] info = {num, column, (column/3) + (row / 3) * 3};
                        matchColumn[matchIndex++] = info;
                    }
                }
            }

        }
        for(int index = 0; index < matchIndex; index++) {
            int[] current = matchColumn[index];
            removeFromBox(current[2], current[0], current[1], false);
        }

        //Box
        //check box counts for 2 or 3 and add them to an array if they exist either in the same row/column
        int[][] matchBox = new int[81][4];  //second index: {number(index form), 0= row 1 = column, row/column num, currentbox}
        matchIndex = 0;
        for(int box = 0; box < 9; box++){
            for(int num = 0; num < 9; num++){
                int testRow, testColumn;
                int count = inferenceDataBoxCount[box][num];
                if(count == 2 || count == 3){
                    testRow = inferenceDataBoxLocations[box][num][0][0];
                    testColumn = inferenceDataBoxLocations[box][num][0][1];
                    int rowFlag = 0, columnFlag = 0;
                    for(int occurences = 0; occurences < count - 1; occurences++){
                        if(testRow != inferenceDataBoxLocations[box][num][occurences + 1][0]){
                            rowFlag = 1;
                        }if(testColumn != inferenceDataBoxLocations[box][num][occurences + 1][1]){
                            columnFlag = 1;
                        }
                    }
                    if(rowFlag == 0){
                        int[] info = {num, 0, testRow, box};
                        matchBox[matchIndex++] = info;
                    }else if(columnFlag == 0){
                        int[] info = {num, 1, testColumn, box};
                        matchBox[matchIndex++] = info;
                    }
                }
            }
        }
        for(int index = 0 ; index < matchIndex; index++){
            int[] current = matchBox[index];
            if(current[1] == 0){
                removeLockedBox(current[2], true, current[3], current[0]);
            }else{
                removeLockedBox(current[2], false, current[3], current[0]);
            }

        }
    }

    public void removeFromBox(int boxNum, int eliminating, int exception, boolean isRow){


        int[][] eliminatingPositions = new int[9][2];
        int index = 0;
        int boxRow = (boxNum / 3) * 3;
        int boxColumn = (boxNum % 3) * 3;
        for(int row = 0; row < 3; row++){
            for(int column = 0; column < 3; column++){
                int[] current = {boxRow + row, boxColumn + column};
                int found = 0;
                if(isRow && current[0] == exception) {
                    found = 1;
                }else if (! isRow && current[1] == exception){
                    found = 1;
                }

                if(found == 0){
                    //this location is not in exceptions
                    eliminatingPositions[index++] = current;
                }

            }

        }
        eliminatingPositions[index] = null;
        String s = "" + (eliminating + 1);
        eliminateAffectedSquares(eliminatingPositions, s.charAt(0));


    }

    public void removeLockedBox(int rowColNum, boolean isRow, int currentBox, int eliminating){
        if(isRow){
            int boxColumn = (currentBox % 3) * 3;
            int[] exception = new int[3];
            exception[0] = boxColumn;
            exception[1] = boxColumn + 1;
            exception[2] = boxColumn + 2;
            int[] rowNums = {rowColNum};
            removeFromRow(rowNums, eliminating, exception);
        }else{
            int boxRow = (currentBox / 3) * 3;
            int[] exception = new int[3];
            exception[0] = boxRow;
            exception[1] = boxRow + 1;
            exception[2] = boxRow + 2;
            int[] columnNums = {rowColNum};
            removeFromColumn(columnNums, eliminating, exception);
        }
    }

    public static void main(String[] args) {
        SudokuSolver sudoku = new SudokuSolver();

        //SudokuSolver sudoku = new SudokuSolver("  6  7  5 3 4   1 8     9   5  9   8  7  1 3 4  6       2  8  6 9  5           4 ");

        //swordfish example(row, 2s)
        //SudokuSolver sudoku = new SudokuSolver("  8  9   3   57  1   1    923     7   54 61   6     389    3   7  84   3   7  6  ");

        //finned swordfish example
        //SudokuSolver sudoku = new SudokuSolver(" 4 7  8       9 6   2 3   18            4  3   9  6  53   5 7     8   4   6  1  9");

        //locked candidate test (xbow also fixes this example)(not always necessarily true)(dont use xbow when doing this example)
        //SudokuSolver sudoku = new SudokuSolver(" 9     1   46 85   26 3 49    7 1    7     8    3 5    42 1 73   78 46   6     4 ");

        //xbow test (9s row 0 , 4)
        //SudokuSolver sudoku = new SudokuSolver("   4  6 2  6   1   9 5   8  5 3     3 12 64 5     7 2  3   2 6   4   9  5 7  9   ");

        //example of hidden pairs  (1,2 in bottom left hand square)
        //SudokuSolver sudoku = new SudokuSolver("  8  9  2 3  5 4  1      6 2    8 9           4 6      7   3  5  6 4    9  2   1 ");
    }
}
