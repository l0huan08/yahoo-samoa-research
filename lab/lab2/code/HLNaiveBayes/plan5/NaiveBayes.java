/**
 * 
 */
package com.yahoo.labs.samoa.learners.classifiers.hl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javacliparser.*;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.learners.Learner;
import com.yahoo.labs.samoa.topology.Stream;
import com.yahoo.labs.samoa.topology.TopologyBuilder;

/**
 * @author hl
 * NaiveBayes Classifier for SAMOA
 * Plan 4
 * 
 * 
 * Only support int numeric attributes.
 * 2014.3.5
 */
public class NaiveBayes implements Learner, Configurable{
	
	/*
	 * NB Topology:
	 * 
	 * 
	 * inputStream-->[NBDistributor:1]--trainStream(shuffle)->[NBStatisticP:p]-statisticStream(all)->[NBModelP:1]-->resultStream
	 * 		             |                                                                                   ^
	 *                   |----------testStream(shuffle)------------------------------------------------------|			
	 */

	/**
	 * 
	 */
	private static final long serialVersionUID = -2927602391151792705L;
	
	private static Logger logger = LoggerFactory.getLogger(NaiveBayes.class);

	/**
	 * Parallel Level , it is the number of this Classifiers
	 * also the number of NBDistributorProcessor
	 */
	private int parallelism; // should be 1
	
	/**
	 * store the NaiveBayes model and do prediction for test data
	 * also aggregate the local statistic information from statisticP
	 */
	private NBModelProcessor modelP;
	private Stream resultStream; //the data of prediction result
	
	public IntOption nBinOption = new IntOption(
            "nBin",
            'b',
            "bin number for numeric attributes",
            10, 1, Integer.MAX_VALUE);

	@Override
	public void init(TopologyBuilder builder, Instances dataset, int parallelism) {
		logger.info("================================================");
		logger.info("Begin init NaiveBayes Classifier topology.");
		
		this.parallelism = parallelism;
		
		// Create Processors
		this.modelP = new NBModelProcessor.Builder().dataset(dataset).nBin(nBinOption.getValue()).build();
		
		builder.addProcessor(this.modelP,1);
        
		logger.debug("NBModelProcessor added.");
		
		// Create Streams
		this.resultStream = builder.createStream(this.modelP);
		this.modelP.setResultStream(this.resultStream);
		
		logger.info("Sucessfully initializing NaiveBayes classifier topology.");
		
		builder.build();
	}
	
	@Override
	public Processor getInputProcessor() {
		return this.modelP;
	}

	@Override
	public Stream getResultStream() {
		return this.resultStream;
	}

}
