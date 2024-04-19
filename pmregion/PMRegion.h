/* 
  This file is part of libpmregion
  
  File:   PMRegion.h
  Author: Florian Heinz <fh@sysv.de>
  
  1 PMRegion.h
    Interface class Definitions for PMregion and RList
 
*/

#ifndef PMREGION_H
#define PMREGION_H

namespace pmreg {

class RList;
class MReal;
class MBool;
class ScalarField;

class PMRegion {
    public:
        PMRegion() {}
        ~PMRegion() {}
        PMRegion(Polyhedron p) {
            polyhedron = p;
        }
        PMRegion operator+(PMRegion& pmr);
        PMRegion operator-(PMRegion& pmr);
        PMRegion operator*(PMRegion& pmr);
        RList atinstant(Kernel::FT instant);
        RList atinstant2(Kernel::FT instant);
        MBool mpointinside(RList& mpoint);
        MBool intersects(PMRegion& pmr);
        MReal perimeter();
        MReal area();
        MReal area2();
        RList traversedarea();
        PMRegion createcdpoly();
        PMRegion createcdpoly(RList baseregion);
        PMRegion restrictcdpoly(RList baseregion);
        PMRegion createccdpoly();
        PMRegion createccdpoly(RList baseregion);
        PMRegion createicdpoly(Kernel::FT duration);
        PMRegion createicdpoly(Kernel::FT duration, RList baseregion);
        Kernel::FT coverduration (Kernel::FT x, Kernel::FT y);
        RList maxcovered (Kernel::FT duration);
        RList mincovered (Kernel::FT duration);
        RList intervalcovered (Kernel::FT startduration);
        Kernel::FT avgcover ();
        Kernel::FT avgcover (RList baseregion);
        void openscad(string filename);
        ScalarField scalarfield();
        void zthicknessprepare();
        Kernel::FT zthickness(Point2d p2d);
        Plane calculate_plane(Polygon p);
        void translate (Kernel::FT x, Kernel::FT y, Kernel::FT z);
        pair<Kernel::FT, Kernel::FT> minmaxz();
        pair<Point3d, Point3d> boundingbox();
        void analyze();
	vector<Polygon_with_holes_2> projectxy();

        static PMRegion fromRList(RList rl);
        RList toRList(bool raw);
        RList toRList();

        static PMRegion fromOFF(std::string off);
        string toOFF();

        static PMRegion fromMRegion(RList mr);
        RList toMRegion();
        RList toMRegion2();
        RList toMRegion2(int raw);

        static PMRegion fromRegion(RList reg, Kernel::FT instant1, Kernel::FT instant2, Kernel::FT xoff);

        Polyhedron polyhedron;
	Polyhedron zthicknesstmp;
	Tree *zthicknesstree;
	map<Point2d, Kernel::FT> ztcache;
	Point_inside *inside_tester;
};

class RList {
    protected:
        int type;
        string str;
        double nr;
	Kernel::FT ft;
        bool boolean;
        string ToString(int indent);

    public:
        vector<RList> items;

        RList();
        void append(double nr);
        void append(Kernel::FT nr);
        void append(string str);
        void appendsym(string str);
        void append(bool val);
        void append(RList l);
        void prepend(RList l);
        RList* point(double x, double y);
        void concat(RList l);
        double getNr () {
            assert(type == NL_DOUBLE || type == NL_FT);
	    if (type == NL_DOUBLE)
		    return nr;
	    else
		    return ::CGAL::to_double(ft);
        }
	Kernel::FT getFt () {
            assert(type == NL_FT);
            return ft;
        }
        bool getBool () {
            assert(type == NL_BOOL);
            return boolean;
        }
        string getString () {
            assert(type == NL_STRING);
            return str;
        }
        string getSym () {
            assert(type == NL_SYM);
            return str;
        }
        int getType () {
            return type;
        }
        RList* nest();

        RList obj(string name, string type);
        static RList parse(std::istream& f);
        string ToString();
};

class MReal {
    public:
        RList rl;

        MReal() {
            rl.appendsym("OBJECT");
            rl.appendsym("mreal");
            RList empty;
            rl.append(empty);
            rl.appendsym("mreal");
            rl.append(empty);
        }

        void append (Kernel::FT start, Kernel::FT end,
                Kernel::FT a, Kernel::FT b, Kernel::FT c) {
            RList ureal;
            RList interval;
            interval.append(timestr(start));
            interval.append(timestr(end));
            interval.append((bool)true);
            interval.append((bool)false);

            RList coeffs;
            coeffs.append(a);
            coeffs.append(b);
            coeffs.append(c);
            coeffs.append((bool)false);

            ureal.append(interval);
            ureal.append(coeffs);
            rl.items[4].append(ureal);
        }
};

class MBool {
    public:
        RList rl;

        MBool() {
            rl.appendsym("OBJECT");
            rl.appendsym("mbool");
            RList empty;
            rl.append(empty);
            rl.appendsym("mbool");
            rl.append(empty);
        }

        void append (Kernel::FT start, Kernel::FT end, bool value) {
            RList ubool;
            RList interval;
            interval.append(timestr(start));
            interval.append(timestr(end));
            interval.append((bool)true);
            interval.append((bool)false);

            ubool.append(interval);
            ubool.append(value);
            rl.items[4].append(ubool);
        }
};

class ScalarField {
    public:
        vector<Polygon> polygons;
        vector<vector<Kernel::FT> > coeffs;

        void add (Polygon polygon, Plane plane);
        Kernel::FT value(Point2d p);

        string ToString();

        static ScalarField fromRList(RList rl);
        RList toRList();
};

}

#endif /* PMREGION_H */
