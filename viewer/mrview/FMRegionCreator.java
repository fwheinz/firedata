package mrview;

import javax.swing.JOptionPane;

public class FMRegionCreator extends ObjectCreator<FMRegion> {
    public static final int
            GETCENTER = 0,
            GETVECTOR = 1,
            GETROTATION = 2,
            ASKNEXT = 3,
            DONE = 4;
    private final FMRegion fmregion;
    private Point c, v;
    private double rotation;
    
    public FMRegionCreator(Region region) {
        this.fmregion = new FMRegion(region);
    }
    
    public FMRegionCreator(FMRegion fmr) {
        this.fmregion = fmr;
    }

    @Override
    protected void paint(MRGraphics g) {
        fmregion.paint(g, fmregion.getEndTime(), true);
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        FMRegionTrans t = fmregion.getLastTransformation();
        switch (state) {
            case GETCENTER:
                c = new Point(x, y);
                return GETVECTOR;
            case GETVECTOR:
                v = new Point(x, y);
                return GETROTATION;
            case GETROTATION:
                rotation = x*2*Math.PI/360;
                Point v0 = t.getV().add(t.getV0());
                double a0 = t.getA()+t.getA0();
                Point c0 = t.getCenter();
                v = v.sub(c);
                c = c.sub(v0).rotate(c0, -a0);
                fmregion.addFMRegionTrans(c, v, rotation, 500000);
                return ASKNEXT;
            case ASKNEXT:
                if (x == JOptionPane.YES_OPTION) {
                    return GETCENTER;
                } else {
                    return DONE;
                }
        }
        throw new UnsupportedOperationException("Foo");
    }
    
    @Override
    protected String stateDescription() {
        switch (getState()) {
            case GETCENTER:
                return "Specify the center of rotation";
            case GETVECTOR:
                return "Set center destination point";
            case GETROTATION:
                return "Specify the rotation angle in degrees";
            case ASKNEXT:
                return "Add another unit?";
        }
        return "";
    }

    @Override
    protected int nextAction(int state) {
        switch (state) {
            case GETCENTER:
            case GETVECTOR:
                return ObjectCreator.GETCLICK;
            case GETROTATION:
                return ObjectCreator.GETNUMBER;
            case ASKNEXT:
                return ObjectCreator.GETYESNO;
            case DONE:
                return ObjectCreator.READY;
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FMRegion getObject(long starttime, long endtime) {
        return fmregion;
    }
}