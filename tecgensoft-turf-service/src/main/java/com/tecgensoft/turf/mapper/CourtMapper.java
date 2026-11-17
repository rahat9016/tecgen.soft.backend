package com.tecgensoft.turf.mapper;

import com.tecgensoft.turf.jpa.daos.CourtDao;
import com.tecgensoft.turf.service.domain.Court;
import com.tecgensoft.turf.service.domain.TurfCommands;
import java.util.UUID;

public final class CourtMapper {
  private CourtMapper() {}

  public static Court toEntity(CourtDao d) {
    return new Court(
        d.getId(),
        d.getOrganizationId(),
        d.getName(),
        d.getSport(),
        d.getSurface(),
        d.isIndoor(),
        d.getSize(),
        d.getImageUrl(),
        d.getRegularPrice(),
        d.getPeakPrice(),
        d.getPlayers(),
        d.getFeatures(),
        d.getDescription(),
        d.isActive());
  }

  public static CourtDao toDao(TurfCommands.Court r, UUID org) {
    var d = new CourtDao();
    d.setOrganizationId(org);
    apply(d, r);
    return d;
  }

  public static void apply(CourtDao d, TurfCommands.Court r) {
    d.setName(r.name());
    d.setSport(r.sport());
    d.setSurface(r.surface());
    d.setIndoor(r.indoor());
    d.setSize(r.size());
    d.setImageUrl(r.imageUrl());
    d.setRegularPrice(r.regularPrice());
    d.setPeakPrice(r.peakPrice());
    d.setPlayers(r.players());
    d.setFeatures(r.features());
    d.setDescription(r.description());
    d.setActive(r.active());
  }
}
