/* 
 * This file is part of libpmregion
 * 
 * File:   PMRegion\_internal.h
 * Author: Florian Heinz <fh@sysv.de>

 1 Internal Header
   Internal Headerfile used by the library sources

*/

#ifndef PMREGION_INTERNAL_HXX
#define PMREGION_INTERNAL_HXX

#define VERSION "1.3"

#include <istream>
#include <string>
#include <vector>
#include <cassert>

#include <CGAL/Polyhedron_3.h>
#include <CGAL/Side_of_triangle_mesh.h>
#include <CGAL/Arrangement_2.h>
#include <CGAL/Arr_segment_traits_2.h>
// #include <CGAL/Arr_non_caching_segment_traits_2.h>
#include <CGAL/General_polygon_set_2.h>
#include <CGAL/Polygon_2.h>
#include <CGAL/Nef_polyhedron_3.h>
#include <CGAL/Polyhedron_incremental_builder_3.h>
#include <CGAL/Modifier_base.h>
#include <CGAL/IO/Polyhedron_iostream.h>
#include <CGAL/Exact_predicates_exact_constructions_kernel_with_sqrt.h>
#include <CGAL/Extended_cartesian.h>
#include <CGAL/AABB_tree.h>
#include <CGAL/AABB_traits.h>
#include <CGAL/boost/graph/graph_traits_Polyhedron_3.h>
#include <CGAL/AABB_face_graph_triangle_primitive.h>
#include <CGAL/Polygon_2.h>
#include <CGAL/Boolean_set_operations_2.h>
#include <CGAL/Polygon_mesh_processing/triangulate_faces.h>
#include <CGAL/Polygon_mesh_processing/orient_polygon_soup.h>
#include <CGAL/Polygon_mesh_processing/polygon_soup_to_polygon_mesh.h>
#include <CGAL/Polygon_mesh_processing/orientation.h>
#include <CGAL/Polygon_mesh_processing/refine.h>
#include <CGAL/Polygon_mesh_processing/fair.h>
// #include <CGAL/Polygon_mesh_processing/measure.h>

//typedef CGAL::Extended_cartesian< CGAL::Lazy_exact_nt<CGAL::Gmpq> > Kernel;
typedef CGAL::Exact_predicates_exact_constructions_kernel Kernel;
//typedef CGAL::Exact_predicates_exact_constructions_kernel_with_sqrt Kernel;
//typedef CGAL::Extended_cartesian< CGAL::Lazy_exact_nt<CGAL::Gmpq> > Kernel;
//typedef CGAL::Simple_cartesian<double> Kernel;
typedef CGAL::Polyhedron_3<Kernel> Polyhedron;
typedef CGAL::Nef_polyhedron_3<Kernel> Nef_polyhedron;

typedef Kernel::Vector_3 Vector;
typedef Kernel::Point_3 Point3d;
typedef Kernel::Point_2 Point2d;
typedef Kernel::Plane_3 Plane;
typedef Kernel::Segment_3 Segment;
typedef Kernel::Segment_2 Segment2d;
typedef CGAL::Polygon_2<Kernel> Polygon;
typedef CGAL::Arr_segment_traits_2<Kernel> Traits_2;
// typedef CGAL::Arr_non_caching_segment_traits_2<Kernel> Traitsnc_2;
typedef Traits_2::Point_2 Point_2;
typedef Traits_2::X_monotone_curve_2 Segment_2;
typedef CGAL::Arrangement_2<Traits_2> Arrangement;
// typedef CGAL::Arrangement_2<Traitsnc_2> Arrangementnc;

typedef CGAL::AABB_face_graph_triangle_primitive<Polyhedron> Primitive;
typedef CGAL::AABB_traits<Kernel, Primitive> Traits;
typedef CGAL::AABB_tree<Traits> Tree;
typedef boost::optional< Tree::Intersection_and_primitive_id<Plane>::Type >
                                                           Plane_intersection;
typedef boost::optional< Tree::Intersection_and_primitive_id<Segment>::Type >
                                                         Segment_intersection;
typedef CGAL::Polyhedron_incremental_builder_3<Polyhedron::HalfedgeDS>
                                                           Polyhedron_builder;
typedef Polyhedron::Vertex_iterator Vertex_iterator;
typedef Polyhedron::Point_iterator Point_iterator;
typedef Polyhedron::Facet_iterator Facet_iterator;
typedef Polyhedron::Halfedge_around_facet_circulator Halfedge_facet_circulator;
typedef Polyhedron::Halfedge_iterator Halfedge_iterator;
typedef Nef_polyhedron::Vertex_const_iterator Vertex_const_iterator;
typedef Nef_polyhedron::Halfedge_const_iterator Halfedge_const_iterator;
typedef Nef_polyhedron::Halffacet_const_iterator Halffacet_const_iterator;
typedef Nef_polyhedron::Halffacet_cycle_const_iterator
                                                Halffacet_cycle_const_iterator;
typedef Nef_polyhedron::SHalfedge_const_handle SHalfedge_const_handle;
typedef Nef_polyhedron::SHalfedge_around_facet_const_circulator
                                       SHalfedge_around_facet_const_circulator;
typedef CGAL::Side_of_triangle_mesh<Polyhedron, Kernel> Point_inside;
typedef CGAL::Projection_traits_xy_3<Kernel> Projection;
typedef CGAL::Polygon_2<Kernel> Polygon_2;
typedef CGAL::Polygon_with_holes_2<Kernel> Polygon_with_holes_2;
typedef Polygon_with_holes_2::Hole_const_iterator Hole_const_iterator;
typedef Polygon_2::Vertex_iterator PG2VI;
typedef Polygon_with_holes_2::Hole_const_iterator PG2H;

using namespace std;

#define PRECISION 15
#define EPSILON Kernel::FT(0.000000001)

namespace pmreg {
std::string timestr (Kernel::FT t);
double parsetime (std::string str);

enum {
    NL_LIST = 1,
    NL_STRING,
    NL_SYM,
    NL_DOUBLE,
    NL_FT,
    NL_BOOL
};

template <typename T> class Range {
        public:
                map<T, T> range;
                void addrange(T a, T b);
                void print();
};

struct seg_compare {
    bool operator() (const Segment2d &l, const Segment2d &r) const {
        Point_2 ls = l.source(), lt = l.target();
        Point_2 rs = r.source(), rt = r.target();
        return ((ls < rs) || ((ls == rs) && (lt < rt)));
    }
};
}

#include "PMRegion.h"

namespace pmreg {
void Arrangement2Region(Arrangement::Face_iterator fi, pmreg::RList& region);
vector<Segment> mpoint2segments (pmreg::RList& obj);
std::set<Kernel::FT> getZEvents (Polyhedron p, Kernel::FT mindiff);
std::set<Kernel::FT> getZEvents (Polyhedron p);
pmreg::RList Polygons2Region (vector<Polygon_with_holes_2> polygons);
Polyhedron nef2polyhedron (Nef_polyhedron& np);
vector<Polygon_with_holes_2> projectxy (PMRegion pm);
Polyhedron extrude (vector<Polygon_with_holes_2> ta, Kernel::FT start, Kernel::FT end);
Kernel::FT area (Polygon_with_holes_2 p);
Kernel::FT area (vector<Polygon_with_holes_2> ps);
Kernel::FT area (RList region);
vector<Polygon_with_holes_2> Region2Polygons (RList region);
void do_deter1(string fname);
}



#endif /* PMREGION_INTERNAL_HXX */
