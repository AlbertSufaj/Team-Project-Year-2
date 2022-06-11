package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.*;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.containers.Order;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseTest {
  
  static String URL;
  static String userName;
  static String password;
  static boolean connection = true;
  static Item item;
  static String[] tables;
  static LoginDB testLogin;
  static Statement st;
  static Order order;
  static OrderDB orderDB;
  static TableDB tableDB;
  
  @BeforeAll
  static void getConnectionInfo() throws ConnectionError, SQLException {
    String configPos = "src/test/resources/Config.txt";
  
    item = new Item(1, "taco", "close but not quite edible", 2, 30,
        ItemCategory.BURRITOS, false, false, false, 10, 1);
    
    try {
      String[] connectionData = Database.getConfigInfo(configPos);
      URL = connectionData[0];
      userName = connectionData[1];
      password = connectionData[2];
    } catch (ConnectionError CE) {
      connection = false;
    }
    
    Statement st = getStatement();
    st.execute("CREATE SCHEMA IF NOT EXISTS public;");
    st.execute("GRANT ALL ON SCHEMA public TO postgres;");
    st.execute("GRANT ALL ON SCHEMA public TO public;");
    DatabaseFactory.reset();
    st.getConnection().close();
  }
  
  @BeforeEach
  void setConfig() throws IOException, SQLException, ConnectionError, ExecutionError {
    changeConfigLine(3, "Menu Format = \"S\"");
    
    st = getStatement();
    
    for (String table : tables) {
      st.execute("DROP TABLE IF EXISTS " + table + " CASCADE;");
    }
    st.execute("DROP TABLE IF EXISTS menu CASCADE;");
    DatabaseFactory.reset();
    
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    for (int id: loginDB.getLoggedInWaiters()){
      loginDB.logOut(id);
    }
    order = new Order(1);
    order.addItemToOrder(item);
  }
  
  void changeConfigLine(int lineNum, String newLine) throws IOException {
    BufferedReader configFile = new BufferedReader(new FileReader("src/test/resources/Config.txt"));
    String line = configFile.readLine();
    List<String> lines = new LinkedList<>();
    while (line != null) {
      lines.add(line);
      line = configFile.readLine();
    }
    lines.set(lineNum, newLine);
    configFile.close();
    
    BufferedWriter newConfigFile = new BufferedWriter(new FileWriter("src/test/resources/Config.txt"));
    newConfigFile.write("");
    for (String l : lines) {
      newConfigFile.append(l + "\n");
    }
    newConfigFile.close();
  }
  
  @AfterAll
  @BeforeAll
  static void printConnectionCount() throws SQLException, ConnectionError {
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT sum(numbackends) FROM pg_stat_database;");
    rs.next();
    System.out.println(rs.getInt(1));
    Database.closeConnection(st.getConnection());
  }
  
  @AfterAll
  public static void clearTables() throws SQLException, ConnectionError, ExecutionError {
    Statement st = getStatement();
    st.execute("DROP SCHEMA public CASCADE;");
    st.execute("CREATE SCHEMA public;");
    st.execute("GRANT ALL ON SCHEMA public TO postgres;");
    st.execute("GRANT ALL ON SCHEMA public TO public;");
    DatabaseFactory.reset();
    st.getConnection().close();
  }
  
  @AfterEach
  void closeConnections() throws SQLException {
    Database.closeConnection(st.getConnection());
  }
  
  static Statement getStatement() throws ConnectionError {
    return Database.getStatement(URL, userName, password);
  }
  
  void testExecutionError(Exception e, String message) {
    assertEquals(ExecutionError.class, e.getClass());
    assertThat(e.getMessage(), containsString(message));
  }
}
