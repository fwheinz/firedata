JFLAGS = -g -cp /usr/share/java/j3dcore.jar:/usr/share/java/j3dutils.jar:/usr/share/java/vecmath.jar:.
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = $(wildcard mrview/*.java)

all: MRView.jar

classes: $(CLASSES:.java=.class)

clean:
	$(RM) mrview/*.class *.jar

copy:
	cp -a ~/MRView/MRView/src/* .

MRView.jar: classes
	jar cfe MRView.jar mrview.MRView mrview/*.class
