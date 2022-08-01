package com.proceed.swhackathon.repository;

import com.proceed.swhackathon.model.Order;
import com.proceed.swhackathon.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o join fetch o.store s where o.id = :id")
    Optional<Order> findOrderByIdWithStore(Long id);

    @Query("select o from Order o where o.store = :store")
    List<Order> findByStore(Store store);

}
