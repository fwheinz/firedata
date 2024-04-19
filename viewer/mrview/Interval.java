package mrview;

public class Interval {

    private long start, end;
    private boolean leftClosed, rightClosed;

    public Interval() {        
    }

    public Interval(long start, long end, boolean leftClosed, boolean rightClosed) {
        this.start = start;
        this.end = end;
        this.leftClosed = leftClosed;
        this.rightClosed = rightClosed;
    }

    public Interval(long start, long end) {
        this.start = start;
        this.end = end;
        this.leftClosed = this.rightClosed = true;
    }

    public Interval intersect (Interval iv) {
        Interval ret = new Interval();
        ret.setStart(Math.max(iv.getStart(), getStart()));
        ret.setEnd(Math.min(iv.getEnd(), getEnd()));
        ret.setLeftClosed(iv.leftClosed && leftClosed);
        ret.setRightClosed(iv.rightClosed && rightClosed);
        if ((ret.start > ret.end) || (ret.start == ret.end && (!ret.leftClosed || !ret.rightClosed)))
            return null;
        
        return ret;
    }
    
    public double project(long currentTime) {
        return ((double) (currentTime - getStart())) / ((double) (getEnd() - getStart()));
    }

    public long project(double t) {
        return Math.round(getEnd() * t + getStart() * (1 - t));
    }
    
    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public NL toNL() {
        NL interval = new NL();
        interval.addStr(Parser.getDate(start));
        interval.addStr(Parser.getDate(end));
        interval.addBoolean(isLeftClosed());
        interval.addBoolean(isRightClosed());
            
        return interval;

    }
    
    public static Interval fromNL(NL nl) {
        Interval iv = new Interval();
        
        iv.setStart(Parser.parseDate(nl.get(0)));
        iv.setEnd(Parser.parseDate(nl.get(1)));
        iv.setLeftClosed(nl.get(2).getBool());
        iv.setRightClosed(nl.get(3).getBool());
        
        return iv;
    }

    /**
     * @return the leftClosed
     */
    public boolean isLeftClosed() {
        return leftClosed;
    }

    /**
     * @param leftClosed the leftClosed to set
     */
    public void setLeftClosed(boolean leftClosed) {
        this.leftClosed = leftClosed;
    }

    /**
     * @return the rightClosed
     */
    public boolean isRightClosed() {
        return rightClosed;
    }

    /**
     * @param rightClosed the rightClosed to set
     */
    public void setRightClosed(boolean rightClosed) {
        this.rightClosed = rightClosed;
    }
    
    public boolean inside (long time) {
        return ((time > start) && (time < end) ||
                (time == start && leftClosed) ||
                (time == end && rightClosed)); 
    }
    
    public double getFrac (long time) {
	if (end == start)
	    return 1;
        return ((double)(time-start))/(end-start);
    }

}
