package com.insurtech.quote.repository;

import com.insurtech.quote.model.entity.Quote;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface QuoteRepository extends ReactiveMongoRepository<Quote, String> {

    Mono<Quote> findByQuoteNumber(String quoteNumber);

    Flux<Quote> findByCustomerId(Long customerId);

    Flux<Quote> findByCustomerIdAndStatus(Long customerId, Quote.QuoteStatus status);

    Flux<Quote> findByValidUntilBefore(LocalDateTime dateTime);

    Flux<Quote> findByQuoteTypeAndStatus(Quote.QuoteType quoteType, Quote.QuoteStatus status);

    Flux<Quote> findByCustomerIdAndQuoteType(Long customerId, Quote.QuoteType quoteType);

    Flux<Quote> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}