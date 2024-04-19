package mrview;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MatchFaces {

    private Point offsrc = new Point(0, 0), scalesrc = new Point(1, 1);
    private Point offdst = new Point(0, 0), scaledst = new Point(1, 1);
    private List<Face> src, dst;
    private int depth = 0;
    private BoundingBox bbsrc, bbdst;

    public static MatchFaces create(List<Face> src, List<Face> dst, int depth) {
        BoundingBox bbsrc = new BoundingBox();
        BoundingBox bbdst = new BoundingBox();

        for (Face f : src) {
            f.src = true;
        }

        for (Face f : dst) {
            f.src = false;
        }

        if (!src.isEmpty() && src.get(0).parent != null) {
            bbsrc = src.get(0).parent.getBoundingBox();
        } else {
            for (Face f : src) {
                bbsrc.update(f.getBoundingBox());
            }
        }

        if (!dst.isEmpty() && dst.get(0).parent != null) {
            bbdst = dst.get(0).parent.getBoundingBox();
        } else {
            for (Face f : dst) {
                bbdst.update(f.getBoundingBox());
            }
        }

        MatchFaces ret = new MatchFaces();
        ret.bbsrc = bbsrc;
        ret.bbdst = bbdst;
        ret.depth = depth;
        ret.src = src;
        ret.dst = dst;
        Point[] trafosrc = bbsrc.computeTransformation();
        ret.offsrc = trafosrc[0];
        ret.scalesrc = trafosrc[1];
        Point[] trafodst = bbdst.computeTransformation();
        ret.offdst = trafodst[0];
        ret.scaledst = trafodst[1];

        return ret;
    }

    private List<Face[][]> appendUnmatched(List<Face[][]> pairs) {
        Set<Face> seen = new HashSet();
        for (Face[][] pair : pairs) {
            for (Face f : pair[0]) {
                seen.add(f);
            }
            for (Face f : pair[1]) {
                seen.add(f);
            }
        }
        for (Face f : src) {
            if (!seen.contains(f)) {
                pairs.add(new Face[][]{new Face[]{f}, new Face[]{}});
            }
        }
        for (Face f : dst) {
            if (!seen.contains(f)) {
                pairs.add(new Face[][]{new Face[]{}, new Face[]{f}});
            }
        }

        return pairs;
    }

    public List<Face[][]> simplePair() {
        List<Face[][]> ret = new LinkedList();

        int iter = Math.min(src.size(), dst.size());
        for (int i = 0; i < iter; i++) {
            Face[][] fcs = new Face[2][];
            fcs[0] = new Face[]{src.get(i)};
            fcs[1] = new Face[]{dst.get(i)};
            ret.add(fcs);
        }

        appendUnmatched(ret);

        System.out.println("simplePair created " + ret.size() + " pairs!");

        return ret;
    }

    public List<Face[][]> multiPair() {
        List<Face[][]> ret = new LinkedList();

        Face[][] fcs = new Face[2][];
        fcs[0] = src.toArray(new Face[0]);
        fcs[1] = dst.toArray(new Face[0]);
        ret.add(fcs);

        return ret;
    }

    public List<Face[][]> nullPair() {
        List<Face[][]> ret = new LinkedList();

        appendUnmatched(ret);

        System.out.println("nullPair created " + ret.size() + " pairs!");

        return ret;
    }

    public List<Face[][]> areaPair(double threshold) {
        return pairByScore(new ScoreFunction() {
            @Override
            public double score(Face f1, Face f2) {
                Region r1 = new Region(f1.copy(offsrc, scalesrc));
                Region r2 = new Region(f2.copy(offdst, scaledst));

                double r1a = r1.getArea();
                double r2a = r2.getArea();
                double ri = r1.intersectArea(r2).getArea();
//                System.out.println("r1a:"+r1a+" r1b:"+r2a+" ri:"+ri);

                double score = 100.0 - (ri * 100.0 / r1a + ri * 100.0 / r2a) / 2.0;
//                System.out.println("Calculated score of "+score);
                return score;
            }
        }, threshold);
    }

    private List<Face[][]> pairByScore(ScoreFunction f, double threshold) {
        List<Face[][]> ret = new LinkedList();

        List<ScorePairing> sp = new LinkedList();
        for (Face sf : src) {
            for (Face df : dst) {
                double score = f.score(sf, df);
                if (score > threshold) {
                    continue;
                }
                sp.add(new ScorePairing(sf, df, score));
            }
        }

        Collections.sort(sp);

        Set<Face> seen = new HashSet();

        for (ScorePairing s : sp) {
            if (!seen.contains(s.sf) && !seen.contains(s.df)) {
                ret.add(new Face[][]{new Face[]{s.sf}, new Face[]{s.df}});
                seen.add(s.sf);
                seen.add(s.df);
            }
        }

        appendUnmatched(ret);

        return ret;
    }

    private interface ScoreFunction {

        double score(Face f1, Face f2);
    }

    protected class ScorePairing implements Comparable {

        public Face sf, df;
        public double score;

        public ScorePairing(Face sf, Face df, double score) {
            this.sf = sf;
            this.df = df;
            this.score = score;
        }

        @Override
        public int compareTo(Object o) {
            double sc2 = ((ScorePairing) o).score;
            if (score < sc2) {
                return -1;
            } else if (score > sc2) {
                return 1;
            }
            return 0;
        }
    }

    private List<BoundingBox> getBbList(List<Face> fcs) {
        List<BoundingBox> bbs = new LinkedList();
        for (Face f : fcs) {
            BoundingBox bb = f.getBoundingBox();
            bb.parent = f;
            bbs.add(bb);
        }

        return bbs;
    }

    public void insertSorted(List<BoundingBox> bbs, BoundingBox bb) {
        int i;

        for (i = 0; i < bbs.size(); i++) {
            BoundingBox bb2 = bbs.get(i);
            if (bb.ur.x < bb2.ur.x || (bb.ur.x == bb.ur.x && bb.ur.y < bb.ur.y)) {
                break;
            }
        }
        bbs.add(i, bb);
    }

    public List<Face[][]> overlapPair() {
        int is = 0;
        List<BoundingBox> bbs = getBbList(src);
        bbs.addAll(getBbList(dst));
        Collections.sort(bbs);

        Map<Face, Set<Face>> fm = new HashMap();
        List<BoundingBox> active = new LinkedList();
        for (BoundingBox b : bbs) {
            double current = b.ll.x;

            // Remove from active list
            Iterator<BoundingBox> it = active.iterator();
            while (it.hasNext()) {
                BoundingBox a = it.next();
                if (a.ur.x < current) {
                    it.remove();
                }
            }

            for (BoundingBox a : active) {
                Face f = (Face) a.parent;
                Face f2 = (Face) b.parent;
                if (f.src == f2.src) {
                    continue;
                }
                if (f.intersects(f2)) {
                    is++;
                    Set<Face> fcs = fm.get(f);
                    if (fcs == null) {
                        fcs = new HashSet();
                        fcs.add(f);
                        fm.put(f, fcs);
                    } else {
                    }
                    fcs.add(f2);
                }
            }

            insertSorted(active, b);
        }

        List<Face[][]> ret = new LinkedList();
        Set<Set<Face>> vals = new HashSet();
        vals.addAll(fm.values());
        for (Set<Face> fs : vals) {
            List<Face> src = new LinkedList();
            List<Face> dst = new LinkedList();
            for (Face f : fs) {
                if (f.src) {
                    src.add(f);
                } else {
                    dst.add(f);
                }
            }
            ret.add(new Face[][]{src.toArray(new Face[0]), dst.toArray(new Face[0])});
        }
        
        

        return ret;
    }
}
