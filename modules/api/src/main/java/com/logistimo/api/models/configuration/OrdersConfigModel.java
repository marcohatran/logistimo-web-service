/*
 * Copyright © 2018 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

package com.logistimo.api.models.configuration;

import java.util.List;

/**
 * Created by naveensnair on 24/11/14.
 */
public class OrdersConfigModel {
  public String og; // ordergeneration
  public boolean agi; // auto goods issue on order shipped
  public boolean agr; // auto goods receipt on order fulfilled
  public boolean tm; // transporter mandatory
  public boolean tiss; // transporter details in status sms
  public boolean ao; // allow orders with no items
  public boolean aof; // allow mark order as fulfilled
  public String po; // payment options
  public String ps; // package sizes
  public String vid; // vendor Id
  public boolean eex; // enable export
  public String et; // export times
  public String an; // export user ids
  public String ip; // isPublic
  public String bn; // banner
  public String hd; // heading
  public String cp; // copyright
  public String url; // public url
  public boolean spb; // show stock levels on public board
  public String createdBy;// last update made by this user
  public String lastUpdated;// last updated time
  public String fn; // first name
  public boolean dop; //disable order pricing
  public boolean enOrdRsn; // enable recommended order reasons
  public String orsn; // recommended order reasons
  public boolean md; // Mark reason field as mandatory
  public List<String> usrTgs; // user tags for orders export schedule
  public boolean aoc; // Allow marking order as confirmed
  public boolean asc; //Allocate stock on confirmation
  public boolean tr; //Rename Transfer orders as Release orders
  public String orr; // Order Recommendation Reasons
  public boolean orrm; // Mark Order recommendation reasons mandatory
  public String eqr; // Editing quantity Reasons
  public boolean eqrm; // Mark Editing quantity Reasons mandatory
  public String psr; // Partial shipment Reasons
  public boolean psrm; // Mark Partial Shipment reasons mandatory
  public String pfr; // Partial fulfillment Reasons
  public boolean pfrm; // Mark partial fulfillment reasons mandatory
  public String cor; // Cancelling order Reasons
  public boolean corm; // Mark cancelling order reasons mandatory
  public boolean acs; // Allow creating shipments
  public boolean aafmsc; // Auto assign first material status to items, when order is confirmed
  private boolean shipOnPickup; // Automatically mark an order as 'Shipped' on pick up of a consignment
  private boolean fulfillOnDelivery; // Automatically mark an order as 'Fulfilled' on delivery of a consignment.
  private boolean disableDeliveryReqByCustomer; // Disable the request for delivery by a customer.
  private String logo;
  private String logoName;
  private String invoiceTemplate;
  private String invoiceTemplateName;
  private String shipmentTemplate;
  private String shipmentTemplateName;
  private String logoDownloadLink;
  private String invoiceTemplateDownloadLink;
  private String shipmentTemplateDownloadLink;
  /**
   * Create order automatically, when min is hit or prediction less than some days
   */
  public boolean autoCreate;
  /**
   * Create orders, when inventory hits min
   */
  public boolean autoCreateOnMin;
  /**
   * Create orders only for these material tags
   */
  public List<String> autoCreateMaterialTags;
  /**
   * Create orders with predicted days of stock <= this value.
   */
  public int pdos;
  /**
   * Create orders only for these store types
   */
  public List<String> autoCreateEntityTags;
  /**
   * Mark reference id mandatory
   */
  private boolean ridm;
  /**
   * Mark estimated arrival date mandatory
   */
  private boolean eadm;
  /**
   * Mark purchase reference id mandatory
   */
  private boolean purchase;
  /**
   * Mark transfer/release reference id  mandatory
   */
  private boolean transfer;

  /**
   * At least one transport service provider is present
   */
  public boolean tspc;

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getLogoName() {
    return logoName;
  }

  public void setLogoName(String logoName) {
    this.logoName = logoName;
  }

  public String getInvoiceTemplate() {
    return invoiceTemplate;
  }

  public void setInvoiceTemplate(String invoiceTemplate) {
    this.invoiceTemplate = invoiceTemplate;
  }

  public String getInvoiceTemplateName() {
    return invoiceTemplateName;
  }

  public void setInvoiceTemplateName(String invoiceTemplateName) {
    this.invoiceTemplateName = invoiceTemplateName;
  }

  public String getShipmentTemplate() {
    return shipmentTemplate;
  }

  public void setShipmentTemplate(String shipmentTemplate) {
    this.shipmentTemplate = shipmentTemplate;
  }

  public String getShipmentTemplateName() {
    return shipmentTemplateName;
  }

  public void setShipmentTemplateName(String shipmentTemplateName) {
    this.shipmentTemplateName = shipmentTemplateName;
  }

  public String getLogoDownloadLink() {
    return logoDownloadLink;
  }

  public void setLogoDownloadLink(String logoDownloadLink) {
    this.logoDownloadLink = logoDownloadLink;
  }

  public String getInvoiceTemplateDownloadLink() {
    return invoiceTemplateDownloadLink;
  }

  public void setInvoiceTemplateDownloadLink(String invoiceTemplateDownloadLink) {
    this.invoiceTemplateDownloadLink = invoiceTemplateDownloadLink;
  }

  public String getShipmentTemplateDownloadLink() {
    return shipmentTemplateDownloadLink;
  }

  public void setShipmentTemplateDownloadLink(String shipmentTemplateDownloadLink) {
    this.shipmentTemplateDownloadLink = shipmentTemplateDownloadLink;
  }

  public boolean isReferenceIdMandatory() { return  ridm; }
  public void setReferenceIdMandatory(boolean ridm) { this.ridm = ridm; }

  public boolean isExpectedArrivalDateMandatory() { return eadm; }
  public void setExpectedArrivalDateMandatory(boolean eadm) { this.eadm = eadm; }

  public boolean isPurchaseReferenceIdMandatory() {
    return purchase;
  }

  public void setPurchaseReferenceIdMandatory(boolean purchase) {
    this.purchase = purchase;
  }

  public boolean isTransferReferenceIdMandatory() {
    return transfer;
  }

  public void setTransferReferenceIdMandatory(boolean transfer) {
    this.transfer = transfer;
  }

  public boolean getMarkShippedOnPickup() {
    return shipOnPickup;
  }

  public void setMarkShippedOnPickup(boolean flag) {
    this.shipOnPickup = flag;
  }

  public boolean getMarkFulfilledOnDelivery() {
    return fulfillOnDelivery;
  }

  public void setMarkFulfilledOnDelivery(boolean flag) {
    this.fulfillOnDelivery = flag;
  }

  public boolean getDisableDeliveryRequestByCustomer() {
    return disableDeliveryReqByCustomer;
  }

  public void setDisableDeliveryRequestByCustomer(boolean flag) {
    this.disableDeliveryReqByCustomer = flag;
  }
}
