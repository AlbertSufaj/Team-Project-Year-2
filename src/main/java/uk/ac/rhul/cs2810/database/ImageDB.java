package uk.ac.rhul.cs2810.database;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.containers.Item;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The object for interacting with the image storage part of the database.
 */
public class ImageDB extends Database {
  
  private final Map<Integer, Image> imageCache;
  private Image imageNotFoundImage;
  
  /**
   * Instantiates a new imageDB.
   *
   * @param testing should this connect to the test database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to setup the tables
   */
  ImageDB(boolean testing) throws ConnectionError, ExecutionError {
    super(new String[]{"images"}, testing);
    
    insertStatments.put("images", "INSERT INTO images VALUES (?,?);");
    layouts.put("images", new char[]{'i', 'B'});
    
    imageCache = new HashMap<>();
    
    Statement st = null;
    try {
      st = getStatement();
      ResultSet rs = st.executeQuery("SELECT count(*) FROM images;");
      rs.next();
      if (rs.getInt(1) == 0) {
        populateTable();
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not setup table", SQLE);
    } finally {
      try {
        if (st != null) {
          closeConnection(st.getConnection());
        }
      } catch (SQLException SQLE) {
        // Not worried by closing issues
      }
    }
  }
  
  /**
   * Gets the image of a given menu item.
   *
   * @param itemID the item id
   * @return the image
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to get the image from the database
   */
  public Image getImage(int itemID) throws ConnectionError, ExecutionError {
    if (imageCache.containsKey(itemID)) {
      return imageCache.get(itemID);
    }
    
    Connection connection = getConnection();
    InputStream imageInput;
    String path = null;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT image FROM images" +
          " WHERE ItemID = ?");
      ps.setInt(1, itemID);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        connection.close();
        if (imageNotFoundImage == null) {
          InputStream stream = getFileAsStream("menu_images/Image_Not_Found.png");
          imageNotFoundImage = new Image(stream);
        }
        return imageNotFoundImage;
      }
      imageInput = rs.getBinaryStream(1);
    } catch (SQLException SQLE) {
      closeConnection(connection);
      throw new ExecutionError("Image could not be fetched", SQLE);
    } catch (FileNotFoundException FNFE) {
      throw new ExecutionError("Could not find image not found image", FNFE);
    }
    
    BufferedImage bufferedImage;
    try {
      bufferedImage = ImageIO.read(imageInput);
    } catch (IOException IOE) {
      throw new ExecutionError("Could not read image");
    } finally {
      closeConnection(connection);
    }
    
    Image finalImage = SwingFXUtils.toFXImage(bufferedImage, null);
    imageCache.put(itemID, finalImage);
    return finalImage;
  }
  
  private void populateTable() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getMenuDB();
    List<String> files = new LinkedList<>();
    List<Item> menu = menuDB.getMenu();
    
    for (Item item : menu) {
      files.add(String.format("%1$" + 4 + "s", item.getID()).replace(' ', '0')
          + ".jpg");
    }
    
    Connection connection = getConnection();
    
    String imagePath = "images/";
    System.out.println("Test image = " + files.get(0));
    try {
      if (getFileAsStream(imagePath + files.get(0)) == null) {
        throw new FileNotFoundException(); // tries next option
      }
    } catch (FileNotFoundException FNFE) {
      try {
        if (getFileAsStream(files.get(0)) == null) {
          throw new FileNotFoundException();
        }
        imagePath = "";
      } catch (FileNotFoundException fnfe) {
        throw new ExecutionError("Cannot load images",
            new FileNotFoundException("Cannot find image folder"));
      }
    }
    
    System.out.println("Image path = " + imagePath);
    
    for (String imageName : files) {
      try {
        PreparedStatement ps =
            inputIntoTable("images",
                new String[]{imageName.split("\\.")[0], // Gets the number
                    imagePath + imageName}, connection);
        ps.executeBatch();
      } catch (SQLException SQLE) {
        closeConnection(connection);
        throw new ExecutionError("Couldn't input into images", SQLE);
      } catch (ExecutionError EE) {
        if (EE.getCause().getClass() == FileNotFoundException.class) {
          System.err.println("Cannot find image for item "+imageName.substring(0, 4));
        } else {
          throw EE;
        }
      }
    }
    
    closeConnection(connection);
  }
  
  @Override
  protected void makeTables() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    
    try {
      st.execute("CREATE TABLE IF NOT EXISTS images(" +
          "ItemID int PRIMARY KEY," +
          "Image bytea," +
          "FOREIGN KEY (ItemID) REFERENCES menu(ItemID)" +
          ");");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make table images", SQLE);
    } finally {
      try {
        closeConnection(st.getConnection());
      } catch (SQLException SQLE) {
        // Not worried with closing errors
      }
    }
  }
}
