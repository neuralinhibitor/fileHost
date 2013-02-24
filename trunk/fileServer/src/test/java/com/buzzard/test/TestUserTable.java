package com.buzzard.test;



import com.buzzard.db.models.UserTable;

import junit.framework.TestCase;



public class TestUserTable extends TestCase
{
  
  public TestUserTable(String name)
  {
    super(name);
  }
  
  public void testUserFeatures()
  {
    final String[] data = new String[]{
        "user112233333333","password1",
        "root999999","test",
        "user225192582159","259381058askdfklhJHHKHJHdf8wqt790475287156897"
    };
    
    
    for(int i=0; i<data.length; i+=2)
    {
      String user = data[i];
      String pass = data[i+1];
      
      final String newPass = "newPassword";
      final String wrongPass = "thisIsWrong";
      
      UserTable.addUser(user, pass);
      assertTrue(UserTable.testVerifyUser(user, pass));
      assertFalse(UserTable.testVerifyUser(user, wrongPass));
      
      UserTable.updateUser(user, pass, user, newPass);
      assertTrue(UserTable.testVerifyUser(user, newPass));
      assertFalse(UserTable.testVerifyUser(user, wrongPass));
      assertFalse(UserTable.testVerifyUser(user, pass));
      
      UserTable.updateUser(user, newPass, user, pass);
      assertTrue(UserTable.testVerifyUser(user, pass));
      assertFalse(UserTable.testVerifyUser(user, wrongPass));
      assertFalse(UserTable.testVerifyUser(user, newPass));
      
      UserTable.deleteUser(user, pass);
      assertFalse(UserTable.testVerifyUser(user, pass));
    }
  }
  
}


