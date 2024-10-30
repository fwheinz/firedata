#define CGAL_HAS_THREADS 1

#include <CGAL/Exact_predicates_exact_constructions_kernel.h>
#include <CGAL/Lazy_kernel.h>
#include <CGAL/Nef_polyhedron_3.h>
#include <CGAL/Polyhedron_3.h>
#include <CGAL/Bbox_3.h>
#include <CGAL/Polygon_mesh_processing/bbox.h>
#include <CGAL/Polygon_mesh_processing/orient_polygon_soup.h>
#include <CGAL/Polygon_mesh_processing/polygon_soup_to_polygon_mesh.h>
#include <CGAL/IO/Polyhedron_iostream.h>
#include <CGAL/Nef_3/SNC_iteration.h>

#include "PMRegion_internal.h"

#include <sys/time.h>
#include <sys/sysinfo.h>

#include <thread>
#include <mutex>

#include <sstream>
#include <fstream>
#include <iomanip>
#include <map>
#include <queue>
#include <ctime>
#include <cstdio>

using namespace std;

typedef CGAL::Exact_predicates_exact_constructions_kernel Kernel;
typedef Kernel::Point_3 Point;
typedef Kernel::Vector_3 Vector;
typedef CGAL::Polyhedron_3<Kernel> Polyhedron;
typedef CGAL::Nef_polyhedron_3<Kernel> Nef_polyhedron;
typedef CGAL::Nef_polyhedron_3<Kernel>::Volume_const_iterator Volume_const_iterator;
typedef CGAL::Nef_polyhedron_3<Kernel>::Shell_entry_const_iterator Shell_entry_const_iterator;
int nrthreads = 64;
int verbose = 0;

double scale = 1.1;
int pattern = 2;

enum {
	FMT_PMREGION = 0,
	FMT_MREGION,
	FMT_PMREGION_STREAM,
	FMT_MREGION_STREAM,
	FMT_OFF
};

int format = FMT_PMREGION;

Nef_polyhedron build_box (Point *p, Kernel::FT dx, Kernel::FT dy);
Nef_polyhedron build_diamond (Point *p, Kernel::FT dx, Kernel::FT dy);
Nef_polyhedron build_arrow (Point *p, Kernel::FT dx, Kernel::FT dy);

const char *outfilename = "firms";

std::mutex cgal;

uint64_t compilems, unionms;

int timems (void) {
	struct timeval tv;
	gettimeofday(&tv, NULL);

	return tv.tv_sec*1000+tv.tv_usec/1000;
}

class Data {
	public:
		Point p;
		double scan, track;
		Nef_polyhedron poly;
		int compiled;
		int nr, level;
		double sx, sy;

		Data(Kernel::FT x, Kernel::FT y, Kernel::FT z, double scan, double track) : scan(scan), track(track) {
			p = Point(x, y, z);
			sx = scan * scale / ((M_PI/180.0)*6367.449*cos(CGAL::to_double(y)*M_PI/180.0));
			sy = track * scale / 110.574;
			nr = 1;
			level = 0;
			compiled = 0;
		}

		void compile () {
			if (compiled == 0) {
				int ms = timems();
				if (pattern == 1)
					poly = build_box(&p, sx, sy);
				else if (pattern == 2)
					poly = build_diamond(&p, sx, sy);
				else if (pattern == 3)
					poly = build_arrow(&p, sx, sy);
				else // default
					poly = build_diamond(&p, sx, sy);
				ms = timems() - ms;
				compilems += ms;
				compiled = 1;
			}
		}

		void reduce (double val) {
			p.z() -= val;
		}

		void add (Data *d) {
			poly += d->poly;
			nr += d->nr;
			level++;
		}
};

class Cluster {
	public:
		vector<Data *> data;
		queue<Data *> q;
		int worksize;
		std::mutex mx;
		CGAL::Bbox_3 bbox;

		Cluster (Data *d) {
			data.push_back(d);
		}

		void compile () {
			assert(data.size() == 1);
			data[0]->compile();
			Polyhedron poly;
			data[0]->poly.convert_to_polyhedron(poly);
			bbox = CGAL::Polygon_mesh_processing::bbox (poly);
		}

		void reduce (double val) {
			assert(data.size() == 1);
			data[0]->reduce(val);
		}

		void merge (Cluster *c) {
			data.insert(data.end(), c->data.begin(), c->data.end());
			bbox += c->bbox;
		}

		void preparework () {
			worksize = 0;
			for (unsigned int i = 0; i < data.size(); i++) {
				q.push(data[i]);
				worksize++;
			}
		}

		int dowork () {

			mx.lock();
			if (worksize >= 2 && q.size() < 2) {
				mx.unlock();
				return -1;
			} else if (worksize == 1) {
				mx.unlock();
				return 1;
			}	
			Data *w1 = q.front();
			q.pop();
			Data *w2 = q.front();
			q.pop();
			int nrw1 = w1->nr;
			int nrw2 = w2->nr;
			mx.unlock();

			int ms = timems();
			w1->add(w2);
			ms = timems() - ms;

			mx.lock();
			unionms += ms;
			if (verbose >= 3)
				cerr << "Merged " << nrw1 << " and " << nrw2 << " cubes in " << ms << " ms (total " << unionms << " ms, qlength " << q.size() << ")" << endl;
			delete w2;
			q.push(w1);
			worksize--;
			int ret = worksize == 1 ? 1 : 0;
			mx.unlock();

			return ret;
		}

		int finished () {
			return worksize == 1;
		}

		Nef_polyhedron getpoly () {
			return q.front()->poly;
		}
};


double minlat, minlon, maxlat, maxlon;
double mintime, maxtime;
int rangeactive = 0, timeactive = 0;

static double utctime (struct tm *tm) {
	char *tz;
	double ret;

	tz = getenv("TZ");
	setenv("TZ", "UTC", 1);
	tzset();
	ret = mktime(tm);
	if (tz)
		setenv("TZ", tz, 1);
	else
        unsetenv("TZ");
    tzset();

    return ret;
}

double parsetime (std::string str) {
    struct tm tm;
    unsigned int msec;
    char sep; // Separator, space or -

    tm.tm_year = tm.tm_mon = tm.tm_mday = 0;
    tm.tm_sec = tm.tm_min = tm.tm_hour = tm.tm_isdst = msec = 0;

    int st = sscanf(str.c_str(), "%u-%u-%u%c%u:%u:%u.%u",
            &tm.tm_year, &tm.tm_mon, &tm.tm_mday, &sep,
            &tm.tm_hour, &tm.tm_min, &tm.tm_sec,
            &msec);
    if (st < 3)
        return NAN;

    tm.tm_year -= 1900; // struct tm expects years since 1900
    tm.tm_mon--; // struct tm expects months to be numbered from 0 - 11

    double ret = utctime(&tm) * 1000 + msec;

    return ret;
}

int nrpoints = -1;

vector<Cluster *> createClusterFromVIIRSCSV (char * fname) {
    ifstream i1(fname, fstream::in);
    vector<Cluster *> ps;

    string line;
    double mini = 1, maxi = 0;
    int inpoints = 0, nrfiltered = 0;

    while (getline(i1, line)) {
        const char *_lat = strtok(strdup(line.c_str()), ",");
        const char *_lon = strtok(NULL, ",");
        strtok(NULL, ","); // bright
        const char *_scan = strtok(NULL, ","); // scan
        const char *_track = strtok(NULL, ","); // track
        const char *_date = strtok(NULL, ","); // acq_date
        const char *_time = strtok(NULL, ","); // acq_time

        double lat = atof(_lat);
        double lon = atof(_lon);
				double scan = atof(_scan);
				double track = atof(_track);
        int time = atoi(_time);
        char instant[100];
        sprintf(instant, "%s-%02d:%02d:00", _date, time/100, time%100);

        double z = parsetime(instant);

        if (!lat)
            continue;
        if (rangeactive && (lat < minlat || lat > maxlat || lon < minlon || lon > maxlon)) {
            nrfiltered++;
            continue;
        }
				if (timeactive && (z < mintime || z > maxtime)) {
					nrfiltered++;
					continue;
				}

        if (mini > maxi)
            mini = maxi = z;
        else if (z < mini)
            mini = z;
        else if (z > maxi)
            maxi = z;

        ps.push_back(new Cluster(new Data(lon, lat, z, scan, track)));
        inpoints++;
	if (nrpoints > 0 && nrpoints <= inpoints)
		break;
    }

    for (unsigned int i = 0; i < ps.size(); i++) {
	    ps[i]->reduce(mini);
    }

    cerr << inpoints << " points loaded, " << nrfiltered << " points filtered." << endl;

    return ps;

}

double duration = 86400000;

Nef_polyhedron build_box (Point* p, Kernel::FT dx, Kernel::FT dy) {
    std::vector<std::vector<std::size_t> > xpolygons;
    std::vector<size_t> xfacet;

    Kernel::FT dz = duration;

    Vector da1(0,  0,  0 );
    Vector da2(dx, 0,  0 );
    Vector da3(dx, dy, 0 );
    Vector da4(0,  dy, 0 );
    Vector db1(0,  0,  dz);
    Vector db2(dx, 0,  dz);
    Vector db3(dx, dy, dz);
    Vector db4(0,  dy, dz);
    Point p0 = *p+da1;
    Point p1 = *p+da2;
    Point p2 = *p+da3;
    Point p3 = *p+da4;
    Point p4 = *p+db1;
    Point p5 = *p+db2;
    Point p6 = *p+db3;
    Point p7 = *p+db4;
    vector<Point> xpoints;
    xpoints.push_back(p0);
    xpoints.push_back(p1);
    xpoints.push_back(p2);
    xpoints.push_back(p3);
    xpoints.push_back(p4);
    xpoints.push_back(p5);
    xpoints.push_back(p6);
    xpoints.push_back(p7);

    xfacet.push_back(0);
    xfacet.push_back(1);
    xfacet.push_back(2);
    xfacet.push_back(3);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(0);
    xfacet.push_back(1);
    xfacet.push_back(5);
    xfacet.push_back(4);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(1);
    xfacet.push_back(2);
    xfacet.push_back(6);
    xfacet.push_back(5);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(2);
    xfacet.push_back(3);
    xfacet.push_back(7);
    xfacet.push_back(6);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(3);
    xfacet.push_back(0);
    xfacet.push_back(4);
    xfacet.push_back(7);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(7);
    xfacet.push_back(6);
    xfacet.push_back(5);
    xfacet.push_back(4);
    xpolygons.push_back(xfacet);

    Polyhedron poly;
    CGAL::Polygon_mesh_processing::orient_polygon_soup(xpoints, xpolygons);
    CGAL::Polygon_mesh_processing::polygon_soup_to_polygon_mesh(xpoints,
            xpolygons, poly);
    Nef_polyhedron np(poly);

    return np;
}

Nef_polyhedron build_diamond (Point *p, Kernel::FT dx, Kernel::FT dy) {
    vector<Point> xpoints;
    std::vector<std::vector<std::size_t> > xpolygons;
    std::vector<size_t> xfacet;

    Kernel::FT dz = duration;

    Vector da1(0,  0,  0 );
    Vector da2(dx, 0,  0 );
    Vector da3(dx, dy, 0 );
    Vector da4(0,  dy, 0 );
    Vector db1(0,  0,  dz);
    Vector db2(dx, 0,  dz);
    Vector db3(dx, dy, dz);
    Vector db4(0,  dy, dz);
    Vector dbelow(dx/2, dy/2, -dz*0.1);
    Vector dabove(dx/2, dy/2, dz*1.1);

    Point p0 = *p+da1;
    Point p1 = *p+da2;
    Point p2 = *p+da3;
    Point p3 = *p+da4;
    Point p4 = *p+db1;
    Point p5 = *p+db2;
    Point p6 = *p+db3;
    Point p7 = *p+db4;
    Point p8 = *p+dbelow;
    Point p9 = *p+dabove;

    xpoints.push_back(p0);
    xpoints.push_back(p1);
    xpoints.push_back(p2);
    xpoints.push_back(p3);
    xpoints.push_back(p4);
    xpoints.push_back(p5);
    xpoints.push_back(p6);
    xpoints.push_back(p7);
    xpoints.push_back(p8);
    xpoints.push_back(p9);

    // Bottom hat
    xfacet.clear();
    xfacet.push_back(0);
    xfacet.push_back(1);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(3);
    xfacet.push_back(0);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(2);
    xfacet.push_back(3);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(1);
    xfacet.push_back(2);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    // Main cube
    xfacet.clear();
    xfacet.push_back(4);
    xfacet.push_back(5);
    xfacet.push_back(1);
    xfacet.push_back(0);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(5);
    xfacet.push_back(6);
    xfacet.push_back(2);
    xfacet.push_back(1);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(6);
    xfacet.push_back(7);
    xfacet.push_back(3);
    xfacet.push_back(2);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(7);
    xfacet.push_back(4);
    xfacet.push_back(0);
    xfacet.push_back(3);
    xpolygons.push_back(xfacet);

    // Top hat
    xfacet.clear();
    xfacet.push_back(5);
    xfacet.push_back(4);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(6);
    xfacet.push_back(5);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(7);
    xfacet.push_back(6);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(4);
    xfacet.push_back(7);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);


    Polyhedron poly;
    CGAL::Polygon_mesh_processing::orient_polygon_soup(xpoints, xpolygons);
    CGAL::Polygon_mesh_processing::polygon_soup_to_polygon_mesh(xpoints,
		    xpolygons, poly);
    Nef_polyhedron np(poly);

    return np;
}

Nef_polyhedron build_arrow (Point *p, Kernel::FT dx, Kernel::FT dy) {
    vector<Point> xpoints;
    std::vector<std::vector<std::size_t> > xpolygons;
    std::vector<size_t> xfacet;

    Kernel::FT dz = duration;

    Vector da1(0,  0,  0 );
    Vector da2(dx, 0,  0 );
    Vector da3(dx, dy, 0 );
    Vector da4(0,  dy, 0 );
    Vector db1(0,  0,  dz*1.1);
    Vector db2(dx, 0,  dz*1.1);
    Vector db3(dx, dy, dz*1.1);
    Vector db4(0,  dy, dz*1.1);
    Vector dbelow(dx/2, dy/2, -dz*0.1);
    Vector dabove(dx/2, dy/2, 0);

    Point p0 = *p+da1;
    Point p1 = *p+da2;
    Point p2 = *p+da3;
    Point p3 = *p+da4;
    Point p4 = *p+db1;
    Point p5 = *p+db2;
    Point p6 = *p+db3;
    Point p7 = *p+db4;
    Point p8 = *p+dbelow;
    Point p9 = *p+dabove;

    xpoints.push_back(p0);
    xpoints.push_back(p1);
    xpoints.push_back(p2);
    xpoints.push_back(p3);
    xpoints.push_back(p4);
    xpoints.push_back(p5);
    xpoints.push_back(p6);
    xpoints.push_back(p7);
    xpoints.push_back(p8);
    xpoints.push_back(p9);

    // Bottom hat
    xfacet.clear();
    xfacet.push_back(0);
    xfacet.push_back(1);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(3);
    xfacet.push_back(0);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(2);
    xfacet.push_back(3);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(1);
    xfacet.push_back(2);
    xfacet.push_back(8);
    xpolygons.push_back(xfacet);

    // Main cube
    xfacet.clear();
    xfacet.push_back(4);
    xfacet.push_back(5);
    xfacet.push_back(1);
    xfacet.push_back(0);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(5);
    xfacet.push_back(6);
    xfacet.push_back(2);
    xfacet.push_back(1);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(6);
    xfacet.push_back(7);
    xfacet.push_back(3);
    xfacet.push_back(2);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(7);
    xfacet.push_back(4);
    xfacet.push_back(0);
    xfacet.push_back(3);
    xpolygons.push_back(xfacet);

    // Top hat
    xfacet.clear();
    xfacet.push_back(5);
    xfacet.push_back(4);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(6);
    xfacet.push_back(5);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(7);
    xfacet.push_back(6);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);

    xfacet.clear();
    xfacet.push_back(4);
    xfacet.push_back(7);
    xfacet.push_back(9);
    xpolygons.push_back(xfacet);


    Polyhedron poly;
    CGAL::Polygon_mesh_processing::orient_polygon_soup(xpoints, xpolygons);
    CGAL::Polygon_mesh_processing::polygon_soup_to_polygon_mesh(xpoints,
		    xpolygons, poly);
    Nef_polyhedron np(poly);

    return np;
}


queue<Cluster *> clusters;
vector<Polyhedron> polys;
vector<Cluster *> points;

std::mutex mtx;
unsigned int nrcomp = 0;
void compilecluster (void) {
	mtx.lock();
	while (1) {
		if (nrcomp >= points.size())
			break;
		Cluster *c = points[nrcomp++];
		mtx.unlock();
		c->compile();
		mtx.lock();
	}
	mtx.unlock();
}

void unifycluster (void) {
	while (1) {
		mtx.lock();
		if (clusters.size() == 0) {
			mtx.unlock();
			break;
		}
		Cluster *c = clusters.front();
		clusters.pop();
		mtx.unlock();
		int st = c->dowork();
		if (st == 1) {
			Nef_polyhedron np = c->getpoly();
			Volume_const_iterator vi = ++np.volumes_begin();
			int i = 1;
			CGAL_forall_volumes(vi, np) {
				if (vi->mark()) {
					Shell_entry_const_iterator si;
					CGAL_forall_shells_of(si, vi) {
						Nef_polyhedron::SFace_const_handle sfch(si);
						Nef_polyhedron O(np, sfch);
						Polyhedron poly;
						O.convert_to_polyhedron(poly);
						mtx.lock();
						polys.push_back(poly);
						if (verbose >= 1)
							cerr << "Pushed back volume " << i++ << endl;
						mtx.unlock();
					}
				}
			}
			delete c;
		} else {
			mtx.lock();
			clusters.push(c);
			mtx.unlock();
		}
	}
}

Polyhedron unify_polyhedra () {
    cerr << "Got " << polys.size() << " polyhedra, concatenating..." << endl;

    vector<Point> xpoints;
    std::vector<std::vector<std::size_t> > xpolygons;

    int idx = 0;
    map<Point, int> pmap;
    for (unsigned int i = 0; i < polys.size(); i++) {
	    Polyhedron polyhedron = polys[i];
	    for (Vertex_iterator v = polyhedron.vertices_begin();
			    v != polyhedron.vertices_end(); ++v) {
		    pmap[v->point()] = idx++;
		    xpoints.push_back(v->point());
	    }

	    for (Facet_iterator f = polyhedron.facets_begin();
			    f != polyhedron.facets_end(); f++) {
		    Halfedge_facet_circulator h = f->facet_begin(), he(h);

		    vector<size_t> xfacet;
		    do {
			    Point p = h->vertex()->point();
			    xfacet.push_back(pmap[p]);
		    } while (++h != he);
		    xpolygons.push_back(xfacet);
	    }
	    if (verbose >= 2) {
		    cerr << (polys.size() - i) << " polyhedra left" << endl;
	    }
    }

    cerr << "Re-building polyhedron from polygon soup..." << endl;
    Polyhedron poly;
    CGAL::Polygon_mesh_processing::orient_polygon_soup(xpoints, xpolygons);
    CGAL::Polygon_mesh_processing::polygon_soup_to_polygon_mesh(xpoints,
		    xpolygons, poly);

    return poly;
}


void do_convert (char * fname) {
    vector<std::thread *> threads;

    int ms = timems();
    int msprev = ms;
    cerr << "Creating Points from CSV..." << endl;
    points = createClusterFromVIIRSCSV(fname);
    nrpoints = points.size();
    ms = timems();
    cerr << "Time elapsed: " << ms - msprev << " ms" << endl;

    msprev = ms;
    cerr << "Compiling Points..." << endl;
    threads.clear();
    for (int i = 0; i < nrthreads; i++) {
	    std::thread *th = new thread(compilecluster);
	    threads.push_back(th);
    }
    for (unsigned int i = 0; i < threads.size(); i++) {
	    threads[i]->join();
    }
    ms = timems();
    cerr << "Time elapsed: " << ms - msprev << " ms" << endl;
    msprev = ms;

    cerr << "Merging to clusters..." << endl;
    for (int i = 0; i < nrpoints; i++) {
	    Cluster *c = points[i];
	    if (c == NULL)
		    continue;
	    int merges = 0;
	    for (int j = i+1; j < nrpoints; j++) {
		    Cluster *c2 = points[j];
		    if (c2 == NULL)
			    continue;
		    if (CGAL::do_overlap(c->bbox, c2->bbox)) {
			    c->merge(c2);
			    delete c2;
			    points[j] = NULL;
			    merges++;
		    }
	    }
	    if (merges > 0) {
		    i--;
	    }
    }

    for (unsigned int i = 0; i < points.size(); i++) {
	    if (points[i]) {
		    points[i]->preparework();
		    clusters.push(points[i]);
	    }
    }

    if (verbose >= 1)
	    cerr << "Got " << clusters.size() << " clusters" << endl;
    ms = timems();
    cerr << "Time elapsed: " << ms - msprev << " ms" << endl;
    msprev = ms;

    cerr << "Unifying Clusters..." << endl;
    threads.clear();
    for (int i = 0; i < nrthreads; i++) {
        std::thread *th = new thread(unifycluster);
        threads.push_back(th);
    }
    for (unsigned int i = 0; i < threads.size(); i++) {
	    threads[i]->join();
    }

    ms = timems();
    cerr << "Time elapsed: " << ms - msprev << " ms" << endl;
    msprev = ms;

    char filename[100];
    if (format == FMT_MREGION_STREAM) {
	    snprintf(filename, sizeof(filename), "%s-s%f-p%d.mregionstream.sec", outfilename, scale, pattern);
	    cerr << "Writing output off file " << filename << " ..." << endl;
	    std::ofstream out(filename);
	    out << "(OBJECT firms () ( rel ( tuple ( ( Region mregion ) ( Boundingbox rect ) ) ) ) ( " << endl;
	    for (unsigned int i = 0; i < polys.size(); i++) {
		    out << " ( " << endl;
		    pmreg::PMRegion pmreg(polys[i]);
		    out << pmreg.toMRegion2(1).ToString() << endl;
		    std::pair<Point3d, Point3d> bb = pmreg.boundingbox();
		    out << "  ( " << bb.first.x() << " " << bb.second.x() << " " << bb.first.y() << " " << bb.second.y() << " ) " << endl;
		    out << " ) " << endl;
	    }
	    out << ") )" << endl;
	    out.close();
    } else if (format == FMT_PMREGION_STREAM) {
	    snprintf(filename, sizeof(filename), "%s-s%f-p%d.pmregionstream.sec", outfilename, scale, pattern);
	    cerr << "Writing output off file " << filename << " ..." << endl;
	    std::ofstream out(filename);
	    out << "(OBJECT firms () ( rel ( tuple ( ( Region pmregion ) ( Boundingbox rect ) ) ) ) ( " << endl;
	    for (unsigned int i = 0; i < polys.size(); i++) {
		    out << " ( " << endl;
		    pmreg::PMRegion pmreg(polys[i]);
		    out << pmreg.toRList(true).ToString() << endl;
		    std::pair<Point3d, Point3d> bb = pmreg.boundingbox();
		    out << "  ( " << bb.first.x() << " " << bb.second.x() << " " << bb.first.y() << " " << bb.second.y() << " ) " << endl;
		    out << " ) " << endl;
	    }
	    out << ") )" << endl;
	    out.close();
    } else if (format == FMT_PMREGION || format == FMT_MREGION || format == FMT_OFF) {
	    cerr << "Unifying polyhedra" << endl;
	    Polyhedron poly = unify_polyhedra();

	    ms = timems();
	    cerr << "Time elapsed: " << ms - msprev << " ms" << endl;
	    msprev = ms;

	    if (format == FMT_OFF) {
		    snprintf(filename, sizeof(filename), "%s-s%f-p%d.off", outfilename, scale, pattern);
		    cerr << "Writing output off file " << filename << " ..." << endl;
		    std::ofstream out(filename);
		    out << std::setprecision(20) << poly;
	    } else if (format == FMT_PMREGION) {
		    snprintf(filename, sizeof(filename), "%s-s%f-p%d.pmreg", outfilename, scale, pattern);
		    cerr << "Converting to PMRegion (" << filename << ")" << endl;
		    pmreg::PMRegion pmreg(poly);
		    std::ofstream outpmreg(filename);
		    pmreg::RList r = pmreg.toRList();
		    outpmreg << std::setprecision(20) << r.ToString() << endl;
	    } else if (format == FMT_MREGION) {
		    snprintf(filename, sizeof(filename), "%s-s%f-p%d.mreg", outfilename, scale, pattern);
		    cerr << "Converting to MRegion (" << filename << ")" << endl;
		    pmreg::PMRegion pmreg(poly);
		    std::ofstream outpmreg(filename);
		    outpmreg << std::setprecision(20) << pmreg.toMRegion2().ToString() << endl;
	    }
    }

    ms = timems();
    cerr << "Time elapsed: " << ms - msprev << " ms" << endl;
    msprev = ms;

    cerr << "Done!" << endl;
}

void usage (void) {
    fprintf(stderr, "Usage: firms [-r <range>] [-s <size>] [-d <duration>] [-n nrpoints] <csvfile>\n"
		    "              -r <minlat>x<minlon>,<maxlat>x<maxlon> (Range)\n"
		    "              -i <starttime>,<endtime> (Range)\n"
		    "              -s <scale> (Scale factor of a single fire, default 1.1)\n"
		    "              -d <duration> (Duration of a fire in ms, default 86400000)\n"
		    "              -n <nrpoints> (Only parse this number of points, default: all)\n"
		    "              -p <pattern> 1: box, 2: diamond, 3: arrow (default: diamond)\n"
		    "              -o <filename> base filename\n"
		    "              -t <threads> number of threads (default: number of cores)\n"
		    "              -f <format> ps / ms / p / m / off ; pmregion/mregion [stream] or off\n"
		    "              -v Increase verbosity (up to 3 times)\n"
		    "\n");
    exit(EXIT_FAILURE);
}

void parserange (char *range) {
    int st = sscanf(range, "%lfx%lf,%lfx%lf", &minlat, &minlon, &maxlat, &maxlon);
    if (st != 4) {
        fprintf(stderr, "Error: Range '%s' invalid.\n", range);
        usage();
    }
    fprintf(stderr, "Using ranges: Latitude %f - %f, Longitude %f - %f\n", minlat, maxlat, minlon, maxlon);
    rangeactive = 1;
}

void parseinterval (char *interval) {
	char *end = strchr(interval, ',');
	if (!end) {
		printf("Invalid interval (no comma)\n");
		usage();
	}
	*end++ = '\0';
	mintime = parsetime(interval);
	maxtime = parsetime(end);
	timeactive = 1;
}

int main (int argc, char **argv) {
    int opt;

    nrthreads = get_nprocs();

    while ((opt = getopt(argc, argv, "o:f:p:r:i:s:d:n:t:v")) != -1) {
			switch (opt) {
				case 'p':
					pattern = atoi(optarg);
					break;
				case 'r':
					parserange(optarg);
					break;
				case 'i':
					parseinterval(optarg);
					break;
				case 's':
					scale = atof(optarg);
					break;
				case 'd':
					duration = atof(optarg);
					break;
				case 'n':
					nrpoints = atoi(optarg);
					break;
				case 'v':
					verbose++;
					break;
				case 'o':
					outfilename = strdup(optarg);
					break;
				case 'f':
					if (strcasecmp(optarg, "p") == 0)
						format = FMT_PMREGION;
					else if (strcasecmp(optarg, "m") == 0)
						format = FMT_MREGION;
					else if (strcasecmp(optarg, "ps") == 0)
						format = FMT_PMREGION_STREAM;
					else if (strcasecmp(optarg, "ms") == 0)
						format = FMT_MREGION_STREAM;
					else if (strcasecmp(optarg, "off") == 0)
						format = FMT_OFF;
					else {
						fprintf(stderr, "Invalid format: %s\n", optarg);
						usage();
					}
					break;
				case 't':
					nrthreads = atoi(optarg);
					break;
				default:
					usage(); // exits
			}
		}

    if (optind >= argc) {
        cerr << "Input filename missing!" << endl;
        usage();
    }

    cerr << "Using " << nrthreads << " Threads." << endl;

    do_convert(argv[optind]);
}

