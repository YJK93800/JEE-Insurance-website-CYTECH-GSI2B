package com.assurcy2000.dao;

import java.util.List;
import java.util.Optional;

public interface BaseDao<T, ID> {
    T save (T e);
    Optional<T> findById(ID id);
    List<T> findAll();
    T update(T e);
    void delete(T e);
}