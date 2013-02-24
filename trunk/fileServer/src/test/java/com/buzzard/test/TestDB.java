package com.buzzard.test;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

import com.buzzard.db.HibernateManager;
import com.buzzard.db.models.DataTable;
import com.buzzard.db.models.FileNameTable;
import com.buzzard.db.models.UserTable;

import junit.framework.TestCase;



public class TestDB extends TestCase
{
  
  public TestDB(String name)
  {
    super(name);
  }
  
  public void testConnection()
  {
    HibernateManager.getSession().close();
  }
  
  private void assertEqual(byte[] a1, Byte[] a2)
  {
    assertTrue(a1.length == a2.length);
    
    for(int i=0; i<a1.length; i++)
    {
      assertTrue(a1[i] == a2[i]);
    }
  }
  
  private Byte[] reconstructData(long fileID)
  {
    Set<Long> offsets = DataTable.getDataOffsets(fileID);
    
    List<Byte> data = new ArrayList<Byte>();
    
    long currentOffset = 0l;
    
    for(Long offset:offsets)
    {
      assertTrue(currentOffset == offset);
      
      byte[] chunk = DataTable.getChunk(fileID, offset);
      
      for(byte b:chunk)
      {
        data.add(b);
      }
      
      currentOffset += chunk.length;
    }
    
    return data.toArray(new Byte[0]);
  }
  
  private static Map<File,byte[]> buildTestFileMap() throws IOException
  {
    File testFileDirectory = 
        new File("./testData");
    
    Map<File,byte[]> fileToDataMap = new HashMap<File,byte[]>();
    
    for(String name:testFileDirectory.list())
    {
      File file = 
          new File(
              String.format(
                  "%s/%s",
                  testFileDirectory.getAbsolutePath(),
                  name));
      
      System.out.printf("%s\n",name);
      
      byte[] data = FileUtils.readFileToByteArray(file);
      fileToDataMap.put(file, data);
    }
    
    return fileToDataMap;
  }
  
  public void testSpeed() throws IOException
  {
    Map<File,byte[]> fileToDataMap = buildTestFileMap();
    
    List<Long> userIDs = new ArrayList<Long>();
    List<String> userNames = new ArrayList<String>();
    
    for(long i=0; i<5; i++)
    {
      String userName = String.format("%d",i);
      userIDs.add(UserTable.addUser(userName, password));
      userNames.add(userName);
    }
    
    long writeTimeMillis = 0l;
    long readTimeMillis = 0l;
    long deleteTimeMillis = 0l;
    long bytesProcessed = 0l;
    
    double writeSpeed = 0.0d;
    double readSpeed = 0.0d;
    double deleteSpeed = 0.0d;
    
    final Map<File,Long> fileToIDMap = new HashMap<File,Long>();
    
    for(long userID:userIDs)
    {
      for(File file:fileToDataMap.keySet())
      {
        byte[] data = fileToDataMap.get(file);
        
        long fileID = -1;
        
        writeTimeMillis += -System.currentTimeMillis();
        {
          fileID = 
              DataTable.persistData(
                  userID, 
                  file.getName(), 
                  0, 
                  data);
        }
        writeTimeMillis += System.currentTimeMillis();
        
        fileToIDMap.put(file,fileID);
        
        bytesProcessed += data.length;
        
        writeSpeed = (1000.0d*bytesProcessed)/(1.0d*writeTimeMillis);
        System.out.printf(
            "write speed: %1.4fB/s (%1.4fkB/s)\n",
            writeSpeed,
            writeSpeed/1024);
      }
    }
    
    for(long userID:userIDs)
    {
      for(File file:fileToDataMap.keySet())
      {
        long fileID = fileToIDMap.get(file);
        
        readTimeMillis += -System.currentTimeMillis();
        {
          reconstructData(fileID);
        }
        readTimeMillis += System.currentTimeMillis();
        
        readSpeed = (1000.0d*bytesProcessed)/(1.0d*readTimeMillis);
        System.out.printf(
            "read speed: %1.4fB/s (%1.4fkB/s)\n",
            readSpeed,
            readSpeed/1024);
      }
    }
    
    for(long userID:userIDs)
    {
      for(File file:fileToDataMap.keySet())
      {
        deleteTimeMillis += -System.currentTimeMillis();
        {
          FileNameTable.deleteFileData(userID, file.getName());
        }
        deleteTimeMillis += System.currentTimeMillis();
        
        deleteSpeed = (1000.0d*bytesProcessed)/(1.0d*deleteTimeMillis);
        System.out.printf(
            "delete speed: %1.4fB/s (%1.4fkB/s)\n",
            deleteSpeed,
            deleteSpeed/1024);
      }
    }
    
    for(String userName:userNames)
    {
      UserTable.deleteUser(userName, password);
    }
    
    System.out.printf(
        "write speed: %1.4fB/s (%1.4fkB/s)\n",
        writeSpeed,
        writeSpeed/1024);
    
    System.out.printf(
        "read speed: %1.4fB/s (%1.4fkB/s)\n",
        readSpeed,
        readSpeed/1024);
    
    System.out.printf(
        "delete speed: %1.4fB/s (%1.4fkB/s)\n",
        deleteSpeed,
        deleteSpeed/1024);
    
//    100 iterations    
//    write speed: 14518757.5743B/s (14178.4742kB/s)
//    read speed: 9566381.1552B/s (9342.1691kB/s)
//    delete speed: 14985487.7042B/s (14634.2653kB/s)
  }
  
  private static final String password = "testPassword";
  
  public void testCreateDataTableObject() throws IOException
  {
    Map<File,byte[]> fileToDataMap = buildTestFileMap();
    
    final int users = 2;
    final int filesPerUser = fileToDataMap.keySet().size();
    
    final Set<String> fileNames = new TreeSet<String>();
    final Set<Long> userIDs = new TreeSet<Long>();
    final Set<String> userNames = new TreeSet<String>();
    
    for(long userID=-99999; userID<users-99999; userID++)
    {
      String userName = String.format("%d",userID);
      userIDs.add(UserTable.addUser(userName, password));
      userNames.add(userName);
    }
    
    for(long userID:userIDs)
    {
      for(File file:fileToDataMap.keySet())
      {
        fileNames.add(file.getName());
        
        DataTable.persistData(
            userID, 
            file.getName(), 
            0, 
            fileToDataMap.get(file));
      }
    }
    
    for(long userID:userIDs)
    {
      Collection<Long> fileIDs = FileNameTable.getFileIDs(userID);
      assertTrue(fileIDs.size() == filesPerUser);
      
      Set<String> observedFileNames = new TreeSet<String>();
      
      for(long fileID:fileIDs)
      {
        String fileName = FileNameTable.getFileNameFromID(userID,fileID);
        
        File file = null;
        for(File key:fileToDataMap.keySet())
        {
          if(key.getName().equals(fileName))
          {
            file = key;
          }
        }
        
        assertNotNull(file);
        
        observedFileNames.add(fileName);
        
        Set<Long> offsets = DataTable.getDataOffsets(fileID);
        
        byte[] goldData = fileToDataMap.get(file);
        Byte[] reconstructedData = reconstructData(fileID);
        
        assertEqual(goldData,reconstructedData);
        
        for(long offset:offsets)
        {
          byte[] data = DataTable.getChunk(fileID, offset);
          assertNotNull(data);
        }
        
        System.out.printf("\t%s\n",offsets);
      }
      
      assertTrue(observedFileNames.size() == fileNames.size());
      
      //delete the file from the table
      for(String fileName:fileNames)
      {
        assertTrue(observedFileNames.contains(fileName));
        
        FileNameTable.deleteFileData(userID,fileName);
      }
      
      assertTrue(FileNameTable.getFileIDs(userID).size()==0);
    }
    
    for(String userName:userNames)
    {
      UserTable.deleteUser(userName, password);
    }
    
  }
  
  
}


