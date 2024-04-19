package mrview;

import javax.swing.JOptionPane;

public abstract class ObjectCreator<T> {
    private int state;
    public static final int LEFTCLICK = 0, RIGHTCLICK = 1;
    public static final int GETCLICK = 0, GETNUMBER = 1, READY = 2, GETYESNO = 3;

    abstract protected void paint (MRGraphics g);
    abstract protected int doAction(int state, double v1, double v2, int type);

    public final void click(double x, double y, int type) {
        state = doAction(state, x, y, type);
        checkAction();
    }
    
    public void checkAction() {
        while (true) {
            MRViewWindow.m.modelChanged();
            switch (nextAction()) {
                case GETCLICK:
                    return;
                case GETNUMBER:
                    getNumber();
                    break;
                case GETYESNO:
                    getYesNo();
                    break;
                case READY:
                    return;
            }
        }
    }

    abstract protected String stateDescription();
    abstract protected int nextAction(int state);

    protected int getState() {
        return state;
    }

    protected void getNumber() {
        while (true) {
            String input = JOptionPane.showInputDialog(stateDescription());
            try {
                int val = Integer.parseInt(input);
                state = doAction(state, val, val, 0);
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void getYesNo() {
        int yesno = JOptionPane.showConfirmDialog(null, stateDescription());
        state = doAction(state, yesno, yesno, 0);
    }
    
    public final int nextAction() {
        return nextAction(state);
    }
    
    abstract public T getObject(long starttime, long endtime);
}
