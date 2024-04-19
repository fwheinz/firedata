package mrview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MRViewCanvas extends JPanel implements MRListener {

    private MRModel model;

    public MRViewCanvas(MRModel model) {
        this.model = model;
        model.addChangeListener(this);
        this.setBackground(Color.WHITE);
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = new Point(e.getPoint().x, e.getPoint().y);
                if (MRGraphics.last == null) {
                    return;
                }
                Point r = MRGraphics.last.positionInModel(p);
                MRViewCanvas.this.model.cursorPosition(r);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = new Point(e.getPoint().x, e.getPoint().y);
                if (MRGraphics.last == null) {
                    return;
                }
                Point r = MRGraphics.last.positionInModel(p);
                MRViewCanvas.this.model.dragEvent(r);
                MRViewCanvas.this.repaint();
            }

        });
        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (MRGraphics.last == null) {
                    return;
                }
                MRViewCanvas.this.model.wheelEvent(e.getWheelRotation());
                MRViewCanvas.this.repaint();
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                MRViewCanvas.this.model.cursorPosition(null);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = new Point(e.getPoint().x, e.getPoint().y);
                if (MRGraphics.last == null) {
                    return;
                }
                MRViewCanvas.this.model.removeDragSegments();
                MRViewCanvas.this.repaint();
            }

        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g2);
        MRGraphics mg = new MRGraphics(g2, this.getWidth(), this.getHeight());
        model.paint(mg);
    }

    @Override
    public void modelChanged(MRModel m) {
        this.model = m;
        this.repaint();
    }

    public void writeSnapshot(FileOutputStream fos) throws IOException {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        paintComponent(graphics);
        graphics.dispose();
        ImageIO.write(img, "png", fos);
    }
    
    public void writeSnapshot(FileOutputStream fos, double frac) throws IOException {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        long cur = model.getCurrentTime();
        model.setCurrentFrac(frac);
        paintComponent(graphics);
        model.setCurrentTime(cur);
        graphics.dispose();
        ImageIO.write(img, "png", fos);
    }

}
