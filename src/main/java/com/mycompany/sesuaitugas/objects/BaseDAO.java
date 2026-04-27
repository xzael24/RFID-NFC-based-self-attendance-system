package com.mycompany.sesuaitugas.objects;

import java.util.List;

public interface BaseDAO<T> {
    void save(T entity);
    void update(int index, T entity);
    void delete(int index);
    List<T> findAll();
    T findByIndex(int index);    
}