package com.example1;

import java.util.ArrayList;
import java.util.Arrays;

public class DataPacket {
	
	private String[] pktTypeArr = {"DATA Packet ", "DACK Packet ","REQ Packet ", "RACK Packet ","CLUS Packet ", "CACK Packet "} ;
	private ArrayList<Integer> allPayloadData ;
	private int payloadSize ;   		//This parameter is 2 * Maximum Num of 2D data vectors. Each vector has two points(x,y)
	private int totalNumOfPkts ;        //Total number of packets goes from zero(apktID) to totalNumOfPkts
	private int aheaderSize ;  			// header Size in bytes. it could be 1,3, or 5 depending on if there is seqNum.
	private int apktID ;                // packet number , different from packet type
	private byte aPackType ;			// packet type
	private byte[] dataPacket ;         // final data packet array.
	private int dataPktArrLen ;
	
	public DataPacket(ArrayList<Integer> totalPayloadData, int apayloadSize) {
		allPayloadData  = totalPayloadData ;
		payloadSize = apayloadSize ;  
		totalNumOfPkts =  Math.round(allPayloadData.size()  / (float) payloadSize) ;
	
		
	}
	public DataPacket() {
	
	}
	
	//This method uses some other methods like the extractByte and getHeader methods to generate Data Packets.
	BitAndByte callbit = new BitAndByte() ;   //This Class is needed because of some Methods required in this code 
	
	public byte[] getDataPkt(byte PackType, int headerSize, int pktID) {
		
		if(headerSize > 5 | headerSize < 1) {
			System.out.println("... Hmmm, Packet header size is invalid. It must be between 1 and 5 bytes long");
			return null ;
		}
		else {
			
			aPackType = PackType ;
			aheaderSize = headerSize ;
			apktID = pktID ;
			int arraySize = payloadSize * 4 + aheaderSize ;  // 4 bytes per data point.
			dataPktArrLen = arraySize ;
			byte[] dataPacket = new byte[arraySize];
			
			int seqNum ; 										//packet sequence number 
			           
			int packetID = pktID ; 									// flexibility to specify the index of packet to generate
			// packetID represents index of the packet to be generated. This number corresponds to the seqNum in the packet.
			// maxNumSegment is the maximum Number of data segments that can be generated from our data stream 
			
			int dataEnd ;  										//This parameter points to the Next data in the ArrayList
			
			if(packetID > (totalNumOfPkts - 1)) {
				System.out.println("No enough data to generate packetID " + packetID);
			}
			else {
				
				seqNum = packetID ;
				
				byte[] dataVectorByte;  // This Array will contain 4 bytes representing each data in allData Master list
				
				int dataStart = packetID*(payloadSize) ;  // gives us the flexibility to generate 1st, 2nd, 3rd..packets			
				
				// determine the dataEnd parameter so we don't get IndexOutOfBound error when reading the allData Array
				if(allPayloadData.size() - dataStart >= (payloadSize)) {
					dataEnd = payloadSize + dataStart ;
				}
				else {
					dataEnd = allPayloadData.size() ;
					byte[] lastPacket = new byte[(dataEnd - dataStart)*4 + aheaderSize];  //Array for the last packet 
					dataPacket = lastPacket ;
					
				}
				int dataCount = 0 ;
				
				for(int j = dataStart; j < dataEnd; j++) {   
					
					dataVectorByte = callbit.extractByte(allPayloadData.get(j),4) ;  // four bytes extracted from each data point
					
					for(int k = 0; k < 4; k++) {
						
						// Since bytes must be appended starting from position x, (index 0 - (x-1) reserved for the header bytes
						// each j iteration generates 4 bytes. (j+k)+ headerSize presents effective Payload start point 
						//System.out.println(Arrays.toString(dataPacket));
						dataPacket[dataCount + k + aheaderSize] = dataVectorByte[k] ; //four extracted bytes from each int. value
					}
					if((dataEnd - j) != 1) {    //Condition to determine if we're at the end of the data stream.
						dataCount += 4 ;
						continue ;
					}
					else {
						payloadSize = (dataEnd - dataStart)  ;  
						ArrayList<Byte> pktHeader ;
						int NumOfDataVector = payloadSize / 2 ;          // each 2D data Vector is made up of two data points
						// We make a call to the getHeader Method as shown below. Method returns 5 bytes of  data in an ArrayList
						
						if(PackType == 00) {
							pktHeader = getHeader(PackType, seqNum, NumOfDataVector); //deals with Data packet
						}
						else {
							pktHeader = getHeader(PackType);	//deals with other packet type where seqNum is not required
						}
						for(int g = 0; g < pktHeader.size(); g++) { //add all aheaderSize bytes to dataPacket ArrayList
							dataPacket[g] = pktHeader.get(g) ;
						}
					}
				}
			}
			System.out.println(toString(dataPacket)) ;
			return dataPacket ;
		}
	}
	
	//The method below accepts variable no of Arguments to generate packet header.
	// Packet fields will following the order: PacketType, SeqNum/ackNum, then Size of Payload.
	
	public ArrayList<Byte>getHeader(byte pktType, int ... otherParam) {
		if(pktType > 5 | pktType < 0) {
			System.out.println("... Hmmm, Packet Type is not supported. Try another Type");
			return null ;
		}
		else {
		
			
			
			ArrayList<Byte> headerArr = new ArrayList<Byte>();  //generate an Array to hold required fields.
			
			headerArr.add(pktType) ;  // add packet type field first .
			
			// Next, we use if else conditions to determine header field parameters :
			
			if(otherParam.length == 0) {        //here no more field is required in the header
				System.out.println(toString(headerArr)) ;
				return headerArr ;
			}
			else if(otherParam.length == 1) {
				//the code below calls extractByte(int aNum, int numByte) to return x number of bytes from any int. value
				
				byte[] seqNumByte = callbit.extractByte(otherParam[0],2);  //otherParam[0] can either be seqNum or ackNum
				
				//the output of extraByte(int x, int y) is an Array containing either 4, 2 or 1 bytes as determined by int. y
				
				headerArr.add(seqNumByte[0]) ; //the two bytes generated are added to the headerArr ArrayList
				headerArr.add(seqNumByte[1]) ;
				//System.out.println(toString(headerArr)) ;
				return headerArr ;
			}
			else if(otherParam.length == 2) {
				// Next we need to add otherParam[1], which is the payload size to our header. Remember all these parameters are
				// optional, depending on the type of packet.
				
				headerArr = getHeader(pktType, otherParam[0]) ; //calls the getHeader method before appending other fields.
				
				byte[] ploadSizeByte = callbit.extractByte(otherParam[1],2); //code generates two bytes to rep num of Payload data
				
				headerArr.add(ploadSizeByte[0]) ; //generated bytes are added to the headerArr ArrayList
				headerArr.add(ploadSizeByte[1]) ;
				return headerArr ;
			}
			else {  // Last statement handles invalid number of arguments
				System.out.println("Packets cannot have more than three fields in the header");
				return null ;
			}
		}
	}
		
	//The code snippet below converts from ArrayList Obj to an Array of byte values. An alternate approach is to use serialization
	public static byte[] arrayListTobyteArr(ArrayList<Byte> arrList) {
		byte[] arrbyte = new byte[arrList.size()];
		for(int b = 0; b < arrList.size(); b++) {
			arrbyte[b] = arrList.get(b) ;
		}
		return arrbyte ;
	}
	public String toString(byte[] dataPacket) {
		String msg = "...Sending " + pktTypeArr[aPackType] + "Number: " + apktID  ;
		msg = msg + "\nThe content is: " + Arrays.toString(dataPacket);
		return msg ;
	}
	public String toString(ArrayList<Byte> dataPacket) {
		String msg = "...Sending " + pktTypeArr[dataPacket.get(0)] + "Number: " + 0  ;
		msg = msg + "\nThe content is: " + Arrays.toString(arrayListTobyteArr(dataPacket));
		return msg ;
	}
	
}
