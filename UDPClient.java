
package com.example1;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.util.regex.*;

public class UDPClient {

	public static void main(String[] args) throws IOException {

		int portNum = 1025;											//Server port Num
		InetAddress ipAddr = InetAddress.getLocalHost();   			//To get Server IP
		String dataString;  										// read each line in the given file as a string
		int maxNumOfData = 50 ; 									//Max number of Data Vector 
		int payloadLenght = 2*maxNumOfData ;						// each 2D data vector has two floating points	
		ArrayList<Integer>dataVector = new  ArrayList<Integer>();  // data structure to store file data

		// Upload 2D Data Vectors from a File  and Store in an ArrayList called dataVector	
		// ------------------------------------------------------------------------------------------------------------------	
		try (BufferedReader reader = new BufferedReader(new FileReader("D:/Users/aderi/eclipse-workspace/com.example1/data01.txt"))) {
			while ((dataString = reader.readLine()) != null) {

				// Here we will use regular expression to match only the numeric contents in the file.

				String regex = "(-)?\\d\\.\\d+";   
				Pattern replace = Pattern.compile(regex);
				Matcher match = replace.matcher(dataString);

				while (match.find()) {
					float f = (Float.parseFloat(match.group()) * 100); //values are multiplied by 100 and converted to integer values 
					int intVal = (int) f;
					dataVector.add(intVal);
				}
			}	
		}
		catch (IOException exc) {
			System.out.println("I/O Error: " + exc);
		}
		int maxNumSegment = Math.round(dataVector.size() / (float)payloadLenght) ;//Total packets/segments achievable from data stream

		// Sending out the First Data Packet
		//----------------------------------------------------------------------------------------------------------------------------	
		// Creating an instance of  DataPacket Class

		DataPacket dataPkt = new DataPacket(dataVector,payloadLenght) ;

		byte pktType = 00;  //Packet type is specified for the 1st Data Packet
		int headerSize = 5 ;
		int packetID = 0;   //index of packet
		InetAddress localServer;
		int sendBufferSize  = 1 ;
		int rcvBufferSize = 0 ;

		byte[] sendBuffer = new byte[sendBufferSize] ;
		byte[] receiveBuffer = new byte [rcvBufferSize];

		String[] pktTypeArr = {"DATA Packet: ", "DACK Packet: ", "REQ Packet: ", "RACK Packet: ", "CLUS Packet: ", "CACK Packet: "} ;
		DatagramSocket socket = null;
		ArrayList<Float> finalCentroidArray = new ArrayList<>() ;  //Array will contain the two cluster centroids M1 & M2.
		try { // handles exception if there is no connection to Network and the  Server.

			int timeoutCounter = 0 ;
			//int timeout = 0 ;
			boolean sendMore = true ;
			int timeout = 1000 ;
			socket = new DatagramSocket();
			while(sendMore) {

				if(pktType == 00) {
					sendBuffer = dataPkt.getDataPkt(pktType,headerSize,packetID);  //packetID '0' shows we're sending 1st Pkt.
					sendBufferSize = 405 ;  //Header + Payload bytes .
					rcvBufferSize = 03 ;  // header for DACK packet is 3 bytes long
				}
				else if(pktType == 02 & packetID < 1) {
					sendBuffer = BitAndByte.arrayListTobyteArr(dataPkt.getHeader(pktType)) ;
					packetID = 0 ;	
					rcvBufferSize = 17 ;  // Since CLUS packet is expected immediately after REQ is sent, so we set its Array Size	
				}
				else if(pktType == 05 ) {
					rcvBufferSize = 01;
					packetID = 0 ;
					timeout = 30000 ;
					sendBuffer = BitAndByte.arrayListTobyteArr(dataPkt.getHeader(pktType)) ;
				}
				DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, ipAddr, portNum);
				socket.send(sendPacket);
				
				if(pktType == 05) { // If a CACK packet has been sent previously
					System.out.println() ;
					System.out.println("....Extracting byte encoded cluster centroid data vectors, pls wait");
					System.out.println() ;
					// extracting byte encoded data vector from each correctly received Data packet.
					for(int k = 1; k < receiveBuffer.length; k+=4) {
						byte b3 = receiveBuffer[k] ;
						byte b2 = receiveBuffer[k+1];
						byte b1 = receiveBuffer[k+2] ;
						byte b0 = receiveBuffer[k+3] ;
						//converting the 4-byte data into integer value by calling byteArrayToInt(byte[] b) method
						byte[] byteArray = {b3,b2,b1,b0} ;	
						int intValue = preparePacket.byteArrayToInt(byteArray) ;
						float f = ((float) intValue) / 1000 ;
						finalCentroidArray.add(f) ;	

					}
					System.out.println("Displaying M1,M2 Cluster Centroid vectors: " + finalCentroidArray);
					socket.close();
					System.exit(-1);
				}

				// Now receiving data from UDP socket. but first test socket time out event
				//----------------------------------------------------------------------------------------------------			
	
				receiveBuffer = new byte [rcvBufferSize];
				try { // Exception due to DatagramPacket issue.

					DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

					try {  // Exception due to socket timeout

						socket.setSoTimeout(timeout);  // set the timeout in milliseconds.
						socket.receive(receivePacket) ;

					}	
					catch (SocketTimeoutException ex) {
						
						System.out.println() ;
						System.out.println((timeoutCounter + 1) + " Client socket timeout!");
						timeout *= 2 ;   // Timeout value doubles after each timeout
						timeoutCounter++ ;
						if(timeoutCounter < 5 & (receiveBuffer[0] != 3)) {  //This condition 
							System.out.println("..Resending Packet again") ;
							System.out.println() ;

						}
						else {
							System.out.println(ex.getMessage() + ": Netw Com failure, closing socket.. " ) ;
							System.exit(0);
						}		
					}

					// all the zeros in the Array for RACK packet. 

					//The Code below calls another Class for validation purpose
					
					if(receiveBuffer[0] != 0)  { //Validation makes sense if the first byte in the array is not a zero
						BitAndByte validityTest = new BitAndByte() ;   //isDACKValid Method is in the Class called BitAndByte 

						if(validityTest.isDACKValid(receiveBuffer, sendBuffer)) {

							System.out.println("...Accepting validated " + pktTypeArr[receiveBuffer[0]] + packetID + " from the Server");
							if(receiveBuffer[0] == 3) {  //receiveBuffer for RACK and CLUS packet share the same size of 17
								byte[] Buffer = new byte[1] ;  // This code eliminates all the zeros in the receiveBuffer for RACK packet
								Buffer[0] = receiveBuffer[0] ;
								System.out.println("The Content of the packet is: " + Arrays.toString( Buffer));	
							}
							else System.out.println("The content of the packet is: " + Arrays.toString( receiveBuffer));
							System.out.println();
							
							if ((maxNumSegment - packetID) == 1) {   //This tests the end of sending of Data packet.
								pktType = 02 ;
								packetID = 0 ;
							}	
							else if(receiveBuffer[0] == 4) { //This enusre CACK packet is generated and sent to the server
								pktType = 05 ;
								packetID = 0 ;
								sendBufferSize  = 1 ;
							}
							else {        //increment packetID to generate more Data packet from the stream
								packetID++ ;
								
							}
						}
						else {
							socket.setSoTimeout(timeout);
							socket.receive(receivePacket);
							throw new SocketTimeoutException("Invalid packet") ;  //Exception is thrown if packet validation failed
						}

					}

				}	
				catch (IOException e) {
					System.out.println();
					System.out.println("...Received packet failed validation, it's discarded") ;
					timeout *= 2 ;   // Timeout value doubles after each timeout
					timeoutCounter++ ;
					if(timeoutCounter < 5 ) {  //This condition 
						System.out.println("..Resending Packet again counting: " + timeoutCounter) ;
						System.out.println() ;
					}
					else {
						System.out.println("Netw Communication failure, closing socket.. " + e.getMessage() ) ;
						System.exit(0);
					}
				} 
			}

		}
		catch (UnknownHostException e) {
			System.out.println("Client: Error connecting to the Server");
		}			System.exit(-1);
	}											
}							




