package org.sopt.daiso.store.repository;

import org.sopt.daiso.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
