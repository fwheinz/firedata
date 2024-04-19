package mrview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MFaceAnalysisWindow extends JFrame {

    private MFaceAnalysis mfa;
    private JPanel left, image;
    private MovingObject[] mo;

    public MFaceAnalysisWindow(MFaceAnalysis mfa) {
        this.mfa = mfa;

        JPanel main = new JPanel(new BorderLayout());
        add(main);

        image = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paintComponent(g);
                MRGraphics mg = new MRGraphics(g, this.getWidth(), this.getHeight());
                System.out.println("Repainting img");
                if (mo != null) {
                    BoundingBox bb = new BoundingBox();
                    for (MovingObject o : mo) {
                        bb.update(o.getBoundingBox());
                    }
                    mg.setBoundingBox(bb);
                    for (MovingObject o : mo) {
                        o.paint(mg, 0, false);
                    }
                    mg.drawBoundingBox(bb);
                }
            }

        };
        main.add(image, BorderLayout.CENTER);
        left = new JPanel(new GridLayout(2, 2));
        main.add(left, BorderLayout.WEST);

        double bbarea = mfa.getMface().getBoundingBox().getArea();
        double bbiarea = mfa.getMface().project(0).getBoundingBox().getArea();
        double bbfarea = mfa.getMface().project(1).getBoundingBox().getArea();
        
        double di = mfa.getIarea() / bbiarea * 100;
        createJLabel("Density Initial:", round(di, 2) + "%", new MovingObject[] {mfa.getIarea_reg()});
        
        double df = mfa.getFarea() / bbfarea * 100;
        createJLabel("Density Final:", round(df, 2) + "%", new MovingObject[] {mfa.getFarea_reg()});
        
        double ca = mfa.getCarea();
        createJLabel("Change of area:", round(ca, 2) + "%", new MovingObject[] {mfa.getCarea_reg()});

        setSize(new Dimension(800, 600));
        setVisible(true);
    }

    public void createJLabel(String name, String value, final MovingObject[] mos) {
        JLabel n = new JLabel(name);
        left.add(n);
        JLabel v = new JLabel(value);
        left.add(v);
        MouseListener mml = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mo = mos;
                image.repaint();
            }

            public void mouseLeft(MouseEvent e) {
                mo = null;
                image.repaint();
            }
        };
        n.addMouseListener(mml);
        v.addMouseListener(mml);
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
