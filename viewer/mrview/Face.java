package mrview;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import mrview.Seg.TransformParam;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class Face {

    private Point lastPoint, degeneratedCenter;
    private List<Seg> segs = new LinkedList();
    private List<Face> holes = new LinkedList();

    public Seg hullseg;
    public Point peerPoint;
    public Face parent;
    public boolean src;

    public Face(Face f) {
        for (Seg s : f.segs) {
            segs.add(new Seg(s));
        }
        if (f.hullseg != null) {
            hullseg = new Seg(f.hullseg);
        }
        if (f.peerPoint != null) {
            peerPoint = new Point(f.peerPoint);
        }
        parent = f.parent;
        for (Face h : f.holes) {
            holes.add(new Face(h));
        }
    }

    public Face() {
    }

    public void addPoint(Point pt) {
        if (getLastPoint() != null) {
            if (segs.size() >= 2 && segs.get(0).s.near(pt)) {
                close();
            } else {
                segs.add(new Seg(getLastPoint(), pt));
            }
        }
        lastPoint = pt;
    }

    public void addPointRaw(Point pt) {
        if (getLastPoint() != null) {
            segs.add(new Seg(getLastPoint(), pt));
        }
        lastPoint = pt;
    }

    public boolean removeLastPoint() {
        if (segs.isEmpty()) {
            if (lastPoint == null) {
                return false;
            }
            lastPoint = null;
            return false;
        } else {
            Seg s = segs.get(segs.size() - 1);
            lastPoint = s.s;
            segs.remove(segs.size() - 1);
        }

        return true;
    }

    public void close() {
        if (segs.size() < 2) {
            return;
        }
        Seg s = segs.get(0);
        segs.add(new Seg(getLastPoint(), s.s));
    }

    public double getArea() {
        double ret = 0;
        for (Seg seg : segs) {
            ret += (seg.s.x * seg.e.y - seg.e.x * seg.s.y);
        }
        ret /= 2;
        if (ret < 0) {
            ret = -ret;
        }

        for (Face h : holes) {
            ret -= h.getArea();
        }

        return ret;
    }

    public Point getCenter() {
        double xs = 0;
        double ys = 0;

        if (segs.size() < 3) {
            return degeneratedCenter;
        }

        for (Seg seg : segs) {
            xs += (seg.s.x + seg.e.x) * (seg.s.x * seg.e.y - seg.e.x * seg.s.y);
            ys += (seg.s.y + seg.e.y) * (seg.s.x * seg.e.y - seg.e.x * seg.s.y);
        }
        double A = getArea();
        xs /= (6 * A);
        ys /= (6 * A);

        return new Point(xs, ys);
    }

    public List<Seg> getSegments() {
        return segs;
    }
    
    private Area _area;

    public Area getAreaObj() {
        if (_area != null)
            return _area;
        Path2D.Double path = new Path2D.Double();
        if (segs.size() < 3) {
            return new Area();
        }

        path.moveTo(segs.get(0).s.x, segs.get(0).s.y);
        for (int i = 1; i < segs.size(); i++) {
            path.lineTo(segs.get(i).s.x, segs.get(i).s.y);
        }
        path.closePath();

        Area ret = new Area(path);
        for (Face h : holes) {
//            ret.subtract(h.getAreaObj());
        }

        _area = ret;
        return ret;
    }

    public Region intersectArea(Face f) {
        Area a = getAreaObj();
        a.intersect(f.getAreaObj());
        PathIterator p = a.getPathIterator(null);

        Region ret = new Region();
        Face fc = null;
        while (!p.isDone()) {
            double[] v = new double[6];
            int st = p.currentSegment(v);
            if (st == PathIterator.SEG_CLOSE) {
                fc.close();
                fc.sort2();
                ret.addFace(fc);
            } else if (st == PathIterator.SEG_MOVETO) {
                fc = new Face();
                fc.addPointRaw(new Point(v[0], v[1]));
            } else if (st == PathIterator.SEG_LINETO) {
                fc.addPointRaw(new Point(v[0], v[1]));
            }
            p.next();
        }

        return ret;
    }

    public static Face createRandom(int nrsegs, BoundingBox bb) {
        Face f = new Face();

        Seg s = new Seg();
        Point start = s.s = Point.random(bb);

        double bbsize = bb.ll.dist(bb.ur);
        int o = nrsegs;
        int attempts = 0;
        while (nrsegs > 0) {
            if (attempts++ > 1000) {
                return Face.createRandom(o, bb);
            }
            s.e = s.s.add(Point.random(bbsize / 10));
            if (s.intersects(f.segs)) {
                continue;
            }
            f.segs.add(new Seg(s));
            s.s = s.e;
            nrsegs--;
        }
        Seg fnl = new Seg(s.e, start);
        if (fnl.intersects(f.segs) || fnl.e.dist(fnl.s) > bb.ll.dist(bb.ur) / 10) {
            return Face.createRandom(o, bb);
        }
        f.segs.add(fnl);

        return f;
    }

    public static Face createRandom2(int nrsegs, BoundingBox bb) {
        Face f = new Face();

        Seg s = new Seg();
        Point start = s.s = Point.random(bb);

        double bbsize = bb.ll.dist(bb.ur);
        int o = nrsegs;
        int attempts = 0;
        while (nrsegs > 0) {
            s.e = Point.random(bb);
            if (s.intersects(f.segs)) {
                continue;
            }
            f.segs.add(new Seg(s));
            s.s = s.e;
            nrsegs--;
        }
        Seg fnl = new Seg(s.e, start);
        if (fnl.intersects(f.segs)) {
            return Face.createRandom2(o, bb);
        }
        f.segs.add(fnl);

        return f;
    }

    public boolean isClosed() {
        if (segs.size() < 3) {
            return false;
        }
        Seg first = segs.get(0);
        Seg last = segs.get(segs.size() - 1);
        return first.s.equals(last.e);
    }

    public boolean intersects(Face f) {
        for (Seg s : f.segs) {
            if (s.intersects(segs)) {
                return true;
            }
        }
        if (inside(f.segs.get(0).s)
                || f.inside(segs.get(0).s)) {
            return true;
        }

        return false;
    }

    public String nl() {
        sort();
        StringBuilder sb = new StringBuilder(" (\n  (\n");
        for (Seg s : segs) {
            sb.append(s.nl());
        }
        sb.append("  )\n");
        for (Face h : holes) {
            sb.append("  (\n");
            for (Seg s : h.getSegments()) {
                sb.append(s.nl());
            }
            sb.append("  )\n");
        }

        sb.append(" )\n");
        return sb.toString();
    }

    public NL segsToNL() {
        NL ret = new NL();
        for (Seg s : segs) {
            NL nl2 = ret.nest();
            nl2.addNr(s.s.x);
            nl2.addNr(s.s.y);
        }

        return ret;
    }

    public NL toNL() {
        NL ret = new NL();
        ret.addNL(segsToNL());

        for (Face f : holes) {
            ret.addNL(f.segsToNL());
        }

        return ret;
    }

    public static Face singleFromNL(NL nl) {
        Face f = new Face();

        try {
            for (int i = 0; i < nl.size(); i++) {
                NL n = nl.get(i);
                Point p = new Point(n.get(0).getNr(), n.get(1).getNr());
                f.addPoint(p);
            }
            f.close();
        } catch (Exception e) {
            System.out.println("Error parsing face: "+nl.toString());
            return null;
        }
        return f;
    }

    public static Face fromNL(NL nl) {
        Face f = singleFromNL(nl.get(0));
        if (f == null) {
            return null;
        }
        for (int i = 1; i < nl.size(); i++) {
            f.addHole(singleFromNL(nl.get(i)));
        }

        return f;
    }

    public String nlmf() {
        sort();
        StringBuilder sb = new StringBuilder("     ( (\n");
        for (Seg s : segs) {
            sb.append(s.nlmf());
        }
        sb.append("       )\n");
        for (Face h : holes) {
            sb.append("       (\n");
            for (Seg s : h.getSegments()) {
                sb.append(s.nlmf());
            }
            sb.append("       )\n");
        }
        sb.append("     )\n");

        return sb.toString();
    }

    public boolean intersects(List<Face> fs) {
        for (Face f : fs) {
            if (this.intersects(f)) {
                return true;
            }
        }
        return false;
    }

    public void paint(MRGraphics g, Color border, Color fill, double transp) {
        List<Point> pts = new LinkedList();
        for (Seg s : getSegments()) {
            pts.add(s.e);
        }
        g.drawPolygon(pts, border, fill, 1, transp);
    }

    public void paint(MRGraphics g, int hole, boolean active) {
        List<Seg> _segs = getSegments();
        if (_segs.isEmpty()) {
            Point pt = getLastPoint();
            if (pt != null) {
                g.drawPoint(pt);
            }
        } else {
            Seg first = _segs.get(0);
            Seg last = _segs.get(_segs.size() - 1);
            boolean open = false;
            if (!first.s.equals(last.e)) {
                open = true;
            }
//            g.drawPoint(first.s);
            List<Point> pts = new LinkedList();
            for (Seg s : getSegments()) {
                pts.add(s.e);
                if (open) {
                    g.drawLine(s, Color.RED);
                }
//                g.drawPoint(s.e);
            }
            if (!open) {
//                g.drawPolygon(pts, srcdst == 0 ? Color.MAGENTA : Color.GREEN, active?0.75:0.25);
                g.drawPolygon(pts, hole == 0 ? Color.MAGENTA : Color.GREEN, 1);
                if (active) {
                    g.drawPolygon(pts, Color.BLACK, 1);
                }
            }
        }
        for (Face h : holes) {
            h.paint(g, 1, active);
        }
    }

    public void addHole(Face f) {
        if (f != null) {
            holes.add(f);
        }
    }
    
    public boolean isccw() {
        double sum = 0;
        for (Seg seg : getSegments()) {
            double v = (seg.e.x - seg.s.x)*(seg.e.y+seg.s.y);
            sum += v;
        }
        
        return sum < 0;
    }
    
    public void reverse() {
        for (Seg s : segs)
            s.changeDirection();
        Collections.reverse(segs);
    }
    
    public void ccw() {
        if (!isccw()) {
            reverse();
        }
        assert(isccw());
    }

    public void sort2() {
        if (segs.size() < 3) {
            return;
        }
        Seg first = null;
        int firstidx = -1;
        for (int i = 0; i < segs.size(); i++) {
            Seg cur = segs.get(i);
            if (firstidx < 0
                    || (cur.s.y < first.s.y)
                    || ((cur.s.y == first.s.y) && (cur.s.x < first.s.x))) {
                firstidx = i;
                first = cur;
            }
        }

        List<Seg> sorted = new LinkedList();
        sorted.addAll(segs.subList(firstidx, segs.size()));
        sorted.addAll(segs.subList(0, firstidx));
        Seg s2 = sorted.get(segs.size() - 1);
        if (s2.reverse().getAngleXAxis() < first.getAngleXAxis()) {
            for (Seg s : sorted) {
                s.changeDirection();
            }
            Collections.reverse(sorted);
        }

        segs = sorted;
    }

    public void sort() {
        if (segs.size() < 3) {
            return;
        }
        Seg best = null;
        for (int i = 0; i < segs.size(); i++) {
            Seg cur = segs.get(i);
            if (best == null
                    || (cur.s.y < best.s.y)
                    || ((cur.s.y == best.s.y) && (cur.s.x < best.s.x))) {
                best = cur;
            }
        }

        Seg s2 = null;
        for (Seg s : segs) {
            if (s.e.equals(best.s)) {
                s2 = s;
                break;
            }
        }
        if (s2 == null) {
            return;
        }
        s2.changeDirection();
        s2.changeDirection();
        if ((s2.getAngleRad() + Math.PI) < best.getAngleRad()) {
            best = s2;
            for (Seg s : segs) {
                s.changeDirection();
            }
        }
        List<Seg> sorted = new LinkedList();
        sorted.add(best);
        Seg c;
        do {
            c = sorted.get(sorted.size() - 1);
            for (Seg s : segs) {
                if (s.s.equals(c.e)) {
                    if (!c.e.equals(best.s)) {
                        sorted.add(s);
                    }
                    break;
                }
            }
        } while (!c.e.equals(best.s));
        segs = sorted;
    }

    public Seg.TransformParam findTransformParam(Face f2, Point desiredcenter, double tolerance) {
        List<Seg> segs1 = segs;
        List<Seg> segs2 = f2.getSegments();
        if (desiredcenter == null) {
            desiredcenter = getCenter();
        }

        if (segs1.size() != segs2.size()) {
            return null;
        }
        int sz = segs1.size();
        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                Seg s1 = segs1.get(i);
                Seg s2 = segs2.get(j);
                Seg.TransformParam tp = s1.getTransformParam(s2, desiredcenter, tolerance);
                if (tp != null) {
                    boolean fail = false;
                    for (int k = 0; k < sz; k++) {
                        Seg cs1 = segs1.get((i + k) % sz);
                        Seg cs2 = segs2.get((j + k) % sz);
                        if (!cs1.transform(tp).similar(cs2, tolerance)) {
                            fail = true;
                            break;
                        }
                    }
                    if (!fail) {
                        return tp;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @return the lastPoint
     */
    public Point getLastPoint() {
        return lastPoint;
    }

    public boolean inside(Point p) {
        if (true)
            return this.getAreaObj().contains(p.x, p.y);
        int wn = 0;
        for (Seg s : this.getSegments()) {
            if (s.s.y <= p.y) {
                if (s.e.y > p.y) {
                    double sign = s.sign(p);
                    if (sign > 0) {
                        wn++;
                    } else if (sign == 0) {
                        System.out.println("On segment");
                        return false;
                    }
                }
            } else {
                if (s.e.y <= p.y) {
                    double sign = s.sign(p);
                    if (sign < 0) {
                        wn--;
                    } else if (sign == 0) {
                        System.out.println("On segment");
                        return false;
                    }
                }
            }
        }
        boolean inside = (wn != 0);
        System.out.println("Winding number "+wn);

        if (!inside) {
            return false;
        }

/*        for (Face h : getHoles()) {
            if (h.inside(p)) {
                return false;
            }
        } */

        return true;
    }

    public void translate(Point v) {
        for (Seg s : segs) {
            s.translate(v);

        }
        for (Face h : holes) {
            h.translate(v);
        }
    }

    public void transform(Point c, Point v, double a) {
        for (Seg s : segs) {
            s.transform(new TransformParam(c, v, a), true);

        }
        for (Face h : holes) {
            h.transform(c, v, a);
        }
    }

    public Face getConvexHull() {
        sort2();

        final Point[] pt = new Point[segs.size()];
        int sz = 0;
        for (Seg s : segs) {
            pt[sz++] = s.s;
        }

        Arrays.sort(pt, 1, sz, new Comparator<Point>() {
            @Override
            public int compare(Point t, Point t1) {
                if (t.sub(pt[0]).getAngleRad() < t1.sub(pt[0]).getAngleRad()) {
                    return 1;
                }
                return -1;
            }

        });

        Stack<Point> st = new Stack();
        st.push(pt[0]);
        st.push(pt[1]);
        int i = 2;
        while (i < pt.length) {
            st.peek();
            Seg s = new Seg(st.get(st.size() - 2), st.get(st.size() - 1));
            Point p = pt[i];
            if (s.sign(p) < 0) {
                st.push(p);
                i++;
            } else {
                st.pop();
            }
        }
        Face ret = new Face();
        for (Point p : st) {
            ret.addPointRaw(p);
        }
        ret.close();
        ret.sort();

        return ret;
    }

    public List<Face> getConcavities() {
        List<Face> ret = new LinkedList();

        Face cx = getConvexHull();
        cx.sort();
        sort();
        int i = 0;
        for (int j = 0; j < cx.segs.size(); j++) {
            Seg s = segs.get(i++);
            Seg cxs = cx.segs.get(j);
            if (!s.s.equals(cxs.s)) {
                return null;
            }

            if (!s.e.equals(cxs.e)) {
                Face cv = new Face();
                cv.addPointRaw(s.s);
                do {
                    System.out.println(s.e.toString());
                    cv.addPointRaw(s.e);
                    s = segs.get(i++);
                } while (!s.e.equals(cxs.e));
                cv.addPointRaw(s.e);
                cv.close();
                cv.sort();
                ret.add(cv);
            }
        }

        return ret;
    }

    /**
     * @return the holes
     */
    public List<Face> getHoles() {
        return holes;
    }

    public Face connect(Point cp, Face peer, Point pcp) {
        Face ret = new Face();
        int i = 0, j = 0;

        for (i = 0; i < segs.size(); i++) {
            Seg seg = segs.get(i);
            if (seg.s.equals(cp)) {
                break;
            }
            ret.segs.add(new Seg(seg));
        }
        if (!cp.equals(pcp)) {
            ret.segs.add(new Seg(cp, pcp, true));
        }
        int off = 0;
        for (off = 0; off < peer.segs.size(); off++) {
            Seg seg = peer.segs.get(off);
            if (seg.s.equals(pcp)) {
                break;
            }
        }
        // TODO: Optimize!
        for (j = 0; j < peer.segs.size(); j++) {
            Seg seg = peer.segs.get((off + j) % peer.segs.size());
            ret.segs.add(seg);
        }
        if (!cp.equals(pcp)) {
            ret.segs.add(new Seg(pcp, cp, true));
        }
        while (i < segs.size()) {
            Seg seg = segs.get(i++);
            ret.segs.add(new Seg(seg));
        }

        ret.sort2();

        return ret;
    }

    public Face copy(Point off, Point scale) {
        Face ret = new Face();
        for (Seg s : segs) {
            ret.segs.add(s.copy(off, scale));
        }
        for (Face h : holes) {
            ret.holes.add(h.copy(off, scale));
        }

        return ret;
    }

    public BoundingBox getBoundingBox() {
        BoundingBox bb = new BoundingBox();

        for (Seg s : segs) {
            bb.update(s);
        }

        return bb;
    }

    private int index = 0;

    public void Begin() {
        index = 0;
    }

    public Seg Cur() {
        return segs.get(index % segs.size());
    }

    public void Next() {
        index++;
    }

    public boolean End() {
        return index >= segs.size();
    }

    public MFace2 collapseExpand(Point p, boolean collapse) {
        MFace2 ret = new MFace2();
        for (Seg s : segs) {
            if (collapse) {
                ret.msegs.add(new MSeg2(s, new Seg(p, p)));
            } else {
                ret.msegs.add(new MSeg2(new Seg(p, p), s));
            }
        }

        return ret;
    }

    public MFace2 collapse(Point p) {
        return collapseExpand(p, true);
    }

    public MFace2 expand(Point p) {
        return collapseExpand(p, false);
    }

    public MFace2 move(Point p) {
        MFace2 ret = new MFace2();
        for (Seg s : segs) {
            MSeg2 ms = s.move(p);
            ret.msegs.add(ms);
        }

        for (Face h : holes) {
            MFace2 mh = h.move(p);
            ret.holes.add(mh);
        }

        return ret;
    }

    public MFace2 collapse() {
        Point p = peerPoint;
        if (p == null) {
            p = getCenter();
        }

        return collapse(p);
    }

    public MFace2 expand() {
        Point p = peerPoint;
        if (p == null) {
            p = getCenter();
        }

        return expand(p);
    }

    public MergeInfo nearest(Face f) {
        MergeInfo mi = null;
        double min = Double.NaN;

        for (Seg ms : segs) {
            for (Seg ps : f.segs) {
                Point p = ps.s;
                Point c = ms.nearestPoint(p);
                double dist = p.dist(c);
                if (Double.isNaN(min) || dist < min) {
                    mi = new MergeInfo(this, f, ms, ps, c, p);
                    min = dist;
                }

                p = ms.s;
                c = ps.nearestPoint(p);
                dist = p.dist(c);
                if (dist < min) {
                    mi = new MergeInfo(this, f, ms, ps, p, c);
                    min = dist;
                }
            }
        }

        return mi;
    }

    public MergeInfo nearest(Collection<Face> fcs) {
        MergeInfo mi = null;
        for (Face f : fcs) {
            MergeInfo m = nearest(f);
            if (mi == null || m.getDistance() < mi.getDistance()) {
                mi = m;
            }
        }

        return mi;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Seg s : segs) {
            sb.append(s.toString()).append("\n");
        }

        return sb.toString();
    }

    public static Face connect(Face[] _fcs) {
        List<Face> fcs = new LinkedList(Arrays.asList(_fcs));
        Face f = null;

        while (fcs.size() > 0) {
            if (f == null) {
                f = fcs.get(0);
                fcs.remove(f);
            } else {
                MergeInfo mi = null;
                for (Face f2 : fcs) {
                    MergeInfo mi2 = f.nearest(f2);
                    if (mi == null || mi2.getDistance() < mi.getDistance()) {
                        mi = mi2;
                    }
                }
                fcs.remove(mi.f2);
                f = mi.doMerge();
            }
        }

        return f;
    }

    public Face shrink() {
        Point v = new Point(0, 0);
        Point c1 = getCenter();

        for (Iterator<Seg> is = segs.iterator(); is.hasNext();) {
            Seg s = is.next();
            if (s.isShrinkable()) {
                v = v.sub(s.getVector());
                is.remove();
            } else {
                s.translate(v);
            }
        }
        Point c2 = getCenter();
        this.translate(c1.sub(c2));

        return this;
    }

    public MFace2 meet() {
        MFace2 mf = new MFace2();

        Point v = new Point(0, 0);

        for (Iterator<Seg> is = segs.iterator(); is.hasNext();) {
            Seg s = is.next();
            if (s.isShrinkable()) {
                v = v.sub(s.getVector());
            } else {
                mf.msegs.add(s.move(v));
            }
        }

        Point c1 = this.getCenter();
        Point c2 = mf.project(1.0).getCenter();
        mf.translateEnd(c1.sub(c2));

        return mf;
    }

//    public static Face connect2 (Face[] _fcs) {
//        List<Face> fcs = new LinkedList(Arrays.asList(_fcs));
//        List<MergeInfo> mis = new LinkedList();
//        Set<Face> used = new HashSet();
//        
//        while (fcs.size() > 0) {
//            if (used.isEmpty()) {
//                used.add(fcs.get(0));
//                fcs.remove(0);
//            } else {
//                MergeInfo mi = null;
//                for (Face f : used) {
//                    for (Face f2 : fcs) {
//                        MergeInfo mi2 = f.nearest(f2);
//                        if (mi == null || mi2.getDistance() < mi.getDistance()) {
//                            mi = mi2;
//                        }
//                    }
//                }
//                used.add(mi.f2);
//                fcs.remove(mi.f2);
//                mis.add(mi);
//            }
//        }
//        
//    }
    /**
     * @return the degeneratedCenter
     */
    public Point getDegeneratedCenter() {
        return degeneratedCenter;
    }

    /**
     * @param degeneratedCenter the degeneratedCenter to set
     */
    public void setDegeneratedCenter(Point degeneratedCenter) {
        this.degeneratedCenter = degeneratedCenter;
    }
}
