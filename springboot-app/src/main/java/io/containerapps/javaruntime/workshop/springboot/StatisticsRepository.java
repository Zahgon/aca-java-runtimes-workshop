package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
interface StatisticsRepository extends CrudRepository<Statistics, Long> {
}
