package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    Optional<User> findByUsername(String username);

    // Search methods for admin
    @Query("{ $or: [ " +
           "{ 'id': { $regex: ?0, $options: 'i' } }, " +
           "{ 'username': { $regex: ?0, $options: 'i' } }, " +
           "{ 'phonenumber': { $regex: ?0, $options: 'i' } }, " +
           "{ 'cccd': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<User> searchUsers(String searchTerm);

    // Count methods for statistics
    long countByRole(int role);
    
    long countByStatus(int status);
}
