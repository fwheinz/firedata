all: view-australia

clean:
	make -C pmregion clean
	make -C viewer clean
	rm -f data/*.pmreg

firms: build
	echo Building firms tool
	make -C pmregion

viewer: build
	echo Building viewer
	make -C viewer

view-southamerica: viewer data/southamerica-s1.100000-p2.pmreg
	viewer/viewer data/southamerica.region data/southamerica-s1.100000-p2.pmreg

data/southamerica-s1.100000-p2.pmreg: 
	pmregion/firms -o data/australia data/VIIRS_Southamerica_Aug_2019.csv

view-australia: viewer data/australia-s1.100000-p2.pmreg
	viewer/viewer data/australia.region data/australia-s1.100000-p2.pmreg

data/australia-s1.100000-p2.pmreg:
	pmregion/firms -o data/australia data/VIIRS_Australia_Jan_2020.csv

.PHONY: build
