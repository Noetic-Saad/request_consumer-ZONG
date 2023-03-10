package com.noeticworld.sgw.requestConsumer.repository;

import com.noeticworld.sgw.requestConsumer.entities.LoginEntity;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface LoginRepository extends JpaRepository<LoginEntity,Integer> {
    @Transactional
    @Modifying
    @Query(value = "update login set code = '0' where id = (select id from login where msisdn=:msisdn order by id desc limit 1)",nativeQuery = true)
    void updateLoginTable(@Param("msisdn") long msisdn);
    LoginEntity findTopByMsisdn(long Msisdn);
}
