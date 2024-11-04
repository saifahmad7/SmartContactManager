package com.smart.dao;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entities.Contact;
import com.smart.entities.User;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

    // Pagination query for fetching contacts associated with a specific user by user ID
    @Query("from Contact c where c.user.id = :userId")
    Page<Contact> findContactsByUser(@Param("userId") int userId, Pageable pageable);

    // Search query for fetching contacts by name containing a certain substring for a specific user
    List<Contact> findByNameContainingAndUser(String name, User user);
}


