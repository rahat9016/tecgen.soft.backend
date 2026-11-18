package com.tecgensoft;

import com.tecgensoft.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class CatalogServiceTest {
  @Test void rejectsInvalidProductCodeBeforePersistence() {
    var service=new CatalogService(null);
    assertThrows(ResponseStatusException.class,()->service.createProduct("bad code","Product",null));
  }
  @Test void rejectsUnsupportedFeatureTypeBeforePersistence() {
    var service=new CatalogService(null);
    assertThrows(ResponseStatusException.class,()->service.createFeature("TURF","NEW_FEATURE","Feature",null,"OBJECT"));
  }
}
