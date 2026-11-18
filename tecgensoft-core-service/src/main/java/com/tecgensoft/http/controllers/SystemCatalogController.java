package com.tecgensoft.http.controllers;

import com.tecgensoft.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system")
@Tag(name="Product and Feature Catalog",description="Platform product, feature, and package feature configuration")
public class SystemCatalogController {
  private final CatalogService service;
  private final com.tecgensoft.service.PermissionAuthorizationService authorization;
  public record CreateProduct(@NotBlank String code,@NotBlank String name,String description) {}
  public record CreateFeature(@NotBlank String code,@NotBlank String name,String description,@NotBlank String featureType) {}
  public record UpdateCatalogItem(@NotBlank String name,String description,String featureType) {}
  public record ActiveRequest(@NotNull Boolean active) {}
  public record FeatureValue(@NotNull com.fasterxml.jackson.databind.JsonNode value) {}

  @GetMapping("/products")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PRODUCT_VIEW')")
  public List<CatalogService.Product> products(){return service.products();}
  @PostMapping("/products")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PRODUCT_CREATE')")
  public CatalogService.Product createProduct(@Valid @RequestBody CreateProduct body){return service.createProduct(body.code(),body.name(),body.description());}
  @PatchMapping("/products/{code}")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PRODUCT_UPDATE')")
  public CatalogService.Product updateProduct(@PathVariable String code,@Valid @RequestBody UpdateCatalogItem body){return service.updateProduct(code,body.name(),body.description());}
  @PatchMapping("/products/{code}/activation")
  public CatalogService.Product productActive(@PathVariable String code,@Valid @RequestBody ActiveRequest body,
      org.springframework.security.core.Authentication authentication){
    String permission=body.active()?"PRODUCT_ACTIVATE":"PRODUCT_DEACTIVATE";
    if(!authorization.hasPermission(authentication,permission)) throw new org.springframework.security.access.AccessDeniedException("Platform permission required");
    return service.productActive(code,body.active());
  }
  @GetMapping("/products/{product}/features")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'FEATURE_VIEW')")
  public List<CatalogService.Feature> features(@PathVariable String product){return service.features(product);}
  @PostMapping("/products/{product}/features")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'FEATURE_CREATE')")
  public CatalogService.Feature createFeature(@PathVariable String product,@Valid @RequestBody CreateFeature body){return service.createFeature(product,body.code(),body.name(),body.description(),body.featureType());}
  @PatchMapping("/features/{featureId}")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'FEATURE_UPDATE')")
  public CatalogService.Feature updateFeature(@PathVariable UUID featureId,@Valid @RequestBody UpdateCatalogItem body){return service.updateFeature(featureId,body.name(),body.description(),body.featureType());}
  @PatchMapping("/features/{featureId}/activation")
  public CatalogService.Feature featureActive(@PathVariable UUID featureId,@Valid @RequestBody ActiveRequest body,
      org.springframework.security.core.Authentication auth){
    if(!authorization.hasPermission(auth,body.active()?"FEATURE_ACTIVATE":"FEATURE_DEACTIVATE"))
      throw new org.springframework.security.access.AccessDeniedException("Platform permission required");
    return service.featureActive(featureId,body.active());
  }
  @GetMapping("/packages/{packageId}/features")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_VIEW')")
  public List<CatalogService.PackageFeature> packageFeatures(@PathVariable UUID packageId){return service.packageFeatures(packageId);}
  @PatchMapping("/packages/{packageId}/features/{featureId}")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_UPDATE')")
  public CatalogService.PackageFeature setPackageFeature(@PathVariable UUID packageId,@PathVariable UUID featureId,
      @Valid @RequestBody FeatureValue body){return service.setPackageFeature(packageId,featureId,body.value());}
  @DeleteMapping("/packages/{packageId}/features/{featureId}")
  @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_UPDATE')")
  public void removePackageFeature(@PathVariable UUID packageId,@PathVariable UUID featureId){service.removePackageFeature(packageId,featureId);}
}
