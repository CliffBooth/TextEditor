package editor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditor extends JFrame {
    JTextArea textArea;
    JTextField searchField;
    JCheckBox checkBox;
    JFileChooser fileChooser;
    {
        //fileChooser = new JFileChooser(new File("."));
        fileChooser = new JFileChooser();
        fileChooser.setName("FileChooser");
        add(fileChooser, BorderLayout.CENTER);
    }

    java.util.List<java.util.List<Integer>> matches = new ArrayList<>();
    int index;

    public TextEditor() {
        setTitle("The fourth stage");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension d = new Dimension(600, 500);
        setPreferredSize(d);
        setMinimumSize(d);
        setLocationRelativeTo(null);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setName("MenuFile");
        JMenuItem openItem = new JMenuItem("open");
        openItem.addActionListener(e -> openFile());
        openItem.setName("MenuOpen");
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setName("MenuSave");
        saveItem.addActionListener(e -> saveFile());
        JMenuItem exitItem = new JMenuItem("exit");
        exitItem.setName("MenuExit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JMenu searchMenu = new JMenu("Search");
        searchMenu.setName("MenuSearch");
        JMenuItem startSearchItem = new JMenuItem("Start search");
        startSearchItem.setName("MenuStartSearch");
        startSearchItem.addActionListener(e -> find());
        JMenuItem prevItem = new JMenuItem("Previous match");
        prevItem.setName("MenuPreviousMatch");
        prevItem.addActionListener(e -> previous());
        JMenuItem nextItem = new JMenuItem("Next match");
        nextItem.setName("MenuNextMatch");
        nextItem.addActionListener(e -> next());
        JMenuItem regexItem = new JMenuItem("Use regular expressions");
        regexItem.setName("MenuUseRegExp");
        regexItem.addActionListener(e -> checkBox.doClick());
        searchMenu.add(startSearchItem);
        searchMenu.add(prevItem);
        searchMenu.add(nextItem);
        searchMenu.add(regexItem);
        menuBar.add(searchMenu);

        Font f = new FontUIResource(fileMenu.getFont().getFontName(), menuBar.getFont().getStyle(), 15);
        UIManager.put("MenuItem.font", f);
        UIManager.put("Menu.font", f);
        SwingUtilities.updateComponentTreeUI(this);


        textArea = new JTextArea();
        textArea.setName("TextArea");
        textArea.setFont(new Font("Courier", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setName("ScrollPane");
        Border margin = new EmptyBorder(new Insets(10, 25, 25, 25));
        scrollPane.setBorder(margin);
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel();
        add(topPanel, BorderLayout.NORTH);

        JButton openButton = new JButton(getImageIcon("openFile.png"));
        Dimension d = new Dimension(openButton.getIcon().getIconWidth(), openButton.getIcon().getIconHeight());
        openButton.setPreferredSize(d);
        openButton.addActionListener(e -> openFile());
        openButton.setName("OpenButton");
        topPanel.add(openButton);

        JButton saveButton = new JButton(getImageIcon("saveFile.png"));
        saveButton.addActionListener(e -> saveFile());
        saveButton.setName("SaveButton");
        saveButton.setPreferredSize(d);
        topPanel.add(saveButton);

        searchField = new JTextField();
        Dimension dim = new Dimension(200, 25);
        searchField.setMinimumSize(dim);
        searchField.setPreferredSize(dim);
        searchField.setName("SearchField");
        topPanel.add(searchField);

        JButton searchButton = new JButton(getImageIcon("search.png"));
        searchButton.setName("StartSearchButton");
        searchButton.addActionListener(e -> find());
        searchButton.setPreferredSize(d);
        topPanel.add(searchButton);

        JButton prevButton = new JButton(getImageIcon("previous.png"));
        prevButton.setName("PreviousMatchButton");
        prevButton.addActionListener(e -> previous());
        prevButton.setPreferredSize(d);
        topPanel.add(prevButton);

        JButton nextButton = new JButton(getImageIcon("next.png"));
        nextButton.setName("NextMatchButton");
        nextButton.addActionListener(e -> next());
        nextButton.setPreferredSize(d);
        topPanel.add(nextButton);

        checkBox = new JCheckBox();
        checkBox.setName("UseRegExCheckbox");
        topPanel.add(checkBox);

        JLabel regexLabel = new JLabel("use Regex");
        topPanel.add(regexLabel);
        regexLabel.setFont(f);

    }


    private void saveFile() {
        int returnValue = fileChooser.showDialog(null, "Save file");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                Files.writeString(Paths.get(selectedFile.getPath()), textArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openFile() {
        int returnValue = fileChooser.showDialog(null, "Open file");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                textArea.setText(new String(Files.readAllBytes(selectedFile.toPath())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ImageIcon getImageIcon(String filename) {
        Image image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        try {
            image = ImageIO.read(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        image = image.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private void find() {
        if (searchField.getText().isBlank())
            return;
        Thread t;
        if (checkBox.isSelected()) {
            t = new Thread(this::findRegex);
        } else {
            t = new Thread(this::findWord);
        }
        t.start();
    }

    private void findWord() {
        matches = new ArrayList<>();
        index = -1;

        String pattern = searchField.getText();
        String text = textArea.getText();
        int offset = 0;
        int start = text.indexOf(pattern, offset);
        while (start != -1) {
            matches.add(Arrays.asList(start, start + pattern.length()));
            offset = start + pattern.length();
            start = text.indexOf(pattern, offset);
        }
        next();
    }

    private void findRegex() {
        matches = new ArrayList<>();
        index = -1;

        String pattern = searchField.getText();
        String text = textArea.getText();
        Matcher matcher = Pattern.compile(pattern).matcher(text);

        while (matcher.find()) {
            matches.add(Arrays.asList(matcher.start(), matcher.end()));
        }
        next();
    }

    private void next() {
        if (index >= matches.size() - 1) {
            return;
        }
        index++;
        int start = matches.get(index).get(0);
        int end = matches.get(index).get(1);
        textArea.setCaretPosition(end);
        textArea.select(start, end);
        textArea.grabFocus();
    }

    private void previous() {
        if (index <= 0) {
            return;
        }
        index--;
        int start = matches.get(index).get(0);
        int end = matches.get(index).get(1);
        textArea.setCaretPosition(end);
        textArea.select(start, end);
        textArea.grabFocus();
    }

}
