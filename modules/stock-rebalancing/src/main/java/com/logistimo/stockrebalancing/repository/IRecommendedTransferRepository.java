package com.logistimo.stockrebalancing.repository;

import com.logistimo.stockrebalancing.entity.RecommendedTransfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Created by nitisha.khandelwal on 23/03/18.
 */

@org.springframework.stereotype.Repository
public interface IRecommendedTransferRepository extends
    JpaRepository<RecommendedTransfer, String>,
    JpaSpecificationExecutor<RecommendedTransfer> {

}
