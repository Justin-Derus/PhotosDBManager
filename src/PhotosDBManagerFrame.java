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

/**
 * PhotosDBManagerFrame Class
 *
 * Class is responsible for creating the Swing Components
 */
public class PhotosDBManagerFrame extends JFrame{


    private final JPanel etThmbPanel = new JPanel(new GridBagLayout()); /** Thumbnail Panel on west side of Edit Images Tab - Updated in SwingWorkers in MySQLConnection */
    private final GridBagConstraints c = new GridBagConstraints();      /** Constraints for the above panel */
    private final JTextField etLocationField = new JTextField();        /** Edit Images Tab field - used in deleteImageFromDB(),updateImageInDB(),inserIntoDB(),createImageIconList()*/
    private final JTextArea etCaptionArea = new JTextArea();            /** Same as editLocationField above*/

    private MySQLConnection connection; /** Create a connection to the MySQL server */
    private Boolean first = true;       /** Flag to see if user has clicked on the file chooser more than once, keeps abs path for list clicking on insertion tab */
    int maxChars = 250;                 /** Used in text fields to stop too much text entry */

    public PhotosDBManagerFrame() throws SQLException{

        this.setResizable(false);   // GUI is now a set size
        c.gridx = 1;                // X Grid Constraint starts at 1
        c.gridy = 1;                // Y Grid Constraint starts at 1

        this.setPreferredSize(new Dimension(1280,720));     // Preferred size of the GUI set here
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// Closes the frame upon X'ing out of the application
        Dimension dim = this.getPreferredSize();            // Getting the dimensions of the GUI, used for layouts

        // Try to connect to the MySQL server, upon failure give the user a pop-up that says connection error.
        try {
            connection = new MySQLConnection();
        } catch (SQLException e){
            JOptionPane.showMessageDialog(null, "Could not establish a connection with MySQL", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }

        JTabbedPane tabbedPane = new JTabbedPane(); //Creates the tabs for the entire GUI, 3 in total - Insert, Edit, Cam/Tag


        /** -------------------------------------------BEGIN INSERT TAB CODE----------------------------------------- */

        JPanel insertTab = new JPanel();                              //Insert panel that is eventually added to the tabbedPane above
        insertTab.setLayout(new BorderLayout(10,10));                 //Set as BorderLayout to access NESW and Center Easily
        JPanel pathPanel = new JPanel();                              //WEST, contains a label, JList, button
        JLabel pathListLabel = new JLabel("Image Paths");             //Label contained in WEST panel
        DefaultListModel<Object> fileModel = new DefaultListModel<>();//Contains the elements the user will choose from file selector - full list found in cnxn filePaths
        JList<Object> pathList = new JList<>(fileModel);              //JList that will be created based off of the fileModel above
        JScrollPane pathPane = new JScrollPane(pathList);             //JScrollPane contained in WEST panel - allows scrolling of longer lists
        JButton deletePathButton = new JButton("Remove");             //JButton Contained in WEST panel - allows removal of a file that was selected
        JPanel infoPanel = new JPanel();                              //EAST, contains fields for user to enter info about the image
        JPanel submitPanel = new JPanel();                            //SOUTH, contains submit button to confirm insertion into DB
        JButton submitButton = new JButton("Insert Into Database");   //Button contained in submitPanel above
        JPanel selectFilePanel = new JPanel();                        //NORTH, contains file selector button to choose images to insert into DB - related -> Boolean first class field
        JButton selectFileButton = new JButton("Select Image(s)");    //Button contained within selectFilePanel above, in a panel because of centering
        JLabel imageLabel = new JLabel("No image available.");        //CENTER - Label that shows when no images are in the west JList
        JLabel chosenImagePathHL = new JLabel();                      //Hidden label for chosen image path - used for finding which image has been chosen - NEVER SHOWN IN GUI

        //EAST infoPanel Styling and function
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS)); //Create a box layout that makes the panel "Hamburger" style
        infoPanel.setBackground(new Color(82, 84, 87));
        JPanel locationPanel = new JPanel();                                //Panel containing a label "Location"
        JLabel locationLabel = new JLabel("Location");                      //Label found in locationPanel directly above
        JTextField locationTextField = new JTextField();                    //Text field where user enters the location of the image taken
        locationPanel.add(locationLabel);                                   //Add the "Location" label to the panel - goes on left by default
        locationPanel.add(locationTextField);                               //Add the user text field to the panel - goes on right by default
        JPanel captionPanel = new JPanel();                                                 //Create a panel that will hold the caption label and fields
        captionPanel.setLayout(new BoxLayout(captionPanel, BoxLayout.PAGE_AXIS));           //Box layout for the "Hamburger" style panel
        JLabel captionLabel = new JLabel("Caption: " + maxChars + " characters remaining"); //Label that has "Caption" and remaining characters based off maxChars class field
        captionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextArea captionTextArea = new JTextArea();                        //Create a text area for the user to enter the caption
        DefaultStyledDocument doc = new DefaultStyledDocument();            //Document will be styled, only 250 chars max and starts a new line when it hits the end and a word
        doc.setDocumentFilter(new DocumentSizeFilter(maxChars));            //is not too long
        doc.addDocumentListener(new DocumentListener() {                    //Add a listener to see how many characters are left
            @Override
            public void insertUpdate(DocumentEvent e) {captionLabel.setText("Caption: " + (maxChars - doc.getLength()) + " characters remaining");}

            @Override
            public void removeUpdate(DocumentEvent e) {captionLabel.setText("Caption: " + (maxChars - doc.getLength()) + " characters remaining");}

            @Override
            public void changedUpdate(DocumentEvent e) {captionLabel.setText("Caption: " + (maxChars - doc.getLength()) + " characters remaining");}
        });
        captionTextArea.setDocument(doc);                         //Set the text area to the style mentioned above
        captionTextArea.setLineWrap(true);                        //Line wrap for the words cutting off the screen
        captionTextArea.setWrapStyleWord(true);                   //When it wraps, do not cut a word in half
        captionPanel.add(captionLabel);                           //Add the Caption label that updates to the caption panel that is part of the EAST panel
        captionPanel.add(captionTextArea);                        //Add the user text area to the caption panel that is part of the EAST panel
        JPanel cameraPanel = new JPanel();                        //Create a camera panel to hold a label and a JList of all the cameras in the DB
        cameraPanel.setLayout(new BoxLayout(cameraPanel, BoxLayout.PAGE_AXIS)); //"Hamburger" Style
        JLabel cameraLabel = new JLabel("Camera");                //Create a label that goes above the JList/JScrollPane
        cameraLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JList<Object> camerasList;                                //Create the JList that will hold all cameras from the DB for the user to select
        if(connection != null){                                   //If the MySQL Server connection is valid, get the cameras or else provide a blank list
            camerasList = new JList<>(connection.getCamModel());
        } else {
            camerasList = new JList<>();
        }
        camerasList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); //Make the selection mode only 1, 1 camera per picture
        JScrollPane cameraPane = new JScrollPane(camerasList);             //Create a JScrollPane based off the camera list the is based off the cnxn
        cameraPanel.add(cameraLabel);                      //Add the "Camera" label to the panel - goes on top
        cameraPanel.add(cameraPane);                       //Add the JScrollPane below the label
        //Tags panel is the same logic as the camera panel
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
        tagsList.setSelectionModel(new DefaultListSelectionModel() {   //Allow the user to only click once instead of shift clicking
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
        infoPanel.add(locationPanel);       //Add the location panel to the top of the EAST panel
        infoPanel.add(captionPanel);        //Add the caption panel to the upper middle of the EAST panel
        infoPanel.add(cameraPanel);         //Add the camera panel to the lower middle of the EAST panel
        infoPanel.add(tagsPanel);           //Add the tags panel to the bottom of the EAST panel

        //SOUTH submitPanel Styling and function
        submitButton.setBackground(new Color(116, 136, 83));
        submitButton.setMaximumSize(new Dimension(200, 100));
        submitButton.setMinimumSize(new Dimension(200, 100));

        //NORTH selectFilePanel Styling and function
        selectFileButton.setMaximumSize(new Dimension(200, 100));   //Setting the maximum size of the file selector button
        selectFileButton.setMinimumSize(new Dimension(200, 100));   //Setting the minimum size of the file selector button
        selectFilePanel.setBackground(new Color(106, 108, 110));    //Background color of NORTH panel
        selectFilePanel.add(selectFileButton);                      //Add the selectFileButton to the NORTH panel
        selectFileButton.addActionListener(e -> {
            if(connection != null) {                                                                                            //Step 1) Check for MySQL server connection
                FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg", "bmp");  //Step 2) Filter files shown
                JFileChooser fc = new JFileChooser();                                                                           //Step 3) Create a file chooser on click
                //fc.setCurrentDirectory(new File("C:\\Users\\" + connection.getUserDesktop() + "\\Desktop"));
                fc.setFileFilter(imageFilter);                                                                                  //Step 4) Set the file chooser to the filter
                fc.setMultiSelectionEnabled(true);                                                                              //Step 5) Allow multiple files to be chosen
                fc.showOpenDialog(selectFileButton.getParent().getParent());                                                    //Step 6) Show the file chooser
                if (first) {                                                              //First-time 1) Check if first time clicking file chooser
                    connection.setFilePaths(fc.getSelectedFiles());                       //First-time 2) set the cnxn File[] that WEST panel contains
                    for (int i = 0; i < connection.getFilePaths().length; i++) {          //First-time 3) Loop thru paths and add them to fileModel contained in WEST and
                        fileModel.addElement(connection.getFilePaths()[i].getName());     //              get the name of the file for shorter reading
                    }                                                                     //
                    first = false;                                                        //First-time 4) Mark the flag as false - no longer first time clicking fc
                } else {
                    File[] temp = fc.getSelectedFiles();                                  //GOAL: get everything stored in filePaths (AKA cnxn File[])
                    int originalLength = connection.getFilePaths().length;                //Second-time 1) create 2 temp arrays, 1 = new fc files
                    File[] temp2 = new File[originalLength + temp.length];                //   the second = empty but new combined
                    for (int i = 0; i < temp2.length; i++) {                              //   length of original filePaths and the new
                        if (i < originalLength) {                                         //   files to be added
                            temp2[i] = connection.getFilePaths()[i];                      //Second-time 2) based on original length add the files to
                        } else {                                                          //   combined array, if its >= original, add
                            temp2[i] = temp[i - originalLength];                          //   the new stuff
                        }                                                                 //Second-time 3) everything now in temp2, so reset the
                    }                                                                     //   length of original to the combined length
                    connection.setFilePaths(new File[temp2.length]);                      //Second-time 4) loop through new length and get data from
                    for (int i = 0; i < connection.getFilePaths().length; i++) {          //   combined array
                        connection.setFilePaths(temp2, i);                                //Second-time 5) if we already have the name in the model,
                        if (!fileModel.contains(connection.getFilePaths()[i].getName())) {//   don't re-add
                            fileModel.addElement(connection.getFilePaths()[i].getName()); //Second-time 6) revalidate the panel
                        }
                    }
                }
                revalidate();
                //The purpose of the first and second time flag is for allowing the user to add-on more files to the list instead of just overwriting them
                //This also allows for shorter file names via .getName()
            } else {
                //Finally, if there is no connection to the MySQL server, show a pop-up
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });


        //WEST pathPanel Styling and function
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.PAGE_AXIS));     //Box layout creates a "Hamburger" style panel
        pathListLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deletePathButton.setBackground(new Color(190, 114, 80));
        deletePathButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pathPanel.add(pathListLabel);                                           //Add Label to top of WEST panel
        pathPanel.add(pathPane);                                                //Add JScrollPane to WEST panel in middle
        pathPanel.add(deletePathButton);                                        //Add delete button to WEST panel at bottom
        deletePathButton.addActionListener(e -> {
            if(connection != null) {                                            //Step 1) Check MySQL server connection
                String chosenPath = (String) pathList.getSelectedValue();       //Step 2) Get select file from list to delete
                if (!fileModel.isEmpty() && chosenPath != null) {               //Step 3) Check to see the list is not empty and the chosen file is valid
                    fileModel.removeElement(chosenPath);                        //Step 4) Remove the chosen file from the model THIS CAN BE SEEN
                    imageLabel.setIcon(null);                                   //Step 5) STOP DISPLAYING THE IMAGE
                    chosenImagePathHL.setText(null);                            //Step 6) Remove the path that is used to create the image display
                    pathList.clearSelection();                                  //Step 7) Remove the selection active from the JLIST on WEST panel
                    if (fileModel.isEmpty()) {                                  //Step 8) Check if the file model is empty to display what the CENTER should say
                        imageLabel.setText("No image available.");
                    } else {
                        imageLabel.setText("No image chosen.");
                    }                                                           //Finally, supply a pop-up for Empty JList and Bad MySQL server connection
                } else {
                    JOptionPane.showMessageDialog(null, "No file selected.", "Error Removing", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        pathList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {                                                              //Step 1) Check to see if the selected value has changed
                for (int i = 0; i < connection.getFilePaths().length; i++) {                            //Step 2) Loop thru FULL paths provided by cnxn filePaths
                    if (pathList.getSelectedValue().equals(connection.getFilePaths()[i].getName())) {   //Step 3) Condition the loop when to do something
                        //File name being displayed but full path for picture visibility
                        chosenImagePathHL.setText(connection.getFilePaths()[i].getPath());              //Step 4) Get the chosen files path
                        StretchIcon centerImage = new StretchIcon(chosenImagePathHL.getText());         //Step 5) Create an icon to display in the CENTER
                        imageLabel.setIcon(centerImage);                                                //Step 6) Display the CENTER image
                        imageLabel.setText(null);                                                       //Step 7) Remove the text and ONLY show the image in the CENTER
                    }
                }
            }
        });
        pathList.addKeyListener(new KeyListener() { //Create a KEY listener to listen for arrow keys on WEST panel
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {   //DOWN arrow key
                if (e.getKeyCode() == KeyEvent.VK_DOWN && pathList.getSelectedIndex() == fileModel.getSize() - 1) {
                    e.consume();
                    pathList.setSelectedIndex(0);
                }
            }
        });

        //CENTER imageLabel Styling and function - Also functions in pathList selection listeners
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        chosenImagePathHL.setText(null);                            //When this is not null, an image will be shown in the CENTER panel - altered by action listeners of WEST

        insertTab.add(selectFilePanel, BorderLayout.NORTH);         //Insert tab NORTH set  - contains file selector button
        insertTab.add(infoPanel, BorderLayout.EAST);                //Insert tab EAST set   - contains info entry areas
        insertTab.add(submitPanel, BorderLayout.SOUTH);             //Insert tab SOUTH set  - contains submit button
        insertTab.add(pathPanel, BorderLayout.WEST);                //Insert tab WEST set   - contains chosen image list/remove button
        insertTab.add(imageLabel, BorderLayout.CENTER);             //Insert tab CENTER set - contains Image
        tabbedPane.addTab("Image Insertion",insertTab);             //Insert tab added to the main tabbed pane

        /** ------------------------------------- BEGIN EDIT IMAGES TAB CODE ---------------------------------------- */

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
        editLocationPanel.add(editLocationLabel);
        editLocationPanel.add(etLocationField);
        editInfoPanel.add(editLocationPanel);

        JPanel editCaptionPanel = new JPanel();
        editCaptionPanel.setLayout(new BoxLayout(editCaptionPanel, BoxLayout.PAGE_AXIS));
        DefaultStyledDocument editDoc = new DefaultStyledDocument();
        JLabel editCaptionLabel = new JLabel("Caption: " + (maxChars - editDoc.getLength()) + " characters remaining");
        editCaptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editCaptionPanel.add(editCaptionLabel);
        editCaptionPanel.add(etCaptionArea);
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
        etCaptionArea.setDocument(editDoc);
        etCaptionArea.setLineWrap(true);
        etCaptionArea.setWrapStyleWord(true);

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
                    connection.deleteImageFromDB(etLocationField, etCaptionArea, editCamList, editTagsList, etThmbPanel, loading, imgLabel);
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
                    connection.updateImageInDB(etLocationField, etCaptionArea, editCamList, editTagsList, imgLabel);
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
            connection.createImageIconList(images, editCamList, etLocationField, etCaptionArea, c, etThmbPanel, loading, imgLabel, editTagsList);//worker thread here
        }

        JScrollPane editScrollPane = new JScrollPane();
        editScrollPane.setViewportView(etThmbPanel);
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
            if (!fileModel.isEmpty() && chosenImagePathHL.getText() != null) {

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
                        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//Directories only allowed
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
                                (String) finalCamerasList.getSelectedValue(), tagsArrayList, editCamList, etLocationField, etCaptionArea,
                                c, etThmbPanel, images, imgLabel, editTagsList, loading);
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }

                //Now that the file is inserted into the database - update the UI to not now the inserted image anymore
                updateUIAfterInsertion(fileModel, imagePath, connection, imageLabel, locationTextField, captionTextArea, finalCamerasList, tagsList, chosenImagePathHL);

            } else { //User has not clicked on a WEST panel list options
                JOptionPane.showMessageDialog(null, "No image chosen.", "Error Inserting", JOptionPane.ERROR_MESSAGE);
                chosenImagePathHL.setText(null);
            }
        });
        submitPanel.add(submitButton);

        EditTagCamPanel editTagCamTab = new EditTagCamPanel(connection, dim);
        tabbedPane.addTab("Edit Tags/Cameras", editTagCamTab);

        JMenuBar bar = new JMenuBar();
        JLabel logo = new JLabel("Photos Database Manager");
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

