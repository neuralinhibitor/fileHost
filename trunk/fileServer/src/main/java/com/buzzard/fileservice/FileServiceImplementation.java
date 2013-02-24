package com.buzzard.fileservice;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.buzzard.db.models.DataTable;
import com.buzzard.db.models.FileNameTable;
import com.buzzard.db.models.UserTable;
import com.buzzard.etc.ServiceHelper;
import com.buzzard.fileserver.AddUser;
import com.buzzard.fileserver.AddUserResponse;
import com.buzzard.fileserver.AuthenticationCredentials;
import com.buzzard.fileserver.File;
import com.buzzard.fileserver.GetFileList;
import com.buzzard.fileserver.GetFileListResponse;
import com.buzzard.fileserver.UpdateUser;
import com.buzzard.fileserver.UpdateUserResponse;
import com.buzzard.fileserver.UploadFileChunk;
import com.buzzard.fileserver.UploadFileChunkResponse;
import com.buzzard.fileserver.service.FileServerServiceSkeletonInterface;


public class FileServiceImplementation implements FileServerServiceSkeletonInterface
{
  
  public GetFileListResponse getFileList(
      GetFileList arg
      )
  {
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();
    
    UserTable userInfo = UserTable.verifyUser(userName, password);
    
    long userID = userInfo.getUserID();
    
    Collection<Long> fileIDs = 
        FileNameTable.getFileIDs(userID);
    
    List<File> files = new ArrayList<File>();
    
    for(Long fileID:fileIDs)
    {
      Collection<Long> offsets = DataTable.getDataOffsets(fileID);
      long[] primitiveOffsets = new long[offsets.size()];
      
      int index = 0;
      for(Long offset:offsets)
      {
        primitiveOffsets[index] = offset;
        index++;
      }
      
      String fileName = FileNameTable.getFileNameFromID(userID, fileID);
      
      File file = new File();
      file.setFileName(fileName);
      file.setFileID(fileID);
      file.setChunkOffsets(primitiveOffsets);
      
      files.add(file);
    }
    
    GetFileListResponse response = new GetFileListResponse();
    response.setFiles(files.toArray(new File[0]));
    
    return response;
  }

  @Override
  public AddUserResponse addUser(AddUser arg)
  {
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();

    boolean wasSuccess = true;
    
    try
    {
      UserTable.addUser(userName, password);
    }
    catch(Exception e)
    {
      wasSuccess = false;
    }
    
    AddUserResponse response = new AddUserResponse();
    response.setWasSuccess(wasSuccess);
    
    return response;
  }

  @Override
  public UpdateUserResponse updateUser(UpdateUser arg)
  {
    boolean wasSuccess = true;
    
    try
    {
      UserTable.updateUser(
          arg.getOldCredentials().getUserName(), 
          arg.getOldCredentials().getUserPassword(), 
          arg.getNewCredentials().getUserName(), 
          arg.getNewCredentials().getUserPassword());
    }
    catch(Exception e)
    {
      wasSuccess = false;
    }
    
    UpdateUserResponse response = new UpdateUserResponse();
    response.setWasSuccess(wasSuccess);
    
    return response;
  }

  @Override
  public UploadFileChunkResponse uploadFileChunk(UploadFileChunk arg)
  {
    UploadFileChunkResponse response = new UploadFileChunkResponse();
    response.setWasSuccess(false);
    
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();
    
    if(!UserTable.testVerifyUser(userName, password))
    {
      return response;
    }
    
    String fileName = arg.getChunk().getFileName();
    long dataOffset = arg.getChunk().getOffset();
    
    long userID = UserTable.verifyUser(userName, password).getUserID();
    
    FileNameTable.addFileNameTableEntryIfMissing(
        userID, 
        fileName);
    
    try
    {
      InputStream input = arg.getChunk().getData().getInputStream();
      
      boolean stop = false;
      while(!stop)
      {
        byte[] data = ServiceHelper.extractNextChunk(input);
        
        if(data == null)
        {
          stop = true;
        }
        else
        {
          DataTable.persistData(userID, fileName, dataOffset, data);
        }
      }
      
      response.setWasSuccess(true);
    }
    catch(Exception e)
    {
      response.setWasSuccess(false);
    }
    
    return response;
  }

}

