package com.insurtech.payment.util;

import com.insurtech.payment.model.dto.*;
import com.insurtech.payment.model.entity.*;
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
    public <D, T> D toDto(final T entity) {
        if (entity == null) {
            return null;
        }
        return (D) modelMapper.map(entity, getTargetDtoClass(entity));
    }

    /**
     * Mapea un objeto de tipo DTO a un objeto de tipo Entity
     */
    public <D, T> T toEntity(final D dto) {
        if (dto == null) {
            return null;
        }
        return (T) modelMapper.map(dto, getTargetEntityClass(dto));
    }


    public RefundDto mapToDto(Refund refund) {
        return modelMapper.map(refund, RefundDto.class);
    }

    // Y este para PaymentMethodDto
    public PaymentMethodDto mapToDto(PaymentMethod paymentMethod) {
        return modelMapper.map(paymentMethod, PaymentMethodDto.class);
    }

    public <T, D> D mapToDto(T entity, Class<D> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }

    /**
     * Mapea una colección de entidades a una lista de DTOs
     */
    public <D, T> List<D> toDtoList(final Collection<T> entityList) {
        return entityList.stream()
                .map(entity -> (D) toDto(entity))
                .collect(Collectors.toList());
    }

    /**
     * Mapea una colección de DTOs a una lista de entidades
     */
    public <D, T> List<T> toEntityList(final Collection<D> dtoList) {
        return dtoList.stream()
                .map(dto -> (T) toEntity(dto))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza los valores de un objeto existente con los valores de otro
     */
    public <T> T updateExistingObject(final Object source, T target) {
        modelMapper.map(source, target);
        return target;
    }

    // Métodos específicos para mapeo de entidades comunes
    public PaymentDto toDto(Payment payment) {
        return modelMapper.map(payment, PaymentDto.class);
    }

    public Payment toEntity(PaymentDto paymentDto) {
        return modelMapper.map(paymentDto, Payment.class);
    }

    public InvoiceDto toDto(Invoice invoice) {
        return modelMapper.map(invoice, InvoiceDto.class);
    }

    public Invoice toEntity(InvoiceDto invoiceDto) {
        return modelMapper.map(invoiceDto, Invoice.class);
    }

    public PaymentMethodDto toDto(PaymentMethod paymentMethod) {
        return modelMapper.map(paymentMethod, PaymentMethodDto.class);
    }

    public PaymentMethod toEntity(PaymentMethodDto paymentMethodDto) {
        return modelMapper.map(paymentMethodDto, PaymentMethod.class);
    }

    public RefundDto toDto(Refund refund) {
        return modelMapper.map(refund, RefundDto.class);
    }

    public Refund toEntity(RefundDto refundDto) {
        return modelMapper.map(refundDto, Refund.class);
    }

    public TransactionDto toDto(Transaction transaction) {
        return modelMapper.map(transaction, TransactionDto.class);
    }

    public Transaction toEntity(TransactionDto transactionDto) {
        return modelMapper.map(transactionDto, Transaction.class);
    }

    // Métodos de ayuda para determinar la clase de destino
    private Class<?> getTargetDtoClass(Object entity) {
        if (entity instanceof Payment) return PaymentDto.class;
        if (entity instanceof Invoice) return InvoiceDto.class;
        if (entity instanceof PaymentMethod) return PaymentMethodDto.class;
        if (entity instanceof Refund) return RefundDto.class;
        if (entity instanceof Transaction) return TransactionDto.class;
        if (entity instanceof PaymentPlan) return PaymentPlanDto.class;
        throw new IllegalArgumentException("Tipo de entidad no soportado: " + entity.getClass().getName());
    }

    private Class<?> getTargetEntityClass(Object dto) {
        if (dto instanceof PaymentDto) return Payment.class;
        if (dto instanceof InvoiceDto) return Invoice.class;
        if (dto instanceof PaymentMethodDto) return PaymentMethod.class;
        if (dto instanceof RefundDto) return Refund.class;
        if (dto instanceof TransactionDto) return Transaction.class;
        if (dto instanceof PaymentPlanDto) return PaymentPlan.class;
        throw new IllegalArgumentException("Tipo de DTO no soportado: " + dto.getClass().getName());
    }
}