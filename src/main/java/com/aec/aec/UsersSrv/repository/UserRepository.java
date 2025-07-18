package com.aec.aec.UsersSrv.repository;
import com.aec.aec.UsersSrv.modelo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNombreUsuario(String nombreUsuario); 
    boolean existsByEmail(String email);
boolean existsByNombreUsuario(String nombreUsuario);
}