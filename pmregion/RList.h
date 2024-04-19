#ifndef RLIST_HXX
#define RLIST_HXX

#include <istream>
#include <string>
#include <vector>
#include <cassert>

#ifdef CGAL_VERSION_NR
#include "cgal.h"
namespace pmr {
#endif




#ifndef RLIST_TYPES
#define RLIST_TYPES
enum {
    NL_LIST = 1,
    NL_STRING,
    NL_SYM,
    NL_DOUBLE,
    NL_FT,
    NL_BOOL
};
#endif

class RList {
    protected:
        int type;
        std::string str;
        double nr;
        bool boolean;
        std::string ToString(int indent);
#ifdef CGAL_VERSION_NR
        Kernel::FT *ft;
#endif

    public:
        std::vector<RList> items;

        RList();
        ~RList();
        void append(double nr);
        void append(std::string str);
        void appendsym(std::string str);
        void append(bool val);
        void append(RList l);
        void prepend(RList l);
        RList* point(double x, double y);
        void concat(RList l);
        void toFile(std::string filename);
        static RList fromFile(std::string filename);
        int size() { return items.size(); }
        double getNr () {
            assert(type == NL_DOUBLE || type == NL_FT);
        if (type == NL_DOUBLE)
            return nr;
        else
#ifdef CGAL_VERSION_NR
            return ::CGAL::to_double(*ft);
#else
				assert(false);
#endif
        }
        bool getBool () {
            assert(type == NL_BOOL);
            return boolean;
        }
        std::string getString () {
            assert(type == NL_STRING);
            return str;
        }
        std::string getSym () {
            assert(type == NL_SYM);
            return str;
        }
        int getType () {
            return type;
        }
        RList* nest();

        RList obj(std::string name, std::string type);
        static RList parse(std::istream& f);
        static RList* parsep(std::istream& f);
        std::string ToString();
#ifdef CGAL_VERSION_NR
        void append(Kernel::FT nr);
        Kernel::FT getFt () {
            assert(type == NL_FT||type == NL_DOUBLE);
            if (type == NL_FT) {
                return *ft;
            } else {
                return Kernel::FT(nr);
            }
        }
#endif
};

#ifdef CGAL_VERSION_NR
}
#endif

#endif /* RLIST_HXX */
