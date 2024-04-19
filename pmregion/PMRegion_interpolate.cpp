/* 
 * This file is part of libpmregion
 * 
 * File:   PMRegion\_interpolate.cpp
 * Author: Florian Heinz <fh@sysv.de>

 1 Interpolate
   Provides a native interpolation operation for polyhedral
   moving regions.
 
*/


#include "PMRegion_internal.h"

#include <CGAL/ch_graham_andrew.h>
#include <CGAL/convex_hull_3.h>
#include <CGAL/Aff_transformation_2.h>
#include <CGAL/aff_transformation_tags.h>

typedef CGAL::Aff_transformation_2<Kernel> Transformation;
typedef CGAL::Bbox_2 Bbox_2;

using namespace pmr;
using namespace std;

namespace pmr {
	class Face {
		public:
			Polygon_with_holes_2 c;
			Polyhedron p;
			bool valid;
			bool matched;

			Face (Polygon_with_holes_2 c, Polyhedron p) : c(c), p(p), valid(true), matched(false) {};
			Face (Polygon_with_holes_2 c) : c(c), valid(true), matched(false) {};
			Face () : valid(false), matched(false) {};

			typedef Polyhedron::Halfedge_around_facet_circulator Halfedge_facet_circulator;
			Point3d collapsePoint () {
				Polygon pg = c.outer_boundary();
				// cerr << "Concavity: " << endl;
				for (auto v = pg.vertices_begin(); v != pg.vertices_end(); ++v) {
					Point_2 p1c = *v;
					Point_2 p2c = v+1 == pg.vertices_end() ? *(pg.vertices_begin()) : *(v+1);
					// cerr << "C: " << p1c << " -> " << p2c << endl;

					for (Facet_iterator f = p.facets_begin(); f != p.facets_end(); ++f) {
						// cerr << "New Face: " << endl;
						Halfedge_facet_circulator circulator = f->facet_begin();
						Halfedge_facet_circulator start = circulator;
						do {
							Point3d v2 = circulator->vertex()->point();
							circulator++;
							Point3d v1 = circulator->vertex()->point();
							// cerr << "F: " << v1 << " -> " << v2 << endl;
							if (v1.z() != v2.z())
								continue;
							Point_2 p1f(v1.x(), v1.y());
							Point_2 p2f(v2.x(), v2.y());
							if (p1f == p1c && p2f == p2c) {
								// cerr << "Found matching simplex!" << endl;
								Kernel::FT z = v1.z();
								circulator = start;
								do {
									Point3d v = circulator->vertex()->point();
									if (v.z() != z) {
										// cerr << "collapsePoint: " << v << endl;
										return v;
									}
									circulator++;
								} while (circulator != start);

							}
						} while (circulator != start);
					}
				}

				return Point3d(0,0,0);
			}


	};

  static Polygon_2 convexhull (Polygon_with_holes_2 reg) {
    vector<Point_2> out;
    ch_graham_andrew(reg.outer_boundary().vertices_begin(), reg.outer_boundary().vertices_end(), std::back_inserter(out));

    return Polygon_2(out.begin(), out.end());
  }

  static vector<Face> concavities (Face reg, Polyhedron ph) {
    Polygon_2 cx = convexhull(reg.c);

    vector<Polygon_with_holes_2> cvs;
    CGAL::difference(cx, reg.c, std::back_inserter(cvs));

		vector<Face> fcs;
		for (Polygon_with_holes_2 c : cvs) {
			fcs.push_back(Face(c, ph));
		}

    return fcs;
  }

  Transformation getPolygonTransformation (vector<Face> p) {
		if (p.size() == 0)
			return Transformation(1, 0, 0, 0, 1, 0);
    Bbox_2 b = p[0].c.bbox();

		for (unsigned long int i = 1; i < p.size(); i++) {
			b += p[i].c.bbox();
		}

		double dx = b.xmax() - b.xmin();
		double dy = b.ymax() - b.ymin();
		double xoff = -b.xmin();
		double yoff = -b.ymin();
		double sx = 1000/dx;
		double sy = 1000/dy;

		Transformation transform(sx, 0, xoff*sx, 0, sy, yoff*sy);

		cerr << "Bbox: " << b << endl;
		cerr << "dx dy: " << dx << " " << dy << endl;
		cerr << "Got Transformation: " << sx << " " << sy << " " << xoff << " " << yoff << endl;
		cerr << "Got Transformation: " << transform << endl;

		return transform;
	}

  Polygon_with_holes_2 scalePolygon (Polygon_with_holes_2 p, Transformation tf) {
		for (auto& vertex : p.outer_boundary().container()) {
			  cerr << vertex << " -> ";
        vertex = tf(vertex);
			  cerr << vertex << endl;
    }

    for (auto holeIt = p.holes_begin(); holeIt != p.holes_end(); ++holeIt) {
        for (auto& vertex : holeIt->container()) {
            vertex = tf(vertex);
        }
    }

		return p;
	}

  vector<Polygon_with_holes_2> scalePolygon (vector<Polygon_with_holes_2> ps, Transformation tf) {
		vector<Polygon_with_holes_2> ret;

		for (auto& p : ps) {
			ret.push_back(scalePolygon(p, tf));
		}

		return ret;
	}

#if 0
	Kernel::FT area (Polygon_with_holes_2 p) {
		Kernel::FT a = p.outer_boundary().area();

		for (auto hole_it = p.holes_begin(); hole_it != p.holes_end(); ++hole_it) {
        a -= hole_it->area();
    }

		return a;
	}

	Kernel::FT area (vector<Polygon_with_holes_2> ps) {
		Kernel::FT a = 0;
		for (auto& p : ps) {
        a += area(p);
    }

		return a;
	}
#endif

	Kernel::FT overlap (Polygon_with_holes_2 p1, Polygon_with_holes_2 p2) {
		std::vector<Polygon_with_holes_2> pi;
		CGAL::intersection(p1, p2, std::back_inserter(pi));

		Kernel::FT ia = area(pi);
		Kernel::FT a1 = area(p1);
		Kernel::FT a2 = area(p2);

		Kernel::FT s1 = ia/a1, s2 = ia/a2;
		
		return max(s1, s2);
  }

  bool matches (Face p1, Face p2) {
    return true;
  }

  static vector<pair<Face,Face> > matchFacesAll (vector<Face> scvs, vector<Face> dcvs) {
    vector<pair<Face,Face> > res;
    for (auto scv : scvs) {
      for (auto dcv : dcvs) {
        if (matches(scv, dcv)) {
          res.push_back(pair<Face,Face>(scv, dcv));
        }
      }
    }

    return res;
  }

  static vector<pair<Face&,Face&> > matchFacesOverlap (vector<Face> &scvs, Transformation stf, vector<Face> &dcvs, Transformation dtf) {
		vector<pair<Face&,Face&> > ret;
		for (auto& scv : scvs) {
			for (auto& dcv : dcvs) {
				Polygon_with_holes_2 s = scalePolygon(scv.c, stf);
				Polygon_with_holes_2 d = scalePolygon(dcv.c, dtf);
				cerr << "Src: " << s << endl;
				cerr << "Dst: " << d << endl;
				Kernel::FT score = overlap(s, d);
				cerr << "Overlap score: " << score << endl;
				if (score > 0.5)
					ret.push_back(pair<Face&,Face&>(scv, dcv));
			}
		}

		return ret;
	}

  static vector<pair<Face&,Face&> > matchFaces (vector<Face> &scvs, Transformation stf, vector<Face> &dcvs, Transformation dtf) {
    vector<pair<Face&,Face&> > matches = matchFacesOverlap(scvs, stf, dcvs, dtf);
		for (auto& p : matches) {
			p.first.matched = true;
			p.second.matched = true;
		}

		for (auto& p : scvs) {
			if (!p.matched) {
				Face *f = new Face();
				matches.push_back(pair<Face&,Face&>(p, *f));
				cerr << "scv not matched" << endl;
			}
		}
		for (auto& p : dcvs) {
			if (!p.matched) {
				Face *f = new Face();
				matches.push_back(pair<Face&,Face&>(*f, p));
				cerr << "dcv not matched" << endl;
			}
		}

		return matches;
  }

  static Polyhedron convex_interpolate (Face src, Face dst) {
    vector<Point3d> points; 
		if (src.valid) {
			for (auto it = src.c.outer_boundary().vertices_begin(); it != src.c.outer_boundary().vertices_end(); it++) {
				Point3d p(it->x(), it->y(), 0);
				points.push_back(p);
			}
		} else {
			points.push_back(dst.collapsePoint());
		}
		if (dst.valid) {
			for (auto it = dst.c.outer_boundary().vertices_begin(); it != dst.c.outer_boundary().vertices_end(); it++) {
				Point3d p(it->x(), it->y(), 3600000);
				points.push_back(p);
			}
		} else {
			points.push_back(src.collapsePoint());
		}
    Polyhedron ret;
    CGAL::convex_hull_3(points.begin(), points.end(), ret);

    return ret;
  }

	static Nef_polyhedron _interpolate (vector<Face> src, Transformation stf, vector<Face> dst, Transformation dtf) {
		Nef_polyhedron poly;

		vector<pair<Face&,Face&> > pairs = matchFaces(src, stf, dst, dtf);

    for (auto& pair : pairs) {
      Polyhedron ph = convex_interpolate(pair.first, pair.second);
			Nef_polyhedron np(ph);

			Transformation stfnew = getPolygonTransformation(src);
			Transformation dtfnew = getPolygonTransformation(dst);

      vector<Face> srccvs = concavities(pair.first, ph);
      vector<Face> dstcvs = concavities(pair.second, ph);
      Nef_polyhedron pcvs = _interpolate(srccvs, stfnew, dstcvs, dtfnew);
      Nef_polyhedron pinterp = np - pcvs;
      poly = poly + pinterp;
    }

    return poly;
  }

  PMRegion PMRegion::interpolate (RList *reg1, RList *reg2) {
    vector<Polygon_with_holes_2> _reg1 = Region2Polygons(*reg1);
    vector<Polygon_with_holes_2> _reg2 = Region2Polygons(*reg2);

		vector<Face> src, dst;
		for (Polygon_with_holes_2 c : _reg1) {
			src.push_back(Face(c));
		}
		for (Polygon_with_holes_2 c : _reg2) {
			dst.push_back(Face(c));
		}
		
		Transformation id;
		Transformation stfnew = getPolygonTransformation(src);
		Transformation dtfnew = getPolygonTransformation(dst);

//    Nef_polyhedron p = _interpolate(_reg1, id, _reg2, id);
    Nef_polyhedron p = _interpolate(src, stfnew, dst, dtfnew);
    Polyhedron p2 = nef2polyhedron(p);

    return PMRegion(p2);
  }
}

