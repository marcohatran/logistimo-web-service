package com.logistimo.stockrebalancing.entity;

import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by nitisha.khandelwal on 23/03/18.
 */

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"sourceStockRebalancingEvent", "destinationStockRebalancingEvent"})
@Table(name = "recommended_transfers")
public class RecommendedTransfer {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", unique = true, nullable = false, updatable = false)
  private String id;

  @NotNull
  @Column(name = "material_id", nullable = false)
  private Long materialId;

  @Column(name = "destination_event_id", insertable = false, updatable = false)
  private String destinationEventId;

  @NotNull
  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "source_event_id", insertable = false, updatable = false)
  private String sourceEventId;

  @NotNull
  @Column(name = "value")
  private BigDecimal value;

  @NotNull
  @Column(name = "cost")
  private BigDecimal cost;

  @NotNull
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private RecommendedTransferStatus status;

  @Column(name = "transfer_id")
  private Long transferId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(referencedColumnName = "id", name = "source_event_id")
  private StockRebalancingEvent sourceStockRebalancingEvent;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(referencedColumnName = "id", name = "destination_event_id")
  private StockRebalancingEvent destinationStockRebalancingEvent;


  public boolean isTransferInitiated() {
    return transferId != null;
  }
}
