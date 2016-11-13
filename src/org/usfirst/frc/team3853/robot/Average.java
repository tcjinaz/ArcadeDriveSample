package org.usfirst.frc.team3853.robot;

public class Average {

  private int       sampleLast;
  private double    sampleList[];

  private final int sampleSize;

  public Average() {
    sampleSize = 4;
    sampleLast = sampleSize;
    sampleList = new double[sampleSize];
    for (int i = 0; i < sampleSize; i++)
      sampleList[i] = 0.0;
  }

  public Average(double val) {
    sampleSize = 4;
    sampleLast = sampleSize;
    sampleList = new double[sampleSize];
    for (int i = 0; i < sampleSize; i++)
      sampleList[i] = val;
  }

  public Average(int size) {
    if (size <= 0) {
      size = 1;
    }
    sampleSize = Math.abs(size);
    sampleLast = sampleSize;
    sampleList = new double[sampleSize];
    for (int i = 0; i < sampleSize; i++)
      sampleList[i] = 0;
  }

  public Average(int size, double val) {
    sampleSize = Math.abs(size);
    sampleLast = sampleSize;
    sampleList = new double[sampleSize];
    for (int i = 0; i < sampleSize; i++)
      sampleList[i] = val;
  }

  public void sampleAdd(double sample) {
    sampleLast += 1;
    if (sampleLast >= sampleSize)
      sampleLast = 0;
    sampleList[sampleLast] = sample;
  }

  public double sampleAverage() {
    double avg = 0.0;
    for (int k = 0; k < sampleSize; k++)
      avg += sampleList[k];
    avg = avg / sampleSize;
    return avg;
  }

  public double sampleAverage(double sample) {
    sampleAdd(sample);
    return sampleAverage();
  }

  public String sampleAverageString() {
    return String.format("%f", sampleAverage());
  }

}