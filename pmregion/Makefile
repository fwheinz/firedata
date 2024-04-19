CXX=g++
#CXX=clang++
CPPFLAGS=-O2 -fPIC -Wall -ggdb -DCGAL_HAS_THREADS=1 -march=native --std=c++14 -DCGAL_DISABLE_ROUNDING_MATH_CHECK=ON  -Wno-unused-function -DPMREGION
#LIBS=-lCGAL -lCGAL_Core -lboost_thread -lboost_system -lgmp -lmpfr -lpthread
LIBS=-lboost_thread -lboost_system -lgmp -lmpfr -lpthread
LDFLAGS=$(LIBS)
LIBOBJ=PMRegion_operations.o PMRegion_conversions.o PMRegion_helpers.o RList.o PMRegion_coverduration.o

all: libpmregion.a pmregcli firms

PMRegion_internal.h.gch: PMRegion_internal.h PMRegion.h
	$(CXX) -x c++-header $(CPPFLAGS) PMRegion_internal.h -o PMRegion_internal.h.gch

%.o: %.cpp PMRegion_internal.h.gch
	$(CXX) $(CPPFLAGS) -c $< -o $@

firms.o: firms.cpp
	$(CXX) $(CPPFLAGS) -c $< -o $@

libpmregion.a: $(LIBOBJ)
	rm -f $@
	ar -cq $@ $(LIBOBJ)

pmregcli: libpmregion.a pmregcli.o
	$(CXX) -o pmregcli pmregcli.o $(LDFLAGS) libpmregion.a

clean:
	rm  -f *.o *~ *.gch* *.pch* pmregcli firms libpmregion.a

secondo:
	cp PMRegion_* PMRegion.h /root/secondo/Algebras/PMRegion/pmregion

firms: firms.o $(LIBOBJ)
	$(CXX) -o firms firms.o $(LIBOBJ) $(LDFLAGS)

