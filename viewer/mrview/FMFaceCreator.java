package mrview;

public class FMFaceCreator extends ObjectCreator<MovingObject> {
    public static final int
            GETFACEPOINT = 0,
            GETCENTER = 1,
            GETVECTOR = 2,
            GETROTATION = 3,
            DONE = 4;
    private final Face face = new Face();
    private Point c, v;
    private double rotation;

    @Override
    protected void paint(MRGraphics g) {
        face.paint(g, 0, true);
        if (c != null)
            g.drawPoint(c);
        if (v != null)
            g.drawPoint(v);
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        switch (state) {
            case GETFACEPOINT:
                face.addPoint(new Point(x,y));
                if (!face.isClosed())
                    return GETFACEPOINT;
                else
                    return GETCENTER;
            case GETCENTER:
                c = new Point(x, y);
                return GETVECTOR;
            case GETVECTOR:
                v = new Point(x, y);
                return GETROTATION;
            case GETROTATION:
                rotation = x;
                return DONE;
        }
        throw new UnsupportedOperationException("Foo");
    }
    
    @Override
    protected String stateDescription() {
        switch (getState()) {
            case GETFACEPOINT:
                return "Specify the next polygon corner";
            case GETCENTER:
                return "Specify the center of rotation";
            case GETVECTOR:
                return "Set center destination point";
            case GETROTATION:
                return "Specify the rotation angle in degrees";
        }
        return "";
    }

    @Override
    protected int nextAction(int state) {
        switch (state) {
            case GETFACEPOINT:
            case GETCENTER:
            case GETVECTOR:
                return ObjectCreator.GETCLICK;
            case GETROTATION:
                return ObjectCreator.GETNUMBER;
            case DONE:
                return ObjectCreator.READY;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MovingObject getObject(long starttime, long endtime) {
        MFace mf = MFace.createFMFace(face, c, v.sub(c), (rotation*Math.PI)/180);
        
//        Plot.reset("NewFMFace");
//        mf.printGraphs("sg", 2);
//        Plot.doPlot();
        
        return mf;
    }
    
}
