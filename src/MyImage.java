import javax.swing.*;
import java.util.ArrayList;

public class MyImage {

    private String absPath;
    private String relPath;
    private String location;
    private String camera;
    private String caption;
    private ArrayList<String> tags = new ArrayList<>();
    private int id;
    private ImageIcon thumbnail; //set and used in MyConnection SwingWorkers

    public MyImage(){}

    public MyImage(String absPath, String relpath, String location, String camera, String caption, ArrayList<String> tags){
        this.absPath = absPath;
        this.relPath = relpath;
        this.location = location;
        this.camera = camera;
        this.caption = caption;
        this.tags = tags;
    }

    public ImageIcon getThumbnail() {return thumbnail;}

    public void setThumbnail(ImageIcon thumbnail) {this.thumbnail = thumbnail;}

    public void setAbsPath(String absPath){this.absPath = absPath;}

    public void setLocation(String location){this.location = location;}

    public void setCamera(String camera) {this.camera = camera;}

    public void setCaption(String caption) {this.caption = caption;}

    public void setId(int id) {this.id = id;}

    public void setTags(ArrayList<String> tags) {this.tags = tags;}

    public String getAbsPath(){return this.absPath;}

    public String getLocation(){return this.location;}

    public String getCamera(){return this.camera;}

    public String getCaption(){return this.caption;}

    public ArrayList<String> getTags(){return this.tags;}

    public void addTag(String tag) { tags.add(tag); }

    public int getId(){ return this.id; }

    public String getRelPath() { return relPath; }

    public void setRelPath(String relPath) { this.relPath = relPath; }
}
