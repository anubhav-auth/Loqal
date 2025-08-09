package com.loqal.merchantservice.repository;


import com.loqal.merchantservice.entity.MerchantExtended;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MerchantExtendedRepository extends JpaRepository<MerchantExtended, UUID> {

}
