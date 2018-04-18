ALTER TABLE INVNTRY ADD COLUMN IAT DATETIME DEFAULT NULL;
ALTER TABLE DEMANDITEM ADD COLUMN RQ DECIMAL(16,4) DEFAULT NULL AFTER DQ;

UPDATE INVNTRY AS I LEFT JOIN (SELECT MIN(T) AS UT, KID, MID FROM TRANSACTION GROUP BY KID, MID) AS T ON I.KID = T.KID AND I.MID = T.MID
SET I.IAT = T.UT;

create index INVNTRYEVNTLOG_TY_ED on INVNTRYEVNTLOG(TY,ED);

update KIOSK set CPERM=0 where CPERM=2;
update KIOSK set VPERM=0 where VPERM=2;

ALTER TABLE `ORDER` CHANGE RID SALES_REF_ID VARCHAR(100), ADD COLUMN PURCHASE_REF_ID VARCHAR(100), ADD COLUMN TRANSFER_REF_ID VARCHAR(100);
ALTER TABLE SHIPMENT CHANGE RID SALES_REF_ID VARCHAR(100);

CREATE TABLE RETURNS (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    source_domain BIGINT(20) DEFAULT NULL,
    order_id BIGINT(20) DEFAULT NULL,
    customer_id BIGINT(20) DEFAULT NULL,
    vendor_id BIGINT(20) DEFAULT NULL,
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    geo_accuracy DOUBLE DEFAULT NULL,
    geo_error VARCHAR(255) DEFAULT NULL,
    status VARCHAR(25) DEFAULT NULL,
    cancel_reason VARCHAR(255) DEFAULT NULL,
    status_updated_at DATETIME DEFAULT NULL,
    status_updated_by VARCHAR(25) DEFAULT NULL,
    created_at DATETIME DEFAULT NULL,
    created_by VARCHAR(25) DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    updated_by VARCHAR(25) DEFAULT NULL,
    source INT(3) DEFAULT NULL,
    PRIMARY KEY (`ID`)
)  ENGINE=INNODB AUTO_INCREMENT=4001100 DEFAULT CHARSET=UTF8;

create index IDX_ORDER_ID on RETURNS (order_id);

CREATE TABLE RETURNS_ITEM (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    returns_id BIGINT(20) DEFAULT NULL,
    material_id BIGINT(20) DEFAULT NULL,
    quantity DECIMAL(16 , 4 ) DEFAULT NULL,
    material_status VARCHAR(255) DEFAULT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    received_quantity DECIMAL(16 , 4 ) DEFAULT NULL,
    received_material_status VARCHAR(255) DEFAULT NULL,
    discrepancy_reason VARCHAR(255) DEFAULT NULL,
    created_at DATETIME DEFAULT NULL,
    created_by VARCHAR(25) DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    updated_by VARCHAR(25) DEFAULT NULL,
    PRIMARY KEY (`ID`)
)  ENGINE=INNODB AUTO_INCREMENT=1000100 DEFAULT CHARSET=UTF8;

create index IDX_RETURNS_ID on RETURNS_ITEM (returns_id);

CREATE TABLE RETURNS_ITEM_BATCH (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    item_id BIGINT(20) DEFAULT NULL,
    batch_id VARCHAR(255) DEFAULT NULL,
    expiry DATETIME DEFAULT NULL,
    manufacturer VARCHAR(255) DEFAULT NULL,
    manufactured DATETIME DEFAULT NULL,
    quantity DECIMAL(16 , 4 ) DEFAULT NULL,
    material_status VARCHAR(255) DEFAULT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    received_quantity DECIMAL(16 , 4 ) DEFAULT NULL,
    received_material_status VARCHAR(255) DEFAULT NULL,
    discrepancy_reason VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
)  ENGINE=INNODB AUTO_INCREMENT=1100100 DEFAULT CHARSET=UTF8;

create index IDX_RETURNSITEM_ID on RETURNS_ITEM_BATCH (item_id);

CREATE OR REPLACE TABLE `EXECUTION_METADATA` (
  `id` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `domain_id` bigint(20) DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE OR REPLACE TABLE `STOCK_REBALANCING_EVENTS` (
  `id` varchar(255) NOT NULL,
  `execution_id` varchar(255) NOT NULL,
  `kiosk_id` bigint(20) NOT NULL,
  `material_id` bigint(20) NOT NULL,
  `quantity` decimal(19,2) NOT NULL,
  `short_code` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `priority` varchar(255) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `value` decimal(19,2) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKosbxk174c1ysl69viymmxteq5` (`execution_id`),
  KEY `FKrd22ncxxld31ire57ee0ooq3t` (`kiosk_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE OR REPLACE TABLE `STOCK_REBALANCING_EVENT_BATCHES` (
  `id` varchar(255) NOT NULL,
  `batch_id` varchar(255) DEFAULT NULL,
  `expiry_date` datetime DEFAULT NULL,
  `manufacture_date` datetime DEFAULT NULL,
  `manufacturer_name` varchar(255) DEFAULT NULL,
  `stock_rebalancing_event_id` varchar(255) NOT NULL,
  `transfer_quantity` decimal(19,2) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9af0olxvybsj90sc7gkwrss74` (`stock_rebalancing_event_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE OR REPLACE TABLE `RECOMMENDED_TRANSFERS` (
  `id` varchar(255) NOT NULL,
  `cost` decimal(19,2) NOT NULL,
  `destination_event_id` varchar(255) DEFAULT NULL,
  `material_id` bigint(20) NOT NULL,
  `quantity` decimal(19,2) NOT NULL,
  `source_event_id` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `transfer_id` bigint(20) DEFAULT NULL,
  `value` decimal(19,2) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

/* eVIN */
UPDATE `ORDER` SET PURCHASE_REF_ID = IF(OTY = 1,SALES_REF_ID,NULL),
TRANSFER_REF_ID = IF(OTY = 0,SALES_REF_ID,NULL),
SALES_REF_ID = IF(OTY = 2,SALES_REF_ID,NULL);

/* AWS */
UPDATE `ORDER` SET PURCHASE_REF_ID = IF(OTY = 1,SALES_REF_ID,NULL),
TRANSFER_REF_ID = IF(OTY = 0,SALES_REF_ID,NULL),
SALES_REF_ID = IF(OTY = 2,SALES_REF_ID,NULL) WHERE SDID <> 1343921;
