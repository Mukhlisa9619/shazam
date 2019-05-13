/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sas;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;
/**
 *
 * @author Muxlisa
 */
public class SaS{

public static void main(String[] args) throws InterruptedException, UnsupportedAudioFileException, IOException, LineUnavailableException {
    
    short choice = 1;
    short songId = 1;
    
    
    System.out.println("\t*** Shazam with DFT ***\n\n");
    final short NUM_EL = 400;
    Scanner sc=new Scanner(System.in); 
    while(choice != 0) {
    System.out.println("Choose option: \n 1. Store music. \n 2. Search for music \n 0. EXIT");
    choice = sc.nextShort();
    String path=sc.nextLine();
    String name=sc.nextLine();  
    if(choice == 1) {
            System.out.println("Enter path for music");  
            path=sc.nextLine(); 
            System.out.println("Enter name of music");  
            name=sc.nextLine();
            System.out.println("Enter ID for song");  
            songId=sc.nextShort(); //it would be searched corresponding to this id
            insertSong(songId, path, name); // inserting songId, music_path and name to tthe database
        }
        if(choice == 2){
            System.out.println("Enter path for music");  // searching music from database 
            path=sc.nextLine();  
        }
        if(choice != 0) {
            //getting the samples of music
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            File fileIn = new File(path); // 
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fileIn);
            AudioFormat format = audioInputStream.getFormat();
            int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
            long frames = audioInputStream.getFrameLength();
            double durationInSeconds = (frames+0.0) / format.getFrameRate(); // total length of music duration 
            short NUM_OF_CHUNKS = (short)(durationInSeconds/0.05); // Divide music into 50ms each 
            byte[][] data_chunks = new byte[NUM_OF_CHUNKS][NUM_EL]; // Create corresponding byte Array
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                bytesPerFrame = 1;
            }
            int numBytes = 1024 * bytesPerFrame;
            byte[] audioBytes = new byte[numBytes]; //total array of samples in music 
            int numBytesRead = 0;
            // writing just music samples, cutting the header part 
            while ((numBytesRead = audioInputStream.read(audioBytes, 0, audioBytes.length)) != -1) {
                outputStream.write(audioBytes, 0, numBytesRead);
            }
            byte[] bytesOut=outputStream.toByteArray(); //array of clean music samples 
            System.out.println(bytesOut.length); //total length of samples for N 
            int ind = 0;
//            System.out.println("Frames: "+ frames + "\n"+"Duration "+NUM_OF_CHUNKS + " sec: " + durationInSeconds);

            for(int i =0; i<NUM_OF_CHUNKS; i++){
                for(int j =0; j<NUM_EL; j++){
                    data_chunks[i][j] = bytesOut[ind++]; // creating window size of 400 for each 50ms signal
                }
              List peaks = makeDft(data_chunks[i]); // sending each 400samples into DFT and returns just peak values 
                if (choice == 1) { 
                    insertHashes(peaks, songId); // inserting hashes into database 
                }
                if(choice == 2) { // music search by hashes 
                    if(searchSong(peaks)){ // searches hashed peak points from database 
                        break;
                    };  
                }

            }
            System.out.println("PATH: " + path + " ID: " + songId); ///printing out path of music with entered song id 
        }
        
    }
    
    

    


   } 
// function to compine array values into one string 
 private static String concat(List<Long> arr) {
        int len = arr.size();
        String concated = ""; 
        for(int i = 0; i < len; i++) {
            concated += String.valueOf(arr.get(i));
        }        
        return concated;
    }
 
 
 private static boolean searchSong(List<Long> peaks) throws LineUnavailableException, IOException, UnsupportedAudioFileException{
     boolean found = false;
     int index = 0;
        try {
            DbConnection db = new DbConnection();
//            String query = "Select * from hashtable;";

            ArrayList<String> ids = new ArrayList<String>();
            ArrayList<String> collectedIds = new ArrayList<String>();

//    divedes each 400 magnitude  by 10 for hashing 
            for(int i=0; i< peaks.size(); i+=10) {
                String query = "Select * from hashtable WHERE value LIKE '%";
                index = 10+i;
                
                // compare the size of peak numbers array with index
                if(peaks.size() > index) {
                   String hash = concat(peaks.subList(i, index)); // 10 magnitude arrays combined into one string 
//                 System.out.println("HASH: " + hash);
                   query += hash +"%';";
                   ids = db.get_data(query, "trackid", "value");
                }else {
                   String hash = concat(peaks.subList(i, peaks.size()));
                   query += hash +"%';";
                    ids = db.get_data(query, "trackid", "value");

                }
         //collecting found ids from databse into one dynamic arrayList 
                for(short k=0; k<ids.size(); k++)
                {
                    collectedIds.add(ids.get(k));
                }
            }
            System.out.println("ids" + collectedIds);
            
            // selecting appropriate music path from databse according the trackid
            
            String getM = "Select * from track where ";
            for(short k=0; k<collectedIds.size(); k++)
            {            
                if(k != collectedIds.size() - 1)getM = getM + "id=" + collectedIds.get(k) + " and ";
                else getM = getM + "id=" + collectedIds.get(k) + ";";
                
            }   
           System.out.println(getM);
      //works if music id found otherwise next 400samples send to DFT to find peak values and to be hashed 
            if(!ids.isEmpty()) { 
                ids = db.get_music(getM);
                found = true;
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(ids.get(0)));
             // Get a sound clip resource.
                Clip clip = AudioSystem.getClip();
             // Open audio clip and load samples from the audio input stream.
                clip.open(audioIn);
                clip.start();
            }

                
        }
        

        catch (SQLException ex) {
            ex.printStackTrace();
       }
//        int len = hashes.size();
        return found;
    }
 // DFT function for each 400samples 
 private static List makeDft(byte[] bytesOut){
     int N=bytesOut.length ; 
     System.out.println("LEN: " + N);
    long magnitudeArr[] =new long [N];
    double real[]=new double [N];
    double imag[]=new double [N];
  
    double omega = (2*Math.PI)/N; 
     for (int k=0; k<N; k++)
     {
        for (int n=0; n<N; n++)
        {
            real[k] += bytesOut[n]*Math.cos(omega*n*k);
            imag[k] += -(bytesOut[n]*Math.sin(omega*n*k));
        }
       // find out magnitude of each sample 
        long  magnitude=Math.round(Math.sqrt(Math.pow(real[k],2)+Math.pow(imag[k], 2)));
        magnitudeArr[k]=magnitude; 
    
     } 
    double avarage= 0.0; 
    
    //find out the peak values from magnitude array for hashing 
    for (int magIndex=0; magIndex<magnitudeArr.length; magIndex++)
    {
          avarage = avarage+ magnitudeArr[magIndex];  
    }
    
    avarage = avarage/magnitudeArr.length; 
    ArrayList<Long> peaks = new ArrayList<Long>();
//  System.out.println(avarage); 

//creating dynamic array for all peak values 
    for (int magIndex=0; magIndex<magnitudeArr.length; magIndex++)
     
    {
        if(magnitudeArr[magIndex]>=avarage)
        {
            peaks.add(magnitudeArr[magIndex]); 
        }
    }
    return peaks;
 }
 
 //inserting Hash values to the database 
 static void insertHashes(List peaks, short id) {
    int index = 0;
    String hashes = "insert into hashtable(trackid, value) values";
    for(int i=0; i< peaks.size(); i+=100) {
        index = 100+i;
        if(peaks.size() > index) {
            String hash = concat(peaks.subList(i, index));
            hashes += "(" + id + ",'"+hash+"'),";
        }else {
            String hash = concat(peaks.subList(i, peaks.size()));
            hashes += "(" + id + ", '"+hash+"');";
        }
    }

        System.out.println(hashes);
        
        try {
        DbConnection db = new DbConnection();
        db.execute(hashes);
        db.closeConnection(); 
       } 
        catch (SQLException ex) {
            ex.printStackTrace();
       }
 }
 
 //inserting song to the 'track' table into database 
 static void insertSong(short id, String path, String name) {
    String songQuery = String.format("insert into track(id, name, location) values(%d, '%s', '%s')", id, name, path);

//        System.out.println(hashes);
        
        try {
        DbConnection db = new DbConnection();
        db.execute(songQuery);
        db.closeConnection();
       } 
        catch (SQLException ex) {
            ex.printStackTrace();
       }
 }
 
 
}
