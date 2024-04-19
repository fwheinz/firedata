/* 
 * This file is part of libpmregion
 * 
 * File:   PMRegion\_conversions.cpp
 * Author: Florian Heinz <fh@sysv.de>

 1 Conversions
   Conversions from PMRegion to other formats
   like MRegion and OFF and vice versa
 
*/


#include "PMRegion_internal.h"
#include <sstream>
#include <iomanip>
#include <map>
#include <ctime>

using namespace pmreg;

namespace pmreg {

/*
             C O N V E R S I O N S

*/


/*
   Conversion functions from and to OFF file format.
   This is directly handled by CGAL

*/

PMRegion PMRegion::fromOFF(std::string off) {
    PMRegion ret;
    std::stringstream ss;

    ss << off;
    ss >> ret.polyhedron;

    return ret;
}   

std::string PMRegion::toOFF() {
    std::stringstream ss;

    ss << setprecision(PRECISION) << polyhedron;

    return ss.str();
}





/*
  Conversion functions from and to mregions.
  The mregion must be in RList format.

*/

// build_from_mregion builds a polyhedron from a list of surfaces
template <class HDS> class build_from_mregion:public CGAL::Modifier_base<HDS> {
    public:
        RList& mc;
        Kernel::FT starttime, endtime;

        build_from_mregion(RList& _reg, Kernel::FT _st, Kernel::FT _et) :
            mc(_reg), starttime(_st), endtime(_et) {}

        void operator() (HDS& hds) {
            int _idx = 0;
            CGAL::Polyhedron_incremental_builder_3<HDS> pb(hds, true);

            vector<Point3d> start, end;
            pb.begin_surface(10, 10, 10, Polyhedron_builder::ABSOLUTE_INDEXING);

            map<Point3d,int> pm;

            for (unsigned int l = 0; l < mc.items.size(); l++) {
                RList& mseg = mc.items[l];

                Point3d p(mseg.items[0].getFt(), mseg.items[1].getFt(),
                        starttime);
                Point3d q(mseg.items[2].getFt(), mseg.items[3].getFt(),
                        endtime);

                if (pm.count(p) == 0) {
                    pm[p] = _idx++;
                    pb.add_vertex(p);
                }

                if (pm.count(q) == 0) {
                    pm[q] = _idx++;
                    pb.add_vertex(q);
                }
            }

            for (unsigned int l = 0; l < mc.items.size(); l++) {
                RList& mseg = mc.items[l];
                RList& prev = ((l == 0)?mc.items[mc.items.size()-1] : 
                        mc.items[l-1]);
                Point3d p(mseg.items[0].getFt(), mseg.items[1].getFt(),
                        starttime);
                Point3d q(mseg.items[2].getFt(), mseg.items[3].getFt(),
                        endtime);
                Point3d prevp(prev.items[0].getFt(), prev.items[1].getFt(),
                        starttime);
                Point3d prevq(prev.items[2].getFt(), prev.items[3].getFt(),
                        endtime);

                if (!(prevp == p)) {
                    start.push_back(p);
                }
                if (!(prevq == q)) {
                    end.push_back(q);
                }


                pb.begin_facet();
                if (prevp == p) {
                    pb.add_vertex_to_facet(pm[p]);
                    pb.add_vertex_to_facet(pm[q]);
                    pb.add_vertex_to_facet(pm[prevq]);
                } else if (prevq == q) {
                    pb.add_vertex_to_facet(pm[p]);
                    pb.add_vertex_to_facet(pm[q]);
                    pb.add_vertex_to_facet(pm[prevp]);
                } else {
                    pb.add_vertex_to_facet(pm[p]);
                    pb.add_vertex_to_facet(pm[q]);
                    pb.add_vertex_to_facet(pm[prevq]);
                    pb.add_vertex_to_facet(pm[prevp]);
                }
                pb.end_facet();
            }
            if (start.size() >= 3) {
                pb.begin_facet();
                for (int i = start.size()-1; i >= 0; i--) {
                    pb.add_vertex_to_facet(pm[start[i]]);
                }
                pb.end_facet();
            }
            if (end.size() >= 3) {
                pb.begin_facet();
                for (unsigned int i = 0; i < end.size(); i++) {
                    pb.add_vertex_to_facet(pm[end[i]]);
                }
                pb.end_facet();
            }
            pb.end_surface();
        }
};

PMRegion PMRegion::fromMRegion (RList reg) {
    Nef_polyhedron np;

    double off = parsetime(reg.items[4].items[0].items[0].items[0].getString());
    off = 0;

    RList& urs = reg.items[4];

    for (unsigned int i = 0; i < urs.items.size(); i++) {
        RList& ur = urs.items[i];
        RList& iv = ur.items[0];


        Kernel::FT starttime = parsetime(iv.items[0].getString()) - off;
        Kernel::FT endtime = parsetime(iv.items[1].getString()) - off;


        RList& fcs = ur.items[1];
        for (unsigned int j = 0; j < fcs.items.size(); j++) {
            RList& fc = fcs.items[j];
            for (unsigned int k = 0; k < fc.items.size(); k++) {
                RList& mc = fc.items[k];
                build_from_mregion<Polyhedron::HalfedgeDS> bm(mc, starttime,
                                     endtime);
                Polyhedron p;

                p.delegate(bm);
                CGAL::Polygon_mesh_processing::triangulate_faces(p);

                Nef_polyhedron nnp(p);
                if (k == 0)
                    np = np + nnp;
                else
                    np = np - nnp;
            }
        }
    }

    PMRegion pmreg(nef2polyhedron(np));

    return pmreg;
}


class MyPoint {
	public:
	double _x, _y, _z;

	MyPoint () : _x(0), _y(0), _z(0) {}
	MyPoint (double x, double y, double z) : _x(x), _y(y), _z(z) {}

	MyPoint (Point3d p3) {
		_x = CGAL::to_double(p3.x());
		_y = CGAL::to_double(p3.y());
		_z = CGAL::to_double(p3.z());
	}

	double x() { return _x; }
	double y() { return _y; }
	double z() { return _z; }

	bool operator< (const MyPoint p) const {
		return  ((_x < p._x) || 
			 ((_x == p._x) && 
			  ((_y < p._y) || (_y == p._y && _z < p._z))));
	}

	bool operator== (const MyPoint p) const {
		return _x == p._x && _y == p._y && _z == p._z;
	}
};

/* Convert a PMRegion to a classical (unit-based) MRegion. */
//typedef MyPoint Point;
//typedef double MyNum;
typedef Point3d Point;
typedef Kernel::FT MyNum;


class MySeg {
	public:
		Point s;
		Point e;

		MySeg() {
			s = Point(0,0,0);
			e = Point(0,0,0);
		}

		MySeg(Point s, Point e) : s(s), e(e) {}

		bool operator< (const MySeg m) const {
			return ((s < m.s) || ((s == m.s) && (e < m.e)));
		}

		bool operator== (const MySeg m) const {
			return ((s == m.s) && (e == m.e));
		}

};

ostream& operator<<(ostream& os, const MyPoint& p) {
	os << p._x << " "  << p._y << " " << p._z;
	return os;
}

ostream& operator<<(ostream& os, const MySeg& ms) {
	os << ms.s << " / " << ms.e;
	return os;
}

/* MovSeg represents a moving segment */
class MovSeg {
    public:
	MySeg i, f;
        bool valid;
        MovSeg(Point is, Point ie, Point fs, Point fe) : valid(true) {
	    i = MySeg(is, ie);
	    f = MySeg(fs, fe);
	}
        MovSeg() : valid(false) {}

        bool isnext(MovSeg* ms) {
            return ms->i.s == i.e && ms->f.s == f.e;
        }
        bool isnext(MovSeg ms) {
            return ms.i.s == i.e && ms.f.s == f.e;
        }
        bool isreversenext (MovSeg* ms) {
            return ms->i.e == i.e && ms->f.e == f.e;
        }
        bool isreversenext (MovSeg ms) {
            return ms.i.e == i.e && ms.f.e == f.e;
        }
        void reverse () {
            Point tmp;
            tmp = i.s;
            i.s = i.e;
            i.e = tmp;
            tmp = f.s;
            f.s = f.e;
            f.e = tmp;
        }

	bool isdegenerated () {
		return i.s == i.e && f.s == f.e;
	}

	static bool isnear (Point a, Point b, MyNum delta) {
		MyNum d1 = a.x() - b.x(),
		       d2 = a.y() - b.y(),
		       d3 = a.z() - b.z();
		if (d1 < 0)
			d1 = -d1;
		if (d2 < 0)
			d2 = -d2;
		if (d3 < 0)
			d3 = -d3;
		return d1 < delta && d2 < delta && d3 < delta;
	}

	bool isdegenerated (double delta) {
		return  isnear(i.s, i.e, delta) &&
			isnear(f.s, f.e, delta);
	}

	bool operator< (const MovSeg ms) const {
		return (i < ms.i) || ((i == ms.i) && f < ms.f);
	}

	bool operator== (const MovSeg ms) const {
		return i == ms.i && f == ms.f;
	}
            
        std::vector<MovSeg *>::iterator findNext (std::vector<MovSeg *>& vec) {
            for (std::vector<MovSeg *>::iterator it = vec.begin();
                        it != vec.end(); it++) {
                if (isnext(*it))
                    return it;
                if (isreversenext(*it)) {
                    (*it)->reverse();
                    return it;
                }
            }
            return vec.end();
        }

        std::vector<MovSeg>::iterator findNext (std::vector<MovSeg>& vec) {
            for (std::vector<MovSeg>::iterator it = vec.begin();
                        it != vec.end(); it++) {
                if (isnext(*it))
                    return it;
                if (isreversenext(*it)) {
                    it->reverse();
                    return it;
                }
            }
            return vec.end();
        }

        string ToString() const {
            std::stringstream ss;
            ss << i.s << " / " << i.e << "  ---  " << f.s << " / " << f.e << endl;
            return ss.str();
        }

};


class MovSegs {
	public:
		set<MovSeg *> segs;
		map<MySeg,vector<MovSeg *> > segsfwd, segsrev;

		void append (MovSeg *ms) {
			segs.insert(ms);
			MySeg fwd(ms->i.s, ms->f.s);
			MySeg rev(ms->i.e, ms->f.e);

			segsfwd[fwd].push_back(ms);
			segsrev[rev].push_back(ms);
		}

		MovSeg* findNext (MovSeg* ms) {
			MySeg p(ms->i.e, ms->f.e);

			map<MySeg,vector<MovSeg *> >::iterator it;
			it = segsfwd.find(p);
			if (it != segsfwd.end()) {
				for (vector<MovSeg *>::iterator mi = it->second.begin(); mi != it->second.end(); mi++) {
					set<MovSeg *>::iterator it2 = segs.find(*mi);
					if (it2 != segs.end()) {
						MovSeg *ret = *it2;
						segs.erase(it2);
						return ret;
					}
				}
			}
			
			it = segsrev.find(p);
			if (it != segsrev.end()) {
				for (vector<MovSeg *>::iterator mi = it->second.begin(); mi != it->second.end(); mi++) {
					set<MovSeg *>::iterator it2 = segs.find(*mi);
					if (it2 != segs.end()) {
						MovSeg *ret = *it2;
						ret->reverse();
						segs.erase(it2);
						return ret;
					}
				}
			}
			cerr << "Did not find followup" << endl;
			for (auto xx = segsfwd.begin(); xx != segsfwd.end(); xx++) {
				if (!(p == xx->first)) {
					cerr << p << " != " << xx->first << endl;
				} else {
					cerr << p << " == " << xx->first << endl;
				}
			}
			for (set<MovSeg *>::iterator it = segs.begin(); it != segs.end(); it++) {
				cerr << (*it)->ToString() << endl;
			}
			assert(false);
            return NULL;
		}

		MovSeg* begin() {
			set<MovSeg *>::iterator it = segs.begin();
			MovSeg* ret = *it;
			segs.erase(it);
			return ret;
		}

		bool empty() {
			return segs.empty();
		}

		int size() {
			return segs.size();
		}
};

ostream& operator<<(ostream& os, const MovSegs& ms) {
	for (set<MovSeg *>::iterator it = ms.segs.begin(); it != ms.segs.end(); it++) {
		os << (*it)->ToString() << endl;
	}
	return os;
}


static std::vector<std::vector<MovSeg> >sortmcycle(std::vector<MovSeg> movsegs);
static std::vector<std::vector<MovSeg *> >sortmcycle2(MovSegs movsegs);

typedef pair<Point, Point> Seg;

class Triangle {
	public:
	Point a, b, c;

	Triangle (Point p1, Point p2, Point p3) {
		if (p1.z() < p2.z() && p1.z() < p3.z()) {
			a = p1;
			if (p2.z() < p3.z()) {
				b = p2;
				c = p3;
			} else {
				b = p3;
				c = p2;
			}
		} else if (p2.z() < p3.z()) {
			a = p2;
			if (p1.z() < p3.z()) {
				b = p1;
				c = p3;
			} else {
				b = p3;
				c = p1;
			}
		} else {
			a = p3;
			if (p1.z() < p2.z()) {
				b = p1;
				c = p2;
			} else {
				b = p2;
				c = p1;
			}
		}
	}

	int between (MyNum z) {
		return (z >= a.z() && z <= c.z());
	}

	Seg project (MyNum z) {
		MyNum p1x, p1y, p1z, p2x, p2y, p2z;
		Point p1, p2;

		assert (z >= a.z() && z <= c.z());
		if (z == a.z()) {
			p1 = a;
			if (z == b.z()) {
				p2 = b;
			} else {
				p2 = a;
			}
		} else if (z == c.z()) {
			p1 = c;
			if (z == b.z()) {
				p2 = b;
			} else {
				p2 = c;
			}
		} else {
			MyNum frac1 = (z - a.z()) / (c.z() - a.z());
			p1x = a.x() + (c.x() - a.x())*frac1;
			p1y = a.y() + (c.y() - a.y())*frac1;
			p1z = z;

			Point t1, t2;
			if (z < b.z()) {
				t1 = a;
				t2 = b;
			} else {
				t1 = b;
				t2 = c;
			}
			MyNum frac2 = (z - t1.z())/(t2.z() - t1.z());
			p2x = t1.x() + (t2.x() - t1.x())*frac2;
			p2y = t1.y() + (t2.y() - t1.y())*frac2;
			p2z = z;
			p1 = Point(p1x, p1y, p1z);
			p2 = Point(p2x, p2y, p2z);
		}

		return pair<Point, Point>(p1, p2);
	}
	
	int sign (MyNum n) {
		if (n > 0)
			return 1;
		else if (n == 0)
			return 0;
		else // if (n < 0)
			return -1;
	}

	MovSeg *project (MyNum z1, MyNum z2) {
		Seg si = project(z1);
		Seg sf = project(z2);

		MyNum dx1 = si.first.x() - si.second.x();
		MyNum dy1 = si.first.y() - si.second.y();
		MyNum dx2 = sf.first.x() - sf.second.x();
		MyNum dy2 = sf.first.y() - sf.second.y();

		if (sign(dx1) != sign(dx2) || sign(dy1) != sign(dy2)) {
			Point tmp = sf.first;
			sf.first = sf.second;
			sf.second = tmp;
		}

		MovSeg *ms = new MovSeg(si.first, si.second, sf.first, sf.second);

		return ms;
	}

};

RList PMRegion::toMRegion2(int raw) {
	map<MyNum, vector<Triangle *> > tri;
	RList uregs;

	set<MyNum> zevents;
	int facets = 0;
	CGAL::Polygon_mesh_processing::triangulate_faces(polyhedron);
	for (Facet_iterator f = polyhedron.facets_begin();
			f != polyhedron.facets_end(); f++) {
		Halfedge_facet_circulator hc = f->facet_begin();
		Point p1 = hc++->vertex()->point();
		Point p2 = hc++->vertex()->point();
		Point p3 = hc++->vertex()->point();
		assert(hc == f->facet_begin());
		if (p1.z() == p2.z() && p1.z() == p3.z())
			continue; // All points are coplanar to a plane parallel to the xy plane
		Triangle *t = new Triangle(p1, p2, p3);
		tri[t->a.z()].push_back(t);
		zevents.insert(t->a.z());
		zevents.insert(t->b.z());
		zevents.insert(t->c.z());
		facets++;
	}
	//cerr << "Processing " << zevents.size() << " events" << endl;

	vector<Triangle *> curset;
	MyNum prevz = 0;
	int nrevents = 0;
	for (set<MyNum>::iterator it = zevents.begin(); it != zevents.end(); ++it) {
		MyNum z = *it;
		//cerr << std::setprecision(30) << z << endl;
		vector<Triangle *> triangles = tri[z];
		curset.insert(curset.end(), triangles.begin(), triangles.end());
		if (nrevents++ == 0) {
			prevz = z;
			continue;
		}

		for (vector<Triangle *>::iterator ti = curset.begin(); ti != curset.end(); ) {
			if ((*ti)->c.z() < prevz) {
				ti = curset.erase(ti);
			} else {
				ti++;
			}
		}

		//cerr << nrevents << "/" << zevents.size() << ": Scanning from " << prevz << " to " << z << " (" << curset.size() << " triangles)" << endl;

		MovSegs msegs;
		for (vector<Triangle *>::iterator ti = curset.begin(); ti != curset.end(); ti++) {
			if ((*ti)->between(prevz) && (*ti)->between(z))
				msegs.append((*ti)->project(prevz, z));
		}
		//cerr << nrevents << "/" << zevents.size() << ": Projection done, sorting cycles" << endl;

		std::vector<std::vector<MovSeg *> > cycles = sortmcycle2(msegs);
		//cerr << nrevents << "/" << zevents.size() << ": Sorting done, building ureg" << endl;
		RList ureg, mfaces, iv;
		iv.append(timestr(prevz));
		iv.append(timestr(z));
		iv.append(true);
		iv.append(false);
		ureg.append(iv);
		for (unsigned int i = 0; i < cycles.size(); i++) {
			RList mfacewithholes;
			RList mface;
			std::vector<MovSeg *>& cyc = cycles[i];
			for (unsigned int j = 0;  j < cyc.size(); j++) {
				MovSeg *ms = cyc[j];
				if (ms->isdegenerated())
					continue;
				RList msrl;
				msrl.append(ms->i.s.x());
				msrl.append(ms->i.s.y());
				msrl.append(ms->f.s.x());
				msrl.append(ms->f.s.y());
				mface.append(msrl);
			}
			mfacewithholes.append(mface);
			mfaces.append(mfacewithholes);
		}
		ureg.append(mfaces);
		if (mfaces.items.size() > 0)
			uregs.append(ureg);

		prevz = z;
	}

	return raw ? uregs : uregs.obj("mregion", "mregion");
}

RList PMRegion::toMRegion2() {
	return toMRegion2(false);
}

#define DELTA 0.0000001

static MovSeg createMSegFromFacet (SHalfedge_const_handle h,
                          Kernel::FT min, Kernel::FT max);
RList PMRegion::toMRegion () {
    bool first = true;
    Kernel::FT prev;
    Nef_polyhedron np(polyhedron);
    set<Kernel::FT> zevents;

    for (Vertex_const_iterator v = np.vertices_begin();
                         v != np.vertices_end(); ++v) {
        zevents.insert(v->point().z());
    }

    cerr << "Events: " << zevents.size() << endl;

    RList uregs;
    for (std::set<Kernel::FT>::iterator it = zevents.begin();
                        it != zevents.end(); it++) {
        if (!first) {
            RList ureg;

            RList iv;

            iv.append(timestr(prev));
            iv.append(timestr(*it));
            iv.append(true);
            iv.append(std::distance(it, zevents.end()) == 1);
            ureg.append(iv);

            Nef_polyhedron r1 = np
                .intersection(Plane(Point3d(0, 0, prev+DELTA), Vector(0, 0, -1)),
                               Nef_polyhedron::CLOSED_HALFSPACE);
            Nef_polyhedron result = r1
                .intersection(Plane(Point3d(0, 0,  *it-DELTA), Vector(0, 0,  1)),
                                Nef_polyhedron::CLOSED_HALFSPACE);

            vector<MovSeg> cycle;
            for (Halffacet_const_iterator f = result.halffacets_begin();
                                f != result.halffacets_end(); f++) {
                if (f->is_twin()) continue;
                for (Halffacet_cycle_const_iterator fc =f->facet_cycles_begin();
                     fc != f->facet_cycles_end(); fc++) {
                    if (fc.is_shalfedge()) {
                        MovSeg ms = createMSegFromFacet(fc, prev, *it);
                        if (ms.valid)
                            cycle.push_back(ms);
                    }
                }
            }
            std::vector<std::vector<MovSeg> > cycles = sortmcycle(cycle);
            RList mfaces;
            for (unsigned int i = 0; i < cycles.size(); i++) {
                RList mfacewithholes;
                RList mface;
                std::vector<MovSeg>& cyc = cycles[i];
                for (unsigned int j = 0;  j < cyc.size(); j++) {
                    MovSeg& ms = cyc[j];
                    RList msrl;
                    msrl.append(ms.i.s.x());
                    msrl.append(ms.i.s.y());
                    msrl.append(ms.f.s.x());
                    msrl.append(ms.f.s.y());
                    mface.append(msrl);
                }
                mfacewithholes.append(mface);
                mfaces.append(mfacewithholes);
            }
            ureg.append(mfaces);
            if (mfaces.items.size() > 0)
                uregs.append(ureg);
        }
        first = false;
        prev = *it;
    }

    return uregs.obj("mregion", "mregion");
}

/* Creates a moving segment from a polyhedron surface */
static MovSeg createMSegFromFacet (SHalfedge_const_handle h, Kernel::FT min,
                                                     Kernel::FT max) {
    SHalfedge_around_facet_const_circulator hc(h), he(hc);
    Kernel::FT prevz;
    bool found = false;
    for (int i = 0, dist = circulator_distance(hc, he); i < dist+1; i++) {
        Kernel::FT z = hc->source()->source()->point().z();
        if (i > 0 && prevz == max && z == min) {
            found = true;
            break;
        }
        hc++;
        prevz = z;
    }
    if (!found)
        return MovSeg();
    he = hc;
    Point3d is = hc->source()->source()->point(), ie;
    do {
        ie = hc->source()->source()->point();
        hc++;
    } while (hc->source()->source()->point().z() == min);
    Point3d fe = hc->source()->source()->point(), fs;
    do {
        fs = hc->source()->source()->point();
        hc++;
    } while (hc->source()->source()->point().z() == max);

    return MovSeg(is, ie, fs, fe);
}

/* Sorts the moving segments to cycles */
static std::vector<std::vector<MovSeg> > sortmcycle
                                           (std::vector<MovSeg> movsegs) {
    std::vector<std::vector<MovSeg> > ret;
    
    while (!movsegs.empty()) {
        std::vector<MovSeg> cycle;
        MovSeg first = *(movsegs.begin());
        cycle.push_back(first);
        MovSeg cur = first;
        movsegs.erase(movsegs.begin());
        Polygon_2 pi, pf;
        Point_2 piprev(0, 0), pifinal(0, 0);
        Point_2 pfprev(0, 0), pffinal(0, 0);
        do {
            std::vector<MovSeg>::iterator it = cur.findNext(movsegs);
            if (it == movsegs.end()) {
		    cerr << "Warning: Failed to find next segment" << endl;
		    break; //  XXX
	    }
            assert(it != movsegs.end());
            cur = *it;
            cycle.push_back(cur);
            movsegs.erase(it);
            Point_2 pip(cur.i.s.x(), cur.i.s.y());
            if ((pip != piprev) && (pip != pifinal)) {
                pi.push_back(pip);
                if (pi.size() == 1)
                    pifinal = pip;
            }
            piprev = pip;
            Point_2 pfp(cur.f.s.x(), cur.f.s.y());
            if ((pfp != pfprev) && (pfp != pffinal)) {
                pf.push_back(pfp);
                if (pf.size() == 1)
                    pffinal = pfp;
            }
            pfprev = pfp;

            if (cur.isnext(first))
                break;
        } while (1);
        if ((pi.is_simple() && pi.orientation() == CGAL::CLOCKWISE) ||
        (pf.is_simple() && pf.orientation() == CGAL::CLOCKWISE)) {
            std::reverse(cycle.begin(), cycle.end());
            for (unsigned int i = 0; i < cycle.size(); i++)
                cycle[i].reverse();
        }
        ret.push_back(cycle);
    }

    return ret;
}

/* Sorts the moving segments to cycles */
static std::vector<std::vector<MovSeg *> > sortmcycle2
                                           (MovSegs movsegs) {
    std::vector<std::vector<MovSeg *> > ret;
    
    while (!movsegs.empty()) {
        std::vector<MovSeg *> cycle;
        MovSeg *first = movsegs.begin();
        cycle.push_back(first);
        MovSeg *cur = first;
        Polygon_2 pi, pf;
        Point_2 piprev(0, 0), pifinal(0, 0);
        Point_2 pfprev(0, 0), pffinal(0, 0);
        do {
            cur = movsegs.findNext(cur);
            cycle.push_back(cur);
            Point_2 pip(cur->i.s.x(), cur->i.s.y());
            if ((pip != piprev) && (pip != pifinal)) {
                pi.push_back(pip);
                if (pi.size() == 1)
                    pifinal = pip;
            }
            piprev = pip;
            Point_2 pfp(cur->f.s.x(), cur->f.s.y());
            if ((pfp != pfprev) && (pfp != pffinal)) {
                pf.push_back(pfp);
                if (pf.size() == 1)
                    pffinal = pfp;
            }
            pfprev = pfp;

            if (cur->isnext(first))
                break;
        } while (1);
        if ((pi.is_simple() && pi.orientation() == CGAL::CLOCKWISE) ||
        (pf.is_simple() && pf.orientation() == CGAL::CLOCKWISE)) {
            std::reverse(cycle.begin(), cycle.end());
            for (unsigned int i = 0; i < cycle.size(); i++)
                cycle[i]->reverse();
        }
        ret.push_back(cycle);
    }

    return ret;
}



/*
  Conversion function from and to native pmregion rlist
  format

*/

PMRegion PMRegion::fromRList (RList rl) {
    RList& obj = rl.items[4];
    RList& points = obj.items[0];
    RList& facets = obj.items[1];
    std::stringstream off;

    off << "OFF" << endl;
    off << points.items.size() << " " << facets.items.size() << " 0" << endl;
    for (unsigned int i = 0; i < points.items.size(); i++) {
        RList& point = points.items[i];
        off << setprecision(100) << point.items[0].getFt() << " " <<
        point.items[1].getFt() << " " << point.items[2].getFt() << endl;
    }

    for (unsigned int i = 0; i < facets.items.size(); i++) {
        RList& facet = facets.items[i];
        off << facet.items.size();
        for (unsigned int j = 0; j < facet.items.size(); j++) {
            off << " " << ((int) facet.items[j].getNr());
        }
        off << endl;
    }
    
    return PMRegion::fromOFF(off.str());
}

RList PMRegion::toRList (bool raw) {
    RList pmreg;

    std::map<Point3d, int> pmap;
    int idx = 0;
    RList points;
    for (Vertex_iterator v = polyhedron.vertices_begin();
                                  v != polyhedron.vertices_end(); ++v) {
        RList point;

        Point3d p = v->point();
        pmap[p] = idx++;
        point.append(p.x());
        point.append(p.y());
        point.append(p.z());
        points.append(point);
    }
    pmreg.append(points);

    RList faces;
    for (Facet_iterator f = polyhedron.facets_begin();
                                   f != polyhedron.facets_end(); f++) {
        Halfedge_facet_circulator h = f->facet_begin(), he(h);

        RList face;
        do {
            Point3d p = h->vertex()->point();
            face.append((double)pmap[p]);
        } while (++h != he);
        faces.append(face);
    }
    pmreg.append(faces);

    return raw ? pmreg : pmreg.obj("pmregion", "pmregion");
}

RList PMRegion::toRList () {
	return toRList(false);
}

void Arrangement2Region(Arrangement::Face_iterator fi, RList& region) {
    for (Arrangement::Hole_iterator fs = fi->holes_begin();
            fs != fi->holes_end(); fs++) {
        RList face;

        RList maincycle;
        Arrangement::Ccb_halfedge_circulator c = *fs, ec(c);
        do {
            RList point;
            point.append(c->source()->point().x());
            point.append(c->source()->point().y());
            maincycle.append(point);
        } while (++c != ec);
        face.append(maincycle);

        Arrangement::Face_handle ft = c->twin()->face();
        for (Arrangement::Hole_iterator hi = ft->holes_begin();
                hi != ft->holes_end(); hi++) {
            RList holecycle;
            Arrangement::Ccb_halfedge_circulator hs = *hi, he = hs;
            do {
                RList point;
                point.append(hs->source()->point().x());
                point.append(hs->source()->point().y());
                holecycle.append(point);
            } while (++hs != he);
            face.append(holecycle);

	    Arrangement2Region(hs->twin()->face(), region);
        }
        region.prepend(face);
    }
}

RList Polygons2Region (vector<Polygon_with_holes_2> polygons) {
    RList faces;

    for (unsigned int i = 0; i < polygons.size(); i++) {
        RList face;
        RList cycle;
        Polygon_with_holes_2& p = polygons[i];
        for (PG2VI vi = p.outer_boundary().vertices_begin();
                                vi != p.outer_boundary().vertices_end(); vi++) {
            RList point;
            point.append(vi->x());
            point.append(vi->y());
            cycle.append(point);
        }
        face.append(cycle);
        for (Hole_const_iterator h = p.holes_begin(); h != p.holes_end(); h++) {
            Polygon_2 po = *h;
            RList hole;
            for (PG2VI vi = po.vertices_begin(); vi != po.vertices_end();vi++) {
                RList point;
                point.append(vi->x());
                point.append(vi->y());
                hole.append(point);
            }
            face.append(hole);
        }
        faces.append(face);
    }

    return faces.obj("region", "region");
}

static Polygon_2 Cycle2Polygon (RList cycle) {
    vector<Point_2> points;
    for (unsigned int i = 0; i < cycle.items.size(); i++) {
        RList pl = cycle.items[i];
        points.push_back(Point_2(pl.items[0].getFt(), pl.items[1].getFt()));
    }
    return Polygon_2(points.begin(), points.end());
}

static Polygon_with_holes_2 Face2Polygon (RList face) {
    Polygon_2 main = Cycle2Polygon(face.items[0]);
    vector<Polygon_2> holes;
    for (unsigned int i = 1; i < face.items.size(); i++) {
        Polygon_2 hole = Cycle2Polygon(face.items[i]);
        holes.push_back(hole);
    }

    return Polygon_with_holes_2(main, holes.begin(), holes.end());
}

vector<Polygon_with_holes_2> Region2Polygons (RList region) {
    vector<Polygon_with_holes_2> ret;
    RList faces = region.items[4];
    for (unsigned int i = 0; i < faces.items.size(); i++) {
        RList face = faces.items[i];
        Polygon_with_holes_2 p = Face2Polygon(face);
        ret.push_back(p);
    }

    return ret;
}

typedef Polygon_with_holes_2::Hole_const_iterator Hole_const_iterator;

Kernel::FT area (Polygon_with_holes_2 p) {
    Kernel::FT ret = 0;

    ret += abs(p.outer_boundary().area());
    for (Hole_const_iterator hi = p.holes_begin(); hi != p.holes_end(); hi++) {
        ret -= abs(hi->area());
    }

    return ret;
}

Kernel::FT area (vector<Polygon_with_holes_2> ps) {
    Kernel::FT ret = 0;

    for (unsigned int i = 0; i < ps.size(); i++) {
        ret += area(ps[i]);
    }

    return ret;
}

Kernel::FT area (RList region) {
    return area(Region2Polygons(region));
}

vector<Segment> mpoint2segments (RList& obj) {
    vector<Segment> segs;
    RList& upoints = obj.items[4];

    for (unsigned int i = 0; i < upoints.items.size(); i++) {
        RList& upoint = upoints.items[i];

        RList& iv = upoint.items[0];
        double z1 = parsetime(iv.items[0].getString());
        double z2 = parsetime(iv.items[1].getString());

        RList& points = upoint.items[1];
	Kernel::FT x1 = points.items[0].getFt();
	Kernel::FT y1 = points.items[1].getFt();
	Kernel::FT x2 = points.items[2].getFt();
	Kernel::FT y2 = points.items[3].getFt();

        Point3d p(x1, y1, z1);
        Point3d q(x2, y2, z2);

                segs.push_back(Segment(p, q));
        }

        return segs;
}

/* Convert a Nef polyhedron to a normal polyhedron */
Polyhedron nef2polyhedron (Nef_polyhedron& np) {
    Polyhedron p;

    
    // If the polyhedron is simple, it can be converted directly
    if (np.is_simple()) {
        np.convert_to_polyhedron(p);
        return p;
    }

    // Otherwise, a polygon soup has to be created
    std::vector<Point3d> points;
    std::vector<std::vector<std::size_t> > polygons;
    for(Nef_polyhedron::Halffacet_const_iterator
            f = np.halffacets_begin (),
            end = np.halffacets_end();
            f != end; ++f) {
        if(f->is_twin()) continue;
        for(Nef_polyhedron::Halffacet_cycle_const_iterator
                fc = f->facet_cycles_begin(),
                end = f->facet_cycles_end();
                fc != end; ++fc)
        {
            if ( fc.is_shalfedge() )
            {
                Nef_polyhedron::SHalfedge_const_handle h = fc;
                Nef_polyhedron::SHalfedge_around_facet_const_circulator hc(h),
                                                                         he(hc);
                std::vector<std::size_t> face;
                CGAL_For_all(hc,he) {
                    Nef_polyhedron::SVertex_const_handle v = hc->source();
                    const Nef_polyhedron::Point_3& point = v->source()->point();
                    std::vector<Point3d>::iterator it =
                        find(points.begin(), points.end(), point);
                    int idx = std::distance(points.begin(), it);
                    if (it == points.end())
                        points.push_back(point);
                    face.push_back(idx);
                }
                polygons.push_back(face);
            }
        }
    }

    // The soup has to be oriented, converted to a mesh and triangulated
    CGAL::Polygon_mesh_processing::orient_polygon_soup(points, polygons);
    CGAL::Polygon_mesh_processing::polygon_soup_to_polygon_mesh(points,
                                                                 polygons, p);
    CGAL::Polygon_mesh_processing::triangulate_faces(p);
    // To be safe, convert to Nef and back
    Nef_polyhedron tmp(p);
    tmp.convert_to_polyhedron(p);

    return p;
}

static Nef_polyhedron cycle2polyhedron (RList cycle, Kernel::FT instant1, Kernel::FT instant2, Kernel::FT xoff) {
    std::vector<Point3d> xpoints;
    std::vector<std::vector<std::size_t> > xpolygons;
    std::map<Point3d, int> points2index;
    int idx = 0;

    std::vector<std::size_t> top, bottom;
    unsigned int sz = cycle.items.size();
    for (unsigned int i = 0; i < sz; i++) {
        std::vector<std::size_t> facet;

        RList prl1 = cycle.items[i];
        RList prl2 = cycle.items[(i+1)%sz];

        Point3d p1(prl1.items[0].getFt(), prl1.items[1].getFt(), instant1);
        Point3d p2(prl2.items[0].getFt(), prl2.items[1].getFt(), instant1);
        Point3d p3(prl2.items[0].getFt()+xoff, prl2.items[1].getFt(), instant2);
        Point3d p4(prl1.items[0].getFt()+xoff, prl1.items[1].getFt(), instant2);

        if (points2index.count(p1) == 0) {
            xpoints.push_back(p1);
            points2index[p1] = idx;
            facet.push_back(idx++);
        } else {
            facet.push_back(points2index[p1]);
        }
        if (points2index.count(p2) == 0) {
            xpoints.push_back(p2);
            points2index[p2] = idx;
            facet.push_back(idx++);
        } else {
            facet.push_back(points2index[p2]);
        }
        if (points2index.count(p3) == 0) {
            xpoints.push_back(p3);
            points2index[p3] = idx;
            facet.push_back(idx++);
        } else {
            facet.push_back(points2index[p3]);
        }
        if (points2index.count(p4) == 0) {
            xpoints.push_back(p4);
            points2index[p4] = idx;
            facet.push_back(idx++);
        } else {
            facet.push_back(points2index[p4]);
        }

        bottom.push_back(points2index[p1]);
        top.push_back(points2index[p4]);
        xpolygons.push_back(facet);
    }

    std::reverse(top.begin(), top.end());
    xpolygons.push_back(top);
    xpolygons.push_back(bottom);

    Polyhedron p;
    CGAL::Polygon_mesh_processing::orient_polygon_soup(xpoints, xpolygons);
    CGAL::Polygon_mesh_processing::polygon_soup_to_polygon_mesh(xpoints,
                                                                 xpolygons, p);
    CGAL::Polygon_mesh_processing::triangulate_faces(p);

    return Nef_polyhedron(p);
}



static Nef_polyhedron face2polyhedron (RList face, Kernel::FT instant1, Kernel::FT instant2, Kernel::FT xoff) {
    RList main = face.items[0];
    Nef_polyhedron np = cycle2polyhedron(main, instant1, instant2, xoff);
    for (unsigned int i = 1; i < face.items.size(); i++) {
        RList hole = face.items[i];
        Nef_polyhedron nphole = cycle2polyhedron(hole, instant1, instant2, xoff);
        np = np - nphole;
    }

    return np;
}

PMRegion PMRegion::fromRegion (RList reg, Kernel::FT instant1, Kernel::FT instant2, Kernel::FT xoff) {
   RList faces = reg.items[4];
   Nef_polyhedron np;
   for (unsigned int i = 0; i < faces.items.size(); i++) {
       RList face = faces.items[i];
       Nef_polyhedron npface = face2polyhedron(face, instant1, instant2, xoff);
       np = np + npface;
   }
   PMRegion ret;
   ret.polyhedron = nef2polyhedron(np);

   return ret;
}

}
