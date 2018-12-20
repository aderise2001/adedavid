package ENT640;
import java.util.*;
import java.io.*;
import java.net.*;

import com.example1.*;

public class RndServer {
	public static void main(String[] args) {
		int SERVER_PORT = 1025;
		ArrayList<Float> rcvDataVectorArray = new ArrayList<>() ;
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(SERVER_PORT);

			System.out.println("Server online, listening on Port " + SERVER_PORT);
			System.out.println() ;
			String[] pktTypeArr = {": DATA Packet", ": DACK Packet", ": REQ Packet", ": RACK Packet", ": CLUS Packet", ": CACK Packet"};

			kMClustering utilclass = new kMClustering(); //One of the three Classes where several Methods exist  
			
			int rcvBufferSize = 405 ;
			ArrayList<Byte> headerByte = new ArrayList<>() ;
			int ackNum  = 0 ;
			int packetID = 0 ;
			byte replyPktType = 0x01 ;
			int donothing = 0 ;
			int timeoutCounter = 0 ;
			int initTimeout = 1000 ;
			
			while (true) {
				try {
					// listening to incoming request
					DataPacket dackPkt = new DataPacket();  //One of the three Classes where several Methods exist
					byte[] receiveBuffer = new byte [rcvBufferSize];
					DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
					socket.receive(receivePacket);
					
					InetAddress address = receivePacket.getAddress();
					int ClientPort = receivePacket.getPort();
					int byteCount = receivePacket.getLength() ; //get the size received packet.
					int pktType = receiveBuffer[0] ;
					byte[] Buffer = new byte [byteCount];  //Buffer array will contain the actual data received from Client
					
					for(int h = 0; h < byteCount ; h++) {  //this code removes zeros in the empty array, except for Data Packet
						Buffer[0] = receiveBuffer[0] ;
					}
					System.out.println("Packet " + packetID + pktTypeArr[pktType] + " has been received") ;
					
					if(receiveBuffer[0] != 0) System.out.println(Arrays.toString(Buffer));
					System.out.println(Arrays.toString(receiveBuffer));
					System.out.println() ;
					
					//Validating Data Packet recieved from the Client
					//--------------------------------------------------------------------------------------------------------------				
					if(receiveBuffer[0] == 0) {

						BitAndByte checkValidity = new BitAndByte() ;  //One of the three Classes where several Methods exist
						int validityIndex = checkValidity.isDATAValid(receiveBuffer, ackNum) ;  //1,2, or 0 is returned to verify Packet

						if(validityIndex == 1) {	
							headerByte = dackPkt.getHeader(replyPktType, ackNum) ; //prepares DACK packet to send to Client
							// extracting byte encoded data vector from each correctly received Data packet.
							
							for(int k = 5; k < byteCount; k += 4) {
								byte b3 = receiveBuffer[k] ;
								byte b2 = receiveBuffer[k+1];
								byte b1 = receiveBuffer[k+2] ;
								byte b0 = receiveBuffer[k+3] ;
								//converting the 4-byte data into integer value by calling byteArrayToInt(byte[] b) method
								byte[] byteArray = {b3,b2,b1,b0} ;	
								int intValue = checkValidity.byteArrayToInt(byteArray) ;
								float f = ((float) intValue) / 100 ;
								rcvDataVectorArray.add(f) ;	//adding the each 2D data point to the BIG ARRAY

							}
							ackNum++ ;
						}
						else if(validityIndex == 2)	{
							//acknowledge previous correctly received packet.
							headerByte = dackPkt.getHeader(replyPktType, ackNum-1) ; //resend DACK packet
						}
						
					}	
					else if(pktType == 02 & ackNum != 0) {
						replyPktType = 0x03;  //Sending RACK packet back to the client.
						headerByte = dackPkt.getHeader(replyPktType) ;
						rcvBufferSize = 1 ;
					}
					else if(pktType == 05 & ackNum != 0){  // expecting CACK packet here
						System.out.println("End of Transmission. Thank you for viewing") ;
						break ;	
					}
					else  {
						System.out.println("No Valid Data packet, ...nothing to be done");
						donothing = -1 ;
					}
					packetID++ ;
					byte[] sendBuffer = DataPacket.arrayListTobyteArr(headerByte) ;	
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, ClientPort);
						if(donothing == 0) {
		
							socket.send(sendPacket);
							System.out.println("...Sending" + pktTypeArr[replyPktType] + ". Packet' content is " + Arrays.toString(sendBuffer));
							System.out.println() ;
						}
						else ;   // Do nothing
					}
					catch (IOException e) {
						System.out.println("Server: Error sending response to client");
					}
					if(pktType == 2 & donothing == 0) {
						float[] newCentroid ;
						//"Starting K-means clustering computation"
						//-----------------------------------------------------------------------------------------------------------------
						try {  // Timer set to 3 seconds in anticipation of REQ packet resend !	
							socket.setSoTimeout(3000); // set a timer to determine if RACK has been received by the client
							throw new SocketTimeoutException("...Starting K-means clustering computation") ;
						}
						catch (InterruptedIOException ex) {
							System.out.println(ex.getMessage());

							// call the getClusterCentroid Method to initialize cluster Centroid Vectors m1 & m2 ;
							
							float[] initialCentroid = utilclass.getInitialClusterCentroid(rcvDataVectorArray);
							
							System.out.println("InitialCentroid is: " + Arrays.toString(initialCentroid));
							System.out.println() ;
							
							// calling the getNewCentroidFromOld Method to recalculate cluster centroid m1 & m2:
							newCentroid = kMClustering.getNewCentroidFromOld(rcvDataVectorArray, initialCentroid) ;
							
							float convergence = 0.00001f ;
							boolean hasNotConverged = true ;
							float currConvergence ;

							//System.out.print(currConvergence > convergence);
							int loop = 0 ;
							while (hasNotConverged) {

								double distx1 = Math.pow((newCentroid[0] - initialCentroid[0]),2) ;
								double disty1 = Math.pow((newCentroid[1] - initialCentroid[1]),2) ;
								float dist_NewM1_OldM1 = (float) Math.sqrt(distx1 + disty1);	

								double distx2 = Math.pow((newCentroid[2] - initialCentroid[2]),2) ;
								double disty2 = Math.pow((newCentroid[3] - initialCentroid[3]),2) ;
								float dist_NewM2_OldM2 = (float) Math.sqrt(distx2 + disty2);			

								currConvergence = dist_NewM1_OldM1 + dist_NewM2_OldM2 ;
								if(currConvergence < convergence) {
									System.out.println("The centroid has converged to " + currConvergence + " after " + loop + " iterations");
									System.out.println("Final: " + Arrays.toString(newCentroid)) ;
									break ;

								}
								else {
									initialCentroid = newCentroid ;
									newCentroid = kMClustering.getNewCentroidFromOld(rcvDataVectorArray, initialCentroid) ;
									loop++ ;	
								}		
							}
							// Coverting the two cluster centroid vectors to byte-encoded format.
						}
						ArrayList<Integer>finalCentroid = new  ArrayList<Integer>(); //creating a finalCentroid to send to Client
						int payloadLength = 4 ; //since we're sending four floating points as payload
						for(int k = 0; k < newCentroid.length; k++) {
							float f = (newCentroid[k] * 1000);
							f = Math.round(f) ;
							int intVal = (int) f ;
							finalCentroid.add(intVal) ;
						}
						// The code below calls DataPacket Class to access some Methods in the class. DataPacket Class is in a separate 
						// Package. So, FQ package name is stated . Creating another instance of DataPacket Class since a new payload
						// is invloved .

						DataPacket dataPkt = new DataPacket(finalCentroid,payloadLength) ; 	//calling the DataPacket Class 
						byte PackType = 04 ;
						int headerSize = 1 ;
						byte pktID = 0 ;  //starting packet index
						System.out.println() ;

						byte[] sendClusBuffer = dataPkt.getDataPkt(PackType, headerSize, pktID) ;
						DatagramPacket sendClusPacket = new DatagramPacket(sendClusBuffer, sendClusBuffer.length, address, ClientPort);
						socket.send(sendClusPacket);
						System.out.println() ;

						try {  // Exception to handle receipt of CACK Packet.

							socket.setSoTimeout(initTimeout);  // set the timeout in milliseconds.
							socket.receive(receivePacket);
						}	
						catch (InterruptedIOException ex) {
							System.out.println((timeoutCounter + 1) + " Client socket timeout!");
							initTimeout *= 2 ;   // Timeout value doubles after each timeout
							timeoutCounter++ ;
							if(timeoutCounter < 5) {
								System.out.println("..Resending Packet again") ;
								socket.send(sendClusPacket);
							}
							else {
								System.out.println("Error: Network Communication failure, closing socket ... ") ;
								System.exit(0);
							}	
						}
					}
				}			
				catch (IOException e) {
					System.out.println("Server: Error receiving client requests for byte data");
				}	
			}
		}
		catch (SocketException e) {
			System.out.println("Server: The Choosen port is not free");
			System.exit(-1);
		}
	}

}






