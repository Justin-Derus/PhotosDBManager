import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseFrame extends JFrame{

    private final JPanel panel = new JPanel(new GridBagLayout());//table area
    private final JTextField locationField;
    private final JTextArea captionArea;
    private final GridBagConstraints c = new GridBagConstraints();
    private Boolean first = true; //checks to see if user has used file chooser more than once
    int maxChars = 250;
    private MySQLConnection connection;

    public DatabaseFrame() throws SQLException{
        this.setResizable(false);
        c.gridx = 1;
        c.gridy = 1;

        this.setPreferredSize(new Dimension(1280,720));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension dim = this.getPreferredSize();

        try {
            connection = new MySQLConnection();
        } catch (SQLException e){
            JOptionPane.showMessageDialog(null, "Could not establish a connection with MySQL", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }

        JTabbedPane tabbedPane = new JTabbedPane();

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        JPanel insertTab = new JPanel();
        insertTab.setLayout(new BorderLayout(10,10));

        JPanel infoPanel = new JPanel();                      //contains photo info for user to enter into db
        JPanel submitPanel = new JPanel();                    //submit button at bottom of screen to send info into db
        JPanel selectFilePanel = new JPanel();
        JButton selectFileButton = new JButton("Select Image(s)");
        selectFileButton.setMaximumSize(new Dimension(200, 100));
        selectFileButton.setMinimumSize(new Dimension(200, 100));
        selectFilePanel.add(selectFileButton);
        insertTab.add(selectFilePanel, BorderLayout.NORTH);
        insertTab.add(infoPanel, BorderLayout.EAST);
        insertTab.add(submitPanel, BorderLayout.SOUTH);
        infoPanel.setBackground(new Color(82, 84, 87));
        selectFilePanel.setBackground(new Color(106, 108, 110));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));

        DefaultListModel<Object> model = new DefaultListModel<>();
        selectFileButton.addActionListener(e -> {
            if(connection != null) {
                FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg", "bmp");
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File("C:\\Users\\" + connection.getUserDesktop() + "\\Desktop"));
                fc.setFileFilter(imageFilter);
                fc.setMultiSelectionEnabled(true);
                fc.showOpenDialog(selectFileButton.getParent().getParent());
                if (first) {                                        //check to see if user has added files from fc prev
                    connection.setFilePaths(fc.getSelectedFiles());       //store absolute paths
                    for (int i = 0; i < connection.getFilePaths().length; i++) {
                        model.addElement(connection.getFilePaths()[i].getName());  //get the name of the file for shorter reading
                    }
                    first = false;                                 //no longer on first fc
                } else {
                    File[] temp = fc.getSelectedFiles();                  //GOAL: get everything stored in filePaths
                    int originalLength = connection.getFilePaths().length;//1) create 2 temp arrays, 1 = new fc files
                    File[] temp2 = new File[originalLength + temp.length];//   the second = empty but new combined
                    for (int i = 0; i < temp2.length; i++) {              //   length of original filePaths and the new
                        if (i < originalLength) {                         //   files to be added
                            temp2[i] = connection.getFilePaths()[i];      //2) based on original length add the files to
                        } else {                                          //   combined array, if its >= original, add
                            temp2[i] = temp[i - originalLength];          //   the new stuff
                        }                                                 //3) everything now in temp2, so reset the
                    }                                                     //   length of original to the combined length
                    connection.setFilePaths(new File[temp2.length]);            //4) loop through new length and get data from
                    for (int i = 0; i < connection.getFilePaths().length; i++) {//   combined array
                        connection.setFilePaths(temp2, i);                       //5) if we already have the name in the model,
                        if (!model.contains(connection.getFilePaths()[i].getName())) {  //   don't re-add
                            model.addElement(connection.getFilePaths()[i].getName());     //6) revalidate the panel
                        }
                    }
                }
                revalidate();
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.PAGE_AXIS));
        JLabel pathListLabel = new JLabel("Image Paths");
        pathListLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JList<Object> pathList = new JList<>(model);
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane pathPane = new JScrollPane(pathList);
        JButton deletePathButton = new JButton("Remove");
        deletePathButton.setBackground(new Color(190, 114, 80));
        deletePathButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        pathPanel.add(pathListLabel);
        pathPanel.add(pathPane);
        pathPanel.add(deletePathButton);
        insertTab.add(pathPanel, BorderLayout.WEST);

        JLabel imageLabel = new JLabel("No image available.");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        insertTab.add(imageLabel, BorderLayout.CENTER);
        JLabel chosenImagePathHL = new JLabel(); //Hidden label for chosen image path
        chosenImagePathHL.setText(null);

        deletePathButton.addActionListener(e -> {
            if(connection != null) {
                String chosenPath = (String) pathList.getSelectedValue();
                if (!model.isEmpty() && chosenPath != null) {
                    model.removeElement(chosenPath);
                    imageLabel.setIcon(null);
                    chosenImagePathHL.setText(null);
                    pathList.clearSelection();
                    if (model.isEmpty()) {
                        imageLabel.setText("No image available.");
                    } else {
                        imageLabel.setText("No image chosen.");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "No file selected.", "Error Removing", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        pathList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                //since we are only displaying file names on list, we need the full path to get the image shown
                for (int i = 0; i < connection.getFilePaths().length; i++) {
                    if (pathList.getSelectedValue().equals(connection.getFilePaths()[i].getName())) {

                        //we have the file name being displayed but full path for picture visibility
                        chosenImagePathHL.setText(connection.getFilePaths()[i].getPath());
                        StretchIcon fittedImage = new StretchIcon(chosenImagePathHL.getText());
                        imageLabel.setIcon(fittedImage);
                        imageLabel.setText(null);
                    }
                }
            }
        });

        pathList.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && pathList.getSelectedIndex() == model.getSize() - 1) {
                    e.consume();
                    pathList.setSelectedIndex(0);

                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        JPanel locationPanel = new JPanel();
        JLabel locationLabel = new JLabel("Location");
        JTextField locationTextField = new JTextField();
        locationPanel.add(locationLabel);
        locationPanel.add(locationTextField);

        JPanel captionPanel = new JPanel();
        captionPanel.setLayout(new BoxLayout(captionPanel, BoxLayout.PAGE_AXIS));
        JLabel captionLabel = new JLabel("Caption: " + maxChars + " characters remaining");
        captionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextArea captionTextArea = new JTextArea();
        DefaultStyledDocument doc = new DefaultStyledDocument();
        doc.setDocumentFilter(new DocumentSizeFilter(maxChars));
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                captionLabel.setText("Caption: " + (maxChars - doc.getLength()) + " characters remaining");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                captionLabel.setText("Caption: " + (maxChars - doc.getLength()) + " characters remaining");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                captionLabel.setText("Caption: " + (maxChars - doc.getLength()) + " characters remaining");
            }
        });
        captionTextArea.setDocument(doc);
        captionTextArea.setLineWrap(true);
        captionTextArea.setWrapStyleWord(true);
        captionPanel.add(captionLabel);
        captionPanel.add(captionTextArea);

        JPanel cameraPanel = new JPanel(); //flow layout
        cameraPanel.setLayout(new BoxLayout(cameraPanel, BoxLayout.PAGE_AXIS));
        JLabel cameraLabel = new JLabel("Camera");
        cameraLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JList<Object> camerasList;
        if(connection != null){
            camerasList = new JList<>(connection.getCamModel());
        } else {
            camerasList = new JList<>();
        }
        camerasList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane cameraPane = new JScrollPane(camerasList);

        cameraPanel.add(cameraLabel);
        cameraPanel.add(cameraPane);

        JPanel tagsPanel = new JPanel();
        tagsPanel.setLayout(new BoxLayout(tagsPanel, BoxLayout.PAGE_AXIS));
        JLabel tagsLabel = new JLabel("Tags");
        tagsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JList<Object> tagsList;
        if(connection != null) {
            tagsList = new JList<>(connection.getTagModel());
        } else {
            tagsList = new JList<>();
        }
        tagsList.setSelectionModel(new DefaultListSelectionModel() {
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0))
                    super.removeSelectionInterval(index0, index1);
                else
                    super.addSelectionInterval(index0, index1);
            }
        });
        JScrollPane tagsPane = new JScrollPane(tagsList);

        tagsPanel.add(tagsLabel);
        tagsPanel.add(tagsPane);

        infoPanel.add(locationPanel);
        infoPanel.add(captionPanel);
        infoPanel.add(cameraPanel);
        infoPanel.add(tagsPanel);

        JButton submitButton = new JButton("Insert Into Database");
        submitButton.setBackground(new Color(116, 136, 83));
        submitButton.setMaximumSize(new Dimension(200, 100));
        submitButton.setMinimumSize(new Dimension(200, 100));
        tabbedPane.addTab("Image Insertion",insertTab);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        JPanel editImageTab = new JPanel();
        JPanel loadingPanel = new JPanel();
        JLabel loading = new JLabel("Loading Images");
        loadingPanel.add(loading);

        editImageTab.setLayout(new BorderLayout());
        editImageTab.add(loadingPanel, BorderLayout.NORTH);

        //Get all image info from the database here
        ArrayList<MyImage> images;
        if(connection != null) {
            images = connection.getData();
        } else {
            images = null;
        }

        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel editInfoPanel = new JPanel();
        editInfoPanel.setLayout(new BoxLayout(editInfoPanel, BoxLayout.PAGE_AXIS));
        editImageTab.add(editInfoPanel, BorderLayout.EAST);

        JPanel editLocationPanel = new JPanel();
        JLabel editLocationLabel = new JLabel("Location: ");
        editLocationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        locationField = new JTextField();
        editLocationPanel.add(editLocationLabel);
        editLocationPanel.add(locationField);
        editInfoPanel.add(editLocationPanel);

        JPanel editCaptionPanel = new JPanel();
        editCaptionPanel.setLayout(new BoxLayout(editCaptionPanel, BoxLayout.PAGE_AXIS));
        DefaultStyledDocument editDoc = new DefaultStyledDocument();
        JLabel editCaptionLabel = new JLabel("Caption: " + (maxChars - editDoc.getLength()) + " characters remaining");
        editCaptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        captionArea = new JTextArea();
        editCaptionPanel.add(editCaptionLabel);
        editCaptionPanel.add(captionArea);
        editInfoPanel.add(editCaptionPanel);
        editDoc.setDocumentFilter(new DocumentSizeFilter(maxChars));
        editDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                editCaptionLabel.setText("Caption: " + (maxChars - editDoc.getLength()) + " characters remaining");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                editCaptionLabel.setText("Caption: " + (maxChars - editDoc.getLength()) + " characters remaining");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                editCaptionLabel.setText("Caption: " + (maxChars - editDoc.getLength()) + " characters remaining");
            }
        });
        captionArea.setDocument(editDoc);
        captionArea.setLineWrap(true);
        captionArea.setWrapStyleWord(true);

        JPanel editCamPanel = new JPanel();
        editCamPanel.setLayout(new BoxLayout(editCamPanel, BoxLayout.PAGE_AXIS));
        JLabel editCamLabel = new JLabel("Cameras");
        editCamLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editCamPanel.add(editCamLabel);
        JList<Object> editCamList;
        if(connection != null) {
            editCamList = new JList<>(connection.getCamModel());
        } else {
            editCamList = new JList<>();
        }
        editCamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane editCamScrollPane = new JScrollPane(editCamList);
        editCamPanel.add(editCamScrollPane);
        editInfoPanel.add(editCamPanel);

        JPanel editTagPanel = new JPanel();
        editTagPanel.setLayout(new BoxLayout(editTagPanel, BoxLayout.PAGE_AXIS));
        JLabel editTagLabel = new JLabel("Tags");
        editTagLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editTagPanel.add(editTagLabel);
        JList<Object> editTagsList;
        if(connection != null) {
            editTagsList = new JList<>(connection.getTagModel());
        } else {
            editTagsList = new JList<>();
        }
        editTagsList.setSelectionModel(new DefaultListSelectionModel() {
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0))
                    super.removeSelectionInterval(index0, index1);
                else
                    super.addSelectionInterval(index0, index1);
            }
        });
        JScrollPane editTagScrollPane = new JScrollPane(editTagsList);
        editTagPanel.add(editTagScrollPane);

        editInfoPanel.add(editTagPanel);

        JLabel imgLabel = new JLabel("No Image Chosen");
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel submitUpdatePanel = new JPanel();
        JButton submitImageUpdateButton = new JButton("Submit Update");
        submitImageUpdateButton.setBackground(new Color(116, 136, 83));
        JButton deleteImageButton = new JButton("Delete Image");
        deleteImageButton.setBackground(new Color(190, 114, 80));
        deleteImageButton.addActionListener(e -> {
            if(connection != null) {
                try {
                    connection.deleteImageFromDatabase(locationField, captionArea, editCamList, editTagsList, panel, loading, imgLabel);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        submitImageUpdateButton.addActionListener(e -> {
            if(connection != null) {
                try {
                    connection.updateImageInDatabase(locationField, captionArea, editCamList, editTagsList, imgLabel);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        submitUpdatePanel.add(deleteImageButton);
        submitUpdatePanel.add(submitImageUpdateButton);

        editInfoPanel.add(submitUpdatePanel);

        editImageTab.add(imgLabel, BorderLayout.CENTER);

        //Create thumbnails on edit page with all current DB info
        if(connection != null) {
            connection.createImageIconList(images, editCamList, locationField, captionArea, c, panel, loading, imgLabel, editTagsList);//worker thread here
        }

        JScrollPane editScrollPane = new JScrollPane();
        editScrollPane.setViewportView(panel);
        editImageTab.add(editScrollPane, BorderLayout.WEST);


        tabbedPane.add("Edit Images", editImageTab);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        JList<Object> finalCamerasList = camerasList;
        submitButton.addActionListener(e -> { //SOUTH SECTION OF insertTab

            if(connection == null){
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            //Check to see if the list of images is empty and that the text for the chosen one is not null
            if (!model.isEmpty() && chosenImagePathHL.getText() != null) {

                //Get the chosen file and all tags chosen
                File imagePath = new File(chosenImagePathHL.getText());
                List<Object> tags = tagsList.getSelectedValuesList();
                ArrayList<String> tagsArrayList = new ArrayList<>();
                for (Object tag : tags) {
                    tagsArrayList.add((String) tag);
                }

                //Check to see if user has chosen a destination spot for the images to be copied to
                //if no, create a file selector and get the directory path the user has selected
                if (connection.getImgDir().equals("")) {

                    //Create array of objects that will be buttons for the user
                    Object[] options = {"Go back", "Choose Destination"};

                    //Get the user choice by creating an option pane for them to click on a choice button
                    int choice = JOptionPane.showOptionDialog(null, "Choose a directory for the image to be stored. \n (Will become `path` attribute in database)",
                            "Destination Choice", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);

                    //Based off user choice, create a file selector for them to choose destination
                    if (choice == JOptionPane.NO_OPTION) {          //AKA Options[1] OR "Choose Destination"
                        JFileChooser fc = new JFileChooser();
                        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//Directorys only allowed
                        int option = fc.showOpenDialog(null);     //Open the file selector
                        if (option == JFileChooser.APPROVE_OPTION) {//User has clicked "Choose Destination"
                            File file = fc.getSelectedFile();     //Get selected directory
                            String folder = file.getPath();       //Convert directory to string containing path
                            folder = folder + "\\";               //Add final '\' to string
                            folder = folder.replace("\\", "\\\\"); //Must use '\\\\' to be stored in db as '\'
                            connection.setImgDir(folder);         //Set the image directory for this entire connection
                        }
                    } else {                                      //Options[0] OR "Go Back"
                        return;
                    }
                }

                //Once a destination is chosen, try to insert into DB - catches SQL errors
                try {
                    //Double check to make sure there is a destination set and that the ID is not invalid because of no connection
                    if (connection.getImgDir() != null) {
                        //Insert into DB
                        connection.insertIntoDB(connection.getLatestId(), imagePath, locationTextField.getText(), captionTextArea.getText(),
                                (String) finalCamerasList.getSelectedValue(), tagsArrayList, editCamList, locationField, captionArea,
                                c, panel, images, imgLabel, editTagsList, loading);
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }

                //Now that the file is inserted into the database - update the UI to not now the inserted image anymore
                updateUIAfterInsertion(model, imagePath, connection, imageLabel, locationTextField, captionTextArea, finalCamerasList, tagsList, chosenImagePathHL);

            } else { //User has not clicked on a WEST panel list options
                JOptionPane.showMessageDialog(null, "No image chosen.", "Error Inserting", JOptionPane.ERROR_MESSAGE);
                chosenImagePathHL.setText(null);
            }
        });
        submitPanel.add(submitButton);

        EditTagCamPanel editTagCamTab = new EditTagCamPanel(connection, dim);
        tabbedPane.addTab("Edit Tags/Cameras", editTagCamTab);

        JMenuBar bar = new JMenuBar();
        JLabel logo = new JLabel("DBDive");
        logo.setFont(new Font("Segoe Script", Font.BOLD,15));
        bar.add(logo);
        setJMenuBar(bar);

        add(tabbedPane);
        setFocusable(true);
        pack();
        setLocationRelativeTo(null);

    }

    public void updateUIAfterInsertion(DefaultListModel<Object> model, File path, MySQLConnection connection, JLabel icon,
                                       JTextField location, JTextArea caption, JList<Object> cameras, JList<Object> tags, JLabel chosenPath){

        //Remove the path from WEST panel
        model.removeElement(path.getName());

        //Remove the path from the connection
        //MyConnection has the stored file paths so that the user is allowed to use multiple file selectors and the UI keeps the pathing correct
        ArrayList<File> tempList = new ArrayList<>(Arrays.asList(connection.getFilePaths()));
        for (int i = 0; i < tempList.size(); i++) {
            if (tempList.get(i).getName().equals(path.getName())) {
                tempList.remove(path);
            }
        }
        connection.setFilePaths(tempList.toArray(new File[0]));

        if(model.isEmpty()){
            icon.setText("No image available.");
        } else {
            icon.setText("No image chosen.");
        }
        icon.setIcon(null);            //Remove the image from the center of the tab
        location.setText(null);        //Remove the text from the info panel
        caption.setText("");           //Remove the text from the info panel
        cameras.clearSelection();      //Deselect the camera chosen
        tags.clearSelection();         //Deselect the tag(s) chosen
        chosenPath.setText(null);      //Set user choice to null
    }
}

