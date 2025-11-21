package AdminBackend.Repository;

import com.auctionaa.backend.Entity.Admin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByStatus(int status);

    @Query("{ $or: [ " +
           "{ 'id': { $regex: ?0, $options: 'i' } }, " +
           "{ 'fullName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'email': { $regex: ?0, $options: 'i' } }, " +
           "{ 'phoneNumber': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Admin> searchAdmins(String searchTerm);
}


