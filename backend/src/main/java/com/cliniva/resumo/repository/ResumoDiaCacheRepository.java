package com.cliniva.resumo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cliniva.resumo.model.ResumoDiaCache;
import com.cliniva.resumo.model.ResumoDiaCacheId;

public interface ResumoDiaCacheRepository extends JpaRepository<ResumoDiaCache, ResumoDiaCacheId> {
}