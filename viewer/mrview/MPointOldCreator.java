package mrview;

public class MPointOldCreator extends ObjectCreator<MovingObject> {
    private Point s, e;
    public static final int GETSTARTPOINT = 0, GETENDPOINT = 1, DONE = 2;
    
    @Override
    protected void paint(MRGraphics g) {
        if (s != null)
            g.drawPoint(s);
        if (e != null)
            g.drawPoint(e);
    }

    @Override
    protected int doAction(int state, double x, double y, int type) {
        switch (state) {
            case GETSTARTPOINT:
                s = new Point(x,y);
                return GETENDPOINT;
            case GETENDPOINT:
                e = new Point(x,y);
                return DONE;
        }
        
        return DONE;
    }

    @Override
    protected String stateDescription() {
        switch (getState()) {
            case GETSTARTPOINT:
                return "Specify the start point";
            case GETENDPOINT:
                return "Specify the end point";
        }
        
        return "";
    }

    @Override
    protected int nextAction(int state) {
        switch (getState()) {
            case GETSTARTPOINT:
                return ObjectCreator.GETCLICK;
            case GETENDPOINT:
                return ObjectCreator.GETCLICK;
            case DONE:
                return ObjectCreator.READY;
        }
        
        return 0;
    }

    @Override
    public MovingObject getObject(long starttime, long endtime) {
        return new MPointOld(s, e.sub(s), starttime, endtime);
    }
}
