import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;

public class EditTagCamPanel extends JPanel {

    public EditTagCamPanel(MySQLConnection conn, Dimension dim) throws SQLException {
        this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));

        //BEGIN WEST PANEL

        JPanel westPanel = new JPanel();
        westPanel.setPreferredSize(new Dimension((int) (dim.width/3.33), dim.height));
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.PAGE_AXIS));

        JPanel tagTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel tagName = new JLabel("Update name: (Choose from list)");
        JTextField tagField = new JTextField();
        tagField.setEditable(false); //made editable on tag list selection
        tagField.setPreferredSize(new Dimension((int) ((dim.width/3.33)/3),26));
        JButton tagSubmitUpdate = new JButton("Submit");
        tagTop.add(tagName);
        tagTop.add(tagField);
        tagTop.add(tagSubmitUpdate);

        JPanel tagMiddle = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel addTagLabel = new JLabel("Create New Tag");
        JTextField addTagField = new JTextField();
        addTagField.setPreferredSize(new Dimension((int) ((dim.width/3.33)/3),26));
        JButton addTagSubmit = new JButton("Submit");
        tagMiddle.add(addTagLabel);
        tagMiddle.add(addTagField);
        tagMiddle.add(addTagSubmit);

        JPanel tagBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel deleteTagLabel = new JLabel("Delete Tag: (Choose from list)");
        JTextField deleteTagField = new JTextField();
        deleteTagField.setPreferredSize(new Dimension((int) ((dim.width/3.33)/3),26));
        deleteTagField.setEditable(false);
        JButton deleteTagSubmit = new JButton("Submit");
        JLabel deleteTagWarning = new JLabel();
        tagBottom.add(deleteTagLabel);
        tagBottom.add(deleteTagField);
        tagBottom.add(deleteTagSubmit);
        tagBottom.add(deleteTagWarning);

        westPanel.add(new JPanel());
        westPanel.add(tagTop);
        westPanel.add(tagMiddle);
        westPanel.add(tagBottom);
        westPanel.add(new JPanel());

        //END WEST PANEL
        //BEGIN EAST PANEL

        JPanel eastPanel = new JPanel();
        eastPanel.setPreferredSize(new Dimension((int) (dim.width/3.33), dim.height));
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.PAGE_AXIS));

        JPanel camTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel camName = new JLabel("Update name: (Choose from list)");
        JTextField camField = new JTextField();
        camField.setEditable(false); //made editable on tag list selection
        camField.setPreferredSize(new Dimension((int) ((dim.width/3.33)/3),26));
        JButton camSubmitUpdate = new JButton("Submit");
        camTop.add(camName);
        camTop.add(camField);
        camTop.add(camSubmitUpdate);

        JPanel camMiddle = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel addCamLabel = new JLabel("Create New Camera");
        JTextField addCamField = new JTextField();
        addCamField.setPreferredSize(new Dimension((int) ((dim.width/3.33)/3),26));
        JButton addCamSubmit = new JButton("Submit");
        camMiddle.add(addCamLabel);
        camMiddle.add(addCamField);
        camMiddle.add(addCamSubmit);

        JPanel camBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel deleteCamLabel = new JLabel("Delete Camera: (Choose from list)");
        JTextField deleteCamField = new JTextField();
        deleteCamField.setPreferredSize(new Dimension((int) ((dim.width/3.33)/3),26));
        deleteCamField.setEditable(false);
        JButton deleteCamSubmit = new JButton("Submit");
        JTextArea deleteCamWarning = new JTextArea();
        deleteCamWarning.setPreferredSize(new Dimension((int) ((dim.width/3.33)),52));
        deleteCamWarning.setWrapStyleWord(true);
        deleteCamWarning.setEditable(false);
        deleteCamWarning.setLineWrap(true);
        camBottom.add(deleteCamLabel);
        camBottom.add(deleteCamField);
        camBottom.add(deleteCamSubmit);
        camBottom.add(deleteCamWarning);

        eastPanel.add(new JPanel());
        eastPanel.add(camTop);
        eastPanel.add(camMiddle);
        eastPanel.add(camBottom);
        eastPanel.add(new JPanel());

        //END EAST PANEL
        //BEGIN CENTER PANEL

        JPanel centerPanel = new JPanel();
        centerPanel.setPreferredSize(new Dimension((int) (dim.width/3.33), dim.height));

        JList<Object> tagList;
        JList<Object> camList;
        //POPULATE LISTS IN CENTER PANEL USING ABOVE JLISTS
        if(conn != null) {
            conn.setTags(conn.fillTagsArray());
            conn.setCameras(conn.fillCamerasArray());

            for (int i = 0; i < conn.getTags().length; i++) {
                conn.addTagModelElement(conn.getTags()[i]);
            }

            for (int i = 0; i < conn.getCameras().length; i++) {
                conn.addCamModelElement(conn.getCameras()[i]);
            }

            tagList = new JList<>(conn.getTagModel());
            tagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tagList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) {
                    tagField.setText(tagList.getSelectedValue().toString());
                    tagField.setEditable(true);
                    deleteTagField.setText(tagList.getSelectedValue().toString());
                }
            });

            camList = new JList<>(conn.getCamModel());
            camList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            camList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) {
                    camField.setText(camList.getSelectedValue().toString());
                    camField.setEditable(true);
                    deleteCamField.setText(camList.getSelectedValue().toString());
                }
            });
        } else {
            tagList = new JList<>();
            camList = new JList<>();
        }

        JScrollPane tagPane = new JScrollPane(tagList);
        tagPane.setPreferredSize(new Dimension((int) (centerPanel.getPreferredSize().getWidth()/2),dim.height - 100));
        JScrollPane camPane = new JScrollPane(camList);
        camPane.setPreferredSize(new Dimension((int) (centerPanel.getPreferredSize().getWidth()/2),dim.height - 100));

        JPanel tagPanel = new JPanel();
        tagPanel.setLayout(new BoxLayout(tagPanel, BoxLayout.PAGE_AXIS));
        JLabel tagPaneLabel = new JLabel("Tags");
        tagPaneLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        tagPaneLabel.setFont(new Font(tagPaneLabel.getFont().toString(), Font.BOLD,15));
        tagPanel.add(tagPaneLabel);
        tagPanel.add(tagPane);
        centerPanel.add(tagPanel);

        JPanel camPanel = new JPanel();
        camPanel.setLayout(new BoxLayout(camPanel, BoxLayout.PAGE_AXIS));
        JLabel camPaneLabel = new JLabel("Camera");
        camPaneLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        camPaneLabel.setFont(new Font(camPaneLabel.getFont().toString(), Font.BOLD,15));
        camPanel.add(camPaneLabel);
        camPanel.add(camPane);
        centerPanel.add(camPanel);

        tagSubmitUpdate.addActionListener(e -> {
            if(conn != null) {
                int x = validSelection(tagList, tagField);
                if (x == 3) {
                    try {
                        conn.updateTagInDatabase(tagField.getText(), tagList.getSelectedValue().toString(), conn.getTagModel());
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (x == 2) {
                    JOptionPane.showMessageDialog(null, "Same tag entered.", "Error Updating", JOptionPane.ERROR_MESSAGE);
                } else if (x == 1) {
                    JOptionPane.showMessageDialog(null, "No tag chosen.", "Error Updating", JOptionPane.ERROR_MESSAGE);
                }
                tagField.setText(null);
                tagField.setEditable(false);
                deleteTagField.setText(null);
                tagList.clearSelection();
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }

        });

        addTagSubmit.addActionListener(e -> {
            if(conn != null) {
                try {
                    conn.addTagToDatabase(addTagField.getText(), conn.getTagModel());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
            addTagField.setText(null);
        });

        deleteTagSubmit.addActionListener(e -> {
            if(conn != null) {
                try {
                    conn.removeTagFromDatabase(deleteTagField.getText(), conn.getTagModel(), tagList);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
            deleteTagField.setText(null);
            tagField.setText(null);
            tagField.setEditable(false);
        });

        deleteTagSubmit.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override //enters the component
            public void mouseEntered(MouseEvent e) {
                deleteTagWarning.setText("Warning: " + deleteTagField.getText() + " will be deleted from `tags` and `tagged`");
            }

            @Override //exits the component
            public void mouseExited(MouseEvent e) {
                deleteTagWarning.setText(null);
            }
        });

        camSubmitUpdate.addActionListener(e -> {
            if(conn != null) {
                int x = validSelection(camList, camField);
                if(x == 3){
                    try {
                        conn.updateCameraInDatabase(camField.getText(), camList.getSelectedValue().toString(), conn.getCamModel());
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                    camField.setText(null);
                    camField.setEditable(false);
                    deleteCamField.setText(null);
                    camList.clearSelection();
                } else if (x == 2){
                    JOptionPane.showMessageDialog(null, "Same camera entered.", "Error Updating", JOptionPane.ERROR_MESSAGE);
                } else if (x == 1){
                    JOptionPane.showMessageDialog(null, "No camera chosen.", "Error Updating", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        addCamSubmit.addActionListener(e -> {
            if(conn != null) {
                conn.addCamToDatabase(addCamField.getText(), conn.getCamModel());
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
            addCamField.setText(null);
        });

        deleteCamSubmit.addActionListener(e -> {
            if(conn != null) {
                try {
                    conn.removeCamFromDatabase(deleteCamField.getText(), conn.getCamModel(), camList);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No connection established", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
            deleteCamField.setText(null);
            camField.setText(null);
            camField.setEditable(false);
        });

        deleteCamSubmit.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override //enters the component
            public void mouseEntered(MouseEvent e) {
                deleteCamWarning.setText("Warning: " + deleteCamField.getText() + " will be deleted from `cameras` and set" +
                        " to null in `images`");
            }

            @Override //exits the component
            public void mouseExited(MouseEvent e) {
                deleteCamWarning.setText(null);
            }
        });

        //END CENTER PANEL
        //ADD ALL PANELS TO THIS

        this.add(westPanel, BorderLayout.WEST);
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(eastPanel, BorderLayout.EAST);
    }

    public int validSelection(JList<Object> list, JTextField field){

        if(list.getSelectedValue() == null) //we have nothing entered
            return 1;

        if(list.getSelectedValue().equals(field.getText()))//we have the same thing
            return 2;

        return 3; //valid change
    }
}
