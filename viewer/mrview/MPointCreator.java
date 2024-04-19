package mrview;

import java.awt.Color;
import javax.swing.JOptionPane;

public class MPointCreator extends ObjectCreator<MovingObject> {
    public static final int
            GETPOINT = 0,
            ASKNEXTPOINT = 1,
            DONE = 4;
    private MPoint mp = new MPoint();
    private long lastTime;

    @Override
    protected void paint(MRGraphics g) {
        Point p = mp.getLastPoint();
        if (p != null)
            g.drawPoint(p);
        for (UPoint u : mp.getUnits()) {
            Point startPoint = u.getS();
            Point endPoint = startPoint.add(u.getV());
            g.drawLine(new Seg(startPoint, endPoint), Color.red);
            g.drawPoint(startPoint);
            g.drawPoint(endPoint);
        }
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        switch (state) {
            case GETPOINT:
                boolean first = mp.getLastPoint() == null;
                mp.addUPoint(new Point(Math.round(x),Math.round(y)), MRViewWindow.m.getCurrentTime());
                return first ? GETPOINT : ASKNEXTPOINT;
            case ASKNEXTPOINT:
                if (x == JOptionPane.YES_OPTION) {
                    return GETPOINT;
                } else
                    return DONE;
        }
        throw new UnsupportedOperationException("Foo");
    }
    
    @Override
    protected String stateDescription() {
        switch (getState()) {
            case GETPOINT:
                return "Specify the next point";
            case ASKNEXTPOINT:
                return "Add another unit to this mpoint?";
        }
        return "";
    }

    @Override
    protected int nextAction(int state) {
        switch (state) {
            case GETPOINT:
                return ObjectCreator.GETCLICK;
            case ASKNEXTPOINT:
                return ObjectCreator.GETYESNO;
            case DONE:
                return ObjectCreator.READY;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MPoint getObject(long starttime, long endtime) {
        System.out.println("S: "+mp.getStartTime()+" E: "+mp.getEndTime());
        return mp;
    }
}
