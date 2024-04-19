/* 
 * This file is part of libpmregion
 * 
 * File:   pmregcli.cpp
 * Author: Florian Heinz <fh@sysv.de>

 1 pmregcli
   CLI program for processing polyhedral moving regions
  
*/

#include <iostream>
#include <sstream>
#include <fstream>
#include <cstdlib>
#include <map>
#include "PMRegion_internal.h"

using namespace std;
using namespace pmreg;

static int iters = 0;

static void usage (void) {
   cerr << "Usage: pmregcli <command> [param_1] [param_2] ... [param_n]"
        << endl
        << "      === PMRegion operations === " << endl
        << "             atinstant     <pmregfile> <instant>" << endl
        << "             perimeter     <pmregfile>" << endl
        << "             area          <pmregfile>" << endl
        << "             traversedarea <pmregfile>" << endl
        << "             mpointinside  <pmregfile> <mpointfile>" << endl
        << "             intersection  <pmregfile> <pmregfile>" << endl
        << "             union         <pmregfile> <pmregfile>" << endl
        << "             difference    <pmregfile> <pmregfile>" << endl
        << "             intersects    <pmregfile> <pmregfile>" << endl
        << "      === Conversion operations === " << endl
        << "             pmreg2mreg    <pmregfile>" << endl
        << "             pmreg2mreg2   <pmregfile>" << endl
        << "             pmreg2off     <pmregfile>" << endl
        << "             off2pmreg     <pmregfile>" << endl
        << "             mreg2pmreg    <mregfile>" << endl
        << "             reg2pmreg     <regfile> <instant1> <instant2> <xoff>" << endl
        << "     === Spatiotemporal coverage analysis ===" << endl
        << "             createcdpoly    <pmreg> [basereg]" << endl
        << "             createccdpoly   <pmreg> [basereg]" << endl
        << "             createicdpoly   <pmreg> <duration/msecs> [basereg]" << endl
        << "             restrictcdpoly  <pmreg> <basereg>" << endl
        << "             coverduration   <cdpoly> <x> <y>" << endl
        << "             maxcovered      <cdpoly> <duration/msecs>" << endl
        << "             mincovered      <ccdpoly> <msecs>" << endl
        << "             intervalcovered <icdpoly> <msecs>" << endl
        << "             avgcover        <cdpoly> [basereg]" << endl
        << "     === Miscellaneous commands ===" << endl
        << "             openscad      <pmregfile>" << endl
        << "             analyze       <pmreg>" << endl
        << "             deter1        <csv>" << endl
        ;
    exit(1);
}

static RList file2rlist(char *fname) {
    ifstream i1(fname, std::fstream::in);
    RList ret = RList::parse(i1);

    return ret;
}

static void atinstant(char **param) {
    RList rl = file2rlist(param[0]);
    double instant = parsetime(param[1]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.atinstant(instant);
    cout << pmreg.atinstant(instant).ToString() << endl;
}

static void atinstant2(char **param) {
    RList rl = file2rlist(param[0]);
    double instant = parsetime(param[1]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.atinstant2(instant);
    cerr << "Calculating atinstant..." << endl;
    cout << pmreg.atinstant2(instant).ToString() << endl;
}

static void createcdpoly(char **param, int argc) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    Nef_polyhedron np(pmreg.polyhedron);
    pmreg.polyhedron = nef2polyhedron(np);
    PMRegion cdpoly;
    do {
	    if (argc == 2) {
		    RList basereg = file2rlist(param[1]);
		    cdpoly = pmreg.createcdpoly(basereg);
	    } else {
		    cdpoly = pmreg.createcdpoly();
	    }
    } while (iters--);

    cout << cdpoly.toRList().ToString() << endl;    
}

static void restrictcdpoly(char **param, int argc) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    PMRegion cdpoly;
    RList basereg = file2rlist(param[1]);
    cdpoly = pmreg.restrictcdpoly(basereg);
    
    cout << cdpoly.toRList().ToString() << endl;    
}

static void createccdpoly(char **param, int argc) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    PMRegion ccdpoly;
    do {
	    if (argc == 2) {
		    RList basereg = file2rlist(param[1]);
		    ccdpoly = pmreg.createccdpoly(basereg);
	    } else {
		    ccdpoly = pmreg.createccdpoly();
	    }
    } while (iters--);
    
    cout << ccdpoly.toRList().ToString() << endl;    
}

static void createicdpoly(char **param, int argc) {
    RList rl = file2rlist(param[0]);
    double duration = atof(param[1]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    PMRegion icdpoly;
    do {
	    if (argc == 3) {
		    RList basereg = file2rlist(param[2]);
		    icdpoly = pmreg.createicdpoly(duration, basereg);
	    } else {
		    icdpoly = pmreg.createicdpoly(duration);
	    }
    } while (iters--);
    
    cout << icdpoly.toRList().ToString() << endl;    
}

static void coverduration (char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion cdpoly = PMRegion::fromRList(rl);
    double x = atof(param[1]);
    double y = atof(param[2]);
    while (iters--)
	    cdpoly.coverduration(x, y);
    cout << cdpoly.coverduration(x, y) << endl;
}

static void maxcovered (char **param) {
    RList rl = file2rlist(param[0]);
    double duration = atof(param[1]);
    PMRegion cdpoly = PMRegion::fromRList(rl);
    while (iters--)
	    cdpoly.maxcovered(duration);
    cout << cdpoly.maxcovered(duration).ToString() << endl;
}

static void mincovered (char **param) {
    RList rl = file2rlist(param[0]);
    double duration = atof(param[1]);
    PMRegion cdpoly = PMRegion::fromRList(rl);
    while (iters--)
	    cdpoly.mincovered(duration);
    cout << cdpoly.mincovered(duration).ToString() << endl;
}

static void intervalcovered (char **param) {
    RList rl = file2rlist(param[0]);
    double duration = atof(param[1]);
    PMRegion cdpoly = PMRegion::fromRList(rl);
    while (iters--)
	    cdpoly.intervalcovered(duration);
    cout << cdpoly.intervalcovered(duration).ToString() << endl;
}

static void avgcover (char **param, int argc) {
    RList rl = file2rlist(param[0]);
    PMRegion cdpoly = PMRegion::fromRList(rl);
    if (argc == 1) {
        char buf[100];
        sprintf(buf, "%.20f", CGAL::to_double(cdpoly.avgcover()));

        cout << buf << endl;
    } else {
        RList baseregion = file2rlist(param[1]);
        char buf[100];
        sprintf(buf, "%.20f", CGAL::to_double(cdpoly.avgcover(baseregion)));

        cout << buf << endl;
    }
}

static void minmaxz(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    pair<Kernel::FT, Kernel::FT> minmax = pmreg.minmaxz();
    cout << ::CGAL::to_double(minmax.first) << " " << ::CGAL::to_double(minmax.second) << endl;
}


static void perimeter(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.perimeter();
    cout << pmreg.perimeter().rl.ToString() << endl;
}

static void area(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.area();
    cout << pmreg.area().rl.ToString() << endl;
}

static void area2(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.area2();
    cout << pmreg.area2().rl.ToString() << endl;
}

static void pmreg2mreg(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.toMRegion();
    cout << pmreg.toMRegion().ToString() << endl;
}

static void pmreg2mreg2(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.toMRegion();
    cout << pmreg.toMRegion2().ToString() << endl;
}

static void pmreg2off(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    while (iters--)
        pmreg.toOFF();
    cout << pmreg.toOFF() << endl;
}

static void off2pmreg(char **param) {
    Polyhedron p;
    ifstream i(param[0], ifstream::in);
    i >> p;
    PMRegion pmreg(p);
    cout << pmreg.toRList().ToString() << endl;
}

static void off2mreg(char **param) {
    Polyhedron p;
    ifstream i(param[0], ifstream::in);
    i >> p;
    PMRegion pmreg(p);
    cout << pmreg.toMRegion().ToString() << endl;
}

static void mreg2pmreg(char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromMRegion(rl);
    while (iters--)
        pmreg.toRList();
    cout << pmreg.toRList().ToString() << endl;
}

static void analyze (char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    pmreg.analyze();
}

static void openscad (char **param) {
    RList rl = file2rlist(param[0]);
    PMRegion pmreg = PMRegion::fromRList(rl);
    pmreg.openscad(param[0]);
    PMRegion cd = pmreg.createcdpoly();
    cd.openscad(((string)param[0])+"cd");
}

static void reg2pmreg(char **param) {
    RList rl = file2rlist(param[0]);
    double inst1 = parsetime(param[1]);
    double inst2 = parsetime(param[2]);
    double xoff = atof(param[3]);
    PMRegion pmreg = PMRegion::fromRegion(rl, inst1, inst2, xoff);
    cout << pmreg.toRList().ToString() << endl;
}

static void intersects (char **param) {
    RList reg1 = file2rlist(param[0]);
    RList reg2 = file2rlist(param[1]);
    PMRegion pmreg1 = PMRegion::fromRList(reg1);
    PMRegion pmreg2 = PMRegion::fromRList(reg2);
    while (iters--)
        pmreg1.intersects(pmreg2);
    cout << pmreg1.intersects(pmreg2).rl.ToString() << endl;
}

static void intersection (char **param) {
    RList reg1 = file2rlist(param[0]);
    RList reg2 = file2rlist(param[1]);

    PMRegion pmreg1 = PMRegion::fromRList(reg1);
    PMRegion pmreg2 = PMRegion::fromRList(reg2);

    while (iters--)
        pmreg1 * pmreg2;
    PMRegion is = pmreg1 * pmreg2;
    cout << is.toRList().ToString() << endl;
}

static void do_union (char **param) {
    RList reg1 = file2rlist(param[0]);
    RList reg2 = file2rlist(param[1]);

    PMRegion pmreg1 = PMRegion::fromRList(reg1);
    PMRegion pmreg2 = PMRegion::fromRList(reg2);

    while (iters--)
        pmreg1 + pmreg2;
    PMRegion is = pmreg1 + pmreg2;
    cout << is.toRList().ToString() << endl;
}

static void mpointinside (char **param) {
    RList reg = file2rlist(param[0]);
    RList mpoint = file2rlist(param[1]);

    PMRegion pmreg = PMRegion::fromRList(reg);
    while (iters--)
        pmreg.mpointinside(mpoint);
    MBool mbool = pmreg.mpointinside(mpoint);
    cout << mbool.rl.ToString() << endl;
}

static void difference (char **param) {
    RList reg1 = file2rlist(param[0]);
    RList reg2 = file2rlist(param[1]);

    PMRegion pmreg1 = PMRegion::fromRList(reg1);
    PMRegion pmreg2 = PMRegion::fromRList(reg2);

    while (iters--)
        pmreg1 - pmreg2;
    PMRegion is = pmreg1 - pmreg2;
    cout << is.toRList().ToString() << endl;
}

static void traversedarea (char **param) {
    RList reg = file2rlist(param[0]);

    PMRegion pmreg = PMRegion::fromRList(reg);

    while (iters--)
        pmreg.traversedarea();
    cout << pmreg.traversedarea().ToString() << endl;
}

static void deter1 (char **param) {
//    do_deter1(param[0]);
}

// Program entry point
int main (int argc, char **argv) {
    if (argc < 2)
        usage();

    // For benchmarking
    if (getenv("ITERS")) {
	    iters = atoi(getenv("ITERS"));
	    cerr << "Performing " << iters << " iterations" << endl;
    }

    char *cmd = argv[1];
    int nrparam = argc - 2;
    char **param = argv+2;

    if (!strcmp(cmd, "pmreg2mreg") && nrparam == 1) {
        pmreg2mreg(param);
    } else if (!strcmp(cmd, "pmreg2mreg2") && nrparam == 1) {
        pmreg2mreg2(param);
    } else if (!strcmp(cmd, "pmreg2off") && nrparam == 1) {
        pmreg2off(param);
    } else if (!strcmp(cmd, "off2pmreg") && nrparam == 1) {
        off2pmreg(param);
    } else if (!strcmp(cmd, "off2mreg") && nrparam == 1) {
        off2mreg(param);
    } else if (!strcmp(cmd, "atinstant") && nrparam == 2) {
        atinstant(param);
    } else if (!strcmp(cmd, "atinstant2") && nrparam == 2) {
        atinstant2(param);
    } else if (!strcmp(cmd, "openscad") && nrparam == 1) {
        openscad(param);
    } else if (!strcmp(cmd, "minmaxz") && nrparam == 1) {
        minmaxz(param);
    } else if (!strcmp(cmd, "perimeter") && nrparam == 1) {
        perimeter(param);
    } else if (!strcmp(cmd, "area") && nrparam == 1) {
        area(param);
    } else if (!strcmp(cmd, "area2") && nrparam == 1) {
        area2(param);
    } else if (!strcmp(cmd, "traversedarea") && nrparam == 1) {
        traversedarea(param);
    } else if (!strcmp(cmd, "intersects") && nrparam == 2) {
        intersects(param);
    } else if (!strcmp(cmd, "mreg2pmreg") && nrparam == 1) {
        mreg2pmreg(param);
    } else if (!strcmp(cmd, "reg2pmreg") && nrparam == 4) {
        reg2pmreg(param);
    } else if (!strcmp(cmd, "intersection") && nrparam == 2) {
        intersection(param);
    } else if (!strcmp(cmd, "union") && nrparam == 2) {
        do_union(param);
    } else if (!strcmp(cmd, "mpointinside") && nrparam == 2) {
        mpointinside(param);
    } else if (!strcmp(cmd, "difference") && nrparam == 2) {
        difference(param);
    } else if (!strcmp(cmd, "analyze") && nrparam == 1) {
        analyze(param);
    } else if (!strcmp(cmd, "createcdpoly") && (nrparam == 1 || nrparam == 2)) {
        createcdpoly(param, nrparam);
    } else if (!strcmp(cmd, "restrictcdpoly") && nrparam == 2) {
        restrictcdpoly(param, nrparam);
    } else if (!strcmp(cmd, "createccdpoly") && (nrparam == 1 || nrparam == 2)) {
        createccdpoly(param, nrparam);
    } else if (!strcmp(cmd, "createicdpoly") && (nrparam == 2 || nrparam == 3)) {
        createicdpoly(param, nrparam);
    } else if (!strcmp(cmd, "coverduration") && nrparam == 3) {
        coverduration(param);
    } else if (!strcmp(cmd, "maxcovered") && nrparam == 2) {
        maxcovered(param);
    } else if (!strcmp(cmd, "mincovered") && nrparam == 2) {
        mincovered(param);
    } else if (!strcmp(cmd, "intervalcovered") && nrparam == 2) {
        intervalcovered(param);
    } else if (!strcmp(cmd, "avgcover") && (nrparam == 1 || nrparam == 2)) {
        avgcover(param, nrparam);
    } else if (!strcmp(cmd, "deter1") && nrparam == 1 ) {
        deter1(param);
    } else {
        usage();
    }

    exit(0);
}

