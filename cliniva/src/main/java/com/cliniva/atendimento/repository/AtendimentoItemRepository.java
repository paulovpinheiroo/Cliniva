package com.cliniva.atendimento.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cliniva.atendimento.model.AtendimentoItem;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.item.Item;

public interface AtendimentoItemRepository extends JpaRepository<AtendimentoItem, UUID> {

    Optional<AtendimentoItem> findByAtendimentoServicoAndItem(AtendimentoServico atendimentoServico, Item item);

}
