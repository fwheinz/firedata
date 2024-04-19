package mrview;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class UReal {
    private Interval iv;
    private double a, b, c;
    private boolean r;
    
    public UReal (Interval iv, double a, double b, double c, boolean r) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.r = r;
        this.iv = iv;
    }
    
    public static UReal fromNL(NL nl) {
        NL params = nl.get(1);
        System.out.println(params.toString());
        UReal ret = new UReal(Interval.fromNL(nl.get(0)),
                params.get(0).getNr(),
                params.get(1).getNr(),
                params.get(2).getNr(),
                params.get(3).getBool());
        return ret;
    }
    
    public Double project (long currentTime) {
        if (iv.inside(currentTime)) {
            double t = ((double)(currentTime-iv.getStart()))/86400000.0;
            double val = a*t*t+b*t+c;
            if (r)
                val = Math.sqrt(val);
						System.out.println("t: "+t+", v: "+val);
            
            return val;
        }
        
        return null;
    }
    
    public NL toNL() {
        NL nl = new NL(NL.L);
        
        nl.addNL(iv.toNL());
        NL params = nl.nest();
        params.addNr(a);
        params.addNr(b);
        params.addNr(c);
        params.addBoolean(r);
        
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
}
