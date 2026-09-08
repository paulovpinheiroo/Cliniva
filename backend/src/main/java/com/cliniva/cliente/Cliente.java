package com.cliniva.cliente;

import java.time.LocalDate;

import com.cliniva.cliente.enums.CanalPreferido;
import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.cliente.enums.OrigemCliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Cliente")
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;
    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "email", unique = true)
    private String email;
    @Column(name = "telefone", unique = true, nullable = false)
    private String telefone;
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ClienteStatus status = ClienteStatus.PROSPECT;
    @Enumerated(EnumType.STRING)
    @Column(name = "origem")
    private OrigemCliente origem;
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_preferido")
    private CanalPreferido canalPreferido;
    @Column(name = "preferencias")
    private String preferencias;
    @Column(name = "observacoes")
    private String observacoes;

}