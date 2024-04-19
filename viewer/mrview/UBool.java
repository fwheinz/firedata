package mrview;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class UBool {
    private Interval iv;
    private boolean value;
    
    public UBool (Interval iv, boolean value) {
        this.value = value;
        this.iv = iv;
    }
    
    public static UBool fromNL(NL nl) {
        UBool ret = new UBool(Interval.fromNL(nl.get(0)), nl.get(1).getBool());
        
        return ret;
    }
    
    public Boolean project (long currentTime) {
        if (iv.inside(currentTime)) {
            return value;
        }
        
        return null;
    }
    
    public NL toNL() {
        NL nl = new NL();
        
        nl.addNL(iv.toNL());
        nl.addBoolean(value);
        
        return nl;
    }

    /**
     * @return the iv
     */
    public Interval getIv() {
        return iv;
    }

    /**
     * @param iv the iv to set
     */
    public void setIv(Interval iv) {
        this.iv = iv;
    }

    /**
     * @return the value
     */
    public boolean isValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(boolean value) {
        this.value = value;
    }
}
