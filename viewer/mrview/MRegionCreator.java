package mrview;

import javax.swing.JOptionPane;

public class MRegionCreator extends ObjectCreator<MRegion> {
    public static final int
            GETCENTER = 0,
            GETVECTOR = 1,
            GETROTATION = 2,
            ASKNEXT = 3,
            DONE = 4;
    private final MRegion mregion = new MRegion();
    private final Region region;
    private Point c, v;
    private long lasttime = 0;
    
    public MRegionCreator(Region region) {
        this.region = region;
    }
    
    @Override
    protected void paint(MRGraphics g) {
        mregion.paint(g, mregion.getEndTime(), true);
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        switch (state) {
            case GETCENTER:
                c = new Point(x, y);
                return GETVECTOR;
            case GETVECTOR:
                v = new Point(x, y);
                Point move = new Point(v.sub(c).x, 0);
                URegion u = region.move(move);
                u.iv.setStart(lasttime);
                u.iv.setEnd(lasttime+500000);
                lasttime += 500000;
                mregion.uregions.add(u);
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
                return "Specify the center of movement";
            case GETVECTOR:
                return "Set center destination point";
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
    public MRegion getObject(long starttime, long endtime) {
        return mregion;
    }
}