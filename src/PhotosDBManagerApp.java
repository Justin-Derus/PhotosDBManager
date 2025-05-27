import com.formdev.flatlaf.intellijthemes.*;
import java.awt.*;
import java.sql.SQLException;

//DRIVER CLASS FOR THE APPLICATION
public class PhotosDBManagerApp {

    public static void main(String[] args){
        EventQueue.invokeLater(() -> {
            FlatHiberbeeDarkIJTheme.setup();

            try {
                new PhotosDBManagerFrame().setVisible(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        });
    }
}
