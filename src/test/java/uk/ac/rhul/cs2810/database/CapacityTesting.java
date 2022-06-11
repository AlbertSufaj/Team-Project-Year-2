package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.*;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;

@Tag("Capacity")
class CapacityTesting extends DatabaseTest {
  
  @BeforeAll
  static void setUp() {
    tables = new String[]{};
  }
  
  @Test
  @Tag("Slow")
  void testGetConnectionTooManyConnections() throws ConnectionError, SQLException {
    Connection[] connections = new Connection[100];
    assertThrows(ConnectionError.class, () -> {
      for (int i = 0; i < connections.length; i++) {
        connections[i] = getStatement().getConnection();
      }
    });
    for (Connection connection : connections) {
      if (connection == null) {
        break;
      }
      Database.closeConnection(connection);
    }
  }
  
  @Test
  @Tag("Slow")
  void testImageCaching() throws ConnectionError, ExecutionError {
    ImageDB imageDB = DatabaseFactory.getTestImageDB();
    long startTime = System.nanoTime();
    imageDB.getImage(1);
    long endTime = System.nanoTime();
    long firstDuration = (endTime - startTime);
    
    startTime = System.nanoTime();
    imageDB.getImage(1);
    endTime = System.nanoTime();
    long secondDuration = (endTime - startTime);
    assertThat(2*secondDuration, lessThan(firstDuration));
  }
}