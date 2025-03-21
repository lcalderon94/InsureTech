package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.Claim;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class ClaimRepositoryCustomImpl implements ClaimRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Claim> search(String searchTerm, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Claim> query = cb.createQuery(Claim.class);
        Root<Claim> root = query.from(Claim.class);

        String likePattern = "%" + searchTerm.toLowerCase() + "%";

        // Crear predicados para cada campo que desea buscar
        List<Predicate> predicates = new ArrayList<>();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("claimNumber")), likePattern));
            predicates.add(cb.like(cb.lower(root.get("policyNumber")), likePattern));
            predicates.add(cb.like(cb.lower(root.get("customerNumber")), likePattern));
            predicates.add(cb.like(cb.lower(root.get("incidentDescription")), likePattern));
        }

        // Combinar predicados con OR
        query.where(cb.or(predicates.toArray(new Predicate[0])));

        // Aplicar ordenamiento
        if (pageable.getSort().isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();

            for (Sort.Order order : pageable.getSort()) {
                if (order.isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            }

            query.orderBy(orders);
        }

        // Aplicar paginación
        TypedQuery<Claim> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Contar resultados totales para paginación
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Claim> countRoot = countQuery.from(Claim.class);
        countQuery.select(cb.count(countRoot));
        if (!predicates.isEmpty()) {
            countQuery.where(cb.or(predicates.toArray(new Predicate[0])));
        }
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }
}