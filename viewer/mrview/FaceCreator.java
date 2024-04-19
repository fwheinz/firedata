package mrview;

public class FaceCreator extends ObjectCreator<Face> {
    public static final int
            GETFACEPOINT = 0,
            DONE = 4;
    private final Face face = new Face();

    @Override
    protected void paint(MRGraphics g) {
        face.paint(g, 0, true);
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        switch (state) {
            case GETFACEPOINT:
                face.addPoint(new Point(Math.round(x),Math.round(y)));
                if (!face.isClosed())
                    return GETFACEPOINT;
                else
                    return DONE;
        }
        throw new UnsupportedOperationException("Foo");
    }
    
    @Override
    protected String stateDescription() {
        switch (getState()) {
            case GETFACEPOINT:
                return "Specify the next polygon corner";
        }
        return "";
    }

    @Override
    protected int nextAction(int state) {
        switch (state) {
            case GETFACEPOINT:
                return ObjectCreator.GETCLICK;
            case DONE:
                return ObjectCreator.READY;
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Face getObject(long starttime, long endtime) {
        face.sort();
        return face;
    }
}
