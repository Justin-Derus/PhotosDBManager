import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySQLConnection{
    private Connection connection;  //MySQL Connection used to link the files together for usage of MySQL
    private File[] filePaths;       //stores list of absolute paths to load the images in DatabaseFrame.java
    private final DefaultListModel<Object> tagModel = new DefaultListModel<>();  //stores list of tags used in EditTagCamPanel.java
    private final DefaultListModel<Object> camModel = new DefaultListModel<>();  //stores list of cams used in EditTagCamPanel.java
    private String[] tags;          //EditTagCamPanel.java line 122ish - used for loading database info to panel
    private String[] cameras;       //EditTagCamPanel.java line 122ish - used for loading database info to panel
    private MyImage imageForUpdate; //image chosen by user - used in updating and deleting functions
    private JButton chosenButton;   //which thumbnail the user has clicked on and data will load for
    private int thumbnailCount = 0; //used for calculating how many images in database
    private String imgDir = "";     //string containing where images will be copied to AKA `relpath` location in DB

    public MySQLConnection() throws SQLException {
        //System.out.println(getDriverType() + getHost());
        connection = DriverManager.getConnection(getDriverType() + getHost(), getUser(), getPass());
        //System.out.println("Connected Successfully");
    }

    protected void createImageIconList(ArrayList<MyImage> images, JList<Object> editCamList,
                                       JTextField locationField, JTextArea captionArea, GridBagConstraints c, JPanel panel, JLabel loading, JLabel imageLabel,
                                       JList<Object> editTagsList) {
        SwingWorker<Void, ImageIcon> worker = new SwingWorker<Void, ImageIcon>() {

            float percent = 0;
            double counter = 0;
            long startTime = 0;
            long endTime = 0;
            @Override
            protected Void doInBackground() {
                System.out.println("Starting icon list worker thread...");
                startTime = System.currentTimeMillis();

                for (MyImage image : images) {
                    ImageIcon rawImg = new ImageIcon(image.getAbsPath());
                    ImageIcon img = new ImageIcon(rawImg.getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH));
                    image.setThumbnail(img);
                    publish(image.getThumbnail());
                    counter++;
                    percent = (float) ((counter / images.size()) * 100);
                }

                return null;
            }

            @Override
            protected void process(List<ImageIcon> chunks) {
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);
                for (ImageIcon icon : chunks) {
                    loading.setText("Loading... " + df.format(percent) + "%");
                    for (int i = 0; i < images.size(); i++) {
                        if (images.get(i).getThumbnail() == icon) {
                            JButton thumbnail = new JButton(icon);
                            thumbnailCount++;
                            int finalI = i;
                            thumbnail.addActionListener(e -> {
                                MyImage chosenImage;
                                chosenButton = thumbnail;
                                int id = images.get(finalI).getId();
                                try {
                                    chosenImage = getImageInfo(id);
                                } catch (SQLException ex) {
                                    throw new RuntimeException(ex);
                                }
                                locationField.setText(chosenImage.getLocation());
                                captionArea.setText(chosenImage.getCaption());
                                //THANKS TO https://github.com/tips4java/tips4java/blob/main/source/StretchIcon.java
                                StretchIcon image = new StretchIcon(chosenImage.getAbsPath());
                                imageLabel.setIcon(image);
                                imageLabel.setText(null);

                                for(int j = 0; j < editCamList.getModel().getSize(); j++){
                                    if(editCamList.getModel().getElementAt(j).equals(chosenImage.getCamera())){
                                        editCamList.clearSelection();
                                        editCamList.setSelectedIndex(j);
                                    }
                                }

                                int[] tagIndices = new int[chosenImage.getTags().size()];
                                for(int j = 0; j < editTagsList.getModel().getSize(); j++){
                                    for(int k = 0; k < chosenImage.getTags().size(); k++){
                                        if(editTagsList.getModel().getElementAt(j).equals(chosenImage.getTags().get(k))){
                                            tagIndices[k] = j;
                                        }
                                    }
                                }
                                editTagsList.clearSelection();
                                editTagsList.setSelectedIndices(tagIndices);

                                setImageForUpdate(chosenImage);

                            });
                            panel.add(thumbnail, c);

                            panel.getParent().getParent().getParent().revalidate();
                            panel.getParent().getParent().getParent().repaint();

                            if(c.gridx % 3 == 0){
                                c.gridx = 0;
                                c.gridy +=1;
                            }
                            c.gridx++;

                        }
                    }
                }
            }

            @Override
            protected void done(){
                endTime = System.currentTimeMillis();
                System.out.println("Icon list worker thread completed: " + (endTime - startTime) + "ms");
                if(images.size() != 0)
                    System.out.println("Average icon load time = " + ((endTime - startTime)/images.size()) + "ms");
                loading.setText(thumbnailCount + " images");
            }
        };
        worker.execute();
    }

    private MyImage getImageInfo(int id) throws SQLException { //called in a SwingWorker - DOES NOT NEED THREADING
        System.out.println("Retrieving information from the database upon thumbnail click of id: " + id + ".");
        MyImage temp = new MyImage();
        Statement statement = connection.createStatement();
        String query = "SELECT `abspath`,`cameraname`,`location`,`caption` FROM `images` WHERE `id` = " + id + ";";
        ResultSet results = statement.executeQuery(query);
        while(results.next()){
            temp.setId(id);
            temp.setAbsPath(results.getString("abspath"));
            temp.setCamera(results.getString("cameraname"));
            temp.setLocation(results.getString("location"));
            temp.setCaption(results.getString("caption"));
        }
        String tagQuery = "SELECT `tagname` FROM `tagged` WHERE `id` = " + id + ";";
        statement.executeQuery(tagQuery);
        ResultSet tags = statement.executeQuery(tagQuery);
        while(tags.next()){
            temp.addTag(tags.getString("tagname"));
        }
        return temp;
    }

    protected void insertIntoDB(int latestId, File imagePath, String location, String caption, String camera, ArrayList<String> tags,
                                JList<Object> editCamList, JTextField locationField, JTextArea captionArea, GridBagConstraints c, JPanel panel,
                                ArrayList<MyImage> images, JLabel imageLabel, JList<Object> editTagsList, JLabel loading){
        System.out.println("Starting insertion worker thread...");

        SwingWorker<Void, ImageIcon> worker = new SwingWorker<Void, ImageIcon>() {

            long startTime = 0;
            long endTime = 0;

            @Override
            protected Void doInBackground() throws Exception {
                //Keep track of time it takes to insert
                startTime = System.currentTimeMillis();

                //Copy the image from the source directory into the chosen directory
                String imgAbsPath = copyImageFile(imagePath, latestId);

                //Obtain the relative path by splitting and readding \\\\ for db storage
                String[] strArray = imgAbsPath.split("\\\\");
                //-3 because there are null separators after every directory in array
                String imgRelPath = strArray[strArray.length - 3] + "\\\\" + strArray[strArray.length - 1];

                //Insert the chosen image into the DB - SQL found here
                insertImageIntoDB(imgAbsPath, imgRelPath, location, caption, camera, tags, latestId);

                //Create MyImage object with all information for a particular image
                MyImage temp = new MyImage(imgAbsPath, imgRelPath, location, camera, caption, tags);
                //Set the ID for MyImage object, not part of constructor - need to use most recent ID
                temp.setId(latestId);

                //ArrayList holds all current information about images in the DB, need to add newly inserted image
                //because thumbnail list on edit page needs updating

                //Create a resized image that will become a button thumbnail when published
                ImageIcon rawImg = new ImageIcon(imgAbsPath);
                ImageIcon img = new ImageIcon(rawImg.getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH));

                temp.setThumbnail(img);
                images.add(temp);

                //Call for an update to the UI
                publish(temp.getThumbnail());

                return null;
            }

            @Override
            protected void process(List<ImageIcon> chunks){
                int finalIndex = images.size() - 1; //used to get last index of images array for creating thumbnail list

                //Create a clickable thumbnail, when clicked it will display all image info from the DB
                //Setting image icon here
                JButton thumbnail = new JButton(chunks.get(0)); //only possible index
                System.out.println("hehehehe: " + chunks.get(0));

                //Increment counter to display all available images
                thumbnailCount++;
                loading.setText(thumbnailCount + " images");

                //Add actionListener to get all the information from the DB when clicked
                thumbnail.addActionListener(e -> {
                    chosenButton = thumbnail; //chosenButton is a class member for MyConnection
                    MyImage chosenImage;      //creating a temporary MyImage object for the button user has clicked
                    int id = images.get(finalIndex).getId();
                    try {
                        chosenImage = getImageInfo(id);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                    locationField.setText(chosenImage.getLocation());
                    captionArea.setText(chosenImage.getCaption());
                    //THANKS TO https://github.com/tips4java/tips4java/blob/main/source/StretchIcon.java
                    StretchIcon image = new StretchIcon(chosenImage.getAbsPath());
                    imageLabel.setIcon(image);
                    imageLabel.setText(null);

                    for(int j = 0; j < editCamList.getModel().getSize(); j++){
                        if(editCamList.getModel().getElementAt(j).equals(chosenImage.getCamera())){
                            editCamList.clearSelection();
                            editCamList.setSelectedIndex(j);
                        }
                    }

                    int[] tagIndices = new int[chosenImage.getTags().size()];
                    for(int j = 0; j < editTagsList.getModel().getSize(); j++){
                        for(int k = 0; k < chosenImage.getTags().size(); k++){
                            if(editTagsList.getModel().getElementAt(j).equals(chosenImage.getTags().get(k))){
                                tagIndices[k] = j;
                            }
                        }
                    }
                    editTagsList.clearSelection();
                    editTagsList.setSelectedIndices(tagIndices);
                    setImageForUpdate(chosenImage);

                });

                //Add the clickable thumbnail to the panel with the next available grid position
                //NOTE: must increment the grid after placing the thumbnail
                panel.add(thumbnail, c);
                if(c.gridx % 3 == 0){
                    c.gridx = 0;
                    c.gridy +=1;
                }
                c.gridx++;
            }

            @Override
            protected void done() {
                endTime = System.currentTimeMillis();
                System.out.println("Insertion Worker thread is complete: " + (endTime - startTime) + "ms");
            }
        };
        worker.execute();
    }

    //called in database frame on submitImageUpdateButton
    public void updateImageInDatabase(JTextField location, JTextArea caption, JList<Object> camList, JList<Object> tagList, JLabel imgLabel) throws SQLException {
        SwingWorker<List<Void>, Void> worker = new SwingWorker<List<Void>, Void>() {
            @Override
            protected List<Void> doInBackground() throws Exception {
                System.out.println("Starting update image in database worker thread...");
                final int x = validImageUpdate(getImageForUpdate(), location, caption, camList, tagList); //3 = new changes, not null | 2 = not null, no changes | 1 = null image

                if (x == 3) {
                    Statement statement = connection.createStatement();

                    //delete FK constraint first, remove all tags, then add new ones based on user entry
                    String deleteTagged = "DELETE FROM `tagged` WHERE `id` = " + getImageForUpdate().getId() + ";";
                    statement.executeUpdate(deleteTagged);
                    for (Object tag : tagList.getSelectedValuesList()) {
                        String addTagged = "INSERT INTO `tagged` (`id`,`tagname`) VALUES (" + getImageForUpdate().getId() + ",'" + tag + "');";
                        statement.executeUpdate(addTagged);
                    }

                    //UPDATE `images` SET `id`='[value-1]',`path`='[value-2]',`dateandtime`='[value-3]',`cameraname`='[value-4]',`location`='[value-5]',`caption`='[value-6]' WHERE 1
                    String updateImages = "UPDATE `images` SET `cameraname`='" + camList.getSelectedValue() + "',`location`='" + location.getText() + "',`caption`='" + caption.getText() +
                            "' WHERE `id`=" + getImageForUpdate().getId() + ";";
                    statement.executeUpdate(updateImages);
                    System.out.println("Update executed for image: " + getImageForUpdate().getId() + " in `images` and `tagged`.");
                    setImageForUpdate(null);
                    clearInfo(location, caption, camList, tagList, imgLabel);
                } else if (x == 2) {
                    JOptionPane.showMessageDialog(null, "No alterations made.", "Error Updating", JOptionPane.ERROR_MESSAGE);
                } else if (x == 1) {
                    JOptionPane.showMessageDialog(null, "No image chosen.", "Error Updating", JOptionPane.ERROR_MESSAGE);
                    setImageForUpdate(null);
                    clearInfo(location, caption, camList, tagList, imgLabel);
                }
                return null;
            }

            @Override
            protected void done() {
                System.out.println("Update image in database worker thread completed.");
            }
        };
        worker.execute();
    }

    //called in updateImageInDatase, which is a SwingWorker
    private int validImageUpdate(MyImage image, JTextField location, JTextArea caption, JList<Object> camList, JList<Object> tagList){
        if(image == null)
            return 1; //image is null dont update

        if (location.getText().equals(image.getLocation()) && caption.getText().equals(image.getCaption())
                && camList.getSelectedValue().equals(image.getCamera()) && tagList.getSelectedValuesList().equals(image.getTags()))
            return 2; //no changes dont update

        return 3; //changes made - update
    }

    protected void deleteImageFromDatabase(JTextField location, JTextArea caption, JList<Object> camList, JList<Object> tagList,
                                           JPanel iconGrid, JLabel loading, JLabel imgLabel) throws SQLException{
        SwingWorker<List<Void>,Void> worker = new SwingWorker<List<Void>,Void>() {
            @Override
            protected List<Void> doInBackground() throws Exception {
                System.out.println("Starting delete image from database worker thread...");
                if (getImageForUpdate() != null) {
                    int choice = JOptionPane.showConfirmDialog(null, "Are you sure?", "Warning", JOptionPane.YES_NO_OPTION);
                    if(choice == JOptionPane.NO_OPTION){
                        return null;
                    }
                    Statement statement = connection.createStatement();
                    String removeTagged = "DELETE FROM `tagged` WHERE `id` = " + getImageForUpdate().getId() + ";";
                    String removeImage = "DELETE FROM `images` WHERE `id` = " + getImageForUpdate().getId() + ";";
                    statement.executeUpdate(removeTagged);
                    statement.executeUpdate(removeImage);
                    System.out.println("Image " + getImageForUpdate().getId() + " deleted from database.");
                    System.out.println("Image to remove from upload = " + getImageForUpdate().getAbsPath());
                    location.setText(null);
                    caption.setText("");
                    camList.clearSelection();
                    tagList.clearSelection();
                    chosenButton.setVisible(false);
                    iconGrid.getParent().getParent().getParent().revalidate();
                    iconGrid.getParent().getParent().getParent().repaint();
                    thumbnailCount--;
                    loading.setText(thumbnailCount + " images");
                    File fileToDelete = new File(getImageForUpdate().getAbsPath());
                    Boolean deleted = fileToDelete.delete();
                    if(deleted){
                        System.out.println("Removed: " + fileToDelete.getName());
                    }
                    setImageForUpdate(null);
                } else {
                    JOptionPane.showMessageDialog(null, "No image chosen.", "Error Deleting", JOptionPane.ERROR_MESSAGE);
                }
                clearInfo(location, caption, camList, tagList, imgLabel);
                return null;
            }

            @Override
            protected void done() {
                System.out.println("Delete image from database worker thread completed.");
            }
        };
        worker.execute();
    }

    //used in delete and submit buttons on edit image page at end if there is no image chosen, Called in update and delete SwingWorkers
    public void clearInfo(JTextField location, JTextArea caption, JList<Object> camList, JList<Object> tagList, JLabel imageLabel){
        imageLabel.setText("No image chosen.");
        imageLabel.setIcon(null);
        location.setText(null);
        caption.setText("");
        camList.clearSelection();
        tagList.clearSelection();
    }

    //UPDATES `tagged` TABLE - FK W/ ON UPDATE CASCADE UPDATES ENTIRE TABLE, LOGIC CHECKS AND MESSAGE DIALOGS - FULLY WORKS
    public void updateTagInDatabase(String newTagName, String oldTagName, DefaultListModel<Object> tagModel) throws SQLException {
        String[] capsArray = newTagName.split("\\s+");
        StringBuilder capitalizedWord = new StringBuilder();
        for(String s : capsArray){
            String first = s.substring(0,1);
            String afterFirst = s.substring(1);
            capitalizedWord.append(first.toUpperCase()).append(afterFirst).append(" ");
        }
        newTagName = capitalizedWord.toString().trim();//trim off the trailing white space created in the for loop above at end
        if(newTagName.equals(oldTagName)){
            return;
        }
        Statement statement = connection.createStatement();
        String update = "UPDATE `tags` SET `tagname` = '" + newTagName + "' WHERE `tagname` = '" + oldTagName + "';";
        statement.executeUpdate(update);
        tagModel.removeElement(oldTagName);
        tagModel.addElement(newTagName);
        sortModel(tagModel);
    }

    //ADDS TO `tag` TABLE - FULLY WORKING WITH MESSAGE DIALOGS AND LOGIC CHECKS
    public void addTagToDatabase(String tagName, DefaultListModel<Object> tagModel) throws SQLException {
        try {
            if (tagName == null || tagName.isEmpty() || tagName.trim().length() == 0) {
                JOptionPane.showMessageDialog(null, "No tag entered.", "Error Inserting", JOptionPane.ERROR_MESSAGE);
            } else {
                String[] caps = tagName.split("\\s+");
                StringBuilder capitalizedWord = new StringBuilder();
                for (String s : caps) {
                    String first = s.substring(0, 1);
                    String afterFirst = s.substring(1);
                    capitalizedWord.append(first.toUpperCase()).append(afterFirst).append(" ");
                }
                tagName = capitalizedWord.toString().trim();
                Statement statement = connection.createStatement();
                String insertStatement = "INSERT INTO `tags`(`tagname`) VALUES ('" + tagName + "');";
                statement.executeUpdate(insertStatement);
                tagModel.addElement(tagName);
                sortModel(tagModel);
            }
        } catch (SQLException e){
            JOptionPane.showMessageDialog(null,"Tag already exists.", "Error Inserting", JOptionPane.ERROR_MESSAGE);
        }
    }

    //DELETE `tagname` FROM `tag` AND `tagged` LOGIC CHECKS AND MESSAGE DIALOGS - FULLY WORKS
    public void removeTagFromDatabase(String tagName, DefaultListModel<Object> tagModel, JList<Object> tagList) throws SQLException {
        if(tagModel.isEmpty()){
            JOptionPane.showMessageDialog(null,"No tags left.", "Error Deleting", JOptionPane.ERROR_MESSAGE);
            tagList.clearSelection();
            return;
        }
        if (tagName == null || tagName.isEmpty()) {
            JOptionPane.showMessageDialog(null,"No tag chosen.", "Error Deleting", JOptionPane.ERROR_MESSAGE);
        } else {
            int confirm = JOptionPane.showConfirmDialog(null, "Warning: delete this from tables `tag` AND `tagged`?", "Deletion confirmation",
                    JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION) {
                Statement statement = connection.createStatement();
                String deleteFromTagged = "DELETE FROM `tagged` WHERE `tagname` = \"" + tagName + "\";";
                statement.executeUpdate(deleteFromTagged);
                String deleteFromTags = "DELETE FROM `tags` WHERE `tagname` = \"" + tagName + "\";";
                statement.executeUpdate(deleteFromTags);
                tagModel.removeElement(tagName);
                sortModel(tagModel);
                tagList.clearSelection();
            } else {
                tagList.clearSelection();
            }
        }
    }

    //UPDATES `cameras` AND `images` BECAUSE OF FK CONSTRAINT W/ UPDATE ON CASCADE
    public void updateCameraInDatabase(String newCamName, String oldCamName, DefaultListModel<Object> camModel) throws SQLException {
        String[] capsArray = newCamName.split("\\s+");
        StringBuilder capitalizedWord = new StringBuilder();
        for(String s : capsArray){
            String first = s.substring(0,1);
            String afterFirst = s.substring(1);
            capitalizedWord.append(first.toUpperCase()).append(afterFirst).append(" ");
        }
        newCamName = capitalizedWord.toString().trim();//trim off the trailing white space created in the for loop above at end
        if(newCamName.equals(oldCamName)){
            return;
        }
        Statement statement = connection.createStatement();
        String update = "UPDATE `cameras` SET `cameraname` = '" + newCamName + "' WHERE `cameraname` = '" + oldCamName + "';";
        statement.executeUpdate(update);
        camModel.removeElement(oldCamName);
        camModel.addElement(newCamName);
        sortModel(camModel);
    }

    //adds camera to cameras table, fully works
    public void addCamToDatabase(String camName, DefaultListModel<Object> camModel){
        try {
            if (camName == null || camName.isEmpty() || camName.trim().length() == 0) {
                JOptionPane.showMessageDialog(null, "No camera entered.", "Error Inserting", JOptionPane.ERROR_MESSAGE);
            } else {
                String[] caps = camName.split("\\s+");
                StringBuilder capitalizedWord = new StringBuilder();
                for(String s : caps){
                    String first = s.substring(0,1);
                    String afterFirst = s.substring(1);
                    capitalizedWord.append(first.toUpperCase()).append(afterFirst).append(" ");
                }
                camName = capitalizedWord.toString().trim();
                Statement statement = connection.createStatement();
                String insertStatement = "INSERT INTO `cameras`(`cameraname`) VALUES ('" + camName + "');";
                statement.executeUpdate(insertStatement);
                camModel.addElement(camName);
                sortModel(camModel);
            }
        }
        catch (SQLException e){
            JOptionPane.showMessageDialog(null, "Camera already exists.", "Error Inserting", JOptionPane.ERROR_MESSAGE);
        }
    }

    //DELETE FROM `cameras` UPDATE `images`.`cameraname` TO NULL, LOGIC CHECK AND MESSAGE DIALOG - FULLY WORKS
    public void removeCamFromDatabase(String camName, DefaultListModel<Object> camModel, JList<Object> camList) throws SQLException {
        if(camModel.isEmpty()){
            JOptionPane.showMessageDialog(null,"No cameras left.", "Error Deleting", JOptionPane.ERROR_MESSAGE);
            camList.clearSelection();
            return;
        }
        if (camName == null || camName.isEmpty()) {
            JOptionPane.showMessageDialog(null,"No camera chosen.", "Error Deleting", JOptionPane.ERROR_MESSAGE);
        } else {
            int confirm = JOptionPane.showConfirmDialog(null, "Warning: delete this from tables `tag` AND `tagged`?", "Deletion confirmation",
                    JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION) {
                Statement statement = connection.createStatement();
                String deleteFromImages = "UPDATE `images` SET `cameraname` = NULL WHERE `cameraname` = '" + camName + "';";
                statement.executeUpdate(deleteFromImages);
                String deleteFromCameras = "DELETE FROM `cameras` WHERE `cameraname` = \"" + camName + "\";";
                statement.executeUpdate(deleteFromCameras);
                camModel.removeElement(camName);
                sortModel(camModel);
            } else {
                camList.clearSelection();
            }
        }
    }

    private void sortModel(DefaultListModel<Object> model){
        ArrayList<String> list = new ArrayList<>();
        for(int i = 0; i < model.size(); i++){
            list.add((String) model.get(i));
        }
        Collections.sort(list);
        model.removeAllElements();
        for(String stringElement : list){
            model.addElement(stringElement);
        }
    }

    public int getLatestId() throws SQLException {
        int id = -1;
        if(connection != null) {
            Statement statement = connection.createStatement();
            ResultSet resultSet;
            //since `id` has A_I, get the last known A_I and use it as the next id
            resultSet = statement.executeQuery("SELECT AUTO_INCREMENT FROM information_schema.tables WHERE table_name = 'images';");
            while (resultSet.next()) {
                id = resultSet.getInt(1);
            }
        }
        return id;
    }

    public String[] fillTagsArray() throws SQLException {
        Statement statement;
        ArrayList<String> tagList = new ArrayList<>();
        if(connection != null) {
            statement = connection.createStatement();
            ResultSet resultSet;
            resultSet = statement.executeQuery("SELECT * FROM tags;");
            while (resultSet.next()) {
                tagList.add(resultSet.getString(1));
            }
        }
        return tagList.toArray(new String[0]); //convert to array that will be used for showing
        //database names
    }

    public String[] fillCamerasArray() throws SQLException{
        Statement statement;
        ArrayList<String> cameraList = new ArrayList<>();
        if(connection != null) {
            statement = connection.createStatement();
            ResultSet resultSet;
            resultSet = statement.executeQuery("SELECT * FROM cameras;");
            while (resultSet.next()) {
                cameraList.add(resultSet.getString(1));
            }
        }
        return cameraList.toArray(new String[0]); //convert to array that will be used for showing
    }

    public void insertImageIntoDB(String absPath, String relPath, String location, String caption, String camera, ArrayList<String> tags, int id) throws SQLException {
        //insert into images
        if(connection == null)
            return;
        Statement statement = connection.createStatement();
        String insertStatement = "INSERT INTO `images`(`abspath`, `relpath`, `cameraname`, `location`, `caption`) VALUES " +
                "('" + absPath + "','" + relPath + "','" + camera + "','" + location + "','" + caption + "');";
        if(camera == null){
            insertStatement = "INSERT INTO `images`(`abspath`, `relpath`, `location`, `caption`) VALUES " +
                    "('" + absPath + "','" + relPath + "','" + location + "','" + caption + "');";
        }
        statement.executeUpdate(insertStatement);

        //insert into tagged
        for (String tag : tags) {
            Statement statement2 = connection.createStatement();
            String insertStatement2 = "INSERT INTO `tagged` (`id`,`tagname`) VALUES (" + id + ",'" + tag + "');";
            statement2.executeUpdate(insertStatement2);
        }

        System.out.println("Inserted " + relPath + " into the database");
    }

    public String copyImageFile(File img, int id) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        System.out.println("Copying file from: " + img);
        String imageNameString = img.getName();
        String[] imgStrArray = imageNameString.split("\\.");
        String fileExtension = imgStrArray[1];
        try {

            fis = new FileInputStream(img);
            //get image from source and copy to new spot
            fos = new FileOutputStream(getImgDir() + "IMG" + id + "." + fileExtension);
            int c;

            // Condition check
            // Reading the input file till there is input
            // present
            while ((c = fis.read()) != -1) {

                // Writing to output file of the specified
                // directory
                fos.write(c);
            }

            // By now writing to the file has ended, so

            // Display message on the console
            System.out.println("Copied the file successfully to: " + getImgDir() + "IMG" + id + "." + fileExtension);
        }   // Optional finally keyword but is good practice to
        // empty the occupied space is recommended whenever
        // closing files,connections,streams
        finally {

            // Closing the streams

            if (fis != null) {

                // Closing the fileInputStream
                fis.close();
            }
            if (fos != null) {

                // Closing the fileOutputStream
                fos.close();
            }
        }
        //MUST HAVE 4 \'S - GETS LOST IN DB INSERTION WITH ONLY 2
        return (getImgDir() + "IMG" + id + "." + fileExtension);
    }

    public ArrayList<MyImage> getData() throws SQLException {

        ArrayList<MyImage> myImages = new ArrayList<>();
        if(connection != null) {
            Statement statement = connection.createStatement();
            String query = "SELECT * FROM `images`;";
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                MyImage image = new MyImage();
                image.setId(resultSet.getInt(1));
                image.setAbsPath(resultSet.getString(2));
                //image.setDatetime(resultSet.getString(3)); never used
                image.setCamera(resultSet.getString(4));
                image.setLocation(resultSet.getString(5));
                image.setCaption(resultSet.getString(6));

                Statement statement1 = connection.createStatement();
                String query1 = "SELECT `tagname` FROM `tagged` WHERE `id` = " + image.getId() + ";";
                ResultSet resultSet1 = statement1.executeQuery(query1);
                ArrayList<String> tempTags = new ArrayList<>();
                while (resultSet1.next()) {
                    tempTags.add(resultSet1.getString(1));
                }
                image.setTags(tempTags);
                myImages.add(image);
            }
        }
        return myImages;
    }

    public String getDriverType(){return "jdbc:mysql:";}

    public String getHost(){return "//localhost:3306/photosdbmanager";}

    public String getUser(){return "dev";}

    public String getPass(){return "devpass";}

    public File[] getFilePaths(){return this.filePaths;}

    public void setFilePaths(File[] filePaths) {this.filePaths = filePaths;}

    public void setFilePaths(File[] temp, int i){this.filePaths[i] = temp[i];}

    public DefaultListModel<Object> getCamModel() {return camModel;}

    public DefaultListModel<Object> getTagModel() {return tagModel;}

    public void addTagModelElement(Object s){tagModel.addElement(s);}

    public void addCamModelElement(Object s){camModel.addElement(s);}

    public String[] getTags(){return tags;}

    public String[] getCameras(){return cameras;}

    public void setCameras(String[] s){cameras = s;}

    public void setTags(String[] s){tags = s;}

    public MyImage getImageForUpdate() {return imageForUpdate;}

    public void setImageForUpdate(MyImage imageForUpdate) {this.imageForUpdate = imageForUpdate;}

    public String getUserDesktop() {return "dev";}

    public String getImgDir() {return imgDir;}

    public void setImgDir(String imgDir) {this.imgDir = imgDir;}

}
