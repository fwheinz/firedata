package mrview;

import java.util.TimeZone;
import javax.swing.JFrame;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRView {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        MRViewWindow mvw = new MRViewWindow(args);
        mvw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
}
