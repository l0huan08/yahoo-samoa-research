# Start ZooKeeper
/bin/openvt -c 1 -s -w -- $SAMOA_HOME/scripts/startZookeeper.sh

# Create Cluster
/bin/openvt -c 2 -s -w -- $SAMOA_HOME/scripts/createCluster1node.sh

# Start a node
/bin/openvt -c 3 -s -w -- $SAMOA_HOME/scripts/startNode.sh

# Run SAMOA task
/bin/openvt -c 4 -s -w -- $SAMOA_HOME/scripts/prequentialEval.sh
