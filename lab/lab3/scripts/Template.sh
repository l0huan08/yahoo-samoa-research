# Create Cluster
cd $S4_HOME
s4 zkServer -clean
s4 newCluster -c=cluster -flp=12000 -nbTasks=1 -zkServer=hlnode3
s4 node -c=cluster -zkServer=hlnode3

# Run SAMOA task
cd $SAMOA_HOME
bin/samoa S4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar "PrequentialEvaluation \
-d /tmp/dump.csv -i 1000000 -f 10000 \
-l (com.yahoo.labs.samoa.learners.classifiers.trees.VerticalHoeffdingTree -p 4) \
-s (com.yahoo.labs.samoa.moa.streams.generators.RandomTreeGenerator -c 2 -o 10 -u 10)"
