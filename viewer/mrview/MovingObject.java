package mrview;

public abstract class MovingObject {
    private boolean visible = true, highlighted, red;
    private String name;
    
    public abstract void paint (MRGraphics g, long currentTime, boolean highlight);
    public abstract long getStartTime();
    public abstract long getEndTime();
    
    public abstract BoundingBox getBoundingBox();

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }
    
    public String[] getOperations () {
        return new String[0];
    }
    
    public String[] getFlags () {
        return new String[0];
    }
    
    public void invokeOperation (String name) {
        try {
            Object o = getClass().getMethod(name).invoke(this);
            if (o != null && o instanceof MovingObject) {
                MRViewWindow.m.addMFace((MovingObject)o);
            }
            MRViewWindow.m.modelChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean getFlag (String name) {
        try {
            return (boolean) getClass().getMethod("is"+name).invoke(this);
        } catch (Exception e) {
        }
        
        return false;
    }
    
    public void setFlag (String name, boolean val) {
        try {
            getClass().getMethod("set"+name, new Class[] {boolean.class}).invoke(this, val);
            MRViewWindow.m.modelChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getObjName() {
        return name == null ? this.getClass().getSimpleName() : name;
    }

    public void setObjName(String name) {
        this.name = name;
    }

    public boolean isRed() {
        return red;
    }

    public void setRed(boolean red) {
        this.red = red;
    }
}
