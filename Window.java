import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by kevin on 1/16/2017.
 */
public class Window extends JFrame implements ActionListener {

    private JTextField[][] squares;
    private JButton submit;
    SudokuSolver sudokuSolver;

    public Window(){
        this(null);
    }

    public Window(SudokuSolver sudokuSolver){
        this.sudokuSolver = sudokuSolver;

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout(10,10));

        JPanel gui = createSquareGUI();
        contentPane.add(gui, BorderLayout.CENTER);

        submit = new JButton("Submit");
        JPanel temp = new JPanel(new FlowLayout());
        temp.add(submit);
        contentPane.add(temp, BorderLayout.SOUTH);
        submit.addActionListener(this);

        this.pack();
        this.setVisible(true);
        this.setSize(600,600);
    }

    public Window(String sudokuBoard, String[][] possibilities, String title){
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new GridLayout(3,3,10,10));

        //create panels to better format JLabels and put them into an array
        JPanel[] boxes = new JPanel[9];
        for(int i = 0; i < 9; i++){
            boxes[i] = new JPanel(new GridLayout(3,3,5,5));
            boxes[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            boxes[i].setSize(50,50);
        }

        char currentChar;
        JPanel panel;
        for(int index = 0; index < sudokuBoard.length();index++){
            currentChar = sudokuBoard.charAt(index);
            int row = index / 9;
            int column = index % 9;

            if(currentChar == ' '){
                panel = new JPanel(new GridLayout(3,3));
                String possible = possibilities[row][column];
                for(int newIndex = 0; newIndex < possible.length(); newIndex++){
                    JLabel label = new JLabel(possible.charAt(newIndex) + " ");
                    label.setFont(label.getFont().deriveFont(15.0f));
                    panel.add(label);
                }

            }else{
                panel = new JPanel(new FlowLayout());
                JLabel label = new JLabel("" + currentChar);
                label.setFont(label.getFont().deriveFont(25.0f));
                panel.add(label);
            }
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            boxes[(index / 27) * 3 + (column / 3)].add(panel);
        }

        for(int i = 0; i < 9; i++){
            contentPane.add(boxes[i]);
        }

        this.setTitle(title);
        this.pack();
        this.setSize(600,600);
        this.setVisible(true);
    }

    public JPanel createSquareGUI(){
        squares = new JTextField[9][9];

        JPanel panel = new JPanel(new GridLayout(3,3,5,5));
        for(int row = 0; row < 9; row++){
            JPanel temp = new JPanel(new GridLayout(3,3));
            temp.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            temp.setSize(50,50);
            for(int column = 0; column < 9; column++){
                int newRow = ((row / 3) * 3) + column / 3;
                int newColumn = column % 3 + ((row % 3) * 3);
                squares[newRow][newColumn] = new JTextField();
                squares[newRow][newColumn].setFont(squares[newRow][newColumn].getFont().deriveFont(25.0f));
                squares[newRow][newColumn].setHorizontalAlignment(SwingConstants.CENTER);
                temp.add(squares[newRow][newColumn]);
            }
            panel.add(temp);
        }
        return panel;
    }

    public void actionPerformed(ActionEvent e){
        //only occurs when submit button is clicked
        String answerString = "";
        String adding;
        for(JTextField[] rows: squares){
            for(JTextField square: rows){
                String pulling = square.getText();
                if(pulling.equals("")){
                    adding = " ";
                }else{
                    adding = pulling;
                }
                answerString += adding;
            }
        }
        System.out.println("given:" + answerString + "end");
        SudokuSolver s = new SudokuSolver(answerString);
        /*
        answerString is a string of characters that hold all of the values of a given sudoku board going from row to
        row from left to right
         */
    }

    public static void main(String[] args) {
        Window wind = new Window();
    }
}
