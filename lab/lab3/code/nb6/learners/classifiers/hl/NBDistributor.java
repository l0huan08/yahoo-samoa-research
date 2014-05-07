package com.yahoo.labs.samoa.learners.classifiers.hl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.labs.samoa.core.ContentEvent;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.learners.InstanceContentEvent;
import com.yahoo.labs.samoa.learners.InstancesContentEvent;
import com.yahoo.labs.samoa.topology.Stream;

/**
 * The processor to distribute input data to trainStream and testStream
 * to NBAttributeStat's
 * 
 * edit 2014.4.29 always return false in process(), to avoid infinitely recreating 
 *   the processor.
 *   
 *   2014.5.5  use concurrent queue instead of block queue to avoid dead-lock
 * @author hl
 *
 */
public class NBDistributor implements Processor {

	/**
	 * 2014.4.16
	 */
	private static final long serialVersionUID = -419416090354513304L;
	
	private static Logger logger = LoggerFactory.getLogger(NBDistributor.class);
	
	private Stream trainStream;
	private Stream testStream;
	//private Stream aggControlStream;
	//private Stream aggResponseStream;
	
	// add by hl 2014.4.17
	//send multiple instances together to utilize more of the network
	private java.util.concurrent.ConcurrentLinkedQueue<Instance> trainBuffer;
	private int trainBufferSize;
	private int nWaitingTrainInstance;
	
	//it is hard to batch test with this NB-6 topology, so i still process the {{ 
	// testing instances one by one 
	private final int TestBufferRatio=2;
	private java.util.concurrent.ConcurrentLinkedQueue<Instance> testBuffer;
	private int testBufferSize;
	private int nWaitingTestInstance;
	//}} del by hl 2014.4.18 
	
	//the whole NB-6 is processing testing data
	// in this duration, the incoming test data cannot send immediately but
	// wait until the testing process finished.
	// The 'finishedTesting' signal is sent from NBAggregator through aggResponseStream
	private boolean isTesting=false; 
	
	private int p1=1;
		
	//private constructor based on Builder pattern
	private NBDistributor(Builder builder){	
		// pass parameters
		this.p1=builder.p1;
		this.trainBufferSize = builder.batchSize;
		this.testBufferSize = builder.batchSize*TestBufferRatio;
	}
		
	@Override
	public boolean process(ContentEvent event) {
		// only for debug |!| {{
//				if (event.isLastEvent())
//				{
//					logger.info("NBDistributor: last event processed.");
//				}
				//}} only for debug
		
		if (event instanceof InstanceContentEvent)
		{
			InstanceContentEvent ice = (InstanceContentEvent)event;
			if (ice.isTraining())
			{
				// -------- Training phase -----------
				//NBDistributor get the training instance, trainInst.
				// Get trainInst.attribute[i], Xi, and the trainInst.class,Cj
				// send this instance to NBAttStat through trainStream
				
				//logger.info("send train instance");//|!|debug
				
				// modify by hl 2014.4.17 send multiple instances together to utilize more of the network {{
				//this.trainStream.put(ice);
				
				
				// modify by hl 2014.5.5 use concurrent queue to avoid dead-lock{{
//				try {
//					this.trainBuffer.put( ice.getInstance() );
//					this.nWaitingTrainInstance++;
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
				//  ----------- new code ------------
				this.trainBuffer.offer(ice.getInstance());
				this.nWaitingTrainInstance++;
				// }}  2014.5.5
				
				
				
				// sending the instances in batch
				if (this.nWaitingTrainInstance>=this.trainBufferSize || ice.isLastEvent() )
				{
					//send instances
					InstancesContentEvent outputEvent = new InstancesContentEvent(ice);
					Instance inst=null;
					int nInstSended = 0;
					while ( ((inst=this.trainBuffer.poll())!=null) 
							&& nInstSended<this.trainBufferSize )
					{
						outputEvent.add(inst);
						nInstSended++;
					}
					if (ice.isLastEvent())
						outputEvent.setLast(true);

					this.nWaitingTrainInstance=0;
					
					this.trainStream.put(outputEvent);
					
//					for (int i=0;i<this.p1;i++)
//					{
//						outputEvent.setClassifierIndex(i);
//						this.trainStream.put(outputEvent);
//					}
				}
				
				// }}				
			}
			
			else if (ice.isTesting())
			{
				// use batch processing 
				
				// modify by hl 2014.5.5 use concurrent queue to avoid dead-lock{{
//				// --- old code --
//				try {
//					this.testBuffer.put( ice.getInstance() );
//					this.nWaitingTestInstance++;
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
				// -- new code --
				this.testBuffer.offer( ice.getInstance() );
				this.nWaitingTestInstance++;
				//}} hl 2014.5.5
				
				// sending the instances in batch
				if (this.nWaitingTestInstance>=this.testBufferSize || ice.isLastEvent() )
				{
					//send instances
					InstancesContentEvent outputEvent = new InstancesContentEvent(ice);
					Instance inst=null;
					int nInstSended = 0;
					while ( ((inst=this.testBuffer.poll())!=null) 
							&& nInstSended<this.testBufferSize )
					{
						outputEvent.add(inst);
						nInstSended++;
					}
					if (ice.isLastEvent())
						outputEvent.setLast(true);

					this.nWaitingTestInstance=0;
					this.testStream.put(outputEvent);
				}
			}

			//otherwise ignore it.
			
			// modify by hl 2014.4.29 should always return false to avoid infinitely recreating the processors {{
			//return true;
			return false;
			// }} modify by hl 2014.4.29 should always return false to avoid infinitely recreating the processors 
		}
		
		return false;
	}

	@Override
	public void onCreate(int id) {
		logger.info("NBDistributor created, id = {}",id);
		//this.trainBuffer = new java.util.concurrent.LinkedBlockingQueue<Instance>(this.trainBufferSize);
		//this.testBuffer = new  java.util.concurrent.LinkedBlockingQueue<Instance>(this.testBufferSize);
		this.trainBuffer = new java.util.concurrent.ConcurrentLinkedQueue<Instance>();
		this.testBuffer = new  java.util.concurrent.ConcurrentLinkedQueue<Instance>();
		
		
		
		this.nWaitingTrainInstance = 0;
		this.nWaitingTestInstance = 0;
		this.isTesting =false;
	}

	@Override
	public Processor newProcessor(Processor p) {
		NBDistributor pp = (NBDistributor)p;
		NBDistributor newP = new NBDistributor.Builder(pp)
				.batchSize(pp.trainBufferSize)
				//.testBufferSize(pp.testBufferSize)
				.build();
		
		newP.trainStream = pp.trainStream;
		newP.testStream = pp.testStream;
		//newP.aggControlStream = pp.aggControlStream;
		//newP.aggResponseStream = pp.aggResponseStream;
		return newP;
	}

	public void setTrainStream(Stream trainStream) {
		this.trainStream = trainStream;
	}

	public void setTestStream(Stream testStream) {
		this.testStream = testStream;
	}

//	public void setAggControlStream(Stream aggControlStream) {
//		this.aggControlStream= aggControlStream;
//		
//	}
//
//	public void setAggResponseStream(Stream aggResponseStream) {
//		this.aggResponseStream=aggResponseStream;
//	}
	


	static class Builder
	{
		private int batchSize=200;
		//private int testBufferSize=200;
		private int p1;
		
		public Builder()
		{}
		
		public Builder(NBDistributor p)
		{
			this.batchSize=p.trainBufferSize;
			this.p1=p.p1;
		}
		
		public Builder batchSize(int val){
			this.batchSize = val;
	        return this;
		}
		
//		public Builder testBufferSize(int val){
//			this.testBufferSize = val;
//	        return this;
//		}
		
		public Builder p1(int val)
		{
			this.p1=val;
			return this;
		}
		
		NBDistributor build()
		{
			return new NBDistributor(this);
		}
	}
}
