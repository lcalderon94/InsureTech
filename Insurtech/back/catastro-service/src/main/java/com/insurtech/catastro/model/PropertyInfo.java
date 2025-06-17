package com.insurtech.catastro.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyInfo {
    private String id;
    private String address;
    private String owner;
}
