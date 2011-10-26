package org.sbml.simulator.math;

public class NoMean extends MeanFunction {
  
  /**
   * 
   */
  private static final long serialVersionUID = 7739137402966833880L;

  /*
   * (non-Javadoc)
   * @see org.sbml.simulator.math.MeanFunction#computeMean(double[])
   */
  public double computeMean(double[] values) {
    double result = 0d;
    for(double value:values) {
      result+=value;
    }
    return result;
  }
  
}
