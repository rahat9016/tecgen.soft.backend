package com.tecgensoft.turf.service;

import com.tecgensoft.turf.http.dtos.responses.CourtResponse;
import com.tecgensoft.turf.mapper.CourtMapper;
import com.tecgensoft.turf.service.domain.Court;
import com.tecgensoft.turf.service.domain.TurfCommands;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the court API. Owns request orchestration and response shaping, and
 * delegates the locking-critical persistence to {@link TurfService}, the shared turf engine, so the
 * court slice follows the Controller -> DTO -> Service -> Mapper -> Repository layering without
 * duplicating its advisory/pessimistic locking.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourtService {
  private final TurfService turf;

  public List<CourtResponse> list(TurfService.Context context, boolean publicOnly) {
    @SuppressWarnings("unchecked")
    List<Court> courts = (List<Court>) turf.courts(context, publicOnly);
    return courts.stream().map(CourtResponse::from).toList();
  }

  public CourtResponse get(TurfService.Context context, UUID id) {
    return CourtResponse.from(CourtMapper.toEntity(turf.court(context, id)));
  }

  public CourtResponse create(TurfService.Context context, TurfCommands.Court command) {
    return CourtResponse.from((Court) turf.createCourt(context, command));
  }

  public CourtResponse update(TurfService.Context context, UUID id, TurfCommands.Court command) {
    return CourtResponse.from((Court) turf.updateCourt(context, id, command));
  }

  public void delete(TurfService.Context context, UUID id) {
    turf.deleteCourt(context, id);
  }
}
