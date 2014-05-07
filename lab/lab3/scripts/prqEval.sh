cd $SAMOA_HOME
bin/samoa S4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar "PrequentialEvaluation \
-d /tmp/dump.csv -i 100000 -f 10000 \
-l (com.yahoo.labs.samoa.learners.classifiers.trees.VerticalHoeffdingTree -p 4) \
-s (com.yahoo.labs.samoa.moa.streams.generators.RandomTreeGenerator -c 2 -o 10 -u 10)"
