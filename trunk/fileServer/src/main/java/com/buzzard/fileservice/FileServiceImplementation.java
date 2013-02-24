package com.buzzard.fileservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.buzzard.db.models.DataTable;
import com.buzzard.db.models.FileNameTable;
import com.buzzard.db.models.UserTable;
import com.buzzard.fileserver.AuthenticationCredentials;
import com.buzzard.fileserver.File;
import com.buzzard.fileserver.GetFileList;
import com.buzzard.fileserver.GetFileListResponse;
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

}
