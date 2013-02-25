package com.buzzard.fileservice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.apache.axiom.attachments.ByteArrayDataSource;


import com.buzzard.db.models.DataTable;
import com.buzzard.db.models.FileNameTable;
import com.buzzard.db.models.UserTable;
import com.buzzard.etc.ServiceHelper;
import com.buzzard.fileserver.AddUser;
import com.buzzard.fileserver.AddUserResponse;
import com.buzzard.fileserver.AuthenticationCredentials;
import com.buzzard.fileserver.DeleteFile;
import com.buzzard.fileserver.DeleteFileResponse;
import com.buzzard.fileserver.DeleteUser;
import com.buzzard.fileserver.DeleteUserResponse;
import com.buzzard.fileserver.DownloadFileChunk;
import com.buzzard.fileserver.DownloadFileChunkResponse;
import com.buzzard.fileserver.File;
import com.buzzard.fileserver.FileChunk;
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
    
    UserTable.addUser(userName, password);
    
    AddUserResponse response = new AddUserResponse();
    response.setWasSuccess(true);
    
    return response;
  }

  @Override
  public UpdateUserResponse updateUser(UpdateUser arg)
  {
    UserTable.updateUser(
        arg.getOldCredentials().getUserName(), 
        arg.getOldCredentials().getUserPassword(), 
        arg.getNewCredentials().getUserName(), 
        arg.getNewCredentials().getUserPassword());
    
    UpdateUserResponse response = new UpdateUserResponse();
    response.setWasSuccess(true);
    
    return response;
  }

  @Override
  public UploadFileChunkResponse uploadFileChunk(UploadFileChunk arg)
  {
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();
    String fileName = arg.getChunk().getFileName();
    
    long userID = UserTable.verifyUser(userName, password).getUserID();
    long dataOffset = arg.getChunk().getOffset();
    
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
          dataOffset += data.length;
        }
      }
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }
    
    UploadFileChunkResponse response = new UploadFileChunkResponse();
    response.setWasSuccess(true);
    
    return response;
  }

  @Override
  public DeleteFileResponse deleteFile(DeleteFile arg)
  {
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();
    
    long userID = UserTable.verifyUser(userName, password).getUserID();
    
    FileNameTable.deleteFileData(userID,arg.getFileName());
    
    DeleteFileResponse response = new DeleteFileResponse();
    response.setWasSuccess(true);
    
    return response;
  }

  @Override
  public DeleteUserResponse deleteUser(DeleteUser arg)
  {
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();
    
    UserTable.deleteUser(userName, password);
    
    DeleteUserResponse response = new DeleteUserResponse();
    response.setWasSuccess(true);
    
    return response;
  }

  @Override
  public DownloadFileChunkResponse downloadFileChunk(
      DownloadFileChunk arg
      )
  {
    AuthenticationCredentials credentials = arg.getCredentials();
    
    String userName = credentials.getUserName();
    String password = credentials.getUserPassword();
    
    long userID = 
        UserTable.verifyUser(userName, password).getUserID();
    
    FileNameTable fileInfo = 
        FileNameTable.addFileNameTableEntryIfMissing(
            userID, 
            arg.getFileName());
    
    byte[] data = DataTable.getChunk(
        fileInfo.getFileID(), 
        arg.getOffset());
    
    DataSource dataSource = new ByteArrayDataSource(data);
    DataHandler handler = new DataHandler(dataSource);
    
    FileChunk chunk = new FileChunk();
    chunk.setData(handler);
    chunk.setFileName(arg.getFileName());
    chunk.setOffset(arg.getOffset());
    
    DownloadFileChunkResponse response = new DownloadFileChunkResponse();
    response.setChunk(chunk);
    
    return response;
  }

}

