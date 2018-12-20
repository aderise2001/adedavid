package com.example1;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class kMClustering {
	float[] centroidArray ;  //Array containing randomly generated cluster centroid vectors
	float[] newCentroid ;
	
	public kMClustering() {
	}
	
	// This Method takes an Array of 2D data Vectors as input to randomly generate two cluster centroid vectors
	
	public float[] getInitialClusterCentroid (ArrayList<Float>dataVector) {
		float minX1 = 0 ;   //min value along xi(1)  i = 1,2,3 .. N
		float maxX1 = 0;	//max value along xi(1) 
		float minX2 = 0;    //minimum value along xi(1) 
		float maxX2 = 0;
		
		// Here we determine the Min and Max values along each X,Y dimension of the vector.
		for(int c = 0; c < dataVector.size(); c ++) {
			if(c % 2 == 0) {
				if (minX1 > dataVector.get(c)) minX1 = dataVector.get(c) ;  // even-numbered data Vectors in the Array, representing x-dimensions
				if (maxX1 < dataVector.get(c)) maxX1 = dataVector.get(c) ;		
			}
			else {
				if (minX2 > dataVector.get(c)) minX2 = dataVector.get(c) ;  // odd-numbered data Vectors in the Array, representing y-dimensions
				if (maxX2 < dataVector.get(c)) maxX2 = dataVector.get(c) ;	
			}
		}
		
		Random rand = new Random() ;
		//m1 = cluster centroid vector1 [m11, m12]
		
		// m11 represent a random floating number within the min and max values along the x-dimension 
		// m12 represent a random floating number within the min and max values along the y-dimension
		float m11 = minX1 + rand.nextFloat() * (maxX1 - minX1);   
		float m12 = minX2 + rand.nextFloat() * (maxX2 - minX2);
		
		//m2 = cluster centroid vector2 [m21, m22]
		// m21 represent a random floating number within the min and max values along the x-dimension 
		// m22 represent a random floating number within the min and max values along the y-dimension
		
		float m21 = minX1 + rand.nextFloat() * (maxX1 - minX1);   
		float m22 = minX2 + rand.nextFloat() * (maxX2 - minX2);
		
		float[] centroidArray = {m11,m12,m21,m22} ;
		//System.out.println("initial centroidArray is " + Arrays.toString(centroidArray));
		return centroidArray ;  //An Array containing two randomly generated cluster centroid vectors
	}
	// The Method below recalculates cluster centroid vectors using the randomly generated ones as input
	//creates two clusters C1, C2, recalculates M1, M2 as averages of the vectors in C1,C2
	
	public static float[] getNewCentroidFromOld(ArrayList<Float> arrList, float[] initialCentroid) {
		//Starting with two empty Clusters C1 & C2
		ArrayList<Float> cluster1 = new ArrayList<>() ;  //Will house data vectors closest to M1
		ArrayList<Float> cluster2 = new ArrayList<>() ;  //Will house data vectors closest to M2
		for(int z = 0; z < (arrList.size() - 1); z += 2) {
			
			//Calculating distance from X to M1 :  //arrList contains all the original Data vectors received from the Client.
			////initialCentroid is the Array containing the initialized cluster centroid vectors M1 and M2
			
			double X1_M11 = Math.pow((arrList.get(z) - initialCentroid[0]),2) ;  
			double X2_M12 = Math.pow((arrList.get(z+1) - initialCentroid[1]),2) ;
			double dist_X_To_M1  = Math.sqrt(X1_M11 + X2_M12);
			
			//Calculating distance from X to M2 :
			double X1_M21 = Math.pow((arrList.get(z) - initialCentroid[2]),2) ;
			double X2_M22 = Math.pow((arrList.get(z+1) - initialCentroid[3]),2) ;
			double dist_X_To_M2  = Math.sqrt(X1_M21 + X2_M22);
			
			if(dist_X_To_M1 < dist_X_To_M2) {
				cluster1.add(arrList.get(z)) ;
				cluster1.add(arrList.get(z + 1)) ;
			}
			else {
				cluster2.add(arrList.get(z)) ;
				cluster1.add(arrList.get(z + 1)) ;	
			}
		}
		// Calling the getCentroidFromAvg(ArrayList<Double> arrList) Method
		ArrayList<Float> centroidM1 = getCentroidFromAvg(cluster1);
		ArrayList<Float> centroidM2 = getCentroidFromAvg(cluster2);
		
		float[] newCentroid = {centroidM1.get(0),centroidM1.get(1),centroidM2.get(0), centroidM2.get(1)} ;
		
		return newCentroid ;
	}
		
	// This Method returns a cluster centroid vector as the average of data vectors in a given ArrayList	
	
	public static ArrayList<Float> getCentroidFromAvg(ArrayList<Float> arrList) {
		ArrayList<Float> centroidVector= new ArrayList<>() ;	
		float sumOfX1Values = 0 ;
		float sumOfX2Values = 0 ;
		for(int c = 0; c < arrList.size(); c ++) {
			
			if(c % 2 == 0) {
				sumOfX1Values += arrList.get(c) ;  // even-numbered data Vectors in the Array, representing x-dimensions
			}	
			else {
				sumOfX2Values += arrList.get(c) ;  // odd-numbered data Vectors in the Array, representing x-dimension	
			}	
		}
		float m11 = sumOfX1Values / (2 * arrList.size()) ; // m11 is average of x-dimension points
		float m12 = sumOfX2Values / (2 * arrList.size()) ; // m11 is average of x-dimension points
		centroidVector.add(m11) ;
		centroidVector.add(m12) ;
		
		return centroidVector ;		
	}
	public String toString(int loop) {
		String text = "The randomly generated cluster centroid M1,M2 is: " + Arrays.toString(centroidArray);
		text = text + "\nNew cluster centroid vectors generated using the randomly generated ones is: " + Arrays.toString(newCentroid);
		return text ;
	}
	
}	



