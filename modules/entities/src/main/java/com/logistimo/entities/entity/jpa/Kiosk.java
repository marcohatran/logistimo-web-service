package com.logistimo.entities.entity.jpa;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Data;

/**
 * Created by nitisha.khandelwal on 27/03/18.
 */

@Data
@Entity
@Table(name = "KIOSK")
public class Kiosk {

  @Id
  @Column(name = "KIOSKID", insertable = false, updatable = false)
  private Long kioskId;

  @Column(name = "CITY")
  private String city;

  @Column(name = "DISTRICT")
  private String district;

  @Column(name = "STATE")
  private String state;

  @Column(name = "NAME")
  private String name;

  @Column(name = "SDID")
  private String sourceDomainId;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "KIOSKID_OID", referencedColumnName = "KIOSKID")
  @NotFound(action = NotFoundAction.IGNORE)
  private Set<KioskDomain> domains;

  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "KIOSKID", referencedColumnName = "KIOSKID")
  @NotFound(action = NotFoundAction.IGNORE)
  private Set<KioskTag> tags;
}
