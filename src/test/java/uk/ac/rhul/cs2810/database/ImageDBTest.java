package uk.ac.rhul.cs2810.database;

import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;


import java.sql.SQLException;

@Tag("ImageDB")
class ImageDBTest extends DatabaseTest {
  
  @BeforeAll
  static void setup() {
    tables = new String[]{"images"};
  }
  
  @Test
  void testGetImage() throws ConnectionError, ExecutionError {
    ImageDB imageDB = DatabaseFactory.getTestImageDB();
    Image image = imageDB.getImage(1);
  }
  
  @Test
  void testGetImageNotFound() throws ConnectionError, ExecutionError {
    ImageDB imageDB = DatabaseFactory.getTestImageDB();
    Image image = imageDB.getImage(-1);
  }
  
  @Test
  @Tag("Prod")
  void testGetProdImageDatabase() throws ConnectionError, ExecutionError, SQLException {
    st = getStatement();
    for (String table : new String[] {"allergensInItems", "menu", "allergens"}) {
      st.execute("DROP TABLE IF EXISTS " + table + " CASCADE;");
    }
    DatabaseFactory.reset();
    Database.closeConnection(st.getConnection());
    
    
    ImageDB imageDB = DatabaseFactory.getImageDB();
  }
}