/opt/samoa/bin/samoa s4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar "PrequentialEvaluation \
 -d /home/hl/lab/app2/result/lab2/resultS43BagP5k -l \
 (com.yahoo.labs.samoa.learners.classifiers.ensemble.Bagging \
   -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes) ) \
 -s (com.yahoo.labs.samoa.streams.hl.ArffFileStream  -f '/home/hl/data/k.url') -f 100000 -i 310000"
 
