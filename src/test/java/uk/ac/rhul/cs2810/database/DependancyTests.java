package uk.ac.rhul.cs2810.database;


import org.junit.jupiter.api.*;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("MenuDB")
class DependancyTests extends DatabaseTest{
  
  @Test
  void testTableCreatesDependancies() throws ConnectionError, ExecutionError {
    try {
      DatabaseFactory.getTestTableDB();
    } catch (ExecutionError EE) {
      if (EE.getCause().getMessage().contains("does not exist")){
        fail("Did not generate dependancies", EE);
      } else {
        throw EE;
      }
    }
  }
  
  @Test
  void testOrderCreatesDependancies() throws ConnectionError, ExecutionError {
    try {
      DatabaseFactory.getTestOrderDB();
    } catch (ExecutionError EE) {
      if (EE.getCause().getMessage().contains("does not exist")){
        fail("Did not generate dependancies", EE);
      } else {
        throw EE;
      }
    }
  }
}