package com.example1;

import java.util.ArrayList;

public class BitAndByte {
	
	private String bitString = "" ;
	private int intVal ;
	private int numOfBits;
	private final int MASK = 1;
		
	public BitAndByte() {  // Constructor to instantiate the class
		
	}
	
	// The method belows displays the specific number of bits in a given integer. It's inclusion is for troubleshooting purpose
	public String showBits(int num, int numOfBits) {	
		
		intVal = num ;
		int bitSeparator = 0;
		for(int mask = MASK << (numOfBits - 1); mask !=0; mask >>>= 1) {
			if((num & mask) != 0) {
				bitString +=1 ;
				//System.out.print("1");
			}
			else {
				bitString +=0 ;
				//System.out.print("0");
			}
			bitSeparator++;
			if((bitSeparator % 8) == 0) {
				bitString +=" ";
				
				bitSeparator = 0;
			}	
		}
		return bitString ;
	}
	
	// The code below extracts individual bytes from the 32-bit integer value
	public byte[] extractByte(int aNum, int numByte) {
		
		// numByte determines the number of bytes returned. Valid output are one, two or four bytes.
		int i = aNum ;
		byte b3 = (byte) ((i>>24));
		byte b2 = (byte) ((i>>16) & 255);
		byte b1 = (byte) ((i>>8) & 255);
		byte b0 = (byte) ((i) & 255);
		if (numByte == 1) {
			byte[] vectorByte = new byte[1] ;
			vectorByte[0] = b0 ;
			return vectorByte ;
		}
		else if(numByte == 2) {
			byte[] vectorByte = new byte[2] ;
			vectorByte[0] = b1 ;
			vectorByte[1] = b0 ;
			return vectorByte ;
		}
		else if(numByte == 4) {
			byte[] vectorByte = new byte[4] ;
			vectorByte[0] = b3 ;
			vectorByte[1] = b2 ;
			vectorByte[2] = b1 ;
			vectorByte[3] = b0 ;
			return vectorByte ;
		}
		else {
			System.out.println("The number of Byte value entered is invalid: Enter 1,2 or 4");
			return null ;		
		}
	}
	
	// The method below converts bytes stream to the corresponding Integer value.
	public static int byteArrayToInt(byte[] b) {
	
	    int value = 0;
	    for (int i = 0; i < b.length; i++) {
	        int shift = (b.length - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}
	
	//The code snippet below converts from ArrayList Obj to an Array of byte values. An alternate approach is to use serialization
		public static byte[] arrayListTobyteArr(ArrayList<Byte> arrList) {
			byte[] arrbyte = new byte[arrList.size()];
			for(int b = 0; b < arrList.size(); b++) {
				arrbyte[b] = arrList.get(b) ;
			}
			return arrbyte ;
		}
	
	//Code for packet verification
	public boolean isDACKValid(byte[] receiveBuffer, byte[] sendBuffer) { //ACk field is compared with SeqNum field in a data packet
		boolean isValidated = false ;
		byte pktType = receiveBuffer[0] ;  
		byte sendPktType = sendBuffer[0] ;
		if(pktType == 01 & sendPktType == 00) { 
			byte[] seqNumByte = {sendBuffer[1] , sendBuffer[2]} ;  //seqNum bytes retrieved from the sendBuffer 
			byte[] ackNumByte = {receiveBuffer[1] , receiveBuffer[2]} ; //ackNum bytes are retrieved from receiveBuffer 
			int seqNum = byteArrayToInt(seqNumByte) ;   // integer value is re-calculated 
			int ackNum = byteArrayToInt(ackNumByte) ;

			if(ackNum == seqNum) {  //seqNum in the sendBuffer == ackNum in the receiveBuffer
				System.out.println() ;
				System.out.println("...Received DACK packet. Validating...., DACK Packet is fine!");
				isValidated = true ;
			}
			else System.out.println("Packet is out of order or corrupted");
			
		}
		
		else if(pktType == 03 & sendPktType == 02 ) {   //Here we want to validate RACK Packet
			System.out.println();
			System.out.println("...Received RACK packet. Validating...., RACK Packet is fine!");
			isValidated = true ;	
		}
		else if(pktType == 04 & sendPktType == 02) {
			System.out.println() ;
			System.out.println("...Received CLUS packet. Validating...., CLUS Packet is fine!");
			isValidated = true ;
		}
		return isValidated ;	
	}
	
	
	public int isDATAValid(byte[] receiveBuffer,int ackNum) {  //This code is used by the Server to validate received DATA packet 
		
		boolean ispktValid = false ;
		byte pktType = receiveBuffer[0] ;  	
		byte[] seqNumByte = {receiveBuffer[1] , receiveBuffer[2]} ;  //seqNum bytes retrieved from the sendBuffer 
		int seqNum = byteArrayToInt(seqNumByte) ;   // integer value is re-calculated 
		
		if(pktType == 0 & ackNum == seqNum) { 
			System.out.println() ;
			System.out.println("... validating Packet, please wait. ok, Packet is fine!");
			return 1 ;
		}
		else if(pktType == 00 & ackNum != seqNum) {
			System.out.println() ;
			System.out.println("... Out of order DATA packet received.! ... Resending the last DACK packet");
			
			return 2 ;
		}
		else return 0 ;
	}
}	
