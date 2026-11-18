package com.tecgensoft.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {
  private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");
  private final JdbcTemplate jdbc;
  public record Product(UUID id,String code,String name,String description,boolean isActive) {}
  public record Feature(UUID id,String productCode,String code,String name,String description,String featureType,boolean isActive) {}
  public record PackageFeature(UUID id,UUID packageId,UUID featureId,String featureCode,String featureValue) {}

  public List<Product> products() {
    return jdbc.query("select id,code,name,description,is_active from products order by code",
        (rs,n)->new Product(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)));
  }
  public Product createProduct(String code,String name,String description) {
    String normalized=code.trim().toUpperCase(Locale.ROOT);
    if(!CODE.matcher(normalized).matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid product code");
    UUID id=UUID.randomUUID();
    try { jdbc.update("insert into products(id,code,name,description) values (?,?,?,?)",id,normalized,name.trim(),description); }
    catch(org.springframework.dao.DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Product code already exists"); }
    return new Product(id,normalized,name.trim(),description,true);
  }
  public Product productActive(String code,boolean active) {
    int n=jdbc.update("update products set is_active=?,updated_at=now() where code=?",active,code.toUpperCase(Locale.ROOT));
    if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
    return jdbc.queryForObject("select id,code,name,description,is_active from products where code=?",
        (rs,i)->new Product(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)),code.toUpperCase(Locale.ROOT));
  }
  public Product updateProduct(String code,String name,String description) {
    String normalized=code.toUpperCase(Locale.ROOT);
    int n=jdbc.update("update products set name=?,description=?,updated_at=now() where code=?",name.trim(),description,normalized);
    if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
    return jdbc.queryForObject("select id,code,name,description,is_active from products where code=?",
        (rs,i)->new Product(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)),normalized);
  }
  public List<Feature> features(String product) {
    String code=product.toUpperCase(Locale.ROOT);
    return jdbc.query("select id,product_code,code,name,description,feature_type,is_active from features where product_code=? order by code",
        (rs,n)->new Feature(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getBoolean(7)),code);
  }
  public Feature createFeature(String product,String feature,String name,String description,String type) {
    String productCode=product.toUpperCase(Locale.ROOT), code=feature.trim().toUpperCase(Locale.ROOT), featureType=type.toUpperCase(Locale.ROOT);
    if(!CODE.matcher(code).matches() || !List.of("BOOLEAN","NUMBER","STRING").contains(featureType))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid feature code or type");
    UUID id=UUID.randomUUID();
    try { jdbc.update("insert into features(id,product_code,code,name,description,feature_type) values (?,?,?,?,?,?)",id,productCode,code,name.trim(),description,featureType); }
    catch(org.springframework.dao.DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Feature code already exists for product"); }
    catch(org.springframework.dao.DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"); }
    return new Feature(id,productCode,code,name.trim(),description,featureType,true);
  }
  public Feature featureActive(UUID id,boolean active) {
    int n=jdbc.update("update features set is_active=?,updated_at=now() where id=?",active,id);
    if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Feature not found");
    return jdbc.queryForObject("select id,product_code,code,name,description,feature_type,is_active from features where id=?",
        (rs,i)->new Feature(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getBoolean(7)),id);
  }
  public Feature updateFeature(UUID id,String name,String description,String type) {
    if(type!=null && !List.of("BOOLEAN","NUMBER","STRING").contains(type.toUpperCase(Locale.ROOT)))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid feature type");
    int n=jdbc.update("update features set name=?,description=?,feature_type=coalesce(?,feature_type),updated_at=now() where id=?",
        name.trim(),description,type==null?null:type.toUpperCase(Locale.ROOT),id);
    if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Feature not found");
    return jdbc.queryForObject("select id,product_code,code,name,description,feature_type,is_active from features where id=?",
        (rs,i)->new Feature(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getBoolean(7)),id);
  }
  public List<PackageFeature> packageFeatures(UUID packageId) {
    return jdbc.query("select pf.id,pf.package_id,pf.feature_id,f.code,pf.feature_value::text from package_features pf join features f on f.id=pf.feature_id where pf.package_id=? order by f.code",
        (rs,n)->new PackageFeature(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getString(4),rs.getString(5)),packageId);
  }
  public PackageFeature setPackageFeature(UUID packageId,UUID featureId,JsonNode value) {
    var feature=jdbc.query("select f.code,f.feature_type,f.product_code from features f join products p on p.code=f.product_code where f.id=? and f.is_active",
        rs->rs.next()?Map.of("code",rs.getString(1),"type",rs.getString(2),"product",rs.getString(3)):null,featureId);
    if(feature==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active feature not found");
    String packageProduct=jdbc.query("select product from packages where id=?",rs->rs.next()?rs.getString(1):null,packageId);
    if(packageProduct==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Package not found");
    if(!packageProduct.equals(feature.get("product"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Feature and package products differ");
    String type=feature.get("type");
    if(value==null || ("BOOLEAN".equals(type)&&!value.isBoolean()) || ("NUMBER".equals(type)&&!value.isNumber()) || ("STRING".equals(type)&&!value.isTextual()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Feature value does not match its type");
    UUID id=jdbc.query("select id from package_features where package_id=? and feature_id=?",rs->rs.next()?rs.getObject(1,UUID.class):null,packageId,featureId);
    if(id==null) id=UUID.randomUUID();
    jdbc.update("insert into package_features(id,package_id,feature_id,feature_value) values (?,?,?,?::jsonb) on conflict(package_id,feature_id) do update set feature_value=excluded.feature_value,updated_at=now()",
        id,packageId,featureId,value.toString());
    String code=feature.get("code");
    if("BOOLEAN".equals(type)) jdbc.update("update packages set features=case when ? then features||to_jsonb(?::text) else features-? end,updated_at=now() where id=?",
        value.booleanValue(),code,code,packageId);
    else if("NUMBER".equals(type)) jdbc.update("update packages set limits=jsonb_set(limits,array[?],?::jsonb,true),updated_at=now() where id=?",code,value.toString(),packageId);
    return jdbc.queryForObject("select pf.id,pf.package_id,pf.feature_id,f.code,pf.feature_value::text from package_features pf join features f on f.id=pf.feature_id where pf.package_id=? and pf.feature_id=?",
        (rs,n)->new PackageFeature(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getString(4),rs.getString(5)),packageId,featureId);
  }
  public void removePackageFeature(UUID packageId,UUID featureId) {
    var row=jdbc.query("select f.code,f.feature_type from package_features pf join features f on f.id=pf.feature_id where pf.package_id=? and pf.feature_id=?",
        rs->rs.next()?Map.of("code",rs.getString(1),"type",rs.getString(2)):null,packageId,featureId);
    if(row==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Package feature mapping not found");
    jdbc.update("delete from package_features where package_id=? and feature_id=?",packageId,featureId);
    if("BOOLEAN".equals(row.get("type"))) jdbc.update("update packages set features=features-?,updated_at=now() where id=?",row.get("code"),packageId);
    else if("NUMBER".equals(row.get("type"))) jdbc.update("update packages set limits=limits-?,updated_at=now() where id=?",row.get("code"),packageId);
  }
}
