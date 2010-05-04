INCLUDES = -I. -I$(JAVA_HOME)\include -I$(JAVA_HOME)\include\win32

SRC_DIR = src\net\sf\cpsolver\ifs\util

all: jprof.dll

jprof.dll: $(SRC_DIR)\jprof.cc
        cl -LD $(INCLUDES) -Tp$(SRC_DIR)\jprof.cc

clean:
	del *.lib
        del *.dll
        del *.ilk
        del *.pdb
        del *.obj
        del *.exp
