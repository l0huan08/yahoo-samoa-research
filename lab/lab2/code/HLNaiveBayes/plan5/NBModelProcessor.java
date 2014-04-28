package com.yahoo.labs.samoa.learners.classifiers.hl;

import java.util.Arrays;

import com.yahoo.labs.samoa.core.ContentEvent;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.topology.Stream;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.learners.InstanceContentEvent;


import com.yahoo.labs.samoa.learners.ResultContentEvent;
import com.yahoo.labs.samoa.moa.core.AutoExpandVector;
import com.yahoo.labs.samoa.moa.core.DoubleVector;

import com.yahoo.labs.samoa.learners.classifiers.hl.attributeclassobservers.AttributeClassObserver;
import com.yahoo.labs.samoa.learners.classifiers.hl.attributeclassobservers.GaussianNumericAttributeClassObserver;
import com.yahoo.labs.samoa.learners.classifiers.hl.attributeclassobservers.NominalAttributeClassObserver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





//Plan 5- to handle real values
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
	private AutoExpandVector<AttributeClassObserver> attributeObservers;
	
	
	/**
	 * number of occurrence of (class)
	 * number of (k)
	 */
	private DoubleVector nClass;
	
	private int m_K; //numClasses
	private int m_A; //numAttributes
	
	private int nBinForNumAtt=10; // bin number for numeric attributes
	
	public void setResultStream(Stream resultStream)
	{
		this.resultStream = resultStream;
	}
	
	//private constructor based on Builder pattern
	private NBModelProcessor(Builder builder){
			this.dataset = builder.dataset;
			this.nBinForNumAtt = builder.bin;
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
		
		int A = this.header.numAttributes();
		
		// init the statistics
		nClass= new DoubleVector();
		
		//alloc memory
		for (int i=0;i<K;i++)
		{
			nClass.setValue(i,0.0);
		}
				
		this.attributeObservers = new AutoExpandVector<AttributeClassObserver>(A);
		for (int i=0;i<A;i++)
		{
			AttributeClassObserver obs=
					this.header.attribute(i).isNominal() ? newNominalClassObserver()
	                        : newNumericClassObserver();
	       this.attributeObservers.set(i, obs);
		}
		
		this.m_K=K;
		this.m_A=A;
		
		logger.info("K={},A={}",K,A);
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
			//logger.debug("event is null.");
			return false;
		}
		
		if (event.isLastEvent())
		{
			logger.info("Processing last event.");
			logger.info("=== output Final Matrix====");
			
			logger.info("K={},A={}",this.m_K,this.m_A);
			logger.info("nClass={}",this.nClass);
			//logger.info("nValuesForAtt",Arrays.toString(this.nValuesForAtt));
			//for (int i=0;i<this.m_K;i++)
			//	logger.info("nClassAttValue[{}]={}",i,this.nClassAttValue.get(i));
			
		}
		
		if (event instanceof InstanceContentEvent)
		{
			//logger.debug("Processing InstanceContentEvent.");
			InstanceContentEvent ice = (InstanceContentEvent)event;

			boolean suc= false;
			
			//[debug] show instance values
			Instance inst = ice.getInstance();
			//debugInstanceValues(inst);
			
			//[!]
			//logger.debug("event is testing={}?is train={}",ice.isTesting(),ice.isTraining());
			ResultContentEvent rce =null;
			
			if (ice.isTesting())
			{
				//predict instance from inputStream
				//logger.debug("Predict instance.");
				rce = predict(ice);
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


	/**
	 * Predict test instance
	 * @param event
	 * @return true if successful predicted
	 */
	private ResultContentEvent predict(InstanceContentEvent event)
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
				
		double[] votes = getVotesForInstance(inst);
		// if votes==null,
		// the ResultContentEvent created with null votes will cause
		// evaluator crash!
		if (votes==null)
		{
			logger.info("error: votes is null, cannot decide the class of instance.");
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
	
	
	private double[] getVotesForInstance(Instance inst) {
        return doNaiveBayesPrediction(inst, this.nClass,
                this.attributeObservers);
    }
	
	private static double[] doNaiveBayesPrediction(Instance inst,
            DoubleVector observedClassDistribution,
            AutoExpandVector<AttributeClassObserver> attributeObservers) {
        double[] votes = new double[observedClassDistribution.numValues()];
        //double observedClassSum = observedClassDistribution.sumOfValues();
        for (int classIndex = 0; classIndex < votes.length; classIndex++) {
            //votes[classIndex] = observedClassDistribution.getValue(classIndex)
            //        / observedClassSum;
        	votes[classIndex] = observedClassDistribution.getValue(classIndex);
            for (int i = 0; i < inst.numAttributes(); i++) {
            	if (i==inst.classIndex())
            		continue;
            	
                //int instAttIndex = modelAttIndexToInstanceAttIndex(attIndex,
                //        inst);
            	
                AttributeClassObserver obs = attributeObservers.get(i);
                if ((obs != null) && !inst.isMissing(i)) {
                    votes[classIndex] *= obs.probabilityOfAttributeValueGivenClass(inst.value(i), classIndex);
                }
            }
        }
        // TODO: need logic to prevent underflow?
        return votes;
    }
	

	private AttributeClassObserver newNominalClassObserver() {
	        return new NominalAttributeClassObserver();
	}

	private AttributeClassObserver newNumericClassObserver() {
	        return new GaussianNumericAttributeClassObserver();
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
			logger.info("event has null instance.");
			return false;
		}
		trainOnInstance(inst);
		
		//logger.debug("finish train instance.");
		return true;
	}
	
	
    private void trainOnInstance(Instance inst) {
        this.nClass.addToValue((int) inst.classValue(), inst.weight());
        for (int i = 0; i < inst.numAttributes(); i++) {
        	if (i==inst.classIndex())
        		continue;
            AttributeClassObserver obs = this.attributeObservers.get(i);
            if (obs == null) {
                obs = inst.attribute(i).isNominal() ? newNominalClassObserver()
                        : newNumericClassObserver();
                this.attributeObservers.set(i, obs);
            }
            
            // error detect and ingorance
            double v = inst.value(i);
            if ( (v>=inst.attribute(i).numValues() || v<0) && inst.attribute(i).isNominal())
            {
            	//invalid data, ignore it
            	logger.error("invalid data [{}] at attribute [{}].\nRecord:[{}]",v,i,inst.toString());
            	//debugInstanceValues(inst);
            	return;//stop train this instance
            }
            else
            {
            	obs.observeAttributeClass(v, (int) inst.classValue(), inst.weight());
            }
        }
    }
    
    /**
     * Show instance values in debug mode
     * @param inst
     */
    private void debugInstanceValues(Instance it)
    {
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
		logger.info("Read Instance: "+itValues);
    }
		
	static class Builder
	{
		//required parameters
		private Instances dataset;
		private int bin;
		
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
		
		public Builder nBin(int bin) {
			this.bin = bin;
			return this;
		}
		
		Builder(NBModelProcessor oldProcessor){
			this.dataset = oldProcessor.dataset;
			this.bin = oldProcessor.nBinForNumAtt;
		}
	}
}
