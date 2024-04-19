package mrview;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class NL {
    public static final int
            UNK = 0,
            NR = 1,
            L = 2,
            STR = 3,
            SYM = 4,
            BOOL = 5;
    
    private int type;
    private Double nr;
    private List<NL> nl;
    private String str;
    private String sym;
    private Boolean bool;
    
    private NL parent;
    
    public NL() {
        this.type = NL.L;
        nl = new ArrayList();
    }
    
    public NL (int type) {
        this.type = type;
        if (type == NL.L) {
            nl = new ArrayList();
        }
    }
    
    public NL nest() {
        NL sublist = new NL(NL.L);
        sublist.setParent(this);
        getNl().add(sublist);
        
        return sublist;
    }
        
    public NL get(int index) {
        return nl.get(index);
    }
    
    public int size () {
        return nl.size();
    }
        
    public NL addBoolean (Boolean b) {
        NL n = new NL(NL.BOOL);
        n.setBool(b);
        n.setParent(this);
        nl.add(n);
        
        return n;
    }
    
    public NL addNr (Double d) {
        NL n = new NL(NL.NR);
        n.setNr(d);
        n.setParent(this);
        nl.add(n);
        
        return n;
    }
    
    public NL addNL (NL n) {
        nl.add(n);
        
        return n;
    }
    
    public NL addStr (String s) {
        NL n = new NL(NL.STR);
        n.setStr(s);
        n.setParent(this);
        nl.add(n);
        
        return n;
    }
    
    public NL addSym (String s) {
        NL n = new NL(NL.SYM);
        n.setSym(s);
        n.setParent(this);
        nl.add(n);
        
        return n;
    }

    public NL getParent() {
        return parent;
    }

    public void setParent(NL parent) {
        this.parent = parent;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Double getNr() {
        return nr;
    }

    public void setNr(Double nr) {
        this.nr = nr;
    }

    public List<NL> getNl() {
        return nl;
    }

    public void setNl(List<NL> nl) {
        this.nl = nl;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public String getSym() {
        return sym;
    }

    public void setSym(String sym) {
        this.sym = sym;
    }

    public Boolean getBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }
    
    public String toString(int indention) {
        String indent = "                                                               ".substring(0, indention);
        switch (type) {
            case NL.BOOL:
                return Boolean.TRUE.equals(bool) ? "TRUE" : "FALSE";
            case NL.L:
                StringBuilder sb = new StringBuilder();
                sb.append("\n").append(indent).append("(");
                for (NL l : nl) {
                    sb.append("")
                            .append(l.toString(indention+4))
                            .append(" ");
                }
                sb.append(")");
                return sb.toString();
            case NL.NR:
                return String.format("%.12f", nr);
            case NL.STR:
                return "\""+str+"\"";
            case NL.SYM:
                return ""+sym;
            case NL.UNK:
                return "UNKNOWN";
        }
        
        return "";
    }
    
    @Override
    public String toString() {
        return toString(0);
    }
}
