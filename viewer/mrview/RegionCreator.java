package mrview;

import javax.swing.JOptionPane;

public class RegionCreator extends ObjectCreator<Region> {
    public static final int
            GETFACEPOINT = 0,
            ASKNEXTHOLE = 1,
            ASKNEXTFACE = 2,
            DONE = 4;
    private final Region region = new Region();
    private Face face = new Face(), lastface;

    @Override
    protected void paint(MRGraphics g) {
        region.paint(g, 0, true);
        face.paint(g, 0, true);
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        switch (state) {
            case GETFACEPOINT:
                face.addPoint(new Point(x, y));
                if (!face.isClosed())
                    return GETFACEPOINT;
                else {
                    face.sort2();
                    if (lastface == null) {
                        region.addFace(face);
                        lastface = face;
                    } else {
                        lastface.addHole(face);
                    }
                    face = new Face();
                    return ASKNEXTHOLE;
                }
            case ASKNEXTHOLE:
                if (x == JOptionPane.YES_OPTION) {
                    return GETFACEPOINT;
                } else {
                    lastface = null;
                    return ASKNEXTFACE;
                }
                
            case ASKNEXTFACE:
                if (x == JOptionPane.YES_OPTION) {
                    return GETFACEPOINT;
                } else
                    return DONE;
        }
        throw new UnsupportedOperationException("Foo");
    }
    
    @Override
    protected String stateDescription() {
        switch (getState()) {
            case GETFACEPOINT:
                return "Specify the next polygon corner";
            case ASKNEXTHOLE:
                return "Add a hole to this face?";
            case ASKNEXTFACE:
                return "Add another face to this region?";
        }
        return "";
    }

    @Override
    protected int nextAction(int state) {
        switch (state) {
            case GETFACEPOINT:
                return ObjectCreator.GETCLICK;
            case ASKNEXTHOLE:
                return ObjectCreator.GETYESNO;
            case ASKNEXTFACE:
                return ObjectCreator.GETYESNO;
            case DONE:
                return ObjectCreator.READY;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Region getObject(long starttime, long endtime) {
        return region;
    }
}
