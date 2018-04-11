package com.logistimo.stockrebalancing.specifications;

import com.logistimo.entities.entity.jpa.Kiosk;
import com.logistimo.entities.entity.jpa.KioskDomain;
import com.logistimo.entities.entity.jpa.KioskTag;
import com.logistimo.materials.entity.jpa.Material;
import com.logistimo.materials.entity.jpa.MaterialTag;
import com.logistimo.stockrebalancing.entity.ExecutionMetadata;
import com.logistimo.stockrebalancing.entity.StockRebalancingEvent;
import com.logistimo.stockrebalancing.entity.TriggerPriority;

import org.springframework.data.jpa.domain.Specification;

import java.util.Calendar;

import javax.persistence.criteria.Join;

/**
 * Created by nitisha.khandelwal on 28/03/18.
 */

public class StockRebalancingEventSpecifications {

  private static final String MATERIAL_ID = "materialId";
  private static final String KIOSK_ID = "kioskId";
  private static final String TRIGGER_SHORT_CODE = "shortCode";
  private static final String DOMAIN_ID = "domainId";

  public static Specification<StockRebalancingEvent> materialIdIs(Long materialId) {

    if (materialId == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get(MATERIAL_ID), materialId);
  }

  @SuppressWarnings("Duplicates")
  public static Specification<StockRebalancingEvent> triggerShortCodeIs(String shortCode) {

    if (shortCode == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get(TRIGGER_SHORT_CODE), shortCode);
  }

  @SuppressWarnings("Duplicates")
  public static Specification<StockRebalancingEvent> kioskIdIs(Long kioskId) {

    if (kioskId == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get(KIOSK_ID), kioskId);
  }


  @SuppressWarnings("Duplicates")
  public static Specification<StockRebalancingEvent> domainIdIs(Long domainId) {

    if (domainId == null) {
      return null;
    }
    return (root, query, criteriaBuilder) -> {
      Join<Kiosk, KioskDomain> domains = root.join("kiosk").join("domains");
      return criteriaBuilder.equal(domains.get(DOMAIN_ID), domainId);
    };
  }

  public static Specification<StockRebalancingEvent> eventIsActive() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    return (root, query, criteriaBuilder) -> {
      Join<StockRebalancingEvent, ExecutionMetadata>
          eventExecutionMetadataJoin =
          root.join("executionMetadata");
      return criteriaBuilder
          .and(criteriaBuilder.greaterThanOrEqualTo(eventExecutionMetadataJoin.get("startTime"),
                  calendar.getTime()),
              criteriaBuilder.equal(eventExecutionMetadataJoin.get("status"), "sc"));
    };
  }

  public static Specification<StockRebalancingEvent> kioskTagIs(String kioskTag) {

    if (kioskTag == null) {
      return null;
    }
    return (root, query, cb) -> {
      Join<Kiosk, KioskTag> tags = root.join("kiosk").join("tags").join("tag");
      return cb.equal(tags.get("name"), kioskTag);
    };
  }

  public static Specification<StockRebalancingEvent> materialTagIs(String materialTag) {

    if (materialTag == null) {
      return null;
    }
    return (root, query, cb) -> {
      Join<Material, MaterialTag> tags = root.join("material").join("tags").join("tag");
      return cb.equal(tags.get("name"), materialTag);
    };
  }

  public static Specification<StockRebalancingEvent> eventIsPrimary() {
    return (root, query, cb) -> cb
        .equal(root.get("triggerPriority"), TriggerPriority.PRIMARY);
  }

}
