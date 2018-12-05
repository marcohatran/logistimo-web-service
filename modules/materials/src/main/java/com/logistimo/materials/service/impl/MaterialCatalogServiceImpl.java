/*
 * Copyright © 2017 Logistimo.
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

/**
 *
 */
package com.logistimo.materials.service.impl;

import com.logistimo.AppFactory;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.domains.utils.EntityRemover;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.exceptions.EventGenerationException;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.logger.XLog;
import com.logistimo.materials.dao.IMaterialDao;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.entity.IMaterialManufacturers;
import com.logistimo.materials.entity.Material;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.ICounter;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.tags.TagUtil;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.utils.Counter;
import com.logistimo.utils.StringUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;


/**
 * @author juhee
 */
@Service
public class MaterialCatalogServiceImpl implements MaterialCatalogService {

  private static final XLog xLogger = XLog.getLog(MaterialCatalogServiceImpl.class);

  private IMaterialDao materialDao;
  private ITagDao tagDao;

  @Autowired
  public void setMaterialDao(IMaterialDao materialDao) {
    this.materialDao = materialDao;
  }

  @Autowired
  public void setTagDao(ITagDao tagDao) {
    this.tagDao = tagDao;
  }

  /* (non-Javadoc)
         * @see org.lggi.samaanguru.service.MaterialCatalogService#addMaterial(org.lggi.samaanguru.entity.Material)
	 */

  @SuppressWarnings("unchecked")
  public Long addMaterial(Long domainId, IMaterial material) throws ServiceException {
    xLogger.fine("Entering addMaterial");
    //Assuming that all other fields including registeredBy is set by the calling function
    //Set the timestamp to now
    Date now = new Date();
    material.setTimeStamp(now);
    material.setLastUpdated(now);
    material.setDomainId(domainId);
    material.setName(StringUtil.getTrimmedName(material.getName()));
    if (materialDao.checkMaterialExists(domainId, material.getUniqueName())) {
      xLogger.warn("addMaterial: Material with name {0} already exists", material.getName());
      final Locale locale = SecurityUtils.getLocale();
      ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
      throw new ServiceException(
          backendMessages.getString("error.cannotadd") + ". '" + material.getName() + "' "
              + backendMessages.getString("error.alreadyexists") + ".");
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      // Add a short-code to the material
      material.setShortCode(getMaterialShortCode(domainId));
      // Check if another material by the same name exists in the database
      Query query = pm.newQuery(JDOUtils.getImplClass(IMaterial.class));
      query.setFilter("dId.contains(domainIdParam) && uName == unameParam");
      query.declareParameters("Long domainIdParam, String unameParam");
      try {
        List<IMaterial>
            results =
            (List<IMaterial>) query.execute(domainId, material.getUniqueName());
        if (results != null && results.size() > 0) {
          // Material with this name already exists in the database!
          xLogger.warn("addMaterial: Material with name {0} already exists", material.getName());
          final Locale locale = SecurityUtils.getLocale();
          ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
          throw new ServiceException(
              backendMessages.getString("error.cannotadd") + ". '" + material.getName() + "' "
                  + backendMessages.getString("error.alreadyexists") + ".");
        }
      } finally {
        query.closeAll();
      }
      // Check if custom ID is specified for the material. If yes, check if the specified custom ID already exists.
      boolean customIdExists = false;
      if (material.getCustomId() != null && !material.getCustomId().isEmpty()) {
        customIdExists =
            materialDao.checkCustomIdExists(material.getDomainId(), material.getCustomId());
      }
      if (customIdExists) {
        // The specified custom ID already exists in the database!
        xLogger.warn("addMaterial: FAILED!! Cannot add material {0}. Custom ID {1} already exists.",
            material.getName(), material.getCustomId());
        final Locale locale = SecurityUtils.getLocale();
        ResourceBundle messages = Resources.get().getBundle("Messages", locale);
        ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
        throw new ServiceException(
            backendMessages.getString("error.cannotadd") + ". " + messages.getString("customid")
                + " " + material.getCustomId() + " " + backendMessages
                .getString("error.alreadyexists") + ".");
      }

      if (material.getTags() != null) {
        material.setTgs(tagDao.getTagsByNames(material.getTags(), ITag.MATERIAL_TAG));
      }

      // Add this object to other domains and persist (superdomains)
      material =
          (IMaterial) DomainsUtil.addToDomain(material, domainId,
              pm);   /// earlier: material = pm.makePersistent(material);
      // Increment counter
      PersistenceManager tagsPm = PMF.get().getPersistenceManager();
      try {
        List<Long> domainIds = material.getDomainIds();
        incrementMaterialCounter(domainIds, 1,
            tagsPm); // increment counter is now done within addToDomain across all domains (superdomains)

      } finally {
        // Close PM
        tagsPm.close();
      }
    } finally {
      pm.close();
    }

    try {
      EventPublisher.generate(domainId, IEvent.CREATED, null,
          JDOUtils.getImplClass(IMaterial.class).getName(),
          materialDao.getKeyString(material.getMaterialId()),
          null);
    } catch (EventGenerationException e) {
      xLogger.warn(
          "Exception when generating event for material-creation for material {0} in domain {1}: {2}",
          material.getMaterialId(), domainId, e.getMessage());
    }
    xLogger.fine("Exiting addMaterial");
    return material.getMaterialId();
  }

	/* (non-Javadoc)
         * @see org.lggi.samaanguru.service.MaterialCatalogService#updateMaterial(org.lggi.samaanguru.entity.Material)
	 */

  public void updateMaterial(IMaterial material, Long domainId) throws ServiceException {
    xLogger.fine("Entering updateMaterial");
    boolean materialExists = true;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    //We use an atomic transaction here to check if the user already exists, and if not, create it
    List<String> oldTags = null;
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      try {
        //First check if the material already exists in the database
        IMaterial mat = JDOUtils.getObjectById(IMaterial.class, material.getMaterialId(), pm);
        if (!domainId.equals(mat.getDomainId())) {
          final Locale locale = SecurityUtils.getLocale();
          ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
          throw new ServiceException(
              backendMessages.getString("material.updation.permission.denied") + " : " + material
                  .getName());
        }
        //If we get here, it means the material exists
        material.setName(StringUtil.getTrimmedName(material.getName()));
        if (!mat.getName().equals(material.getName())) {
          mat.setName(material.getName());
        }
        mat.setDescription(material.getDescription());
        mat.setIdentifierType(material.getIdentifierType());
        mat.setIdentifierValue(material.getIdentifierValue());
        mat.setImagePath(material.getImagePath());
        mat.setSeasonal(material.isSeasonal());
        oldTags = mat.getTags();
        mat.setTgs(tagDao.getTagsByNames(material.getTags(), ITag.MATERIAL_TAG));
        mat.setVertical(material.getVertical());
        mat.setMSRP(material.getMSRP());
        mat.setSalePrice(material.getSalePrice());
        mat.setRetailerPrice(material.getRetailerPrice());
        mat.setCurrency(material.getCurrency());
        mat.setLastUpdated(new Date());
        mat.setLastUpdatedBy(material.getLastUpdatedBy());
        // NOTE: short-code is not something that can be updated (it is set once)
        mat.setShortName(material.getShortName());
        mat.setInfo(material.getInfo());
        mat.setInfoDisplay(material.displayInfo());
        mat.setBatchEnabled(material.isBatchEnabled());
        mat.setBatchEnabledOnMobile(material.isBatchEnabled());
        mat.setTemperatureSensitive(material.isTemperatureSensitive());
        mat.setTemperatureMax(material.getTemperatureMax());
        mat.setTemperatureMin(material.getTemperatureMin());
        // Check if custom ID is specified for the material. If yes, check if the specified custom ID already exists.
        boolean customIdExists = false;
        xLogger.fine("Checking if customId {0} exists:", mat.getCustomId());

        if (material.getCustomId() != null && !material.getCustomId().isEmpty() && !material
            .getCustomId().equals(mat.getCustomId())) {
          customIdExists =
              materialDao.checkCustomIdExists(material.getDomainId(), material.getCustomId());
        }

        if (customIdExists) {
          // Custom ID already exists in the database!
          xLogger.warn(
              "updateMaterial: FAILED!! Cannot update material {0}. Custom ID {1} already exists.",
              material.getName(), material.getCustomId());
          final Locale locale = SecurityUtils.getLocale();
          ResourceBundle messages = Resources.get().getBundle("Messages", locale);
          ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
          throw new ServiceException(
              backendMessages.getString("error.cannotupdate") + " '" + material.getName() + "'. "
                  + messages.getString("customid") + " " + material.getCustomId() + " "
                  + backendMessages.getString("error.alreadyexists") + ".");
        }
        mat.setCustomId(material.getCustomId());
        material = pm.makePersistent(mat);
        material = pm.detachCopy(material);
      } catch (JDOObjectNotFoundException e) {
        xLogger.warn("updateMaterial: FAILED!! Material does not exist: {0}",
            material.getMaterialId());
        materialExists = false;
      }
      tx.commit();

      // Update tags, if needed
      PersistenceManager tagsPm = PMF.get().getPersistenceManager();
      try {
        AppFactory.get().getDaoUtil()
            .updateTags(material.getDomainIds(), oldTags, material.getTags(), TagUtil.TYPE_MATERIAL,
                material.getMaterialId(), tagsPm);
      } finally {
        tagsPm.close();
      }
      // Generate event, if configured

      try {
        // Get the material tags if configured.
        EventPublisher.generate(domainId, IEvent.MODIFIED, null, Material.class.getName(),
            materialDao.getKeyString(material.getMaterialId()),
            null);
      } catch (EventGenerationException e) {
        xLogger.warn(
            "Exception when generating event for material modification for material {0} in domain {1}: {2}",
            material.getMaterialId(), material.getDomainId(), e.getMessage());
      }
    } finally {
      if (tx.isActive()) {
        xLogger.warn("updateMaterial: Rolling back transaction");
        tx.rollback();
      }
      xLogger.fine("Exiting updateMaterial");
      pm.close();
    }
    if (!materialExists) {
      throw new ServiceException("Material does not exist");
    }
  }

	/* (non-Javadoc)
         * @see org.lggi.samaanguru.service.MaterialCatalogService#getMaterial(java.lang.Long)
	 */

  public IMaterial getMaterial(Long materialId) throws ServiceException {
    xLogger.fine("Entering getMaterial");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      //Get the material object from the database
      IMaterial material = JDOUtils.getObjectById(IMaterial.class, materialId, pm);
      //If we get here, it means the material exists
      material = pm.detachCopy(material);
      xLogger.fine("Exiting getMaterial");
      return material;
    } catch (JDOObjectNotFoundException e) {
      xLogger.warn("getMaterial: FAILED!!! Material {0} does not exist in the database", materialId,
          e);
      final Locale locale = SecurityUtils.getLocale();
      ResourceBundle messages = Resources.get().getBundle("Messages", locale);
      ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);

      throw new ServiceException(
          messages.getString("material") + " " + materialId + " " + backendMessages
              .getString("error.notfound"));
    } finally {
      pm.close();
    }
  }

  /**
   * Get a materialId, given a domain and material short-code
   */
  @SuppressWarnings({"unchecked"})
  public Long getMaterialId(Long domainId, String shortCode) throws ServiceException {
    xLogger.fine("Entered getMaterialId");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query
        q =
        pm.newQuery("SELECT materialId FROM " + JDOUtils.getImplClass(IMaterial.class).getName() +
            " WHERE dId.contains(dIdParam) && scode == scodeParam PARAMETERS Long dIdParam, String scodeParam");
    Long materialId = null;
    try {
      List<Long> materialIds = (List<Long>) q.execute(domainId, shortCode);
      if (materialIds != null && !materialIds.isEmpty()) {
        materialId = materialIds.get(0);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    xLogger.fine("Exiting getMaterialId");
    return materialId;
  }

  @SuppressWarnings({"unchecked"})
  public IMaterial getMaterialByName(Long domainId, String materialName) throws ServiceException {
    xLogger.fine("Entering getMaterialByName");
    if (domainId == null || materialName == null || materialName.isEmpty()) {
      throw new ServiceException("Invalid parameters");
    }
    IMaterial m = null;
    // Form query
    PersistenceManager pm = PMF.get().getPersistenceManager();

    try {
      // Form the query
      Query materialQuery = pm.newQuery(JDOUtils.getImplClass(IMaterial.class));
      materialQuery.setFilter("dId.contains(dIdParam) && uName == nameParam");
      materialQuery.declareParameters("Long dIdParam, String nameParam");
      // Execute the query
      try {
        List<IMaterial>
            results =
            (List<IMaterial>) materialQuery.execute(domainId, materialName.toLowerCase());
        if (results != null && !results.isEmpty()) {
          m = results.get(0);
          m = pm.detachCopy(m);
        }
      } finally {
        materialQuery.closeAll();
      }

    } catch (Exception e) {
      xLogger.severe("{0} when trying to get Material for Material Name {1}. Message: {2}",
          e.getClass().getName(), materialName, e.getMessage());
    } finally {
      pm.close();
    }
    xLogger.fine("Exiting getMaterial");
    return m;
  }

  /* (non-Javadoc)
   * @see org.lggi.samaanguru.service.MaterialCatalogService#deleteMaterials(java.util.List)
   */
  public void deleteMaterials(Long domainId, List<Long> materialIds) throws ServiceException {
    xLogger.fine("Entering deleteMaterials");
    IMaterial material;

    PersistenceManager pm = PMF.get().getPersistenceManager();
    PersistenceManager tagsPm = PMF.get().getPersistenceManager();
    try {
      List<IMaterial> materials = new ArrayList<>(materialIds.size());
      List<String> sdFailedMaterials = new ArrayList<>(1);
      for (Long materialId : materialIds) {
        try {
          material = JDOUtils.getObjectById(IMaterial.class, materialId, pm);
          if (domainId.equals(material.getDomainId())) {
            materials.add(material);
          } else {
            sdFailedMaterials.add(materialId.toString());
          }
        } catch (JDOObjectNotFoundException e) {
          xLogger.warn("Error while deleting materials. Could not find material with Id {0}",
              materialId);
        }
      }
      if (!sdFailedMaterials.isEmpty()) {
        final Locale locale = SecurityUtils.getLocale();
        ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);

        throw new ServiceException(
            backendMessages.getString("material.deletion.permission.denied") + " : " + StringUtil
                .getCSV(sdFailedMaterials));
      }
      for (Long materialId : materialIds) {
        try {
          // Get material
          material = JDOUtils.getObjectById(IMaterial.class, materialId, pm);
          // Get the material tags
//					List<String> tags = material.getTags();
          // Generate event
          try {
            EventPublisher
                .generate(domainId, IEvent.DELETED, null, Material.class.getName(),
                    materialDao.getKeyString(materialId), null, material);
          } catch (EventGenerationException e) {
            xLogger.warn(
                "Exception when generating event for material-deletion for material {0} in domain {1}: {2}",
                materialId, domainId, e.getMessage());
          }
          materials.add(material);
          // Delete related entities
          List<Long> materialDomainIds = material.getDomainIds();
          for (Long dId : materialDomainIds) {
            EntityRemover
                .removeRelatedEntities(dId, JDOUtils.getImplClass(IMaterial.class).getName(),
                    materialId, false);
          }
        } catch (JDOObjectNotFoundException e) {
          xLogger.warn("Error while deleting materials. Could not find material with Id {0}",
              materialId);
        }
      }
      pm.deletePersistentAll(materials);
    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    } finally {
      try {
        pm.close();
      } catch (Exception ignored) {

      }
      tagsPm.close();
    }
  }

  @SuppressWarnings("unchecked")
  public Results getAllMaterials(Long domainId, String tag, PageParams pageParams)
      throws ServiceException {
    return materialDao.getAllMaterials(domainId, tag, pageParams);
  }

  public Results searchMaterialsNoHU(Long domainId, String q) {
    return materialDao.searchMaterialsNoHU(domainId, q);
  }

  // Get the short-code for a newer material
  private String getMaterialShortCode(Long domainId) throws ServiceException {
    xLogger.fine("Entered getMaterialShortCode");
    ICounter counter = Counter.getMaterialCounter(domainId, null);
    xLogger.fine("Exiting getMaterialShortCode");
    return String.valueOf(counter.getCount() + 1);
  }

  // Increment the material counter by specified amount
  private void incrementMaterialCounter(List<Long> domainIds, int amount, PersistenceManager pm) {
    if (domainIds == null || domainIds.isEmpty()) {
      return;
    }
    for (Long domainId : domainIds) {
      Counter.getMaterialCounter(domainId, null).increment(amount, pm);
    }
  }

  public List<Long> getAllMaterialIds(Long domainId) {
    return materialDao.getAllMaterialsIds(domainId);
  }

  public List<IMaterialManufacturers> getMaterialManufacturers(Long materialId)
      throws ServiceException {
    PersistenceManager pm = getPM();
    String query = "SELECT * FROM MATERIALMANUFACTURERS WHERE MATERIAL_ID = ?";
    Query q = pm.newQuery("javax.jdo.query.SQL", query);
    q.setClass(JDOUtils.getImplClass(IMaterialManufacturers.class));
    try {
      List<IMaterialManufacturers> manufacturers =
          (List<IMaterialManufacturers>) q.execute(materialId);
      manufacturers = (List<IMaterialManufacturers>) pm.detachCopyAll(manufacturers);
      return manufacturers;
    } catch (Exception e) {
      xLogger.warn("Error while fetching manufacturer list for material {0}", materialId, e);
      throw e;
    } finally {
      q.closeAll();
      pm.close();
    }
  }

  public PersistenceManager getPM() {
    return PMF.get().getPersistenceManager();
  }

}
