package mrview;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MBool extends MovingObject implements SecondoObject {
    List<UBool> units = new LinkedList();
    
    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        Boolean b = project(currentTime);
        String val;
        if (Objects.equals(b, Boolean.TRUE)) {
            val = "TRUE  (inside)";
        } else if (Objects.equals(b, Boolean.FALSE)) {
            val = "FALSE (outside)";
        } else {
            val = "UNDEF";
        }
        g.drawString(getObjName()+" is: "+val);
        System.out.println(getObjName()+" is: "+val);
    }
    
    public Boolean project (long currentTime) {
        for (UBool u : units) {
            Boolean b = u.project(currentTime);
            if (b != null)
                return b;
        }
        
        return null;
    }
    
    public void addUnit (UBool u) {
        units.add(u);
    }

    @Override
    public long getStartTime() {
        Long minTime = null;
        for (UBool u : units) {
            if (minTime == null || u.getIv().getStart() < minTime) {
                minTime = u.getIv().getStart();
            }
        }

        return minTime == null ? -1 : minTime;
    }

    @Override
    public long getEndTime() {
        Long maxTime = null;
        for (UBool u : units) {
            if (maxTime == null || u.getIv().getEnd() > maxTime) {
                maxTime = u.getIv().getEnd();
            }
        }

        return maxTime == null ? -1 : maxTime;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public String getSecondoType() {
        return "mbool";
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        MBool ret = new MBool();
        
        for (int i = 0; i < nl.size(); i++) {
            UBool u = UBool.fromNL(nl.get(i));
            ret.addUnit(u);
        }
        
        return ret;
    }

    @Override
    public NL serialize() {
        NL nl = new NL();
        
        for (UBool u : units) {
            nl.addNL(u.toNL());
        }
        
        return nl;
    }
    
}
