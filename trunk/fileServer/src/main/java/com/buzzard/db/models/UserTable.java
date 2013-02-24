package com.buzzard.db.models;


import org.hibernate.criterion.Restrictions;

import com.buzzard.db.DBObject;



public class UserTable extends DBObject
{
  private long id;
  private long userID;
  private String userName;
  private String userPassword;
  private int permissions;
  
  public static void deleteUser(String userName, String password)
  {
    UserTable user = getUser(userName,password);
    if(user == null)
    {
      throw new RuntimeException("unable to verify user name and/or password");
    }
    
    user.delete();
  }
  
  public static UserTable verifyUser(String userName, String password)
  {
    if(!testVerifyUser(userName, password))
    {
      throw new RuntimeException(
          String.format(
              "incorrect user or password (user=%s)",
              userName));
    }
    
    return getUser(userName,password);
  }
  
  public static boolean testVerifyUser(String userName, String password)
  {
    UserTable user = getUser(userName,password);
    if(user == null)
    {
      return false;
    }
    
    return true;
  }
  
  private static UserTable getUser(String userName, String password)
  {
    UserTable table = new UserTable();
    
    UserTable object = 
        (UserTable)
        table.getObject(
            Restrictions.eq("userName", userName),
            Restrictions.eq("userPassword", password));
    
    return object;
  }
  
  public static void updateUser(
      String oldUserName, 
      String oldPassword,
      String newUserName,
      String newPassword
      )
  {
    UserTable object = getUser(oldUserName,oldPassword);
    
    if(object == null)
    {
      throw new RuntimeException(
          String.format(
              "user %s does not exist or password incorrect", 
              oldUserName));
    }
    
    object.delete();
    
    object.setUserName(newUserName);
    object.setUserPassword(newPassword);
    
    object.save();
  }
  
  public static long addUser(
      String userName, 
      String password
      )
  {
    UserTable table = new UserTable();
    
    UserTable object = 
        (UserTable)
        table.getObject(
            Restrictions.eq("userName", userName));
    
    if(object == null)
    {
      long userID = table.getUniqueLongFieldID("userID");
      
      object = new UserTable();
      object.setUserID(userID);
      object.setUserName(userName);
      object.setUserPassword(password);
      object.setPermissions(0);
      object.save();
      
      return userID;
    }
    
    throw new RuntimeException(
        String.format(
            "user %s already exists", 
            userName));
  }
  
  
  public UserTable()
  {
    super(UserTable.class);
  }

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public long getUserID()
  {
    return userID;
  }

  public void setUserID(long userID)
  {
    this.userID = userID;
  }

  public String getUserName()
  {
    return userName;
  }

  public void setUserName(String userName)
  {
    this.userName = userName;
  }

  public String getUserPassword()
  {
    return userPassword;
  }

  public void setUserPassword(String userPassword)
  {
    this.userPassword = userPassword;
  }

  public int getPermissions()
  {
    return permissions;
  }

  public void setPermissions(int permissions)
  {
    this.permissions = permissions;
  }
  
  
  
  
}

