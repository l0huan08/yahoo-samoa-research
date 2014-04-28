/opt/samoa/bin/samoa local target/SAMOA-Local-0.0.1-SNAPSHOT.jar "PrequentialEvaluation \
 -d /home/hl/lab/app2/result/lab2/resultLocalBagP4m -l \
 (com.yahoo.labs.samoa.learners.classifiers.ensemble.Bagging \
   -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes) ) \
 -s (ArffFileStream -f /home/hl/data/m.arff) -f 1000 -i 5000"
 
