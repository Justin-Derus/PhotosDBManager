import javax.swing.ImageIcon;
import java.util.ArrayList;

/**
 * MyImage class
 *
 * Class is used for storing the images that are in the database, so they become accessible in the code
 * the members are set after retrieving the information using SQL.
 *
 * Class Objects can be found in MySQLConnection.java
 *
 */
public class MyImage {

    private String absPath;                             /** Stores the absolute path of the image (`abspath` in db) */
    private String relPath;                             /** Stores the relative path of the image (`relpath` in db) */
    private String location;                            /** Stores the location of where the image was taken  (`location` in db) */
    private String camera;                              /** Stores what type of camera was used to take the image (`cameraname` in db) */
    private String caption;                             /** Stores the (`caption` in db) for the image */
    private int id;                                     /** Stores the (`id` in db) AKA the primary key of the image - AUTO INCREMENTED */
    private ArrayList<String> tags = new ArrayList<>(); /** An array list of strings containing all the tags related to the image */
    private ImageIcon thumbnail;                        /** ImageIcon that stores the thumbnails of the images, set and used in MySQLConnection.java SwingWorkers */

    /**
     * Empty Constructor
     */
    public MyImage(){}

    /**
     * Constructor
     *
     * @param absPath contains the absolute path of the image
     * @param relPath contains the relative path of the image (allows use of html <img> tag)
     * @param location contains location of where the image was taken
     * @param camera contains the camera used
     * @param caption contains the caption of the image
     * @param tags contains all tags pertaining to the image
     */
    public MyImage(String absPath, String relPath, String location, String camera, String caption, ArrayList<String> tags){
        this.absPath = absPath;
        this.relPath = relPath;
        this.location = location;
        this.camera = camera;
        this.caption = caption;
        this.tags = tags;
    }


    /**
     * SETTER FUNCTION FOR CLASS FIELDS
     */
    public void setAbsPath(String absPath){this.absPath = absPath;}
    public void setRelPath(String relPath) { this.relPath = relPath; }
    public void setLocation(String location){this.location = location;}
    public void setCamera(String camera) {this.camera = camera;}
    public void setCaption(String caption) {this.caption = caption;}
    public void setId(int id) {this.id = id;}
    public void setTags(ArrayList<String> tags) {this.tags = tags;}
    public void setThumbnail(ImageIcon thumbnail) {this.thumbnail = thumbnail;}

    /**
     * GETTER FUNCTION FOR CLASS FIELDS
     */
    public String getAbsPath(){return this.absPath;}
    public String getRelPath() { return relPath; }
    public String getLocation(){return this.location;}
    public String getCamera(){return this.camera;}
    public String getCaption(){return this.caption;}
    public int getId(){ return this.id; }
    public ArrayList<String> getTags(){return this.tags;}
    public ImageIcon getThumbnail() {return thumbnail;}

    /**
     * addTag(String tag) adds a tag to the array list of tags
     *
     * @param tag the tag to be added to the array list
     */
    public void addTag(String tag) { tags.add(tag); }

}
