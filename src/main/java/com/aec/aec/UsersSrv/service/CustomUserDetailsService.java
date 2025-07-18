
package com.aec.aec.UsersSrv.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.aec.aec.UsersSrv.modelo.User;
import com.aec.aec.UsersSrv.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        User usuario = userRepo.findByNombreUsuario(nombreUsuario)
    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombreUsuario));


        return org.springframework.security.core.userdetails.User.builder()
            .username(usuario.getNombreUsuario())
            .password(usuario.getClave())
            //.roles(usuario.getRol().name().substring(5)) 
            .authorities(usuario.getRol().name())
            .build();
    }
} 
    

