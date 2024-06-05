import com.formdev.flatlaf.intellijthemes.*;

import java.awt.*;
import java.sql.SQLException;

public class PhotosDBManagerApp {

    public static void main(String[] args){
        EventQueue.invokeLater(() -> {
            //FlatNordIJTheme.setup();
            FlatHiberbeeDarkIJTheme.setup();
            //FlatHighContrastIJTheme.setup();
            //FlatLightFlatIJTheme.setup();  can be cool if tab pane can change color
            //FlatMaterialDesignDarkIJTheme.setup();


            try {
                new DatabaseFrame().setVisible(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        });
    }
}
