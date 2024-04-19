package mrview;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MReal extends MovingObject implements SecondoObject {
    List<UReal> units = new LinkedList();
    
    @Override
    public void paint(MRGraphics g, long currentTime, boolean highlight) {
        Double v = project(currentTime);
        String val = "UNDEF";
        if (v != null) {
            val = Double.toString(Math.ceil(v*100)/100);
        }
        g.drawString(getObjName()+" is: "+val);
        System.out.println(getObjName()+" is: "+val);
    }
    
    public Double project (long currentTime) {
        for (UReal u : units) {
            Double val = u.project(currentTime);
            if (val != null)
                return val;
        }
        
        return null;
    }
    
    public void addUnit (UReal u) {
        units.add(u);
    }

    @Override
    public long getStartTime() {
        Long minTime = null;
        for (UReal u : units) {
            if (minTime == null || u.getIv().getStart() < minTime) {
                minTime = u.getIv().getStart();
            }
        }

        return minTime == null ? -1 : minTime;
    }

    @Override
    public long getEndTime() {
        Long maxTime = null;
        for (UReal u : units) {
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
        return "mreal";
    }

    @Override
    public SecondoObject deserialize(NL nl) {
        MReal ret = new MReal();
        
        for (int i = 0; i < nl.size(); i++) {
            UReal u = UReal.fromNL(nl.get(i));
            ret.addUnit(u);
        }
        
        return ret;
    }

    @Override
    public NL serialize() {
        NL nl = new NL();
        
        for (UReal u : units) {
            nl.addNL(u.toNL());
        }
        
        return nl;
    }
    
}
