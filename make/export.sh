#!/bin/bash

cd `dirname $0`

rm -rf panini
rm -rf panini.zip

mkdir panini
mkdir panini/lib
mkdir panini/bin
mkdir panini/editors
mkdir panini/examples 

cp -r ../dist panini/lib/dist
cp -r ../licenses panini/licenses
cp export/panc panini/bin/panc
cp export/panc.bat panini/bin/
cp export/panini panini/bin/
cp export/panini.bat panini/bin/
cp export/panp panini/bin/
cp export/panp.bat panini/bin/
cp export/panini_rt.jar panini/lib/
cp export/profile.jar panini/lib/
cp export/profile.properties panini/lib/
cp export/javac panini/lib/dist/bootstrap/bin/
cp export/panini-mode.el panini/editors/
cp export/panini.vim panini/editors/

# Examples - new examples need to be included here to be part of the distribution.
cp ../examples/HelloWorld.java panini/examples/
cp ../examples/SignatureExample.java panini/examples/
cp ../examples/Pi.java panini/examples/
cp ../examples/Histogram.java panini/examples/
cp ../examples/shaks12.txt panini/examples/
cp ../examples/Pipeline.java panini/examples/
cp ../examples/Philosophers.java panini/examples/
cp ../examples/EchoClient.java panini/examples/
cp ../examples/EchoServer.java panini/examples/
 
cp export/README panini/
perl -pe 's/\r\n|\n|\r/\r\n/g' export/README > panini/README.txt

find panini -name '.svn' -exec rm -Rf '{}' ';' 2>/dev/null
zip -r panini.zip panini
