/opt/samoa/bin/samoa s4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar  "PrequentialEvaluation \
 -d /home/hl/lab/app2/result/lab2/resultS43BagVHTn -l \
 (com.yahoo.labs.samoa.learners.classifiers.ensemble.Bagging \
   -l (com.yahoo.labs.samoa.learners.classifiers.trees.VerticalHoeffdingTree -p 6) ) \
 -s (com.yahoo.labs.samoa.streams.hl.ArffFileStream -f '/home/hl/data/n.url') -f 10000 -i 51000"
 
