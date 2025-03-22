package com.insurtech.payment.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityDtoMapper {

    private final ModelMapper modelMapper;

    public EntityDtoMapper() {
        this.modelMapper = new ModelMapper();
        this.modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);
    }

    /**
     * Mapea un objeto de tipo Entity a un objeto de tipo DTO
     */
    public <D, T> D mapToDto(final T entity, Class<D> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }

    /**
     * Mapea un objeto de tipo DTO a un objeto de tipo Entity
     */
    public <D, T> T mapToEntity(final D dto, Class<T> entityClass) {
        return modelMapper.map(dto, entityClass);
    }

    /**
     * Mapea una colección de entidades a una lista de DTOs
     */
    public <D, T> List<D> mapToDtoList(final Collection<T> entityList, Class<D> dtoClass) {
        return entityList.stream()
                .map(entity -> mapToDto(entity, dtoClass))
                .collect(Collectors.toList());
    }

    /**
     * Mapea una colección de DTOs a una lista de entidades
     */
    public <D, T> List<T> mapToEntityList(final Collection<D> dtoList, Class<T> entityClass) {
        return dtoList.stream()
                .map(dto -> mapToEntity(dto, entityClass))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza los valores de un objeto existente con los valores de otro
     */
    public <T> T updateExistingObject(final Object source, T target) {
        modelMapper.map(source, target);
        return target;
    }
}