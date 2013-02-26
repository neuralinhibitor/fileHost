package com.buzzard.test;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.commons.io.FileUtils;

import com.buzzard.db.models.UserTable;
import com.buzzard.etc.BuzzardConstants;
import com.buzzard.fileserver.AddUser;
import com.buzzard.fileserver.AuthenticationCredentials;
import com.buzzard.fileserver.DeleteFile;
import com.buzzard.fileserver.DeleteFileResponse;
import com.buzzard.fileserver.DeleteUser;
import com.buzzard.fileserver.DownloadFileChunk;
import com.buzzard.fileserver.DownloadFileChunkResponse;
import com.buzzard.fileserver.FileChunk;
import com.buzzard.fileserver.GetFileList;
import com.buzzard.fileserver.GetFileListResponse;
import com.buzzard.fileserver.UpdateUser;
import com.buzzard.fileserver.UploadFileChunk;
import com.buzzard.fileservice.FileServiceImplementation;

import junit.framework.TestCase;



public class TestServiceInterface extends TestCase
{
  private final FileServiceImplementation service = 
      new FileServiceImplementation();
  
  private final Map<String,String> userToPasswordMap = 
      new TreeMap<String,String>();
  
  private final Map<String,String> newUserToPasswordMap = 
      new TreeMap<String,String>();
  
  private static final File testFile = 
      new File("./testData/test.dat");
  
  public TestServiceInterface(String name)
  {
    super(name);
    
    userToPasswordMap.put(
        "user112233333333",
        "password1");
    
    userToPasswordMap.put(
        "root999999",
        "test");
    
    userToPasswordMap.put(
        "user225192582159",
        "259381058askdfklhJHHKHJHdf8wqt790475287156897");
    
    
    
    newUserToPasswordMap.put(
        "user112233333333",
        "password1234");
    
    newUserToPasswordMap.put(
        "root999999",
        "testasf");
    
    newUserToPasswordMap.put(
        "user225192582159",
        "25938");
  }
  
  
  
  public void testAddUser()
  {
    addUsers();
    deleteUsers();
  }
  
  public void testDeleteUser()
  {
    addUsers();
    deleteUsers();
  }
  
  public void testUpdateUser()
  {
    addUsers();
    
    for(String userName:userToPasswordMap.keySet())
    {
      String oldPassword = userToPasswordMap.get(userName);
      String newPassword = newUserToPasswordMap.get(userName);
      
      UpdateUser arg = new UpdateUser();
      arg.setOldCredentials(getCredentials(userName,oldPassword));
      arg.setNewCredentials(getCredentials(userName,newPassword));
      
      service.updateUser(arg);
      
      assertFalse(UserTable.testVerifyUser(userName,oldPassword));
      assertTrue(UserTable.testVerifyUser(userName,newPassword));
    }
    
    for(String userName:userToPasswordMap.keySet())
    {
      String oldPassword = newUserToPasswordMap.get(userName);
      String newPassword = userToPasswordMap.get(userName);
      
      UpdateUser arg = new UpdateUser();
      arg.setOldCredentials(getCredentials(userName,oldPassword));
      arg.setNewCredentials(getCredentials(userName,newPassword));
      
      service.updateUser(arg);
      
      assertFalse(UserTable.testVerifyUser(userName,oldPassword));
      assertTrue(UserTable.testVerifyUser(userName,newPassword));
    }
    
    deleteUsers();
  }
  
  public void testGetFileList()
  {
    addUsers();
    
    for(String userName:userToPasswordMap.keySet())
    {
      String password = userToPasswordMap.get(userName);
      
      GetFileList arg = new GetFileList();
      arg.setCredentials(getCredentials(userName,password));
      
      GetFileListResponse response = service.getFileList(arg);
      
      assertTrue(response.getFiles().length == 0);
    }
    
    deleteUsers();
  }
  
  public void testFileUpload() throws IOException
  {
    addUsers();
    
    for(String userName:userToPasswordMap.keySet())
    {
      String password = userToPasswordMap.get(userName);
      
      List<FileChunk> chunks = getChunks();
      
      UploadFileChunk arg = new UploadFileChunk();
      arg.setCredentials(getCredentials(userName,password));
      
      for(FileChunk chunk:chunks)
      {
        arg.setChunk(chunk);
        
        service.uploadFileChunk(arg);
      }
      
      GetFileList getFileList = new GetFileList();
      getFileList.setCredentials(getCredentials(userName,password));
      
      GetFileListResponse response = service.getFileList(getFileList);
      
      assertTrue(response.getFiles().length == 1);
      com.buzzard.fileserver.File f = response.getFiles()[0];
      
      int numChunks = 
          (int)Math.ceil(
              (1.0d*getBytes().length)/
              (1.0d*BuzzardConstants.MAX_DATA_CHUNK_SIZE));
      
      for(long offset:f.getChunkOffsets())
      {
        System.out.printf("%d, ",offset);
      }
      System.out.printf("\n");
      
      assertTrue(f.getChunkOffsets().length == numChunks);
      

      //verify the chunk data
      List<Byte> bytes = new ArrayList<Byte>();
      for(long offset:f.getChunkOffsets())
      {
        DownloadFileChunk chunkArg = new DownloadFileChunk();
        chunkArg.setCredentials(getCredentials(userName,password));
        chunkArg.setFileName(testFile.getName());
        chunkArg.setOffset(offset);
        
        DownloadFileChunkResponse chunkResponse = 
            service.downloadFileChunk(chunkArg);
        
        InputStream chunkInput = 
            chunkResponse.getChunk().getData().getInputStream();
        
        boolean stop = false;
        while(!stop)
        {
          int nextByte = chunkInput.read();
          
          if(nextByte == -1)
          {
            stop = true;
          }
          else
          {
            bytes.add((byte)nextByte);
          }
        }
      }
      
      assertEqual(getBytes(),bytes.toArray(new Byte[0]));
      
      DeleteFile deleteArg = new DeleteFile();
      deleteArg.setCredentials(getCredentials(userName,password));
      deleteArg.setFileName(testFile.getName());
      
      DeleteFileResponse deleteResponse = service.deleteFile(deleteArg);
      assertTrue(deleteResponse.getWasSuccess());
      
      response = service.getFileList(getFileList);
      assertTrue(response.getFiles().length == 0);
    }
    
    deleteUsers();
  }
  
  private void uploadChunks(
      final FileChunk[] chunks,
      final int numThreads,
      final String userName, 
      final String password
      ) throws InterruptedException
  {
    assertTrue(chunks.length > 1);
    
    CountDownLatch latch = 
        new CountDownLatch(numThreads);
    
    int chunksPerThread = 
        (int)Math.ceil((1.0d*chunks.length)/(1.0d*numThreads));
    
    int chunkCount = 0;
    
    List<Thread> threads = new ArrayList<Thread>();
    for(int i=0; i<numThreads; i++)
    {
      List<FileChunk> threadChunks = new ArrayList<FileChunk>();
      for(int j=0; j<chunksPerThread; j++)
      {
        int index = i*chunksPerThread+j;
        
        if(index < chunks.length)
        {
          threadChunks.add(chunks[index]);
          chunkCount++;
        }
      }
      
      Uploader uploader = 
          new Uploader(service,userName,password,threadChunks,latch);
      
      Thread thread = new Thread(uploader);
      threads.add(thread);
    }
    
    assertTrue(threads.size() == numThreads);
    assertTrue(chunkCount == chunks.length);
    
    for(Thread thread:threads)
    {
      thread.start();
    }
    
    latch.await();
  }
  
  private static class Uploader implements Runnable
  {
    private final CountDownLatch latch;
    private final FileServiceImplementation service;
    private final List<FileChunk> chunks;
    private final String username;
    private final String password;
    
    public Uploader(
        FileServiceImplementation service,
        String username, 
        String password, 
        List<FileChunk> chunks,
        CountDownLatch latch
        )
    {
      this.username = username;
      this.password = password;
      this.chunks = chunks;
      this.service = service;
      this.latch = latch;
    }

    @Override
    public void run()
    {
      for(FileChunk chunk:chunks)
      {
        UploadFileChunk arg = new UploadFileChunk();
        arg.setCredentials(getCredentials(username,password));
        arg.setChunk(chunk);
        
        service.uploadFileChunk(arg);
      }
      
      latch.countDown();
    }
    
  }
  
  public void testMultithreadedUpload() throws IOException, InterruptedException
  {
    long sequentialTime = uploadFileMultithreaded(1);
    
    int[] threadCounts = new int[]{2,4,8,16,32,64};
    long[] threadTimes = new long[threadCounts.length];
    
    assertEquals(threadCounts.length,threadTimes.length);
    
    for(int i=0;i<threadCounts.length;i++)
    {
      int numThreads = threadCounts[i];
      long time = uploadFileMultithreaded(numThreads);
      threadTimes[i] = time;
    }
    
    System.out.printf("speedups:\n");
    for(int i=0;i<threadCounts.length;i++)
    {
      System.out.printf(
          "with %d threads, s=%1.4f\n",
          threadCounts[i],
          (1.0*sequentialTime)/(1.0*threadTimes[i]));
    }
  }
  
  private long uploadFileMultithreaded(
      int numThreads
      ) throws IOException, InterruptedException
  {
    long time = 0l;
    
    addUsers();
    
    for(String userName:userToPasswordMap.keySet())
    {
      String password = userToPasswordMap.get(userName);
      
      List<FileChunk> chunks = getChunks(1024*64);
      System.out.printf("%d chunks\n", chunks.size());
      {
        //TODO
        
        time = time - System.currentTimeMillis();
        
        uploadChunks(
            chunks.toArray(new FileChunk[0]),//final FileChunk[] chunks,
            numThreads,
            userName, 
            password);
        
        time = time + System.currentTimeMillis();
      }
      
      GetFileList getFileList = new GetFileList();
      getFileList.setCredentials(getCredentials(userName,password));
      
      GetFileListResponse response = service.getFileList(getFileList);
      
      assertTrue(response.getFiles().length == 1);
      com.buzzard.fileserver.File f = response.getFiles()[0];

      //verify the chunk data
      List<Byte> bytes = new ArrayList<Byte>();
      for(long offset:f.getChunkOffsets())
      {
        DownloadFileChunk chunkArg = new DownloadFileChunk();
        chunkArg.setCredentials(getCredentials(userName,password));
        chunkArg.setFileName(testFile.getName());
        chunkArg.setOffset(offset);
        
        DownloadFileChunkResponse chunkResponse = 
            service.downloadFileChunk(chunkArg);
        
        InputStream chunkInput = 
            chunkResponse.getChunk().getData().getInputStream();
        
        boolean stop = false;
        while(!stop)
        {
          int nextByte = chunkInput.read();
          
          if(nextByte == -1)
          {
            stop = true;
          }
          else
          {
            bytes.add((byte)nextByte);
          }
        }
      }
      
      assertEqual(getBytes(),bytes.toArray(new Byte[0]));
      
      DeleteFile deleteArg = new DeleteFile();
      deleteArg.setCredentials(getCredentials(userName,password));
      deleteArg.setFileName(testFile.getName());
      
      DeleteFileResponse deleteResponse = service.deleteFile(deleteArg);
      assertTrue(deleteResponse.getWasSuccess());
      
      response = service.getFileList(getFileList);
      assertTrue(response.getFiles().length == 0);
    }
    
    deleteUsers();
    
    return time;
  }
  
  
  private static void assertEqual(byte[] a1, Byte[] a2)
  {
    assertTrue(a1.length == a2.length);
    
    for(int i=0; i<a1.length; i++)
    {
      assertTrue(a1[i] == a2[i]);
    }
  }
  
  private static AuthenticationCredentials getCredentials(
      String user, 
      String password
      )
  {
    AuthenticationCredentials credentials = new AuthenticationCredentials();
    
    credentials.setUserName(user);
    credentials.setUserPassword(password);
    
    return credentials;
  }
  
  private void deleteUsers()
  {
    for(String userName:userToPasswordMap.keySet())
    {
      String password = userToPasswordMap.get(userName);
      
      DeleteUser arg = new DeleteUser();
      arg.setCredentials(getCredentials(userName,password));
      
      service.deleteUser(arg);
      
      assertFalse(UserTable.testVerifyUser(userName, password));
    }
  }
  
  private void addUsers()
  {
    for(String userName:userToPasswordMap.keySet())
    {
      String password = userToPasswordMap.get(userName);
      
      AddUser arg = new AddUser();
      arg.setCredentials(getCredentials(userName,password));
      
      service.addUser(arg);
      
      assertTrue(UserTable.testVerifyUser(userName, password));
    }
  }
  
  private byte[] getBytes() throws IOException
  {
    return FileUtils.readFileToByteArray(testFile);
  }
  
  private List<FileChunk> getChunks(final int chunkSize) throws IOException
  {
    List<FileChunk> chunks = new ArrayList<FileChunk>();
    byte[] bytes = getBytes();
    
    System.out.printf("retrieved %d bytes\n",bytes.length);
    
    int offset = 0;
    boolean stop = false;
    while(!stop)
    {
      int chunkLength = bytes.length - offset;
      if(chunkLength > chunkSize)
      {
        chunkLength = chunkSize;
      }
      
      if(chunkLength == 0)
      {
        stop = true;
      }
      else
      {
        byte[] data = new byte[chunkLength];
        System.arraycopy(bytes, offset, data, 0, chunkLength);
        
        {
          ByteArrayDataSource source = new ByteArrayDataSource(data);
          DataHandler dataHandler = new DataHandler(source);
          
          FileChunk chunk = new FileChunk();
          
          chunk.setFileName(testFile.getName());
          chunk.setOffset(offset);
          chunk.setData(dataHandler);
          chunks.add(chunk);
        }
        
        System.out.printf("\t@%d : %d\n", offset,chunkLength);
        
        offset+=chunkLength;
      }
    }
    
    int numChunks = (int)Math.ceil((1.0d*bytes.length)/(1.0d*chunkSize));
    
    assertTrue(offset == bytes.length);
    assertTrue(chunks.size() == numChunks);
    assertTrue(numChunks > 1);
    
    return chunks;
  }
  
  private List<FileChunk> getChunks()//TODO: return more than one chunk
  {
    FileDataSource dataSource = new FileDataSource(testFile);
    DataHandler dataHandler = new DataHandler(dataSource);
    List<FileChunk> chunks = new ArrayList<FileChunk>();
    
    FileChunk chunk = new FileChunk();
    chunk.setFileName(testFile.getName());
    chunk.setData(dataHandler);
    chunk.setOffset(0);
    
    chunks.add(chunk);
    
    return chunks;
  }
  
}


