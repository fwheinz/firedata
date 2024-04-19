Creating Moving Regions from Satellite Scan Data
================================================

Author: Florian Heinz <florian.heinz@oth-regensburg.de>
Version: 3.1 (2024-04-19)
Copyright: GNU General Public License Version 3 (a copy can be found in GPL-3.0)

Prerequisites:
g++ or clang++ compiler
libCGAL (>= 5.0)
boost

Compile with: make
Result:
libpmregion.a - Library
firms - Convert FIRMS fire data to moving regions

Command line programs:
======================

firms - Create moving regions from satellite data
=================================================

The program "firms" takes a CSV file in the following format:

latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,confidence,version,bright_ti5,frp,daynight

for example:

-7.48285,-46.72977,302.8,0.47,0.4,2019-08-21,04:12,N,nominal,1.0NRT,291.1,1.3,N

and converts it into a moving region. The following parameters are available:

-r <minlat>x<minlon>,<maxlat>x<maxlon> - Only consider points inside the given
                                         coordinates
-s <scale>    - Scale factor of a single fire, default is 1.1
-i <scale>    - Input Scale factor, default 1
-d <duration> - Duration of a fire in ms, default is 86400000 (1 day)
-n <nrpoints> - Only parse this number of points, default: all
-p <pattern>  - Shape of events; 1: box, 2: diamond, 3: arrow (default: diamond)
-o <filename> - base filename
-t <threads>  - number of threads for calculations, default: number of cores
-f <format>   - ps / ms / p / m / off ; pmregion/mregion [stream] or "off"-format
-v            - Increase verbosity (up to 3 times)



Example from Demo paper "Creating Moving Regions from Satellite Scan Data":
===========================================================================

South America forest fires August 2019

./firms -o southamerica VIIRS_southamerica.csv

This creates a polyhedral moving region file "southamerica-s1.100000-p2.pmreg", that
can for instance be viewed in the viewer:

./viewer region_southamerica southamerica-s1.100000-p2.pmreg




Australia bush fires January 2020

./firms -o australia VIIRS_australia.csv

This creates a polyhedral moving region file "australia-s1.100000-p2.pmreg", that
can for instance be viewed in the viewer:

./viewer region_australia australia-s1.100000-p2.pmreg


