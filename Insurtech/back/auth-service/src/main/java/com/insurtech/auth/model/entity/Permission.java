package com.insurtech.auth.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PERMISSIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PERMISSIONS")
    @SequenceGenerator(name = "SEQ_PERMISSIONS", sequenceName = "SEQ_PERMISSIONS", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;
}