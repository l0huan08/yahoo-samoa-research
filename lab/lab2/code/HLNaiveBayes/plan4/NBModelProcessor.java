package com.yahoo.labs.samoa.learners.classifiers.hl;

import java.util.Arrays;

import com.yahoo.labs.samoa.core.ContentEvent;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.topology.Stream;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.learners.InstanceContentEvent;
//import com.yahoo.labs.samoa.moa.core.AutoExpandVector;
//import com.yahoo.labs.samoa.moa.core.DoubleVector;
import com.yahoo.labs.samoa.learners.ResultContentEvent;
import com.yahoo.labs.samoa.moa.core.AutoExpandVector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.google.common.collect.HashBasedTable;
//import com.google.common.collect.Table;

import org.ejml.simple.*;

//Plan 4 
// single NBModel that test and train instances
// resultStream 
public class NBModelProcessor implements Processor{
	private static final long serialVersionUID = 5861167167021535214L;
	private static Logger logger = LoggerFactory.getLogger(NBModelProcessor.class);
	
	private Stream resultStream;
	
	private int processorId;
	
	//required parameters
	private final Instances dataset; //only use in builder
	private InstancesHeader header; //atribute infomation
	
	private boolean bSupportData=false;
	
	/**
	 * Assume there are total K classes, A attribute, each attributes has V(A) values
	 * number of occurrence of (class,attributeIndex,a) 
	 * number of (k, a, v)
	 */
	//private AutoExpandVector<AutoExpandVector<DoubleVector>> nClassAttValue=new AutoExpandVector<AutoExpandVector<DoubleVector>>();
	//private int[][][] nClassAttValue;	
	private AutoExpandVector<SimpleMatrix> nClassAttValue;
	
	/**
	 * number of occurrence of (class)
	 * number of (k)
	 */
	//private DoubleVector nClass=new DoubleVector();
	//private int[] nClass;
	private AutoExpandVector<Double> nClass;
	
	/**
	 * number of distinct values for each attribute
	 */
	//private DoubleVector nValuesForAtt = new DoubleVector();
	private int[] nValuesForAtt;	
	
	/**
	 * Store local statistics corresponding to each local NBStatisticProcessor
	 */
	//ConcurrentHashMap<String, NBStatisticContentEvent> localStatistics=new ConcurrentHashMap<String, NBStatisticContentEvent>();
	private int m_K; //numClasses
	private int m_A; //numAttributes
	private int m_V; //numValues for each attributes
	
	
	public void setResultStream(Stream resultStream)
	{
		this.resultStream = resultStream;
	}
	
	//private constructor based on Builder pattern
	private NBModelProcessor(Builder builder){
			this.dataset = builder.dataset;
			this.header = new InstancesHeader(dataset);			
	}
	
	@Override
	public Processor newProcessor(Processor p) {
		NBModelProcessor pp = (NBModelProcessor)p;
		NBModelProcessor newP = new NBModelProcessor.Builder(pp).build();
		newP.resultStream = pp.resultStream;
		
		return newP;
	}

	@Override
	public void onCreate(int id) {
		this.processorId=id;
		logger.info("NBModelProcessor created, id = {}",id);
		
		
		int K = this.header.numClasses();//only support nominal class attribute
		if (K==0)
		{
			logger.info("Error: Only support nominal class attribute. END.");
			bSupportData = false;
			return;
		}
		else
		{
			bSupportData=true;
		}
		
		//int K  = findNumClasses();
		
		int A = this.header.numAttributes();
		
		// init the statistics
		nClass= new AutoExpandVector<Double>(K);
		//alloc memory
		for (int i=0;i<K;i++)
		{
			nClass.set(i,0.0);
		}
		
		// find max numValues for all attributes
		nValuesForAtt = new int[A];
		int V = 0;
		for (int i=0;i<A;i++)
		{
			int aV = this.header.attribute(i).numValues();//if a is numeric attribute, always get 0
			if (aV==0)//means the attribute is numeric attribute, so we assume there is at least 1  value
				aV=1;
			
			nValuesForAtt[i]=aV; //numValues for attribute i
			if (aV>V)
			{
				V=aV;
			}
		}
		V = Math.max(1, V);// V at least be 1
				
		//nClassAttValue = new int[K][A][maxV];
		nClassAttValue = new AutoExpandVector<SimpleMatrix>(K);
		for (int i=0;i<K;i++)
		{
			SimpleMatrix mat = new SimpleMatrix(A,V);
			nClassAttValue.set(i, mat);
		}
		
		this.m_K=K;
		this.m_A=A;
		this.m_V=V;
		
		logger.info("K={},A={},V={}",K,A,m_V);
	}

	@Override
	public boolean process(ContentEvent event) {
		if (bSupportData==false)
		{
			return false;
		}
		
		//logger.debug("Begin process event.");
		if (event==null)
		{
			logger.info("event is null.");
			return false;
		}
		
		if (event.isLastEvent())
		{
			logger.info("Processing last event.");
			logger.info("=== output Final Matrix====");
			
			logger.info("K={},A={},V={}",this.m_K,this.m_A,this.m_V);
			logger.info("nClass={}",this.nClass);
			logger.info("nValuesForAtt",Arrays.toString(this.nValuesForAtt));
			for (int i=0;i<this.m_K;i++)
				logger.info("nClassAttValue[{}]={}",i,this.nClassAttValue.get(i));
			
		}
		
		if (event instanceof InstanceContentEvent)
		{
			//logger.debug("Processing InstanceContentEvent.");
			InstanceContentEvent ice = (InstanceContentEvent)event;

			boolean suc= false;
			
			//[debug]
			//debugShowInstance(ice);
			
			
			//[!]
			//logger.debug("event is testing={}?is train={}",ice.isTesting(),ice.isTraining());
			ResultContentEvent rce =null;
			if (ice.isTesting())
			{
				//predict instance from inputStream
				//logger.debug("Predict instance.");
				rce = predict2(ice);
			}
			
			if (ice.isTraining())
			{
				//train instance from inputStream
				//logger.debug("Train instance.");
				boolean sucTrain = train(ice);
				suc = suc && sucTrain;
			}
			
			if (rce!=null)
			{
				//[!]
				//logger.debug("sending rce");
				//send the result stream
				this.resultStream.put(rce);
			}
			return suc;
		}
		
		return false;
	}

	private void debugShowInstance(InstanceContentEvent ice) {
		Instance it = ice.getInstance();
		String itValues = new String();
		for (int i=0;i<this.m_A;i++)
		{
			if (i==it.classIndex())
			{
				itValues += ", "+String.valueOf(it.classValue());
			}
			else
			{
				itValues += ", "+String.valueOf(it.value(i));
			}
		}
		logger.debug("Read Instance: "+itValues);
	}


	/**
	 * Predict test instance
	 * @param event
	 * @return true if successful predicted
	 */
	private ResultContentEvent predict2(InstanceContentEvent event)
	{
		//[!]
		//logger.debug("begin predict instance.");
		if (event==null)
			return null;
		Instance inst = event.getInstance();
		if (inst==null)
		{
			logger.warn("event has null instance.");
			return null;
		}
				
		double[] votes = getPredictionVotesForInstance(inst);
		// if votes==null,
		// the ResultContentEvent created with null votes will cause
		// evaluator crash!
		if (votes==null)
		{
			logger.warn("error: votes is null, cannot decide the class of instance.");
			return null;
		}
		
		// Avoid array boundary overflow in evaluation, resize votes!
		if ( (int)inst.classValue() >= votes.length)
		{
			votes = Arrays.copyOf(votes, (int)inst.classValue()+1 );
		}
		ResultContentEvent rce =  newResultContentEvent(votes,event);
		
		//[!]
		//logger.debug("true Class={},votes={}",event.getClassId(),Arrays.toString(votes));
		//logger.debug("end predict instance.");

		return rce;
		
	}
	
	/**
	 * Helper method to generate new ResultContentEvent based on an instance and
	 * its prediction result.
	 * @param prediction The predicted class label from the decision tree model.
	 * @param inEvent The associated instance content event
	 * @return ResultContentEvent to be sent into Evaluator PI or other destination PI.
	 */
	private ResultContentEvent newResultContentEvent(double[] votes, InstanceContentEvent inEvent)
	{
		ResultContentEvent rce = new ResultContentEvent(inEvent.getInstanceIndex(), inEvent.getInstance(), inEvent.getClassId(), votes, inEvent.isLastEvent());
		rce.setClassifierIndex(this.processorId);
		rce.setEvaluationIndex(inEvent.getEvaluationIndex());
		return rce;
	}
	
	/**
	 * Helper method to get the prediction result. The actual prediction result
	 * is delegated to the leaf node.
	 * 
	 * @param inst
	 * @return
	 */
	private double[] getPredictionVotesForInstance(Instance inst) {
		double[] votes = null;
		
		int K = this.m_K; //number of classes
		if (K==0)
		{
			logger.info("classes number of model is 0");
			return null;
		}
		//int[][] nAttValue = this.nClassAttValue[0];
		
		int A =this.m_A; //num of attributes
		if (A==0)
		{
			logger.info("attributes number of model is 0");
			return null;
		}
		
		if (inst.numAttributes() != A)
		{
			logger.info("numAttributes of instance not equal to numAttributes of model.");
			return null;
		}
		
		votes = new double[K];
		int aClass = inst.classIndex(); //the indx of "class" attribute
		
		for (int k=0;k<K;k++)
		{
			double p=1.0;
			double nk = this.nClass.get(k);
			
			//[!]
			//logger.info("calc vote for class k, k={}",k);
			SimpleMatrix attValue = this.nClassAttValue.get(k);
			if (attValue==null)
			{
				logger.info("error: nClassAttValue{k} is null",k);
				return null;
			}
			for (int a=0;a<A;a++)
			{
		      	if (a==aClass)
					continue; //do not count class attribute
				int v = (int)inst.value(a); //value of this attribute for this instance
				
				if (v >= attValue.numCols() )
				{
					//meet new value which is bigger than attValue current size
					//need to enlarge all the matrix
					// enlarge every the matrix in nClassAttValue
					assert(this.m_V<v+1);
					enlargeNumValueOfAttribute(k,a,v); //new mV
					attValue = this.nClassAttValue.get(k); //get new enlarged attValue
					//logger.info("error: value {} of test data at attribute {} is bigger than nClassAttValue[{}][{}]",v,a,k,a);
					//return null;
				}
				//p *= this.nClassAttValue.get(k).get(a).getValue(v) / nk; //p*= N(a,v,k)/Nk
				int J = (int)this.nValuesForAtt[a];//|Fa|, number of distinct value of attribute a
				p *= smoothProbability(attValue.get(a,v), nk, J); //p*= N(a,v,k)/Nk
			}
			p *= nk;
			votes[k]=p;
		}
		
		return votes;
	}
	
	/**
	 * Smooth the probability of P(ai | k)
	 * to avoid "zero counts problem": P(a|k) = Navk/Nk when Nk=0, or Navk=0
	 * This effect may be too harsh for some classification tasks (for instance, text classification)
	 * @param Navk  Number of instances X which has value v at attribute a and class k
	 * @param Nk  Number of instances X which belongs to class k
	 * @param J   number of distinct value of attribute a
	 * @return
	 */
	private double smoothProbability( double Navk, double Nk, double J)
	{
		//the unsmoothed P should be Navk/Nk
		//smoothed  P = (Navk+l)/(Nk+l*J)
		// J = |Fa|, number of distinct value of attribute a
		// l = user define, typically is 1, defines a Laplace smoothing
		final double l=1;
		
		//[d]debug
		//logger.info("navk={},l={},nk={},J={}",Navk,l,Nk,J);
		return (Navk+l) / (Nk+l*J);
	}
	
	
	/**
	 * Train the instance
	 * change the statistic, n[c,a,v]
	 * @param event
	 * @return
	 */
	private boolean train(InstanceContentEvent event)
	{
		//[d]
		//logger.debug("begin train instance.");
		if (event==null)
			return false;
		Instance inst = event.getInstance();
		if (inst==null)
		{
			logger.debug("event has null instance.");
			return false;
		}
		int c = (int)inst.classValue();
		
		double weight = inst.weight();
		//[d]
		//logger.debug("inst weight = {}",inst.weight());
		

		int A = this.m_A;//numAttributes
		int V = this.m_V;
		if (c >= this.nClass.size()) //instance has bigger c(class) than current C in statsitic
		{
			this.nClass.set(c, weight);
			this.m_K = c+1;
			
			//[d]
			//logger.debug("increase numClass={},nClass={}",this.nClass.size(),nClass);
		}
		else
		{
			this.nClass.set(c, this.nClass.get(c)+weight);
			//[d]
			//logger.info("add instance weight of class {} to nClass.New nClass={}",weight,nClass);
		}
		
		SimpleMatrix attValue = this.nClassAttValue.get(c);
		if (attValue==null)
		{
			SimpleMatrix newAttValue = new SimpleMatrix(A,V); //init with 1 possible values
			this.nClassAttValue.set(c, newAttValue);
			attValue = newAttValue;
		}
		
		for (int a=0;a<A;a++)
		{
			if (a==inst.classIndex())
				continue;
			
			int v = (int)inst.value(a);
			
			// value of training instance is bigger than current size of matrix attValue
			if (v >= attValue.numCols() )
			{
								
				// enlarge every the matrix in nClassAttValue
				assert(V<v+1);
				V = enlargeNumValueOfAttribute(c,a,v); //new mV
								
				// get new enlarged attValue
				attValue = this.nClassAttValue.get(c);
				attValue.set(a,v,weight);
			}
			else
			{
				double orgCount = attValue.get(a,v);
				attValue.set(a, v, orgCount+weight);
			}
			
			//[d]
			//logger.debug("nClassAttValue[{},{},{}]={}",c,a,v,nClassAttValue.get(c).get(a, v));
		}
		
		//logger.debug("finish train instance.");
		return true;
	}
	
	
	/**
	 * Enlarge the matrix size of nClassAttValue[c] when the value of new instance is
	 *  bigger than (nCol of) current matrix size.
	 * @param c class index
	 * @param a the attribute index of the bigger value
	 * @param v the (bigger) value of the new instance
	 * @return new nValuesForAtt[a]
	 */
	private int enlargeNumValueOfAttribute(int c, int a, int v)
	{
		int oldV = this.nValuesForAtt[a];
		
		// do not need to enlarge matrix
		if (oldV>v+1)
			return oldV;
		
		// update nValuesForAtt with lager value
		this.nValuesForAtt[a] = Math.max(this.nValuesForAtt[a], v+1);
		this.m_V = Math.max(this.m_V, v+1);
		
		final int V = this.m_V;
		final int A = this.m_A;
		
		SimpleMatrix newAttValue = new SimpleMatrix(A,V);
		SimpleMatrix oldAttValue =  this.nClassAttValue.get(c);
		newAttValue.insertIntoThis(0, 0,  oldAttValue);
		this.nClassAttValue.set(c, newAttValue);		
		
		return this.nValuesForAtt[a];
	}
	
//	/**
//	 * Takes much time, only use in init
//	 * @return K, numClasses of the whole dataset
//	 */
//	private int findNumClasses()
//	{
//		int K  = this.header.numClasses();
//		if (K>0) //only when class attribute is nominal(enumerator) attribute
//			return K;
//		
//		//when K=0, it means the class attribute is numeric attribute
//		int n = this.dataset.numInstances();
//		//loop the whole dataset, find max class value
//		for (int i=0;i<n;i++)
//		{
//			int c = (int)this.dataset.instance(i).classValue();
//			K = Math.max(c, K);
//		}
//		return K+1;
//	}
	
	static class Builder
	{
		//required parameters
		private Instances dataset;
		
		NBModelProcessor build()
		{
			return new NBModelProcessor(this);
		}
		
		public Builder()
		{	
		}
		
		public Builder dataset(Instances val){
			this.dataset = val;
            return this;
		}
		
		Builder(NBModelProcessor oldProcessor){
			this.dataset = oldProcessor.dataset;
		}
	}
}
