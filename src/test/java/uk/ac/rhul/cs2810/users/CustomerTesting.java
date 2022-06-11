package uk.ac.rhul.cs2810.users;

import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.MissingDataError;
import uk.ac.rhul.cs2810.database.*;

import java.io.IOException;

public class CustomerTesting {
  
  public static void main(String[] args) throws IOException, ExecutionError, ConnectionError,
      MissingDataError, InterruptedException {
    LoginDB loginDB = DatabaseFactory.getLoginDB();
    Login login = new Login();
    loginDB.getID(LoginDB.hash(8149));
    login.setUserType('c');
  }
  
}
