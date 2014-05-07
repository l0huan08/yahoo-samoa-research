package com.yahoo.labs.samoa.learners.classifiers.hl;

import net.jcip.annotations.Immutable;// to make NBAttStat create succesfully in S4 multi node mode

import com.yahoo.labs.samoa.core.ContentEvent;
import com.yahoo.labs.samoa.learners.classifiers.hl.attributeclassobservers.AttributeClassObserver;

/**
 * Carry AttributeStat informations P(Xi|Cj) where j=0..nClass-1
 *   attributeObservers[iMin~iMax]
 * and i=iMin..iMax
 * this information is sent from NBAttributeStat after training, and 
 * sent to NBModel to update it
 * @author hl
 *
 * edit 2014.5.2 to make it immutable
 */
@Immutable
final public class NBAttStatEvent implements ContentEvent {

	//private static final long serialVersionUID = ???;

	/**
	 * 2014.4.18
	 */
	private static final long serialVersionUID = 3255308513313788188L;
	
	private boolean isLast = false;
	private long key = 0;
	private AttributeClassObserver[] attributeObservers;
	private int iMin; //range of attributes' index
	private int iMax;
	
	NBAttStatEvent()
	{}
	
	NBAttStatEvent(AttributeClassObserver[] attributeObservers,int iMin,int iMax,boolean isLast)
	{
		this.attributeObservers= attributeObservers;
		this.iMin=iMin;
		this.iMax=iMax;
		this.isLast=isLast;
	}
	
	@Override
	public String getKey() {
		return Long.toString(this.key);
	}

	@Override
	public void setKey(String key) {
		this.key = Long.parseLong(key);
	}

	@Override
	public boolean isLastEvent() {
		return isLast;
	}

//	public void setLast(boolean isLast) {
//		this.isLast = isLast;
//	}

	public AttributeClassObserver[] getAttributeObservers()
	{
		return this.attributeObservers;
	}
//	public void setAttributeObservers(AttributeClassObserver[] attributeObservers)
//	{
//		this.attributeObservers = attributeObservers;
//	}
	
	public int getIMin()
	{
		return this.iMin;
	}
	
	//min index of attribute observers
//	public void setIMin(int iMin)
//	{
//		this.iMin=iMin;
//	}
	
	public int getIMax()
	{
		return this.iMax;
	}
	
	//max index of attribute observers
//	public void setIMax(int iMax)
//	{
//		this.iMax=iMax;
//	}
}
