package mrview;

import java.awt.BorderLayout;
import java.awt.Color;
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
public class MRegionAnalysisWindow extends JFrame {

    private MRegionAnalysis mra;
    private JPanel left, image;
    private AnalysisDrawer ad;
    
    public MRegionAnalysisWindow(final MRegionAnalysis mra) {
        this.mra = mra;

        JPanel main = new JPanel(new BorderLayout());
        add(main);

        image = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paintComponent(g);
                MRGraphics mg = new MRGraphics(g, this.getWidth(), this.getHeight());
                mg.setBoundingBox(mra.getBb());
                if (ad != null)
                    ad.draw(mg);
            }

        };
        main.add(image, BorderLayout.CENTER);
        left = new JPanel(new GridLayout(0, 1));
        main.add(left, BorderLayout.WEST);

        double bbarea = mra.getBb().getArea();
        double bbiarea = mra.getBbi().getArea();
        double bbfarea = mra.getBbf().getArea();
        double maxbbarea = bbiarea > bbfarea ? bbiarea : bbfarea;
        
        AnalysisDrawer ifacead = new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getIarea_reg().paint(g, Color.BLACK, Color.GREEN, Color.WHITE, 0.0);
                g.drawBoundingBox(mra.getBb());
            }
        };
        
        AnalysisDrawer ffacead = new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getFarea_reg().paint(g, Color.BLACK, Color.RED, Color.WHITE, 0.0);
                g.drawBoundingBox(mra.getBb());
            }
        };
        
        AnalysisDrawer movead = new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getIarea_reg().paint(g, Color.BLACK, Color.GREEN, Color.WHITE, 0.75);
                mra.getFarea_reg().paint(g, Color.BLACK, Color.RED, Color.WHITE, 0.75);
                for (MFace2 mf : mra.getUregion().mfaces) {
                    Point s = mf.project(MRegionAnalysis.initial).getCenter();
                    Point e = mf.project(MRegionAnalysis.final_).getCenter();
                    if (s != null && e != null)
                        g.drawArrow(new Seg(s, e), Color.BLUE, 3, 10);
                }
                g.drawBoundingBox(mra.getBb());
            }
        };
        
        createJLabel("Faces initial: ", mra.ifacesize, "m²", ifacead);
        createJLabel("Faces final: ", mra.ffacesize, "m²", ffacead);
        createJLabel("Faces diff: ", mra.dfacesize, "m²", null);
        
        createJLabel("Boundingbox change: ", mra.bbchange + " %", null);
        createJLabel("Boundingbox changerate: ", mra.bbchangerate + " %/s", null);
        
        double di = mra.getIarea() / maxbbarea * 100;
        createJLabel("Density Initial:", round(di, 2) + "%", ifacead);
        
        double df = mra.getFarea() / maxbbarea * 100;
        createJLabel("Density Final:", round(df, 2) + "%", ffacead);
        
        double caperc = mra.getCarea() / mra.getFarea() * 100.0;
        createJLabel("Created area:", round(caperc, 2) + "% ("+((int)(mra.getCarea()/mra.duration))+" m²/s)", new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getIarea_reg().paint(g, Color.BLACK, Color.GREEN, Color.WHITE, 0.2);
                mra.getFarea_reg().paint(g, Color.BLACK, Color.RED, Color.WHITE, 0.2);
                mra.getCarea_reg().paint(g, Color.BLACK, Color.BLUE, Color.WHITE, 1.0);
                g.drawBoundingBox(mra.getBb());
            }
        });
        
        double raperc = mra.getRarea() / mra.getIarea() * 100.0;
        createJLabel("Removed area:", round(raperc, 2)  + "% ("+((int)(mra.getRarea()/mra.duration))+" m²/s)", new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getIarea_reg().paint(g, Color.BLACK, Color.GREEN, Color.WHITE, 0.2);
                mra.getFarea_reg().paint(g, Color.BLACK, Color.RED, Color.WHITE, 0.2);
                mra.getRarea_reg().paint(g, Color.BLACK, Color.BLUE, Color.WHITE, 1.0);
                g.drawBoundingBox(mra.getBb());
            }
        });
        
        double ta = mra.getTraversedarea();
        double taperc = ta / maxbbarea * 100;
        createJLabel("Traversed area:", round(taperc, 2)  + "% ("+((int)ta)+" m²)", new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getTraversedarea_reg().paint(g, Color.BLACK, Color.BLUE, Color.WHITE, 1.0);
                g.drawBoundingBox(mra.getBb());
            }
        });
        
        double va = mra.getVisitedarea();
        double vaperc = va / maxbbarea * 100;
        createJLabel("Visited area:", round(vaperc, 2)  + "% ("+((int)va)+" m²)", new AnalysisDrawer () {
            @Override
            public void draw(MRGraphics g) {
                mra.getVisitedarea_reg().paint(g, Color.BLACK, Color.BLUE, Color.WHITE, 1.0);
                g.drawBoundingBox(mra.getBb());
            }
        });
        
        createJLabel("Distance:", mra.lengthdev, "m", movead);
        createJLabel("Speed:", mra.speeddev, "m/s", movead);
//        createJLabel("Angle:", mra.angledev, "", movead);

        setSize(new Dimension(800, 600));
        setVisible(true);
    }

    private void createJLabel(String name, String value, final AnalysisDrawer _ad) {
//        JLabel n = new JLabel(name);
//        left.add(n);
//        JLabel v = new JLabel(value);
//        left.add(v);
        JLabel nv = new JLabel("<HTML>"+name+" "+value+"</HTML>");
        left.add(nv);
        MouseListener mml = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ad = _ad;
                image.repaint();
            }

            public void mouseLeft(MouseEvent e) {
                ad = null;
                image.repaint();
            }
        };
//        n.addMouseListener(mml);
//        v.addMouseListener(mml);
        nv.addMouseListener(mml);
    }
    
    private void createJLabel(String name, double[] vals, String unit, final AnalysisDrawer _ad) {
        String valstr;
        
        valstr = "<HTML>";
        valstr += "nr / sum / mean / stddev / var<BR>";
        valstr += vals[0]+" / "+round(vals[1],2)+unit+" / "+round(vals[2], 2)+unit+" / "+round(vals[3], 2)+"% / "+round(vals[4],2)+"%";
        valstr += "</HTML>";
        
        createJLabel(name, valstr, _ad);
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        if (Double.isInfinite(value))
            return value;
        else if (Double.isNaN(value))
            return value;

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    private abstract class AnalysisDrawer {
        public abstract void draw (MRGraphics g);
    }
}
